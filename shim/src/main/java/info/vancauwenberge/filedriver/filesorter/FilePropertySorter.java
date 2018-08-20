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

package info.vancauwenberge.filedriver.filesorter;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IFileSorterStrategy;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.util.TraceLevel;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.EnumConstraint;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

/**
 * FileSorterStrategy that sorts a list of files based on a file property.
 */
public class FilePropertySorter extends AbstractStrategy implements IFileSorterStrategy{
	private enum Parameters implements IStrategyParameters{
		DRIVER_PARAM_FILE_SORT_METHOD{
			public String getParameterName() {
				return "fileSort_SortMethod";
			}

			public String getDefaultValue() {
				return "";
			}

			public DataType getDataType() {
				return DataType.STRING;
			}

			public Constraint[] getConstraints() {
		        EnumConstraint constraint = new EnumConstraint();
		        List<String> possibleMethods = getSortMethods();
		        constraint.addLiterals((String[]) possibleMethods.toArray(new String[possibleMethods.size()]));
		        return new Constraint[]{constraint}; 
			}
		},
		DRIVER_PARAM_FILE_SORT_ORDER_ASC{
			public String getParameterName() {
				return "fileSort_SortOrderAsc";
			}

			public String getDefaultValue() {
				return "";
			}

			public DataType getDataType() {
				return DataType.BOOLEAN;
			}

			public Constraint[] getConstraints() {
				return null;
			}
		}
		;

		public abstract String getParameterName();

		public abstract String getDefaultValue();

		public abstract DataType getDataType();

		public abstract Constraint[] getConstraints();
		
	}
	/**
	 * Methode to use to sort files on. Based on the method as set in the driver's Publisher parameters.
	 */
	private Method fileSortMethod;
	/**
	 * Sort the files ascending(true) or descending(false)
	 */
	private boolean fileSortOrderAsc = true;
	private Trace trace;

	/**
	 * We do not cache this: it is only when the driver is started that this is required. It would just consume memory.
	 * @return
	 */
	public static List<String> getSortMethods() {
		Method[] methodes = File.class.getMethods();
		ArrayList<String> possibleMethods = new ArrayList<String>();
		for (int i = 0; i < methodes.length; i++) {
			Method method = methodes[i];
			if (method.getParameterTypes().length==0){
				List<Class<?>> l = Arrays.asList(method.getReturnType().getInterfaces());
				//The return type must implement Comparable or a primitive!= void/boolean
				if (l.contains(Comparable.class) || 
						(method.getReturnType().isPrimitive() 
								&& (method.getReturnType()!=void.class) 
								&& (method.getReturnType()!=boolean.class)))
					possibleMethods.add(method.getName());
			}
		}
		return possibleMethods;
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileSorterStrategy#getFirstFile(java.io.File[])
	 */
	public File getFirstFile(File[] fileList) {
		if (fileList != null && fileList.length > 0)
		{
			trace.trace("getNextFile: found file(s)", TraceLevel.DEBUG);
			if (fileSortMethod != null && fileList.length>1){
				//Sort the files
				List<File> list = Arrays.asList(fileList);
				Collections.sort(list, fileComparator);
				return (File) list.get(0);
			}
			else
			{
				return fileList[0];
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileSorterStrategy#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	public void init(Trace trace, Map<String,Parameter> driverParams, IPublisher publisher) throws XDSParameterException {
		this.trace = trace;

		String fileSortMethodName = getStringValueFor(Parameters.DRIVER_PARAM_FILE_SORT_METHOD, driverParams);
		//driverParams.get(DRIVER_PARAM_FILE_SORT_METHOD).toString();
		List<String> possibleMethods = getSortMethods();
		if (possibleMethods.contains(fileSortMethodName)){
			fileSortOrderAsc = getBoolValueFor(Parameters.DRIVER_PARAM_FILE_SORT_ORDER_ASC, driverParams);
			//driverParams.get(DRIVER_PARAM_FILE_SORT_ORDER_ASC).toBoolean().booleanValue();
			try {
				fileSortMethod = File.class.getMethod(fileSortMethodName,new Class[]{});
			} catch (SecurityException e) {
				throwXDSParameterException(trace, "fileSortMethodName cannot be called:"+e.getMessage());
			} catch (NoSuchMethodException e) {
				throwXDSParameterException(trace, "fileSortMethodName not found:"+e.getMessage());
			}
		}
		else
		{
			if (fileSortMethod!= null && !"".equals(fileSortMethod))
				throwXDSParameterException(trace, "fileSortMethode ("+fileSortMethodName+") is not a valid method for the object File. Possible methodes are:"+possibleMethods);
		}
	}

	private void throwXDSParameterException(Trace trace, String message) throws XDSParameterException{
		trace.trace(message, TraceLevel.ERROR_WARN);
		throw new XDSParameterException(message);			
		
	}



	/**
	 * The Comparator to use when multipe files are found.
	 */
	private final Comparator<File> fileComparator = getFileComparator();

	/**
	 * Get the Comparator used to sort the inout files found for processing.
	 * Overwrite if you want to use another Comparator 
	 * @return
	 */
	public Comparator<File> getFileComparator(){
		return new SimpleFileComparator();
	}

	/**
	 * Comparator that sorts the files that matches the filter. Only used if more then 1 file is found.
	 * The Comparator uses the given property/method from the file object and the given sort order as specified in
	 * the driver's subscription parameters.
	 */
	public class SimpleFileComparator implements Comparator<File> {
		@SuppressWarnings("unchecked")
		public int compare(File file1, File file2){
			Comparable<Object> result1;
			Comparable<Object> result2;
			try {
				result1 = (Comparable<Object>) fileSortMethod.invoke(file1,new Object[]{});
			} catch (Exception e) {
				throw new RuntimeException("Error trying to invoke "+fileSortMethod.getName()+" on given file "+file1, e);
			}
			try {
				result2 = (Comparable<Object>) fileSortMethod.invoke(file2,new Object[]{});
			} catch (Exception e) {
				throw new RuntimeException("Error trying to invoke "+fileSortMethod.getName()+" on given file "+file2, e);
			}
			if (fileSortOrderAsc)
				return result1.compareTo(result2);
			else
				return -(result1.compareTo(result2));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}

}
