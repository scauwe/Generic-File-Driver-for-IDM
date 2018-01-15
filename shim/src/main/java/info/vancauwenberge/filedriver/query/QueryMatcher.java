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
package info.vancauwenberge.filedriver.query;

import info.vancauwenberge.filedriver.api.IDriver;

import java.util.List;
import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.XDSAssociationElement;
import com.novell.nds.dirxml.driver.xds.XDSQueryDocument;
import com.novell.nds.dirxml.driver.xds.XDSQueryElement;
import com.novell.nds.dirxml.driver.xds.XDSSearchAttrElement;

public abstract class QueryMatcher {

	@SuppressWarnings("unchecked")
	public static QueryMatcher getMatcher(Trace trace, XDSQueryDocument queryDoc, IDriver driver){
    	List<XDSQueryElement> queryList = queryDoc.extractQueryElements();
		XDSQueryElement queryElem = queryList.get(0);//We only support one query element...
		String associationValue = getAssociationValueFromQuery(queryElem);
		String destDNValue = queryElem.getDestDN();
		List<XDSSearchAttrElement> attributesToSearch = queryElem.extractSearchAttrElements();
		if (((associationValue != null)||(destDNValue!=null)) 
				&& (attributesToSearch != null) && (attributesToSearch.size()>0)){
			return new AndMatcher(trace, new AssociationMatcher(trace, driver,associationValue,destDNValue), new AttributeMatcher(trace, attributesToSearch));
		}
		if ((associationValue != null)||(destDNValue!=null))
			return new AssociationMatcher(trace, driver,associationValue,destDNValue);
		if ((attributesToSearch != null) && (attributesToSearch.size()>0))
			return new AttributeMatcher(trace, attributesToSearch);
		return new NullMatcher(trace);
	}
	
	private static String getAssociationValueFromQuery(XDSQueryElement queryElem) {
		XDSAssociationElement association = queryElem.extractAssociationElement();
		String associationValue = null;
		if (association != null)
			associationValue = association.extractText();
		if (associationValue != null)
			associationValue = associationValue.trim();
		if ("".equals(associationValue))
			return null;
		return associationValue;
	}

	public abstract boolean matchesRecord(Map<String,String> aRecord);

}
