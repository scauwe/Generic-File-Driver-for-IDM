/*******************************************************************************
 * Copyright (c) 2007-2016 Stefaan Van Cauwenberge
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
package info.vancauwenberge.filedriver.shim;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.novell.nds.dirxml.driver.SubscriptionShim;
import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.XmlDocument;
import com.novell.nds.dirxml.driver.XmlQueryProcessor;
import com.novell.nds.dirxml.driver.xds.CommandElement;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.ElementImpl;
import com.novell.nds.dirxml.driver.xds.NonXDSElement;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.QueryScope;
import com.novell.nds.dirxml.driver.xds.RequiredConstraint;
import com.novell.nds.dirxml.driver.xds.StateDocument;
import com.novell.nds.dirxml.driver.xds.StatusLevel;
import com.novell.nds.dirxml.driver.xds.StatusType;
import com.novell.nds.dirxml.driver.xds.ValueType;
import com.novell.nds.dirxml.driver.xds.XDS;
import com.novell.nds.dirxml.driver.xds.XDSAddAssociationElement;
import com.novell.nds.dirxml.driver.xds.XDSAddAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSAddElement;
import com.novell.nds.dirxml.driver.xds.XDSAddValueElement;
import com.novell.nds.dirxml.driver.xds.XDSAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSAuthenticationInfoElement;
import com.novell.nds.dirxml.driver.xds.XDSCommandDocument;
import com.novell.nds.dirxml.driver.xds.XDSCommandResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSComponentElement;
import com.novell.nds.dirxml.driver.xds.XDSException;
import com.novell.nds.dirxml.driver.xds.XDSInitDocument;
import com.novell.nds.dirxml.driver.xds.XDSInitParamsElement;
import com.novell.nds.dirxml.driver.xds.XDSInstanceElement;
import com.novell.nds.dirxml.driver.xds.XDSModifyAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSModifyElement;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;
import com.novell.nds.dirxml.driver.xds.XDSParseException;
import com.novell.nds.dirxml.driver.xds.XDSQueryDocument;
import com.novell.nds.dirxml.driver.xds.XDSQueryElement;
import com.novell.nds.dirxml.driver.xds.XDSQueryResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSReadAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSStatusElement;
import com.novell.nds.dirxml.driver.xds.XDSSubscriberStateElement;
import com.novell.nds.dirxml.driver.xds.XDSValueElement;
import com.novell.nds.dirxml.driver.xds.util.StatusAttributes;
import com.novell.nds.dirxml.driver.xds.util.XDSUtil;

import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.IFileNameStrategy;
import info.vancauwenberge.filedriver.api.IFileStartStrategy;
import info.vancauwenberge.filedriver.api.IFileWriteStrategy;
import info.vancauwenberge.filedriver.api.IPostProcessStrategy;
import info.vancauwenberge.filedriver.api.IShutdown;
import info.vancauwenberge.filedriver.api.IStrategy;
import info.vancauwenberge.filedriver.api.ISubscriberFileListener;
import info.vancauwenberge.filedriver.api.ISubscriberShim;
import info.vancauwenberge.filedriver.api.ISubscriberStrategy;
import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.filename.SimpleDateFormatFileNameStrategy;
import info.vancauwenberge.filedriver.filepostprocess.ExternalExec;
import info.vancauwenberge.filedriver.filestart.BasicNewFileDecider;
import info.vancauwenberge.filedriver.filewriter.DummyFileWriter;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;
import info.vancauwenberge.filedriver.util.Errors;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;
import info.vancauwenberge.filedriver.util.XDSInstanceDocument;


/**
 *  A basic skeleton for implementing the <code>SubscriptionShim</code>.
 *  <p>
 *  The <code>SubscriptionShim</code> defines an interface for an application
 *  driver to receive commands from the DirXML engine. These commands must be
 *  executed in the application on behalf of the DirXML engine.
 */
