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
package info.vancauwenberge.filedriver.shim.driver;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.novell.nds.dirxml.driver.ClassFilter;
import com.novell.nds.dirxml.driver.DriverFilter;
import com.novell.nds.dirxml.driver.DriverShim;
import com.novell.nds.dirxml.driver.PublicationShim;
import com.novell.nds.dirxml.driver.SubscriptionShim;
import com.novell.nds.dirxml.driver.XmlCommandProcessor;
import com.novell.nds.dirxml.driver.XmlDocument;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.EnumConstraint;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.QueryScope;
import com.novell.nds.dirxml.driver.xds.RequiredConstraint;
import com.novell.nds.dirxml.driver.xds.StatusLevel;
import com.novell.nds.dirxml.driver.xds.StatusType;
import com.novell.nds.dirxml.driver.xds.XDSAttrDefElement;
import com.novell.nds.dirxml.driver.xds.XDSAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSClassDefElement;
import com.novell.nds.dirxml.driver.xds.XDSComponentElement;
import com.novell.nds.dirxml.driver.xds.XDSInitDocument;
import com.novell.nds.dirxml.driver.xds.XDSInstanceElement;
import com.novell.nds.dirxml.driver.xds.XDSParseException;
import com.novell.nds.dirxml.driver.xds.XDSQueryDocument;
import com.novell.nds.dirxml.driver.xds.XDSQueryResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSSchemaDefElement;
import com.novell.nds.dirxml.driver.xds.XDSSchemaResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSStatusElement;
import com.novell.nds.dirxml.driver.xds.XDSValueElement;
import com.novell.nds.dirxml.driver.xds.util.StatusAttributes;
import com.novell.nds.dirxml.driver.xds.util.XDSUtil;
import com.novell.soa.script.mozilla.javascript.Context;
import com.novell.xml.util.Base64Codec;

//DriverShim, PublicationShim, and SubscriptionShim are the interfaces
//  a driver needs to implement to interface with the DirXML engine

import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.ISubscriberShim;
import info.vancauwenberge.filedriver.filepublisher.FileDriverPublicationShimImpl;
import info.vancauwenberge.filedriver.shim.FileDriverPublicationShim;
import info.vancauwenberge.filedriver.shim.FileDriverSubscriptionShim;
import info.vancauwenberge.filedriver.util.EcmascriptBuilder;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;



/**
 *  A basic skeleton for implementing a <code>DriverShim</code>.
 *  <p>
 *  <code>com.novell.nds.dirxml.driver.DriverShim</code> defines the interface
 *  used by the DirXML engine to start stop an application shim.
 *  <p>
 *  A <code>DriverShim</code> must implement a constructor which takes no
 *  parameters in order for the DirXML engine to instantiate it.
 *  <p>
 *  NOTE:  the Skeleton Driver makes no use of .ini files or any
 *  information outside of that supplied through the initialization data
 *  supplied to the shim <code>init()</code> methods; in general, it is
 *  inappropriate for a driver to store configuration information outside of
 *  eDirectory where it cannot be remotely configured using ConsoleOne,
 *  iManager, or other remote configuration utilities
 *  <p>
 *  NOTE:  it is unwise to have static mutable class data, since that
 *  prevents multiple instances of the driver from functioning independently
 */
