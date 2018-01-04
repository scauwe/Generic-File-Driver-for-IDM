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
package info.vancauwenberge.filedriver.query;

import info.vancauwenberge.filedriver.api.IDriver;

import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;

public class AssociationMatcher extends QueryMatcher {
	private String associationValue;
	private String destDNValue;
	private IDriver driver;
	private Trace trace;

	protected AssociationMatcher(Trace trace,IDriver driver2, String associationValue, String destDNValue) {
		super();
		this.driver= driver2;
		this.associationValue = associationValue;
		this.destDNValue = destDNValue;
		this.trace = trace;
	}


	@Override
	public boolean matchesRecord(Map<String,String> record) {
		String thisRecordAssociation = driver.getAssociationField(record);
		String thisRecordDestDN = driver.getSourceField(record);
		if (destDNValue!= null && associationValue != null){
			boolean result = associationValue.equals(thisRecordAssociation) && destDNValue.equals(thisRecordDestDN);
			if (result)
				trace.trace("Matched on association and destDn");
			return result;
		}
		if (destDNValue!= null){
			boolean result= destDNValue.equals(thisRecordDestDN);
			if (result)
				trace.trace("Matched on destDn");
			return result;
		}
		if (associationValue != null){
			boolean result = associationValue.equals(thisRecordAssociation);
			if (result)
				trace.trace("Matched on association");
			return result;
		}
		//This should never happen
		return false;
	}
}
