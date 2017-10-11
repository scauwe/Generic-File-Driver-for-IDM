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
package info.vancauwenberge.filedriver.filewriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.IFileWriteStrategy;
import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.util.Util;
import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

public class DummyFileWriter implements IFileWriteStrategy{
	private File theFile;
	private OutputStreamWriter osw;
	private Trace trace;
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#getParameterDefinitions()
	 */
	public Map<String,Parameter> getParameterDefinitions() {
		return new HashMap<String,Parameter>();
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void init(Trace trace, Map<String,Parameter> driverParams, IDriver driver) throws XDSParameterException {
		this.trace = trace;
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	public void openFile(File f) throws WriteException {
		try {
			FileOutputStream fos = new FileOutputStream(f);
			osw = new OutputStreamWriter(fos,"UTF-8");
			theFile = f;
		} catch (Exception e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#writeRecord(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void writeRecord(Map<String,String> m) throws WriteException {
		try {
			osw.write(m.toString()+"\n");
		} catch (IOException e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileWriteStrategy#close()
	 */
	public File close() throws WriteException {
		try {
			osw.close();
			return theFile;
		} catch (IOException e) {
			Util.printStackTrace(trace,e);
			throw new WriteException(e);
		}
	}
}
