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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.novell.nds.dirxml.driver.DriverFilter;
import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.XmlCommandProcessor;
import com.novell.nds.dirxml.driver.XmlDocument;
import com.novell.nds.dirxml.driver.XmlQueryProcessor;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.EnumConstraint;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.RangeConstraint;
import com.novell.nds.dirxml.driver.xds.RequiredConstraint;
import com.novell.nds.dirxml.driver.xds.StateDocument;
import com.novell.nds.dirxml.driver.xds.StatusLevel;
import com.novell.nds.dirxml.driver.xds.StatusType;
import com.novell.nds.dirxml.driver.xds.XDSAddAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSAddElement;
import com.novell.nds.dirxml.driver.xds.XDSAddValueElement;
import com.novell.nds.dirxml.driver.xds.XDSAttrDefElement;
import com.novell.nds.dirxml.driver.xds.XDSAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSClassDefElement;
import com.novell.nds.dirxml.driver.xds.XDSCommandDocument;
import com.novell.nds.dirxml.driver.xds.XDSCommandResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSDeleteElement;
import com.novell.nds.dirxml.driver.xds.XDSHeartbeatDocument;
import com.novell.nds.dirxml.driver.xds.XDSInitDocument;
import com.novell.nds.dirxml.driver.xds.XDSInitParamsElement;
import com.novell.nds.dirxml.driver.xds.XDSInstanceElement;
import com.novell.nds.dirxml.driver.xds.XDSModifyAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSModifyElement;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;
import com.novell.nds.dirxml.driver.xds.XDSParseException;
import com.novell.nds.dirxml.driver.xds.XDSPublisherStateElement;
import com.novell.nds.dirxml.driver.xds.XDSQueryDocument;
import com.novell.nds.dirxml.driver.xds.XDSQueryElement;
import com.novell.nds.dirxml.driver.xds.XDSQueryResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSReadAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSStatusElement;
import com.novell.nds.dirxml.driver.xds.XDSValueElement;
import com.novell.nds.dirxml.driver.xds.util.StatusAttributes;
import com.novell.nds.dirxml.driver.xds.util.XDSUtil;

import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.IFileLocatorStrategy;
import info.vancauwenberge.filedriver.api.IFileReadStrategy;
import info.vancauwenberge.filedriver.api.IFileSorterStrategy;
import info.vancauwenberge.filedriver.api.IPubFileCleanStrategy;
import info.vancauwenberge.filedriver.api.IPublisherLoggerStrategy;
import info.vancauwenberge.filedriver.api.IPublisherLoggerStrategy.LogField;
import info.vancauwenberge.filedriver.api.IPublisherStrategy;
import info.vancauwenberge.filedriver.api.IShutdown;
import info.vancauwenberge.filedriver.api.IStrategy;
import info.vancauwenberge.filedriver.exception.ReadException;
import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.filelocator.RegExpFileLocator;
import info.vancauwenberge.filedriver.filelogger.PublisherNoLogger;
import info.vancauwenberge.filedriver.filepubclean.NoPubCleaningStrategy;
import info.vancauwenberge.filedriver.filereader.csv.CSVFileReader;
import info.vancauwenberge.filedriver.filesorter.NoSortSorter;
import info.vancauwenberge.filedriver.query.QueryMatcher;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;
import info.vancauwenberge.filedriver.util.Errors;
import info.vancauwenberge.filedriver.util.MetaDataManager;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

/**
 * A basic skeleton for implementing the <code>PublicationShim</code>.
 * <p>
 * The <code>PublicationShim</code> is an interface used by the DirXML engine to
 * start and stop an application driver's publication process.
 * <p>
 * A <code>PublicationShim</code> will almost always also implement
 * <code>XmlQueryProcessor</code> but it could also delegate it to another
 * object.
 * <p>
 * NOTE: the publisher init() and start() methods are called on a thread
 * separate from the thread used for calling the DriverShim and SubscriptionShim
 * methods
 */
