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


import info.vancauwenberge.filedriver.shim.AbstractShim;
import com.novell.nds.dirxml.driver.xds.*;


/**
 *  Common implementation for the Skeleton Driver.
 *  <p>
 *  This class contains common methods and fields used in DriverShims.
 *  
 */
public abstract class AbstractDriverShim extends AbstractShim
{

    //these constants are used to construct a <source> element in
    //  documents sent to the DirXML engine:


    //MODIFY:  put a temporary driver identifier here
    /**
     *  A temporary trace identifier for this driver.
     *  <p>
     *  Temporarily identifies this driver in trace messages
     *  until the driver's RDN is known.
     */
    static protected final String TRACE_ID = "Generic File Driver";



    /** This driver's Relative Distinguished Name (RDN). */
    protected String driverRDN = null;



	private String driverSrcDN;

    /**
     *  Connection state.
     *  <p>
     *  @see SkeletonSubscriptionShim#execute(com.novell.nds.dirxml.driver.XmlDocument, com.novell.nds.dirxml.driver.XmlQueryProcessor)
     *  @see SkeletonPublicationShim#start(com.novell.nds.dirxml.driver.XmlCommandProcessor)
     *  @see #connect()
     */
    //protected boolean connected = false;


    /**
     *  Constructor.
     */
    protected AbstractDriverShim()
    {
    }//CommonImpl()


	/**
     *  Sets this driver's name (the CN).
     *  <p>
     *  @param name the Relative Distinguished Name (RDN) of this
     *  driver instance; may be <code>null</code>
     */
    protected void setDriverRDN(String name)
    {
        driverRDN = name;
    }//setDriverRDN(String):void

	/**
     *  Sets this driver's DN.
     *  <p>
     *  @param name the Relative Distinguished Name (RDN) of this
     *  driver instance; may be <code>null</code>
     */
    protected void setDriverSrcDN(String name)
    {
        this.driverSrcDN = name;
    }//setDriverRDN(String):void

    /**
     *  Gets this driver's name.
     *  <p>
     *  @return will not return <code>null</code>
     */
    public String getDriverInstanceName()
    {
        return (driverRDN == null) ? TRACE_ID : driverRDN;
    }//getDriverRDN():String

    /**
     *  Gets this driver's name.
     *  <p>
     *  @return will not return <code>null</code>
     */
    public String getDriverSrcDN()
    {
        return (driverSrcDN == null) ? TRACE_ID : driverSrcDN;
    }//getDriverRDN():String


    /**
     *  Utility method for instantiating an <code>XDSResultDocument</code>.
     *  <p>
     *  The returned result document contains a populated
     *  <code>&lt;source&gt;</code> element.
     *  <p>
     *  @return will not return <code>null</code>
     */
/*    public XDSResultDocument newResultDoc()
    {
        XDSResultDocument resultDoc;

        resultDoc = new XDSResultDocument();
        appendSourceInfo(resultDoc);

        return resultDoc;
    }//newResultDoc():XDSResultDocument
*/

    /**
     *  Utility method for instantiating an <code>XDSCommandDocument</code>.
     *  <p>
     *  The returned result document contains a populated source element.
     *  <p>
     *  @return will not return <code>null</code>
     */
/*    public XDSCommandDocument newCommandDoc()
    {
        XDSCommandDocument commandDoc;

        commandDoc = new XDSCommandDocument();
        appendSourceInfo(commandDoc);

        return commandDoc;
    }//newCommandDoc():XDSCommandDocument
*/
}//class CommonImpl