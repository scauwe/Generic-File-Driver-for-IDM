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

package info.vancauwenberge.filedriver.filename;

import java.util.HashMap;
import java.util.Map;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.IFileNameStrategy;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

public class GUIDFileNameStrategy extends AbstractStrategy implements IFileNameStrategy {
	private enum Parameters implements IStrategyParameters{
		/**
		 * Part before the GUID
		 */
		GUID_NAME_FILE_POSTFIX {
			@Override
			public String getParameterName() {
				return "GUIDNamer_FilePostFix";
			}

			@Override
			public String getDefaultValue() {
				return ".xml";
			}
		},
		/**
		 * Part after the GUID
		 */
		GUID_NAME_FILE_PREFIX {
			@Override
			public String getParameterName() {
				return "GUIDNamer_FilePreFix";
			}

			@Override
			public String getDefaultValue() {
				return "";
			}
		};
		

		public abstract String getParameterName();

		public abstract String getDefaultValue();

		public DataType getDataType() {
			return DataType.STRING;
		}

		public Constraint[] getConstraints() {
			return null;
		}
	}
	
	private String fileExtention;
	private String filePreFix;
	
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.INewFileNamer#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void init(Trace trace, Map<String, Parameter> driverParams, IDriver driver)
			throws XDSParameterException {
		fileExtention = getStringValueFor(Parameters.GUID_NAME_FILE_POSTFIX,driverParams);
		filePreFix = getStringValueFor(Parameters.GUID_NAME_FILE_PREFIX,driverParams);
	}
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.INewFileNamer#getNewFileName(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public String getNewFileName(Map<String,String> record) {
		StringBuilder sb = new StringBuilder(filePreFix);
		sb.append((new RandomGUID()).valueAfterMD5).append(fileExtention);
		return sb.toString();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}

	public static void main(String[] args){
		GUIDFileNameStrategy sdf = new GUIDFileNameStrategy();
		System.out.println("Params:"+sdf.getParameterDefinitions());
		HashMap<String, Parameter> map = new HashMap<String, Parameter>();
        map.put(Parameters.GUID_NAME_FILE_POSTFIX.getParameterName(), new Parameter(Parameters.GUID_NAME_FILE_POSTFIX.getParameterName(), ".csv", DataType.STRING));
        map.put(Parameters.GUID_NAME_FILE_PREFIX.getParameterName(), new Parameter(Parameters.GUID_NAME_FILE_PREFIX.getParameterName(), "new_file", DataType.STRING));
        try {
			sdf.init(null, map,null);
		} catch (XDSParameterException e) {
			e.printStackTrace();
		}
		System.out.println(sdf.getNewFileName(null));
	}
	
}
