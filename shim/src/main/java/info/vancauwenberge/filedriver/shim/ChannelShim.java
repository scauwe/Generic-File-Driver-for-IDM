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
package info.vancauwenberge.filedriver.shim;


import com.novell.nds.dirxml.driver.xds.*;

import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;


/**
 *  Common implementation for the Skeleton Driver.
 *  <p>
 *  This class contains common methods and fields used in the DriverShim,
 *  PublicationShim, and SubscriptionShim implementations of the Skeleton
 *  Driver.
 */
public abstract class ChannelShim extends AbstractShim
{

    //these constants are used to construct a <source> element in
    //  documents sent to the DirXML engine:


    protected GenericFileDriverShim driver;

    /**
     *  Constructor.
     */
    protected ChannelShim(GenericFileDriverShim someDriver, String traceSuffix)
    {
        driver = someDriver;
        setTrace(traceSuffix);
    }//CommonImpl()

    /*
     * Shut down this channel
     */
    public abstract void shutdown(XDSResultDocument result);

    /**
     *  Gets this driver's name.
     *  <p>
     *  @return will not return <code>null</code>
     */
    protected String getDriverInstanceName()
    {
        return (driver==null)?"null":driver.getDriverInstanceName();
    }//getDriverRDN():String


}//class CommonImpl