public class FileDriverSubscriptionShim
extends ChannelShim
implements SubscriptionShim, ISubscriberShim
{

	private final class ShutdownHook extends Thread {
		@Override
		public void run(){
			try {
				finishFile();
			} catch (final Throwable e) {
				e.printStackTrace();
			}
		}
	}


	/*
	 * Enum holding the general subscriber parameters
	 */
	private enum Parameters {
		/**
		 * java class that generates a new file name.
		 */
		SUB_FILE_NAME_STRATEGY("sub_FileNameStrategy",RequiredConstraint.REQUIRED,SimpleDateFormatFileNameStrategy.class.getName()),
		/**
		 * java class that determines when a new file must be started.
		 */
		SUB_FILE_START_STRATEGY("sub_FileStartStrategy",RequiredConstraint.REQUIRED,BasicNewFileDecider.class.getName()),
		/**
		 * java class strategy for writing a file
		 */
		SUB_FILE_WRITE_STRATEGY("sub_FileWriteStrategy",RequiredConstraint.REQUIRED,DummyFileWriter.class.getName()),
		/**
		 * Temporary work folder for the files while they are being created
		 */
		SUB_WORK_DIR("sub_WorkDir",RequiredConstraint.REQUIRED,"/var/opt/temp"),
		/**
		 * Path where the file will be moved to after completion
		 */
		SUB_DEST_DIR("sub_DestFolder",RequiredConstraint.REQUIRED,"/var/opt"),
		/**
		 * Path where a local copy of the file will be stored (as well as on the dest folder). Note: failing to copy this file
		 * will not generate an error or fatal status message. This is not guaranteed.
		 */
		SUB_LOCAL_COPY_PATH("sub_LocalCopyPath",null,""),
		SUB_FILE_POSTPROCESS_STRATEGY("sub_PostProcessStrategy",RequiredConstraint.REQUIRED,ExternalExec.class.getName());
		private String name;
		private String defaultValue;
		private Constraint constraint;
		Parameters(final String name, final Constraint constraint, final String defaultValue){
			this.name=name;
			this.defaultValue = defaultValue;
			this.constraint = constraint;
		}


		public String getValueFor(final Map<String,Parameter> subParams){
			return subParams.get(name).toString();
		}


		public static Map<String,Parameter> getIDMParameters(){
			final Parameters[] values = Parameters.values();
			final Map<String,Parameter> result = new HashMap<String, Parameter>(values.length);
			for (final Parameters subscriptionShimParameter : values) {
				final Parameter param = new Parameter(subscriptionShimParameter.name, //tag name
						subscriptionShimParameter.defaultValue, //default value (optional)
						DataType.STRING); //data type
				if (subscriptionShimParameter.constraint != null) {
					param.add(subscriptionShimParameter.constraint);
				}
				result.put(subscriptionShimParameter.name, param);
			}
			return result;

		}
	}

	//this constant identifies the channel/module being traced
	//  (e.g., Skeleton Driver\Driver)
	static private final String TRACE_SUFFIX = "ST";

	private static final String STATE_ADD_COUNT = "addCount";

	//MODIFY:  put your driver's activation id and activation level here
	/** This driver's activation id. */
	static private final String DRIVER_ID_VALUE = "SVCGENFILE";
	/** This driver's activation level. */
	static private final String DRIVER_MIN_ACTIVATION_VERSION = "0";

	private enum Strategies{
		FILENAMER(Parameters.SUB_FILE_NAME_STRATEGY,IFileNameStrategy.class), 
		FILESTARTER(Parameters.SUB_FILE_START_STRATEGY, IFileStartStrategy.class),
		FILEWRITER(Parameters.SUB_FILE_WRITE_STRATEGY, IFileWriteStrategy.class),
		FILEPOSTPROCESS(Parameters.SUB_FILE_POSTPROCESS_STRATEGY, IPostProcessStrategy.class); 
		private Parameters parameter;
		private Class<?> interfaceClass;

		Strategies(final Parameters paramDef, final Class<?> interfaceClass){
			this.parameter = paramDef;
			this.interfaceClass = interfaceClass;
		}
	}
	private final Map<Strategies, IStrategy> strategyMap = new EnumMap<Strategies, IStrategy>(Strategies.class);
	/*    
    private IFileNameStrategy fileNamer;

	private IFileStartStrategy fileStarter;

	private IFileWriteStrategy fileWriter;
	 */
	private boolean isFileOpen = false;
	private final List<ISubscriberFileListener> fileListeners = new ArrayList<ISubscriberFileListener>();

	private final Object fileLock = new Object();

	private String workDir;

	private String destDir;

	private String localCopyDir;

	private String newFileName;

	private ShutdownHook shutdownHook;

	/*
	 * The number of records already in the current file
	 */
	private int currentFileRecordCount;
	/*
	 * The total number of records since the life of the driver. This is written as state info after every record added. 
	 */
	private int totalRecordCount;

	/**
	 * Holder for the connection parameters: username, password and connect string
	 */
	private ConnectionInfo connectInfo=null;


	/**
	 *  Constructor.
	 *  <p>
	 *  @param someDriver a reference to this driver instance;
	 *      must not be <code>null</code>
	 */
	public FileDriverSubscriptionShim(final GenericFileDriverShim someDriver)
	{
		super(someDriver,TRACE_SUFFIX);

		totalRecordCount = -1;
		currentFileRecordCount = -1;
	}//SkeletonSubscriptionShim(SkeletonDriverShim)


	/**
	 *  A non-interface method that specifies the parameters
	 *  this SubscriptionShim is expecting.
	 */
	@Override
	protected Map<String,Parameter> getParameterDefs()
	{
		return Parameters.getIDMParameters();
		//MODIFY:  construct parameter descriptors here for your
		//  subscriber

		//The XDS.jar library automatically checks parameter
		//data types for you.  When a RequiredConstraint
		//is added to a parameter, the library will check init documents
		//to ensure the parameter is present and has a value.  When you
		//add RangeConstraints or EnumConstraints to a parameter, the
		//library will check parameter values to see if they satisfy these
		//constraints.

		/*        Parameter param;
        HashMap<String,Parameter> paramDefs = new HashMap<String,Parameter>(6);

        //Add file namer as param
        param = new Parameter(SUB_FILE_NAME_STRATEGY, //tag name
                DEFAULT_FILE_NAME_STRATEGY, //default value (optional)
                DataType.STRING); //data type
        param.add(RequiredConstraint.REQUIRED);
        paramDefs.put(param.tagName(), param);

        //Add file starter as param
        param = new Parameter(SUB_FILE_START_STRATEGY, //tag name
                DEFAULT_FILE_START_STRATEGY, //default value (optional)
                DataType.STRING); //data type
        param.add(RequiredConstraint.REQUIRED);
        paramDefs.put(param.tagName(), param);

        //Add file write as param
        param = new Parameter(SUB_FILE_WRITE_STRATEGY, //tag name
                DEFAULT_FILE_WRITE_STRATEGY, //default value (optional)
                DataType.STRING); //data type
        param.add(RequiredConstraint.REQUIRED);        
        paramDefs.put(param.tagName(), param);

        param = new Parameter(SUB_WORK_DIR,
                DEFAULT_WORK_DIR,
                DataType.STRING);
        param.add(RequiredConstraint.REQUIRED);
        paramDefs.put(param.tagName(), param);

        param = new Parameter(SUB_DEST_DIR,
                DEFAULT_DEST_DIR,
                DataType.STRING);
        param.add(RequiredConstraint.REQUIRED);
        paramDefs.put(param.tagName(), param);

        return paramDefs;*/
	}//setSubParams():void


	/**
	 * Initialize the state of subscriber (if any). Just keeping a record of the total number of records processed.
	 * @param init
	 */
	private void initState(final XDSInitDocument init) {
		final XDSInitParamsElement subOptionsParam = init.extractInitParamsElement();
		final XDSSubscriberStateElement subOptions = subOptionsParam.extractSubscriberStateElement();

		if (subOptions != null){
			try{
				final String strAddCount = subOptions.paramText(STATE_ADD_COUNT);
				if (strAddCount == null){
					trace.trace("init - Initial driver boot (no addCount found)",TraceLevel.DEBUG);
				}else{
					try{
						totalRecordCount = Integer.parseInt(strAddCount);
					}catch(final NumberFormatException e){
						trace.trace("init - addCount was not a number. Resetting to 0",TraceLevel.ERROR_WARN);
						totalRecordCount = 0;
					}
					trace.trace("init - Written "+totalRecordCount+" records already in the past...",TraceLevel.DEBUG);
				}
			}catch (final XDSParameterException e) {
				trace.trace("init - unable to read addCount("+e.getMessage()+"). Resetting to 0",TraceLevel.ERROR_WARN);				
				totalRecordCount = 0;
			}
		}else{
			trace.trace("init - Initial driver boot (no subscriber state found)",TraceLevel.DEBUG);            	
		}
	}

	/**
	 * Create and initialize a new Strategy
	 * @param initDocument
	 * @param className
	 * @param allParams This map will be updated with the parametervalues from this ISubscriberStrategy
	 * @return
	 * @throws Exception 
	 */
	private ISubscriberStrategy initStrategy(final XDSInitDocument initDocument, final String className, final Map<String,Parameter> allParams) throws Exception{
		final ISubscriberStrategy aStrategy = (ISubscriberStrategy) Class.forName(className).newInstance();
		//Get the required parameters for this IStrategy
		final Map<String,Parameter> strategyParameters = aStrategy.getParameterDefinitions();

		//initDocument.parameters cannot handle null parameter object.
		if ((strategyParameters != null) && !strategyParameters.isEmpty()){
			//Update the map with the values from the initDocument
			initDocument.parameters(strategyParameters);

			//Add the parameters of this IStrategy to the allParameters map
			allParams.putAll(strategyParameters);

			//Add the general driver parameter (eg schema)
			strategyParameters.putAll(driver.getDriverParams());
		}

		//Initialize the strategy
		final Trace trace = new Trace(driver.getDriverInstanceName()+"\\"+aStrategy.getClass().getSimpleName());
		aStrategy.init(trace,strategyParameters, driver);
		return aStrategy;
	}

	/**
	 * Populate the strategy map with all configured strategies.
	 * @param init
	 * @param pubParams
	 * @throws Exception
	 */
	private void initStrategyMap(final XDSInitDocument init, final Map<String, Parameter> pubParams, final Map<String, Parameter> allParams) throws Exception {
		for (final Strategies strategy : Strategies.values()) {
			trace.trace("init - Creating strategy: "+strategy);
			final Parameter param = pubParams.get(strategy.parameter.name);
			final IStrategy strategyInstance = initStrategy(init, param.toString(), allParams);
			if (strategy.interfaceClass.isAssignableFrom(strategyInstance.getClass())) {
				strategyMap.put(strategy, strategyInstance);
			} else{
				throw new ClassCastException(param.toString() + " cannot be casted to "+strategy.interfaceClass);
			}
		}
	}

	/**
	 *  <code>init</code> will be called before the first invocation of
	 *  <code>execute</code>.
	 *  <p>
	 *  In general, application connectivity should be handled in
	 *  <code>execute(XmlDocument, XmlQueryProcessor)</code> so a driver
	 *  can start when the application is down.
	 *  <p>
	 *  @param initXML XML document that contains the subscriber
	 *      initialization parameters and state
	 *  @return an XML document containing status messages for this
	 *      operation
	 */
	@Override
	public XmlDocument init(final XmlDocument initXML)
	{
		//MODIFY:  initialize your driver here

		//example initialization document:

		this.shutdownHook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product version="1.1a">DirXML</product>
                <contact>Novell, Inc.</contact>
            </source>
            <input>
                <init-params src-dn="\NEW_DELL_TREE\NOVELL\Driver Set\Skeleton Driver (Java, XDS)\Subscriber">
                    <authentication-info>
                        <server>server.app:400</server>
                        <user>User1</user>
                    </authentication-info>
                    <driver-filter>
                        <allow-class class-name="User">
                            <allow-attr attr-name="Surname"/>
                            <allow-attr attr-name="Telephone Number"/>
                            <allow-attr attr-name="Given Name"/>
                        </allow-class>
                    </driver-filter>
                    <subscriber-options>
                <sub-1 display-name="Sample Subscriber option">String for Subscriber</sub-1>
            </subscriber-options>
                </init-params>
            </input>
        </nds>
		 */

		//example result document:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product build="20021214_0304" instance="Skeleton Driver (Java, XDS)" version="1.1">DirXML Skeleton Driver (Java, XDS)</product>
                <contact>My Company Name</contact>
            </source>
            <output>
                <status level="success" type="driver-status">
                    <parameters>
                        <current-association display-name="current-association">1</current-association>
                        <sub-1 display-name="Sample Subscriber option">String for Subscriber</sub-1>
                    </parameters>
                </status>
            </output>
        </nds>
		 */

		trace.trace("init", 1);

		XDSResultDocument result;
		StatusAttributes attrs;

		//create result document for reporting status to the DirXML engine
		result = newResultDoc();

		try
		{
			final HashMap<String,Parameter> allParams = new HashMap<String,Parameter>();
			//parse initialization document
			final XDSInitDocument initDocument = new XDSInitDocument(initXML);

			//Get the connection information
			final XDSInitParamsElement iniutParams = initDocument.extractInitParamsElement();

			final XDSAuthenticationInfoElement auth = iniutParams.extractAuthenticationInfoElement();
			if (auth != null){
				this.connectInfo = new ConnectionInfo(auth.extractPasswordText(), auth.extractUserText(),auth.extractServerText());
			}

			XDSStatusElement status;

			//get subscriber parameters from init doc
			final Map<String,Parameter> subParams = getParameterDefs();//getShimParameters();
			initDocument.parameters(subParams);
			//Add the general driver parameters to the publisher parameters
			subParams.putAll(driver.getDriverParams());
			trace.trace("Mergde parameters:"+subParams);

			//get any state info from the init doc; if this is the first
			//  time the driver is started, we'll get the default
			//addCount = ((Parameter) subParams.get(STATE_ADD_COUNT)).toInteger().intValue();
			initState(initDocument);
			initStrategyMap(initDocument, subParams, allParams);
			/*
            //The File Namer strategy
            trace.trace("init - Creating fileNamer object");
            fileNamer = (IFileNameStrategy) initStrategy(
            		initDocument, 
            		Parameters.SUB_FILE_NAME_STRATEGY.getValueFor(subParams), 
            		allParams);

            //The FileStart strategy
            trace.trace("init - Creating fileStart object");
            fileStarter = (IFileStartStrategy) initStrategy(
            		initDocument, 
            		Parameters.SUB_FILE_START_STRATEGY.getValueFor(subParams), 
            		allParams);

            //The FileWrite strategy
            trace.trace("init - Creating fileWrite object");
            fileWriter = (IFileWriteStrategy) initStrategy(
            		initDocument, 
            		Parameters.SUB_FILE_WRITE_STRATEGY.getValueFor(subParams), 
            		allParams);
			 */
			//Work dir
			workDir = Parameters.SUB_WORK_DIR.getValueFor(subParams);
			if (!workDir.endsWith(File.separator)) {
				workDir = workDir + File.separatorChar;
			}

			//Local copy directory
			localCopyDir = Parameters.SUB_LOCAL_COPY_PATH.getValueFor(subParams);
			if (localCopyDir != null) {
				localCopyDir = localCopyDir.trim();
			}
			if ("".equals(localCopyDir)) {
				localCopyDir = null;
			} else 
				if (!localCopyDir.endsWith(File.separator)) {
					localCopyDir = localCopyDir + File.separator;
				}

			//Dest dir
			destDir = Parameters.SUB_DEST_DIR.getValueFor(subParams);
			if (!destDir.endsWith(File.separator)) {
				destDir = destDir + File.separatorChar;
			}
			//append a successful <status> element to result doc
			attrs = StatusAttributes.factory(StatusLevel.SUCCESS,
					StatusType.DRIVER_STATUS,
					null); //event-id
			status = XDSUtil.appendStatus(result, //doc to append to
					attrs, //status attribute values
					null); //description

			//append the parameter values the subscriber is actually using
			allParams.putAll(subParams);
			status.parametersAppend(allParams);
		}//try
		catch (final ClassCastException e) //don't want to catch Error class with Throwable
		{
			Util.printStackTrace(trace,e);
			attrs = StatusAttributes.factory(StatusLevel.FATAL,
					StatusType.DRIVER_STATUS,
					null); //event-id
			XDSUtil.appendStatus(result, //doc to append to
					attrs, //status attribute values
					"One or more strategies specified in the subscriber channel are not implementing the correct interface.", //description
					e, //exception
					XDSUtil.appendStackTrace(e), //append stack trace?
					initXML); //xml to append
		}
		catch (final Exception e) //don't want to catch Error class with Throwable
		{
			Util.printStackTrace(trace,e);
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

		//return result doc w/ status to the DirXML engine
		return result.toXML();
	}//init(XmlDocument):XmlDocument


	/**
	 *  <code>execute</code> will execute a command encoded in an XML
	 *  document.
	 *  <p>
	 *  @param commandXML the document that contains the commands
	 *  @param processor a query processor that can be used to query the
	 *      directory
	 *  @return an XML document containing status messages and commands
	 *      resulting from this operation
	 *      (e.g., &lt;add-association&gt;, &lt;remove-association&gt;)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public XmlDocument execute(final XmlDocument commandXML,
			final XmlQueryProcessor processor)
	{

		//example command document:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product version="1.1a">DirXML</product>
                <contact>Novell, Inc.</contact>
            </source>
            <input>
                <add class-name="User" event-id="0" src-dn="\NEW_DELL_TREE\NOVELL\test\a" src-entry-id="33071">
                    <add-attr attr-name="Surname">
                        <value timestamp="1040071990#3" type="string">a</value>
                    </add-attr>
                    <add-attr attr-name="Telephone Number">
                        <value timestamp="1040072034#1" type="teleNumber">111-1111</value>
                    </add-attr>
                </add>
            </input>
        </nds>
		 */

		//example result document:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product build="20021216_0123" instance="Skeleton Driver (Java, XDS)" version="1.1">DirXML Skeleton Driver (Java, XDS)</product>
                <contact>My Company Name</contact>
            </source>
            <output>
                <add-association dest-dn="\NEW_DELL_TREE\NOVELL\test\a" event-id="0">1</add-association>
                <init-params>
                    <subscriber-state>
                        <current-association>2</current-association>
                    </subscriber-state>
                </init-params>
                <status event-id="0" level="success" type="driver-general"/>
            </output>
        </nds>
		 */

		trace.trace("execute", 1);

		XDSCommandResultDocument result;
		StatusAttributes attrs;
		String eventID;

		//setup result document for use by command handlers
		result = new XDSCommandResultDocument();
		appendSourceInfo(result);
		eventID = null;

		try
		{
			XDSCommandDocument commands;

			//parse/validate command document; it may have been malformed or
			//  invalidated during style sheet processing
			commands = new XDSCommandDocument(commandXML);

			//unlike other commands, the identity query doesn't require
			//  app connectivity to be processed
			if (commands.containsIdentityQuery())
			{
				XDSQueryElement query;

				query = commands.identityQuery();
				eventID = query.getEventID();
				appendDriverIdentityInfo(result);

				//append a successful <status> element to result doc
				attrs = StatusAttributes.factory(StatusLevel.SUCCESS,
						StatusType.DRIVER_GENERAL,
						eventID);
				XDSUtil.appendStatus(result, //doc to append to
						attrs, //status attribute values
						null); //description
			}//if
			else
			{
				final List<CommandElement> c = commands.childElements();
				for (final CommandElement command : c) {
					eventID = command.getEventID();
					dispatch(command, result,processor);

					//append a successful <status> element to result doc
					attrs = StatusAttributes.factory(StatusLevel.SUCCESS,
							StatusType.DRIVER_GENERAL,
							eventID);
					XDSUtil.appendStatus(result, //doc to append to
							attrs, //status attribute values
							null); //description
				}//while
			}//else
		}//try
		catch (final VirtualMachineError vme){
			//Stop the driver if we still can
			attrs = StatusAttributes.factory(StatusLevel.FATAL,
					StatusType.DRIVER_GENERAL,
					null);
			Util.appendStatus(result, //doc to append to
					attrs,  //status attribute values
					Errors.FATAL, //description
					vme,    //exception
					false,  //append stack trace?
					commandXML); //xml to append
		}
		catch (final XDSException xds)
		{
			//command document is malformed or invalid
			attrs = StatusAttributes.factory(StatusLevel.ERROR,
					StatusType.DRIVER_GENERAL,
					null);
			XDSUtil.appendStatus(result, //doc to append to
					attrs,  //status attribute values
					Errors.INVALID_DOC, //description
					xds,    //exception
					false,  //append stack trace?
					commandXML); //xml to append
		}
		catch(final WriteException we){
			//We had an exception when trying to write data
			attrs = StatusAttributes.factory(StatusLevel.ERROR,
					StatusType.DRIVER_GENERAL,
					null);
			//Append the correct exception message
			Exception e = (Exception) we.getCause();
			if (e==null) {
				e=we;
			}
			final File workFile = new File(workDir, newFileName);
			trace.trace(Errors.FILE_WRITE_ERROR + "("+workFile.getAbsolutePath()+")"+e.getMessage(), TraceLevel.ERROR_WARN);
			XDSUtil.appendStatus(result, //doc to append to
					attrs,  //status attribute values
					Errors.FILE_WRITE_ERROR + "("+workFile.getAbsolutePath()+")", //description
					e,      //exception
					false, //append stack trace?
					commandXML); //xml to append        	
		}
		catch (final Exception e) //don't want to catch Error class with Throwable
		{
			//something bad happened...

			//there's something wrong with the driver; rather than
			//  loose the event, shutdown
			attrs = StatusAttributes.factory(StatusLevel.FATAL,
					StatusType.DRIVER_STATUS,
					eventID);
			XDSUtil.appendStatus(result, //doc to append to
					attrs, //status attribute values
					e.getMessage(), //description
					e, //exception
					true, //append stack trace?
					commandXML); //xml to append
		}//catch

		//return result doc w/ status to the DirXML engine
		return result.toXML();
	}//execute(XmlDocument, XmlQueryProcessor):XmlDocument


	/**
	 *  A non-interface method that dispatches a command to the
	 *  appropriate handler.
	 *  <p>
	 *  @param command the command element from the command document
	 *  @param result the document to append commands to (e.g.,
	 *      &lt;add-association&gt;, &lt;remove-association&gt;)
	 *  @throws XDSParseException if command missing required
	 *      &lt;association&gt; element
	 */
	private void dispatch(final CommandElement command, final XDSCommandResultDocument result,final XmlQueryProcessor processor) throws XDSParseException, WriteException
	{
		trace.trace("dispatch start", TraceLevel.TRACE);
		if (driver.getObjectClass().equals(command.getClassName())){        
			//We only support Add or instance events
			if (command instanceof XDSAddElement)
			{
				addHandler((XDSAddElement) command, result);
				appendStateInfo(result);
			}
			else if ("instance".equals(command.tagName()) && (command instanceof NonXDSElement))
			{
				instanceHandler((NonXDSElement) command, result);
				appendStateInfo(result);
			}
			else if (command instanceof XDSModifyElement)
			{
				modifyHandler((XDSModifyElement) command, result, processor);
				appendStateInfo(result);
			}else if (command instanceof XDSQueryElement){
				queryHandler((XDSQueryElement) command, result, processor);
			}
			else{
				//this is either a custom command from a style sheet or
				//  a command from a newer DTD that the XDS library
				//  doesn't recongize
				trace.trace("unhandled element:  " + command.tagName() + " - " + command.getClass().getName(), 3);
			}
		}
		else{
			//Unsupported object class. Just trace.
			trace.trace("unhandled object class:  " + command.getClassName(), 3);
		}
		trace.trace("dispatch done", TraceLevel.TRACE);
	}//dispatch(CommandElement, XDSCommandResultDocument):void


	private void queryHandler(final XDSQueryElement command, final XDSCommandResultDocument result, final XmlQueryProcessor processor) {
		//If we have a query with an association, just return an empty instance back.
		//This is in order to support a 'migrate from' scenario.
		//Append and add-association element back
		final String fieldName = command.extractAssociationText();
		if (fieldName != null){
			trace.trace("Query with association: returning empty instance. This is in order to support sync events.");
			final XDSInstanceElement instanceElem = result.appendInstanceElement();
			final String eventId = command.getEventID();
			if ((eventId != null) && !"".equals(eventId)) {
				instanceElem.setEventID(eventId);
			}
			final String className = command.getClassName();
			if ((className!=null) && !"".equals(className)) {
				instanceElem.setClassName(className);
			}
			instanceElem.appendAssociationElement(fieldName);
		}else{
			trace.trace("Not responding to a query without association.");
		}
	}


	@SuppressWarnings("unchecked")
	private void modifyHandler(final XDSModifyElement command, final XDSCommandResultDocument result, final XmlQueryProcessor processor) throws WriteException, XDSParseException {

		trace.trace("modifyHandler start", 1);

		//Generate a map containing the fields
		//iterate through the <add-attr> child elements
		XDSModifyAttrElement addAttr;
		//XDSValueElement value;
		final Set<String> availableAttributes = new HashSet<String>();

		final HashMap<String,String> data = new HashMap<String,String>();

		Iterator<ElementImpl> a = command.extractModifyAttrElements().listIterator();
		while (a.hasNext())
		{
			addAttr = (XDSModifyAttrElement) a.next();
			final String attrName=addAttr.getAttrName();
			trace.trace("modifyHandler: attr-name  == " + Util.toLiteral(addAttr.getAttrName()), 2);
			availableAttributes.add(addAttr.getAttrName());
			final List<ElementImpl> valueList = addAttr.childElements();
			final Iterator<ElementImpl> values = valueList.iterator();
			while (values.hasNext()) {
				final Object aValue = values.next();
				if (aValue instanceof XDSAddValueElement) {
					final XDSAddValueElement addElemValue = (XDSAddValueElement) aValue;

					final List<XDSValueElement> addedValueList = addElemValue.extractValueElements();
					addValueList2Map(data, addedValueList, attrName);

				}
			}
		}//while

		//Get all the attributes we do not yet have
		final Set<String> missingAttributes =  new HashSet<String>(Arrays.asList(driver.getSchema()));
		missingAttributes.removeAll(availableAttributes);
		trace.trace("Missing attributes:"+missingAttributes);
		//Special scenario: a modify of only the fileclose trigger attribute
		//If we miss all attributes but this command requires to start a new file: do not query back.
		final IFileStartStrategy fileStarter = (IFileStartStrategy) strategyMap.get(Strategies.FILESTARTER);
		if ((driver.getSchema().length == missingAttributes.size()) && fileStarter.requiresNewFile( data)){
			//No need to query for missing attributes: this is only a notification to close the file
			//Close the old file (if any)
			trace.trace("Missing all attributes and we seem required to close file. Assume only closing file is required.");
			if (!finishFile()) {
				trace.trace("Not closed file.", TraceLevel.ERROR_WARN);
			}				
		}else{
			final XDSQueryResultDocument instance = queryForMissingAttributes(command, processor, missingAttributes);
			if (instance != null){
				final List<XDSInstanceElement> instanceElems = instance.extractInstanceElements();
				if (instanceElems.size()==0){
					trace.trace("Unable to read current state of object:"+command.getSrcDN());
				}else{
					final XDSInstanceElement instanceElem = instanceElems.get(0);
					a = instanceElem.extractAttrElements().listIterator();
					XDSAttrElement instanceAttr;
					while (a.hasNext())
					{
						instanceAttr = (XDSAttrElement) a.next();
						trace.trace("modifyHandler: attr-name  == " + Util.toLiteral(instanceAttr.getAttrName()), 2);
						final List<XDSValueElement> valueList = instanceAttr.extractValueElements();
						final String attrName=instanceAttr.getAttrName();

						addValueList2Map(data, valueList, attrName);
					}//while
				}
			}
			addRecord(data);
		}
	}


	private XDSQueryResultDocument queryForMissingAttributes(final XDSModifyElement command, final XmlQueryProcessor processor,
			final Set<String> missingAttributes) throws XDSParseException {
		if (missingAttributes.size()>0){
			final XDSQueryDocument queryDoc = newQueryDoc();
			final XDSQueryElement queryElem = queryDoc.appendQueryElement();
			for (final String object : missingAttributes) {
				final XDSReadAttrElement readAttrElem = queryElem.appendReadAttrElement();
				readAttrElem.setAttrName(object);
			}
			queryElem.setDestDN(command.getSrcDN());
			queryElem.setScope(QueryScope.ENTRY);
			queryElem.setClassName(driver.getObjectClass());
			final XmlDocument queryResult = processor.query(queryDoc.toXML());
			final XDSQueryResultDocument instance = new XDSQueryResultDocument(queryResult);
			return instance;
		}
		return null;
	}


	private void addValueList2Map(final HashMap<String,String> data, final List<XDSValueElement> valueList, final String attrName) {
		XDSValueElement value;
		//We do NOT support multi-values attributes => No need to loop through the values.
		if (valueList.size()>0){
			value = valueList.get(0);
			if (value.getType().equals(ValueType.STRUCTURED)){
				@SuppressWarnings("unchecked")
				final
				List<XDSComponentElement> components = value.extractComponentElements();
				final StringBuffer text = new StringBuffer();
				for (final XDSComponentElement aComponent : components) {
					final String textValue = aComponent.extractText();
					if (textValue!=null) {
						text.append(textValue);
					}
				}
				data.put(attrName, text.toString());
			}else{
				final String textValue = value.extractText();
				if (textValue != null) {
					data.put(attrName, textValue);
				} else {
					data.put(attrName, "");
				}
			}
		}
	}


	@SuppressWarnings("unchecked")
	private void instanceHandler(final NonXDSElement command, final XDSCommandResultDocument result) throws WriteException {
		//MODIFY:  put code to handle an <instance> command here

		//example <add> element:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product version="1.1a">DirXML</product>
                <contact>Novell, Inc.</contact>
            </source>
            <input>
                <add class-name="User" event-id="0" src-dn="\NEW_DELL_TREE\NOVELL\test\a" src-entry-id="33071">
                    <add-attr attr-name="Surname">
                        <value timestamp="1040071990#3" type="string">a</value>
                    </add-attr>
                    <add-attr attr-name="Telephone Number">
                        <value timestamp="1040072034#1" type="teleNumber">111-1111</value>
                    </add-attr>
                </add>
            </input>
        </nds>
		 */

		//example add result:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product build="20021216_0123" instance="Skeleton Driver (Java, XDS)" version="1.1">DirXML Skeleton Driver (Java, XDS)</product>
                <contact>My Company Name</contact>
            </source>
            <output>
                <add-association dest-dn="\NEW_DELL_TREE\NOVELL\test\a" event-id="0">1</add-association>
                <init-params>
                    <subscriber-state>
                        <current-association>2</current-association>
                    </subscriber-state>
                </init-params>
                <status event-id="0" level="success" type="driver-general"/>
            </output>
        </nds>
		 */

		trace.trace("instanceHandler start", 1);

		//Generate a map containing the fields
		//iterate through the <add-attr> child elements
		//XDSAddAttrElement addAttr;
		//XDSValueElement value;

		final HashMap<String, String> data = new HashMap<String, String>();

		final XmlDocument xmlDoc = new XmlDocument(command.domDocument()); 
		XDSInstanceDocument instance;
		try {
			instance = new XDSInstanceDocument(xmlDoc, trace);
		} catch (final XDSParseException e) {
			trace.trace(e.getClass().getName()+"-"+e.getMessage(),TraceLevel.ERROR_WARN);
			return;
		}
		//Loop over all the instances
		final ListIterator<XDSInstanceElement> a = instance.extractInstanceElements().listIterator();
		while (a.hasNext()) {
			final XDSInstanceElement instanceElem = a.next();
			//For each instance loop over all the attributes
			final ListIterator<XDSAttrElement> b = instanceElem.extractAttrElements().listIterator();
			XDSAttrElement instanceAttr;
			while (b.hasNext())
			{
				instanceAttr = b.next();
				trace.trace("instanceHandler: attr-name  == " + Util.toLiteral(instanceAttr.getAttrName()), 2);
				final List<XDSValueElement> valueList = instanceAttr.extractValueElements();
				final String attrName=instanceAttr.getAttrName();

				addValueList2Map(data, valueList, attrName);
			}//while
			addRecord(data);
		}

		//in the case of this skeleton driver, we will setup a fake
		//  association value for the <add> so that we will get modifies,
		//  deletes, etc. on the associated object; for a real driver,
		//  we would add an association using whatever unique key the
		//  application can supply for the application object
		//  (a dn, a GUID, etc.)

		/*        XDSAddAssociationElement addAssociation;

        //append an <add-association> element to result doc
        addAssociation = result.appendAddAssociationElement();
        //add the attributes that tells the DirXML engine to what eDirectory object
        //  this applies
        addAssociation.setDestDN(add.getSrcDN());
        addAssociation.setEventID(add.getEventID());
        //create the association value and put it under the <add-association>
        //  element increment our fake association value for the next add
        addAssociation.appendText(String.valueOf(currentAssociation));
		 */
	}


	/**
	 *  A non-interface method that handles an &lt;add> command.
	 *  <p>
	 *  @param add the &lt;add> element; must not be <code>null</code>
	 *  @param result the document to append an &lt;add-association&gt; to;
	 *      must not be <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	private void addHandler(final XDSAddElement add,
			final XDSCommandResultDocument result) throws WriteException
	{
		//MODIFY:  put code to handle an <add> command here

		//example <add> element:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product version="1.1a">DirXML</product>
                <contact>Novell, Inc.</contact>
            </source>
            <input>
                <add class-name="User" event-id="0" src-dn="\NEW_DELL_TREE\NOVELL\test\a" src-entry-id="33071">
                    <add-attr attr-name="Surname">
                        <value timestamp="1040071990#3" type="string">a</value>
                    </add-attr>
                    <add-attr attr-name="Telephone Number">
                        <value timestamp="1040072034#1" type="teleNumber">111-1111</value>
                    </add-attr>
                </add>
            </input>
        </nds>
		 */

		//example add result:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product build="20021216_0123" instance="Skeleton Driver (Java, XDS)" version="1.1">DirXML Skeleton Driver (Java, XDS)</product>
                <contact>My Company Name</contact>
            </source>
            <output>
                <add-association dest-dn="\NEW_DELL_TREE\NOVELL\test\a" event-id="0">1</add-association>
                <init-params>
                    <subscriber-state>
                        <current-association>2</current-association>
                    </subscriber-state>
                </init-params>
                <status event-id="0" level="success" type="driver-general"/>
            </output>
        </nds>
		 */

		trace.trace("addHandler start", 1);

		//Generate a map containing the fields
		XDSAddAttrElement addAttr;
		final HashMap<String, String> data = new HashMap<String, String>();

		//iterate through the <add-attr> child elements
		final ListIterator<XDSAddAttrElement> a = add.extractAddAttrElements().listIterator();
		while (a.hasNext())
		{
			addAttr = a.next();
			trace.trace("addHandler: attr-name  == " + Util.toLiteral(addAttr.getAttrName()), 2);
			final List<XDSValueElement> valueList = addAttr.extractValueElements();
			final String attrName = addAttr.getAttrName();
			addValueList2Map(data, valueList, attrName);
		}//while


		addRecord(data);

		//Append and add-association element back
		final String fieldName = ((IDriver) driver).getAssociationField(data);
		if ((fieldName != null) && !fieldName.trim().equals("")){
			final XDSAddAssociationElement associationElem = result.appendAddAssociationElement();
			associationElem.appendText(fieldName);
			associationElem.setDestDN(add.getSrcDN());
			associationElem.setEventID(add.getEventID());
		}


		//in the case of this skeleton driver, we will setup a fake
		//  association value for the <add> so that we will get modifies,
		//  deletes, etc. on the associated object; for a real driver,
		//  we would add an association using whatever unique key the
		//  application can supply for the application object
		//  (a dn, a GUID, etc.)

		/*        XDSAddAssociationElement addAssociation;

        //append an <add-association> element to result doc
        addAssociation = result.appendAddAssociationElement();
        //add the attributes that tells the DirXML engine to what eDirectory object
        //  this applies
        addAssociation.setDestDN(add.getSrcDN());
        addAssociation.setEventID(add.getEventID());
        //create the association value and put it under the <add-association>
        //  element increment our fake association value for the next add
        addAssociation.appendText(String.valueOf(currentAssociation));
		 */
	}//addHandler(XDSAddElement, XDSCommandResultDocument):void


	private void startFile(final HashMap<String, String> data) throws WriteException{
		final IFileNameStrategy fileNamer = (IFileNameStrategy) strategyMap.get(Strategies.FILENAMER);
		final IFileWriteStrategy fileWriter = (IFileWriteStrategy) strategyMap.get(Strategies.FILEWRITER);
		synchronized(fileLock){
			//Get a new filename
			newFileName = fileNamer.getNewFileName( data);
			//Open the new file
			final File f = new File(workDir, newFileName);
			fileWriter.openFile(f);
			trace.trace("Starting new file "+f.getPath(),TraceLevel.TRACE);
			isFileOpen = true;
			//Notify all listeners that a file was opned.
			fireAfterFileOpened(f);
		}
	}

	/**
	 * Synchronised with finishFile: make sure that our file does not get closed
	 * while we want to write.
	 * @param data
	 * @throws WriteException
	 */
	private void addRecord(final HashMap<String, String> data) throws WriteException {
		final IFileStartStrategy fileStarter = (IFileStartStrategy) strategyMap.get(Strategies.FILESTARTER);
		final IFileWriteStrategy fileWriter = (IFileWriteStrategy) strategyMap.get(Strategies.FILEWRITER);

		synchronized(fileLock){
			//ensure we do not need to start a new file before adding.
			//ensure we do not need to start a new file.
			if (!isFileOpen || fileStarter.requiresNewFile(data))
			{
				//Close the old file (if any)
				finishFile();
				startFile(data);
			}
			//Write a record
			trace.trace("Appending record to file.",TraceLevel.TRACE);
			//Notify all listeners that a record will be added.
			fireBeforeRecordAdded(data);
			fileWriter.writeRecord(data);

			//Update any counters
			if (currentFileRecordCount<0) {
				currentFileRecordCount=1;
			} else {
				currentFileRecordCount++;
			}
			if (totalRecordCount<0) {
				totalRecordCount=1;
			} else {
				totalRecordCount++;
			}
		}
		//Notify all listeners that a record was added.
		fireAfterRecordAdded(data);
	}



	private void fireAfterRecordAdded(final HashMap<String, String> data) {
		synchronized (fileListeners) {
			for (final ISubscriberFileListener listener : fileListeners) {
				try{
					listener.afterRecordAdded(null);					
				}catch(final Throwable th){
					trace.trace("Error while notifying fileListener:",TraceLevel.EXCEPTION);
					Util.printStackTrace(trace,th);
				}
			}
		}		
	}


	private void fireBeforeRecordAdded(final HashMap<String, String> data) {
		synchronized (fileListeners) {
			for (final ISubscriberFileListener listener : fileListeners) {
				try{
					listener.beforeRecordAdded(null);					
				}catch(final Throwable th){
					trace.trace("Error while notifying fileListener:",TraceLevel.EXCEPTION);
					Util.printStackTrace(trace,th);
				}
			}
		}		
	}

	private void fireAfterFileOpened(final File f) {
		synchronized (fileListeners) {
			for (final ISubscriberFileListener listener : fileListeners) {
				try{
					listener.afterFileOpened(null);					
				}catch(final Throwable th){
					trace.trace("Error while notifying fileListener:",TraceLevel.EXCEPTION);
					Util.printStackTrace(trace,th);
				}
			}
		}		
	}

	private void fireAfterFileClosed() {
		synchronized (fileListeners) {
			for (final ISubscriberFileListener listener : fileListeners) {
				try{
					listener.afterFileClose(null);					
				}catch(final Throwable th){
					trace.trace("Error while notifying fileListener:",TraceLevel.EXCEPTION);
					Util.printStackTrace(trace,th);
				}
			}
		}		
	}




	/**
	 *  A non-interface method that appends driver identification information
	 *  to <code>result</code> in response to an identity query.
	 *  <p>
	 *  @param result may be <code>null</code>
	 */
	private void appendDriverIdentityInfo(final XDSCommandResultDocument result)
	{
		//MODIFY:  put your identity query response here

		//example identity query:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product version="1.1a">DirXML</product>
                <contact>Novell, Inc.</contact>
            </source>
            <input>
                <query event-id="query-driver-ident" scope="entry">
                    <search-class class-name="__driver_identification_class__"/>
                    <read-attr/>
                </query>
            </input>
        </nds>
		 */

		//example identity query response:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product build="20021214_0304" instance="Skeleton Driver (Java, XDS)" version="1.1">DirXML Skeleton Driver (Java, XDS)</product>
                <contact>My Company Name</contact>
            </source>
            <output>
                <instance class-name="__driver_identification_class__">
                    <attr attr-name="driver-id">
                        <value type="string">JSKEL</value>
                    </attr>
                    <attr attr-name="driver-version">
                        <value type="string">1.1</value>
                    </attr>
                    <attr attr-name="min-activation-version">
                        <value type="string">0</value>
                    </attr>
                </instance>
                <status event-id="query-driver-ident" level="success" type="driver-general"/>
            </output>
        </nds>
		 */

		if (result != null)
		{
			//when appending static XML, it's simplest to use the XDSUtil class

			String driverID;

			//library version and skeleton version are synonymous
			driverID = "<instance class-name=\"__driver_identification_class__\">" +
					"<attr attr-name=\"driver-id\">" +
					"<value type=\"string\">" + DRIVER_ID_VALUE + "</value>" +
					"</attr>" +
					"<attr attr-name=\"driver-version\">" +
					"<value type=\"string\">" + XDS.VERSION + "</value>" +
					"</attr>" +
					"<attr attr-name=\"min-activation-version\">" +
					"<value type=\"string\">" + DRIVER_MIN_ACTIVATION_VERSION + "</value>" +
					"</attr>" +
					"</instance>";

			//append this XML to the <output> element
			XDSUtil.appendXML(result.domIOElement(), driverID);

			//here's the equivalent XDS implementation:

			/*
            XDSInstanceElement instance;
            XDSAttrElement attr;
            XDSValueElement value;

            instance = result.appendInstanceElement();
            instance.setClassName(DTD.VAL_DRIVER_IDENT_CLASS);
            attr = instance.appendAttrElement();
            attr.setAttrName(DTD.VAL_DRIVER_ID);
            value = attr.appendValueElement(ValueType.STRING, DRIVER_ID_VALUE);
            attr = instance.appendAttrElement();
            attr.setAttrName(DTD.VAL_DRIVER_VERSION);
            value = attr.appendValueElement(ValueType.STRING, XDS.VERSION);
            attr = instance.appendAttrElement();
            attr.setAttrName(DTD.VAL_MIN_ACTIVATION_VERSION);
            value = attr.appendValueElement(ValueType.STRING, DRIVER_MIN_ACTIVATION_VERSION);
			 */
		}//if
	}//appendDriverIdentityInfo(XDSQueryElement, XDSCommandResultDocument):void


	/**
	 *  A non-interface method called from
	 *  <code>SkeletonDriverShim.shutdown(XmlDocument)</code>
	 *  that signals to the subscriber to free its resources.
	 *  <p>
	 *  @see GenericFileDriverShim#shutdown(XmlDocument)
	 */
	@Override
	public void shutdown(final XDSResultDocument reasonXml)
	{
		synchronized (fileLock) {
			//do whatever is required to disconnect from the application
			//  e.g., close a Socket, etc.

			//We are closed as it should so, remove the shutwon hook
			if (shutdownHook != null) {
				Runtime.getRuntime().removeShutdownHook(shutdownHook);
			}
			shutdownHook = null;

			trace.trace("shutdown", 1);
			if (isFileOpen) {
				try {
					finishFile();
				} catch (final WriteException e) {
					//If we cannot close the file, issue a fatal exception
					final StatusAttributes attrs = StatusAttributes.factory(StatusLevel.FATAL,
							StatusType.DRIVER_GENERAL,
							null);
					Util.appendStatus(reasonXml, //doc to append to
							attrs,  //status attribute values
							Errors.FATAL, //description
							e,    //exception
							false,  //append stack trace?
							null); //xml to append
				}
			}
			//Call shutdown on every plugged in component.
			final Collection<IStrategy> keys = strategyMap.values();
			for (final IStrategy aStrategy : keys) {
				if (aStrategy instanceof IShutdown ) {
					((IShutdown)aStrategy).onShutdown(reasonXml);
				}
			}
			/*            if (fileWriter instanceof IShutdown)
            	((IShutdown)fileWriter).onShutdown(reasonXml);
            if (fileStarter instanceof IShutdown)
            	((IShutdown)fileStarter).onShutdown(reasonXml);
            if (fileNamer instanceof IShutdown)
            	((IShutdown)fileNamer).onShutdown(reasonXml);*/			
		}
	}//shutdown():void


	/**
	 *  A non-interface method that appends subscriber state information
	 *  to <code>doc</code>.
	 *  <p>
	 *  @param doc the document to append state to; may be <code>null</code>
	 */
	void appendStateInfo(final StateDocument doc)
	{
		//MODIFY:  remove this method or put code to append state info
		//  here; you may need to add equivalent methods to your
		//  DriverShim and PublicationShim as well

		//appending custom content requires use of DOM or the XDSUtil
		//  class;  the example below uses the XDSUtil class:

		if (doc != null)
		{
			String state;
			state = "<init-params>" +
					"<subscriber-state>" +
					((totalRecordCount>0)?(
							"<"+STATE_ADD_COUNT+">" +
									String.valueOf(totalRecordCount) +
									"</"+STATE_ADD_COUNT+">"):("")) +
					"</subscriber-state>" +
					"</init-params>";
			//append this XML to the <output> element
			XDSUtil.appendXML(doc.domIOElement(), state);

			//here's an equivalent DOM implementation:

			/*
            XDSInitParamsElement initParams;
            XDSSubscriberStateElement state;

            initParams = doc.appendInitParamsElement();
            state = initParams.appendSubscriberStateElement();

            Element parent, child;
            Document document;
            Text text;

            document = state.domDocument();
            parent = state.domElement();
            child = document.createElement(TAG_CURRENT_ASSOCIATION);
            text = document.createTextNode(String.valueOf(currentAssociation));
            child.appendChild(text);
            parent.appendChild(child);
			 */
		}//if
	}//appendStateInfo(StateDocument):void


	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.shim.IFileOwnerCallback#finishFile()
	 */
	@Override
	public boolean finishFile() throws WriteException {
		//trace.trace("finishFile() start", TraceLevel.TRACE);
		final IFileWriteStrategy fileWriter = (IFileWriteStrategy) strategyMap.get(Strategies.FILEWRITER);
		synchronized(fileLock)
		{
			if (isFileOpen){
				trace.trace("Closing file", TraceLevel.TRACE);
				final File tmpResult = fileWriter.close();
				isFileOpen = false;
				currentFileRecordCount = -1;
				//Notify all listeners that the file was closed. Do this before moving.
				fireAfterFileClosed();

				//Make a local copy if required
				if (localCopyDir != null){
					try {
						trace.trace("Creating a local copy of your file in "+localCopyDir,TraceLevel.EXCEPTION);
						final File localCopy = new File(localCopyDir, newFileName);
						Files.copy(tmpResult.toPath(), localCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
					} catch (final Exception e) {
						trace.trace("Exception while creating a local copy. This exception will be ignored in favour of the actual destination copy.",TraceLevel.EXCEPTION);
						Util.printStackTrace(trace, e);
					}
				}

				//Move the file from the workFolder to the dest folder
				final File result = new File(destDir, newFileName);
				if (!Util.moveFile(trace, tmpResult, result)){
					trace.trace("Unable to move the file. See trace for reason.", TraceLevel.ERROR_WARN);
					throw new WriteException("Unable to move the file. See trace for reason.",null);
				}
				trace.trace("File closed", TraceLevel.TRACE);
				//TODO: postprocess strategy
				final IPostProcessStrategy postProcess = (IPostProcessStrategy) strategyMap.get(Strategies.FILEPOSTPROCESS);
				if (postProcess != null) {
					trace.trace("finishFile() postprocess.doProcess() begin", TraceLevel.TRACE);
					postProcess.doPostProcess(result);
					trace.trace("finishFile() postprocess.doProcess() end", TraceLevel.TRACE);
				}
				trace.trace("finishFile() end", TraceLevel.TRACE);
				return true;
			}else{
				//trace.trace("Nothing to close: no file is open.", TraceLevel.TRACE);
				return false;
			}
			//trace.trace("finishFile() end", TraceLevel.TRACE);
		}
	}


	/**
	 * @return Returns the fileLock.
	 */
	@Override
	public Object getFileLock() {
		return fileLock;
	}


	@Override
	public int getCurrentFileRecordCount() {
		return currentFileRecordCount;
	}


	@Override
	public boolean isFileOpen() {
		synchronized (fileLock) {
			return isFileOpen;			
		}
	}


	@Override
	public void addFileListener(final ISubscriberFileListener listener) {
		synchronized (fileListeners) {
			fileListeners.add(listener);			
		}		
	}


	@Override
	public void removeFileListener(final ISubscriberFileListener listener) {
		synchronized (fileListeners) {
			fileListeners.remove(listener);			
		}		
	}


	@Override
	public ConnectionInfo getConnectionInfo() {
		return connectInfo;
	}
}//class SkeletonSubscriptionShim