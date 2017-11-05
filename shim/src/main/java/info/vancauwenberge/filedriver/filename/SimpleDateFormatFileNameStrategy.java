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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.IFileNameStrategy;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.RequiredConstraint;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

public class SimpleDateFormatFileNameStrategy extends AbstractStrategy implements IFileNameStrategy {
	//private static final String DATE_NAME_FORMAT_STRING = "simpleDateNamer_FormatString";
	//private static final String DATE_NAME_TIMEZONE = "simpleDateNamer_TimeZone";
	
	   private static enum Parameters implements IStrategyParameters{
		   DATE_NAME_FORMAT_STRING {
			@Override
			public String getParameterName() {
				return "simpleDateNamer_FormatString";
			}

			@Override
			public String getDefaultValue() {
				return "'NEW'_'FILE'_yyyyMMdd-HHmmssSSS.'out'";
			}

			@Override
			public Constraint[] getConstraints() {
				return new Constraint[]{RequiredConstraint.REQUIRED};
			}
		},
		   DATE_NAME_TIMEZONE {
			@Override
			public String getParameterName() {
				return "simpleDateNamer_TimeZone";
			}

			@Override
			public String getDefaultValue() {
				return "GMT";
			}

			@Override
			public Constraint[] getConstraints() {
				return null;
			}
		};
	        
			public abstract String getParameterName();


			public abstract String getDefaultValue();


			public DataType getDataType() {
				return DataType.STRING;
			}


			public abstract Constraint[] getConstraints();
	    }
	
	private String formatString;
	private TimeZone timeZone;
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.INewFileNamer#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void init(Trace trace, Map<String, Parameter> driverParams, IDriver driver)
			throws XDSParameterException {
		formatString = getStringValueFor(Parameters.DATE_NAME_FORMAT_STRING,driverParams);
		
		String strTimeZone = getStringValueFor(Parameters.DATE_NAME_TIMEZONE,driverParams);
		if ("".equals(strTimeZone))
			timeZone = TimeZone.getDefault();
		else
			timeZone = TimeZone.getTimeZone(strTimeZone);
	}
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.INewFileNamer#getNewFileName(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public String getNewFileName(Map<String,String> record) {
		SimpleDateFormat formatter = new SimpleDateFormat(formatString);
		formatter.setTimeZone(timeZone);
		return formatter.format(new Date());
	}
	
	public static void main(String[] args){
		SimpleDateFormatFileNameStrategy sdf = new SimpleDateFormatFileNameStrategy();
		HashMap<String, Parameter> map = new HashMap<String, Parameter>();
        map.put(Parameters.DATE_NAME_FORMAT_STRING.getParameterName(), new Parameter(Parameters.DATE_NAME_FORMAT_STRING.getParameterName(), "'NEWER'_'FILE'_yyyy-MM-dd_HH-mm-ss_SSS", DataType.STRING));
        map.put(Parameters.DATE_NAME_TIMEZONE.getParameterName(), new Parameter(Parameters.DATE_NAME_TIMEZONE.getParameterName(), "GMT", DataType.STRING));
        try {
			sdf.init(null, map, null);
		} catch (XDSParameterException e) {
			e.printStackTrace();
		}
		System.out.println(sdf.getNewFileName(null));
	}
	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}
}
