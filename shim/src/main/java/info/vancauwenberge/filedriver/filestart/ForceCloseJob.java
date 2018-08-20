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

import info.vancauwenberge.filedriver.api.ISubscriberShim;
import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.novell.nds.dirxml.driver.Trace;

public class ForceCloseJob implements org.quartz.Job{
	
	public static final String JOBMAP_SUBSCRIBER = "callback";
	public static final String JOBMAP_BASICNEWFILEDECIDER = "BasicNewFileDecider";
	
	public ForceCloseJob(){
		
	}
	public void execute(JobExecutionContext arg0)
			throws JobExecutionException {
		Trace trace = new Trace("CronJob");		
		trace.trace("Cron triggered.");
		try{
			ISubscriberShim subscriberShim = (ISubscriberShim) arg0.getJobDetail().getJobDataMap().get(JOBMAP_SUBSCRIBER);
			BasicNewFileDecider master = (BasicNewFileDecider) arg0.getJobDetail().getJobDataMap().get(JOBMAP_BASICNEWFILEDECIDER);
			trace.trace("Cron executing");
			try{
				if (subscriberShim.finishFile())
					trace.trace("Closed file due to cron "+master.getCronExpression());
			} catch (WriteException e) {
				trace.trace("Unable to close file:"+e.getMessage());
				Util.printStackTrace(trace, e);
			}
		}catch (RuntimeException e) {
			Util.printStackTrace(trace, e);
			throw e;
		}
		trace.trace("Cron finished.",TraceLevel.TRACE);
	}
}
