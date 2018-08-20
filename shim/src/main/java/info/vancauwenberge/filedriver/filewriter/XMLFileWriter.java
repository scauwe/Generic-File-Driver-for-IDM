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
package info.vancauwenberge.filedriver.filewriter;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.IFileWriteStrategy;
import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
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

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

public class XMLFileWriter extends AbstractStrategy implements IFileWriteStrategy {
	enum Parameters implements IStrategyParameters{
		/**
		 * NAme of the root elelemt
		 */
		XML_FILE_WRITE_ROOT_ELEMENT {
			@Override
			public String getParameterName() {
				return "xmlWriter_RootName";
			}

			@Override
			public String getDefaultValue() {
				return  "root";
			}
		},
		/**
		 * Name of the record element
		 */
		XML_FILE_WRITE_RECORD_ELEMENT {
			@Override
			public String getParameterName() {
				return "xmlWriter_RecordName";
			}

			@Override
			public String getDefaultValue() {
				return "record";
			}
		},
		/**
		 * The forced file encoding
		 */
		XML_FILE_WRITE_ENCODING {
			@Override
			public String getParameterName() {
				return "xmlWriter_ForcedEncoding";
			}

			@Override
			public String getDefaultValue() {
				return Charset.defaultCharset().name();
			}
		},

		/**
		 * Post xsl processing
		 */
		XML_FILE_WRITE_XSL {
			@Override
			public String getParameterName() {
				return "xmlWriter_PostXSL";
			}

			@Override
			public String getDefaultValue() {
				return "";
			}
		};
		public abstract String getParameterName();

		public abstract String getDefaultValue();

		public DataType getDataType(){
			return DataType.STRING;
		}

		public Constraint[] getConstraints() {
			return null;
		}

	}
	
	private String encoding;
	private String rootElement;
	private String recordElement;
	private String xslt;
	private String[] schema;
	private BufferedWriter bos;
	private File theFile;
	
	private static final String[][] replaceString =
	{
        {
            "&amp;", "&"
        }, {
            "&quot;", "\""
        }, {
            "&lt;", "<"
        }, {
            "&gt;", ">"
        },{
        	"&apos;", "'"
        }
	};
	private Transformer xsltTransformer;
	private Trace trace;

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void init(Trace trace, Map<String,Parameter> driverParams, IDriver driver)
			throws XDSParameterException {
		this.trace = trace;

   		trace.trace(" Initialization.", TraceLevel.TRACE);
		encoding = getStringValueFor(Parameters.XML_FILE_WRITE_ENCODING, driverParams);
		//driverParams.get(XML_FILE_WRITE_ENCODING).toString();
		//Test to see that we have the given encoding
		try{
			Charset.forName(encoding);
		}catch(Exception e){
			throw new XDSParameterException("Encoding not supported:"+e.getClass().getName()+"-"+e.getMessage());
		}
		rootElement = getStringValueFor(Parameters.XML_FILE_WRITE_ROOT_ELEMENT, driverParams);
		//driverParams.get(XML_FILE_WRITE_ROOT_ELEMENT).toString();
		recordElement = getStringValueFor(Parameters.XML_FILE_WRITE_RECORD_ELEMENT, driverParams);
		//driverParams.get(XML_FILE_WRITE_RECORD_ELEMENT).toString();
		xslt = getStringValueFor(Parameters.XML_FILE_WRITE_XSL, driverParams);
		//driverParams.get(XML_FILE_WRITE_XSL).toString();
   		if ("".equals(encoding) || (encoding==null)){
   			encoding=Util.getSystemDefaultEncoding();
   			trace.trace("No encoding given. Using system default of "+encoding, TraceLevel.ERROR_WARN);
   		}
   		schema = GenericFileDriverShim.getSchemaAsArray(driverParams);
   		if (!"".equals(xslt)) //We need to apply an xslt prior to processing the file
   		{
   	        // construct a transformer using the generic stylesheet
   	        TransformerFactory factory = TransformerFactory.newInstance();
   	        StreamSource xslSource     = new StreamSource(new StringReader(xslt));
   	        try {
				xsltTransformer    = factory.newTransformer(xslSource);
				if (encoding != null)
					xsltTransformer.setOutputProperty( OutputKeys.ENCODING, encoding);
			} catch (TransformerConfigurationException e) {
				Util.printStackTrace(trace,e);
				throw new XDSParameterException("Error creating XSLT transformer:"+e.getMessage());
			}
   			
   		}
   		trace.trace(" Initialization completed", TraceLevel.TRACE);
   		trace.trace(" Encoding:"+encoding, TraceLevel.TRACE);
   		trace.trace(" RootElement:"+rootElement, TraceLevel.TRACE);
   		trace.trace(" RecordElement:"+recordElement, TraceLevel.TRACE);
   		trace.trace(" Xslt:"+xslt, TraceLevel.TRACE);
   		trace.trace(" Xslt:"+xslt, TraceLevel.TRACE);
   		trace.trace(" XsltTransformer:"+xsltTransformer, TraceLevel.TRACE);
	}
	
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	public void openFile(File f) throws WriteException {
		trace.trace(" Initializing new file:"+f.getName(), TraceLevel.TRACE);
		try {
			FileOutputStream fos = new FileOutputStream(f);
			OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
			bos = new BufferedWriter(osw);
			theFile = f;
			//write XML processing instructions:
			bos.write("<?xml version=\"1.0\" encoding=\""+encoding+"\"?>");
			bos.newLine();
			//write the root element
			bos.write("<");
			bos.write(rootElement);
			bos.write(">");
			bos.newLine();
		} catch (Exception e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}
		trace.trace(" Initializing new file completed.", TraceLevel.TRACE);
	}
	
