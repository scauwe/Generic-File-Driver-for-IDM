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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

public class CSVFileWriter extends AbstractStrategy implements IFileWriteStrategy {
	/*
     * Enum holding the general subscriber parameters
     */

	private enum Parameters implements IStrategyParameters{
    	/**
    	 * Should a header record be written
    	 */
    	CSV_FILE_WRITE_HEADER{
			@Override
			public String getParameterName() {
				return "csvWriter_WriteHeader";
			}

			@Override
			public String getDefaultValue() {
				return "true";
			}

			@Override
			public DataType getDataType() {
				return DataType.BOOLEAN;
			}
		},
    	/**
    	 * Seperator to use
    	 */
    	CSV_FILE_WRITE_SEPERATOR{
			@Override
			public String getParameterName() {
				return "csvWriter_Seperator";
			}

			@Override
			public String getDefaultValue() {
				return ";";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}
		},

    	/**
    	 * The forced file encoding
    	 */
    	CSV_FILE_WRITE_ENCODING{
			@Override
			public String getParameterName() {
				return "csvWriter_ForcedEncoding";
			}

			@Override
			public String getDefaultValue() {
				return "UTF-8";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}
		},

    	/**
    	 * Should every line/record be flushed, or should we let the io system decide.
    	 */
    	CSV_FILE_WRITE_FLSUH{
			@Override
			public String getParameterName() {
				return "csvWriter_FlushMethod";
			}

			@Override
			public String getDefaultValue() {
				return "false";
			}

			@Override
			public DataType getDataType() {
				return DataType.BOOLEAN;
			}
		};



		public abstract String getParameterName();

		public abstract String getDefaultValue();

		public abstract DataType getDataType();

		public Constraint[] getConstraints(){
			return null;
		}
    }

	
	private boolean writeHeader = false;
	private boolean flushEvryLine = false;
	private char seperator =';';
	private String encoding=null;
	private BufferedWriter bos;
	private File theFile;
	private String[] schema;
	private Trace trace;
	
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void init(Trace trace, Map<String,Parameter> driverParams, IDriver driver)
			throws XDSParameterException {
		this.trace = trace;

		trace.trace("Initialization.", TraceLevel.TRACE);
		encoding = getStringValueFor(Parameters.CSV_FILE_WRITE_ENCODING,driverParams);//.get(CSV_FILE_WRITE_ENCODING).toString();
		writeHeader = getBoolValueFor(Parameters.CSV_FILE_WRITE_HEADER,driverParams);//.get(CSV_FILE_WRITE_HEADER).toBoolean().booleanValue();
		
		flushEvryLine = getBoolValueFor(Parameters.CSV_FILE_WRITE_FLSUH,driverParams);//.get(CSV_FILE_WRITE_FLSUH).toBoolean().booleanValue();
		
   		//Tabs and spaces in the driver config are removed by Designer, so we need to use a special 'encoding' for the tab character.
		String strSeperator = getStringValueFor(Parameters.CSV_FILE_WRITE_SEPERATOR,driverParams);//.get(CSV_FILE_WRITE_SEPERATOR).toString();
		if ("{tab}".equalsIgnoreCase(strSeperator)){
			seperator='\t';
		}else if ("{space}".equalsIgnoreCase(strSeperator)){
			seperator=' ';
		}else if (strSeperator != null && !"".equals(strSeperator)){
	   		seperator = strSeperator.charAt(0);//This is a required field, so we should have at least one character
		}else
			throw new XDSParameterException("Invalid parameter value for seperator:"+strSeperator);
		
   		if ("".equals(encoding) || (encoding==null)){
   			encoding=Util.getSystemDefaultEncoding();
   			trace.trace("No encoding given. Using system default of "+encoding, TraceLevel.ERROR_WARN);
   		}
   		schema = GenericFileDriverShim.getSchemaAsArray(driverParams);
		trace.trace("Initialization completed:", TraceLevel.TRACE);
		trace.trace(" Encoding:"+encoding, TraceLevel.TRACE);
		trace.trace(" WriteHeader:"+writeHeader, TraceLevel.TRACE);
		trace.trace(" FlushEvryLine:"+flushEvryLine, TraceLevel.TRACE);
		trace.trace(" Seperator:"+seperator, TraceLevel.TRACE);
	}
	
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	public void openFile(File f) throws WriteException {
		trace.trace(" OpenFile "+f.getName(), TraceLevel.TRACE);
		try {
			FileOutputStream fos = new FileOutputStream(f);
			OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
			bos = new BufferedWriter(osw);
			theFile = f;
			if (writeHeader)
				writeLine(schema);
		} catch (Exception e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}
		trace.trace(" OpenFile completed.", TraceLevel.TRACE);
	}

	/**
	 * Escape the value in a csv file. Assumed seperator is ;
	 * @param input
	 * @return
	 */
	private String escapeCsv(String input){
		if (input==null)
			return "";
		//if it contains a double quote: escape with double quote
		String temp = input.replaceAll("\"","\"\"");
		//if it contains a double quote, the seperator or newline: surround with double quote
		if (temp.indexOf('"')>=0 | temp.indexOf('\n')>=0 | temp.indexOf(seperator)>=0)
			temp = "\""+temp+"\"";
		return temp;
	}

	private void writeLine(String[] values) throws IOException{
		for (int i = 0; i < values.length; i++) {
			if (i>0)
				bos.write(seperator);
			String string = values[i];
			bos.write(escapeCsv(string));
		}
		bos.newLine();
		if (flushEvryLine)
			bos.flush();
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#close(com.novell.nds.dirxml.driver.Trace)
	 */
	public File close() throws WriteException {
		trace.trace(" Closing file.", TraceLevel.TRACE);
		try {
			bos.close();
			return theFile;
		} catch (IOException e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}finally{
			trace.trace(" Closing file completed.", TraceLevel.TRACE);			
		}
	}


	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#writeRecord(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void writeRecord(Map<String,String> m) throws WriteException {
		trace.trace(" Writing record.", TraceLevel.TRACE);
		String[] values = new String[schema.length];
		for (int i = 0; i < schema.length; i++) {
			String value = (String) m.get(schema[i]);
			values[i]=value;
		}
		try {
			writeLine(values);
		} catch (IOException e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}
		trace.trace(" Writing record completed.", TraceLevel.TRACE);
	}


	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}
}
