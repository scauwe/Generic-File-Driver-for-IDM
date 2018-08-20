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
package info.vancauwenberge.filedriver.filereader.raw;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;
import org.mockito.Mock;

import com.novell.nds.dirxml.driver.Trace;

import info.vancauwenberge.filedriver.AbstractStrategyTest;
import info.vancauwenberge.filedriver.ParamMap;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.exception.ReadException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;

public class RawFileReaderTester extends AbstractStrategyTest{
	private static final Decoder decoder = Base64.getDecoder();
	//private static final Encoder encoder = Base64.getEncoder();
	private static String TEST_STRING = "Qk12AQAAAAAAADYAAAAoAAAACgAAAAoAAAABABgAAAAAAEABAADEDgAAxA4AAAAAAAAAAAAA////////AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD///////8AAAAAAAAAAAAAAAAAAAD///////////8AAP///////wAAAAAAAAAAAAAAAAAAAP///////////wAA////////AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAkHO0kHO0AAADMSD/MSD/MSD/MSD/MSD/MSD/MSD8AACQc7SQc7QAAAMxIP8xIP8xIP8xIP8xIP8xIP8xIPwAAJBztJBztAAAAzEg/zEg/zEg/zEg/zEg/zEg/zEg/AAAkHO0kHO0AAADMSD/MSD/MSD/MSD/MSD/MSD/MSD8AACQc7SQc7SQc7cxIP8xIP8xIP8xIP8xIP8xIP8xIPwAAJBztJBztJBztzEg/zEg/zEg/zEg/zEg/zEg/zEg/AAA=";
	private static byte[] TEST_BYTES = decoder.decode(TEST_STRING);

	@Mock(answer=Answers.RETURNS_MOCKS)
	IDriver driver;

	@Mock
	IPublisher publisher;

	//The Folder will be created before each test method and (recursively) deleted after each test method.
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testPlain() throws Exception{
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(RawFileReader.Parameters.TYPE.getParameterName(), "TEXT");
		params.putParameter(RawFileReader.Parameters.TEXT_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(RawFileReader.Parameters.MAX_SIZE.getParameterName(), 2000);

		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		final FileWriter fw = new FileWriter(f);
		fw.write(TEST_STRING);
		fw.close();

		//Start the test
		final RawFileReader testSubject = new RawFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//An imagefile should contain only one record
		assertNull(testSubject.readRecord());

		assertEquals(record.keySet().size(), 1);
		assertEquals(record.get(RawFileReader.FIELD_RAW_DATA), TEST_STRING);
	}

	@Test(expected=ReadException.class)
	public void testMaxSize() throws Exception{
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(RawFileReader.Parameters.TYPE.getParameterName(), "TEXT");
		params.putParameter(RawFileReader.Parameters.TEXT_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(RawFileReader.Parameters.MAX_SIZE.getParameterName(), TEST_STRING.length()-1);

		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		final FileWriter fw = new FileWriter(f);
		fw.write(TEST_STRING);
		fw.close();

		//Start the test
		final RawFileReader testSubject = new RawFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//An imagefile should contain only one record
		assertNull(testSubject.readRecord());

		assertEquals(record.keySet().size(), 1);
		assertEquals(record.get(RawFileReader.FIELD_RAW_DATA), TEST_STRING);
	}
	@Test()
	public void testMaxSizeAllowed() throws Exception{
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(RawFileReader.Parameters.TYPE.getParameterName(), "TEXT");
		params.putParameter(RawFileReader.Parameters.TEXT_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(RawFileReader.Parameters.MAX_SIZE.getParameterName(), TEST_STRING.length());

		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		final FileWriter fw = new FileWriter(f);
		fw.write(TEST_STRING);
		fw.close();

		//Start the test
		final RawFileReader testSubject = new RawFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//An imagefile should contain only one record
		assertNull(testSubject.readRecord());

		assertEquals(record.keySet().size(), 1);
		assertEquals(record.get(RawFileReader.FIELD_RAW_DATA), TEST_STRING);
	}

	@Test
	public void testBinary() throws Exception{
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(RawFileReader.Parameters.TYPE.getParameterName(), "BINARY");
		params.putParameter(RawFileReader.Parameters.TEXT_ENCODING.getParameterName(), "UTF-8");
		params.putParameter(RawFileReader.Parameters.MAX_SIZE.getParameterName(), 2000);

		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		final FileOutputStream fw = new FileOutputStream(f);
		fw.write(TEST_BYTES);
		fw.close();

		//Start the test
		final RawFileReader testSubject = new RawFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//An imagefile should contain only one record
		assertNull(testSubject.readRecord());

		assertEquals(record.keySet().size(), 1);
		assertEquals(record.get(RawFileReader.FIELD_RAW_DATA), TEST_STRING);
	}

}
