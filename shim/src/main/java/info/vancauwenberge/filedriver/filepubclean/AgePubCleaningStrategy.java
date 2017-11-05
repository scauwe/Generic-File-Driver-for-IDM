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
package info.vancauwenberge.filedriver.filepubclean;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IPubFileCleanStrategy;
import info.vancauwenberge.filedriver.api.IShutdown;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.RangeConstraint;
import com.novell.nds.dirxml.driver.xds.XDSResultDocument;

public class AgePubCleaningStrategy extends AbstractStrategy implements IPubFileCleanStrategy, IShutdown {

	private enum Parameters implements IStrategyParameters{
		AGE_NUMBER {
			@Override
			public String getParameterName() {
				return "pubclean_age_number";
			}

			@Override
			public String getDefaultValue() {
				return "1";
			}
		},
		AGE_UNITS {
			@Override
			public String getParameterName() {
				return "pubclean_age_unit";
			}

			@Override
			public String getDefaultValue() {
				return "2592000";//Baseline is seconds. 1 Month = 60 * 60 * 24 * 30 = 43200
			}

		},
		TEST_INTERVAL {
			@Override
			public String getParameterName() {
				return "pubclean_age_test_interval_number";
			}

			@Override
			public String getDefaultValue() {
				return "1";
			}
		},
		TEST_INTERVAL_UNIT {
			@Override
			public String getParameterName() {
				return "pubclean_age_test_interval_unit";
			}

			@Override
			public String getDefaultValue() {
				return "3600";//Baseline is seconds. 1 hour = 60 * 60
			}
		};

		public abstract String getParameterName();

		public abstract String getDefaultValue();

		public DataType getDataType() {
			return DataType.LONG;
		}

		public Constraint[] getConstraints() {
			return new Constraint[]{RangeConstraint.POSITIVE};
		}
		
	}
	/**
	 * A file filter that uses regexp to find the files to process. It also checks if
	 * the file is locked or not.
	 */
	private class RegExpFileFilter implements FilenameFilter {
		private final Trace trace;
		private long maxAge;
		public RegExpFileFilter(Trace trace, long maxAge) {
			this.trace = trace;
			this.maxAge = maxAge;
		}
		public boolean accept(File dir, String name) {
			File f = new File(dir, name);
			//Do not add this file if it is currently in process
			if (f.equals(publisher.getCurrentFile()))
					return false;
			
			//We only delete directories. Single files have another source...
			if (f.isDirectory()){
				
				if (f.lastModified()<maxAge)
				{
					trace.trace("Marked '"+name + "' for deletion: last modified is "+new Date(f.lastModified()), TraceLevel.DEBUG);
					return true;
				}else{
					trace.trace("Not marked for deletion ("+name+ "): last modified is "+new Date(f.lastModified()), TraceLevel.DEBUG);
					return false;
				}
			}
			return false;
		}
	}
	
	private final Object lock = new Object();
	private Thread runningThread = null;
	private long testIntervalInSeconds = 0;
	private int ageInSeconds = 0;
	private IPublisher publisher;
	private Trace trace;
	
	public void init(final Trace trace, Map<String, Parameter> driverParams, IPublisher publisher) throws Exception {
		this.publisher = publisher;
		this.trace = trace;
		long tmpAgeInSeconds = ((long)getLongValueFor(Parameters.AGE_NUMBER, driverParams)) * ((long)getLongValueFor(Parameters.AGE_UNITS, driverParams));
		this.testIntervalInSeconds = ((long)getLongValueFor(Parameters.TEST_INTERVAL, driverParams)) * ((long)getLongValueFor(Parameters.TEST_INTERVAL_UNIT, driverParams));
		
		if (this.testIntervalInSeconds <= 0){
			this.testIntervalInSeconds= Integer.parseInt(Parameters.TEST_INTERVAL.getDefaultValue()) * Integer.parseInt(Parameters.TEST_INTERVAL_UNIT.getDefaultValue());
			trace.trace("Invalid value for the testinterval. Using default of "+testIntervalInSeconds, TraceLevel.ERROR_WARN);
		}
		
		if (tmpAgeInSeconds < 0){
			tmpAgeInSeconds= Integer.parseInt(Parameters.AGE_NUMBER.getDefaultValue()) * Integer.parseInt(Parameters.AGE_UNITS.getDefaultValue());
			trace.trace("Invalid value for the testinterval. Use '0' if you want to disable the age test. Using default of "+tmpAgeInSeconds, TraceLevel.ERROR_WARN);
		}
		
		if (tmpAgeInSeconds > Integer.MAX_VALUE){
			tmpAgeInSeconds= Integer.parseInt(Parameters.AGE_NUMBER.getDefaultValue()) * Integer.parseInt(Parameters.AGE_UNITS.getDefaultValue());
			trace.trace("Invalid value for the testinterval. Using default of "+tmpAgeInSeconds, TraceLevel.ERROR_WARN);
		}
		this.ageInSeconds = (int)-tmpAgeInSeconds;
		start();
	}

