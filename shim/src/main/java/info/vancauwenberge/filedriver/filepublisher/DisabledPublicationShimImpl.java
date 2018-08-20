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
package info.vancauwenberge.filedriver.filepublisher;

import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.util.Util;

import java.util.HashMap;
import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.XmlCommandProcessor;
import com.novell.nds.dirxml.driver.XmlDocument;
import com.novell.nds.dirxml.driver.XmlQueryProcessor;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.StatusLevel;
import com.novell.nds.dirxml.driver.xds.StatusType;
import com.novell.nds.dirxml.driver.xds.XDSClassDefElement;
import com.novell.nds.dirxml.driver.xds.XDSHeartbeatDocument;
import com.novell.nds.dirxml.driver.xds.XDSInitDocument;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;
import com.novell.nds.dirxml.driver.xds.XDSParseException;
import com.novell.nds.dirxml.driver.xds.XDSQueryResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSStatusElement;
import com.novell.nds.dirxml.driver.xds.util.StatusAttributes;
import com.novell.nds.dirxml.driver.xds.util.XDSUtil;

public class DisabledPublicationShimImpl implements IPublisherImplStrategy, XmlQueryProcessor {
//extends PublisherChannelShim implements
//		PublicationShim, XmlQueryProcessor {

	

    /**
     *  Variable used to control how often the thread in <code>start()</code>
     *  wakes up to send a document to the DirXML engine. Value is in milliseconds.
     *  <p>
     *  @see #start(XmlCommandProcessor)
     */
	private long heartbeatInterval = 0;


    /**
     *  Should a heartbeat document be sent?
     *  <p>
     *  @see #start(XmlCommandProcessor)
     */
	private boolean doHeartbeat;

	private boolean shutdown=false;
	private final Object semaphore = new Object();


	private IDriver driver;


	private Trace trace;

    /**
     *  Constructor.
     *  <p>
     *  @param someDriver a reference to this driver instance;
     *      must not be <code>null</code>
     */
    public DisabledPublicationShimImpl()
    {
    	//super(someDriver,TRACE_SUFFIX);
    }//SkeletonPublicationShim(SkeletonDriverShim)


    /**
     * Initialize this publisher implementation
     */
	public XmlDocument init(Trace trace, Map<String,Parameter> commonArgs, IDriver driver, XmlDocument initXML) throws Exception{
    	this.driver = driver;
    	this.trace = trace;
    	
        trace.trace("init", 1);

        XDSResultDocument result;
        StatusAttributes attrs;

        //create result document for reporting status to the DirXML engine
        result = driver.newResultDoc();

        try
        {
        	HashMap<String,Parameter> allParams = new HashMap<String,Parameter>();
            XDSInitDocument init;
            XDSStatusElement status;
            Parameter param;

            //parse initialization document
            init = new XDSInitDocument(initXML);

            //get any publisher options from init doc
            //Map<String,Parameter> pubParams = getDefaultParameterDefs();//getShimParameters();
            //pubParams.putAll(commonArgs);

            //This implementation does not have any other parameters but the common ones
            Map<String,Parameter> pubParams = commonArgs;
            
            init.parameters(pubParams);
            System.out.println(pubParams);
            
            //Add the general driver parameters to the publisher parameters
            pubParams.putAll(driver.getDriverParams());

            //get the heartbeat interval that may have been passed in
            param = (Parameter) pubParams.get(PUB_HEARTBEAT_INTERVAL);
            //our heartbeatInterval value is in minutes, Object.wait(long) takes milliseconds => convert
            heartbeatInterval  = XDSUtil.toMillis(param.toInteger().intValue()*60);
            
            doHeartbeat = (heartbeatInterval > 0);
            
            //construct a driver filter for the publication shim to use for
            //filtering application events in start(); in an actual driver,
            //the publisher would use the filter to filter events from the
            //application to avoid publishing unnecessary events to the DirXML
            //engine
            //
            //NOTE: the skeleton publisher doesn't actually make use of the
            //filter, but this code is here to illustrate how to get the
            //publisher filter from the init document

            //perform any other initialization that might be required

            //append a successful <status> element to the result doc
            attrs = StatusAttributes.factory(StatusLevel.SUCCESS,
                                             StatusType.DRIVER_STATUS,
                                             null); //event-id
            status = XDSUtil.appendStatus(result, //doc to append to
                                          attrs,
                                          null); //description
            
            //append the parameter values the publisher is actually using
            allParams.putAll(pubParams);
            status.parametersAppend(allParams);
        }//try
        catch (Exception e) //don't want to catch Error class with Throwable
        {
        	Util.printStackTrace(trace, e);
            //e instance of XDSException:
            //
            //  init document is malformed or invalid -- or --
            //  it is missing required parameters or contains
            //  illegal parameter values

            //e instance of RuntimeException:

            //  e.g., NullPointerException

            attrs = StatusAttributes.factory(StatusLevel.FATAL,
                                             StatusType.DRIVER_STATUS,
                                             null); //event-id
            XDSUtil.appendStatus(result, //doc to append to
                                 attrs, //status attribute values
                                 null, //description
                                 e, //exception
                                 XDSUtil.appendStackTrace(e), //append stack trace?
                                 initXML); //xml to append
        }//catch

        //return result doc w/ status to DirXML
        return result.toXML();
	}

