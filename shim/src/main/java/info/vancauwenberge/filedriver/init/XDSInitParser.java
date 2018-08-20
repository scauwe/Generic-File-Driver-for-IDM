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
package info.vancauwenberge.filedriver.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.novell.nds.dirxml.driver.XmlDocument;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.MultiRequiredConstraint;
import com.novell.nds.dirxml.driver.xds.MultiValueConstraint;
import com.novell.nds.dirxml.driver.xds.NonXDSElement;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSInitDocument;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;
import com.novell.nds.dirxml.driver.xds.XDSParseException;
import com.novell.nds.dirxml.driver.xds.XDSSubscriberOptionsElement;

public class XDSInitParser {
	private static final String inputDoc="<nds dtdversion=\"1.1\" ndsversion=\"8.6\">"
			+"        <source>"
			+"            <product version=\"1.1a\">DirXML</product>"
			+"            <contact>Novell, Inc.</contact>"
			+"        </source>"
			+"        <input>"
			+"            <init-params src-dn=\"\\NEW_DELL_TREE\\NOVELL\\Driver Set\\Skeleton Driver (Java, XDS)\\Subscriber\">"
			+"                <authentication-info>"
			+"                    <server>server.app:400</server>"
			+"                    <user>User1</user>"
			+"                </authentication-info>"
			+"                <driver-filter>"
			+"                    <allow-class class-name=\"User\">"
			+"                        <allow-attr attr-name=\"Surname\"/>"
			+"                        <allow-attr attr-name=\"Telephone Number\"/>"
			+"                        <allow-attr attr-name=\"Given Name\"/>"
			+"                    </allow-class>"
			+"                </driver-filter>"
			+"                <subscriber-options>"
			+"            		<sub-1 display-name=\"Sample Subscriber option\">String for Subscriber<sub-1-sub>aSubSubVal</sub-1-sub></sub-1>"
			//+"            		<sub-1 display-name=\"Sample Subscriber option\">String for Subscriber2</sub-1>"
			+"        		  </subscriber-options>"
			+"            </init-params>"
			+"        </input>"
			+"    </nds>";

	public XDSInitParser(final XmlDocument initXML) throws XDSParseException, XDSParameterException {
		this.initXML = initXML;
		final XDSInitDocument initDocument = new XDSInitDocument(initXML);
		System.out.println("server:"+initDocument.extractInitParamsElement().extractAuthenticationInfoElement().extractServerElement().extractText());
		//System.out.println("server:"+initDocument.extractInitParamsElement().extractSubscriberOptionsElement().paramText("sub-1"));
		final Map<String,Parameter> subParams = getParameterDefs();//getShimParameters();

		//initDocument.parameters(subParams);

		final XDSSubscriberOptionsElement subscriberOptions = initDocument.extractInitParamsElement().extractSubscriberOptionsElement();
		final List paramNodes = subscriberOptions.childElements();
		subscriberOptions.parameters(subParams);

		final com.novell.nds.dirxml.driver.xds.NonXDSElement child;
		for (final Object object : paramNodes) {
			System.out.println(object);
			if (object instanceof NonXDSElement){
				final NonXDSElement element = (NonXDSElement)object;
				System.out.println("  localName:"+element.localName());
				System.out.println("  tagName:"+element.tagName());
				System.out.println("  position:"+element.position());
				System.out.println("  extractText:"+element.extractText());
			}
		}
		/*
		final Parameter aParam = subParams.get("sub-1");
		System.out.println("toString="+aParam.toString());
		System.out.println("isContentSensitive="+aParam.isContentSensitive());
		System.out.println("hasContent="+aParam.hasContent());
		System.out.println("hasValues="+aParam.hasValues());
		System.out.println("isMultiValued="+aParam.isMultiValued());
		System.out.println("isScalar="+aParam.isScalar());
		System.out.println("isSingleValued="+aParam.isSingleValued());
		System.out.println("isStructured="+aParam.isStructured());*/
	}

	private Map<String, Parameter> getParameterDefs() {
		final Map<String, Parameter> map = new HashMap<String, Parameter>();
		final Parameter param = new Parameter("sub-1", null, DataType.STRUCT);
		param.add(new Parameter("sub-1-sub", "default", DataType.STRING));
		param.add(MultiValueConstraint.MULTI_VALUE);
		param.add(new MultiRequiredConstraint());
		map.put("sub-1", param);
		return map;
	}

	final XmlDocument initXML;


	public static void main(final String[] args){
		try {
			final XDSInitParser xds= new XDSInitParser(new XmlDocument(inputDoc));
		} catch (final XDSParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final XDSParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
