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
package info.vancauwenberge.filedriver.filereader.csv;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import info.vancauwenberge.filedriver.filereader.RecordQueue;

public class CSVFileParser {
	private static final char TOKEN_COMMENT = '#';
	private static final int LINE_BEGIN = 0;
	private static final int LINE_NORMAL = 1;
	private static final int LINE_COMMENT = 2;

	private static final int FIELD_BEGIN = 0;
	private static final int FIELD_QUOTED = 1;
	private static final int FIELD_NORMAL = 2;

	private static final int CHAR_NORMAL = 0;
	private static final int CHAR_ESCAPE = 1;

	//fields
	private boolean fileHeaderprocessed=false;
	private final boolean loadNodeNamesFromHeader;
	private final boolean skipEmptyLines;
	private int fieldStat = FIELD_BEGIN;
	private int charStat = CHAR_NORMAL;
	private int lineStat = LINE_BEGIN;
	private HashMap<String,String> thisRecord = new HashMap<String,String>();
	private int fieldIndex = 0;
	private char token_seperator = ';';
	private String[] currentRecordFields;
	//private String dataRecordTag;
	private String[] dataRecordFields;
	//private String headerRecordTag;
	private final String[] headerRecordFields;

	private RecordQueue queue;

	//caching fields
	//private String currentRecordTag;
	private final boolean hasHeader;

	public void resetParser(){
		synchronized (dataRecordFields) {
			if (hasHeader) {
				fileHeaderprocessed=false;
			} else {
				fileHeaderprocessed=true;
			}
			dataRecordFields.notifyAll();
		}
		fieldStat = FIELD_BEGIN;
		charStat = CHAR_NORMAL;
		lineStat = LINE_BEGIN;
		thisRecord = new HashMap<String,String>();
		fieldIndex = 0;
		queue = new RecordQueue();
		updateCurrentRecordMetaData();
	}
	/**
	 * Do the actual parsing of a char array to an xml doc.
	 * @param _aCSV
	 * @return
	 * @throws java.text.ParseException
	 */
	public void doParse(final Reader stream) throws java.text.ParseException, IOException{
		final StringBuffer aPart = new StringBuffer();
		int currentCharAsInt;
		while((currentCharAsInt = stream.read()) != -1){
			final char currentChar = (char) currentCharAsInt;
			if (lineStat == LINE_BEGIN) {
				//Start a new node
				if (currentChar == TOKEN_COMMENT) {
					lineStat = LINE_COMMENT;
				} else {
					fieldIndex = 0;
					lineStat = doLineNormal(currentChar,aPart);
				}
			} else if (lineStat == LINE_NORMAL) {
				lineStat = doLineNormal(currentChar,aPart);
			} else if (lineStat == LINE_COMMENT) {
				//comment line: do nothing, unless newline
				if (currentChar == '\n') {
					saveCommentField(aPart);
				} else {
					aPart.append(currentChar);
				}
			}
		}

		//if the file does not finish with a newline, save the last field and record
		if (lineStat == LINE_NORMAL){
			saveField(aPart);
			queue.addRecord(thisRecord);
		}
		queue.setFinished();
	}

	/**
	 * Get the name of the field with the given index 
	 * @param index
	 * @return
	 */
	private String getFieldName(final int index) {
		if (index >= currentRecordFields.length) {
			return ((fileHeaderprocessed)?"field":"header") + index;
		}
		return currentRecordFields[index];
	}

	/**
	 * @param aPart
	 */
	private void saveCommentField(final StringBuffer aPart) {
		//Comments are not part of record
		/*		Comment comment = doc.createComment(aPart.toString());
		doc.getFirstChild().appendChild(comment);*/
		aPart.setLength(0);
		lineStat = LINE_BEGIN;
	}

	/**
	 * @param value
	 * @param fieldIndex2
	 */
	private void saveHeaderName(final String value, final int fieldIndex) {
		if (dataRecordFields.length<=fieldIndex){
			//increament the array
			final String[] newArray = new String[fieldIndex+1];
			System.arraycopy(dataRecordFields,0,newArray,0,fieldIndex);
			dataRecordFields = newArray;
		}

		//TODO: make sure it is a valid name
		dataRecordFields[fieldIndex] = value;
	}


	/**
	 * Add the current buffer as a field in the xml document
	 */
	private void saveField(final StringBuffer aPart) {
		final String value = aPart.toString();
		thisRecord.put(getFieldName(fieldIndex), value);
		//If we need to load the tags from the header, save it as tag name if we are still processing the header
		if (loadNodeNamesFromHeader & !fileHeaderprocessed){
			saveHeaderName(value, fieldIndex);
		}
		aPart.setLength(0);
		fieldIndex++;
		fieldStat = FIELD_BEGIN;
		charStat = CHAR_NORMAL;
	}

