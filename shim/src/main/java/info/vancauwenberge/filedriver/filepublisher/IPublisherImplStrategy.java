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
package info.vancauwenberge.filedriver.filepublisher;

import info.vancauwenberge.filedriver.api.IDriver;

import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.XmlCommandProcessor;
import com.novell.nds.dirxml.driver.XmlDocument;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSClassDefElement;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;
import com.novell.nds.dirxml.driver.xds.XDSParseException;
import com.novell.nds.dirxml.driver.xds.XDSResultDocument;
/**
 * This is the implementation that the shim expects.
 * @author stefaanv
 *
 */
public interface IPublisherImplStrategy {
	/**
	 * Common configuration options. Each publisher implementation should do somthing with this.
	 */
	static public final String PUB_HEARTBEAT_INTERVAL = "pub_heartbeatInterval";

	/**
	 * Delegation method from XDSPublisher
	 * @param arg0
	 * @return
	 */
	public XmlDocument query(XmlDocument arg0);
	/**
	 * Delegation method from XDSPublisher
	 * @param arg0
	 * @return
	 */
    public XmlDocument start(XmlCommandProcessor arg0);
    /**
     * init this publisher implementation
     * @param trace
     * @param driverParams
     * @param driver
     * @param initXML
     * @return
     * @throws Exception
     */
    public XmlDocument init(Trace trace, Map<String,Parameter> driverParams, IDriver driver, XmlDocument initXML) throws Exception;

    
    public void shutdown(XDSResultDocument result);
    
    public void extendSchema(XDSClassDefElement userClassDef, XmlDocument initXML) throws XDSParseException, XDSParameterException;
}
