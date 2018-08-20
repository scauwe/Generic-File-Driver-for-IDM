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

package info.vancauwenberge.filedriver.filestart;

import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.RangeConstraint;
import com.novell.nds.dirxml.driver.xds.StatusLevel;
import com.novell.nds.dirxml.driver.xds.StatusType;
import com.novell.nds.dirxml.driver.xds.XDSResultDocument;
import com.novell.nds.dirxml.driver.xds.util.StatusAttributes;
import com.novell.nds.dirxml.driver.xds.util.XDSUtil;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.IFileStartStrategy;
import info.vancauwenberge.filedriver.api.IShutdown;
import info.vancauwenberge.filedriver.api.ISubscriberFileListener;
import info.vancauwenberge.filedriver.api.ISubscriberShim;
import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

public class BasicNewFileDecider extends AbstractStrategy implements IFileStartStrategy,IShutdown,ISubscriberFileListener {
	/**
	 * positive seconds assumes that it needs to be converted to milliseconds, so it sets the max value
	 * to MAX_VALUE/1000
	 */
	private static final RangeConstraint positiveSecondsRange = new RangeConstraint(0, (Integer.MAX_VALUE/1000));

	private class InactivityMonitor extends Thread{
		boolean keepRunning = true;
		public InactivityMonitor(){
			setDaemon(true);
			setName("InactivityMonitor");
		}

		@Override
		public void run(){
			trace.trace("InactivityMonitor - Starting.",TraceLevel.TRACE);
			synchronized(subscriber.getFileLock()){
				while(keepRunning){
					try {
						trace.trace("InactivityMonitor - Waiting for "+(fileInactiveSaveInterval/1000)+" seconds of inactivity.",TraceLevel.TRACE);
						subscriber.getFileLock().wait(fileInactiveSaveInterval);
						trace.trace("InactivityMonitor - Awake after "+(fileInactiveSaveInterval/1000)+" seconds of inactivity.",TraceLevel.TRACE);
						if (keepRunning && subscriber.finishFile()) {
							trace.trace("InactivityMonitor - Closed file after "+(fileInactiveSaveInterval/1000)+" seconds of inactivity.",TraceLevel.TRACE);
						}
					} catch (final InterruptedException e) {
						trace.trace("InactivityMonitor - Interupted",TraceLevel.TRACE);
					} catch (final WriteException e) {
						trace.trace("InactivityMonitor - Unable to close file:"+e.getMessage(),TraceLevel.ERROR_WARN);
						Util.printStackTrace(trace, e);
					}
				}
			}
		}

		public void notifyActivity(){
			this.interrupt();
		}

		public void killMonitor(){
			keepRunning = false;
			this.interrupt();
		}

	}
	private class MaxFileAgeMonitor extends Thread{

		public MaxFileAgeMonitor(){
			super();
			setDaemon(true);
			setName("MaxFileAgeMonitor");
		}

		@Override
		public void run(){
			trace.trace("MaxFileAgeMonitor - Starting.",TraceLevel.TRACE);
			synchronized(subscriber.getFileLock()){
				try {
					trace.trace("MaxFileAgeMonitor - Waiting for "+(maxFileAge/1000)+" seconds after cration.",TraceLevel.TRACE);
					subscriber.getFileLock().wait(maxFileAge);
					trace.trace("MaxFileAgeMonitor - Awake after "+(maxFileAge/1000)+" seconds after cration.",TraceLevel.TRACE);
					if (subscriber.finishFile()) {
						trace.trace("MaxFileAgeMonitor - Closed file after "+(maxFileAge/1000)+" seconds of creation.",TraceLevel.TRACE);
					}
				} catch (final InterruptedException e) {
					trace.trace("MaxFileAgeMonitor - Interupted",TraceLevel.TRACE);
				} catch (final WriteException e) {
					trace.trace("MaxFileAgeMonitor - Unable to close file:"+e.getMessage(),TraceLevel.ERROR_WARN);
					Util.printStackTrace(trace, e);
				}
			}
			trace.trace("MaxFileAgeMonitor - Stopped.",TraceLevel.TRACE);
		};

