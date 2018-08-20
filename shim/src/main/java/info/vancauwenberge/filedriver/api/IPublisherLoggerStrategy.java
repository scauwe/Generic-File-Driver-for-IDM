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
package info.vancauwenberge.filedriver.api;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import com.novell.nds.dirxml.driver.xds.XDSStatusElement;

import info.vancauwenberge.filedriver.exception.WriteException;

public interface IPublisherLoggerStrategy extends IPublisherStrategy {
	public enum LogField{
		RECORDNUMBER, //The Number of the record
		LOGSTATUS, //RETRY, ERROR, ETC
		LOGMESSAGE, //The message in the status
		LOGEVENTID //Event ID in the status message
	}
	/**
	 * Tells the logger to prepare for writing a given file.
	 * @param schema
	 * @param f
	 * @throws IOException
	 */
	public void openFile(File f, String[] schema, EnumMap<LogField, String> logFieldSchemaMap) throws WriteException;
	/**
	 * Write one record to the file.
	 * @param m A map of key/value pairs. The key is the fieldname (String), the value is the String value for that field. Value can be null.
	 * @return 
	 * @throws IOException
	 */
	public void logCommand(int recordNumber, Map<String,String> thisRecord, XDSStatusElement xdsStatusElement) throws WriteException;
	/**
	 * Tells the logger that no more records will be added to this file. 
	 * The writer should release all resources associated with the file and close the file.
	 * @throws IOException
	 */
	public File close() throws WriteException;

}
