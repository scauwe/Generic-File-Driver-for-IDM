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
package info.vancauwenberge.filedriver.util;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MetaDataManager {
	//Meta data field names
	private static final String META_RECORDNUMBER = "recordNumber";
	private static final String META_ISLASTRECORD = "isLastRecord";
	private static final String META_FILEPATH = "filePath";
	private static final String META_FILENAME = "fileName";
	private static final String META_FILESIZE = "fileSize";

	//Evaluated meta data flags
	private boolean requiresFileSize;
	private boolean requiresFileName;
	private boolean requiresFilePath;
	private boolean requiresLastRecordFlag;
	private boolean requiresRecordNumber;
	
	public MetaDataManager(String paramMetaData){
		List<String> metaData = Arrays.asList(paramMetaData.split(","));
		requiresFileSize= metaData.contains(META_FILESIZE);
		requiresFileName = metaData.contains(META_FILENAME);
		requiresFilePath = metaData.contains(META_FILEPATH);
		requiresLastRecordFlag = metaData.contains(META_ISLASTRECORD);
		requiresRecordNumber = metaData.contains(META_RECORDNUMBER);
	}
	
	/**
     * Put the initial meta data of a given file in a map and return the map.
	 * @param workFile
	 * @return map with meta data.
	 */
	public Map<String,String> getStaticMetaData(File workFile) {
		Map<String,String> metaDataMap = new HashMap<String,String>();
		if (requiresFileSize)
			metaDataMap.put(META_FILESIZE, workFile.length()+"");
		if (requiresFileName)
			metaDataMap.put(META_FILENAME, workFile.getName());
		if (requiresFilePath)
			metaDataMap.put(META_FILEPATH, workFile.getPath());
		if (requiresLastRecordFlag)
			metaDataMap.put(META_ISLASTRECORD, "false");
		return metaDataMap;
	}
	
	/**
	 * Returns the schema field names that will be added to events generated
	 * @return
	 */
	public List<String> getSchemaFields(){
		List<String> fields = new LinkedList<String>();
		if (requiresFileSize)
			fields.add(META_FILESIZE);
		if (requiresFileName)
			fields.add(META_FILENAME);
		if (requiresFilePath)
			fields.add(META_FILEPATH);
		if (requiresLastRecordFlag)
			fields.add(META_ISLASTRECORD);
		if (requiresRecordNumber)
			fields.add(META_RECORDNUMBER);
		return fields;		
	}
	
	public void addDynamicMetaData(Map<String,String> metaDataMap, Map<String,String> thisRecord, Map<String,String> nextRecord, int recordNumber) {
		//Add the meta data if required
		thisRecord.putAll(metaDataMap);
		if (this.requiresRecordNumber)
			thisRecord.put(META_RECORDNUMBER, recordNumber+"");
		if (nextRecord == null)
			if (requiresLastRecordFlag)
				thisRecord.put(META_ISLASTRECORD, "true");
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MetaDataManager [requiresFileSize=")
				.append(requiresFileSize).append(", requiresFileName=")
				.append(requiresFileName).append(", requiresFilePath=")
				.append(requiresFilePath).append(", requiresLastRecordFlag=")
				.append(requiresLastRecordFlag)
				.append(", requiresRecordNumber=").append(requiresRecordNumber)
				.append("]");
		return builder.toString();
	}



}
