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

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.IFileWriteStrategy;
import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;
import info.vancauwenberge.filedriver.util.TraceLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

public class XlsFileWriter extends AbstractStrategy implements IFileWriteStrategy {
	enum Parameters implements IStrategyParameters{
	/**
	 * Name of the tab
	 */
		XLS_FILE_WRITE_SCHEET_NAME {
			@Override
			public String getParameterName() {
				return "xlsWriter_SheetName";
			}

			@Override
			public String getDefaultValue() {
				return "Data";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}
		},
	/**
	 * Should we write a header row
	 */
	XLS_FILE_WRITE_ADD_HEADER {
		@Override
		public String getParameterName() {
			return "xlsWriter_AddHeader";
		}

		@Override
		public String getDefaultValue() {
			return "true";
		}

		@Override
		public DataType getDataType() {
			return DataType.BOOLEAN;
		}
	};
	
	
	public abstract String getParameterName();

	public abstract String getDefaultValue();

	public abstract DataType getDataType();

	public Constraint[] getConstraints() {
		return null;
	}

	}
	
	private HSSFWorkbook wb = null;
	private HSSFSheet currentSheet = null;
	private String sheetName;
	private boolean addHeader;
	private String[] schema;
	private File targetFile;
	private Trace trace;
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#close(com.novell.nds.dirxml.driver.Trace)
	 */
	public File close() throws WriteException {
		trace.trace("Writing file "+targetFile.getAbsolutePath(), TraceLevel.DEBUG);
		// Write the output to a file
		try {
		    FileOutputStream fileOut = new FileOutputStream(targetFile);
			wb.write(fileOut);
		    fileOut.close();
		    return targetFile;
		} catch (IOException e) {
			throw new WriteException("Error while trying to write the xls file to file system.",e);
		}
	}
	
	

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void init(Trace trace, Map<String,Parameter> driverParams, IDriver driver)
			throws XDSParameterException {
		this.trace = trace;

		sheetName = getStringValueFor(Parameters.XLS_FILE_WRITE_SCHEET_NAME, driverParams);
		//driverParams.get(XLS_FILE_WRITE_SCHEET_NAME).toString();
		addHeader = getBoolValueFor(Parameters.XLS_FILE_WRITE_ADD_HEADER, driverParams);
		//driverParams.get(XLS_FILE_WRITE_ADD_HEADER).toBoolean().booleanValue();
   		schema = GenericFileDriverShim.getSchemaAsArray(driverParams);
	}
	
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	public void openFile(File f) throws WriteException {
		targetFile = f;
		wb = new HSSFWorkbook();
		//Create a sheet with the given name.
		currentSheet = wb.createSheet(sheetName);
		
		//Write a header if needed
		if (addHeader){
			Map<String,String> headerMap = new HashMap<String,String>(schema.length);
		    for (int i = 0; i < schema.length; i++) {
				String fieldName = schema[i];
				headerMap.put(fieldName,fieldName);
		    }
		    writeRecord(headerMap);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#writeRecord(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void writeRecord(Map<String,String> m) throws WriteException {
	    if (schema.length>Short.MAX_VALUE){
	    	throw new WriteException("XLS files cannot contain more then "+Short.MAX_VALUE+" values.", null);
	    }

		//Note: the xls is written to memory, NOT to disk
		
		//Create a new row
		int rowNum = 0;
		//If we do not have any rows yet, take position 0 (default), else take last +1.
		if (currentSheet.getPhysicalNumberOfRows() != 0)
			rowNum = currentSheet.getLastRowNum() + 1;
		
		trace.trace("Adding datarow at position "+ rowNum,TraceLevel.DEBUG);
		
	    HSSFRow row = currentSheet.createRow((short)(rowNum));
	    for (short i = 0; i < schema.length; i++) {
			String fieldName = schema[i];
			String value = m.get(fieldName);
		    row.createCell(i).setCellValue(value);			
		}
	}



	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}
}
