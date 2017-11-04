package info.vancauwenberge.filedriver.filereader.xml;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

import info.vancauwenberge.filedriver.AbstractStrategyTest;
import info.vancauwenberge.filedriver.ParamMap;
import info.vancauwenberge.filedriver.api.IFileReadStrategy;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;

public class XPathXMLFileReaderTester extends AbstractStrategyTest{
	@Mock
	IPublisher publisher;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	//The Folder will be created before each test method and (recursively) deleted after each test method.
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();


	@Test
	public void testReadRecord_OneRecordUseDriverSchema() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XPathXMLFileReader.Parameters.USE_DRIVER_SCHEMA.getParameterName(), true);
		params.putParameter(XPathXMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_RECORD_NODESET.getParameterName(), "/root/someRecord");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_EXPRESSIONS.getParameterName(), "AField/text(),BField/text(),CField/text()");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"AField,BField,CField");
		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root><someRecord><AField>AValue</AField><BField>BValue</BField><CField>CValue</CField></someRecord></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XPathXMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		assertEquals(record.get("AField"), "AValue");
		assertEquals(record.get("BField"), "BValue");
		assertEquals(record.get("CField"), "CValue");
		assertEquals(record.keySet().size(), 3);

		//The file should contain only one record.
		assertNull(testSubject.readRecord());


	}

	@Test
	public void testReadRecord_MoreRecordUseDriverSchema() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XPathXMLFileReader.Parameters.USE_DRIVER_SCHEMA.getParameterName(), true);
		params.putParameter(XPathXMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_RECORD_NODESET.getParameterName(), "/root/someRecord");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_EXPRESSIONS.getParameterName(), "AField/text(),BField/text(),CField/text()");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"AField,BField,CField");
		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root><someRecord><AField>AValue</AField><BField>BValue</BField><CField>CValue</CField></someRecord>"
				+ "<someRecord><AField>AValue2</AField><BField>BValue2</BField><CField>CValue2</CField></someRecord></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XPathXMLFileReader();
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
		//The file should contain only one record.
		assertNull(testSubject.readRecord());


	}

	@Test
	public void testReadRecord_OneRecordUseXPathSchema() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XPathXMLFileReader.Parameters.USE_DRIVER_SCHEMA.getParameterName(), false);
		params.putParameter(XPathXMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_RECORD_NODESET.getParameterName(), "/root/someRecord");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_EXPRESSIONS.getParameterName(), "AField/text(),BField/text(),CField/text()");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"AField,BField,CField");
		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root><someRecord><AField>AValue</AField><BField>BValue</BField><CField>CValue</CField></someRecord></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XPathXMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		assertEquals(record.get("AField_text__"), "AValue");
		assertEquals(record.get("BField_text__"), "BValue");
		assertEquals(record.get("CField_text__"), "CValue");
		assertEquals(record.keySet().size(), 3);

		//The file should contain only one record.
		assertNull(testSubject.readRecord());
	}

	@Test
	public void testReadRecord_MoreRecordUseXPathSchema() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XPathXMLFileReader.Parameters.USE_DRIVER_SCHEMA.getParameterName(), false);
		params.putParameter(XPathXMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_RECORD_NODESET.getParameterName(), "/root/someRecord");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_EXPRESSIONS.getParameterName(), "AField/text(),BField/text(),CField/text()");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"AField,BField,CField");
		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root><someRecord><AField>AValue</AField><BField>BValue</BField><CField>CValue</CField></someRecord>"
				+ "<someRecord><AField>AValue2</AField><BField>BValue2</BField><CField>CValue2</CField></someRecord></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XPathXMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		Map<String, String> record = testSubject.readRecord();

		assertEquals(record.get("AField_text__"), "AValue");
		assertEquals(record.get("BField_text__"), "BValue");
		assertEquals(record.get("CField_text__"), "CValue");
		assertEquals(record.keySet().size(), 3);

		record = testSubject.readRecord();

		assertEquals(record.get("AField_text__"), "AValue2");
		assertEquals(record.get("BField_text__"), "BValue2");
		assertEquals(record.get("CField_text__"), "CValue2");
		assertEquals(record.keySet().size(), 3);
		//The file should contain only one record.
		assertNull(testSubject.readRecord());


	}


	@Test
	public void testReadRecord_NoRecordUseXPathSchema() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XPathXMLFileReader.Parameters.USE_DRIVER_SCHEMA.getParameterName(), false);
		params.putParameter(XPathXMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_RECORD_NODESET.getParameterName(), "/root/someRecord");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_EXPRESSIONS.getParameterName(), "AField/text(),BField/text(),CField/text()");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"AField,BField,CField");
		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XPathXMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//The file should contain only one record.
		assertNull(record);


	}

	@Test
	public void testReadRecord_XPathReturnTypes() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XPathXMLFileReader.Parameters.USE_DRIVER_SCHEMA.getParameterName(), true);
		params.putParameter(XPathXMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_RECORD_NODESET.getParameterName(), "/root/someRecord");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_EXPRESSIONS.getParameterName(), "AField/@value,local-name(BField),count(CField)>0,count(CField)");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"AField,BField,CField,DField");
		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root><someRecord><AField value='AValue'></AField><BField>BValue</BField><CField>CValue</CField></someRecord></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XPathXMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();
		assertEquals(record.get("AField"), "AValue");
		assertEquals(record.get("BField"), "BField");
		assertEquals(record.get("CField"), "true");
		assertEquals(record.get("DField"), "1");
		assertEquals(record.keySet().size(), 4);

		//The file should contain only one record.
		assertNull(testSubject.readRecord());
	}


	@Test
	public void testReadRecord_XPathMerged() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XPathXMLFileReader.Parameters.USE_DRIVER_SCHEMA.getParameterName(), true);
		params.putParameter(XPathXMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_RECORD_NODESET.getParameterName(), "/root/someRecord");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_EXPRESSIONS.getParameterName(), "AField[@value=\"test,me\"]/text(),AField[@value=\"test,meto\"]/text()");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"AField,BField,CField,DField");
		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root><someRecord><AField value='test,me'>AValue</AField><AField value='test,meto'>BValueTo</AField><BField>BValue</BField><CField>CValue</CField></someRecord></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XPathXMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();
		assertEquals(record.get("AField"), "AValue");
		assertEquals(record.get("BField"), "BValueTo");
		assertEquals(record.keySet().size(), 2);

		//The file should contain only one record.
		assertNull(testSubject.readRecord());
	}

	@Test
	public void testReadRecord_SchemaToShort() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XPathXMLFileReader.Parameters.USE_DRIVER_SCHEMA.getParameterName(), true);
		params.putParameter(XPathXMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_RECORD_NODESET.getParameterName(), "/root/someRecord");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_EXPRESSIONS.getParameterName(), "AField/@value,local-name(BField),count(CField)>0,count(CField)");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"AField,BField");
		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root><someRecord><AField value='AValue'></AField><BField>BValue</BField><CField>CValue</CField></someRecord></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XPathXMLFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();
		System.out.println(record);
		assertEquals(record.get("AField"), "AValue");
		assertEquals(record.get("BField"), "BField");
		assertEquals(record.get("field2"), "true");
		assertEquals(record.get("field3"), "1");
		assertEquals(record.keySet().size(), 4);

		//The file should contain only one record.
		assertNull(testSubject.readRecord());
	}


	@Test
	public void testReadRecord_XPathInvalid() throws Exception {

		//test type
		thrown.expect(XDSParameterException.class);

		//test message
		thrown.expectMessage(is("Invalid XPath. Renaming part:local-nam(BField),count(CField)>0,count(CField)"));

		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(XPathXMLFileReader.Parameters.USE_DRIVER_SCHEMA.getParameterName(), true);
		params.putParameter(XPathXMLFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_RECORD_NODESET.getParameterName(), "/root/someRecord");
		params.putParameter(XPathXMLFileReader.Parameters.XPATH_EXPRESSIONS.getParameterName(), "AField/@value,local-nam(BField),count(CField)>0,count(CField)");
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"AField,BField,CField,DField");
		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("<root><someRecord><AField value='AValue'></AField><BField>BValue</BField><CField>CValue</CField></someRecord></root>");
		fw.close();

		//Start the test
		final IFileReadStrategy testSubject = new XPathXMLFileReader();
		testSubject.init(trace,params,publisher);
		fail("Exception should have been thrown");

	}
}
