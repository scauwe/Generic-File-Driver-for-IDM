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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.RangeConstraint;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.api.IPostProcessStrategy;
import info.vancauwenberge.filedriver.filereader.csv.CSVFileParser;
import info.vancauwenberge.filedriver.shim.ConnectionInfo;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

public class ExternalExec extends AbstractStrategy implements IPostProcessStrategy {

	private class LineScanner{
		private final String stringToMatch;
		private final int stringToMatchLen;
		private int currentIndex;

		public LineScanner(final String stringToMatch){
			this.stringToMatchLen = stringToMatch==null?-1:stringToMatch.length();
			this.stringToMatch = stringToMatch;
			currentIndex=0;
		}

		/**
		 * Test if thisChar matches the next char in triggerString.
		 * If as a result the complete string is matched, true is returned, otherwise false.
		 * @param thisChar
		 * @return true of a match was found, false otherwise
		 * @throws IOException
		 */
		public boolean nextChar(final char thisChar) throws IOException {
			if (stringToMatch != null){
				if (thisChar == stringToMatch.charAt(currentIndex)) {
					currentIndex++;
				} else {
					currentIndex=0;
				}
			}
			if (currentIndex == stringToMatchLen){
				currentIndex=0;//Start over again
				return true;
			}
			return false;
		}
	}
	enum Parameters implements IStrategyParameters{
		/**
		 * Close the file after <i>nnn</i> seconds of inactivity
		 */
		EXTERNALEXEC_MAXWAITTIMESECONDS{
			@Override
			public String getParameterName() {
				return "externalExec_maxWaitTimeSeconds";
			}

			@Override
			public String getDefaultValue() {
				return "0";
			}

			@Override
			public DataType getDataType() {
				return DataType.INT;
			}

			@Override
			public Constraint[] getConstraints() {
				//Divide by 100 because we need to multiply to get millis
				return new Constraint[]{new RangeConstraint(0, (Integer.MAX_VALUE/1000))};
			}
		},
		/**
		 * Use given cron string to close the file 
		 */
		EXTERNALEXEC_WORKDIR{
			@Override
			public String getParameterName() {
				return "externalExec_workDir";
			}

			@Override
			public String getDefaultValue() {
				return "";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}

			@Override
			public Constraint[] getConstraints() {
				return null;
			}
		},
		/**
		 * Field to manually indicate that a new file should be started or not
		 */
		EXTERNALEXEC_COMMAND{
			@Override
			public String getParameterName() {
				return "externalExec_command";
			}

			@Override
			public String getDefaultValue() {
				return "";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}

			@Override
			public Constraint[] getConstraints() {
				return null;
			}
		},
		/**
		 * Field to manually indicate that a new file should be started or not
		 */
		EXTERNALEXEC_USERNAMETRIGGER{
			@Override
			public String getParameterName() {
				return "externalExec_trigger_username";
			}

			@Override
			public String getDefaultValue() {
				return "";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}

			@Override
			public Constraint[] getConstraints() {
				return null;
			}
		},
		/**
		 * Field to manually indicate that a new file should be started or not
		 */
		EXTERNALEXEC_PASSWORDTRIGGER{
			@Override
			public String getParameterName() {
				return "externalExec_trigger_password";
			}

			@Override
			public String getDefaultValue() {
				return "";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}

			@Override
			public Constraint[] getConstraints() {
				return null;
			}
		},
		/**
		 * Field to manually indicate that a new file should be started or not
		 */
		EXTERNALEXEC_CONNECTURLTRIGGER{
			@Override
			public String getParameterName() {
				return "externalExec_trigger_connectURL";
			}

			@Override
			public String getDefaultValue() {
				return "";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}

			@Override
			public Constraint[] getConstraints() {
				return null;
			}
		};

		@Override
		public abstract String getParameterName();
		@Override
		public abstract String getDefaultValue();
		@Override
		public abstract DataType getDataType();
		@Override
		public abstract Constraint[] getConstraints();

	}

	private ProcessBuilder pb=null;
	private List<String> commandAndParams=null;
	private Trace trace;
	private int maxWaitTime;
	private ConnectionInfo connectioninfo;
	private String userNameTriggerLine;
	private String passwordTriggerLine;
	private String connectURLTriggerLine;