	private String escapeXML(String input){
		String tmp = input;
		for (int i = 0; i < replaceString.length; i++) {
			String[] replacer = replaceString[i];
			tmp = tmp.replaceAll(replacer[1],replacer[0]);
		}
		return tmp;
	}
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#writeRecord(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void writeRecord(Map<String,String> m) throws WriteException {
		trace.trace(" Writing record to file.", TraceLevel.TRACE);
		try {
			//write the record element
			bos.write("<");
			bos.write(recordElement);
			bos.write(">");
			bos.newLine();
			//Write all fields
			for (int i = 0; i < schema.length; i++) {
				String fieldName = schema[i];
				String value = m.get(fieldName);
				if (value==null){
					writeEmptyValue(fieldName);
				}
				else
				{
					writeValue(fieldName, value);
				}
			}
			//write the record element
			bos.write("</");
			bos.write(recordElement);
			bos.write(">");
			bos.newLine();
		} catch (IOException e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}
		trace.trace(" Writing record to file completed.", TraceLevel.TRACE);
	}
	/**
	 * @param fieldName
	 * @param value
	 */
	private void writeValue(String fieldName, String value) throws IOException {
		bos.write("<");
		bos.write(fieldName);
		bos.write(">");
		bos.write(escapeXML(value));
		bos.write("</");
		bos.write(fieldName);
		bos.write(">");
		bos.newLine();
	}


	/**
	 * @param fieldName
	 */
	private void writeEmptyValue(String fieldName) throws IOException {
		bos.write("<");
		bos.write(fieldName);
		bos.write("/>");
		bos.newLine();
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
		InputStreamReader isr = new InputStreamReader(new FileInputStream(initialFile), encoding);
		source = new StreamSource(isr);

		 
		File targetFile = new File(initialFile.getParent(), initialFile.getName()+".transformed");
		Result result;
		OutputStreamWriter fis = new OutputStreamWriter(new FileOutputStream(targetFile), encoding);
		result = new StreamResult(fis);					
		
		xsltTransformer.transform(source,result);
		fis.close();
		return targetFile;
	}


	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#close(com.novell.nds.dirxml.driver.Trace)
	 */
	public File close() throws WriteException {
		trace.trace(" Closing file.", TraceLevel.TRACE);
		try {
			//write the root element
			bos.write("</");
			bos.write(rootElement);
			bos.write(">");
			bos.newLine();
			bos.close();
			//If we have an xslt: convert.
			if ("".equals(xslt) || (xslt==null))
				return theFile;
			try {
				return transformFile(theFile);
			} catch (TransformerException e1) {
				Util.printStackTrace(trace,e1);
				throw new WriteException(e1);
			}
		} catch (IOException e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}finally{
			trace.trace(" Closing file completed.", TraceLevel.TRACE);
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}
}