	private void updateCurrentRecordMetaData(){
		if (fileHeaderprocessed){
			//currentRecordTag = dataRecordTag;
			currentRecordFields = dataRecordFields;
		}else{
			//currentRecordTag = headerRecordTag;
			currentRecordFields = headerRecordFields;
		}		
	}


	public String[] getCurrentSchema(){
		synchronized (dataRecordFields) {
			if (!fileHeaderprocessed) {
				try {
					dataRecordFields.wait();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return dataRecordFields;
	}

	/**
	 * @param c
	 */
	private int doLineNormal(final char c,final StringBuffer aPart) {
		//We process a non-comment line
		if (c == token_seperator) {//seperator
			if ((fieldStat == FIELD_NORMAL) || (fieldStat == FIELD_BEGIN)
					|| (charStat == CHAR_ESCAPE)) { //This ends the field
				saveField(aPart);
				return LINE_NORMAL;
			}
		} else if (c == '"') {//quote
			if (fieldStat == FIELD_BEGIN) {
				fieldStat = FIELD_QUOTED;//skip this character
				return LINE_NORMAL;
			} else if (charStat == CHAR_NORMAL) {
				charStat = CHAR_ESCAPE;
				return LINE_NORMAL;
			}
		} else if (c == '\n') {//DOS & UNIX newline
			if ((fieldStat != FIELD_QUOTED)
					|| ((fieldStat == FIELD_QUOTED) && (charStat == CHAR_ESCAPE))) {
				saveField(aPart);
				//Save the current record
				final boolean toAdd = currentRecordNeedsAdded();
				if (toAdd){
					if (!fileHeaderprocessed){
						synchronized (dataRecordFields) {
							fileHeaderprocessed = true;
							dataRecordFields.notifyAll();
						}
						updateCurrentRecordMetaData();
					}else{
						queue.addRecord(thisRecord);
					}
					//Start a new record
					thisRecord = new HashMap<String,String>();
				}
				return LINE_BEGIN;
			}
		} else if (c == '\r') {//DOS return: ignore it
			return LINE_NORMAL;
		}
		aPart.append(c);
		charStat = CHAR_NORMAL;
		return LINE_NORMAL;
	}

	/**
	 * Should the current record be added as a data record o the queue?
	 * returns true if the record contains data or when skipEmptyLines is false.
	 * return false when skipEmptyLines is true and the record does not contain data.
	 * @return
	 */
	private boolean currentRecordNeedsAdded() {
		boolean hasData=true;
		if (skipEmptyLines) {
			if (thisRecord.size()==1) {
				if ("".equals(thisRecord.values().iterator().next())) {
					hasData=false;
				}
			}
		}
		return hasData;
	}

	public CSVFileParser(final char seperator, final String[] dataRecordFields, final boolean skipEmptyLines, final boolean hasHeader, final boolean loadNodeNamesFromHeader){
		this.token_seperator=seperator;
		this.dataRecordFields = dataRecordFields;
		this.headerRecordFields = new String[]{};
		this.skipEmptyLines=skipEmptyLines;
		this.hasHeader = hasHeader;
		this.loadNodeNamesFromHeader = loadNodeNamesFromHeader;
	}

	public static void main(final String[] args)
	{
		final boolean skipEmptyLines=true;
		final boolean hasHeader=true;
		final boolean loadNodeNamesFromHeader=true;
		final CSVFileParser fr = new CSVFileParser(
				',',
				new String[]{"customField1","customField2"},
				skipEmptyLines,
				hasHeader,
				loadNodeNamesFromHeader);

		try {
			//File f = new File();
			StringReader sr = new StringReader("aCSVHeader1,aCSVHeader2\n\nvalue1.1,value1.2\n\n\n\nvalue2.1,value2.2\n\n\n\n\n\n\n");
			fr.resetParser();
			fr.doParse(sr);
			fr.queue.setFinished();
			Map<String,String> result;
			while ((result =fr.queue.getNextRecord()) !=null) {
				System.out.println(result);
			}
			System.out.println();
			sr = new StringReader("secondHeader1,secondHeader2\n\nsecondvalue1.1,secondvalue1.2\n\n\n\nsecondvalue2.1,secondvalue2.2\n\n\n\n\n\n\n");
			fr.resetParser();
			fr.doParse(sr);
			fr.queue.setFinished();
			while ((result =fr.queue.getNextRecord()) !=null) {
				System.out.println(result);
			}
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @return Returns the queue.
	 */
	public RecordQueue getQueue() {
		return queue;
	}
}
