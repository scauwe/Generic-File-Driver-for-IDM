/*******************************************************************************
 * Copyright (c) 2007-2016 Stefaan Van Cauwenberge
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
 *  the Initial Developer are Copyright (C) 2007-2016 by
 * Stefaan Van Cauwenberge. All Rights Reserved.
 *
 * Contributor(s): none so far.
 *    Stefaan Van Cauwenberge: Initial API and implementation
 *******************************************************************************/
package info.vancauwenberge.filedriver.filereader.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

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

public class XMLFileReader extends AbstractStrategy implements IFileReadStrategy{
	/*
	static final String TAG_USE_TAG_NAMES = "xmlReader_useTagNames";
	static final String TAG_PRE_XSLT = "xmlReader_preXslt";
	static final String TAG_FORCED_ENCODING ="xmlReader_forcedEncoding";
	 */
	protected enum Parameters implements IStrategyParameters{
		USE_TAG_NAMES  ("xmlReader_useTagNames"   ,"true",DataType.BOOLEAN),
		PRE_XSLT       ("xmlReader_preXslt"       ,""    ,DataType.STRING),
		FORCED_ENCODING("xmlReader_forcedEncoding",null  ,DataType.STRING);

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

	private boolean useTagNames = true;
	private String[] tagNames;
	private Transformer xsltTransformer;
	private SaxHandler handler = null;

	/**
	 * The encoding to use for the XML file. Empty will use the encoding as specified in the XML file (or the platform default if not specified).
	 * Comment for <code>encoding</code>
	 */
	private String encoding = "ISO-8859-1";
	private Trace trace;

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
		this.trace = trace;

		useTagNames = getBoolValueFor(Parameters.USE_TAG_NAMES,driverParams);
		tagNames = GenericFileDriverShim.getSchemaAsArray(driverParams);

		encoding = getStringValueFor(Parameters.FORCED_ENCODING, driverParams);
		if ("".equals(encoding)) {
			encoding=null;
		}
		final String preXslt = getStringValueFor(Parameters.PRE_XSLT, driverParams);
		if ((preXslt != null) && !"".equals(preXslt.trim())) //We need to apply an xslt prior to processing the file
		{
			// construct a transformer using the generic stylesheet
			final TransformerFactory factory = TransformerFactory.newInstance();
			final StreamSource xslSource     = new StreamSource(new StringReader(preXslt));
			try {
				xsltTransformer    = factory.newTransformer(xslSource);
				if (encoding != null) {
					xsltTransformer.setOutputProperty( OutputKeys.ENCODING, encoding);
				}
			} catch (final TransformerConfigurationException e) {
				Util.printStackTrace(trace,e);
				throw new XDSParameterException("Error creating XSLT transformer:"+e.getMessage());
			}   			
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	@Override
	public void openFile(final File initialFile) throws ReadException {
		try {
			File targetFile;
			//Transform if required
			if (xsltTransformer!=null){
				targetFile = transformFile(initialFile);
			}else{
				targetFile = initialFile;
			}

			// Start the parser that will parse this document		
			final InputSource is = getEncodedInputSource(targetFile);
			final XMLReader xr = XMLReaderFactory.createXMLReader();
			handler = new SaxHandler(useTagNames, tagNames);
			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);
			queue = handler.getQueue();

			parsingThread = new Thread(){
				@Override
				public void run(){
					try {
						//FileReader r = new FileReader(f);
						//xr.parse(new InputSource(r));	
						xr.parse(is);
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

	/**
	 * XSLT Transform the file. Write the result to a new file. Return the new file.
	 * @param initialFile
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 * @throws TransformerException
	 */
	private File transformFile(final File initialFile) throws TransformerException, IOException {

		Source source;
		if (encoding != null) //Use the given encoding
		{
			final InputStreamReader fis = new InputStreamReader(new FileInputStream(initialFile), encoding);
			source = new StreamSource(fis);
		}
		else//Use system default or as specified in XML file (encoding=...)
		{
			source=new StreamSource(initialFile);
		}


		final File targetFile = new File(initialFile.getParent(), initialFile.getName()+".transformed");
		//If we were restarted, the targetfile might be already present. Delete it and start over.
		if (targetFile.exists()) {
			targetFile.delete();
		}

		if (encoding != null)
		{
			final OutputStreamWriter fis = new OutputStreamWriter(new FileOutputStream(targetFile), encoding);
			final Result result = new StreamResult(fis);					
			xsltTransformer.transform(source,result);
			fis.close();
		}
		else
		{
			final Result result = new StreamResult(new FileOutputStream(targetFile));
			xsltTransformer.transform(source,result);		
		}
		return targetFile;

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
		handler = null;
	}

	@Override
	public String[] getActualSchema() {
		return handler.getCurrentSchema();
	}
}