		public void killMonitor(){
			synchronized (this) {
				this.interrupt();					
			}
		}
	}
	private enum Parameters implements IStrategyParameters{
		/**
		 * The file should not contain more then <i>nnn</i> records
		 */
		NEW_FILE_MAX_RECORDS{
			@Override
			public String getParameterName() {
				return "newFile_MaxRecords";
			}

			@Override
			public String getDefaultValue() {
				return "100";
			}

			@Override
			public DataType getDataType() {
				return DataType.INT;
			}

			@Override
			public Constraint[] getConstraints() {
				return new Constraint[]{RangeConstraint.NON_NEGATIVE};
			}
		},
		/**
		 * Close the file after <i>nnn</i> seconds of adding the first record
		 */
		NEW_FILE_MAX_FILE_AGE{
			@Override
			public String getParameterName() {
				return "newFile_MaxFileAge";
			}

			@Override
			public String getDefaultValue() {
				return "60";
			}

			@Override
			public DataType getDataType() {
				return DataType.INT;
			}

			@Override
			public Constraint[] getConstraints() {
				return new Constraint[]{positiveSecondsRange};
			}
		},
		/**
		 * Close the file after <i>nnn</i> seconds of inactivity
		 */
		NEW_FILE_INACTIVE_SAVE_INTERVAL{
			@Override
			public String getParameterName() {
				return "newFile_InactiveSaveInterval";
			}

			@Override
			public String getDefaultValue() {
				return "0";
			}

			@Override
			public DataType getDataType() {
				return DataType.INT;
			}

			@Override
			public Constraint[] getConstraints() {
				return new Constraint[]{positiveSecondsRange};
			}
		},
		/**
		 * Use given cron string to close the file 
		 */
		NEW_FILE_CRON_EXPRESSION{
			@Override
			public String getParameterName() {
				return "newFile_CronExpression";
			}

			@Override
			public String getDefaultValue() {
				return "";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}

			@Override
			public Constraint[] getConstraints() {
				return null;
			}
		},
		/**
		 * Field to manually indicate that a new file should be started or not
		 */
		NEW_FILE_FIELD{
			@Override
			public String getParameterName() {
				return "newFile_FieldName";
			}

			@Override
			public String getDefaultValue() {
				return "";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}

			@Override
			public Constraint[] getConstraints() {
				return null;
			}
		};

		@Override
		public abstract String getParameterName();
		@Override
		public abstract String getDefaultValue();
		@Override
		public abstract DataType getDataType();
		@Override
		public abstract Constraint[] getConstraints();

	}




	private String fieldName;
	private int maxRecordCount = 0;
	private int maxFileAge = 0;
	private MaxFileAgeMonitor maxFileAgeMonitor;
	private InactivityMonitor fileInactivityMonitor;

