/*******************************************************************************
 * Copyright (c) 2007, 2018 Stefaan Van Cauwenberge
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0 (the "License"). If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  	 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Initial Developer of the Original Code is
 * Stefaan Van Cauwenberge. Portions created by
 *  the Initial Developer are Copyright (C) 2007, 2018 by
 * Stefaan Van Cauwenberge. All Rights Reserved.
 *
 * Contributor(s): none so far.
 *    Stefaan Van Cauwenberge: Initial API and implementation
 *******************************************************************************/
package info.vancauwenberge.filedriver.filereader.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IFileReadStrategy;
import info.vancauwenberge.filedriver.exception.ReadException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.filereader.RecordQueue;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

public class XPathXMLFileReader extends AbstractStrategy implements IFileReadStrategy{
	protected enum Parameters implements IStrategyParameters{

		USE_DRIVER_SCHEMA   ("xmlxpathReader_UseDriverSchema","true",DataType.BOOLEAN),
		FORCED_ENCODING     ("xmlxpathReader_forcedEncoding" ,null  ,DataType.STRING),
		XPATH_EXPRESSIONS   ("xmlxpathReader_attributeXpaths",null  ,DataType.STRING),
		XPATH_RECORD_NODESET("xmlxpathReader_recordXpath"    ,"/"   ,DataType.STRING);

		private Parameters(final String name, final String defaultValue, final DataType dataType) {
			this.name = name;
			this.defaultValue = defaultValue;
			this.dataType = dataType;
		}

		private final String name;
		private final String defaultValue;
		private final DataType dataType;

		@Override
		public String getParameterName(){
			return name;
		}

		@Override
		public String getDefaultValue(){
			return defaultValue;
		}

		@Override
		public DataType getDataType(){
			return dataType;
		}

		@Override
		public Constraint[] getConstraints() {
			return null;
		}
	}

	private RecordQueue queue;
	private Thread parsingThread;

	private String[] tagNames=null;

