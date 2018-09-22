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
package info.vancauwenberge.filedriver.filereader;

import java.util.LinkedList;
import java.util.Map;

public class RecordQueue {
	private LinkedList<Map<String, String>> list = new LinkedList<Map<String, String>>();
	private boolean isFinished = false;
	private static final int MAXQUEUESIZE = 20;
	private Exception exceptionToThrow = null;

	public void setFinished() {
		synchronized (list) {
			isFinished = true;
			list.notifyAll();
		}
	}

	public void addRecord(Map<String, String> record) {
		synchronized (list) {
			// Only add when we did not have any exception and are still running
			if ((exceptionToThrow == null) && !isFinished) {
				if (list.size() >= MAXQUEUESIZE) {
					try {
						System.out.println("..waiting before appending records to the queue.");
						list.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				list.add(record);
			}
			list.notifyAll();
		}
	}

	public Map<String, String> getNextRecord() throws Exception {
		Map<String, String> result = null;
		synchronized (list) {
			// If 0, we either reached the end, we had an exception or still
			// parsing and we need to wait
			if (list.size() == 0) {
				if (!isFinished && (exceptionToThrow == null)) {
					try {
						// wait until we get notified about an update
						System.out.println("..waiting for next record to get added.");
						list.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (list.size() == 0) {
					if (exceptionToThrow != null) {
						list.notifyAll();
						throw exceptionToThrow;
					} // Assume finished. Only option left...
					return null;
				} else {
					result = list.removeFirst();
					list.notifyAll();
				}
			} else {
				// size >0
				result = list.removeFirst();
				list.notifyAll();
			}
		}
		return result;
	}

	/**
	 *
	 */
	public void setFinishedInError(Exception e) {
		exceptionToThrow = e;
		synchronized (list) {
			list.notifyAll();
		}
	}
}
