/**
 *
 */
package info.vancauwenberge.filedriver.filestart;

import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.novell.nds.dirxml.driver.Trace;

import info.vancauwenberge.filedriver.api.ISubscriberShim;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

/**
 * We move te quartz/crons tuff to a seperate class. The imports will otherwise
 * block when not found and Queartz not used. We do nto want to force users to
 * install the quartz libraries.
 *
 * @author scauwe
 *
 */
public class QuartzUtil {
	/**
	 * Initialize quartz in another class to avoid imports of quartz when no
	 * used.
	 * 
	 * @param trace
	 * @param cronExpression
	 * @param subscriber
	 * @param callabck
	 * @return
	 * @throws Exception
	 */
	static org.quartz.Scheduler initQuartz(Trace trace, String cronExpression, ISubscriberShim subscriber,
			BasicNewFileDecider callabck) throws Exception {
		trace.trace("Initializing quartz for cron (if required)", TraceLevel.TRACE);
		Scheduler cronScheduler = null;
		try {
			trace.trace("Using cron string " + cronExpression, TraceLevel.TRACE);
			final CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity("GenFileDriverCrontrigger")
					.withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build();
			final Properties props = new Properties();
			props.put(StdSchedulerFactory.PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON, "true");// "org.quartz.scheduler.makeSchedulerThreadDaemon"
			props.put("org.quartz.threadPool.makeThreadsDaemons", "true");
			props.put("org.quartz.threadPool.threadCount", "1");
			final String uniqueId = UUID.randomUUID().toString();
			props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, uniqueId);// "org.quartz.scheduler.instanceName"
			trace.trace("Creating Quartz schedular factory", TraceLevel.TRACE);
			final SchedulerFactory schFactory = new StdSchedulerFactory(props);
			cronScheduler = schFactory.getScheduler();
			cronScheduler.start();

			final JobDataMap map = new JobDataMap();
			map.put(ForceCloseJob.JOBMAP_SUBSCRIBER, subscriber);
			map.put(ForceCloseJob.JOBMAP_BASICNEWFILEDECIDER, callabck);
			trace.trace("Creating Quartz jobDetail", TraceLevel.TRACE);
			final JobDetail job = JobBuilder.newJob(ForceCloseJob.class).withIdentity("GenerifFileDriver_" + uniqueId)
					.usingJobData(map).build();

			trace.trace("Scheduling Quartz cron job:", TraceLevel.TRACE);
			cronScheduler.scheduleJob(job, cronTrigger);
			trace.trace("Cron will trigger first on: " + cronTrigger.getFireTimeAfter(new Date()), TraceLevel.TRACE);
		} catch (final Exception e) {
			trace.trace("Error initializing Quartz cron schedular:" + e.getClass() + "-" + e.getMessage(),
					TraceLevel.ERROR_WARN);
			Util.printStackTrace(trace, e);
			throw e;
		}
		return cronScheduler;
	}
}
