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
package info.vancauwenberge.filedriver.filereader.xml;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import info.vancauwenberge.filedriver.filereader.*;

public class SaxHandler extends DefaultHandler {
	private RecordQueue queue = new RecordQueue();
	
	private int level = 0;
	private StringBuffer currentValue;
	private HashMap<String,String> currentRecord;
	
	private HashMap<String,String> queuedRecord;
	//private Object semaphore = new Object();

	private String[] alternateTags;
	private List<String> actualFirstRecordTags = new ArrayList<String>();
	private boolean firstRecordRead = false;
	private int currentTagIndex = 0;

	private boolean useTagNames;
	
	/**
	 * Get the fieldname to use for the current field
	 * @param xmlTag
	 * @return
	 */
	private String getFieldName(String xmlTag){
		if (useTagNames)
			return xmlTag;
		if (currentTagIndex>alternateTags.length-1)
			return xmlTag;
		return alternateTags[currentTagIndex];
	}
	
	
	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	public void startDocument() {
		System.out.println("Start document");
		level++;
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	public void endDocument() {
		System.out.println("End document");
		level--;
		queue.setFinished();

		synchronized (actualFirstRecordTags) {
			actualFirstRecordTags.notifyAll();
		}
	}
	
	
	/**
	 * @param uri the Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
	 * @param name the local name (without prefix), or the empty string if Namespace processing is not being performed.
	 * @param qName the qualified XML name (with prefix), or the empty string if qualified names are not available.
	 */
	public void startElement(String uri, String localName, String qName, Attributes atts) {
		level++;
		currentValue = new StringBuffer();
		//Level == 3 => new record is started
		if (level == 3){
			currentTagIndex = 0;
			currentRecord = new HashMap<String,String>();
		}
		//DEBUG
		/*
		if ("".equals(uri))
			System.out.println("Start element: (level,name)=("+level+"," + qName+")");
		else
			System.out.println("Start element: (level,uri,name)={"+level+"," + uri + "," + localName+")");
			*/
	}
	
	public String[] getCurrentSchema(){
		synchronized (actualFirstRecordTags) {
			if (!firstRecordRead)
				try {
					actualFirstRecordTags.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		return actualFirstRecordTags.toArray(new String[actualFirstRecordTags.size()]);
	}
	
	/**
	 * @param uri the Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
	 * @param name the local name (without prefix), or the empty string if Namespace processing is not being performed.
	 * @param qName the qualified XML name (with prefix), or the empty string if qualified names are not available.
	 */
	public void endElement(String uri, String localName, String qName) {
		//If level == 3: we ended a field for the current tag.
		if (level==4)
		{
			String fieldName = getFieldName(localName);
			currentRecord.put(fieldName, currentValue.toString());
			currentValue = new StringBuffer();
			currentTagIndex++;
			if (!firstRecordRead)
				actualFirstRecordTags.add(fieldName);
		}
		else
			//If level == 4: we ended a record
			if (level==3){
				synchronized (actualFirstRecordTags) {
					firstRecordRead = true;
					actualFirstRecordTags.notifyAll();
				}
				//synchronized(semaphore){
					if (queuedRecord == null){
						queuedRecord = currentRecord;
					}
				//}
				//Save the parsed record in the queue for processing
				queue.addRecord(currentRecord);
			}

		//DEBUG
			/*
		if ("".equals(uri))
			System.out.println("End element: (level,name)=("+level+"," + qName+")");
		else
			System.out.println("End element: (level,uri,name)={"+level+"," + uri + "," + localName+")");
		System.out.println("content:"+currentValue.toString());
*/
		//END DEBUG
		level--;
	}

	public void characters(char ch[], int start, int length) {
		currentValue.append(ch,start,length);
		/*
		System.out.print("Characters:    \"");
		for (int i = start; i < start + length; i++) {
			switch (ch[i]) {
				case '\\' :
					System.out.print("\\\\");
					break;
				case '"' :
					System.out.print("\\\"");
					break;
				case '\n' :
					System.out.print("\\n");
					break;
				case '\r' :
					System.out.print("\\r");
					break;
				case '\t' :
					System.out.print("\\t");
					break;
				default :
					System.out.print(ch[i]);
					break;
			}
		}
		System.out.print("\"\n");
		*/
	}
	
	/**
	 * @return Returns the queue.
	 */
	public RecordQueue getQueue() {
		return queue;
	}

	
	public static void main(String args[]) throws Exception {
		System.setProperty("org.xml.sax.driver","org.apache.xerces.parsers.SAXParser");
		
		final XMLReader xr = XMLReaderFactory.createXMLReader();
		SaxHandler handler = new SaxHandler(true, new String[]{"first"});
		xr.setContentHandler(handler);
		xr.setErrorHandler(handler);
		
		Thread th = new Thread(){
			public void run(){
				FileReader r;
				try {
					r = new FileReader("C:\\workspace\\DirXMLDriver\\testinput.xml");
					xr.parse(new InputSource(r));				
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		th.start();
		
		RecordQueue queue = handler.getQueue();
		Map<String,String> aRecord;
		while ((aRecord = queue.getNextRecord()) != null) {
			System.out.println(aRecord);
		}
		System.out.println("DONE");
	}
	
	/**
	 * @param useTagNames do we use the tag names found in te xml file as field names or not
	 * @param alternateTags if <code>useTagNames</code> is false, the actual tags to use
	 */
	/**
	 * 
	 */
	protected SaxHandler(boolean useTagNames, String[] alternateTags) {
		super();
		this.useTagNames = useTagNames;
		this.alternateTags = alternateTags;
		if ((useTagNames == false) && (alternateTags==null)){
			this.useTagNames = true;
		}
	}
}