	private int fileInactiveSaveInterval;
	private ISubscriberShim subscriber;
	private String cronExpression;
	private Scheduler cronScheduler=null;
	private Trace trace;

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IStrategy#init(com.novell.nds.dirxml.driver.Trace, java.util.Map, info.vancauwenberge.filedriver.api.IDriver)
	 */
	@Override
	public void init(final Trace trace, final Map<String,Parameter> driverParams, final IDriver driver) throws Exception {
		this.trace = trace;

		this.subscriber=driver.getSubscriber();
		this.subscriber.addFileListener(this);
		maxRecordCount = getIntValueFor(Parameters.NEW_FILE_MAX_RECORDS,driverParams);
		//Integer.parseInt(driverParams.get(NEW_FILE_MAX_RECORDS).toString());
		maxFileAge = getIntValueFor(Parameters.NEW_FILE_MAX_FILE_AGE,driverParams)* 1000;
		//Integer.parseInt(driverParams.get(NEW_FILE_MAX_FILE_AGE).toString()) * 1000;
		fileInactiveSaveInterval = getIntValueFor(Parameters.NEW_FILE_INACTIVE_SAVE_INTERVAL,driverParams) * 1000;;
		//Integer.parseInt(driverParams.get(NEW_FILE_INACTIVE_SAVE_INTERVAL).toString()) * 1000;
		cronExpression = getStringValueFor(Parameters.NEW_FILE_CRON_EXPRESSION,driverParams);
		//driverParams.get(NEW_FILE_CRON_EXPRESSION).toString();
		fieldName = getStringValueFor(Parameters.NEW_FILE_FIELD,driverParams);
		//driverParams.get(NEW_FILE_FIELD).toString();

		//We can only initialize quartz when the rest is initialized.
		initQuartz();
		//Create file monitor if required:
		// if fileInactiveSaveInterval is set (>0)
		// and if the interval is smaller then the 'live' time of a file (fileSaveInterval)
		if (fileInactiveSaveInterval>0){
			if ((maxFileAge==0) | (fileInactiveSaveInterval<maxFileAge)){
				fileInactivityMonitor = new InactivityMonitor();
				fileInactivityMonitor.start();
			} else {
				trace.trace("Warn: file inactivity monitor set but not used since fileInactiveSaveInterval("+fileInactiveSaveInterval+") > maxFileAge("+maxFileAge+")", TraceLevel.ERROR_WARN);
			}
		}


		if (trace.getTraceLevel()>=TraceLevel.TRACE){
			trace.trace("Initialization completed:", TraceLevel.TRACE);
			trace.trace(" MaxRecordCount:"+maxRecordCount +((maxRecordCount==0)?"(disabled)":"(enabled)"), TraceLevel.TRACE);
			trace.trace(" maxFileAge:"+maxFileAge+((maxFileAge==0)?"(disabled)":"(enabled)"), TraceLevel.TRACE);
			trace.trace(" fileInactiveSaveInterval:"+fileInactiveSaveInterval+((fileInactivityMonitor==null)?"(disabled)":"(enabled)"), TraceLevel.TRACE);
			trace.trace(" cronExpression:"+cronExpression, TraceLevel.TRACE);
			trace.trace(" fieldName:"+fieldName, TraceLevel.TRACE);
		}


	}