	@Override
	public void init(final Trace trace, final Map<String, Parameter> driverParams, final IDriver driver) throws Exception {
		this.trace = trace;
		this.connectioninfo = driver.getSubscriber().getConnectionInfo();
		this.maxWaitTime = getIntValueFor(Parameters.EXTERNALEXEC_MAXWAITTIMESECONDS, driverParams);
		final String command = getStringValueFor(Parameters.EXTERNALEXEC_COMMAND,driverParams);
		if (!"".equals(command)){
			commandAndParams = new ArrayList<>();
			//Parse the command as a CSV line with space as a separator
			final CSVFileParser parser = new CSVFileParser(' ', new String[]{}, true, false, false);
			parser.resetParser();
			parser.doParse(new StringReader(command));
			final Map<String,String> fields = parser.getQueue().getNextRecord();
			//loop over the fields and get the values
			for (int i = 0; i < fields.size(); i++) {
				final String value = fields.get("field"+i);
				if (! "".equals(value)) {
					commandAndParams.add(value);
				}
			}
			pb = new ProcessBuilder();
			pb.redirectErrorStream(true);
			final String workDir = getStringValueFor(Parameters.EXTERNALEXEC_WORKDIR,driverParams);
			if (!"".equals(workDir)) {
				final File workdirFile = new File(workDir);
				if (!workdirFile.isDirectory()) {
					throw new IllegalArgumentException("The workdirectory is not a directory.");
				}
				pb.directory(workdirFile);
			}else{
				//On the actual exec, it will be set to the parent of the output file (=the output folder)
			}
			//Get the trigger lines (if any)
			this.userNameTriggerLine = getStringValueFor(Parameters.EXTERNALEXEC_USERNAMETRIGGER,driverParams);
			if ((userNameTriggerLine != null) && userNameTriggerLine.equals("")) {
				userNameTriggerLine = null;
			}
			this.passwordTriggerLine = getStringValueFor(Parameters.EXTERNALEXEC_PASSWORDTRIGGER,driverParams);
			if ((passwordTriggerLine != null) && passwordTriggerLine.equals("")) {
				passwordTriggerLine = null;
			}
			this.connectURLTriggerLine = getStringValueFor(Parameters.EXTERNALEXEC_CONNECTURLTRIGGER,driverParams);
			if ((connectURLTriggerLine != null) && connectURLTriggerLine.equals("")) {
				connectURLTriggerLine = null;
			}

			if (trace.getTraceLevel()>=TraceLevel.TRACE){
				trace.trace("Initialization completed:", TraceLevel.TRACE);
				trace.trace("Post processes environment: "+pb.environment(), TraceLevel.TRACE);
				trace.trace("Post processes command and arguments: "+commandAndParams, TraceLevel.TRACE);
				trace.trace("Post processes workDir: "+workDir, TraceLevel.TRACE);
				trace.trace("Post processes max process wait time: "+maxWaitTime, TraceLevel.TRACE);
			}
		}
	}

