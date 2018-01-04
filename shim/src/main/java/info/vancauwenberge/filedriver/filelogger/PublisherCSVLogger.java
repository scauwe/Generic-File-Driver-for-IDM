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
package info.vancauwenberge.filedriver.filelogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.ElementImpl;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.StatusLevel;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;
import com.novell.nds.dirxml.driver.xds.XDSStatusElement;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IPublisherLoggerStrategy;
import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

public class PublisherCSVLogger extends AbstractStrategy implements IPublisherLoggerStrategy{
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
				return "csvLogger_WriteHeader";
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
				return "csvLogger_Seperator";
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
				return "csvLogger_ForcedEncoding";
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
				return "csvLogger_FlushMethod";
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



		@Override
		public abstract String getParameterName();

		@Override
		public abstract String getDefaultValue();

		@Override
		public abstract DataType getDataType();

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
	//private String[] schema;
	private Trace trace;
	private EnumMap<LogField, Integer> logFieldPositionMap ;
	private String[] schema;

	@Override
	public void init(final Trace trace, final Map<String, Parameter> driverParams, final IPublisher publisher) throws Exception {
		this.trace = trace;

		trace.trace("Initialization.", TraceLevel.TRACE);
		encoding = getStringValueFor(Parameters.CSV_FILE_WRITE_ENCODING,driverParams);//.get(CSV_FILE_WRITE_ENCODING).toString();
		writeHeader = getBoolValueFor(Parameters.CSV_FILE_WRITE_HEADER,driverParams);//.get(CSV_FILE_WRITE_HEADER).toBoolean().booleanValue();

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
		//schema = GenericFileDriverShim.getSchemaAsArray(driverParams);
		trace.trace("Initialization completed:", TraceLevel.TRACE);
		trace.trace(" Encoding:"+encoding, TraceLevel.TRACE);
		trace.trace(" WriteHeader:"+writeHeader, TraceLevel.TRACE);
		trace.trace(" FlushEvryLine:"+flushEvryLine, TraceLevel.TRACE);
		trace.trace(" Seperator:"+seperator, TraceLevel.TRACE);
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
		if ((temp.indexOf('"')>=0) | (temp.indexOf('\n')>=0) | (temp.indexOf(seperator)>=0)) {
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


	@Override
	public void openFile(final File f, final String[] schema, final EnumMap<LogField, String> logFieldSchemaMap) throws WriteException {
		trace.trace(" Open LogFile "+f.getName(), TraceLevel.TRACE);
		trace.trace(" schema map"+logFieldSchemaMap, TraceLevel.TRACE);
		this.schema = schema;
		this.logFieldPositionMap = new EnumMap<IPublisherLoggerStrategy.LogField, Integer>(IPublisherLoggerStrategy.LogField.class);
		try {
			final FileOutputStream fos = new FileOutputStream(f);
			final OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
			bos = new BufferedWriter(osw);
			theFile = f;
			//Append the log schema fields at the end
			final String[] extendedSchema = new String[schema.length+logFieldSchemaMap.size()];
			System.arraycopy(schema, 0, extendedSchema, 0, schema.length);
			final Set<LogField> keys = logFieldSchemaMap.keySet();
			int index = schema.length;
			for (LogField logField : keys) {
				extendedSchema[index] = logFieldSchemaMap.get(logField);
				logFieldPositionMap.put(logField, index);
				trace.trace("Adding "+logField+" at position "+index, TraceLevel.TRACE);
				index++;
			}
			if (writeHeader){
				writeLine(extendedSchema);
			}
		} catch (final Exception e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}
		trace.trace(" OpenFile completed.", TraceLevel.TRACE);
		trace.trace("  Logfields:" + logFieldPositionMap, TraceLevel.TRACE);
	}

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

	@Override
	public void logCommand(final int recordNumber, final Map<String,String> thisRecord, final XDSStatusElement xdsStatusElement) throws WriteException {
		String[] values = new String[schema.length + logFieldPositionMap.size()];
		for (int i = 0; i < schema.length; i++) {
			final String fieldName = schema[i];
			values[i] = thisRecord.get(fieldName);
		}

		//If we have a statusElement, log the details as well
		if (xdsStatusElement != null){
			values = appendStatusFields(recordNumber, xdsStatusElement, values);
		}
		try {
			writeLine(values);
			if (flushEvryLine) {
				bos.flush();
			}
		} catch (final IOException e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}
	}


	@SuppressWarnings("rawtypes")
	private String[] appendStatusFields(final int recordNumber, final XDSStatusElement xdsStatusElement, final String[] values) {
		final List children = xdsStatusElement.childElements();
		trace.trace("status children: "+children);
		if (children != null){
			for (final Object object : children) {
				if (object instanceof ElementImpl){
					final ElementImpl elem = (ElementImpl)object;
					trace.trace("status child: "+elem.localName());
					trace.trace("status child: "+elem.tagName());
				}
			}
		}

		final Set<LogField> schemaPositions = logFieldPositionMap.keySet();
		for (LogField logField : schemaPositions) {
			switch (logField) {
			case LOGMESSAGE:
				values[logFieldPositionMap.get(logField)] = xdsStatusElement.extractText();
				break;
			case LOGSTATUS:
				final StatusLevel level = xdsStatusElement.getLevel();
				if (level==null) {
					values[logFieldPositionMap.get(logField)] = "";
				} else {
					values[logFieldPositionMap.get(logField)] = level.toString();
				}
				break;
			case RECORDNUMBER:
				values[logFieldPositionMap.get(logField)] = Integer.toString(recordNumber);
				break;
			case LOGEVENTID:
				values[logFieldPositionMap.get(logField)] = xdsStatusElement.getEventID();
				break;				
			}
		}
		return values;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}
}