	/*
	 * Initialize the quartz schedular if needed
	 */
	private void initQuartz () throws Exception {
		if (cronScheduler == null){
			//Trace trace = new Trace("");
			trace.trace("Initializing quartz for cron (if required)",TraceLevel.TRACE);

			if ((cronExpression != null) && !"".equals(cronExpression.trim())){
				try{
					trace.trace("Using cron string "+cronExpression,TraceLevel.TRACE);
					final CronTrigger cronTrigger = TriggerBuilder.newTrigger()
							.withIdentity("GenFileDriverCrontrigger")
							.withSchedule(CronScheduleBuilder.cronSchedule(cronExpression.trim()))
							.build();
					final Properties props = new Properties();
					props.put(StdSchedulerFactory.PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON, "true");//"org.quartz.scheduler.makeSchedulerThreadDaemon"
					props.put("org.quartz.threadPool.makeThreadsDaemons", "true");
					props.put("org.quartz.threadPool.threadCount", "1");
					final String uniqueId = UUID.randomUUID().toString();
					props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME,uniqueId);//"org.quartz.scheduler.instanceName"
					trace.trace("Creating Quartz schedular factory",TraceLevel.TRACE);
					final SchedulerFactory schFactory = new StdSchedulerFactory(props);
					cronScheduler = schFactory.getScheduler();
					cronScheduler.start();

					final JobDataMap map = new JobDataMap();
					map.put(ForceCloseJob.JOBMAP_SUBSCRIBER, subscriber);
					map.put(ForceCloseJob.JOBMAP_BASICNEWFILEDECIDER, this);
					trace.trace("Creating Quartz jobDetail",TraceLevel.TRACE);
					final JobDetail job = JobBuilder.newJob(ForceCloseJob.class)
							.withIdentity("GenerifFileDriver_"+uniqueId)
							.usingJobData(map)
							.build();

					trace.trace("Scheduling Quartz cron job:",TraceLevel.TRACE);
					cronScheduler.scheduleJob(job, cronTrigger);
					trace.trace("Cron will trigger first on: "+cronTrigger.getFireTimeAfter(new Date()),TraceLevel.TRACE);
				}catch(final Exception e){
					trace.trace("Error initializing Quartz cron schedular:"+e.getClass()+"-"+e.getMessage(),TraceLevel.ERROR_WARN);
					Util.printStackTrace(trace, e);
					throw e;
				}
			}else{
				trace.trace("No cron string defined. Skipping Quartz.",TraceLevel.TRACE);
			}

		}
	}


	/**
	 * Reschedule the maximum file age monitor (stop the old monitor and start a new one).
	 */
	/*	private void rescheduleMaxAgeMonitor(){
		//Only do something if fileInactiveSaveInterval is set (>0)
		// and if the interval is smaller then the 'live' time of a file (fileSaveInterval)
		if (maxFileAge>0){
			if (maxFileAgeMonitor!=null) //We had one before. Finish it.
				maxFileAgeMonitor.interrupt();
			else
			{
				maxFileAgeMonitor = new Thread(){
					public void run(){
						synchronized(subscriber.getFileLock()){
							while (true){
								try {
									subscriber.getFileLock().wait(maxFileAge);
									if (subscriber.finishFile())
										trace.trace("Closed file "+(maxFileAge/1000)+" seconds after creation.",TraceLevel.TRACE);
								} catch (InterruptedException e) {
									trace.trace(getName()+ " Interupted",TraceLevel.TRACE);
								} catch (WriteException e) {
									trace.trace("Unable to close file:"+e.getMessage());
								}
							}
						}
					}					
				};
				maxFileAgeMonitor.setDaemon(true);
				maxFileAgeMonitor.setName("MaxFileAgeMonitor");
				maxFileAgeMonitor.start();
			}
		}

	}*/
	/**
	 * Reschedule the inactivity monitor (stop the old monitor and start a new one).
	 */
	/*
	private void rescheduleInactivityMonitor(){
		//Only do something if fileInactiveSaveInterval is set (>0)
		// and if the interval is smaller then the 'live' time of a file (fileSaveInterval)
		if ((fileInactiveSaveInterval>0) & (fileInactiveSaveInterval<maxFileAge)){
			if (fileInactivityMonitor!=null) //We had one before. Finish it.
				fileInactivityMonitor.interrupt();
			else{
				//Save the file after nnn seconds of inactivity
				fileInactivityMonitor = new Thread(){
					public void run(){
						synchronized(subscriber.getFileLock()){
							while(true){
								try {
									subscriber.getFileLock().wait(fileInactiveSaveInterval);
									if (subscriber.finishFile())
										trace.trace("Closed file after "+(fileInactiveSaveInterval/1000)+" seconds of inactivity.",TraceLevel.TRACE);
								} catch (InterruptedException e) {
									trace.trace(getName()+ " Interupted",TraceLevel.TRACE);
								} catch (WriteException e) {
									trace.trace("Unable to close file:"+e.getMessage(),TraceLevel.ERROR_WARN);
								}
							}
						}
					}

				};
				fileInactivityMonitor.setDaemon(true);
				fileInactivityMonitor.setName("FileInactivityMonitor");
				fileInactivityMonitor.start();
			}
		}

	}*/


	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.INewFileDecider#requiresNewFile(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 * Returns true when nnn records have been saved in the file (nnn=value of parameter NEW_FILE_MAX_RECORDS)
	 */
	@Override
	public boolean requiresNewFile(final Map<String,String> record) {
		trace.trace("requiresNewFile?",TraceLevel.TRACE);
		synchronized (subscriber.getFileLock()){
			if (subscriber.getCurrentFileRecordCount()<=0){//always return true when no records have been saved yet!
				trace.trace("No records have been written yet to the file.",TraceLevel.TRACE);
				return true;				
			}
			else
			{
				final String value = record.get(fieldName);
				if (value!=null){//If we have a value, use that algorithm
					if ("true".equals(value)){
						trace.trace("Closing file after setting '"+fieldName+"' to 'true'.",TraceLevel.TRACE);
						return true;
					}
					else
					{
						trace.trace("Not closing file after explicit setting of '"+fieldName+"' to NOT 'true'.",TraceLevel.TRACE);
						return false;
					}
				}else{//We do not have a value, use the maxFileRecords algorithm (if not zero)
					if ((maxRecordCount != 0) && (subscriber.getCurrentFileRecordCount() >= maxRecordCount)){
						trace.trace("Closing file after "+maxRecordCount+" records have been added.",TraceLevel.TRACE);
						return true;
					}
					else
					{
						trace.trace("Not required to close the file.",TraceLevel.TRACE);
						return false;
					}
				}
			}
		}
	}



	public String getCronExpression() {
		return cronExpression;
	}


	@Override
	public void onShutdown(final XDSResultDocument reasonXML) {
		subscriber.removeFileListener(this);
		if (maxFileAgeMonitor!=null) {
			maxFileAgeMonitor.killMonitor();
		}
		if (fileInactivityMonitor!=null) {
			fileInactivityMonitor.killMonitor();
		}

		if (cronScheduler != null) {
			try {
				cronScheduler.shutdown();
			} catch (final SchedulerException e) {
				final StatusAttributes attrs = StatusAttributes.factory(StatusLevel.FATAL,
						StatusType.DRIVER_STATUS,
						null); //event-id
				XDSUtil.appendStatus(reasonXML, //do to append to
						attrs, //status attribute values
						null, //description
						e, //exception
						true, //append stack trace?
						null); //xml to append
				Util.printStackTrace(new Trace(""), e);
			}
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}

	/*
	 * (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.ISubscriberFileListener#afterFileOpened(info.vancauwenberge.filedriver.api.ISubscriberFileListener.FileEvent)
	 */
	@Override
	public void afterFileOpened(final FileEvent event) {
		if (maxFileAge>0){
			if (maxFileAgeMonitor!=null) {
				maxFileAgeMonitor.killMonitor();
			}
			maxFileAgeMonitor = new MaxFileAgeMonitor();
			maxFileAgeMonitor.start();
		}

	}

	/*
	 * (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.ISubscriberFileListener#afterFileClose(info.vancauwenberge.filedriver.api.ISubscriberFileListener.FileEvent)
	 */
	@Override
	public void afterFileClose(final FileEvent event) {
		if (maxFileAgeMonitor!=null) {
			maxFileAgeMonitor.killMonitor();
		}
	}


	/*
	 * (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.ISubscriberFileListener#afterRecordAdded(info.vancauwenberge.filedriver.api.ISubscriberFileListener.FileEvent)
	 */
	@Override
	public void afterRecordAdded(final FileEvent event) {
		if ((maxRecordCount != 0) && (subscriber.getCurrentFileRecordCount() >= maxRecordCount)){
			trace.trace(maxRecordCount + " records have been added. Trying to close the file.",TraceLevel.TRACE);
			try {
				if (subscriber.finishFile()) {
					trace.trace("Closed file after "+maxRecordCount+" records have been added.",TraceLevel.TRACE);
				}
			} catch (final WriteException e) {
				Util.printStackTrace(trace, e);
			}

		}
	}


	@Override
	public void beforeRecordAdded(final FileEvent event) {
		if (fileInactivityMonitor!=null) {
			fileInactivityMonitor.notifyActivity();
		}
	}
}
