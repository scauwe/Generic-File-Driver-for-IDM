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

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.XmlDocument;
import com.novell.nds.dirxml.driver.xds.XDSParseException;
import com.novell.nds.dirxml.driver.xds.XDSQueryResultDocument;

public class XDSInstanceDocument extends XDSQueryResultDocument {
	
	private static XmlDocument rename(XmlDocument xmlDoc, Trace trace){
		Document doc = xmlDoc.getDocument();
		NodeList nodeList = doc.getFirstChild().getChildNodes();
		for(int j=0; j<nodeList.getLength(); j++){ 
			Node element = nodeList.item(j);
			if ("input".equals(element.getNodeName())){
				trace.trace("Found node with name "+element.getNodeName());
			    Element element2 = doc.createElement("output");
			    NamedNodeMap attrs = element.getAttributes();
			    for (int i = 0; i < attrs.getLength(); i++) {
			      Attr attr2 = (Attr) doc.importNode(attrs.item(i), true);
			      element2.getAttributes().setNamedItem(attr2);
			    }
			    while (element.hasChildNodes()) {
			      element2.appendChild(element.getFirstChild());
			    }
			    element.getParentNode().replaceChild(element2, element);				
			}
		}
	    return new XmlDocument(doc);
	  }	
	
	public XDSInstanceDocument(XmlDocument xmlDoc, Trace trace) throws XDSParseException{
		super(rename(xmlDoc,trace));
	}
	

}
