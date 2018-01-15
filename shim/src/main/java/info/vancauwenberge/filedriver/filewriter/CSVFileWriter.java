/*******************************************************************************
 * Copyright (c) 2007-2017 Stefaan Van Cauwenberge
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

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.IFileWriteStrategy;
import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

public class CSVFileWriter extends AbstractStrategy implements IFileWriteStrategy {
	/*
	 * Enum holding the general subscriber parameters
	 */

	protected enum Parameters implements IStrategyParameters{
		/**
		 * Should a header record be written
		 */
		CSV_FILE_WRITE_HEADER("csvWriter_WriteHeader","true",DataType.BOOLEAN),
		/**
		 * Should we always quote the fields
		 */
		CSV_FILE_QUOTE_ALWAYS("csvWriter_QuoteAlways","false",DataType.BOOLEAN),
		/**
		 * Seperator to use
		 */
		CSV_FILE_WRITE_SEPERATOR("csvWriter_Seperator",";",DataType.STRING),
		/**
		 * The forced file encoding
		 */
		CSV_FILE_WRITE_ENCODING("csvWriter_ForcedEncoding","UTF-8",DataType.STRING),
		/**
		 * Should every line/record be flushed, or should we let the io system decide.
		 */
		CSV_FILE_WRITE_FLSUH("csvWriter_FlushMethod","false",DataType.BOOLEAN);


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
	private boolean quoteAlways;


	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	@Override
	public void init(final Trace trace, final Map<String,Parameter> driverParams, final IDriver driver)
			throws XDSParameterException {
		this.trace = trace;

		trace.trace("Initialization.", TraceLevel.TRACE);
		encoding = getStringValueFor(Parameters.CSV_FILE_WRITE_ENCODING,driverParams);//.get(CSV_FILE_WRITE_ENCODING).toString();
		writeHeader = getBoolValueFor(Parameters.CSV_FILE_WRITE_HEADER,driverParams);//.get(CSV_FILE_WRITE_HEADER).toBoolean().booleanValue();
		this.quoteAlways = getBoolValueFor(Parameters.CSV_FILE_QUOTE_ALWAYS, driverParams);
		flushEvryLine = getBoolValueFor(Parameters.CSV_FILE_WRITE_FLSUH,driverParams);//.get(CSV_FILE_WRITE_FLSUH).toBoolean().booleanValue();

		//Tabs and spaces in the driver config are removed by Designer, so we need to use a special 'encoding' for the tab character.
		final String strSeperator = getStringValueFor(Parameters.CSV_FILE_WRITE_SEPERATOR,driverParams);//.get(CSV_FILE_WRITE_SEPERATOR).toString();
		if ("{tab}".equalsIgnoreCase(strSeperator)){
			seperator='\t';
		}else if ("{space}".equalsIgnoreCase(strSeperator)){
			seperator=' ';
		}else if ((strSeperator != null) && !"".equals(strSeperator)){
			seperator = strSeperator.charAt(0);//This is a required field, so we should have at least one character
		} else {
			throw new XDSParameterException("Invalid parameter value for seperator:"+strSeperator);
		}

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
		trace.trace(" AlwaysQuote:"+quoteAlways, TraceLevel.TRACE);
	}


	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	@Override
	public void openFile(final File f) throws WriteException {
		trace.trace(" OpenFile "+f.getName(), TraceLevel.TRACE);
		try {
			final FileOutputStream fos = new FileOutputStream(f);
			final OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
			bos = new BufferedWriter(osw);
			theFile = f;
			if (writeHeader) {
				writeLine(schema);
			}
		} catch (final Exception e) {
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
	private String escapeCsv(final String input){
		if (input==null) {
			return "";
		}
		//if it contains a double quote: escape with double quote
		String temp = input.replaceAll("\"","\"\"");
		//if it contains a double quote, the seperator or newline: surround with double quote
		if (quoteAlways || (temp.indexOf('"')>=0) || (temp.indexOf('\n')>=0) || (temp.indexOf(seperator)>=0)) {
			temp = "\""+temp+"\"";
		}
		return temp;
	}

	private void writeLine(final String[] values) throws IOException{
		for (int i = 0; i < values.length; i++) {
			if (i>0) {
				bos.write(seperator);
			}
			final String string = values[i];
			bos.write(escapeCsv(string));
		}
		bos.newLine();
		if (flushEvryLine) {
			bos.flush();
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#close(com.novell.nds.dirxml.driver.Trace)
	 */
	@Override
	public File close() throws WriteException {
		trace.trace(" Closing file.", TraceLevel.TRACE);
		try {
			bos.close();
			return theFile;
		} catch (final IOException e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}finally{
			trace.trace(" Closing file completed.", TraceLevel.TRACE);			
		}
	}


	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#writeRecord(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	@Override
	public void writeRecord(final Map<String,String> m) throws WriteException {
		trace.trace(" Writing record.", TraceLevel.TRACE);
		final String[] values = new String[schema.length];
		for (int i = 0; i < schema.length; i++) {
			final String value = m.get(schema[i]);
			values[i]=value;
		}
		try {
			writeLine(values);
		} catch (final IOException e) {
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
