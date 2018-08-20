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
package info.vancauwenberge.filedriver.filename;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.IFileNameStrategy;

import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.RequiredConstraint;
import com.novell.soa.script.mozilla.javascript.Context;


public class EcmaScriptFileNameStrategy extends AbstractStrategy implements IFileNameStrategy {
    private enum Parameters implements IStrategyParameters{
        SCRIPT {
			@Override
			public String getParameterName() {
				return "ecmaNamer_script.js";
			}

			@Override
			public String getDefaultValue() {
				return null;
			}
		};
        
		public abstract String getParameterName();
		public abstract String getDefaultValue();

		public DataType getDataType() {
			return DataType.STRING;
		}


		public Constraint[] getConstraints() {
			return new Constraint[]{RequiredConstraint.REQUIRED};
		}
    }

	private String ecmaScript;
	private IDriver driver;
	private Trace trace;


	public void init(Trace trace, Map<String, Parameter> driverParams, IDriver driver)
			throws Exception {
		this.trace = trace;
		this.ecmaScript = getStringValueFor(Parameters.SCRIPT,driverParams);
		this.driver = driver;
	}

	public String getNewFileName(Map<String, String> record) {
		Object result =  driver.getEcmaBuilder().evaluateEcmaWithParams(trace, record, ecmaScript, Parameters.SCRIPT.getParameterName());
		//Object result =  EcmaUtil.evaluateEcmaWithParams(trace, EcmaUtil.JsProcessing.REPLACE, replaceString, record, ecmaScript, Parameters.SCRIPT.getParameterName());
		return Context.toString(result);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}

}