	  public void shutdown(XDSResultDocument reasonXML)
	  {
	    this.trace.trace("shutdown", 3);

	    this.shutdown = true;

	    synchronized (this.semaphore)
	    {
	      this.semaphore.notifyAll();
	    }
	  }

	/*
	 * Only send hartbeat event every xxx seconds.
	 * @see com.novell.nds.dirxml.driver.PublicationShim#start(com.novell.nds.dirxml.driver.XmlCommandProcessor)
	 */
	public XmlDocument start(XmlCommandProcessor processor) {
	    this.trace.trace("start", 3);

	    XDSResultDocument result = driver.newResultDoc();
	    XmlDocument heartbeat = createHeartbeatDoc();

	    StatusAttributes shutdownAttrs = StatusAttributes.factory(StatusLevel.SUCCESS, 
	      StatusType.DRIVER_STATUS, 
	      null);

	    try
	    {
	      while (!this.shutdown)
	      {
	        try
	        {
	        	if (this.doHeartbeat)
	            {
	        		this.trace.trace("sending heartbeat", 2);
	        		processor.execute(heartbeat, this);
	        		this.trace.trace("Sleeping for " + heartbeatInterval/ 1000L + " seconds", 2);
		            if (!this.shutdown)
		            	synchronized (this.semaphore){
		            		this.semaphore.wait(heartbeatInterval);
		            	}
	            }else{//Wait until shutdown
	            	synchronized (this.semaphore){
	            		this.semaphore.wait();
	            	}
	            }
	        }
	        catch (InterruptedException ie)
	        {
	          Util.printStackTrace(this.trace, ie);
	        }

	      }

	      XDSUtil.appendStatus(result, 
	        shutdownAttrs, 
	        null);
	    }
	    catch (Exception e)
	    {
	      Util.printStackTrace(this.trace, e);
	      shutdownAttrs.setLevel(StatusLevel.FATAL);
	      XDSUtil.appendStatus(result, 
	        shutdownAttrs, 
	        null, 
	        e, 
	        true, 
	        null);
	    }
	    finally
	    {
	      this.trace.trace("stopping...", 2);
	    }

	    return result.toXML();
	}


	private XmlDocument createHeartbeatDoc() {
		XDSHeartbeatDocument heartbeatXDS = new XDSHeartbeatDocument();
	    driver.appendSourceInfo(heartbeatXDS);
	    return heartbeatXDS.toXML();
	}

	/*
	 * (non-Javadoc)
	 * @see com.novell.nds.dirxml.driver.XmlQueryProcessor#query(com.novell.nds.dirxml.driver.XmlDocument)
	 */
	public XmlDocument query(XmlDocument arg0) {
	    this.trace.trace("query", 1);

	    XDSQueryResultDocument result = new XDSQueryResultDocument();
	    driver.appendSourceInfo(result);

	    StatusAttributes attrs = StatusAttributes.factory(StatusLevel.ERROR, 
	      StatusType.DRIVER_GENERAL, 
	      null);
	    XDSUtil.appendStatus(result, 
	      attrs, 
	      "Query not implemented");
	    return result.toXML();
	}

	public void extendSchema(XDSClassDefElement userClassDef,
			XmlDocument initXML) throws XDSParseException,
			XDSParameterException {
		//No schema extention on disabled publisher
	}

}
