package info.vancauwenberge.filedriver.filereader.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import com.novell.nds.dirxml.driver.Trace;

import info.vancauwenberge.filedriver.AbstractStrategyTest;
import info.vancauwenberge.filedriver.ParamMap;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;

public class CSVFileReaderTester extends AbstractStrategyTest{
	@Mock(answer=Answers.RETURNS_MOCKS)
	IDriver driver;

	@Mock
	IPublisher publisher;

	@Test
	public void testCSVWithHeaderUsed() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(CSVFileReader.Parameters.HAS_HEADER.getParameterName(), true);
		params.putParameter(CSVFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(CSVFileReader.Parameters.SEPERATOR.getParameterName(), ",");
		params.putParameter(CSVFileReader.Parameters.SKIP_EMPTY_LINES.getParameterName(), true);
		params.putParameter(CSVFileReader.Parameters.USE_HEADER_NAMES.getParameterName(), true);
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"not,used,schema");
		final File f = File.createTempFile("test", ".csv", new File(System.getProperty("user.dir")));
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("AField,BField,CField\n\nAValue,BValue,CValue\n");
		fw.close();

		//Start the test
		final CSVFileReader testSubject = new CSVFileReader();
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
	public void testCSVWithHeaderUnUsed() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(CSVFileReader.Parameters.HAS_HEADER.getParameterName(), true);
		params.putParameter(CSVFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(CSVFileReader.Parameters.SEPERATOR.getParameterName(), ",");
		params.putParameter(CSVFileReader.Parameters.SKIP_EMPTY_LINES.getParameterName(), true);
		params.putParameter(CSVFileReader.Parameters.USE_HEADER_NAMES.getParameterName(), false);
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"other,optional,schema");
		final File f = File.createTempFile("test", ".csv", new File(System.getProperty("user.dir")));
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("AField,BField,CField\nAValue,BValue,CValue");
		fw.close();
		//Start the test
		final CSVFileReader testSubject = new CSVFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//The file should contain only one record.
		assertNull(testSubject.readRecord());
		System.out.println(record);
		assertEquals(record.get("other"), "AValue");
		assertEquals(record.get("optional"), "BValue");
		assertEquals(record.get("schema"), "CValue");
		assertEquals(record.keySet().size(), 3);

	}

	@Test
	public void testCSVWithoutHeader() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(CSVFileReader.Parameters.HAS_HEADER.getParameterName(), false);
		params.putParameter(CSVFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(CSVFileReader.Parameters.SEPERATOR.getParameterName(), ",");
		params.putParameter(CSVFileReader.Parameters.SKIP_EMPTY_LINES.getParameterName(), true);
		params.putParameter(CSVFileReader.Parameters.USE_HEADER_NAMES.getParameterName(), false);
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"other,optional,schema");
		final File f = File.createTempFile("test", ".csv", new File(System.getProperty("user.dir")));
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("\n\n\nAValue,BValue,CValue\n\n\n");
		fw.close();
		//Start the test
		final CSVFileReader testSubject = new CSVFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//The file should contain only one record.
		assertNull(testSubject.readRecord());
		System.out.println(record);
		assertEquals(record.get("other"), "AValue");
		assertEquals(record.get("optional"), "BValue");
		assertEquals(record.get("schema"), "CValue");
		assertEquals(record.keySet().size(), 3);

	}

	@Test
	public void testReadRecord_WithHeaderNoRecords() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(CSVFileReader.Parameters.HAS_HEADER.getParameterName(), true);
		params.putParameter(CSVFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(CSVFileReader.Parameters.SEPERATOR.getParameterName(), ",");
		params.putParameter(CSVFileReader.Parameters.SKIP_EMPTY_LINES.getParameterName(), true);
		params.putParameter(CSVFileReader.Parameters.USE_HEADER_NAMES.getParameterName(), true);
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"not,used,schema");
		final File f = File.createTempFile("test", ".csv", new File(System.getProperty("user.dir")));
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("AField,BField,CField\n");
		fw.close();

		//Start the test
		final CSVFileReader testSubject = new CSVFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		assertNull(testSubject.readRecord());
	}


	@Test
	public void testReadRecord_WithoutHeaderNoRecords() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(CSVFileReader.Parameters.HAS_HEADER.getParameterName(), false);
		params.putParameter(CSVFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(CSVFileReader.Parameters.SEPERATOR.getParameterName(), ",");
		params.putParameter(CSVFileReader.Parameters.SKIP_EMPTY_LINES.getParameterName(), true);
		params.putParameter(CSVFileReader.Parameters.USE_HEADER_NAMES.getParameterName(), false);
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"other,optional,schema");
		final File f = File.createTempFile("test", ".csv", new File(System.getProperty("user.dir")));
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("\n\n\n");
		fw.close();
		//Start the test
		final CSVFileReader testSubject = new CSVFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);

		//The file should contain only one record.
		assertNull(testSubject.readRecord());
	}

	@Test
	public void testCSVTabSeperator() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(CSVFileReader.Parameters.HAS_HEADER.getParameterName(), true);
		params.putParameter(CSVFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(CSVFileReader.Parameters.SEPERATOR.getParameterName(), "\t");
		params.putParameter(CSVFileReader.Parameters.SKIP_EMPTY_LINES.getParameterName(), true);
		params.putParameter(CSVFileReader.Parameters.USE_HEADER_NAMES.getParameterName(), false);
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"other,optional,schema");
		final File f = File.createTempFile("test", ".csv", new File(System.getProperty("user.dir")));
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("AField\tBField\tCField\nAValue\tBValue\tCValue");
		fw.close();
		//Start the test
		final CSVFileReader testSubject = new CSVFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//The file should contain only one record.
		assertNull(testSubject.readRecord());
		System.out.println(record);
		assertEquals(record.get("other"), "AValue");
		assertEquals(record.get("optional"), "BValue");
		assertEquals(record.get("schema"), "CValue");
		assertEquals(record.keySet().size(), 3);

	}

	@Test
	public void testCSVTabSeperatorEmptyLines() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(CSVFileReader.Parameters.HAS_HEADER.getParameterName(), true);
		params.putParameter(CSVFileReader.Parameters.FORCED_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(CSVFileReader.Parameters.SEPERATOR.getParameterName(), "\t");
		params.putParameter(CSVFileReader.Parameters.SKIP_EMPTY_LINES.getParameterName(), false);
		params.putParameter(CSVFileReader.Parameters.USE_HEADER_NAMES.getParameterName(), true);
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"other,optional,schema");
		final File f = File.createTempFile("test", ".csv", new File(System.getProperty("user.dir")));
		f.deleteOnExit();
		//Write the CSV file
		final FileWriter fw = new FileWriter(f);
		fw.write("AField\tBField\tCField\n\nAValue\tBValue\tCValue");
		fw.close();
		//Start the test
		final CSVFileReader testSubject = new CSVFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		//The file should contain two record records: an empty (first) and non empty (second)
		Map<String, String> record = testSubject.readRecord();
		assertEquals(record.keySet().size(), 1);
		assertEquals(record.get("AField"), "");

		record = testSubject.readRecord();

		assertNull(testSubject.readRecord());
		System.out.println(record);
		assertEquals(record.get("AField"), "AValue");
		assertEquals(record.get("BField"), "BValue");
		assertEquals(record.get("CField"), "CValue");
		assertEquals(record.keySet().size(), 3);

	}
}
