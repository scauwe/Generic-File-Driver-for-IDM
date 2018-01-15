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
package info.vancauwenberge.filedriver;

import java.util.HashMap;

import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;

@SuppressWarnings("serial")
public class ParamMap extends HashMap<String, Parameter> {
	public void putParameter(final String name, final String value){
		this.put(name, new Parameter(name,value,DataType.STRING));
	}

	public void putParameter(final String name, final long value){
		//No clue how the Parameter works, so just overwrite what we need...
		this.put(name, new Parameter(name,"0",DataType.LONG){
			@Override
			public Long toLong(){
				return value;
			}
		});
	}

	public void putParameter(final String name, final int value){
		//No clue how the Parameter works, so just overwrite what we need...
		this.put(name, new Parameter(name,"0",DataType.INT){
			@Override
			public Integer toInteger(){
				return value;
			}
		});
	}

	public void putParameter(final String name, final boolean value){
		this.put(name, new Parameter(name,"true",DataType.BOOLEAN){
			@Override
			public Boolean toBoolean(){
				return value;
			}
		});
	}
}