	/**
	 * Tests if a file is locked
	 * @param f
	 * @return
	 */
	private boolean isLocked(File f){
		//Try to see if it locked.
		FileChannel channel = null;
		FileLock theLock = null;
		try {
			channel = new RandomAccessFile(f, "rw").getChannel();
			theLock = channel.tryLock();
			if (theLock == null)
			{
				trace.trace("File is locked. Will try again later:"+f.getName(), TraceLevel.DEBUG);
				return true;
			}
			else{
				theLock.release();
				channel.close();
				return false;
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
			trace.trace("File is locked. Will try again later:"+f.getName(), TraceLevel.DEBUG);
			return true;
		} catch(Exception e){
			try{
				if(theLock != null)
					theLock.release();
				if (channel != null)
					channel.close();
			}catch (Exception eIgnore) {
			}
			Util.printStackTrace(trace, e);
			return true;
		}

	}
	
	/**
	 * Delete this file/folder and any children (subtree delete).
	 * @param file
	 */
	private void deleteFiles(File file) {		
	    if (file.isDirectory()) {
			trace.trace("Deleting folder contents of "+file.getName(), TraceLevel.DEBUG);
       		for (File f : file.listFiles()) {
       			deleteFiles(f);
       		}
	    }
	    
		trace.trace("Deleting "+file.getName(), TraceLevel.DEBUG);
       	if (file.isDirectory() || !isLocked(file)){//We cannot test the lock on a folder!
       		try {
           		//file.delete();
				Files.delete(file.toPath());
			} catch (Exception e) {
        		trace.trace("Exception while deleting file "+file.getName(), TraceLevel.EXCEPTION);
        		Util.printStackTrace(trace, e);
			}
       	}
       	else{
       		trace.trace("Unable to delete locked file:"+file.getName(), TraceLevel.DEBUG);
	    }
	}
	
	/**
	 * Get the list of files to delete
	 * @return
	 */
	private File[] getFileList() {
		String publisherDir = publisher.getWorkDir();
		trace.trace("AgePubCleaningStrategy.getFileList start ("+publisherDir+")", TraceLevel.TRACE);
		try{

			File f = new File(publisherDir);
			String [] fileList = f.list(getFileFilter());
			if (fileList != null && fileList.length>0){
				//Transform the list of strings to a list of files
				File[] files = new File[fileList.length];
				for (int i = 0; i < fileList.length; i++) {
					String fileName = fileList[i];
					files[i] = new File(publisherDir, fileName);
				}
				return files;
			} else{
				trace.trace("FileList is null. Nothing to do.", TraceLevel.DEBUG);
				return null;
			}
		}finally{
			trace.trace("AgePubCleaningStrategy.getFileList done", TraceLevel.TRACE);
		}
	}
	
	/**
	 * Return the file filter to use. Overwrite if you want to change the default RegExpFileFilter.
	 * @param trace
	 * @return
	 */
	private FilenameFilter getFileFilter() {
		Calendar calc = Calendar.getInstance();
		calc.add(Calendar.SECOND, ageInSeconds);
		if (trace.getTraceLevel()>=TraceLevel.DEBUG){
			trace.trace("Current date: "+new Date(), TraceLevel.DEBUG);
			trace.trace("Searching for files last modified before: "+calc.getTime(), TraceLevel.DEBUG);
		}
		return new RegExpFileFilter(trace, calc.getTimeInMillis());
	}

	private void start() {
		synchronized (lock) {
			runningThread = new Thread(){
				public void run(){
					synchronized (lock) {
						while (true){
							try {
								lock.wait(testIntervalInSeconds*1000);
							} catch (InterruptedException e) {
								return;
							}
							if (isInterrupted())
								return;
							//Do the test
							File[] files = getFileList();
							if (files != null)
								for (int i = 0; i < files.length; i++) {
									deleteFiles(files[i]);						
								}
						}
					}
					
				}
			};
			runningThread.setDaemon(true);
			runningThread.setPriority(Thread.MIN_PRIORITY);
			runningThread.setName("AgePubCleaningThread");
			runningThread.start();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}

	/**
	 * Whenever the driver is shutting down, stop our monitor "gracefull".
	 * Note: the runningThread is a deamon thread, so it would stop anyhow.
	 */
	public void onShutdown(XDSResultDocument reasonXML) {
		synchronized (lock) {
			if (runningThread != null)
				runningThread.interrupt();
		}
		
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IPubFileCleanStrategy#onPreFile(java.io.File)
	 */
	public void onPreFile(File f) {
		//Nothing to do here
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IPubFileCleanStrategy#onPostFile(java.io.File)
	 */
	public void onPostFile(File f) {
		//Nothing to do here
	}

}
