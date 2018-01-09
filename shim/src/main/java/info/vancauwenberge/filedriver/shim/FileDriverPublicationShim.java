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
package info.vancauwenberge.filedriver.shim;

import java.util.HashMap;
import java.util.Map;

import com.novell.nds.dirxml.driver.PublicationShim;
import com.novell.nds.dirxml.driver.XmlCommandProcessor;
import com.novell.nds.dirxml.driver.XmlDocument;
import com.novell.nds.dirxml.driver.XmlQueryProcessor;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.RangeConstraint;
import com.novell.nds.dirxml.driver.xds.RequiredConstraint;
import com.novell.nds.dirxml.driver.xds.StatusLevel;
import com.novell.nds.dirxml.driver.xds.StatusType;
import com.novell.nds.dirxml.driver.xds.XDSClassDefElement;
import com.novell.nds.dirxml.driver.xds.XDSInitDocument;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;
import com.novell.nds.dirxml.driver.xds.XDSParseException;
import com.novell.nds.dirxml.driver.xds.XDSResultDocument;
import com.novell.nds.dirxml.driver.xds.util.StatusAttributes;
import com.novell.nds.dirxml.driver.xds.util.XDSUtil;

import info.vancauwenberge.filedriver.filepublisher.FileDriverPublicationShimImpl;
import info.vancauwenberge.filedriver.filepublisher.IPublisherImplStrategy;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;
import info.vancauwenberge.filedriver.util.Util;

public class FileDriverPublicationShim  extends ChannelShim implements PublicationShim, XmlQueryProcessor{

	static public final String DEFAULT_PUB_HEARTBEAT_INTERVAL = "0"; //minutes (disabled)

	private static final String PUB_IMPL_CLASS = "pub_impl_class";
	private static final String DEFAULT_IMPL_CLASS = FileDriverPublicationShimImpl.class.getName();
	static final private String TRACE_SUFFIX = "PT";


	private IPublisherImplStrategy  publicationShimImpl;

	public FileDriverPublicationShim(final GenericFileDriverShim someDriver) {
		super(someDriver, TRACE_SUFFIX);
	}

	@Override
	public XmlDocument query(final XmlDocument arg0) {
		return publicationShimImpl.query(arg0);
	}

	@Override
	public XmlDocument init(final XmlDocument initXML) {
		trace.trace("init", 1);
		//trace.trace("inputDoc"+initXML.getDocumentString(),1);
		XDSResultDocument result;
		StatusAttributes attrs;

		//create result document for reporting status to the DirXML engine
		result = newResultDoc();

		try
		{

			//parse initialization document
			final XDSInitDocument init = new XDSInitDocument(initXML);

			//get any publisher options from init doc
			final Map<String,Parameter> pubParams = getParameterDefs();
			//Set and validate the parameters
			init.parameters(pubParams);

			//get the implementation class
			final String implClass = pubParams.get(PUB_IMPL_CLASS).toString();
			trace.trace("Creating publisher of class:"+implClass);
			publicationShimImpl = (IPublisherImplStrategy) Class.forName(implClass).newInstance();
			//Note: XDS limitation: you cannot evaluate twice the same parameter object (eg: the default SingleValueConstraint will fail) 
			//due to a limitation of the XDS implementation
			//For this reason, we create a new parameterDefs
			return publicationShimImpl.init(trace, getParameterDefs(), driver, initXML);

		}//try
		catch (final ClassCastException e) //don't want to catch Error class with Throwable
		{
			Util.printStackTrace(trace,e);
			attrs = StatusAttributes.factory(StatusLevel.FATAL,
					StatusType.DRIVER_STATUS,
					null); //event-id
			XDSUtil.appendStatus(result, //doc to append to
					attrs, //status attribute values
					"One or more strategies specified in the publisher channel are not implementing the correct interface.", //description
					e, //exception
					XDSUtil.appendStackTrace(e), //append stack trace?
					initXML); //xml to append
		}
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

		return result.toXML();

	}

	@Override
	public XmlDocument start(final XmlCommandProcessor arg0) {
		driver.onStart(arg0);
		return publicationShimImpl.start(arg0);
	}

	/**
	 * 
	 * Extend the schema (User class) with the meta data requested
	 */
	public void extendSchema(final XDSClassDefElement userClassDef,
			final XmlDocument initXML) throws XDSParseException,
	XDSParameterException {
		//This is called when init/shutdown is not called.
		if (publicationShimImpl == null){
			init(initXML);
		}
		if (publicationShimImpl != null) {
			publicationShimImpl.extendSchema(userClassDef, initXML);
		}
	}

	@Override
	public void shutdown(final XDSResultDocument result) {
		//Test for null. Failure to start the driver might leave us with a null implementation
		if (publicationShimImpl != null) {
			publicationShimImpl.shutdown(result);
		}
	}


	@Override
	protected Map<String, Parameter> getParameterDefs() {
		final HashMap<String, Parameter> map = new HashMap<String, Parameter>();
		//Add our own and the common parameters:


		//heartbeat
		Parameter param = new Parameter(IPublisherImplStrategy.PUB_HEARTBEAT_INTERVAL,
				DEFAULT_PUB_HEARTBEAT_INTERVAL,
				DataType.INT);
		param.add(RangeConstraint.NON_NEGATIVE); //ensures >= 0
		map.put(IPublisherImplStrategy.PUB_HEARTBEAT_INTERVAL, param);


		//the actual implementation class
		param = new Parameter(PUB_IMPL_CLASS,
				DEFAULT_IMPL_CLASS,
				DataType.STRING);
		param.add(RequiredConstraint.REQUIRED);

		map.put(param.tagName(), param);

		return map;
	}


}