public class GenericFileDriverShim
extends AbstractDriverShim
implements DriverShim, IDriver
{

	//this constant identifies the channel/module being traced
	//  (e.g., Skeleton Driver\Driver)
	private static final String TRACE_SUFFIX = "GenFileDrv";


	/**
	 * All driver parameters
	 * Public for testclass access
	 */
	public enum DriverParam{
		//Driver specific parameters
		/**
		 * Schema to use. Note: we cannot use the filter to derive the schema: the filter has no defined sequence.
		 */
		SCHEMA("schema","LastName,FirstName,Title,Email,WorkPhone,Fax,WirelessPhone,Description",DataType.STRING, RequiredConstraint.REQUIRED),
		/**
		 * Object class to use in the generated events.
		 */
		OBJECT_CLASS("objectClass","User",DataType.STRING,RequiredConstraint.REQUIRED),
		/**
		 * Ecmascript creating the association
		 */
		ASSOCIATION_FIELD("association-field-script",null,DataType.STRING),
		/**
		 * Include Ecmascript libraries
		 */
		INCLUDE_ECMA_LIBRARIES("include-ecma-libraries","false",DataType.BOOLEAN),
		/**
		 * Ecmascript for creating the src-dn
		 */
		SOURCE_FIELD("source-field-script",null,DataType.STRING),
		/**
		 * Handling of special characters for field names in the above Ecma scripts
		 */
		JS_PROCESS("js-processing",EcmascriptBuilder.JsProcessing.NOPROCESSING.toString(),DataType.STRING,RequiredConstraint.REQUIRED,EcmascriptBuilder.JsProcessing.values()),
		/**
		 * Replacement char for when {@link GenericFileDriverShim.DriverParam.JS_PROCESS} is configured as {@link EcmaUtil.JsProcessing.NOPROCESSING}
		 */
		JS_REPLACEMENT_CHAR("js-replacement-char","_",DataType.CHAR),
		//Authentication parameters => do we need them???
		//SERVER(DTD.TAG_SERVER,null,DataType.STRING),
		//USER(DTD.TAG_USER,null,DataType.STRING),
		//PASSWORD(DTD.TAG_PASSWORD,null,DataType.STRING)
		;

		private final String paramName;
		private final String defaultValue;
		private final List<Constraint> cons;
		private final DataType dataType;

		private DriverParam(final String paramName, final String defaultValue, final DataType type){
			this(paramName,defaultValue,type,null,null);
		}

		private DriverParam(final String paramName, final String defaultValue, final DataType type, final Constraint cons){
			this(paramName,defaultValue,type,cons,null);
		}

		private DriverParam(final String paramName, final String defaultValue, final DataType type, final Constraint cons, final Object[] enumConstraint){
			this.paramName = paramName;
			this.defaultValue = defaultValue;
			this.cons=new LinkedList<Constraint>();
			if (cons != null) {
				this.cons.add(cons);
			}
			if (enumConstraint != null){
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
	//put the tag names of the parameters here
	/*    private static final String TAG_SCHEMA = "schema";
    private static final String TAG_OBJECT_CLASS = "objectClass";
    private static final String TAG_ASSOCIATION_FIELD = "association-field-script";
    private static final String TAG_SOURCE_FIELD = "source-field-script";
    private static final String TAG_JS_PROCESS = "js-processing";
    private static final String TAG_JS_REPLACEMENT_CHAR = "js-replacement-char";

    //put the default values for the parameters here
    private static final String DEFAULT_SCHEMA = "LastName,FirstName,Title,Email,WorkPhone,Fax,WirelessPhone,Description";
    private static final String DEFAULT_OBJECT_CLASS = "User";
    private static final String DEFAULT_SERVER = null;
    private static final String DEFAULT_USER = null;
    private static final String DEFAULT_PASSWORD = null;
	private static final String DEFAULT_ASSOCIATION_FIELD = null;
	private static final String DEFAULT_SOURCE_FIELD = null;
	private static final String DEFAULT_JS_PROCESS = JsProcessing.NOPROCESSING.toString();
	private static final String DEFAULT_JS_REPLACEMENT_CHAR = "_";
	 */
	//MODIFY:  put the name of your application here
	private static final String APPLICATION_NAME = "Generic File Driver";

	/**
	 *  Object implementing
	 *  <code>com.novell.nds.dirxml.driver.SubscriptionShim</code>,
	 *  which is supplied to the DirXML engine via the
	 *  <code>getSubscriptionShim()</code> method.
	 *  <p>
	 *  @see #getSubscriptionShim()
	 */
	private FileDriverSubscriptionShim subscriptionShim = null;

	/**
	 *  Object implementing
	 *  <code>com.novell.nds.dirxml.driver.PublicationShim</code>,
	 *  which is supplied to the DirXML engine via the
	 *  <code>getPublicationShim()</code> method.
	 *  <p>
	 *  @see #getPublicationShim()
	 */
	private info.vancauwenberge.filedriver.shim.FileDriverPublicationShim publicationShim = null;

	/**
	 * The object class that this driver is supposed to handle.
	 */
	private String objectClass;

	private String associationField;

	private boolean includeEcmaLibraries = false;

	private String[] schema;


	private String sourceField;

	private EcmascriptBuilder.JsProcessing jsProcessingMode;
	private String jsIdentifierReplacement;

	/*
	 * All driver parameters as received during initialization
	 */
	private Map<String,Parameter> driverParams;

	private EcmascriptBuilder ecmaBuilder;

	/**
	 *  A Java driver shim <code>must</code> have a constructor which takes
	 *  no parameters.  The DirXML engine uses this to construct the driver
	 *  object.
	 */
	public GenericFileDriverShim()
	{
		//create temporary trace object until we know what the driver's
		//  RDN is
		setDriverRDN(TRACE_ID);
	}//SkeletonDriverShim()


	/**
	 *  A non-interface method that describes the parameters
	 *  this DriverShim is expecting.
	 *  <p>
	 *  @see GenericFileDriverShim#init(XmlDocument)
	 */
	@Override
	protected Map<String,Parameter> getParameterDefs()
	{
		//The XDS.jar library automatically checks parameter data
		//types for you.  When a RequiredConstraint is added to a parameter,
		//the library will check init documents to ensure the parameter is
		//present and has a value.  When you add RangeConstraints or
		//EnumConstraints to a parameter, the library will check parameter
		//values to see if they satisfy these constraints.
		trace.trace("getInitialDriverParams start",TraceLevel.TRACE);
		final DriverParam[] parameters = DriverParam.values();

		final HashMap<String,Parameter> driverParams = new HashMap<String,Parameter>(parameters.length);
		for (final DriverParam driverParam : parameters) {
			final Parameter param = new Parameter(driverParam.getParamName(), //tag name
					driverParam.getDefaultValue(), //default value
					driverParam.getDataType()); //data type
			final List<Constraint> cons = driverParam.getConstraints();
			for (final Constraint constraint : cons) {
				param.add(constraint);
			}
			driverParams.put(param.tagName(), param);			
		}
		trace.trace("getInitialDriverParams done",TraceLevel.TRACE);
		return driverParams;
	}//setDriverParams():void


	/**
	 *  <code>init</code> will be called after a driver is instantiated to allow
	 *  it to perform any necessary initialization before event processing begins.
	 *  Typically it will perform any setup that is common to both the subscriber
	 *  and publisher channels.
	 *  <p>
	 *  @param initXML XML document that contains the driver's
	 *      initialization parameters
	 *  @return an XML document containing status messages for this
	 *      operation
	 */
	@Override
	public XmlDocument init(final XmlDocument initXML)
	{
		//MODIFY:  initialize your driver here

		//example initialization document:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product version="1.1a">DirXML</product>
                <contact>Novell, Inc.</contact>
            </source>
            <input>
                <init-params src-dn="\NEW_DELL_TREE\NOVELL\Driver Set\Skeleton Driver (Java, XDS)">
                    <authentication-info>
                        <server>server.app:400</server>
                        <user>User1</user>
                    </authentication-info>
                    <driver-options>
                <option-1 display-name="Sample String option">This is a string</option-1>
                <option-2 display-name="Sample int option (enter an integer)">10</option-2>
                <option-3 display-name="Sample boolean option (enter a boolean value)">true</option-3>
                <option-4 display-name="Sample required option (enter some value)">not null</option-4>
            </driver-options>
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
                        <option-1 display-name="Sample String option">This is a string</option-1>
                        <option-2 display-name="Sample int option (enter an integer)">10</option-2>
                        <option-3 display-name="Sample boolean option (enter a boolean value)">true</option-3>
                        <option-4 display-name="Sample required option (enter some value)">not null</option-4>
                        <password display-name="password"><!-- content suppressed -->
                        </password>
                        <server display-name="server">server.app:400</server>
                        <user display-name="user">User1</user>
                    </parameters>
                </status>
            </output>
        </nds>
		 */

		trace.trace("init start", TraceLevel.TRACE);

		//create result document for reporting status to DirXML engine
		//can't add source info yet since we don't have the driver's RDN
		final XDSResultDocument result = new XDSResultDocument();
		StatusAttributes attrs;


		try
		{
			//parse initialization document
			final XDSInitDocument init = new XDSInitDocument(initXML);
			final XDSStatusElement status;


			setDriverRDN(init.rdn());
			setDriverSrcDN(init.srcDN());
			//create new trace object that uses driver's RDN
			setTrace(TRACE_SUFFIX);
			//append a <source> element now that we know the driver's RDN
			appendSourceInfo(result);

			//get driver parameters from the initialization doc
			//Field constraints are validated by XDS => XDSException
			driverParams = getParameterDefs();//getShimParameters();
			init.parameters(driverParams);

			//Get our data. Mandatory fields have been validated by XDS (RequiredConstraint.REQUIRED)
			objectClass = driverParams.get(DriverParam.OBJECT_CLASS.getParamName()).toString();
			Object param = driverParams.get(DriverParam.ASSOCIATION_FIELD.getParamName());
			if (param!= null) {
				associationField=param.toString();
			}

			param = driverParams.get(DriverParam.SOURCE_FIELD.getParamName());
			if (param!= null) {
				sourceField=param.toString();
			}
			schema = getSchemaAsArray(driverParams);

			//JS identifier processing
			param = driverParams.get(DriverParam.JS_PROCESS.getParamName());
			if (param!= null) {
				jsProcessingMode=EcmascriptBuilder.JsProcessing.valueOf(param.toString());
			}
			param = driverParams.get(DriverParam.JS_REPLACEMENT_CHAR.getParamName());
			if (param!= null) {
				jsIdentifierReplacement=param.toString();
			}

			param = driverParams.get(DriverParam.INCLUDE_ECMA_LIBRARIES.getParamName());
			if (param != null) {
				includeEcmaLibraries = Boolean.parseBoolean(param.toString());
			}

			//get the filter
			final DriverFilter driverFilter = init.driverFilter();
			trace.trace("Filter:"+driverFilter,TraceLevel.TRACE);
			if (driverFilter!=null){
				final ClassFilter classFilter = driverFilter.getClassFilter(objectClass);
				trace.trace("ClassFilter:"+classFilter,TraceLevel.TRACE);
				if (classFilter!= null) {
					trace.trace("ClassFilter:"+classFilter.toString(),TraceLevel.TRACE);
				}
			}


			//create a publisher and subscriber instance
			subscriptionShim = new FileDriverSubscriptionShim(this);
			publicationShim = new FileDriverPublicationShim(this);

			//perform any other initialization that might be required

			//append a successful <status> element to the result doc
			attrs = StatusAttributes.factory(StatusLevel.SUCCESS,
					StatusType.DRIVER_STATUS,
					null); //event-id
			status = XDSUtil.appendStatus(result, //doc to append to
					attrs, //status attribute values
					null); //description
			//append the parameter values the driver is actually using
			status.parametersAppend(driverParams);
		}//try
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

		//return result doc w/ status to DirXML engine
		trace.trace("init done", TraceLevel.TRACE);
		return result.toXML();
	}//init(XmlDocument):XmlDocument


	/**
	 *  <code>shutdown</code> indicates to the DriverShim that
	 *  the driver is being terminated.
	 *  <p>
	 *  @param reasonXML unused
	 *  @return an XML document containing status messages for this
	 *      operation
	 */
	@Override
	public XmlDocument shutdown(final XmlDocument reasonXML)
	{
		//MODIFY:  put your shutdown code here

		//example result document:

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

		trace.trace("shutdown start", TraceLevel.TRACE);

		final XDSResultDocument result = newResultDoc();
		StatusAttributes attrs;

		try
		{
			//shutdown whatever needs shutting down
			//  which includes telling the publication shim to
			//  return control back to the DirXML engine and
			//  telling the subscription shim to free resources

			if (publicationShim != null)
			{
				publicationShim.shutdown(result);
			}

			if (subscriptionShim != null)
			{
				subscriptionShim.shutdown(result);
			}

			//TODO: test to see if we already have a status element (eg: an error from the shutdown somewhere)

			//append a successful <status> element to the result doc
			attrs = StatusAttributes.factory(StatusLevel.SUCCESS,
					StatusType.DRIVER_STATUS,
					null); //event-id
			XDSUtil.appendStatus(result, //doc to append to
					attrs, //status attribute values
					null); //description
		}//try
		catch (final Exception e) //don't want to catch Error class with Throwable
		{
			//something bad happened...
			attrs = StatusAttributes.factory(StatusLevel.FATAL,
					StatusType.DRIVER_STATUS,
					null); //event-id
			XDSUtil.appendStatus(result, //do to append to
					attrs, //status attribute values
					null, //description
					e, //exception
					true, //append stack trace?
					null); //xml to append
		}//catch

		//return the result doc w/ status to DirXML engine
		trace.trace("shutdown done", TraceLevel.TRACE);
		return result.toXML();
	}//shutdown(XmlDocument):XmlDocument


	/**
	 *  <code>getSubscriptionShim</code> gets the implementation of
	 *  SubscriptionShim that will be used to process commands on behalf of
	 *  the DirXML engine.
	 *  <p>
	 *  NOTE: the returned instance will be initialized by the DirXML engine
	 *  <p>
	 *  @see FileDriverSubscriptionShim#init(XmlDocument)
	 *  @return an instance of  SubscriptionShim that will be used to process
	 *      commands on behalf of the DirXML engine
	 */
	@Override
	public SubscriptionShim getSubscriptionShim()
	{
		trace.trace("getSubscriptionShim", TraceLevel.TRACE);

		return subscriptionShim;
	}//getSubscriptionShim():SubscriptionShim


	/**
	 *  <code>getPublicationShim</code> gets the implementation of
	 *  PublicationShim that will be invoked by the DirXML engine.
	 *  <p>
	 *  NOTE: the returned instance will be initialized by the DirXML engine
	 *  <p>
	 *  @see FileDriverPublicationShimImpl#init(XmlDocument)
	 *  @return an instance of PublicationShim that will be invoked by
	 *  the DirXML engine
	 */
	@Override
	public PublicationShim getPublicationShim()
	{
		trace.trace("getPublicationShim", TraceLevel.TRACE);

		return publicationShim;
	}//getPublicationShim():PublicationShim

	@Override
	public ISubscriberShim getSubscriber(){
		return subscriptionShim;
	}

	/**
	 *  <code>getSchema</code> returns the application schema encoded
	 *  in XDS XML.
	 *  <p>
	 *  This will be called only when the driver is not running. In other
	 *  words, if this method is called init()/shutdown() will not be called
	 *  for the current instance of the DriverShim.
	 *  <p>
	 *  @param initXML XML document containing the driver shim
	 *      initialization parameters as well as the subscription shim and
	 *      publication shim initialization parameters.
	 *  @return XML document containing the application schema or an
	 *      error status
	 */
	@Override
	public XmlDocument getSchema(final XmlDocument initXML)
	{
		//MODIFY:  get your application schema here

		//example initialization document:

		/*
        <nds dtdversion="1.1" ndsversion="8.6">
            <source>
                <product version="1.1a">DirXML</product>
                <contact>Novell, Inc.</contact>
            </source>
            <input>
                <init-params src-dn="\NEW_DELL_TREE\NOVELL\Driver Set\Skeleton Driver (Java, XDS)">
                    <authentication-info>
                        <server>server.app:400</server>
                        <user>User1</user>
                    </authentication-info>
                    <driver-options>
                <option-1 display-name="Sample String option">This is a string</option-1>
                <option-2 display-name="Sample int option (enter an integer)">10</option-2>
                <option-3 display-name="Sample boolean option (enter a boolean value)">true</option-3>
                <option-4 display-name="Sample required option (enter some value)">not null</option-4>
            </driver-options>
                    <subscriber-options>
                <sub-1 display-name="Sample Subscriber option">String for Subscriber</sub-1>
            </subscriber-options>
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
                <schema-def application-name="Skeleton Application" hierarchical="true">
                    <class-def class-name="fake-class-1" container="true">
                        <attr-def attr-name="fake-attr-1" case-sensitive="true" multi-valued="true" naming="true" read-only="true" required="true" type="string"/>
                    </class-def>
                    <class-def class-name="fake-class-2" container="false">
                        <attr-def attr-name="fake-attr-2" case-sensitive="false" multi-valued="false" naming="false" read-only="false" required="false" type="int"/>
                    </class-def>
                </schema-def>
                <status level="warning" type="driver-general">
                    <description>Get schema not implemented.</description>
                </status>
            </output>
        </nds>
		 */
		trace.trace("getSchema start", TraceLevel.TRACE);

		//create result document for reporting status to DirXML engine
		//can't add source info yet since we don't have the driver's instance name
		final XDSSchemaResultDocument result = new XDSSchemaResultDocument();
		StatusAttributes attrs;

		try
		{
			final XmlDocument initResultDoc = init(initXML);
			final StatusLevel level = Util.getFirstStatus(initResultDoc);

			if (level.compareTo(StatusLevel.WARNING) < 0){
				//Oops, some error occured. Return the error.
				result.empty();
				attrs = StatusAttributes.factory(level,
						StatusType.DRIVER_STATUS,
						null); //event-id

				//Search for the description
				final String descr = Util.getFirstStatusDescription(initXML);
				XDSUtil.appendStatus(result, //doc to append to
						attrs, //status attribute values
						descr, //description
						null, //exception
						false, //append stack trace?
						initXML); //xml to append
			}
			else //Init was succesfull
			{
				/*
       			 XDSInitDocument init;

				//parse initialization document
				 init = new XDSInitDocument(initXML);

				//append a <source> element to the result document
				 setDriverRDN(init.rdn());
				 setTrace(TRACE_SUFFIX);
				 */
				appendSourceInfo(result);

				//get driver parameters from the initialization doc
				//init.parameters(driverParams);

				//MODIFY:  put your application's schema in the result document

				XDSSchemaDefElement schemaDef; //the <schema-def> element from the result doc
				XDSClassDefElement userClassDef; //a <class-def> element from the result doc
				XDSAttrDefElement attrDef; //an <attr-def> element from the result doc

				//a driver that supports a real application would do whatever is
				//necessary to create an XDS schema description document and
				//return it; for example, it might create an instance of the
				//Subscriber object and query the application schema; if the
				//application has an invariant schema, the driver might just
				//have an embedded XML document describing the schema in XDS
				//terms; this embedded document could then be returned to DirXML
				//wrapped by an XmlDocument object
				//
				//however, since we are just a skeleton, return a fake schema with
				//a warning that this method isn't really implemented

				schemaDef = result.appendSchemaDefElement();
				schemaDef.setApplicationName(APPLICATION_NAME);
				schemaDef.setHierarchical(true);
				userClassDef = schemaDef.appendClassDefElement();
				userClassDef.setClassName(objectClass);
				userClassDef.setContainer(true);
				//for (int i = 0; i < schema.length; i++) {
				for (final String aField : schema) {
					attrDef = userClassDef.appendAttrDefElement();
					attrDef.setAttrName(aField);
					attrDef.setMultiValued(false);
				}
				//Let the publisher extend the schema if needed
				publicationShim.extendSchema(userClassDef,initXML);
				attrs = StatusAttributes.factory(StatusLevel.SUCCESS,
						StatusType.DRIVER_GENERAL,
						null); //event-id
				XDSUtil.appendStatus(result, //doc to append to
						attrs,  //status attribute values
						null);  //description
			}
		}//try
		catch (final Exception e) //don't want to catch Error class with Throwable
		{
			Util.printStackTrace(trace, e);
			//e instance of XDSException:
			//
			//  init document is malformed or invalid -- or --
			//  it is missing required parameters or contains
			//  illegal parameter values

			//e instance of RuntimeException:

			//  e.g., NullPointerException

			//clean out partial schema
			result.empty();
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

		trace.trace("getSchema done", TraceLevel.TRACE);
		//return result doc w/ schema and status to DirXML engine
		return result.toXML();
	}//getSchema(XmlDocument):XmlDocument

	/**
	 * Upon starting of the publisher, do whatever additional initialization that is required.
	 * @param processor
	 * @throws XDSParseException
	 */
	public void onStart(final XmlCommandProcessor processor){
		//Get the linked ecma script
		try{
			final Map<Integer, String> jsLibraries;
			if (includeEcmaLibraries){
				jsLibraries = getLinkedEcmaLibraries(processor);
			}else{
				jsLibraries = Collections.emptyMap();
			}
			this.ecmaBuilder = createEcmaBuilder(processor, jsLibraries);
		}catch (final Exception e) {
			//This should not happen, but you never know.
			//IF it happens, we do not load the libraries
			Util.printStackTrace(trace, e);
			trace.trace("Driver's ecmascript libraries not loaded.", 0);
			try {
				this.ecmaBuilder = createEcmaBuilder(processor,null);
			} catch (final XDSParseException e1) {
			}
		}
	}


	/**
	 * Create an ecma builder by loading the given ecma libraries into it's scope.
	 * @param processor
	 * @param jsLibraries
	 * @return
	 * @throws XDSParseException
	 */
	@SuppressWarnings("unchecked")
	private EcmascriptBuilder createEcmaBuilder(final XmlCommandProcessor processor,
			final Map<Integer, String> jsLibraries) throws XDSParseException {

		final EcmascriptBuilder ecmaBuilder = new EcmascriptBuilder(jsProcessingMode, jsIdentifierReplacement);

		if ((jsLibraries == null) || jsLibraries.isEmpty()) {
			return ecmaBuilder;
		}

		final Set<Integer> keys = jsLibraries.keySet();

		final List<Integer> list = new ArrayList<Integer>(keys);
		java.util.Collections.sort(list);

		for (final Integer index : list) {
			//Get the content of the library
			final ArrayList<String> readAttrDetails = new ArrayList<String>();
			readAttrDetails.add("DirXML-Data");
			final XDSQueryDocument queryDetail = Util.createQueryDoc(null, jsLibraries.get(index), null, QueryScope.ENTRY, readAttrDetails, null);
			final XmlDocument responseDetail = processor.execute(queryDetail.toXML(), null);
			final XDSQueryResultDocument resultDetail = new XDSQueryResultDocument(responseDetail);
			final List<XDSInstanceElement> instancesDetail =resultDetail.extractInstanceElements();
			if ((instancesDetail != null) && (instancesDetail.size()==1)){
				final XDSInstanceElement singleton = instancesDetail.get(0);
				final List<XDSAttrElement> attrs = singleton.extractAttrElements();
				if (attrs != null){
					for (final XDSAttrElement xdsAttrElement : attrs) {
						if ("DirXML-Data".equals(xdsAttrElement.getAttrName())){
							final List<XDSValueElement> values = xdsAttrElement.extractValueElements();
							for (final XDSValueElement xdsValueElement : values) {
								byte[] decoded;
								try {
									decoded = Base64Codec.decode(xdsValueElement.extractText());
									ecmaBuilder.addSharedLibrary(new String(decoded,"UTF-8"), jsLibraries.get(index));									
								} catch (final IOException e) {
									//This should not happen, so we just log in case it does.
									trace.trace("Failed to decode the given octet string. The ECMAscript library '"+jsLibraries.get(index)+"'will not be included.",TraceLevel.ERROR_WARN);
								}

							}
						}
					}
				}
			}
		}


		return ecmaBuilder;
	}


	/**
	 * Create a map with the ECMA library references.
	 * Key = index (the list of libraries is ordered)
	 * Value = the DN to the library
	 * @param processor
	 * @return
	 * @throws XDSParseException
	 */
	@SuppressWarnings("unchecked")
	private Map<Integer, String> getLinkedEcmaLibraries(
			final XmlCommandProcessor processor) throws XDSParseException {
		final List<String> readAttr = new ArrayList<String>();
		readAttr.add("DirXML-Policies");
		final XDSQueryDocument query = Util.createQueryDoc("DirXML-Driver", getDriverSrcDN(), null, QueryScope.ENTRY, readAttr, null);
		final XmlDocument response = processor.execute(query.toXML(), null);
		final XDSQueryResultDocument result = new XDSQueryResultDocument(response);
		final List<XDSInstanceElement> instances =result.extractInstanceElements();
		//We should have exactly 1 instance
		final Map<Integer, String> jsLibraries = new HashMap<Integer, String>();
		if ((instances != null) && (instances.size()==1)){
			final XDSInstanceElement singleton = instances.get(0);
			final List<XDSAttrElement> attrs = singleton.extractAttrElements();
			if (attrs != null) {
				for (final XDSAttrElement xdsAttrElement : attrs) {
					if ("DirXML-Policies".equals(xdsAttrElement.getAttrName())){
						final List<XDSValueElement> values = xdsAttrElement.extractValueElements();
						for (final XDSValueElement xdsValueElement : values) {
							final List<XDSComponentElement> components = xdsValueElement.extractComponentElements();
							String dn=null;
							String index=null;
							String path=null;
							//Policies have 3 components: dn,level and interval
							for (final XDSComponentElement xdsComponentElement : components) {
								if(xdsComponentElement.getName().equals("level")) {
									index=xdsComponentElement.extractText();
								} else if(xdsComponentElement.getName().equals("dn")) {
									dn=xdsComponentElement.extractText();
								} else if(xdsComponentElement.getName().equals("interval")) {
									path=xdsComponentElement.extractText();
								}
							}
							if ("3".equals(path)){//"3" indicates an ecmescript library
								trace.trace("Found ECMAscript library:"+dn,TraceLevel.DEBUG);
								jsLibraries.put(Integer.parseInt(index), dn);
							}else{
								trace.trace("Non ECMAscript policy:"+dn,TraceLevel.DEBUG);								
							}
						}
					}
				}
			}
		}
		//If we found any libraries: get the content and process them
		return jsLibraries;
	}


	@Override
	public String getAssociationField(final Map<String,String> thisRecord) {
		if ((associationField != null) && !associationField.equals("")){
			trace.trace("Calculating association value using ecmascript '"+associationField+"'",TraceLevel.TRACE);
			try{
				//final Object result = EcmaUtil.evaluateEcmaWithParams(trace, jsProcessingMode, jsIdentifierReplacement, thisRecord,associationField,DriverParam.ASSOCIATION_FIELD.getParamName());
				final Object result = ecmaBuilder.evaluateEcmaWithParams(trace, thisRecord,associationField,DriverParam.ASSOCIATION_FIELD.getParamName());
				trace.trace("Calculated association: "+ result,TraceLevel.TRACE);
				String assValue = Context.toString(result);
				if (assValue != null) {
					assValue = assValue.trim();
				}
				if ("".equals(assValue)) {
					assValue=null;
				}
				return assValue;
			}catch(final Exception e){
				final StringBuilder error = new StringBuilder("Error while calculating the association using ecmascript :'");
				error.append(associationField).append("'. ").append(e.getClass().getName()).append(":").append(e.getMessage()).append(".").append("Record read:").append(thisRecord);
				final String errorStr = error.toString();
				trace.trace(errorStr, TraceLevel.ERROR_WARN);
				throw new IllegalArgumentException(errorStr, e);
			}
		}
		else{
			trace.trace("No association script set.",TraceLevel.TRACE);
			return null;
		}
	}


	@Override
	public String getSourceField(final Map<String,String> thisRecord) {
		if ((sourceField != null) && !sourceField.equals("")){
			trace.trace("Calculating src-dn value using ecmascript '"+sourceField+"'",TraceLevel.TRACE);
			try{
				//final Object result = EcmaUtil.evaluateEcmaWithParams(trace, jsProcessingMode, jsIdentifierReplacement, thisRecord,sourceField,DriverParam.SOURCE_FIELD.getParamName());
				final Object result = ecmaBuilder.evaluateEcmaWithParams(trace, thisRecord,sourceField,DriverParam.SOURCE_FIELD.getParamName());
				trace.trace("Calculated src-dn: "+ result,TraceLevel.TRACE);
				String srcValue = Context.toString(result);
				if (srcValue != null){
					srcValue = srcValue.trim().replaceAll("[+=,\\.\\\\]", "-");
					srcValue=srcValue.trim();
				}
				if ("".equals(srcValue)) {
					srcValue=null;
				}
				return srcValue;
			}catch(final Exception e){
				final StringBuilder error = new StringBuilder("Error while calculating the src-dn value using ecmascript :'");
				error.append(sourceField).append("'. ").append(e.getClass().getName()).append(":").append(e.getMessage()).append(".").append("Record read:").append(thisRecord);
				final String errorStr = error.toString();
				trace.trace(errorStr, TraceLevel.ERROR_WARN);
				throw new IllegalArgumentException(errorStr, e);
			}
		}
		else{
			trace.trace("No src-dn script set.",TraceLevel.TRACE);
			return null;
		}
	}


	@Override
	public String[] getSchema() {
		return schema;
	}


	public String getObjectClass() {
		return objectClass;
	}


	@Override
	public Map<String,Parameter> getDriverParams() {
		return driverParams;
	}

	/*
	 * Parse the schema out of the parametermap as an array of strings
	 */
	public static String[] getSchemaAsArray(final Map<String,Parameter> parammap){
		final String tempFieldNames = parammap.get(DriverParam.SCHEMA.getParamName()).toString();
		return tempFieldNames.split(","); //We always have at least 1 element (RequiredConstraint.REQUIRED)
	}

	/**
	 * Get a preconfigured ECMA script evaluator
	 */
	@Override
	public EcmascriptBuilder getEcmaBuilder() {
		return ecmaBuilder;
	}

}//class SeletonDriverShim