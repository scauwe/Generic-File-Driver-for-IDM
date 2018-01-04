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
package info.vancauwenberge.filedriver.filelogger;

import info.vancauwenberge.filedriver.api.IPublisherLoggerStrategy;
import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSStatusElement;

public class PublisherNoLogger implements IPublisherLoggerStrategy{

	public void init(Trace trace, Map<String, Parameter> driverParams, IPublisher publisher) throws Exception {
	}

	public Map<String, Parameter> getParameterDefinitions() {
		return new HashMap<String, Parameter>(0);
	}

	public File close() throws WriteException {
		return null;
	}

	public void logCommand(int recordNumber, Map<String,String> thisRecord,
			XDSStatusElement xdsStatusElement) throws WriteException {
	}

	public void openFile(File f, String[] schema,
			EnumMap<LogField, String> logFieldSchemaMap) throws WriteException {
		// TODO Auto-generated method stub
		
	}

}
