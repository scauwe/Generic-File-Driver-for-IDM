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
package info.vancauwenberge.filedriver.filereader.csv;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IFileReadStrategy;
import info.vancauwenberge.filedriver.exception.ReadException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.filereader.RecordQueue;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

public class CSVFileReader extends AbstractStrategy implements IFileReadStrategy {
	private boolean useHeaderNames;
	private char seperator;
	private String encoding;
	private boolean hasHeader;
	private boolean skipEmptyLines;
	private Thread parsingThread;
	private RecordQueue queue;
	private String[] schema;
	private Trace trace;
	private CSVFileParser handler;

	private enum Parameters implements IStrategyParameters{
		SKIP_EMPTY_LINES {
			@Override
			public String getParameterName() {
				return "csvReader_skipEmptyLines";
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
		USE_HEADER_NAMES {
			@Override
			public String getParameterName() {
				return "csvReader_UseHeaderNames";
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
		HAS_HEADER {
			@Override
			public String getParameterName() {
				return "csvReader_hasHeader";
			}

			@Override
			public String getDefaultValue() {
				return "true";
			}

			@Override
			public DataType getDataType() {
				return DataType .BOOLEAN;
			}
		},
		FORCED_ENCODING {
			@Override
			public String getParameterName() {
				return "csvReader_forcedEncoding";
			}

			@Override
			public String getDefaultValue() {
				return null;
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}
		},
		SEPERATOR {
			@Override
			public String getParameterName() {
				return "csvReader_seperator";
			}

			@Override
			public String getDefaultValue() {
				return ",";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING ;
			}
		};

		public abstract String getParameterName();

		public abstract String getDefaultValue();

		public abstract DataType getDataType();

		public Constraint[] getConstraints() {
			return null;
		}
	}
	
	
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void init(Trace trace, Map<String,Parameter> driverParams, IPublisher publisher)
			throws XDSParameterException {
		if (trace.getTraceLevel()>TraceLevel.TRACE){
			trace.trace("CSVFileReader.init() driverParams:"+driverParams);
		}
		this.trace = trace;

		useHeaderNames = getBoolValueFor(Parameters.USE_HEADER_NAMES,driverParams);
		//driverParams.get(TAG_USE_HEADER_NAMES).toBoolean().booleanValue();
		encoding = getStringValueFor(Parameters.FORCED_ENCODING,driverParams);
		//driverParams.get(TAG_FORCED_ENCODING).toString();
		hasHeader = getBoolValueFor(Parameters.HAS_HEADER,driverParams);
		//driverParams.get(TAG_HAS_HEADER).toBoolean().booleanValue();
		skipEmptyLines = getBoolValueFor(Parameters.SKIP_EMPTY_LINES,driverParams);
		//driverParams.get(TAG_SKIP_EMPTY_LINES).toBoolean().booleanValue();
   		if ("".equals(encoding)){
   			encoding=Util.getSystemDefaultEncoding();
   			trace.trace("No encoding given. Using system default of "+encoding, TraceLevel.ERROR_WARN);
   		}
   		//Tabs and spaces in the driver config are removed by Designer, so we need to use a special 'encoding' for the tab character.
		String strSeperator = getStringValueFor(Parameters.SEPERATOR,driverParams);
		//driverParams.get(TAG_SEPERATOR).toString();
		if ("{tab}".equalsIgnoreCase(strSeperator)){
			seperator='\t';
		}else if ("{space}".equalsIgnoreCase(strSeperator)){
			seperator=' ';
		}else if (strSeperator != null && !"".equals(strSeperator)){
	   		seperator = strSeperator.charAt(0);//This is a required field, so we should have at least one character
		}else
			throw new XDSParameterException("Invalid parameter value for seperator:"+strSeperator);

   		schema = GenericFileDriverShim.getSchemaAsArray(driverParams);
	}
	
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	public void openFile(File f) throws ReadException {
		try {
			// Start the parser that will parse this document
			FileInputStream fr = new FileInputStream(f);
			final Reader reader = new UnicodeReader(fr, encoding);
			handler = new CSVFileParser(seperator,schema,skipEmptyLines, hasHeader, useHeaderNames);
			handler.resetParser();
			queue = handler.getQueue();
			
			parsingThread = new Thread(){
				public void run(){
					try {
						handler.doParse(reader);
					} catch (Exception e) {
						Util.printStackTrace(trace, e);
						queue.setFinishedInError(e);
					}
				}
			};
			
			parsingThread.setName("CSVParser");
			parsingThread.start();
		} catch (Exception e1) {
			trace.trace("Exception while handeling CSV document:" +e1.getMessage(), TraceLevel.ERROR_WARN);
			throw new ReadException("Exception while handeling CSV document:" +e1.getMessage(),e1);
		}
	}
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#readRecord(com.novell.nds.dirxml.driver.Trace)
	 */
	public Map<String,String> readRecord() throws ReadException {
		try{
			return queue.getNextRecord();
		}catch (Exception e) {
			throw new ReadException(e);
		}
	}
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#close()
	 */
	public void close() throws ReadException {
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



	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}


	public String[] getActualSchema() {
		//The parsing is done in a seperate thread. We need to make sure that at least one record is read.
		return handler.getCurrentSchema();
	}
}
