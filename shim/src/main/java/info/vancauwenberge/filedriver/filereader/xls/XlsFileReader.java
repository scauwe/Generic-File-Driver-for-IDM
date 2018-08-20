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
package info.vancauwenberge.filedriver.filereader.xls;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IFileReadStrategy;
import info.vancauwenberge.filedriver.exception.ReadException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

public class XlsFileReader extends AbstractStrategy implements IFileReadStrategy {
	private enum Parameters implements IStrategyParameters{
		/**
		 * Name of the tab to read
		 */
		XLS_FILE_READ_SCHEET_NAME{
			@Override
			public String getParameterName() {
				return "xlsReader_SheetName";
			}

			@Override
			public String getDefaultValue() {
				return "Sheet1";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}
		},
		/**
		 * Does the input contain a header row?
		 */
		XLS_FILE_READ_HAS_HEADER {
			@Override
			public String getParameterName() {
				return "xlsReader_HasHeader";
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
		 * Should we use the header names as defined in the xls, or should we stick to the driver schema
		 */
		XLS_USE_HEADER_NAMES{
			@Override
			public String getParameterName() {
				return "xlsReader_UseHeaderNames";
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
	private int nextRowNumber = 0;
	private HSSFWorkbook wb = null;
	private HSSFSheet currentSheet = null;
	private String sheetName;
	private boolean hasHeader;
	private String[] schema;
	//private File targetFile;
	private boolean useHeaderNames;
	private Trace trace;

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReadStrategy#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void init(Trace trace, Map<String,Parameter> driverParams, IPublisher publisher)
			throws XDSParameterException {
		this.trace = trace;

		sheetName = getStringValueFor(Parameters.XLS_FILE_READ_SCHEET_NAME, driverParams);
		//driverParams.get(XLS_FILE_READ_SCHEET_NAME).toString();
		hasHeader = getBoolValueFor(Parameters.XLS_FILE_READ_HAS_HEADER, driverParams);
		//driverParams.get(XLS_FILE_READ_HAS_HEADER).toBoolean().booleanValue();
		useHeaderNames = getBoolValueFor(Parameters.XLS_USE_HEADER_NAMES,driverParams);
		//driverParams.get(XLS_USE_HEADER_NAMES).toBoolean().booleanValue();
   		schema = GenericFileDriverShim.getSchemaAsArray(driverParams);
	}
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReadStrategy#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	public void openFile(File f) throws ReadException {
		trace.trace("Reading "+ f.getAbsolutePath());
		//If the file is existing, open and read it
		try {
			FileInputStream fin = new FileInputStream(f);
			POIFSFileSystem poifs = new POIFSFileSystem(fin);
			wb = new HSSFWorkbook(poifs);
		} catch (IOException e) {
			throw new ReadException("Error while trying to read file "+f.getAbsolutePath(), e);
		}
		currentSheet = wb.getSheet(sheetName);
		//If we do not have a sheet with the given name, throw exception.
		if (currentSheet == null)
			throw new ReadException("No sheet with name "+sheetName+" found in file "+f.getAbsolutePath(), null);
		nextRowNumber = currentSheet.getFirstRowNum();
		
		//If we have a aheader row, read it to get the actual schema
		if (hasHeader){
			if (useHeaderNames){
				HSSFRow row = currentSheet.getRow(nextRowNumber);
				//Last cell num is zero based => +1
				String[] fields = new String[row.getLastCellNum()];
				trace.trace("Number of fields:"+fields.length);
				Iterator<Cell> iter = row.cellIterator();
				while (iter.hasNext()) {
					HSSFCell element = (HSSFCell) iter.next();
					String value = element.getStringCellValue();
					fields[element.getCellNum()]=value;
				}
				//We might have some nulls in the array. Default them.
				for (int i = 0; i < fields.length; i++) {
					String string = fields[i];
					if (string==null)
						fields[i]="_Unknown_"+i+"_";
				}
				schema = fields;
				nextRowNumber++;
			}
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReadStrategy#readRecord(com.novell.nds.dirxml.driver.Trace)
	 */
	public Map<String,String> readRecord() throws ReadException {
		HSSFRow row = currentSheet.getRow(nextRowNumber);
		if (row!=null){
			//We have data.
			Map<String,String> result = new HashMap<String,String>(schema.length);
			//Do not use the iterator (row.cellIterator()): this will cause to skip empty cells!
			//Use the schema to loop over the cells
			for (short i = 0; i < schema.length; i++) {
				String fieldName = schema[i];
				HSSFCell cel = row.getCell(i);
				if (cel != null){
					String value="";
					if (cel.getCellType()==HSSFCell.CELL_TYPE_NUMERIC){
//						TODO: make this configurable: conversion from double to string
						value = cel.getNumericCellValue()+"";
					}else{
						value = cel.getStringCellValue();
					}
					result.put(fieldName, value);					
				}
				else
				{
					result.put(fieldName, "");
				}
			}			
			nextRowNumber++;
			return result;
		}
		else
		{
			return null;
		}
	}
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReadStrategy#close(com.novell.nds.dirxml.driver.Trace)
	 */
	public void close() throws ReadException {
		wb = null;
		currentSheet = null;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}
	public String[] getActualSchema() {
		return schema;
	}
}
