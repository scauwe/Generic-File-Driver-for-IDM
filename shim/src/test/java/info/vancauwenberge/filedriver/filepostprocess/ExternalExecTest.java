/*******************************************************************************
 * Copyright (c) 2007, 2017 Stefaan Van Cauwenberge
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
 *  the Initial Developer are Copyright (C) 2007, 2017 by
 * Stefaan Van Cauwenberge. All Rights Reserved.
 *
 * Contributor(s): none so far.
 *    Stefaan Van Cauwenberge: Initial API and implementation
 *******************************************************************************/
package info.vancauwenberge.filedriver.filepostprocess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;
import org.mockito.Mock;

import com.novell.nds.dirxml.driver.Trace;

import info.vancauwenberge.filedriver.AbstractStrategyTest;
import info.vancauwenberge.filedriver.ParamMap;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.ISubscriberShim;
import info.vancauwenberge.filedriver.shim.ConnectionInfo;

public class ExternalExecTest extends AbstractStrategyTest{
	@Mock(answer=Answers.RETURNS_MOCKS)
	IDriver driver;

	@Mock
	ISubscriberShim subscriber;

	//The Folder will be created before each test method and (recursively) deleted after each test method.
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testShortCommand() throws Exception {
		final Trace trace = new Trace(">");

		final ParamMap params = new ParamMap();
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS.getParameterName(), 1000);
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "dir . $PARENTPATH$ $FILEPATH$ $FILENAME$");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_WORKDIR.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_CONNECTURLTRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_USERNAMETRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_PASSWORDTRIGGER.getParameterName(), "");

		final ExternalExec testSubject = new ExternalExec();
		testSubject.init(trace,params,driver);

		final File f = temporaryFolder.newFile();
		final long startTime = System.currentTimeMillis();
		testSubject.doPostProcess(f);
		final long endTime = System.currentTimeMillis();
		assertTrue((endTime-startTime) < 100000);//Should be faster than 100 seconds
	}

	@Test
	public void testLongCommand() throws Exception {
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS.getParameterName(), 5);
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "choice /m:$PARENTPATH$");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_WORKDIR.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_CONNECTURLTRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_USERNAMETRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_PASSWORDTRIGGER.getParameterName(), "");

		final ExternalExec testSubject = new ExternalExec();
		testSubject.init(trace,params,driver);

		final File f = temporaryFolder.newFile();
		final long startTime = System.currentTimeMillis();
		testSubject.doPostProcess(f);
		final long endTime = System.currentTimeMillis();
		assertTrue((endTime-startTime) > 5000);//Should be 10 seconds or more
	}

	@Test
	public void testWorkingDirConfigured() throws Exception {
		//This test is OS specific
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		final File explicitWorkDir = temporaryFolder.newFolder("aSubFolder");
		final String workdir = explicitWorkDir.getAbsolutePath();//System.getProperty("java.io.tmpdir"); 
		//Create a temp file in the current working dir		
		final File tmpFile = temporaryFolder.newFile();

		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS.getParameterName(), 1000);
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"echo %cd%>> \""+tmpFile.getAbsolutePath()+"\"\"");
		//We configure the working dir
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_WORKDIR.getParameterName(), workdir);
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_CONNECTURLTRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_USERNAMETRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_PASSWORDTRIGGER.getParameterName(), "");

		final ExternalExec testSubject = new ExternalExec();
		testSubject.init(trace,params,driver);

		final File f = temporaryFolder.newFile();
		testSubject.doPostProcess(f);

		final FileReader fr = new FileReader(tmpFile);
		final BufferedReader br = new BufferedReader(fr);
		assertEquals(workdir, br.readLine());
		br.close();
	}

	@Test
	public void testWorkingDirFallBack() throws Exception {
		final Trace trace = new Trace(">");
		final File tmpFile = temporaryFolder.newFile();

		final ParamMap params = new ParamMap();
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS.getParameterName(), 1000);
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"echo %cd%>> \""+tmpFile.getAbsolutePath()+"\"\"");
		//No working dir configured.
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_WORKDIR.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_CONNECTURLTRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_USERNAMETRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_PASSWORDTRIGGER.getParameterName(), "");

		final ExternalExec testSubject = new ExternalExec();
		testSubject.init(trace,params,driver);

		//The file should not reside in the workdir this time!!!
		final File workDir = temporaryFolder.newFolder();
		final File f = new File(workDir,"dummy.csv");
		testSubject.doPostProcess(f);

		final FileReader fr = new FileReader(tmpFile);
		final BufferedReader br = new BufferedReader(fr);
		assertEquals(workDir.getAbsolutePath(), br.readLine());
		br.close();
	}

	@Test
	public void testTokenReplacementConnectionLess() throws Exception {
		//$PARENTPATH$ C:\temp
		//$FILENAME$   out.csv
		//$FILEPATH$   C:\temp\out.csv
		final Trace trace = new Trace(">");
		final File tmpFile = temporaryFolder.newFile();

		final ParamMap params = new ParamMap();
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS.getParameterName(), 1000);
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"echo $PARENTPATH$ $FILENAME$ $FILEPATH$>> \""+tmpFile.getAbsolutePath()+"\"\"");
		//No working dir configured.
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_WORKDIR.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_CONNECTURLTRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_USERNAMETRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_PASSWORDTRIGGER.getParameterName(), "");

		final ExternalExec testSubject = new ExternalExec();
		testSubject.init(trace,params,driver);
		//The file should not reside in the workdir this time!!!
		final File tmpFFolder = temporaryFolder.newFolder();
		final File f = new File(tmpFFolder,"dummy.tst");
		testSubject.doPostProcess(f);

		final FileReader fr = new FileReader(tmpFile);
		final BufferedReader br = new BufferedReader(fr);
		assertEquals(f.getParent()+" "+f.getName()+" "+f.getAbsolutePath(), br.readLine());
		br.close();
	}

	@Test
	public void testTokenReplacementConnectionFull() throws Exception {
		//$PARENTPATH$ C:\temp
		//$FILENAME$   out.csv
		//$FILEPATH$   C:\temp\out.csv
		final Trace trace = new Trace(">");
		final File tmpFile = temporaryFolder.newFile();

		final ParamMap params = new ParamMap();
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS.getParameterName(), 1000);
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"echo $PARENTPATH$ $FILENAME$ $FILEPATH$ $CONNECTUSER$ $CONNECTPASSWORD$ $CONNECTURL$>> \""+tmpFile.getAbsolutePath()+"\"");
		//No working dir configured.
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_WORKDIR.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_CONNECTURLTRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_USERNAMETRIGGER.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_PASSWORDTRIGGER.getParameterName(), "");

		final ExternalExec testSubject = new ExternalExec();
		final IDriver driver = mock(IDriver.class);
		final ISubscriberShim subscriber = mock(ISubscriberShim.class);
		when(subscriber.getConnectionInfo()).thenReturn(new ConnectionInfo("aPassword", "aUserName", "aConnectURL"));
		when(driver.getSubscriber()).thenReturn(subscriber);

		testSubject.init(trace,params,driver);
		//The file should not reside in the workdir this time!!!
		final File tmpFFolder = temporaryFolder.newFolder();
		final File f = new File(tmpFFolder,"dummy.tst");
		testSubject.doPostProcess(f);

		final FileReader fr = new FileReader(tmpFile);
		final BufferedReader br = new BufferedReader(fr);
		assertEquals(f.getParent()+" "+f.getName()+" "+f.getAbsolutePath()+" aUserName aPassword aConnectURL", br.readLine());
		br.close();
	}

	@Test
	public void testStdInUserName() throws Exception {
		//$PARENTPATH$ C:\temp
		//$FILENAME$   out.csv
		//$FILEPATH$   C:\temp\out.csv
		final Trace trace = new Trace(">");
		final File tmpFile = temporaryFolder.newFile();

		final ParamMap params = new ParamMap();
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS.getParameterName(), 3);
		//set /P id="Enter token:" & call echo %^id%>C:\temp\tmp.out

		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"set /P id=\"\"Enter token:\"\"  & call echo %^id%>> \""+tmpFile.getAbsolutePath()+"\"");
		//params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"set /P id=\"\"Enter token:\"\"  \"");
		//No working dir configured.
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_WORKDIR.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_CONNECTURLTRIGGER.getParameterName(), "Enter URL:");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_USERNAMETRIGGER.getParameterName(), "Enter token:");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_PASSWORDTRIGGER.getParameterName(), "Enter password:");

		final ExternalExec testSubject = new ExternalExec();
		final IDriver driver = mock(IDriver.class);
		final ISubscriberShim subscriber = mock(ISubscriberShim.class);
		when(subscriber.getConnectionInfo()).thenReturn(new ConnectionInfo("aPassword", "aUserName", "aConnectURL"));
		when(driver.getSubscriber()).thenReturn(subscriber);

		testSubject.init(trace,params,driver);
		//The file should not reside in the workdir this time!!!
		final File tmpFFolder = temporaryFolder.newFolder();
		final File f = new File(tmpFFolder,"dummy.tst");
		testSubject.doPostProcess(f);

		final FileReader fr = new FileReader(tmpFile);
		final BufferedReader br = new BufferedReader(fr);
		assertEquals("aUserName", br.readLine());
		br.close();
	}

	@Test
	public void testStdInPassword() throws Exception {
		//$PARENTPATH$ C:\temp
		//$FILENAME$   out.csv
		//$FILEPATH$   C:\temp\out.csv
		final Trace trace = new Trace(">");
		final File tmpFile = temporaryFolder.newFile();

		final ParamMap params = new ParamMap();
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS.getParameterName(), 3);
		//set /P id="Enter token:" & call echo %^id%>C:\temp\tmp.out

		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"set /P id=\"\"Enter password:\"\"  & call echo %^id%>> \""+tmpFile.getAbsolutePath()+"\"");
		//params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"set /P id=\"\"Enter token:\"\"  \"");
		//No working dir configured.
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_WORKDIR.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_CONNECTURLTRIGGER.getParameterName(), "Enter URL:");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_USERNAMETRIGGER.getParameterName(), "Enter token:");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_PASSWORDTRIGGER.getParameterName(), "Enter password:");

		final ExternalExec testSubject = new ExternalExec();
		final IDriver driver = mock(IDriver.class);
		final ISubscriberShim subscriber = mock(ISubscriberShim.class);
		when(subscriber.getConnectionInfo()).thenReturn(new ConnectionInfo("aPassword", "aUserName", "aConnectURL"));
		when(driver.getSubscriber()).thenReturn(subscriber);

		testSubject.init(trace,params,driver);
		//The file should not reside in the workdir this time!!!
		final File tmpFFolder = temporaryFolder.newFolder();
		final File f = new File(tmpFFolder,"dummy.tst");
		testSubject.doPostProcess(f);

		final FileReader fr = new FileReader(tmpFile);
		final BufferedReader br = new BufferedReader(fr);
		assertEquals("aPassword", br.readLine());
		br.close();
	}

	@Test
	public void testStdInConnectURL() throws Exception {
		//$PARENTPATH$ C:\temp
		//$FILENAME$   out.csv
		//$FILEPATH$   C:\temp\out.csv
		final Trace trace = new Trace(">");
		final File tmpFile = temporaryFolder.newFile();

		final ParamMap params = new ParamMap();
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS.getParameterName(), 3);
		//set /P id="Enter token:" & call echo %^id%>C:\temp\tmp.out

		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"set /P id=\"\"Enter URL:\"\"  & call echo %^id%>> \""+tmpFile.getAbsolutePath()+"\"");
		//params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"set /P id=\"\"Enter token:\"\"  \"");
		//No working dir configured.
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_WORKDIR.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_CONNECTURLTRIGGER.getParameterName(), "Enter URL:");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_USERNAMETRIGGER.getParameterName(), "Enter token:");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_PASSWORDTRIGGER.getParameterName(), "Enter password:");

		final ExternalExec testSubject = new ExternalExec();
		final IDriver driver = mock(IDriver.class);
		final ISubscriberShim subscriber = mock(ISubscriberShim.class);
		when(subscriber.getConnectionInfo()).thenReturn(new ConnectionInfo("aPassword", "aUserName", "aConnectURL"));
		when(driver.getSubscriber()).thenReturn(subscriber);

		testSubject.init(trace,params,driver);
		//The file should not reside in the workdir this time!!!
		final File tmpFFolder = temporaryFolder.newFolder();
		final File f = new File(tmpFFolder,"dummy.tst");
		testSubject.doPostProcess(f);

		final FileReader fr = new FileReader(tmpFile);
		final BufferedReader br = new BufferedReader(fr);
		assertEquals("aConnectURL", br.readLine());
		br.close();
	}

	@Test
	public void testStdInAll() throws Exception {
		//$PARENTPATH$ C:\temp
		//$FILENAME$   out.csv
		//$FILEPATH$   C:\temp\out.csv
		final Trace trace = new Trace(">");
		final File tmpFile = temporaryFolder.newFile();

		final ParamMap params = new ParamMap();
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS.getParameterName(), 3);
		//set /P id="Enter token:" & call echo %^id%>C:\temp\tmp.out

		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"set /P id=\"\"Enter token:\"\" & call echo %^id%>> \""+tmpFile.getAbsolutePath()+
				" & set /P pswd=\"\"Enter password:\"\"  & call echo %^pswd%>> \""+tmpFile.getAbsolutePath()+
				" & set /P url=\"\"Enter URL:\"\"  & call echo %^url%>> \""+tmpFile.getAbsolutePath()+"\"");
		//params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"set /P id=\"\"Enter token:\"\"  \"");
		//No working dir configured.
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_WORKDIR.getParameterName(), "");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_CONNECTURLTRIGGER.getParameterName(), "Enter URL:");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_USERNAMETRIGGER.getParameterName(), "Enter token:");
		params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_PASSWORDTRIGGER.getParameterName(), "Enter password:");

		final ExternalExec testSubject = new ExternalExec();
		final IDriver driver = mock(IDriver.class);
		final ISubscriberShim subscriber = mock(ISubscriberShim.class);
		when(subscriber.getConnectionInfo()).thenReturn(new ConnectionInfo("aPassword", "aUserName", "aConnectURL"));
		when(driver.getSubscriber()).thenReturn(subscriber);

		testSubject.init(trace,params,driver);
		//The file should not reside in the workdir this time!!!
		final File tmpFFolder = temporaryFolder.newFolder();
		final File f = new File(tmpFFolder,"dummy.tst");
		testSubject.doPostProcess(f);

		final FileReader fr = new FileReader(tmpFile);
		final BufferedReader br = new BufferedReader(fr);
		assertEquals("aUserName ", br.readLine());
		assertEquals("aPassword ", br.readLine());
		assertEquals("aConnectURL", br.readLine());
		br.close();
	}
	/*
	@Test
	public void testStdInftp() {
		//$PARENTPATH$ C:\temp
		//$FILENAME$   out.csv
		//$FILEPATH$   C:\temp\out.csv
		Trace.registerImpl(SysoutTrace.class, 0);
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		final String workdir = System.getProperty("java.io.tmpdir"); 
		//Create a temp file in the current working dir		
		final File tmpFile;
		try {
			//We will echo the working dir in the file. This test is OS specific
			tmpFile = File.createTempFile("test", ".tmp", new File(System.getProperty("user.dir")));
			tmpFile.deleteOnExit();

			params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS.getParameterName(), 20);
			//set /P id="Enter token:" & call echo %^id%>C:\temp\tmp.out

			params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "ftp ftp.novell.com");
			//params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"ftp ftp.novell.com>> \""+tmpFile.getAbsolutePath()+"\"");
			//params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_COMMAND.getParameterName(), "cmd /C \"set /P id=\"\"Enter token:\"\"  \"");
			//No working dir configured.
			params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_WORKDIR.getParameterName(), "");
			params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_CONNECTURLTRIGGER.getParameterName(), "ftp>");
			params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_USERNAMETRIGGER.getParameterName(), "User (ftp.novell.com:(none)):");
			params.putParameter(ExternalExec.Parameters.EXTERNALEXEC_PASSWORDTRIGGER.getParameterName(), "Password:");
			final ExternalExec exec = new ExternalExec();
			final IDriver driver = mock(IDriver.class);
			final ISubscriberShim subscriber = mock(ISubscriberShim.class);
			when(subscriber.getConnectionInfo()).thenReturn(new ConnectionInfo("aPassword@me.com", "anonymous", "quit"));
			when(driver.getSubscriber()).thenReturn(subscriber);

			exec.init(trace,params,driver);
			//The file should not reside in the workdir this time!!!
			final File f = new File(workdir+File.separatorChar+"dummy.csv");
			final FileReader fr = new FileReader(tmpFile);
			final BufferedReader br = new BufferedReader(fr);
			exec.doPostProcess(f);
			assertEquals("aUserName ", br.readLine());
			assertEquals("aPassword ", br.readLine());
			assertEquals("aConnectURL", br.readLine());
			br.close();
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
}
