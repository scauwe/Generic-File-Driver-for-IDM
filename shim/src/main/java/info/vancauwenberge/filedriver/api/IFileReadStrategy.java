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

import info.vancauwenberge.filedriver.exception.ReadException;

import java.io.File;
import java.io.IOException;
import java.util.Map;


public interface IFileReadStrategy extends IPublisherStrategy{
	/**
	 * Tells the reader to prepare for reading a given file. The previous file (if any) was processed completly.
	 * @param trace
	 * @param f
	 * @throws IOException
	 */
	public void openFile(File f) throws ReadException;
    /**
     * Reads one record from the file. Return null if no more records can be read from the file.
     * @param trace
     * @return a map of key/value pairs. The key is the fieldname (String), the value is the String value for that field. Value can be null.
     * @throws IOException
     */
    public Map<String,String> readRecord() throws ReadException;
    /**
     * Tells the reader that no more records will be read from this file (readRecord returned null). 
     * The reader should release all resources associated with the file.
     * @throws IOException
     */
    public void close() throws ReadException;
    
    
	/**
	 * Get the actual schema used by the file reader. This can differ than the schema configured on the driver (eg: headers in the CSV file).
	 * @return
	 */
	public String[] getActualSchema();
}