	@Override
	public void doPostProcess(final File result) {
		try{
			if (pb != null){
				//Clone the commandAndArguments and search replace the tokens
				//$PARENTPATH$ C:\temp
				//$FILENAME$   out.csv
				//$FILEPATH$   C:\temp\out.csv
				//$CONNECTUSER$ userName
				//$CONNECTPASSWORD$   password
				//$CONNECTURL$   URL
				final String fileName = result.getName();//Name only
				final String filePath = result.getCanonicalPath();//Full path (folder+name)
				final File parentPathFile = result.getParentFile();
				final String parentPath = parentPathFile.getCanonicalPath();//Name only

				final List<String> commands = new ArrayList<>(commandAndParams.size());
				for (final String aCmd : commandAndParams) {
					String thisValue = aCmd.replace("$PARENTPATH$", parentPath)
							.replace("$FILENAME$", fileName)
							.replace("$FILEPATH$", filePath);
					if (connectioninfo != null) {
						thisValue = thisValue.replace("$CONNECTUSER$", connectioninfo.getUserName())
								.replace("$CONNECTPASSWORD$", connectioninfo.getPassword())
								.replace("$CONNECTURL$", connectioninfo.getConnectURL());
					}
					commands.add(thisValue);
				}
				if (trace.getTraceLevel()>=TraceLevel.TRACE){
					trace.trace("Command and arguments:"+commands, TraceLevel.TRACE);
				}
				//Set the working directory if not yet set
				if (pb.directory()==null) {
					pb.directory(parentPathFile);
				}
				pb.command(commands);
				trace.trace("Process starting", TraceLevel.TRACE);
				final Process process = pb.start();

				//Read the result in a different thread: it is blocking!!!
				final Thread th = new Thread("postProcessReader"){
					@Override
					public void run() {
						try{
							//trace.trace("Start reading");
							final OutputStream os = process.getOutputStream();
							final OutputStreamWriter wr = new OutputStreamWriter(os);
							//Even if not tracing, we need to clear the buffer or the process might lock
							final InputStream is = process.getInputStream();
							final InputStreamReader isr = new InputStreamReader(is);
							final char[] buffer = new char[1024];
							int numCharsRead;

							final StringBuilder aLine = new StringBuilder();
							final LineScanner userNameScanner = new LineScanner(userNameTriggerLine);
							final LineScanner passwordScanner = new LineScanner(passwordTriggerLine);
							final LineScanner urlScanner = new LineScanner(connectURLTriggerLine);
							while((numCharsRead = isr.read(buffer)) > 0) {
								for (int c = 0; c < numCharsRead; c++) {
									final char thisChar = buffer[c];
									//System.out.println(thisChar);
									String stringToWrite=null;
									if (userNameScanner.nextChar(thisChar)){
										stringToWrite=connectioninfo.getUserName();
									}
									if (passwordScanner.nextChar(thisChar)){
										stringToWrite=connectioninfo.getPassword();
									}
									if (urlScanner.nextChar(thisChar)){
										stringToWrite=connectioninfo.getConnectURL();
									}
									if (stringToWrite != null){
										wr.write(stringToWrite );
										wr.write("\n");
										wr.flush();//We need to flush!!!!
										//Trace
										aLine.append(thisChar);
										trace.trace(aLine + stringToWrite , TraceLevel.TRACE);
										aLine.setLength(0);
									}else if (thisChar=='\n'){
										//Trace
										trace.trace(aLine.toString(), TraceLevel.TRACE);
										aLine.setLength(0);
									} else {
										//Trace
										aLine.append(thisChar);
									}
								}
							}
							trace.trace(aLine.toString(), TraceLevel.TRACE);
							isr.close();
							wr.close();
						}catch(final IOException e){
							Util.printStackTrace(trace, e);
						}
					}
				};
				th.start();
				//waitFor with timeOut is only since java 1.8
				final boolean terminated = waitFor18(process,maxWaitTime);
				if (terminated){
					trace.trace("Process terminated with exit value "+process.exitValue(), TraceLevel.TRACE);
				}else{
					trace.trace("WARN: process did not finish after "+maxWaitTime+" seconds.", TraceLevel.ERROR_WARN);
					trace.trace("If this happens to often, you might get a cumulative effect. To prevent this, increase the max wait time.", TraceLevel.ERROR_WARN);
				}
			}
		}catch(final Exception e){
			trace.trace("Unhandled exception while executing the external postprocessing command for file "+result, TraceLevel.ERROR_WARN);
			Util.printStackTrace(trace, e);
		}
	}


	/**
	 * Process.waitFor is only implemented since java 1.8
	 * For pre-java-8 compatibility, we do something ourself
	 * @param proces
	 * @param timeout
	 * @return
	 * @throws InterruptedException
	 */
	private static boolean waitFor18(final Process proces, final long timeout) throws InterruptedException{
		final long startTime = System.nanoTime();
		long rem = TimeUnit.SECONDS.toNanos(timeout);

		do {
			try {
				proces.exitValue();
				return true;
			} catch(final IllegalThreadStateException ex) {
				//System.out.println(rem);
				if (rem > 0) {
					Thread.sleep(
							Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 200));
				}
			}
			rem = TimeUnit.SECONDS.toNanos(timeout) - (System.nanoTime() - startTime);
		} while (rem > 0);
		return false;
	}



	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}

}
