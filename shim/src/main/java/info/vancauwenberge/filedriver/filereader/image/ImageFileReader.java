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
package info.vancauwenberge.filedriver.filereader.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTranscoder;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.xds.Constraint;
import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.EnumConstraint;
import com.novell.nds.dirxml.driver.xds.Parameter;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;
import com.novell.xml.util.Base64Codec;

import info.vancauwenberge.filedriver.SysoutTrace;
import info.vancauwenberge.filedriver.api.AbstractStrategy;
import info.vancauwenberge.filedriver.api.IFileReadStrategy;
import info.vancauwenberge.filedriver.exception.ReadException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;
import info.vancauwenberge.filedriver.util.TraceLevel;

public class ImageFileReader extends AbstractStrategy implements IFileReadStrategy {
	//Image properties after transformation
	protected static final String FIELD_IMAGE_BYTES = "imageBytes";
	protected static final String FIELD_IMAGE_HEIGHT = "imageHeight";
	protected static final String FIELD_IMAGE_WIDTH = "imageWidth";
	protected static final String FIELD_IMAGE_FORMAT = "imageFormat";
	//Image properties before transformation
	protected static final String FIELD_SRC_HEIGHT = "srcHeight";
	protected static final String FIELD_SRC_WIDTH = "srcWidth";
	protected static final String FIELD_SRC_FORMAT = "srcFormat";

	private Trace trace;
	private boolean resize;
	private int resizeHeight;
	private int resizeWidth;
	private boolean transcode;
	private String transcodeTargetFormat;
	///private final Encoder base64Encoder = Base64.getEncoder();
	//Base64Codec base64Encoder = new Base64Codec();
	/**
	 * ImageInputStream to the newly opened file.
	 * null if no file was opened or of the file was read.
	 */
	private ImageInputStream inputStream=null;
	private boolean includeImageMeta;
	private Color paddingColor;
	private boolean resizeExact; 

