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
package info.vancauwenberge.filedriver.filereader.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import com.novell.nds.dirxml.driver.Trace;

import info.vancauwenberge.filedriver.AbstractStrategyTest;
import info.vancauwenberge.filedriver.ParamMap;
import info.vancauwenberge.filedriver.api.IFileReadStrategy;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;

public class XMLFileReaderTester extends AbstractStrategyTest{

	@Mock
	IPublisher publisher;

	//The Folder will be created before each test method and (recursively) deleted after each test method.
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testReadRecord_WithTagsUsed() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XMLFileReader.Parameters.USE_TAG_NAMES.getParameterName(), true);
		params.putParameter(XMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XMLFileReader.Parameters.PRE_XSLT.getParameterName(), "");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"not,used,schema");
		final File f = temporaryFolder.newFile();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root><someRecord><AField>AValue</AField><BField>BValue</BField><CField>CValue</CField></someRecord></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//The file should contain only one record.
		assertNull(testSubject.readRecord());

		assertEquals(record.get("AField"), "AValue");
		assertEquals(record.get("BField"), "BValue");
		assertEquals(record.get("CField"), "CValue");
		assertEquals(record.keySet().size(), 3);

	}
	@Test
	public void testReadRecord_WithoutTagsUsed() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XMLFileReader.Parameters.USE_TAG_NAMES.getParameterName(), false);
		params.putParameter(XMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XMLFileReader.Parameters.PRE_XSLT.getParameterName(), "");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"not,used,schema");
		final File f = temporaryFolder.newFile();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root><someRecord><AField>AValue</AField><BField>BValue</BField><CField>CValue</CField></someRecord></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//The file should contain only one record.
		assertNull(testSubject.readRecord());

		assertEquals(record.get("not"), "AValue");
		assertEquals(record.get("used"), "BValue");
		assertEquals(record.get("schema"), "CValue");
		assertEquals(record.keySet().size(), 3);

	}

	@Test
	public void testReadRecord_MultipleRecords() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XMLFileReader.Parameters.USE_TAG_NAMES.getParameterName(), true);
		params.putParameter(XMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XMLFileReader.Parameters.PRE_XSLT.getParameterName(), "  ");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"not,used,schema");
		final File f = temporaryFolder.newFile();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root><someRecord><AField>AValue</AField><BField>BValue</BField><CField>CValue</CField></someRecord>"
				+ "<someRecord><AField>AValue2</AField><BField>BValue2</BField><CField>CValue2</CField></someRecord></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		Map<String, String> record = testSubject.readRecord();

		assertEquals(record.get("AField"), "AValue");
		assertEquals(record.get("BField"), "BValue");
		assertEquals(record.get("CField"), "CValue");
		assertEquals(record.keySet().size(), 3);

		record = testSubject.readRecord();

		assertEquals(record.get("AField"), "AValue2");
		assertEquals(record.get("BField"), "BValue2");
		assertEquals(record.get("CField"), "CValue2");
		assertEquals(record.keySet().size(), 3);

		//The file should contain no more records.
		assertNull(testSubject.readRecord());

	}
	@Test
	public void testReadRecord_XSLT() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XMLFileReader.Parameters.USE_TAG_NAMES.getParameterName(), true);
		params.putParameter(XMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XMLFileReader.Parameters.PRE_XSLT.getParameterName(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
				"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">"+
				"<xsl:output method=\"xml\" indent=\"yes\"/>"+
				"<xsl:template match=\"/UserDetails\">"+
				"<root>"+
				"<aRecord>"+
				"<xsl:for-each select=\"/UserDetails/*\">"+
				"<xsl:copy-of select=\".\"/>"+
				"</xsl:for-each>"+
				"</aRecord>"+
				"</root>"+
				"</xsl:template>"+
				"</xsl:stylesheet>");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"not,used,schema");
		final File f = temporaryFolder.newFile();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<UserDetails>"+
				"<AppUserID>10001</AppUserID>"+
				"<FirstName>Test10001</FirstName>"+
				"<LastName>Lname10001</LastName>"+
				"<EmailID>ttest10001@me.com</EmailID>"+
				"</UserDetails>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		assertEquals(record.get("AppUserID"), "10001");
		assertEquals(record.get("FirstName"), "Test10001");
		assertEquals(record.get("LastName"), "Lname10001");
		assertEquals(record.get("EmailID"), "ttest10001@me.com");
		assertEquals(record.keySet().size(), 4);


		//The file should contain no more records.
		assertNull(testSubject.readRecord());

	}
	@Test
	public void testReadRecord_NoRecords() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XMLFileReader.Parameters.USE_TAG_NAMES.getParameterName(), true);
		params.putParameter(XMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XMLFileReader.Parameters.PRE_XSLT.getParameterName(), "");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"not,used,schema");
		final File f = temporaryFolder.newFile();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);

		//The file should contain no more records.
		assertNull(testSubject.readRecord());

	}
}
