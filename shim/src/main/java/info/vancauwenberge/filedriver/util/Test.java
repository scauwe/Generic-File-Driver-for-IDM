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
package info.vancauwenberge.filedriver.util;

import info.vancauwenberge.filedriver.Build;
/*
import java.util.HashMap;

import com.novell.nds.dirxml.driver.XmlDocument;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.RangeConstraint;
import com.novell.nds.dirxml.driver.xds.RequiredConstraint;
import com.novell.nds.dirxml.driver.xds.XDSInitDocument;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;
import com.novell.nds.dirxml.driver.xds.XDSParseException;
*/
public class Test {
	static final String initDocStr = "<nds dtdversion=\"4.0\" ndsversion=\"8.x\">"
			+"  <source>"
			+"    <product edition=\"Standard\" version=\"4.5.3.0\">DirXML</product>"
			+"    <contact>NetIQ Corporation</contact>"
			+"  </source>"
			+"  <input>"
			+"    <init-params src-dn=\"\\IDVDS\\system\\driverset1\\DrvProfilesCSV\">"
			+"      <driver-filter>"
			+"        <allow-class class-name=\"User\">"
			+"          <allow-attr attr-name=\"action\"/>"
			+"          <allow-attr attr-name=\"emailAddress\"/>"
			+"          <allow-attr attr-name=\"managerEmail\"/>"
			+"          <allow-attr attr-name=\"managerUid\"/>"
			+"          <allow-attr attr-name=\"MobileNumber\"/>"
			+"          <allow-attr attr-name=\"telephoneNumber\"/>"
			+"          <allow-attr attr-name=\"isManager\"/>"
			+"        </allow-class>"
			+"      </driver-filter>"
			+"      <subscriber-options>"
			+"        <sub_FileStartStrategy display-name=\"File Start Strategy:\">info.vancauwenberge.filedriver.filestart.BasicNewFileDecider</sub_FileStartStrategy>"
			+"        <newFile_MaxRecords display-name=\"Maximum number of data records in a file:\">100</newFile_MaxRecords>"
			+"        <newFile_MaxFileAge display-name=\"Maximum 'age' of a file (in seconds):\">2147483</newFile_MaxFileAge>"
			+"        <newFile_InactiveSaveInterval display-name=\"Close file after nnn seconds of inactivity:\">60</newFile_InactiveSaveInterval>"
			+"      </subscriber-options>"
			+"      <subscriber-state>"
			+"        <addCount>1564</addCount>"
			+"      </subscriber-state>"
			+"    </init-params>"
			+"  </input>"
			+"</nds>";
    //public static final RangeConstraint rc = new RangeConstraint(0, (Integer.MAX_VALUE/1000));

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
/*			XmlDocument initDoc = new XmlDocument(initDocStr);
			XDSInitDocument initDocument = new XDSInitDocument(initDoc);
			HashMap<String,Parameter> paramDefs = new HashMap<String,Parameter>(6);

	        //Add file namer as param
			Parameter param1 = new Parameter("newFile_MaxFileAge", //tag name
	                "0", //default value (optional)
	                DataType.INT); //data type
	        param1.add(RequiredConstraint.REQUIRED);
	        param1.add(RequiredConstraint.REQUIRED);
	        param1.add(rc);
	        paramDefs.put(param1.tagName(), param1);

	        Parameter param2 = new Parameter("newFile_MaxRecords", //tag name
	                "0", //default value (optional)
	                DataType.INT); //data type
	        param2.add(RequiredConstraint.REQUIRED);
	        param2.add(RequiredConstraint.REQUIRED);
	        param2.add(rc);
	        paramDefs.put(param2.tagName(), param2);
	        initDocument.parameters(paramDefs);
	        
	        System.out.println(rc);
	        System.out.println("Param:"+param1.toInteger());*/
	        System.out.println(Build.BUILD_NO);
	        System.out.println(Build.COMPANY_NAME);
	        System.out.println(Build.PRODUCT_NAME);
	        System.out.println(Build.PRODUCT_VERSION);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
