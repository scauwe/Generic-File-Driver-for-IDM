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
package info.vancauwenberge.filedriver.filereader;

import info.vancauwenberge.filedriver.api.IFileReadStrategy;
import info.vancauwenberge.filedriver.exception.ReadException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

public class DummyFileReader implements IFileReadStrategy{
	BufferedReader br;
	/**
	 * Get a Map of parameter definitions. Key=parameter name, value=Parameter object
	 * @param
	 * @return the map with parameter definitions
	 */
	public Map<String, Parameter> getParameterDefinitions(){
		return new HashMap<String, Parameter>(0);
	}
	
    /**
     * Initialize this file reader on startup
     * @param trace
     * @param driverParams
     * @throws XDSParameterException
     */
    public void init(Trace trace, Map<String, Parameter> driverParams, IPublisher publisher) throws XDSParameterException{

    }

    /* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	public void openFile(File f) throws ReadException{
		try{
			FileInputStream fis = new FileInputStream(f);
			InputStreamReader isr = new InputStreamReader(fis,"UTF-8");
			br = new BufferedReader(isr);
		}catch (IOException e) {
			throw new ReadException("Exception during openFile:"+e.getMessage(),e);
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#readRecord(com.novell.nds.dirxml.driver.Trace)
	 */
	public Map<String,String> readRecord() throws ReadException{
		try{
		//as long as we have bytes in the file, read the next byte
		String line = br.readLine();
		if (line != null)
		{
			Map<String,String> m = new HashMap<String,String>();
			m.put("field1","dummy1");
			m.put("field2","dummy2");
			m.put("line",line);
			return m;
		}
		else
			return null;
		}catch (IOException e) {
			throw new ReadException("Exception during readRecord:"+e.getMessage(),e);
		}
	}

	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#close()
	 */
	public void close() throws ReadException{
		try {
			br.close();
		} catch (IOException e) {
			throw new ReadException("Exception during close:"+e.getMessage(),e);
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReadStrategy#getActualSchema()
	 */
	public String[] getActualSchema() {
		return null;
	}
}
