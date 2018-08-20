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
package info.vancauwenberge.filedriver.api;



import java.util.HashMap;
import java.util.Map;

import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;

public abstract class AbstractStrategy  implements IStrategy{

	public interface IStrategyParameters{
		public String getParameterName();
		public String getDefaultValue();
		public DataType getDataType();
		public Constraint[] getConstraints();
	}
	
	public static String getStringValueFor(IStrategyParameters paramDef, Map<String,Parameter> subParams){
    	return subParams.get(paramDef.getParameterName()).toString();
    }

    public static boolean getBoolValueFor(IStrategyParameters paramDef,Map<String,Parameter> subParams){
    	return subParams.get(paramDef.getParameterName()).toBoolean().booleanValue();
    }

    public static int getIntValueFor(IStrategyParameters paramDef, Map<String,Parameter> subParams){
    	return subParams.get(paramDef.getParameterName()).toInteger();
    }

    public static long getLongValueFor(IStrategyParameters paramDef, Map<String,Parameter> subParams){
    	return subParams.get(paramDef.getParameterName()).toLong();
    }
	
	private <E extends Enum<?> & IStrategyParameters> Map<String,Parameter> getIDMParameters(Class<E>  parameters){
    	E[] values = parameters.getEnumConstants();
    	Map<String,Parameter> result = new HashMap<String, Parameter>(values.length);
    	for (E aParameter : values) {
            Parameter param = new Parameter(aParameter.getParameterName(), //tag name
            		aParameter.getDefaultValue(), //default value (optional)
            		aParameter.getDataType()); //data type
            Constraint[] cons = aParameter.getConstraints();
            if (cons != null)
            	for (Constraint  constraint: cons) {
                	param.add(constraint);
				}
            result.put(aParameter.getParameterName(), param);
		}
		return result;
    	
    }
 
	
	public Map<String,Parameter> getParameterDefinitions(){
		//The XDS.jar library automatically checks parameter
		//data types for you.  When a RequiredConstraint
		//is added to a parameter, the library will check init documents
		//to ensure the parameter is present and has a value.  When you
		//add RangeConstraints or EnumConstraints to a parameter, the
		//library will check parameter values to see if they satisfy these
		//constraints.
		return getIDMParameters(getParametersEnum());
	}




    public abstract <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum();

}