public class FileDriverPublicationShimImpl implements IPublisherImplStrategy, XmlQueryProcessor, IPublisher
// extends PublisherChannelShim
// implements PublicationShim, XmlQueryProcessor
{

	private enum GenerateCommnd {
		/**
		 * All generated commands will be add commands.
		 */
		ADD,
		/**
		 * All generated commands will be modify commands.
		 */
		MODIFY,
		/**
		 * Generated commands will be adds or modifies, depending whether or not
		 * a search based on association will find te object or not.
		 */
		DYNAMIC,
		/**
		 * Generate add, modify or delete events based on the input received.
		 * Delete events are identified by the regexp, add or modify events are
		 * based on a search on association (see {@link DYNAMIC}).
		 */
		DYNAMIC_INPUT,
		/**
		 * Generate delete commands.
		 */
		DELETE;
		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private enum Parameters {
		/**
		 * File locator to use. Defaults to {@link RegExpFileLocator}
		 */
		FILE_LOCATOR_STRATEGY("pub_fileLocator", RegExpFileLocator.class.getName(), DataType.STRING,
				RequiredConstraint.REQUIRED),
		/**
		 * File sorter to use . Defaults to {@link NoSortSorter}
		 */
		FILE_SORTER_STRATEGY("pub_FileSorter", NoSortSorter.class.getName(), DataType.STRING),
		/**
		 * Publisher logger to use . Defaults to {@link PublisherNoLogger}
		 */
		FILE_LOGGER_STRATEGY("pub_FileLogger", PublisherNoLogger.class.getName(), DataType.STRING),
		/**
		 * File reader to use . Defaults to {@link CSVFileReader}
		 */
		FILE_READER_STRATEGY("pub_fileReader", CSVFileReader.class.getName(), DataType.STRING,
				RequiredConstraint.REQUIRED),
		/**
		 * File cleanup strategy. When should files be deleted from the work
		 * folder? Defaults to {@link NoPubCleaningStrategy}
		 */
		FILE_CLEAN_STRATEGY("pub_fileClean", NoPubCleaningStrategy.class.getName(), DataType.STRING),
		/**
		 * The polling interval (in seconds)
		 */
		POLLING_INTERVAL("pub_pollingInterval", "10", DataType.INT, RangeConstraint.POSITIVE), // 10
																								// =
																								// in
																								// seconds
		/**
		 * Heartbeat interval (in minutes)
		 */
		HEARTBEAT_INTERVAL(PUB_HEARTBEAT_INTERVAL, "0", DataType.INT, RangeConstraint.NON_NEGATIVE), // 0=in
																										// minutes,
																										// disabled
		/**
		 * Work folder (this folder will contain the intermediate files).
		 */
		WORK_DIR("pub_workDir", "/var/opt", DataType.STRING, RequiredConstraint.REQUIRED),
		/**
		 * Attributes that should be marked as sensitive.
		 */
		SENSITIVE_ATTRIBUTES("pub_sensitiveAttributes", "", DataType.STRING),
		/**
		 * The metadata fileds that need to be added to the event
		 */
		META_DATA("pub_metaData", "", DataType.STRING),
		/**
		 * The publisher command to generate. Defaults to
		 * {@link GenerateCommnd.ADD}.
		 */
		COMMAND("pub_command", GenerateCommnd.ADD.toString().toLowerCase(), DataType.STRING,
				RequiredConstraint.REQUIRED, GenerateCommnd.values()),
		/**
		 * Field in the input event that donates the command to generate
		 */
		COMMAND_FIELD("pub_command_fieldName", "", DataType.STRING),
		/**
		 * When is the field (as defined in {@link #COMMAND_FIELD}) a delete
		 * event (all others arr modify or add events as defined in
		 * {@link #COMMAND} )
		 */
		COMMAND_DELETE_EGEXP("pub_command_delete_regexp", "", DataType.STRING), LOGENABLED("pub_IsLogging", "false",
				DataType.STRING),

		/**
		 * The name of the field in the logfile that will contain the status
		 */
		LOGFIELD_STATUS("pub_logFieldStatus", "", DataType.STRING),
		/**
		 * The name of the field in the logfile that will contain the status
		 * message
		 */
		LOGFIELD_STATUSMESSAGE("pub_logFieldStatusMessage", "", DataType.STRING),
		/*
		 * The name of the field in the logfile that will contain the record
		 * number
		 */
		LOGFIELD_RECORDNUMBER("pub_logFieldRecordNumber", "", DataType.STRING),
		/*
		 * The name of the field in the logfile that will contain the status
		 * type
		 */
		LOGFIELD_EVENTID("pub_logFieldEventID", "", DataType.STRING),;

		private final String paramName;
		private final String defaultValue;
		private final List<Constraint> cons;
		private final DataType dataType;

		private Parameters(final String paramName, final String defaultValue, final DataType type) {
			this(paramName, defaultValue, type, null, null);
		}

		private Parameters(final String paramName, final String defaultValue, final DataType type,
				final Constraint cons) {
			this(paramName, defaultValue, type, cons, null);
		}

		private Parameters(final String paramName, final String defaultValue, final DataType type,
				final Constraint cons, final Object[] enumConstraint) {
			this.paramName = paramName;
			this.defaultValue = defaultValue;
			this.cons = new LinkedList<Constraint>();
			if (cons != null) {
				this.cons.add(cons);
			}
			if (enumConstraint != null) {
				final EnumConstraint enumC = new EnumConstraint();
				for (final Object object : enumConstraint) {
					enumC.addLiteral(object.toString());
				}
				this.cons.add(enumC);
			}
			this.dataType = type;
		}

		public String getParamName() {
			return paramName;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public List<Constraint> getConstraints() {
			return cons;
		}

		public DataType getDataType() {
			return dataType;
		}
	}

	// Publisher STATE names
	private static final String STATE_RECORDNUMBER = "recordNumber";
	private static final String STATE_FILE_PATH = "filePath";
	private static final String STATE_FILRE_READER_CLASS = "fileReader";

	private PublisherStateMeta stateMeta = null;
	/**
	 * Variable used to determine when to return from <code>start()</code>.
	 * <p>
	 *
	 * @see #start(XmlCommandProcessor)
	 */
	private boolean shutdown;

	/**
	 * Variable used to control how often the thread in <code>start()</code>
	 * wakes up to poll the application. Value is in milliseconds.
	 * <p>
	 *
	 * @see #start(XmlCommandProcessor)
	 */
	private long pollingInterval;

	/**
	 * Variable used to control how often the thread in <code>start()</code>
	 * wakes up to send a document to the DirXML engine. Value is in
	 * milliseconds.
	 * <p>
	 *
	 * @see #start(XmlCommandProcessor)
	 */
	private long heartbeatInterval;

	private enum Strategies {
		//@formatter:off
		FILELOCATOR(Parameters.FILE_LOCATOR_STRATEGY,IFileLocatorStrategy.class), 
		FILESORTER(Parameters.FILE_SORTER_STRATEGY, IFileSorterStrategy.class),
		FILEREADER(Parameters.FILE_READER_STRATEGY, IFileReadStrategy.class), 
		FILECLEANER(Parameters.FILE_CLEAN_STRATEGY, IPubFileCleanStrategy.class),
		LOGGER(Parameters.FILE_LOGGER_STRATEGY, IPublisherLoggerStrategy.class); 
		//@formatter:onn
		private Parameters parameter;
		private Class<?> interfaceClass;

		Strategies(final Parameters paramDef, final Class<?> interfaceClass) {
			this.parameter = paramDef;
			this.interfaceClass = interfaceClass;
		}
	}

	private final Map<Strategies, IStrategy> strategyMap = new EnumMap<Strategies, IStrategy>(Strategies.class);

	/**
	 * Should a heartbeat document be sent?
	 * <p>
	 *
	 * @see #start(XmlCommandProcessor)
	 */
	private boolean doHeartbeat;

	/**
	 * The smaller of the polling interval or heartbeat interval > 0.
	 * <p>
	 *
	 * @see #start(XmlCommandProcessor)
	 */
	private long interval;

	/**
	 * Object used as a semaphore so subscriber-channel thread can wake
	 * publisher thread up to tell it to shutdown.
	 * <p>
	 *
	 * @see #start(XmlCommandProcessor)
	 */
	private final Object semaphore;

	/**
	 * Used to filter application events before sending them to the DirXML
	 * engine. Events are already filtered on the subscriber channel before they
	 * reach the driver so we don't have to worry about filtering subscription
	 * events.
	 * <p>
	 *
	 * @see #start(XmlCommandProcessor)
	 */
	private DriverFilter filter;

	/**
	 * The temporary folder that will contain the files when processing/after
	 * processing
	 */
	private String workDir;
	/**
	 * The file currently being read. null if no file is read.
	 */
	private File currentFile;

	private MetaDataManager metaData;

	/**
	 * Internal number to identify the event (sequence)
	 */
	private long eventid = 0;

	private HashMap<String, Parameter> allParams;

	// The command to generate: add or modify
	private GenerateCommnd commandToGenerate;

	private String commandIdentifierField;

	private Pattern commandDeleteRegexp;

	private Trace trace;

	private IDriver driver;
	// private File workFile;
	private final EnumMap<IPublisherLoggerStrategy.LogField, String> logFieldschemaMap = new EnumMap<IPublisherLoggerStrategy.LogField, String>(
			IPublisherLoggerStrategy.LogField.class);
	private List<String> sensitiveAttributes;

	public void setDriver(final IDriver someDriver) {
		driver = someDriver;
		trace = new Trace(driver.getDriverInstanceName());
	}

	/**
	 * Constructor.
	 * <p>
	 *
	 * @param someDriver
	 *            a reference to this driver instance; must not be
	 *            <code>null</code>
	 */
	public FileDriverPublicationShimImpl()// GenericFileDriverShim someDriver)
	{
		// super(someDriver,TRACE_SUFFIX);
		shutdown = false;
		pollingInterval = -1;
		semaphore = new short[0]; // smallest object
		filter = null;
		doHeartbeat = false;
	}// SkeletonPublicationShim(SkeletonDriverShim)

	/**
	 * A non-interface method that describes the parameters this PublicationShim
	 * is expecting.
	 * <p>
	 *
	 * @see FileDriverPublicationShimImpl#init(XmlDocument)
	 */
	private Map<String, Parameter> getDefaultParameterDefs() {
		// MODIFY: construct parameter descriptors here for your
		// publisher parameters

		// The XDS.jar library automatically checks parameter
		// data types for you. When a RequiredConstraint
		// is added to a parameter, the library will check init documents
		// to ensure the parameter is present and has a value. When you
		// add RangeConstraints or EnumConstraints to a parameter, the
		// library will check parameter values to see if they satisfy
		// these constraints.
		trace.trace("getInitialDriverParams start", TraceLevel.TRACE);
		final Parameters[] parameters = Parameters.values();

		final HashMap<String, Parameter> driverParams = new HashMap<String, Parameter>(parameters.length);

		for (final Parameters driverParam : parameters) {
			final Parameter param = new Parameter(driverParam.getParamName(), // tag
																				// name
					driverParam.getDefaultValue(), // default value
					driverParam.getDataType()); // data type
			final List<Constraint> cons = driverParam.getConstraints();
			for (final Constraint constraint : cons) {
				param.add(constraint);
			}
			driverParams.put(param.tagName(), param);
		}
		trace.trace("getInitialDriverParams done", TraceLevel.TRACE);
		return driverParams;
	}// setPubParams():void

	/**
	 * Create and initialize a given strategy implementation
	 *
	 * @param init
	 * @param className
	 * @param allParams
	 *            This map will be updated with the parametervalues from this
	 *            IPublisherStrategy
	 * @return
	 * @throws Exception
	 */
	private IPublisherStrategy initStrategy(final XDSInitDocument initDocument, final String className,
			final Map<String, Parameter> allParams) throws Exception {
		final IPublisherStrategy aStrategy = (IPublisherStrategy) Class.forName(className).newInstance();

		// Get the required parameters for this IStrategy
		final Map<String, Parameter> strategyParameters = aStrategy.getParameterDefinitions();

		// initDocument.parameters cannot handle null parameter object.
		if ((strategyParameters != null) && !strategyParameters.isEmpty()) {
			// Update the map with the values from the initDocument
			initDocument.parameters(strategyParameters);

			// Add the parameters of this IStrategy to the allParameters map
			allParams.putAll(strategyParameters);

			// Add the general driver parameter (eg schema)
			strategyParameters.putAll(driver.getDriverParams());
		}

		// Initialize the strategy with it's own trace prefix
		final Trace trace = new Trace(driver.getDriverInstanceName() + "\\" + aStrategy.getClass().getSimpleName());
		aStrategy.init(trace, strategyParameters, this);
		return aStrategy;
	}

	/**
	 * <code>init</code> will be called before the invocation of
	 * <code>start</code>.
	 * <p>
	 * In general, application connectivity should be handled in
	 * <code>start(XmlCommandProcessor)</code> so a driver can start when the
	 * application is down.
	 * <p>
	 *
	 * @param initXML
	 *            XML document that contains the publisher initialization
	 *            parameters and state
	 * @return an XML document containing status messages for this operation
	 */
	@Override
	public XmlDocument init(final Trace trace, final Map<String, Parameter> commonParams, final IDriver driver,
			final XmlDocument initXML) throws Exception {

		// public XmlDocument init(Map<String,Parameter> commonParams,
		// XmlDocument initXML)
		// {
		// MODIFY: initialize your publisher here

		//@formatter:off
		// example initialization document:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product version="1.1a">DirXML</product>
                <contact>Novell, Inc.</contact>
            </source>
            <input>
                <init-params src-dn="\NEW_DELL_TREE\NOVELL\Driver Set\Skeleton Driver (Java, XDS)\Publisher">
                    <authentication-info>
                        <server>server.app:400</server>
                        <user>User1</user>
                    </authentication-info>
                    <driver-filter>
                        <allow-class class-name="User">
                            <allow-attr attr-name="Surname"/>
                            <allow-attr attr-name="Given Name"/>
                        </allow-class>
                    </driver-filter>
                    <publisher-options>
                <pub-1 display-name="Sample Publisher option">String for Publisher</pub-1>
                <polling-interval display-name="Polling interval in seconds">10</polling-interval>
            </publisher-options>
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
                        <polling-interval display-name="Polling interval in seconds">10</polling-interval>
                        <pub-1 display-name="Sample Publisher option">String for Publisher</pub-1>
                    </parameters>
                </status>
            </output>
        </nds>
		//@formatter:on
		 */
		this.driver = driver;
		this.trace = trace;// new Trace(driver.getDriverRDN());

		trace.trace("init", 1);

		// create result document for reporting status to the DirXML engine
		final XDSResultDocument result = driver.newResultDoc();
		StatusAttributes attrs;

		try {
			allParams = new HashMap<String, Parameter>();
			// parse initialization document
			final XDSInitDocument init = new XDSInitDocument(initXML);
			final XDSStatusElement status;
			Parameter param;

			// get any publisher options from init doc
			System.out.println(commonParams);

			final Map<String, Parameter> pubParams = getDefaultParameterDefs();// getShimParameters();
			pubParams.putAll(commonParams);
			init.parameters(pubParams);

			// Add the general driver parameters to the publisher parameters
			pubParams.putAll(driver.getDriverParams());

			// get the polling interval that may have been passed in
			param = pubParams.get(Parameters.POLLING_INTERVAL.getParamName());
			// our pollingInterval value is in seconds, Object.wait(long) takes
			// milliseconds => convert
			pollingInterval = XDSUtil.toMillis(param.toInteger().intValue());

			// get the polling interval that may have been passed in
			param = pubParams.get(PUB_HEARTBEAT_INTERVAL);
			// our heartbeatInterval value is in minutes, Object.wait(long)
			// takes milliseconds => convert
			heartbeatInterval = XDSUtil.toMillis(param.toInteger().intValue() * 60);

			doHeartbeat = (heartbeatInterval > 0);
			interval = (doHeartbeat) ? Math.min(pollingInterval, heartbeatInterval) : pollingInterval;

			// Get the publisher work dir
			workDir = pubParams.get(Parameters.WORK_DIR.getParamName()).toString();
			if (!workDir.endsWith(File.separatorChar + "")) {
				workDir = workDir + File.separatorChar;
			}

			// The command to generate (add or modify)
			commandToGenerate = GenerateCommnd
					.valueOf(pubParams.get(Parameters.COMMAND.getParamName()).toString().toUpperCase());

			if (commandToGenerate == GenerateCommnd.DYNAMIC_INPUT) {
				this.commandIdentifierField = pubParams.get(Parameters.COMMAND_FIELD.getParamName()).toString();
				if ((commandIdentifierField == null) || "".equals(commandIdentifierField.trim())) {
					throw new IllegalArgumentException("Value for " + Parameters.COMMAND_FIELD.getParamName()
							+ " is required when DYNAMIC_INPUT is selected.");
				}
				if (!Arrays.asList(driver.getSchema()).contains(commandIdentifierField)) {
					trace.trace("Warning: Value for " + Parameters.COMMAND_FIELD.getParamName()
							+ " is not part of the schema.This is probably an invalid configuration.");
				}
				final String commandDeleteRegexpStr = pubParams.get(Parameters.COMMAND_DELETE_EGEXP.getParamName())
						.toString();
				commandDeleteRegexp = Pattern.compile(commandDeleteRegexpStr);
			}
			// logField schema map
			String value = pubParams.get(Parameters.LOGFIELD_STATUS.getParamName()).toString();
			if ((value != null) && !value.trim().equals("")) {
				logFieldschemaMap.put(LogField.LOGSTATUS, value);
			}
			value = pubParams.get(Parameters.LOGFIELD_STATUSMESSAGE.getParamName()).toString();
			if ((value != null) && !value.trim().equals("")) {
				logFieldschemaMap.put(LogField.LOGMESSAGE, value);
			}
			value = pubParams.get(Parameters.LOGFIELD_RECORDNUMBER.getParamName()).toString();
			if ((value != null) && !value.trim().equals("")) {
				logFieldschemaMap.put(LogField.RECORDNUMBER, value);
			}
			value = pubParams.get(Parameters.LOGFIELD_EVENTID.getParamName()).toString();
			if ((value != null) && !value.trim().equals("")) {
				logFieldschemaMap.put(LogField.LOGEVENTID, value);
			}
			trace.trace("Log schema:" + logFieldschemaMap);

			// The requested metaData
			param = pubParams.get(Parameters.META_DATA.getParamName());
			trace.trace("MetaData read:" + param);
			metaData = new MetaDataManager(param.toString());
			trace.trace("MetaData result:" + metaData);
			// The fields that should be marked as sensitive
			final String sensitive = pubParams.get(Parameters.SENSITIVE_ATTRIBUTES.getParamName()).toString();
			if ((sensitive != null) && !"".equals(sensitive.trim())) {
				this.sensitiveAttributes = Arrays.asList(sensitive.split(","));
			} else {
				this.sensitiveAttributes = new ArrayList<String>(0);
			}
			initStrategyMap(init, pubParams);

			// Publisher STATE
			initState(init);

			// construct a driver filter for the publication shim to use for
			// filtering application events in start(); in an actual driver,
			// the publisher would use the filter to filter events from the
			// application to avoid publishing unnecessary events to the DirXML
			// engine
			//
			// NOTE: the skeleton publisher doesn't actually make use of the
			// filter, but this code is here to illustrate how to get the
			// publisher filter from the init document
			filter = init.driverFilter();

			// perform any other initialization that might be required

			// append a successful <status> element to the result doc
			attrs = StatusAttributes.factory(StatusLevel.SUCCESS, StatusType.DRIVER_STATUS, null); // event-id
			status = XDSUtil.appendStatus(result, // doc to append to
					attrs, null); // description

			// append the parameter values the publisher is actually using
			allParams.putAll(pubParams);
			status.parametersAppend(allParams);
		} // try
		catch (final ClassCastException e) // don't want to catch Error class
											// with Throwable
		{
			Util.printStackTrace(trace, e);
			attrs = StatusAttributes.factory(StatusLevel.FATAL, StatusType.DRIVER_STATUS, null); // event-id
			XDSUtil.appendStatus(result, // doc to append to
					attrs, // status attribute values
					"One or more strategies specified in the publisher channel are not implementing the correct interface.", // description
					e, // exception
					XDSUtil.appendStackTrace(e), // append stack trace?
					initXML); // xml to append
		} catch (final Exception e) // don't want to catch Error class with
									// Throwable
		{
			Util.printStackTrace(trace, e);
			// e instance of XDSException:
			//
			// init document is malformed or invalid -- or --
			// it is missing required parameters or contains
			// illegal parameter values

			// e instance of RuntimeException:

			// e.g., NullPointerException

			attrs = StatusAttributes.factory(StatusLevel.FATAL, StatusType.DRIVER_STATUS, null); // event-id
			XDSUtil.appendStatus(result, // doc to append to
					attrs, // status attribute values
					null, // description
					e, // exception
					XDSUtil.appendStackTrace(e), // append stack trace?
					initXML); // xml to append
		} // catch

		// return result doc w/ status to DirXML
		return result.toXML();
	}// init(XmlDocument):XmlDocument

	/**
	 * Populate the strategy map with all configured strategies.
	 *
	 * @param init
	 * @param pubParams
	 * @throws Exception
	 */
	private void initStrategyMap(final XDSInitDocument init, final Map<String, Parameter> pubParams) throws Exception {
		// Logging is an exception in the strategy:
		// if LOGENABLED is false, then the strategy is NoLogger
		final boolean isLogging = Boolean.valueOf(pubParams.get(Parameters.LOGENABLED.getParamName()).toString());
		for (final Strategies strategy : Strategies.values()) {
			trace.trace("init - Creating strategy: " + strategy);
			final IStrategy strategyInstance;
			final Parameter param = pubParams.get(strategy.parameter.getParamName());
			if ((strategy == Strategies.LOGGER) && !isLogging) {
				strategyInstance = initStrategy(init, PublisherNoLogger.class.getName(), allParams);
			} else {
				strategyInstance = initStrategy(init, param.toString(), allParams);
			}
			trace.trace("init - Created strategy instance: " + strategyInstance.getClass().getName());
			if (strategy.interfaceClass.isAssignableFrom(strategyInstance.getClass())) {
				strategyMap.put(strategy, strategyInstance);
			} else {
				throw new ClassCastException(param.toString() + " cannot be casted to " + strategy.interfaceClass);
			}
		}

	}

	/**
	 * Initialize the state of this driver (did it complete the file last
	 * time?).
	 *
	 * @param init
	 */
	private void initState(final XDSInitDocument init) {
		final XDSInitParamsElement pubOptionsParam = init.extractInitParamsElement();
		final XDSPublisherStateElement pubOptions = pubOptionsParam.extractPublisherStateElement();
		if (pubOptions != null) {
			final String recNumber = pubOptions.attributeValueGet(STATE_RECORDNUMBER);
			if (recNumber == null) {
				trace.trace("init - Initial driver boot (no recNumber found)");
			} else if ("-1".equals(recNumber)) {
				trace.trace("init - Last file finished nicely. Nothing else to do.");
			} else {
				trace.trace("init - Last file did not complete.", TraceLevel.ERROR_WARN);
				final String fileReaderFromState = pubOptions.attributeValueGet(STATE_FILRE_READER_CLASS);
				final IStrategy readerObject = strategyMap.get(Strategies.FILEREADER);
				if (readerObject.getClass().getName().equals(fileReaderFromState)) {
					// We still use the same class
					final String filePath = pubOptions.attributeValueGet(STATE_FILE_PATH);
					stateMeta = new PublisherStateMeta(Integer.parseInt(recNumber), filePath);
					trace.trace("init - Will start file " + filePath + " after record " + recNumber,
							TraceLevel.ERROR_WARN);
				} else {
					trace.trace(
							"init - File reader changed from " + fileReaderFromState + " to "
									+ readerObject.getClass().getName() + ". Unable to reprocess last file.",
							TraceLevel.ERROR_WARN);
				}
			}
		} else {
			trace.trace("init - Initial driver boot (no publisher state found)");
		}
	}

	/**
	 * <code>start()</code> starts the <code>PublicationShim</code>. The
	 * publisher shim should not return from start until DriverShim.shutdown()
	 * is called, or a fatal error occurs. Returning prematurely from
	 * <code>start()</code> will cause the DirXML engine to shut down the
	 * driver.
	 * <p>
	 *
	 * @param processor
	 *            <code>XmlCommandProcessor</code> that can invoked in order to
	 *            publish information to eDirectory on behalf of the
	 *            application; processor must only be invoked from the thread on
	 *            which <code>start()</code> was invoked
	 * @return XML document containing status from start operation
	 * @see GenericFileDriverShim#shutdown(XmlDocument)
	 */
	@Override
	public XmlDocument start(final XmlCommandProcessor processor) {
		// MODIFY: implement your publisher here

		// example result document:

		//@formatter:off
		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product build="20021214_0304" instance="Skeleton Driver (Java, XDS)" version="1.1">DirXML Skeleton Driver (Java, XDS)</product>
                <contact>My Company Name</contact>
            </source>
            <output>
                <status level="success" type="driver-status"/>
            </output>
        </nds>
		 */
		//@formatter:on

		// NOTE: this implements a polling method of communication with the
		// application; this may not be appropriate if the application
		// supports an event notification system

		trace.trace("start", TraceLevel.TRACE);

		// create result document for reporting status to DirXML engine
		final XDSResultDocument result = driver.newResultDoc();
		final XmlDocument heartbeat = createHeartbeatDocument();

		StatusAttributes shutdownAttrs;
		long lastBeatTime, lastPollTime, elapsedTime, sleep;
		boolean published;

		// assume we'll shutdown normally
		shutdownAttrs = StatusAttributes.factory(StatusLevel.SUCCESS, StatusType.DRIVER_STATUS, null);

		// ensure we poll first time into the loop
		lastPollTime = pollingInterval;
		lastBeatTime = heartbeatInterval;

		try {
			// If the last file did not complete, process it.
			if (stateMeta != null) {
				if (stateMeta.getRecordNumber() >= 0) {
					processFile(processor, new File(stateMeta.getFileName()), stateMeta.getRecordNumber());
				}
				// clean up memory.
				stateMeta = null;
			}
			// loop until we're told to shutdown (or some fatal error occurs)
			while (!shutdown) {
				// skeleton implementation just wakes up every so often to
				// see if it needs to poll, issue a heartbeat, or
				// shutdown and return
				try {
					published = false;
					if (lastPollTime >= pollingInterval) {
						published = poll(processor);
						lastPollTime = 0;
					} // if

					if (published) {
						// sending any document equates to a heartbeat
						lastBeatTime = 0;
					} else if (lastBeatTime >= heartbeatInterval) {
						lastBeatTime = 0;
						if (doHeartbeat) {
							trace.trace("sending heartbeat", 2);
							processor.execute(heartbeat, this);
						}
					} // if

					// how long do we need to sleep for to poll again or
					// issue a heartbeat?
					sleep = Math.min(pollingInterval - lastPollTime, heartbeatInterval - lastBeatTime);
					// 0 == sleep forever
					if (sleep == 0) {
						sleep = interval;
					}

					// wait for subscriber channel thread to wake us up, for
					// polling interval to expire, or heartbeat interval to
					// expire
					//
					// NOTE: the use of the semaphore is highly recommended. It
					// prevents a long polling interval from interfering with
					// the
					// orderly shutdown of the driver.

					synchronized (semaphore) {
						if (!shutdown) {
							final long start = System.currentTimeMillis();
							trace.trace("Sleeping for " + (sleep / 1000) + " seconds", 2);
							semaphore.wait(sleep);
							elapsedTime = (System.currentTimeMillis() - start);
							lastPollTime += elapsedTime;
							lastBeatTime += elapsedTime;
						}
					}
				} // try
				catch (final InterruptedException ie) {
					Util.printStackTrace(trace, ie);
					// we've been woken by the subscriber thread;
					// it's time to shutdown
				}
			} // while

			// append a successful <status> element to the result doc
			XDSUtil.appendStatus(result, // doc to append to
					shutdownAttrs, // status attribute values
					null); // description
		} // try
		catch (final Exception e) // don't want to catch Error class with
									// Throwable
		{
			// something bad happened...
			Util.printStackTrace(trace, e);
			shutdownAttrs.setLevel(StatusLevel.FATAL);
			XDSUtil.appendStatus(result, // doc to append to
					shutdownAttrs, // status attribute values
					null, // description
					e, // exception
					true, // append stack trace?
					null); // xml to append
		} finally {
			trace.trace("stopping...", 2);
			// Release any resources that need to be released...
			strategyMap.clear();
		}

		// return result doc w/ status to DirXML engine
		return result.toXML();
	}// start(XmlCommandProcessor):XmlDocument

	private XmlDocument createHeartbeatDocument() {
		final XDSHeartbeatDocument heartbeat = new XDSHeartbeatDocument();
		driver.appendSourceInfo(heartbeat);
		return heartbeat.toXML();
	}

	/**
	 * Get the full path name to the location where the file must be saved.
	 * Folder ends with the separatorChar
	 *
	 * @return
	 */
	private String getNewTargetFolder() {
		// fileDest + 2003.09.30_08.54.48
		final Date d = new Date();
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");

		final StringBuffer sb = new StringBuffer(workDir);
		sb.append(sdf.format(d));
		sb.append(File.separatorChar);
		return sb.toString();
	}

	/**
	 * A non-interface method called from
	 * <code>SkeletonPublicationShim.start(XmlCommandProcessor)</code> to poll
	 * the application for events.
	 * <p>
	 *
	 * @see FileDriverPublicationShimImpl#start(XmlCommandProcessor)
	 */
	private boolean poll(final XmlCommandProcessor processor) {
		boolean published = false;
		trace.trace("poll start", TraceLevel.TRACE);
		File[] inputFiles = null;
		File targetFolder = null;
		final IFileLocatorStrategy fileLocator = (IFileLocatorStrategy) strategyMap.get(Strategies.FILELOCATOR);
		final IPubFileCleanStrategy pubCleaner = (IPubFileCleanStrategy) strategyMap.get(Strategies.FILECLEANER);
		final IFileSorterStrategy fileSorter = (IFileSorterStrategy) strategyMap.get(Strategies.FILESORTER);
		while (!shutdown && ((inputFiles = fileLocator.getFileList()) != null)) {
			// Get the correct file from the list
			final File theInputFile = fileSorter.getFirstFile(inputFiles);
			trace.trace("File to process:" + theInputFile.getName(), TraceLevel.TRACE);
			// Notify any file cleaning strategy
			pubCleaner.onPreFile(theInputFile);
			// Move the file to the work folder. As long as we have input, we
			// use the same work folder.
			// Note: this might cause in issue when a file with the same name is
			// created while we are still processing the others.
			if (targetFolder == null) {
				targetFolder = new File(getNewTargetFolder());
				targetFolder.mkdirs();
				if (targetFolder.exists()) {
					trace.trace("Folder created:" + targetFolder.getName(), TraceLevel.TRACE);
				} else {
					trace.trace("Folder creation failed:" + targetFolder.getName(), TraceLevel.ERROR_WARN);
				}
			}
			this.currentFile = new File(targetFolder, theInputFile.getName());
			trace.trace("File size before move:" + theInputFile.length());
			if (Util.moveFile(trace, theInputFile, currentFile)) {
				trace.trace("File size after move:" + currentFile.length());
				// OK, process the actual file now
				published = processFile(processor, currentFile, 0);
				// Notify any cleaning strategy that we are done.
				pubCleaner.onPostFile(currentFile);
			} else {
				trace.trace("poll - Move failed!", TraceLevel.ERROR_WARN);
			}
			this.currentFile = null;
		}

		trace.trace("poll done", TraceLevel.TRACE);
		return published;
		// example command document:
		//@formatter:off
		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product build="20021214_0304" instance="Skeleton Driver (Java, XDS)" version="1.1">DirXML Skeleton Driver (Java, XDS)</product>
                <contact>My Company Name</contact>
            </source>
            <input>
                <status event-id="0" level="warning" type="driver-general">
                    <description>Publisher not implemented.</description>
                </status>
            </input>
        </nds>
		 */

		// example result document:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product version="1.1a">DirXML</product>
                <contact>Novell, Inc.</contact>
            </source>
            <output>
                <status event-id="0" level="success"></status>
            </output>
        </nds>
		 */
		//@formatter:on

	}// poll(XmlCommandProcessor):boolean

	/**
	 * Process a file. The file has already been moved to the work folder.
	 *
	 * @param processor
	 * @param published
	 * @param workFile
	 * @return true of any document got processed
	 */
	private boolean processFile(final XmlCommandProcessor processor, final File workFile, final int recordsToSkip) {
		trace.trace("processFile: start", TraceLevel.TRACE);
		boolean published = false;
		final Map<String, String> metaDataMap = metaData.getStaticMetaData(workFile);
		trace.trace("processFile: staticMetaData added:" + metaDataMap, TraceLevel.TRACE);
		final Map<String, String> stateDataMap = getInitialStateMap(workFile);

		final IPublisherLoggerStrategy fileLogger = (IPublisherLoggerStrategy) strategyMap.get(Strategies.LOGGER);

		try { // try to process this file
			final IFileReadStrategy fileReader = (IFileReadStrategy) strategyMap.get(Strategies.FILEREADER);

			// Start the file
			currentFile = workFile;
			fileReader.openFile(workFile);

			int recordNumber = skipRecords(recordsToSkip);

			Map<String, String> thisRecord = fileReader.readRecord();
			Map<String, String> nextRecord = fileReader.readRecord();

			// sendStatusState(processor, workFile, stateDataMap, recordNumber);

			// Open the file logger
			try {
				fileLogger.openFile(getLogFileFor(workFile), fileReader.getActualSchema(), logFieldschemaMap);
			} catch (final WriteException e1) {
				trace.trace("Logging will be disabled since the file generated an error.", TraceLevel.ERROR_WARN);
				Util.printStackTrace(trace, e1);
			}

			// Process the records
			while ((thisRecord != null) && !shutdown) {// While we have records,
														// and while we do not
														// need to shut down
				recordNumber++;
				try {
					metaData.addDynamicMetaData(metaDataMap, thisRecord, nextRecord, recordNumber);
					trace.trace("processFile: dynamicMetaData added:" + metaDataMap, TraceLevel.TRACE);

					// Execute the command
					final boolean recordProcessed = sendRecord(processor, fileLogger, recordNumber, thisRecord,
							stateDataMap);
					// If the record was not processed, we need to send a status
					// message with the previous record number
					if (!recordProcessed) {
						sendStatusState(processor, workFile, stateDataMap, recordNumber - 1);
					}
					published = true;
				} catch (final Exception e) {
					// Some exception. Submit the error on the pub. channel and
					// go to next record.
					final XDSCommandDocument command = driver.newCommandDoc();
					final StatusAttributes pollAttrs = StatusAttributes.factory(StatusLevel.ERROR,
							StatusType.DRIVER_GENERAL, null); // event-id
					XDSUtil.appendStatus(command, // doc to append to
							pollAttrs, // status attribute values
							Errors.PROCESS_ERROR, // description
							e, // exception
							true, // append stack trace?
							null); // xml to append
					processor.execute(command.toXML(), this);
					published = true;
				}
				thisRecord = nextRecord;
				nextRecord = fileReader.readRecord();
			}
			// Close the status for this driver if we do not need to shut down
			if (!shutdown) {
				sendStatusState(processor, workFile, stateDataMap, -1);
				published = true;
			}
			if (!published) {
				sendStatusState(processor, workFile, stateDataMap, recordNumber);
				published = true;
			}
			// Close the fileReader
			currentFile = null;
			fileReader.close();
			// Close the logger
			try {
				fileLogger.close();
			} catch (final WriteException e) {
				trace.trace("Failed to close the log file.", TraceLevel.ERROR_WARN);
				Util.printStackTrace(trace, e);
			}
		} catch (final ReadException re) {
			final XDSCommandDocument command = driver.newCommandDoc();
			final StatusAttributes pollAttrs = StatusAttributes.factory(StatusLevel.ERROR, StatusType.DRIVER_GENERAL,
					null); // event-id
			// Append the correct exception message
			Exception e = (Exception) re.getCause();
			if (e == null) {
				e = re;
			}
			trace.trace(Errors.FILE_READ_ERROR + "(" + workFile.getAbsolutePath() + ")" + e.getMessage(),
					TraceLevel.ERROR_WARN);
			XDSUtil.appendStatus(command, // doc to append to
					pollAttrs, // status attribute values
					Errors.FILE_READ_ERROR + "(" + workFile.getAbsolutePath() + ")", // description:
																						// Communications
																						// error.
					e, // exception
					false, // append stack trace?
					null); // xml to append
			processor.execute(command.toXML(), this);
			published = true;
		}
		trace.trace("processFile done", TraceLevel.TRACE);
		return published;
	}

	/**
	 * Get the file name to use for logging purposes. For a file xxx.csv, this
	 * will first try to get the xxx.log.csv If that exists, it will try
	 * xxx.n.log.csv, where n is an increasing number
	 *
	 * @param workFile
	 * @return
	 */
	private static File getLogFileFor(final File workFile) {
		final String fileName = workFile.getName();
		final int extentionPos = fileName.lastIndexOf('.');
		final String extentionPart = fileName.substring(extentionPos + 1);
		final String namePart = fileName.substring(0, extentionPos);

		File logfile = new File(workFile.getParentFile(), namePart + ".log." + extentionPart);
		if (logfile.exists()) {
			int index = 0;
			while (true) {
				logfile = new File(workFile.getParentFile(), namePart + "." + index + ".log." + extentionPart);
				if (logfile.exists()) {
					index++;
				} else {
					return logfile;
				}
			}
		}
		return logfile;
	}

	private void sendStatusState(final XmlCommandProcessor processor, final File workFile,
			final Map<String, String> stateDataMap, final int recordNumber) {
		final XDSCommandDocument command = driver.newCommandDoc();

		// In order to avoid "State only, not sending to remote side", we append
		// a status to the state and disguise it as a heartbeat
		final StatusAttributes pollAttrs = StatusAttributes.factory(StatusLevel.SUCCESS, StatusType.HEARTBEAT,
				"gfd-pub-state-" + getNextEventId());
		XDSUtil.appendStatus(command, // doc to append to
				pollAttrs, // status attribute values
				"Save state of file " + workFile.getName(), // description
				null, // exception
				false, // append stack trace?
				null); // xml to append

		// Submit the current state on the publisher channel
		// TODO: Issue: State only, not sending to remote side...
		addPublisherState(command, stateDataMap, recordNumber);
		trace.trace("sendStatusState: heartbeat with status data.", TraceLevel.TRACE);
		processor.execute(command.toXML(), this);
	}

	/**
	 * Send a given record to the publisher channel, doing 'retry' if indicated
	 * to do so.
	 *
	 * @param processor
	 * @param recordNumber
	 * @param command
	 * @return true if the record was processed with or without succes. False if
	 *         the record was not processed (retry & shutdown)
	 * @throws XDSParseException
	 */
	private boolean sendRecord(final XmlCommandProcessor processor, final IPublisherLoggerStrategy fileLogger,
			final int recordNumber, final Map<String, String> thisRecord, final Map<String, String> stateDataMap) {

		XDSCommandResultDocument response = null;
		boolean toRetry = false;

		try {

			// Create the add command based on the attributes read
			final XDSCommandDocument command = driver.newCommandDoc();
			addCommandElement(processor, command, thisRecord);

			// Add the state info for this record
			addPublisherState(command, stateDataMap, recordNumber);

			do {
				toRetry = false;
				final XmlDocument xmlCommand = command.toXML();
				trace.trace("processFile: Executing record " + recordNumber, TraceLevel.DEBUG);
				trace.trace(xmlCommand);
				final XmlDocument executeResponse = processor.execute(xmlCommand, this);
				response = new XDSCommandResultDocument(executeResponse);

				// check command results
				final StatusLevel level = response.mostSevereStatusLevel("");
				trace.trace("processFile: status == " + Util.toLiteral(level.toString()), TraceLevel.DEBUG);

				@SuppressWarnings("unchecked")
				final List<XDSStatusElement> statusList = response.extractStatusElements();
				// If we did not get a status, make sure to log an entry as well
				if ((statusList == null) || (statusList.size() == 0)) {
					try {
						fileLogger.logCommand(recordNumber, thisRecord, null);
					} catch (final WriteException e) {
						trace.trace("Writing record entry to log file failed.", TraceLevel.ERROR_WARN);
						Util.printStackTrace(trace, e);
					}
				} else {
					for (final XDSStatusElement xdsStatusElement : statusList) {
						try {
							fileLogger.logCommand(recordNumber, thisRecord, xdsStatusElement);
						} catch (final WriteException e) {
							trace.trace("Writing record entry to log file failed.", TraceLevel.ERROR_WARN);
							Util.printStackTrace(trace, e);
						}
						if (StatusLevel.RETRY.equals(xdsStatusElement.getLevel())) {
							toRetry = true;
						}
					}
				}
				if (toRetry) {
					trace.trace("processFile: Waiting for retry.", TraceLevel.ERROR_WARN);
					try {
						synchronized (semaphore) {
							if (!shutdown) {
								semaphore.wait(30000);
							}
						}
					} catch (final InterruptedException e) {
						// Maybe a shutdown. No clue what else could interrupt
						// us.
						trace.trace("processFile: Retry was interrupted. Check the current state of the driver.", 1);
					}
				}
			} while (toRetry && !shutdown);
		} catch (final XDSParseException xds) {
			// the doc we got back is malformed or invalid due to style sheet
			// processing
			final XDSCommandDocument command = driver.newCommandDoc();
			final StatusAttributes pollAttrs = StatusAttributes.factory(StatusLevel.ERROR, StatusType.DRIVER_GENERAL,
					null); // event-id
			XDSUtil.appendStatus(command, // doc to append to
					pollAttrs, // status attribute values
					Errors.INVALID_DOC, // description
					xds, // exception
					false, // append stack trace?
					(response == null) ? null : response.toXML()); // xml to
																	// append
			processor.execute(command.toXML(), this);
			return true;
		} // catch
		return !toRetry;
	}

	/**
	 * Skip recordsToSkip from the inputfile. Return the current record number.
	 *
	 * @param recordsToSkip
	 * @return
	 * @throws ReadException
	 */
	private int skipRecords(final int recordsToSkip) throws ReadException {
		int recordNumber = 0;
		final IFileReadStrategy fileReader = (IFileReadStrategy) strategyMap.get(Strategies.FILEREADER);

		// skip the given number of records.
		if (recordsToSkip > 0) {
			trace.trace("Processing old file. Skipping " + recordsToSkip + " record(s).", TraceLevel.DEBUG);
			for (int i = 0; i < recordsToSkip; i++) {
				fileReader.readRecord();
				recordNumber++;
			}
		}
		return recordNumber;
	}

	/**
	 * Get a map of data with the default publishers state.
	 *
	 * @param workFile
	 * @return
	 */
	private Map<String, String> getInitialStateMap(final File workFile) {
		final IFileReadStrategy fileReader = (IFileReadStrategy) strategyMap.get(Strategies.FILEREADER);
		final Map<String, String> stateDataMap = new HashMap<String, String>();
		stateDataMap.put(STATE_RECORDNUMBER, "-1");
		stateDataMap.put(STATE_FILE_PATH, workFile.getPath());
		stateDataMap.put(STATE_FILRE_READER_CLASS, fileReader.getClass().getName());
		return stateDataMap;
	}

	/**
	 * Add all key/value pairs from the given map as a state element Add the
	 * record number as a state element.
	 *
	 * @param command
	 * @param stateDataMap
	 * @param recordNumber
	 */
	private void addPublisherState(final StateDocument command, final Map<String, String> stateDataMap,
			final int recordNumber) {
		final XDSPublisherStateElement stateElem = command.appendInitParamsElement().appendPublisherStateElement();
		final Iterator<String> iter = stateDataMap.keySet().iterator();
		while (iter.hasNext()) {
			final String element = iter.next();
			final String value = stateDataMap.get(element);
			stateElem.attributeValueSet(element, value);
		}
		stateElem.attributeValueSet(STATE_RECORDNUMBER, recordNumber + "");
	}

	/**
	 * Extend the given command document by adding a command element with all
	 * attributes in the map. The operation in the command element (add, modify
	 * or delete) is determined based on the configuration of the driver: -
	 * DYNAMIC_INPUT - ADD - MODIFY - DELETE
	 *
	 * @param thisRecord
	 * @return
	 * @throws XDSParseException
	 */
	private void addCommandElement(final XmlCommandProcessor processor, final XDSCommandDocument command,
			final Map<String, String> thisRecord) throws XDSParseException {
		// OK, we have all the data. Create and execute the command
		final String assValue = driver.getAssociationField(thisRecord);
		String destDn = null;
		GenerateCommnd calculatedCommand = commandToGenerate;
		if (calculatedCommand == GenerateCommnd.DYNAMIC_INPUT) {
			// The input partly determines the event generated:
			// Delete => Delete
			// Else: dynamic add/modify
			calculatedCommand = GenerateCommnd.DYNAMIC;
			final String value = thisRecord.get(commandIdentifierField);
			if (value != null) {
				if (commandDeleteRegexp.matcher(value).matches()) {
					trace.trace("Field " + commandIdentifierField + "(" + value
							+ ") matches delete regexp. Generating delete event.");
					calculatedCommand = GenerateCommnd.DELETE;
				} else {
					trace.trace("Field " + commandIdentifierField + "(" + value
							+ ") does not match delete regexp. Using dynamic add/modify.");
				}
			} else {
				trace.trace("Field " + commandIdentifierField + "is unvalued. Using dynamic add/modify.");
			}
		}

		if (calculatedCommand == GenerateCommnd.DYNAMIC) {
			if (assValue == null) {
				calculatedCommand = GenerateCommnd.ADD;
			} else {
				// Get the actual commandname by querying the IDV based on the
				// association
				// If we find an object, it is a modify, otherwise it is an add
				trace.trace("Command is dynamic. Querying vault to find associated object.", TraceLevel.DEBUG);
				final XDSQueryDocument query = Util.createQueryDoc(((GenericFileDriverShim) driver).getObjectClass(),
						null, assValue, null, null, null);
				final XmlDocument response = processor.execute(query.toXML(), null);
				final XDSQueryResultDocument result = new XDSQueryResultDocument(response);
				@SuppressWarnings("unchecked")
				final List<XDSInstanceElement> instances = result.extractInstanceElements();

				if ((instances != null) && (instances.size() == 1)) {
					calculatedCommand = GenerateCommnd.MODIFY;
					final XDSInstanceElement singleton = instances.get(0);
					destDn = singleton.getSrcDN();
					trace.trace("Exactly one instance found. Will trigger a 'modify' command.", TraceLevel.DEBUG);
				} else {
					calculatedCommand = GenerateCommnd.ADD;
					trace.trace("Zero or multiple instances found. Will trigger an 'add' command.", TraceLevel.DEBUG);
				}
			}
		}

		_addCommandElement(command, thisRecord, assValue, destDn, calculatedCommand);
	}

	/**
	 * Make sure that we do not overflow. Start again if we would.
	 *
	 * @return
	 */
	private long getNextEventId() {
		if (eventid == Long.MAX_VALUE) {
			eventid = 0;
		}
		eventid++;
		return eventid;
	}

	/**
	 * Extend the given command document by adding a command element with all
	 * attributes in the map. The operation in the command element (add, modify
	 * or delete) is a paramater: - ADD - MODIFY - DELETE
	 *
	 * @param command
	 * @param thisRecord
	 * @param assValue
	 * @param destDn
	 * @param calculatedCommand
	 *            Command to generate (non-dynamic)
	 */
	private void _addCommandElement(final XDSCommandDocument command, final Map<String, String> thisRecord,
			final String assValue, final String destDn, final GenerateCommnd calculatedCommand) {
		// OK, now do the actual command generation
		switch (calculatedCommand) {
		case ADD: {
			// We generate an 'add' event
			final XDSAddElement addElement = command.appendAddElement();
			addElement.setClassName(((GenericFileDriverShim) driver).getObjectClass());
			// If we have an association, set the association
			if (assValue != null) {
				addElement.appendAssociationElement(assValue);
			}

			// If we have a src-dn, set the association
			final String srcValue = driver.getSourceField(thisRecord);
			if ((srcValue != null) && !"".equals(srcValue)) {
				addElement.setSrcDN(srcValue);
			}
			addElement.setEventID("gfd-pub-" + (getNextEventId()));
			// Append all fields
			final Iterator<String> iter = thisRecord.keySet().iterator();
			while (iter.hasNext()) {
				final String key = iter.next();
				final String value = thisRecord.get(key);
				final XDSAddAttrElement addAttr = addElement.appendAddAttrElement();
				addAttr.setAttrName(key);
				final XDSValueElement valueElem = addAttr.appendValueElement(value);
				if (sensitiveAttributes.contains(key)) {
					valueElem.attributeValueSet("is-sensitive", "true");
				}
			}

			break;
		}
		case DELETE: {
			// We generate a 'delete' event
			final XDSDeleteElement deleteElem = command.appendDeleteElement();
			deleteElem.setClassName(((GenericFileDriverShim) driver).getObjectClass());
			if (assValue != null) {
				deleteElem.appendAssociationElement(assValue);
			}
			deleteElem.setEventID("gfd-pub-" + (eventid++));
			break;
		}
		default: {
			// We generate a 'modify' event
			final XDSModifyElement modElement = command.appendModifyElement();
			modElement.setClassName(((GenericFileDriverShim) driver).getObjectClass());
			if (destDn != null) {
				modElement.setDestDN(destDn);
			}
			// If we have an association, set the association
			if (assValue != null) {
				modElement.appendAssociationElement(assValue);
			}

			// If we have a src-dn, set it
			final String srcValue = driver.getSourceField(thisRecord);
			if (srcValue != null) {
				modElement.setSrcDN(srcValue);
			}
			modElement.setEventID("gfd-pub-" + (eventid++));
			// Append all fields
			final Iterator<String> iter = thisRecord.keySet().iterator();
			while (iter.hasNext()) {
				final String key = iter.next();
				final String value = thisRecord.get(key);
				final XDSModifyAttrElement addAttr = modElement.appendModifyAttrElement();
				addAttr.setAttrName(key);
				addAttr.appendRemoveAllValuesElement();
				final XDSAddValueElement addValue = addAttr.appendAddValueElement();
				final XDSValueElement valueElem = addValue.appendValueElement(value);
				if (sensitiveAttributes.contains(key)) {
					valueElem.attributeValueSet("is-sensitive", "true");
				}
			}
			break;
		}
		}
	}

	/**
	 * A non-interface method called from
	 * <code>SkeletonDriverShim.shutdown()</code> to signal the publisher thread
	 * that it needs to exit from <code>start()</code>.
	 * <p>
	 * NOTE: This method is called by a thread other than the publisher thread.
	 * <p>
	 *
	 * @see GenericFileDriverShim#shutdown(XmlDocument)
	 */
	@Override
	public void shutdown(final XDSResultDocument reasonXml) {
		// MODIFY: put your shutdown code here

		trace.trace("shutdown", TraceLevel.TRACE);

		// tell the publisher thread to wake up, if it happens to be sleeping
		synchronized (semaphore) {
			// tell publisher thread it's time to exit
			shutdown = true;

			// Call shutdown on every plugged in component.
			final Collection<IStrategy> keys = strategyMap.values();
			for (final IStrategy aStrategy : keys) {
				if (aStrategy instanceof IShutdown) {
					((IShutdown) aStrategy).onShutdown(reasonXml);
				}
			}

			semaphore.notifyAll();
		}
	}// shutdown():void

	/**
	 * <code>query</code> will accept an XDS-encoded query and return the
	 * results. Will return en error when no current file is being processed.
	 * <p>
	 *
	 * @param queryXML
	 *            a document containing an XDS-encoded query
	 * @return the results of the query
	 */
	@Override
	public XmlDocument query(final XmlDocument queryXML) {

		trace.trace("query --== beta implementation ==--", 1);
		// Execute a query on the current file being read.
		if (currentFile == null) {
			// No file is being read at this moment. Return error.
			final String mssg = "No file is currently being processed.";
			return generateError(mssg);
		}

		final XDSQueryResultDocument result = new XDSQueryResultDocument();
		driver.appendSourceInfo(result);

		try {
			final XDSQueryDocument queryDoc = new XDSQueryDocument(queryXML);
			@SuppressWarnings("unchecked")
			final List<XDSQueryElement> queryList = queryDoc.extractQueryElements();
			boolean errorAdded = false;
			if (queryList.size() > 0) {
				final XDSQueryElement queryElem = queryList.get(0);// We only
																	// support
																	// one query
																	// element...
				// We ignore the class name in the query
				final List<String> attributesToRead = getAttributesToRead(queryElem);
				final QueryMatcher matcher = QueryMatcher.getMatcher(trace, queryDoc, driver);

				final Map<String, String> metaDataMap = metaData.getStaticMetaData(currentFile);
				// Map stateDataMap = getInitialStateMap(workFile);

				final IFileReadStrategy queryFileReader = createQueryFileReader();
				try {
					queryFileReader.openFile(currentFile);
					Map<String, String> thisRecord = queryFileReader.readRecord();
					Map<String, String> nextRecord = queryFileReader.readRecord();
					int recordNumber = 0;

					while (thisRecord != null) {
						recordNumber++;
						try {
							metaData.addDynamicMetaData(metaDataMap, thisRecord, nextRecord, recordNumber);
							// Read all records and add the matching records to
							// the result
							if (matcher.matchesRecord(thisRecord)) {
								addInstanceToQueryResult(thisRecord, result, attributesToRead);
							}
						} catch (final Exception e) {
							final StringWriter sw = new StringWriter();
							final PrintWriter ps = new PrintWriter(sw);
							e.printStackTrace(ps);
							trace.trace(sw.toString(), 0);
							// Add errorstatus to query result doc and continue
							// with next record
							errorAdded = true;
							final StatusAttributes pollAttrs = StatusAttributes.factory(StatusLevel.ERROR,
									StatusType.DRIVER_GENERAL, null); // event-id
							XDSUtil.appendStatus(result, // doc to append to
									pollAttrs, // status attribute values
									Errors.PROCESS_ERROR, // description
									e, // exception
									true, // append stack trace?
									null); // xml to append
						}

						// Next record
						thisRecord = nextRecord;
						nextRecord = queryFileReader.readRecord();
					}
				} finally {
					// Close the file
					try {
						queryFileReader.close();
					} catch (final Exception e) {
						// Eat this exception. We might have another one...
					}
				}
			}
			// Add query status if no error status was added
			if (!errorAdded) {
				final StatusAttributes attrs = StatusAttributes.factory(StatusLevel.SUCCESS, StatusType.DRIVER_GENERAL,
						null);// event ID
				XDSUtil.appendStatus(result, // doc to append to
						attrs, // status attribute values
						""); // description
			}
			return result.toXML();

		} catch (final Exception e) {
			final StringWriter sw = new StringWriter();
			final PrintWriter ps = new PrintWriter(sw);
			e.printStackTrace(ps);
			trace.trace(sw.toString(), 0);
			final String mssg = "Error while executing query:" + e.getClass().getName() + " - " + e.getMessage();
			return generateError(mssg);
		}

	}// query(XmlDocument):XmlDocument

	/**
	 * Create a new fileReader instance used for querying the file (support for
	 * publisher query)
	 *
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws Exception
	 */
	private IFileReadStrategy createQueryFileReader() throws InstantiationException, IllegalAccessException, Exception {
		final IFileReadStrategy fileReader = (IFileReadStrategy) strategyMap.get(Strategies.FILEREADER);
		final IFileReadStrategy queryFileReader = fileReader.getClass().newInstance();

		queryFileReader.init(trace, allParams, this);
		return queryFileReader;
	}

	private List<String> getAttributesToRead(final XDSQueryElement queryElem) {
		// Note: we ignore the class name in the query
		@SuppressWarnings("unchecked")
		final List<XDSReadAttrElement> xdsAttributesToRead = queryElem.extractReadAttrElements();
		final List<String> attributesToRead = new ArrayList<String>(xdsAttributesToRead.size());
		final Iterator<XDSReadAttrElement> iter = xdsAttributesToRead.iterator();
		while (iter.hasNext()) {
			final XDSReadAttrElement readElem = iter.next();
			attributesToRead.add(readElem.getAttrName());
		}
		return attributesToRead;
	}

	private void addInstanceToQueryResult(final Map<String, String> thisRecord, final XDSQueryResultDocument result,
			final List<String> attributesToRead) {
		final XDSInstanceElement instance = result.appendInstanceElement();
		instance.setClassName(((GenericFileDriverShim) driver).getObjectClass());
		// If we have an association, set the association
		final String assValue = driver.getAssociationField(thisRecord);
		if (assValue != null) {
			instance.appendAssociationElement(assValue);
		}

		// If we have a src-dn, set the association
		final String srcValue = driver.getSourceField(thisRecord);
		if (srcValue != null) {
			instance.setSrcDN(srcValue);
		}
		// Append all fields
		final Iterator<String> iter = attributesToRead.iterator();
		while (iter.hasNext()) {
			final String key = iter.next();
			final String value = thisRecord.get(key);
			if ((value != null) && !"".equals(value)) {
				final XDSAttrElement addAttr = instance.appendAttrElement();
				addAttr.setAttrName(key);
				addAttr.appendValueElement(value);
			}
		}
	}

	private XmlDocument generateError(final String mssg) {
		final XDSQueryResultDocument result = new XDSQueryResultDocument();
		driver.appendSourceInfo(result);

		final StatusAttributes attrs = StatusAttributes.factory(StatusLevel.ERROR, StatusType.DRIVER_GENERAL, null);// event
																													// ID
		XDSUtil.appendStatus(result, // doc to append to
				attrs, // status attribute values
				mssg); // description
		return result.toXML();
	}

	/**
	 * The publisher can extend the schema by adding metadata elements
	 *
	 * @param userClassDef
	 */
	@Override
	public void extendSchema(final XDSClassDefElement userClassDef, final XmlDocument initXML)
			throws XDSParseException, XDSParameterException {
		// Init has not yet been called when the schema is querried => build the
		// metadata manually.
		// The wanted metaData
		final XDSInitDocument init;
		final Parameter param;

		// parse initialization document
		init = new XDSInitDocument(initXML);

		// get any publisher options from init doc
		/*
		 * final Map<String,Parameter> pubParams =
		 * getDefaultParameterDefs();//getShimParameters();
		 * init.parameters(pubParams);
		 *
		 * param =
		 * (Parameter)pubParams.get(Parameters.META_DATA.getParamName()); final
		 * List<String> metaData = Arrays.asList(param.toString().split(","));
		 */
		final Iterator<String> iter = metaData.getSchemaFields().iterator();

		while (iter.hasNext()) {
			final String aField = iter.next();
			final XDSAttrDefElement attrDef; // an <attr-def> element from the
												// result doc
			attrDef = userClassDef.appendAttrDefElement();
			attrDef.setAttrName(aField);
			attrDef.setMultiValued(false);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * info.vancauwenberge.filedriver.shim.PublisherChannelShim#getWorkDir()
	 */
	@Override
	public String getWorkDir() {
		return workDir;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * info.vancauwenberge.filedriver.filepublisher.IPublisher#getCurrentFile()
	 */
	@Override
	public File getCurrentFile() {
		return currentFile;
	}

}