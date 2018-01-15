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

import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;

public class AndMatcher extends QueryMatcher {
	private QueryMatcher matcher1;
	private QueryMatcher matcher2;
	private Trace trace;

	protected AndMatcher(Trace trace, QueryMatcher matcher1, QueryMatcher matcher2) {
		super();
		this.matcher1= matcher1;
		this.matcher2 = matcher2;
		this.trace = trace;
	}


	@Override
	public boolean matchesRecord(Map<String,String> record) {
		boolean result= matcher1.matchesRecord(record) && matcher2.matchesRecord(record);
		if (result)
			trace.trace("Matched on AND of 2 conditions");
		return result;
	}
}
