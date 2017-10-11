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


import info.vancauwenberge.filedriver.util.EcmascriptBuilder;

import java.util.Map;

import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.WriteableDocument;
import com.novell.nds.dirxml.driver.xds.XDSCommandDocument;
import com.novell.nds.dirxml.driver.xds.XDSResultDocument;

public interface IDriver {
	public ISubscriberShim getSubscriber();
	/**
	 * Create a new XDS result document
	 * @return
	 */
	public XDSResultDocument newResultDoc();
	
	/**
	 * Get all the driver parameters
	 * @return
	 */
	public Map<? extends String, ? extends Parameter> getDriverParams();
	
	/**
	 * Append the source node to the given query doc
	 * @param result
	 */
	public void appendSourceInfo(WriteableDocument doc);
	
	/**
	 * Get the drivers RDN (=cn)
	 * @return
	 */
	public String getDriverInstanceName();
	
	/**
	 * Get the driver's schema
	 * @return
	 */
	public Object getSchema();
	
	/**
	 * Create a new command doc
	 * @return
	 */
	public XDSCommandDocument newCommandDoc();
	
	/**
	 * Get the association value for the given record
	 * @param record
	 * @return
	 */
	public String getAssociationField(Map<String, String> record);
	/**
	 * Get the source field for the given record
	 * @param record
	 * @return
	 */
	public String getSourceField(Map<String, String> record);
	
	/**
	 * Get a pre-configured ECMA script evaluator
	 * 
	 */
	public EcmascriptBuilder getEcmaBuilder();

}
