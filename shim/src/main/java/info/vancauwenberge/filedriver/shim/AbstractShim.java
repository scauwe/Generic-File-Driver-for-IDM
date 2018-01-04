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

import info.vancauwenberge.filedriver.Build;

import java.lang.reflect.Method;
import java.util.Map;


import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.WriteableDocument;
import com.novell.nds.dirxml.driver.xds.XDSCommandDocument;
import com.novell.nds.dirxml.driver.xds.XDSProductElement;
import com.novell.nds.dirxml.driver.xds.XDSQueryDocument;
import com.novell.nds.dirxml.driver.xds.XDSResultDocument;
import com.novell.nds.dirxml.driver.xds.XDSSourceElement;

public abstract class AbstractShim {
	
    /**
     *  Container for parameters passed to
     *  <code>init(XmlDocument)</code>.
     *  <p>
     *  Contains parameters for the driver (or Channel) shim.
     *  <p>
     *  @see GenericFileDriverShim#init(com.novell.nds.dirxml.driver.XmlDocument)
     *  @see GenericFileDriverShim#setDriverParams()
     */
    //private Map<String,Parameter> shimParameters = null;
	
    /**
     *  Used by derived classes to output trace messages to DSTrace
     *  or a Java trace file.
     *  <p>
     *  To cause trace messages to appear on the DSTrace screen set
     *  <code>the DirXML-DriverTraceLevel</code> attribute on the driver
     *  set object to a value greater than zero. To log trace messages
     *  to a file, set the <code>DirXML-JavaTraceFile</code> attribute
     *  on the driver set object to a file path.
     */
    protected Trace trace;


    /**
     *  Creates a trace object.
     *  <p>
     *  @param suffix the suffix of the trace message to follow this driver's
     *  RDN; may be <code>null</code>
     */
    protected void setTrace(String suffix)
    {
        String header;

        header = (suffix == null) ? getDriverInstanceName() : (getDriverInstanceName() + "\\" + suffix);
        trace = new Trace(header);
        if (header != null){
        	try{
        		//We need 2 classes in order for MDC to work
        		Class.forName("org.slf4j.impl.StaticMDCBinder");
        		Class<?> c = Class.forName("org.slf4j.MDC");
        		Method m =c.getMethod("put", String.class, String.class);
        		m.invoke(null, "Trace", header);
        		if (trace != null)
        			trace.trace("SLF4J MDC set to:"+header,3);
        	}catch (Exception e) {
        		if (trace != null)
        			trace.trace("SLF4J not found. MDC not configured.",3);
        	}
        }
    }//setTrace(String):void
    
	protected abstract String getDriverInstanceName();
	
    /**
     *  Appends a populated <code>&lt;source&gt;</code> element to
     *  <code>doc</code>.
     *  <p>
     *  @param doc may be <code>null</code>
     */
    public void appendSourceInfo(WriteableDocument doc)
    {
        if (doc != null)
        {
            XDSSourceElement source;
            XDSProductElement product;

            source = doc.appendSourceElement();
            product = source.appendProductElement();

            //TODO: MODIFY:  put the time and date your driver was built here
            product.setBuild(Build.BUILD_NO);
            product.setInstance(getDriverInstanceName());

            product.setVersion(Build.PRODUCT_VERSION);
            product.appendText(Build.PRODUCT_NAME);
            source.appendContactElement(Build.COMPANY_NAME);
        }//if
    }//appendSourceInfo(WriteableDocument):void


    /**
	 * initialize the driver parameters
	 */
	protected abstract Map<String,Parameter> getParameterDefs();


    
	/**
	 *  Utility method for instantiating an <code>XDSCommandDocument</code>.
	 *  <p>
	 *  The returned result document contains a populated source element.
	 *  <p>
	 *  @return will not return <code>null</code>
	 */
	public XDSCommandDocument newCommandDoc()
	{
	    XDSCommandDocument commandDoc;
	
	    commandDoc = new XDSCommandDocument();
	    appendSourceInfo(commandDoc);
	
	    return commandDoc;
	}//newCommandDoc():XDSCommandDocument

	/**
	 *  Utility method for instantiating an <code>XDSQueryDocument</code>.
	 *  <p>
	 *  The returned result document contains a populated source element.
	 *  <p>
	 *  @return will not return <code>null</code>
	 */
	public XDSQueryDocument newQueryDoc()
	{
		XDSQueryDocument commandDoc;
	
	    commandDoc = new XDSQueryDocument();
	    appendSourceInfo(commandDoc);
	
	    return commandDoc;
	}//newCommandDoc():XDSCommandDocument

	/**
	 *  Utility method for instantiating an <code>XDSResultDocument</code>.
	 *  <p>
	 *  The returned result document contains a populated
	 *  <code>&lt;source&gt;</code> element.
	 *  <p>
	 *  @return will not return <code>null</code>
	 */
	public XDSResultDocument newResultDoc()
	{
	    XDSResultDocument resultDoc;
	
	    resultDoc = new XDSResultDocument();
	    appendSourceInfo(resultDoc);
	
	    return resultDoc;
	}//newResultDoc():XDSResultDocument

	/**
	 * 
	 */
	protected AbstractShim() {
        setTrace(null); //no channel yet
		//shimParameters = getInitialDriverParams();
	}
	
    
	/**
	 * @return Returns the shimParameters.
	 */
	/*
	protected Map<String,Parameter> getShimParameters() {
		return shimParameters;
	}*/
}
