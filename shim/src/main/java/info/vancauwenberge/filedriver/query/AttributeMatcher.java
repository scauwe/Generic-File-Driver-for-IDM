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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.XDSSearchAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSValueElement;

public class AttributeMatcher extends QueryMatcher {
	private List<XDSSearchAttrElement> attributesToSearch;
	private Trace trace;

	protected AttributeMatcher(Trace trace,List<XDSSearchAttrElement> attributesToSearch) {
		super();
		this.attributesToSearch = attributesToSearch;
		this.trace = trace;

	}

	@Override
	public boolean matchesRecord(Map<String,String> record) {
		Iterator<XDSSearchAttrElement> it = attributesToSearch.iterator();
		while (it.hasNext()) {
			XDSSearchAttrElement anAttribute = it.next();
			String value = (String)record.get(anAttribute.getAttrName());
			if (value != null){
				@SuppressWarnings("unchecked")
				List<XDSValueElement> searchValues = anAttribute.extractValueElements();
				if (searchValues.size()>0){
					XDSValueElement theSearchValue = (XDSValueElement)searchValues.get(0);//We only support one value element
					if (!value.equals(theSearchValue.extractText())){
						return false;
					}
				}
			}else{
				return false;
			}
		}
		trace.trace("Record matched based on attributes");
		return true;
	}
}
