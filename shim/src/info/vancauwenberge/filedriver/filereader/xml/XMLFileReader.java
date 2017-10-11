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

import info.vancauwenberge.filedriver.api.IFileReadStrategy;
import info.vancauwenberge.filedriver.exception.ReadException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.filereader.RecordQueue;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
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
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

public class XMLFileReader implements IFileReadStrategy{
	
	private RecordQueue queue;
	private Thread parsingThread;
	private static final String TAG_USE_TAG_NAMES = "xmlReader_useTagNames";
	private static final String TAG_PRE_XSLT = "xmlReader_preXslt";
	private static final String TAG_FORCED_ENCODING ="xmlReader_forcedEncoding";
	
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

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#getParameterDefinitions()
	 */
	public Map<String,Parameter> getParameterDefinitions() {
		Map<String,Parameter> paramDefs = new HashMap<String,Parameter>(0);
        Parameter param;

        //Add file locator as param
        param = new Parameter(TAG_USE_TAG_NAMES, //tag name
                "true", //default value (optional)
                DataType.BOOLEAN); //data type
        paramDefs.put(param.tagName(), param);

        //we need info about the driver schema
        /*
        param = new Parameter(GenericFileDriverShim.TAG_SCHEMA, //tag name
        		GenericFileDriverShim.DEFAULT_SCHEMA, //default value
                              DataType.STRING); //data type
        param.add(RequiredConstraint.REQUIRED);
        paramDefs.put(param.tagName(), param);
*/
        //xslt transform prior to parsing?
        param = new Parameter(TAG_PRE_XSLT, //tag name
        		"", //default value: no transform
                DataType.STRING); //data type
        paramDefs.put(param.tagName(), param);

        //forced file encoding
        param = new Parameter(TAG_FORCED_ENCODING, //tag name
        		null, //default value: no transform
                DataType.STRING); //data type
        paramDefs.put(param.tagName(), param);

        return paramDefs;
		
		
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void init(Trace trace, Map<String,Parameter> driverParams, IPublisher publisher) throws XDSParameterException {
		this.trace = trace;

		useTagNames = driverParams.get(TAG_USE_TAG_NAMES).toBoolean().booleanValue();
   		tagNames = GenericFileDriverShim.getSchemaAsArray(driverParams);

   		encoding = driverParams.get(TAG_FORCED_ENCODING).toString();
   		if ("".equals(encoding))
   			encoding=null;
   		String preXslt = driverParams.get(TAG_PRE_XSLT).toString();
   		if (!"".equals(preXslt)) //We need to apply an xslt prior to processing the file
   		{
   	        // construct a transformer using the generic stylesheet
   	        TransformerFactory factory = TransformerFactory.newInstance();
   	        StreamSource xslSource     = new StreamSource(new StringReader(preXslt));
   	        try {
				xsltTransformer    = factory.newTransformer(xslSource);
				if (encoding != null)
					xsltTransformer.setOutputProperty( OutputKeys.ENCODING, encoding);
			} catch (TransformerConfigurationException e) {
				Util.printStackTrace(trace,e);
				throw new XDSParameterException("Error creating XSLT transformer:"+e.getMessage());
			}   			
   		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	public void openFile(File initialFile) throws ReadException {
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
				public void run(){
					try {
						//FileReader r = new FileReader(f);
						//xr.parse(new InputSource(r));	
						xr.parse(is);
					} catch (Exception e) {
						Util.printStackTrace(trace, e);
						queue.setFinishedInError(e);
					}
				}
			};
			parsingThread.setName("XMLParser");
			parsingThread.start();
		} catch (Exception e1) {
			Util.printStackTrace(trace, e1);
			throw new ReadException("Exception while handeling XML document:" +e1.getClass().getName()+" - "+e1.getMessage(),e1);
		}
	}

	/**
	 * @param targetFile
	 * @return
	 * @throws FileNotFoundException
	 */
	private InputSource getEncodedInputSource(File targetFile) throws UnsupportedEncodingException, FileNotFoundException {
		InputSource source;
		if (encoding != null) //Use the given encoding
		{
			InputStreamReader fis = new InputStreamReader(new FileInputStream(targetFile), encoding);
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
	private File transformFile(File initialFile) throws TransformerException, IOException {
		
		Source source;
		if (encoding != null) //Use the given encoding
		{
			InputStreamReader fis = new InputStreamReader(new FileInputStream(initialFile), encoding);
			source = new StreamSource(fis);
		}
		else//Use system default or as specified in XML file (encoding=...)
		{
			source=new StreamSource(initialFile);
		}

		 
		File targetFile = new File(initialFile.getParent(), initialFile.getName()+".transformed");
		//If we were restarted, the targetfile might be already present. Delete it and start over.
		if (targetFile.exists())
			targetFile.delete();
		
		if (encoding != null)
		{
			OutputStreamWriter fis = new OutputStreamWriter(new FileOutputStream(targetFile), encoding);
			Result result = new StreamResult(fis);					
			xsltTransformer.transform(source,result);
			fis.close();
		}
		else
		{
			Result result = new StreamResult(new FileOutputStream(targetFile));
			xsltTransformer.transform(source,result);		
		}
		return targetFile;
		
	}


	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#readRecord(com.novell.nds.dirxml.driver.Trace)
	 */
	public Map<String, String> readRecord() throws ReadException {
		try{
			return queue.getNextRecord();
		}catch (Exception e) {
			throw new ReadException(e);
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#close()
	 */
	public void close() throws ReadException{
		queue = null;
		if (parsingThread.isAlive()){
			trace.trace("WARN: parsing thread is still alive...", TraceLevel.ERROR_WARN);
			try {
				parsingThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//Thread is dead. Normal situation.
		parsingThread = null;
		handler = null;
	}

	public String[] getActualSchema() {
		return handler.getCurrentSchema();
	}
}
