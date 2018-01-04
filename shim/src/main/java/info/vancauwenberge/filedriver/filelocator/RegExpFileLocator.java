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

package info.vancauwenberge.filedriver.filelocator;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IFileLocatorStrategy;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;


public class RegExpFileLocator extends AbstractStrategy implements IFileLocatorStrategy{
	
	
	
	/**
	 * A file filter that uses regexp to find the files to process. It also checks if
	 * the file is locked or not.
	 */
	public class RegExpFileFilter implements FilenameFilter {
		public RegExpFileFilter() {
			super();
		}
		public boolean accept(File dir, String name) {
			regExpMatcher.reset(name);
			if (regExpMatcher.matches())
			{
				trace.trace("File matches regexp:"+name, TraceLevel.DEBUG);
				File actualFile = new File(dir, name);
				//We do not read directories...
				if (actualFile.isDirectory())
				{
					trace.trace("File is a folder, ignored:"+name, TraceLevel.DEBUG);
					return false;
				}
				else
				{
					File f = new File(dir, name);
					//Try to see if it locked.
					FileChannel channel = null;
					FileLock theLock = null;
					try {
						channel = new RandomAccessFile(f, "rw").getChannel();
						theLock = channel.tryLock();
						if (theLock == null)
						{
							trace.trace("File is locked, ignored:"+name, TraceLevel.DEBUG);
							return false;
						}
						else{
							theLock.release();
							channel.close();
							return true;
						}
					} catch (java.io.FileNotFoundException e) {
						try{
							if(theLock != null)
								theLock.release();
							if (channel != null)
								channel.close();
						}catch (Exception eIgnore) {
						}
						//RandomAccessFile throws FileNotFound when the file is locked
						trace.trace("File is locked, ignored:"+name, TraceLevel.DEBUG);
						return false;
					} catch(Exception e){
						try{
							if(theLock != null)
								theLock.release();
							if (channel != null)
								channel.close();
						}catch (Exception eIgnore) {
						}
						Util.printStackTrace(trace, e);
						return false;
					}
				}
			}else{
				trace.trace("File does not match regexp, ignored:"+name, TraceLevel.DEBUG);
			}
			return false;
		}
	}

	private enum Parameters implements IStrategyParameters{
		/**
		 * Source folder parameter
		 */
		DRIVER_PARAM_SOURCE_FOLDER {
			@Override
			public String getParameterName() {
				return "regExp-sourceFolder";
			}

			@Override
			public String getDefaultValue() {
				return "/opt/novell";
			}
		},
		/**
		 * Regexp file name matcher parameter
		 */
		DRIVER_PARAM_REGEXP {
			@Override
			public String getParameterName() {
				return "regExp-regExp";
			}

			@Override
			public String getDefaultValue() {
				return ".*";
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
	
	
	/**
	 * Folder to scan for files-to-process
	 */
	private String sourceFolder;
	/**
	 * Compiled regular expression used by the file Filter
	 */
	private Matcher regExpMatcher = null;
	private Trace trace;

	
	
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileLocator#getNextFile()
	 */
	public File[] getFileList() {
		trace.trace("getFileList start ("+sourceFolder+")", TraceLevel.TRACE);
		//Get the next file object.
		File f = new File(sourceFolder);
		String [] fileList = f.list(getFileFilter());
		if (fileList != null && fileList.length>0){
			//Transform the list of strings to a list of files
			File[] files = new File[fileList.length];
			for (int i = 0; i < fileList.length; i++) {
				String fileName = fileList[i];
				files[i] = new File(sourceFolder, fileName);
			}
			trace.trace("getFileList done", TraceLevel.TRACE);
			return files;
		} else if (fileList==null){
			trace.trace("FileList is null. Will try again in next polling cycle.", TraceLevel.ERROR_WARN);			
		}
		trace.trace("getFileList done", TraceLevel.TRACE);
		return null;
	}

	/**
	 * Return the file filter to use. Overwrite if you want to change the default RegExpFileFilter.
	 * @param trace
	 * @return
	 */
	public FilenameFilter getFileFilter() {
		FilenameFilter filter = new RegExpFileFilter();
		return filter;
	}


	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileLocator#init(com.novell.nds.dirxml.driver.XmlDocument)
	 */
	public void init(Trace trace, Map<String,Parameter> driverParams, IPublisher publisher) throws XDSParameterException{
		this.trace = trace;
		
		sourceFolder = getStringValueFor(Parameters.DRIVER_PARAM_SOURCE_FOLDER,driverParams);
		//driverParams.get(DRIVER_PARAM_SOURCE_FOLDER).toString();
		String regExpString = getStringValueFor(Parameters.DRIVER_PARAM_REGEXP,driverParams);
		//driverParams.get(DRIVER_PARAM_REGEXP).toString();
		if (regExpString!=null && !"".equals(regExpString))
			regExpMatcher = Pattern.compile(regExpString).matcher("");
		
		if ((sourceFolder == null) | (regExpMatcher==null))
		{
			throwXDSParameterException(trace, "Sourcefolder nor regexp can be null.");
		}
		trace.trace("init:"+sourceFolder+" - "+regExpMatcher);
		//Check if the folder is exsisting
		//Create if needed.
		File f = new File(sourceFolder);
		if (!f.exists())
		{
			trace.trace("Creating source folder.", TraceLevel.DEBUG);
			f.mkdirs();
		}
		if (!f.isDirectory())
		{
			throwXDSParameterException(trace, "Sourcefolder is not a directory.");
		}
	}

	private void throwXDSParameterException(Trace trace, String message) throws XDSParameterException{
		trace.trace(message, TraceLevel.ERROR_WARN);
		throw new XDSParameterException(message);			
		
	}

	
	public static void main(String[] args)
	{
		Field[] fields = File.class.getFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			System.out.println(field.getName()+":"+field.getType()+" - "+field.isAccessible());
		}
		try {
			System.out.println(new RegExpFileLocator().getParameterDefinitions());
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}
}
