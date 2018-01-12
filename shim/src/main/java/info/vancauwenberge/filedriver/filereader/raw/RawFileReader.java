/*******************************************************************************
 * Copyright (c) 2007-2016 Stefaan Van Cauwenberge
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
package info.vancauwenberge.filedriver.filereader.raw;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.EnumConstraint;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;
import com.novell.xml.util.Base64Codec;

import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IFileReadStrategy;
import info.vancauwenberge.filedriver.exception.ReadException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.util.TraceLevel;
import info.vancauwenberge.filedriver.util.Util;

public class RawFileReader extends AbstractStrategy implements IFileReadStrategy {
	//Image properties after transformation
	protected static final String FIELD_RAW_DATA = "rawData";

	private Trace trace;
	/**
	 * ImageInputStream to the newly opened file.
	 * null if no file was opened or of the file was read.
	 */
	private FileInputStream inputStream=null;

	private boolean isBinary;

	private Charset encoding;

	private int maxFileSize;

	private long currentFileLength;

	protected enum Parameters implements IStrategyParameters{
		/**
		 * Binary or text raw data
		 */
		TYPE {
			@Override
			public String getParameterName() {
				return "rawReader_type";
			}

			@Override
			public String getDefaultValue() {
				return "BINARY";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}
			@Override
			public Constraint[] getConstraints(){
				final EnumConstraint cons = new EnumConstraint();
				cons.addLiteral("BINARY");
				cons.addLiteral("TEXT");
				return new Constraint[]{cons};
			}
		},
		/**
		 * Binary or text raw data
		 */
		MAX_SIZE {
			@Override
			public String getParameterName() {
				return "rawReader_maxSize";
			}

			@Override
			public String getDefaultValue() {
				return "";
			}

			@Override
			public DataType getDataType() {
				return DataType.INT;
			}
		},
		/**
		 * in case of text, file encoding. Empty equals platform default
		 */
		TEXT_ENCODING {
			@Override
			public String getParameterName() {
				return "rawReader_typeText_Encoding";
			}

			@Override
			public String getDefaultValue() {
				return "";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}
		},
		;

		@Override
		public abstract String getParameterName();

		@Override
		public abstract String getDefaultValue();

		@Override
		public abstract DataType getDataType();

		@Override
		public Constraint[] getConstraints() {
			return null;
		}
	}



	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#init(com.novell.nds.dirxml.driver.Trace, java.util.Map)
	 */
	@Override
	public void init(final Trace trace, final Map<String,Parameter> driverParams, final IPublisher publisher)
			throws XDSParameterException {
		if (trace.getTraceLevel()>TraceLevel.TRACE){
			trace.trace("ImageFileReader.init() driverParams:"+driverParams,TraceLevel.TRACE);
		}
		this.trace = trace;

		this.isBinary = "BINARY".equals(getStringValueFor(Parameters.TYPE, driverParams));
		if (!isBinary){
			final String encodingStr = getStringValueFor(Parameters.TEXT_ENCODING, driverParams);
			if (encodingStr.trim().equals("")){
				this.encoding = Charset.defaultCharset();
			}else{
				try{
					this.encoding = Charset.forName(encodingStr);
				}catch(final Exception e){
					trace.trace("ERROR: Invalid character encoding:"+encodingStr,TraceLevel.ERROR_WARN);
					Util.printStackTrace(trace, e);
					throw new XDSParameterException("Invalid character encoding");
				}
			}
		}

		this.maxFileSize = getIntValueFor(Parameters.MAX_SIZE, driverParams);
		if (maxFileSize<=0){
			maxFileSize = Integer.MAX_VALUE;
		}
	}

	private byte[] readBinary() throws ReadException {

		final byte[] buffer = new byte[(int)currentFileLength];
		try {
			if (inputStream.read(buffer) == -1) {
				throw new ReadException("Unexpected EOF reached while trying to read the whole file");
			}
		} catch (final IOException e) {
			Util.printStackTrace(trace, e);
			throw new ReadException(e);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (final IOException e) {
			}
			inputStream=null;
		}
		return buffer;
	}
	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	@Override
	public void openFile(final File f) throws ReadException {
		close();//Should not be needed.
		this.currentFileLength = f.length(); 
		if (currentFileLength > maxFileSize) {
			throw new ReadException("File is bigger than predefined maximum size:"+this.currentFileLength+">"+maxFileSize);
		}
		if (currentFileLength > (Integer.MAX_VALUE-8)) {//-8 to be safe: some vms store som header info in the array
			throw new ReadException("File is bigger than the maximum of "+(Integer.MAX_VALUE-8));
		}
		try {
			inputStream = new FileInputStream(f);
		} catch (final Exception e1) {
			trace.trace("Exception while reading raw file:" +e1.getMessage(), TraceLevel.ERROR_WARN);
			throw new ReadException("Exception while reading raw file:" +e1.getMessage(),e1);
		}
	}

	private String readFile() throws ReadException{

		if (isBinary){
			//Base64 encode it
			return new String(Base64Codec.encode(readBinary()));
		}else{
			//Now encode it a a string
			return new String(readBinary(),encoding);
		}

	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#readRecord(com.novell.nds.dirxml.driver.Trace)
	 */
	@Override
	public Map<String,String> readRecord() throws ReadException {

		if (inputStream != null){
			try {
				final Map<String,String> result = new HashMap<String, String>();
				result.put(FIELD_RAW_DATA, readFile());
				return result;
			} catch (final Exception e) {
				trace.trace("Exception while reading raw file:" +e.getMessage(), TraceLevel.ERROR_WARN);
				throw new ReadException(e);
			} finally {
				//Always close the file. Only one image is assumed.
				try{
					close();
				}catch(final Exception e){
					//We eat this exception if any. It was already traced by the close method. We only want to return the exception that happened during reading, not during closing.
				}
			}			
		}else{
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#close()
	 */
	@Override
	public void close() throws ReadException {
		if (inputStream!= null){
			try {
				inputStream.close();
				inputStream=null;
				currentFileLength=0;
			} catch (final IOException e1) {
				trace.trace("Exception while closing image stream:" +e1.getMessage(), TraceLevel.ERROR_WARN);
				throw new ReadException("Exception while closing image stream:" +e1.getMessage(),e1);
			}
		}
	}



	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<?> & IStrategyParameters> Class<E> getParametersEnum() {
		return (Class<E>) Parameters.class;
	}


	@Override
	public String[] getActualSchema() {
		return new String[]{FIELD_RAW_DATA};
	}


}