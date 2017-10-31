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
package info.vancauwenberge.filedriver.api;

import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.shim.ConnectionInfo;

public interface ISubscriberShim {
	/**
	 * Forcefully close the current file. This file already places a synchronized block on the file lock. If this
	 * is the only file operation you inted to do, no need to add an additional synchronized block on the file lock.
	 * Used to be part of IFileOwnerCallback
	 * @return true if a file was actually closed. False otherwise.
	 * @throws WriteException
	 */
	public boolean finishFile() throws WriteException;
	/**
	 * Get the filelock that is used by this object to read and write to the file or close a new file.
	 * Used to be part of IFileOwnerCallback. Synchronize on this object to perform tests on hte current file.
	 * @return
	 */
	public Object getFileLock();
	/**
	 * Get the number of records in the current file or -1 if no current file.
	 */
	public abstract int getCurrentFileRecordCount();
	/**
	 * Does the subscriber currently have an open file
	 */
	public abstract boolean isFileOpen();

	/**
	 * Add a file operation listener to the subscriber
	 * @param listener
	 */
	public void addFileListener(ISubscriberFileListener listener);
	/**
	 * Remove a file operation listener from the subscriber
	 * @param listener
	 */
	public void removeFileListener(ISubscriberFileListener listener);
	/**
	 * Returns a data wrapper holding the configured connection information.
	 * @return
	 */
	public ConnectionInfo getConnectionInfo();
}
