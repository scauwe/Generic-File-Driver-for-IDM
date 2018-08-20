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
package info.vancauwenberge.filedriver.filewriter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;
import org.mockito.Mock;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;

import info.vancauwenberge.filedriver.AbstractStrategyTest;
import info.vancauwenberge.filedriver.ParamMap;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.exception.ReadException;
import info.vancauwenberge.filedriver.exception.WriteException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.shim.driver.GenericFileDriverShim;

public class CSVFileWriterTester extends AbstractStrategyTest{
	@Mock(answer=Answers.RETURNS_MOCKS)
	IDriver driver;

	@Mock
	IPublisher publisher;

	//The Folder will be created before each test method and (recursively) deleted after each test method.
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();


	private static List<Byte> toList(final byte[] array) {
		if (array == null) {
			return null;
		} else if (array.length == 0) {
			return Collections.emptyList();
		}
		final Byte[] result = new Byte[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = new Byte(array[i]);
		}
		return Arrays.asList(result);
	}
	private String readFile(final File f) throws IOException, ReadException {
		final byte[] buffer = new byte[(int)f.length()];
		System.out.println("Buffer size:"+buffer.length);
		FileInputStream inputStream=null;
		try {
			inputStream = new FileInputStream(f);
			if (inputStream.read(buffer) == -1) {
				throw new RuntimeException("Unexpected EOF reached while trying to read the whole file");
			}
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (final IOException e) {
			}
		}
		return new String(buffer,Charset.defaultCharset());
	}

	@Test
	public void testCSVWithHeaderNoQuotes() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(CSVFileWriter.Parameters.CSV_FILE_WRITE_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(CSVFileWriter.Parameters.CSV_FILE_WRITE_FLSUH.getParameterName(), false);
		params.putParameter(CSVFileWriter.Parameters.CSV_FILE_WRITE_HEADER.getParameterName(), true);
		params.putParameter(CSVFileWriter.Parameters.CSV_FILE_WRITE_SEPERATOR.getParameterName(), ";");
		params.putParameter(CSVFileWriter.Parameters.CSV_FILE_QUOTE_ALWAYS.getParameterName(), false);
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"field1,field2,field3");

		final String result = doTestNormal(trace, params);

		assertEquals(result,"field1;field2;field3"+System.getProperty("line.separator")+"\"value1\nnewline\";\"value2\"\"quote\";value3"+System.getProperty("line.separator"));
	}


	@Test
	public void testCSVWithHeaderWithQuotes() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(CSVFileWriter.Parameters.CSV_FILE_WRITE_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(CSVFileWriter.Parameters.CSV_FILE_WRITE_FLSUH.getParameterName(), false);
		params.putParameter(CSVFileWriter.Parameters.CSV_FILE_WRITE_HEADER.getParameterName(), true);
		params.putParameter(CSVFileWriter.Parameters.CSV_FILE_WRITE_SEPERATOR.getParameterName(), ";");
		params.putParameter(CSVFileWriter.Parameters.CSV_FILE_QUOTE_ALWAYS.getParameterName(), true);
		params.putParameter(GenericFileDriverShim.DriverParam.SCHEMA.getParamName(),"field1,field2,field3");

		final String result = doTestNormal(trace, params);

		//The file should contain only one record.
		//Potential issue: the \n used for the record seperator is platform specific, the \n in the field value is not...
		assertEquals(result,"\"field1\";\"field2\";\"field3\""+System.getProperty("line.separator")+"\"value1\nnewline\";\"value2\"\"quote\";\"value3\""+System.getProperty("line.separator"));

	}
	private String doTestNormal(final Trace trace, final ParamMap params)
			throws IOException, XDSParameterException, WriteException, ReadException {
		final File f = temporaryFolder.newFile();

		//Start the test
		final CSVFileWriter testSubject = new CSVFileWriter();
		testSubject.init(trace,params,driver);
		testSubject.openFile(f);
		final Map<String, String> record = new HashMap<String, String>();
		record.put("field1", "value1\nnewline");
		record.put("field2", "value2\"quote");
		record.put("field3", "value3");
		testSubject.writeRecord(record);
		testSubject.close();

		//Read the CSV file
		final String result = readFile(f);
		return result;
	}
}
