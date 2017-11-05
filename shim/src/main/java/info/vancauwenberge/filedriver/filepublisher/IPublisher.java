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
package info.vancauwenberge.filedriver.filepublisher;

import java.io.File;

/**
 * This is the API that the enabled publisher strategy exposes.
 * @author stefaanv
 *
 */
public interface IPublisher {

	/**
	 * Get the folder where files are moved to for processing
	 * @return
	 */
	public String getWorkDir();

	/**
	 * Get the current file in process (should be in workDir).
	 * Note: files are only moved to the workdir just prior to processing. If multiple files are found for processing, only
	 * the first one is moved to 'workdir' and processed. After this, the next file is moved and processed etc...
	 * @return
	 */
	public File getCurrentFile();

}