	/**
	 * The encoding to use for the XML file. Empty will use the encoding as specified in the XML file (or the platform default if not specified).
	 * Comment for <code>encoding</code>
	 */
	private String encoding = "ISO-8859-1";
	private Trace trace;
	private DocumentBuilder builder;
	private ArrayList<XPathExpression> xPathExpressions;
	private XPathExpression xpathRecordNodeSet;

	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	@Override
	public void init(final Trace trace, final Map<String,Parameter> driverParams, final IPublisher publisher) throws XDSParameterException {
		try {
			this.trace = trace;
			if (trace.getTraceLevel()>TraceLevel.TRACE){
				trace.trace("XPathXMLFileReader.init() driverParams:"+driverParams);
			}
			this.encoding = getStringValueFor(Parameters.FORCED_ENCODING,driverParams);
			if ("".equals(encoding)) {
				encoding=null;
			}

			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			this.builder = factory.newDocumentBuilder();

			final XPathFactory xPathfactory = XPathFactory.newInstance();
			final XPath xpath = xPathfactory.newXPath();
			final String xPathExpressionsParam = getStringValueFor(Parameters.XPATH_EXPRESSIONS,driverParams);
			final String[] expressions = xPathExpressionsParam.split(",");
			this.xPathExpressions = new ArrayList<XPathExpression>();
			final List<String> fieldNames = new ArrayList<String>();
			final StringBuilder thisExpr = new StringBuilder();
			for (final String anExpression : expressions) {
				try{
					if (thisExpr.length()>0){
						thisExpr.append(',').append(anExpression);
					}else{
						thisExpr.append(anExpression);					
					}
					final XPathExpression expr = xpath.compile(thisExpr.toString());
					trace.trace("Valid XPAth compiled:"+thisExpr.toString(), TraceLevel.TRACE);
					fieldNames.add(thisExpr.toString());
					xPathExpressions.add(expr);
					thisExpr.setLength(0);
				}catch(final Exception e){
					trace.trace("Illigal XPAth found, trying to merge with next:"+anExpression, TraceLevel.DEBUG);
				}
			}
			if (thisExpr.length()>0){
				throw new XDSParameterException("Invalid XPath. Renaming part:"+thisExpr.toString());				
			}

			if (getBoolValueFor(Parameters.USE_DRIVER_SCHEMA,driverParams)){
				tagNames=GenericFileDriverShim.getSchemaAsArray(driverParams);
				if (tagNames.length<fieldNames.size()){
					//The shim defined schema does not contain enough fields. We need to extend it with 'dummy' fieldsnames
					final String[] extendedTagNames = new String[fieldNames.size()];
					System.arraycopy(tagNames, 0, extendedTagNames, 0, tagNames.length);
					for (int i = tagNames.length; i < extendedTagNames.length; i++) {
						extendedTagNames[i]="field"+i;
					}
					tagNames = extendedTagNames;
				}
			}else{
				tagNames = new String[fieldNames.size()];
				for (int i = 0; i < tagNames.length; i++) {
					tagNames[i]=fieldNames.get(i).replaceAll("[^a-zA-Z0-9]", "_");					
				}
			}

			final String xPathRecordParam = getStringValueFor(Parameters.XPATH_RECORD_NODESET,driverParams);
			this.xpathRecordNodeSet = xpath.compile(xPathRecordParam);


		} catch (final ParserConfigurationException e1) {
			Util.printStackTrace(trace, e1);
			throw new XDSParameterException(e1.getClass().getName()+":"+e1.getMessage());
		} catch (final XPathExpressionException e) {
			Util.printStackTrace(trace, e);
			throw new XDSParameterException(e.getClass().getName()+":"+e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	@Override
	public void openFile(final File initialFile) throws ReadException {
		try {
			final InputSource is = getEncodedInputSource(initialFile);
			final Document doc = builder.parse(is);

			queue = new RecordQueue();

			parsingThread = new Thread(){
				@Override
				public void run(){
					try {
						final NodeList nodes = (NodeList) xpathRecordNodeSet.evaluate(doc, XPathConstants.NODESET);
						for (int i = 0; i < nodes.getLength(); i++) {
							final Map<String,String> record = new HashMap<String, String>();
							for (int j = 0; j < xPathExpressions.size(); j++) {
								final XPathExpression anExpression = xPathExpressions.get(j);
								String value=null;
								try{
									final Node result = (Node)anExpression.evaluate(nodes.item(i), XPathConstants.NODE);
									switch (result.getNodeType()) {
									case Node.ATTRIBUTE_NODE:
									case Node.TEXT_NODE:
									case Node.COMMENT_NODE:
									case Node.CDATA_SECTION_NODE:
										value=result.getNodeValue();									
										break;
									default:
										value=result.getNodeName();									
										break;
									} 
								}catch(final Exception e){
									try{
										//It seems that the XPath does not evaluate to a Node, try a String
										value = (String)anExpression.evaluate(nodes.item(i), XPathConstants.STRING);
									}catch(final Exception e3){
										//It seems that the XPath does not evaluate to a String, try a Boolean
										value = ((Boolean)anExpression.evaluate(nodes.item(i), XPathConstants.BOOLEAN)).toString();
									}
								}
								record.put(tagNames[j], value);
							}
							queue.addRecord(record);
						}
						queue.setFinished();
					} catch (final Exception e) {
						Util.printStackTrace(trace, e);
						queue.setFinishedInError(e);
					}
				}
			};
			parsingThread.setName("XMLParser");
			parsingThread.start();
		} catch (final Exception e1) {
			Util.printStackTrace(trace, e1);
			throw new ReadException("Exception while handeling XML document:" +e1.getClass().getName()+" - "+e1.getMessage(),e1);
		}
	}

	/**
	 * @param targetFile
	 * @return
	 * @throws FileNotFoundException
	 */
	private InputSource getEncodedInputSource(final File targetFile) throws UnsupportedEncodingException, FileNotFoundException {
		InputSource source;
		if (encoding != null) //Use the given encoding
		{
			final InputStreamReader fis = new InputStreamReader(new FileInputStream(targetFile), encoding);
			source = new InputSource(fis);
		}
		else//Use system default or as specified in XML file (encoding=...)
		{
			source = new InputSource(new FileInputStream(targetFile));
		}
		return source;
	}



	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#readRecord(com.novell.nds.dirxml.driver.Trace)
	 */
	@Override
	public Map<String, String> readRecord() throws ReadException {
		try{
			return queue.getNextRecord();
		}catch (final Exception e) {
			throw new ReadException(e);
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#close()
	 */
	@Override
	public void close() throws ReadException{
		queue = null;
		if (parsingThread.isAlive()){
			trace.trace("WARN: parsing thread is still alive...", TraceLevel.ERROR_WARN);
			try {
				parsingThread.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		//Thread is dead. Normal situation.
		parsingThread = null;
	}

	@Override
	public String[] getActualSchema() {
		return tagNames;
	}
}