	protected enum Parameters implements IStrategyParameters{
		/**
		 * Should the image be resized or not
		 */
		/*RESIZE_STRUCT {
			@Override
			public String getParameterName() {
				return "imgReader_resize_struct";
			}

			@Override
			public String getDefaultValue() {
				return "default";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRUCT;
			}
		},*/
		/**
		 * Should the image be resized or not
		 */
		RESIZE {
			@Override
			public String getParameterName() {
				return "imgReader_resize";
			}

			@Override
			public String getDefaultValue() {
				return "false";
			}

			@Override
			public DataType getDataType() {
				return DataType.BOOLEAN;
			}
		},
		/**
		 * Should the image be transcoded or not.
		 * Note: transcoding is not supported for every file type, color model etc.
		 * Especially transcoding to jpg results in issues.
		 * Transcoding will also remove any image metadata, since this is not transcoded by default.
		 */
		TRANSCODE {
			@Override
			public String getParameterName() {
				return "imgReader_transcode";
			}

			@Override
			public String getDefaultValue() {
				return "false";
			}

			@Override
			public DataType getDataType() {
				return DataType.BOOLEAN;
			}
		},
		/**
		 * If transcoding, the expected format.
		 */
		TRANSCODE_FORMAT {
			@Override
			public String getParameterName() {
				return "imgReader_transcodeFormat";
			}

			@Override
			public String getDefaultValue() {
				return "png";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}
			@Override
			public Constraint[] getConstraints(){
				final EnumConstraint cons = new EnumConstraint(String.CASE_INSENSITIVE_ORDER);
				cons.addLiterals(ImageIO.getWriterFormatNames());
				return new Constraint[]{cons};
			}
		},
		/**
		 * If resizing, the expected height. Leave '0' to calculate the width keeping aspect ratio.
		 */
		RESIZE_HEIGTH {
			@Override
			public String getParameterName() {
				return "imgReader_resizeY";
			}

			@Override
			public String getDefaultValue() {
				return "0";
			}

			@Override
			public DataType getDataType() {
				return DataType.INT;
			}
		},
		/**
		 * If resizing, the expected width. Leave '0' to calculate the width keeping aspect ratio.
		 */
		RESIZE_WIDTH {
			@Override
			public String getParameterName() {
				return "imgReader_resizeX";
			}

			@Override
			public String getDefaultValue() {
				return "0";
			}

			@Override
			public DataType getDataType() {
				return DataType.INT ;
			}
		},
		/**
		 * If resizing, the mode: EXACT(exact width & height) or DYNAMIC(keep aspect ratio).
		 */
		RESIZE_RESIZE_MODE {
			@Override
			public String getParameterName() {
				return "imgReader_resizeMode";
			}

			@Override
			public String getDefaultValue() {
				return "DYNAMIC";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}
			@Override
			public Constraint[] getConstraints(){
				final EnumConstraint cons = new EnumConstraint();
				cons.addLiteral("DYNAMIC");
				cons.addLiteral("EXACT");
				return new Constraint[]{cons};
			}
		},
		/**
		 * If resizing, the mode: EXACT(exact width & height) or DYNAMIC(keep aspect ratio).
		 */
		RESIZE_RESIZE_PADDING {
			@Override
			public String getParameterName() {
				return "imgReader_resizePadding";
			}

			@Override
			public String getDefaultValue() {
				return "NOPADDING";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}
			@Override
			public Constraint[] getConstraints(){
				final EnumConstraint cons = new EnumConstraint();
				cons.addLiteral("PADDING");
				cons.addLiteral("NOPADDING");
				return new Constraint[]{cons};
			}
		},
		/**
		 * If resizing, the mode: EXACT(exact width & height) or DYNAMIC(keep aspect ratio).
		 */
		RESIZE_RESIZE_PADDING_COLOR {
			@Override
			public String getParameterName() {
				return "imgReader_resizePaddingColor";
			}

			@Override
			public String getDefaultValue() {
				return "FFFFFF";
			}

			@Override
			public DataType getDataType() {
				return DataType.STRING;
			}
		},
		/**
		 * Should the image meta data be added (if any)
		 */
		IMAGE_META {
			@Override
			public String getParameterName() {
				return "imgReader_includeMeta";
			}

			@Override
			public String getDefaultValue() {
				return "false";
			}

			@Override
			public DataType getDataType() {
				return DataType.BOOLEAN;
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

		this.includeImageMeta = getBoolValueFor(Parameters.IMAGE_META, driverParams);

		this.resize = getBoolValueFor(Parameters.RESIZE,driverParams);
		if (this.resize){
			this.resizeHeight = Math.max(0, getIntValueFor(Parameters.RESIZE_HEIGTH, driverParams));
			this.resizeWidth = Math.max(0, getIntValueFor(Parameters.RESIZE_WIDTH, driverParams));
		}
		//Validate the height and width parameters. They cannot both be <=0
		if ((this.resize) && (this.resizeHeight==0) && (this.resizeWidth==0)){
			trace.trace("WARN: Both width and height are <=0. Disabling resizing",TraceLevel.ERROR_WARN);
			this.resize=false;
		}
		if (this.resize){
			this.resizeExact = "EXACT".equals(getStringValueFor(Parameters.RESIZE_RESIZE_MODE, driverParams));
			if (resizeExact){
				//Height and width should be > 0
				if ((this.resizeHeight==0) || (this.resizeWidth==0)){
					trace.trace("WARN: Width or height are <=0. Changing resize mode to from 'EXACT' to 'Max dimensions(DYNAMIC)'",TraceLevel.ERROR_WARN);
					this.resizeExact=false;
				}
			}
			if (!resizeExact){//In dynamic mode
				final boolean doPadding = "PADDING".equals(getStringValueFor(Parameters.RESIZE_RESIZE_PADDING, driverParams));
				if (doPadding){
					final String colorStr = getStringValueFor(Parameters.RESIZE_RESIZE_PADDING_COLOR, driverParams);
					if (colorStr != null){
						final String trimmedColor = colorStr.trim();
						if (trimmedColor.length()==6){
							final String colorR = trimmedColor.substring(0, 2);
							final String colorG = trimmedColor.substring(2, 4);
							final String colorB = trimmedColor.substring(4, 6);
							this.paddingColor = new Color(Integer.parseInt(colorR,16),Integer.parseInt(colorG,16),Integer.parseInt(colorB,16));
						}
					}
					if (this.paddingColor==null){
						trace.trace("WARN: Invalid padding color:"+colorStr+". Falling black to 000000 (black)",TraceLevel.ERROR_WARN);					
						this.paddingColor = new Color(0,0,0);
					}
				}
			}
		}
		this.transcode = getBoolValueFor(Parameters.TRANSCODE, driverParams);
		if (this.transcode){
			this.transcodeTargetFormat = getStringValueFor(Parameters.TRANSCODE_FORMAT, driverParams);
		}
	}


	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#openFile(com.novell.nds.dirxml.driver.Trace, java.io.File)
	 */
	@Override
	public void openFile(final File f) throws ReadException {

		try {
			inputStream = ImageIO.createImageInputStream(f);
		} catch (final Exception e1) {
			trace.trace("Exception while reading image:" +e1.getMessage(), TraceLevel.ERROR_WARN);
			throw new ReadException("Exception while reading image:" +e1.getMessage(),e1);
		}
	}

	/* (non-Javadoc)
	 * @see info.vancauwenberge.filedriver.api.IFileReader#readRecord(com.novell.nds.dirxml.driver.Trace)
	 */
	@Override
	public Map<String,String> readRecord() throws ReadException {

		if (inputStream != null){
			try {
				final Map<String, String> result =  getImageDataMap();
				return result;
			} catch (final NoSuchElementException e) {
				trace.trace("Unsupported image type:" +e.getMessage(), TraceLevel.ERROR_WARN);
				throw new ReadException("Unsupported image type.",e);
			} catch (final IOException e) {
				trace.trace("IOException while reading image:" +e.getMessage(), TraceLevel.ERROR_WARN);
				throw new ReadException("IOException while reading image.",e);
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
		//if (resize)
		return new String[]{FIELD_IMAGE_BYTES,FIELD_IMAGE_HEIGHT,FIELD_IMAGE_WIDTH,FIELD_IMAGE_FORMAT,FIELD_SRC_FORMAT,FIELD_SRC_HEIGHT,FIELD_SRC_WIDTH};
		//else
		//	return new String[]{FIELD_IMAGE_BYTES,FIELD_IMG_HEIGHT,FIELD_IMG_WIDTH};
	}

	private BufferedImage resize(final BufferedImage image, final int width, final int height, final ImageTypeSpecifier sourceType){
		final Image tmp = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		final BufferedImage resized ;
		if (paddingColor != null){
			//We need to pad. The canvas size is resizeWidth and Height
			resized = sourceType.createBufferedImage(resizeWidth, resizeHeight);			
		} else {
			resized = sourceType.createBufferedImage(width, height);
		}
		//final BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = resized.createGraphics();
		if (paddingColor != null){
			g2d.setBackground(paddingColor);//The color used to draw the transparency of the image
			g2d.setColor(paddingColor);//The color used to draw the rectangle
			g2d.fillRect(0, 0, resizeWidth, resizeHeight);
			g2d.drawImage(tmp, (resizeWidth-width)/2, (resizeHeight-height)/2, paddingColor, null);//No need for an observer since the image is already buffered
		}else{
			g2d.drawImage(tmp, 0, 0, null, null);//No need for an observer since the image is already buffered
		}
		g2d.dispose();
		return resized;
	}


	private ImageTranscoder getTranscoder(final ImageReader imgReader, final Iterator<ImageWriter> writerIter){
		while (writerIter.hasNext()) {
			final ImageWriter aWriter = writerIter.next();
			final Iterator<ImageTranscoder> transcoders = ImageIO.getImageTranscoders(imgReader, aWriter);
			if (transcoders.hasNext()){
				trace.trace("Transcoder found.");
				return transcoders.next();
			}
		}
		return null;
	}

	private ImageTranscoder getMetaDataTranscoder(final ImageReader imgReader, final ImageTypeSpecifier imageSrcDestType) throws IOException, ReadException{
		if (transcodeTargetFormat==null) {
			return null;
		}
		final Iterator<ImageWriter> writers = ImageIO.getImageWriters(imageSrcDestType, transcodeTargetFormat);
		//First try to get a transcoder
		ImageTranscoder transcoder = getTranscoder(imgReader, writers);
		if (transcoder==null){
			final Iterator<ImageWriter> writers2 = ImageIO.getImageWritersByFormatName(transcodeTargetFormat);
			transcoder = getTranscoder(imgReader, writers2);
		}
		return transcoder;
	}

	/**
	 * 
	 * @param f
	 * @return
	 * @throws ReadException 
	 * @throws IOException if the image file type is not supported
	 */
	private Map<String,String> getImageDataMap() throws ReadException, IOException {
		final Map<String,String> result = new HashMap<String, String>();

		final ImageReader imgReader = ImageIO.getImageReaders(inputStream).next();
		trace.trace("Reading image of type:"+imgReader.getFormatName());
		result.put(FIELD_SRC_FORMAT, imgReader.getFormatName());

		imgReader.setInput(inputStream, false, false);
		ImageTypeSpecifier imageSrcDestType = imgReader.getRawImageType(0);
		if (imageSrcDestType==null){
			imageSrcDestType = imgReader.getImageTypes(0).next();
		}
		final ImageReadParam imageReadParams = imgReader.getDefaultReadParam();
		imageReadParams.setDestinationType(imageSrcDestType);

		final BufferedImage img = imgReader.read(0, imageReadParams);
		final int imgWidth = imgReader.getWidth(0);
		final int imgHeight = imgReader.getHeight(0);
		result.put(FIELD_SRC_WIDTH, Integer.toString(imgWidth));
		result.put(FIELD_SRC_HEIGHT, Integer.toString(imgHeight));

		final BufferedImage scaledImage;
		if (resize){
			//Calculate the new size
			int desiredWidth=resizeWidth;
			int desiredHeight=resizeHeight;				
			if (!resizeExact){
				if (desiredWidth==0){
					//Calculate the width
					desiredWidth = (imgWidth*desiredHeight)/imgHeight;
				}else if (desiredHeight==0){
					desiredHeight = (imgHeight*desiredWidth)/imgWidth;
				}else{
					//Max bounds
					final float ratioX = (float)desiredWidth / imgWidth;
					final float ratioY = (float)desiredHeight / imgHeight;
					if (ratioX<=ratioY){
						//take the smallest one, so update width to ratio of height
						desiredHeight = (int)(imgHeight*ratioX);
					}else{
						desiredWidth = (int)(imgWidth*ratioY);						
					}
				}

			}
			if (paddingColor==null){//No padding: wet the calculated image dimentions
				result.put(FIELD_IMAGE_WIDTH, Integer.toString(desiredWidth));
				result.put(FIELD_IMAGE_HEIGHT, Integer.toString(desiredHeight));
			}else{//With padding: wet the canvas dimantions
				result.put(FIELD_IMAGE_WIDTH, Integer.toString(resizeWidth));
				result.put(FIELD_IMAGE_HEIGHT, Integer.toString(resizeHeight));				
			}
			scaledImage = resize(img, desiredWidth,desiredHeight, imageSrcDestType);
		}else{
			//No resizing
			result.put(FIELD_IMAGE_WIDTH, Integer.toString(imgWidth));
			result.put(FIELD_IMAGE_HEIGHT, Integer.toString(imgHeight));
			scaledImage = img;
		}

		//Get the image writer. If transcoding, get a specific one, otherwise get the one corresponding to the input
		final ImageWriter imgWriter;
		if (transcode){
			final Iterator<ImageWriter> writers = ImageIO.getImageWriters(imageSrcDestType, transcodeTargetFormat);
			if (writers.hasNext()) {
				imgWriter = writers.next();
			} else {
				final Iterator<ImageWriter> writers2 = ImageIO.getImageWritersByFormatName(transcodeTargetFormat);
				if (writers2.hasNext()) {
					imgWriter = writers2.next();
				}else{
					throw new ReadException("No image writer found for format "+transcodeTargetFormat);
				}
			}
			result.put(FIELD_IMAGE_FORMAT, transcodeTargetFormat);
		}
		else{
			//Get the writer corresponding to this reader
			imgWriter = ImageIO.getImageWriter(imgReader);
			result.put(FIELD_IMAGE_FORMAT, imgReader.getFormatName());
		}

		//Now write the image to a byte array

		/*		String destFileName = f.getAbsolutePath();
		final String extention = destFileName.substring(destFileName.lastIndexOf('.'));
		destFileName=destFileName.substring(0, destFileName.lastIndexOf('.'))+"resized"+extention+"."+targetFormat;
		final FileOutputStream baos = new FileOutputStream(new File(destFileName));*/
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		final ImageOutputStream imageos = ImageIO.createImageOutputStream(baos);
		imgWriter.setOutput(imageos);

		final IIOMetadata srcImageMetaData = imgReader.getImageMetadata(0);
		//		 * Reading the metadata does not yet work. I seem unable to get it...

		if (includeImageMeta && srcImageMetaData.isStandardMetadataFormatSupported()){
			final Element metaTree = (Element) srcImageMetaData.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);//"javax_imageio_jpeg_image_1.0");//
			printSubtree(metaTree,0);
			/*
			final NodeList textElements = ((Element) metaTree.getFirstChild()).getElementsByTagName("Text");
			if (textElements != null){
				for (int i = 0; i < textElements.getLength(); i++) {
					final Element aTextNode = (Element) textElements.item(i);
					final NodeList textElementEntries = aTextNode.getElementsByTagName("TextEntry");
					if (textElementEntries != null){
						for (int j = 0; j < textElementEntries.getLength(); j++) {
							final Node node = textElementEntries.item(j);
							final String keyword = node.getAttributes().getNamedItem("keyword").getNodeValue();
							if (keyword==null){
								trace.trace("Not including meta parameter that has no keyword");
							}else{
								final String value = node.getAttributes().getNamedItem("value").getNodeValue();
								result.put("_meta_"+keyword, value);
							}
						}
					}
				}
			}*/
		}
		final ImageWriteParam writeParams = imgWriter.getDefaultWriteParam();
		//writeParams.setDestinationType(imageSrcDestType);
		writeParams.setDestinationType(ImageTypeSpecifier.createFromRenderedImage(scaledImage));
		final ImageTranscoder metaDataTranscoder = getMetaDataTranscoder(imgReader, imageSrcDestType);
		final IIOMetadata destImageMetaData;
		if (metaDataTranscoder != null){
			destImageMetaData = metaDataTranscoder.convertImageMetadata(srcImageMetaData, imageSrcDestType, writeParams);
		}else{
			destImageMetaData = srcImageMetaData;
		}
		final IIOImage image = new IIOImage(scaledImage, null, destImageMetaData);
		imgWriter.write(destImageMetaData, image, writeParams);

		imageos.close();
		baos.flush();
		baos.close();
		imgWriter.dispose();
		imgReader.dispose();

		final byte[] imgBytes = baos.toByteArray();
		result.put(FIELD_IMAGE_BYTES, new String(Base64Codec.encode(imgBytes)));

		return result;
	}

	private void printSubtree(final Element metaTree, final int i) {
		// TODO Auto-generated method stub
		for (int j = 0; j < i; j++) {
			System.out.print("  ");
		}
		System.out.println(metaTree.getTagName());
		final NodeList children = metaTree.getChildNodes();
		for (int j = 0; j < children.getLength(); j++) {
			printSubtree((Element) children.item(j), i+1);
		}
	}


	public static void main(final String[] args){
		Trace.registerImpl(SysoutTrace.class, 0);
		final ImageFileReader ifr = new ImageFileReader();
		ifr.resize=true;
		ifr.resizeHeight=0;
		ifr.resizeWidth=100;
		ifr.transcode=true;
		ifr.transcodeTargetFormat="jpg";
		ifr.trace=new Trace(">");
		try {
			ifr.openFile(new File(args[0]));
			final Map<String, String> record = ifr.readRecord();
			System.out.println(record);
			ifr.close();
			OutputStream out = null;
			try {
				out = new BufferedOutputStream(new FileOutputStream(args[1]));
				final byte[] bytes = Base64.getDecoder().decode(record.get(FIELD_IMAGE_BYTES));
				out.write(bytes);
			} catch (final Exception e) {
				e.printStackTrace();
			} finally {
				if (out != null) {
					out.close();
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}


	}
}