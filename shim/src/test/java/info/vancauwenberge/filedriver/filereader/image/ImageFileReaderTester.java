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
package info.vancauwenberge.filedriver.filereader.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;
import org.mockito.Mock;

import com.novell.nds.dirxml.driver.Trace;

import info.vancauwenberge.filedriver.AbstractStrategyTest;
import info.vancauwenberge.filedriver.ParamMap;
import info.vancauwenberge.filedriver.api.IDriver;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;

public class ImageFileReaderTester extends AbstractStrategyTest{
	private static final Decoder decoder = Base64.getDecoder();
	//private static final Encoder encoder = Base64.getEncoder();
	private static byte[] BMP_IMG = decoder.decode("Qk12AQAAAAAAADYAAAAoAAAACgAAAAoAAAABABgAAAAAAEABAADEDgAAxA4AAAAAAAAAAAAA////////AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD///////8AAAAAAAAAAAAAAAAAAAD///////////8AAP///////wAAAAAAAAAAAAAAAAAAAP///////////wAA////////AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAkHO0kHO0AAADMSD/MSD/MSD/MSD/MSD/MSD/MSD8AACQc7SQc7QAAAMxIP8xIP8xIP8xIP8xIP8xIP8xIPwAAJBztJBztAAAAzEg/zEg/zEg/zEg/zEg/zEg/zEg/AAAkHO0kHO0AAADMSD/MSD/MSD/MSD/MSD/MSD/MSD8AACQc7SQc7SQc7cxIP8xIP8xIP8xIP8xIP8xIP8xIPwAAJBztJBztJBztzEg/zEg/zEg/zEg/zEg/zEg/zEg/AAA=");
	private static byte[] GIF_IMG = decoder.decode("R0lGODlhCgAKAPcAAAAAAAAAMwAAZgAAmQAAzAAA/wArAAArMwArZgArmQArzAAr/wBVAABVMwBVZgBVmQBVzABV/wCAAACAMwCAZgCAmQCAzACA/wCqAACqMwCqZgCqmQCqzACq/wDVAADVMwDVZgDVmQDVzADV/wD/AAD/MwD/ZgD/mQD/zAD//zMAADMAMzMAZjMAmTMAzDMA/zMrADMrMzMrZjMrmTMrzDMr/zNVADNVMzNVZjNVmTNVzDNV/zOAADOAMzOAZjOAmTOAzDOA/zOqADOqMzOqZjOqmTOqzDOq/zPVADPVMzPVZjPVmTPVzDPV/zP/ADP/MzP/ZjP/mTP/zDP//2YAAGYAM2YAZmYAmWYAzGYA/2YrAGYrM2YrZmYrmWYrzGYr/2ZVAGZVM2ZVZmZVmWZVzGZV/2aAAGaAM2aAZmaAmWaAzGaA/2aqAGaqM2aqZmaqmWaqzGaq/2bVAGbVM2bVZmbVmWbVzGbV/2b/AGb/M2b/Zmb/mWb/zGb//5kAAJkAM5kAZpkAmZkAzJkA/5krAJkrM5krZpkrmZkrzJkr/5lVAJlVM5lVZplVmZlVzJlV/5mAAJmAM5mAZpmAmZmAzJmA/5mqAJmqM5mqZpmqmZmqzJmq/5nVAJnVM5nVZpnVmZnVzJnV/5n/AJn/M5n/Zpn/mZn/zJn//8wAAMwAM8wAZswAmcwAzMwA/8wrAMwrM8wrZswrmcwrzMwr/8xVAMxVM8xVZsxVmcxVzMxV/8yAAMyAM8yAZsyAmcyAzMyA/8yqAMyqM8yqZsyqmcyqzMyq/8zVAMzVM8zVZszVmczVzMzV/8z/AMz/M8z/Zsz/mcz/zMz///8AAP8AM/8AZv8Amf8AzP8A//8rAP8rM/8rZv8rmf8rzP8r//9VAP9VM/9VZv9Vmf9VzP9V//+AAP+AM/+AZv+Amf+AzP+A//+qAP+qM/+qZv+qmf+qzP+q///VAP/VM//VZv/Vmf/VzP/V////AP//M///Zv//mf//zP///wAAAAAAAAAAAAAAACH5BAEAAPwALAAAAAAKAAoAAAhOAKW9gtXFiw4dNA5me2VozJiEZBJKg6Xi4UGDOqa9CpAjYkSEr16pyJHwIBkdrwoFGJODZMsc+vYBmBkAQM0A+2TOnJmz506ePXX+nBkQADs=");
	private static byte[] PNG_IMG = decoder.decode("iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsQAAA7EAZUrDhsAAAAxSURBVChTY3gro/Ifhu09zuDEpCtkYGDAqgCGB1LhfygAKcSLoeqwSiID6ihEYIb/AIv31YddzQXiAAAAAElFTkSuQmCC");
	private static byte[] JPG_IMG = decoder.decode("/9j/4AAQSkZJRgABAQEAYABgAAD/4QBaRXhpZgAATU0AKgAAAAgABQMBAAUAAAABAAAASgMDAAEAAAABAAAAAFEQAAEAAAABAQAAAFERAAQAAAABAAAOxFESAAQAAAABAAAOxAAAAAAAAYagAACxj//bAEMAAgEBAgEBAgICAgICAgIDBQMDAwMDBgQEAwUHBgcHBwYHBwgJCwkICAoIBwcKDQoKCwwMDAwHCQ4PDQwOCwwMDP/bAEMBAgICAwMDBgMDBgwIBwgMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDP/AABEIAAoACgMBIgACEQEDEQH/xAAfAAABBQEBAQEBAQAAAAAAAAAAAQIDBAUGBwgJCgv/xAC1EAACAQMDAgQDBQUEBAAAAX0BAgMABBEFEiExQQYTUWEHInEUMoGRoQgjQrHBFVLR8CQzYnKCCQoWFxgZGiUmJygpKjQ1Njc4OTpDREVGR0hJSlNUVVZXWFlaY2RlZmdoaWpzdHV2d3h5eoOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4eLj5OXm5+jp6vHy8/T19vf4+fr/xAAfAQADAQEBAQEBAQEBAAAAAAAAAQIDBAUGBwgJCgv/xAC1EQACAQIEBAMEBwUEBAABAncAAQIDEQQFITEGEkFRB2FxEyIygQgUQpGhscEJIzNS8BVictEKFiQ04SXxFxgZGiYnKCkqNTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqCg4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2dri4+Tl5ufo6ery8/T19vf4+fr/2gAMAwEAAhEDEQA/APjz4y32qfs5/tf+KPgx48t9QtfiLZ6xp9hpvh2wsn1S6m+2WFlNbwxfY1lE0kslwdqKzOTIFx0A7/8A4Z0+LP8A0Q/9oD/w1fiL/wCQq8S/4OYfFmq+Av8Ag4B+MGu6FqWoaLrei3fhu/0/ULC4e2urC4i0HS3imilQh45EdVZXUgqQCCCK+cP+HsX7U3/Ry37QH/hw9X/+SK/TOHfFTNslyyhlWEp03ToxUU5RldpdXaaV+9kjTiStPPM1xGcY1v2lecpyS2Tk72je7UVtFNuySVz/2Q==");

	@Mock(answer=Answers.RETURNS_MOCKS)
	IDriver driver;

	@Mock
	IPublisher publisher;

	//The Folder will be created before each test method and (recursively) deleted after each test method.
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private static void createImgFile(final File f, final byte[] bytes) throws IOException{
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(f));
			out.write(bytes);
		} finally {
			if (out != null) {
				out.close();
			}
		}		
	}

	private void testPlain(final String format, final byte[] srcImgBytes) throws Exception{
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(ImageFileReader.Parameters.RESIZE.getParameterName(), false);
		params.putParameter(ImageFileReader.Parameters.TRANSCODE.getParameterName(), false);

		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		createImgFile(f, srcImgBytes);

		//Start the test
		final ImageFileReader testSubject = new ImageFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//An imagefile should contain only one record
		assertNull(testSubject.readRecord());

		assertEquals(record.keySet().size(), 7);
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_FORMAT), format);
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_HEIGHT), "10");
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_WIDTH), "10");
		assertEquals(record.get(ImageFileReader.FIELD_SRC_FORMAT), format);
		assertEquals(record.get(ImageFileReader.FIELD_SRC_HEIGHT), "10");
		assertEquals(record.get(ImageFileReader.FIELD_SRC_WIDTH), "10");

		//Now validate the image
		final byte[] imgBytes = decoder.decode(record.get(ImageFileReader.FIELD_IMAGE_BYTES));
		final ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
		final ImageInputStream imgInputStream = ImageIO.createImageInputStream(bais);
		final ImageReader imgReader = ImageIO.getImageReaders(imgInputStream).next();
		imgReader.setInput(imgInputStream, false, false);
		ImageTypeSpecifier imageSrcDestType = imgReader.getRawImageType(0);
		if (imageSrcDestType==null){
			imageSrcDestType = imgReader.getImageTypes(0).next();
		}
		final ImageReadParam imageReadParams = imgReader.getDefaultReadParam();
		imageReadParams.setDestinationType(imageSrcDestType);

		final BufferedImage img = imgReader.read(0, imageReadParams);

		assertEquals(img.getWidth(), 10);
		assertEquals(img.getHeight(), 10);
		assertEquals(imgReader.getFormatName(), format);		
	}

	private void testResizeAspectRatio(final String format, final byte[] srcImgBytes) throws Exception{
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(ImageFileReader.Parameters.RESIZE.getParameterName(), true);
		params.putParameter(ImageFileReader.Parameters.RESIZE_HEIGTH.getParameterName(), 0);
		params.putParameter(ImageFileReader.Parameters.RESIZE_WIDTH.getParameterName(), 30);
		params.putParameter(ImageFileReader.Parameters.TRANSCODE.getParameterName(), false);

		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		createImgFile(f, srcImgBytes);

		//Start the test
		final ImageFileReader testSubject = new ImageFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//An imagefile should contain only one record
		assertNull(testSubject.readRecord());

		assertEquals(record.keySet().size(), 7);
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_FORMAT), format);
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_HEIGHT), "30");
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_WIDTH), "30");
		assertEquals(record.get(ImageFileReader.FIELD_SRC_FORMAT), format);
		assertEquals(record.get(ImageFileReader.FIELD_SRC_HEIGHT), "10");
		assertEquals(record.get(ImageFileReader.FIELD_SRC_WIDTH), "10");

		//Now validate the image
		final byte[] imgBytes = decoder.decode(record.get(ImageFileReader.FIELD_IMAGE_BYTES));
		final ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
		final ImageInputStream imgInputStream = ImageIO.createImageInputStream(bais);
		final ImageReader imgReader = ImageIO.getImageReaders(imgInputStream).next();
		imgReader.setInput(imgInputStream, false, false);
		ImageTypeSpecifier imageSrcDestType = imgReader.getRawImageType(0);
		if (imageSrcDestType==null){
			imageSrcDestType = imgReader.getImageTypes(0).next();
		}
		final ImageReadParam imageReadParams = imgReader.getDefaultReadParam();
		imageReadParams.setDestinationType(imageSrcDestType);

		final BufferedImage img = imgReader.read(0, imageReadParams);

		assertEquals(img.getWidth(), 30);
		assertEquals(img.getHeight(), 30);
		assertEquals(imgReader.getFormatName(), format);		
	}

	private void testResizeNoAspectRatio(final String format, final byte[] srcImgBytes) throws Exception{
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(ImageFileReader.Parameters.RESIZE.getParameterName(), true);
		params.putParameter(ImageFileReader.Parameters.RESIZE_HEIGTH.getParameterName(), 40);
		params.putParameter(ImageFileReader.Parameters.RESIZE_WIDTH.getParameterName(), 30);
		params.putParameter(ImageFileReader.Parameters.TRANSCODE.getParameterName(), false);

		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		createImgFile(f, srcImgBytes);

		//Start the test
		final ImageFileReader testSubject = new ImageFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//An imagefile should contain only one record
		assertNull(testSubject.readRecord());

		assertEquals(record.keySet().size(), 7);
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_FORMAT), format);
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_HEIGHT), "40");
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_WIDTH), "30");
		assertEquals(record.get(ImageFileReader.FIELD_SRC_FORMAT), format);
		assertEquals(record.get(ImageFileReader.FIELD_SRC_HEIGHT), "10");
		assertEquals(record.get(ImageFileReader.FIELD_SRC_WIDTH), "10");

		//Now validate the image
		final byte[] imgBytes = decoder.decode(record.get(ImageFileReader.FIELD_IMAGE_BYTES));
		final ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
		final ImageInputStream imgInputStream = ImageIO.createImageInputStream(bais);
		final ImageReader imgReader = ImageIO.getImageReaders(imgInputStream).next();
		imgReader.setInput(imgInputStream, false, false);
		ImageTypeSpecifier imageSrcDestType = imgReader.getRawImageType(0);
		if (imageSrcDestType==null){
			imageSrcDestType = imgReader.getImageTypes(0).next();
		}
		final ImageReadParam imageReadParams = imgReader.getDefaultReadParam();
		imageReadParams.setDestinationType(imageSrcDestType);

		final BufferedImage img = imgReader.read(0, imageReadParams);

		assertEquals(img.getWidth(), 30);
		assertEquals(img.getHeight(), 40);
		assertEquals(imgReader.getFormatName(), format);		
	}

	private void testTranscode(final String srcFormat, final String destFormat, final byte[] srcImgBytes) throws Exception{
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(ImageFileReader.Parameters.RESIZE.getParameterName(), false);
		params.putParameter(ImageFileReader.Parameters.TRANSCODE.getParameterName(), true);
		params.putParameter(ImageFileReader.Parameters.TRANSCODE_FORMAT.getParameterName(), destFormat);

		final File f = temporaryFolder.newFile();
		f.deleteOnExit();
		createImgFile(f, srcImgBytes);

		//Start the test
		final ImageFileReader testSubject = new ImageFileReader();
		testSubject.init(trace,params,publisher);
		testSubject.openFile(f);
		final Map<String, String> record = testSubject.readRecord();

		//An imagefile should contain only one record
		assertNull(testSubject.readRecord());

		assertEquals(record.keySet().size(), 7);
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_FORMAT), destFormat);
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_HEIGHT), "10");
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_WIDTH), "10");
		assertEquals(record.get(ImageFileReader.FIELD_SRC_FORMAT), srcFormat);
		assertEquals(record.get(ImageFileReader.FIELD_SRC_HEIGHT), "10");
		assertEquals(record.get(ImageFileReader.FIELD_SRC_WIDTH), "10");

		//Now validate the image
		final byte[] imgBytes = decoder.decode(record.get(ImageFileReader.FIELD_IMAGE_BYTES));
		final ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
		final ImageInputStream imgInputStream = ImageIO.createImageInputStream(bais);
		final ImageReader imgReader = ImageIO.getImageReaders(imgInputStream).next();
		imgReader.setInput(imgInputStream, false, false);
		ImageTypeSpecifier imageSrcDestType = imgReader.getRawImageType(0);
		if (imageSrcDestType==null){
			imageSrcDestType = imgReader.getImageTypes(0).next();
		}
		final ImageReadParam imageReadParams = imgReader.getDefaultReadParam();
		imageReadParams.setDestinationType(imageSrcDestType);

		final BufferedImage img = imgReader.read(0, imageReadParams);

		assertEquals(img.getWidth(), 10);
		assertEquals(img.getHeight(), 10);
		assertEquals(imgReader.getFormatName(), destFormat);		
	}

	@Test
	public void testPlainImagePNG() throws Exception {
		testPlain("png", PNG_IMG);
	}

	@Test
	public void testPlainImageJPG() throws Exception {
		testPlain("JPEG", JPG_IMG);
	}

	@Test
	public void testPlainImageGIF() throws Exception {
		testPlain("gif", GIF_IMG);
	}

	@Test
	public void testPlainImageBMP() throws Exception {
		testPlain("bmp", BMP_IMG);
	}

	@Test
	public void testResizeImagePNG() throws Exception {
		testResizeAspectRatio("png", PNG_IMG);
	}

	@Test
	public void testResizeImageJPG() throws Exception {
		testResizeAspectRatio("JPEG", JPG_IMG);
	}

	@Test
	public void testResizeImageGIF() throws Exception {
		testResizeAspectRatio("gif", GIF_IMG);
	}
	@Test
	public void testResizeImageBMP() throws Exception {
		testResizeAspectRatio("bmp", BMP_IMG);
	}

	@Test
	public void testResizeNoARImagePNG() throws Exception {
		testResizeNoAspectRatio("png", PNG_IMG);
	}

	@Test
	public void testResizeNoARImageJPG() throws Exception {
		testResizeNoAspectRatio("JPEG", JPG_IMG);
	}

	@Test
	public void testResizeNoARImageGIF() throws Exception {
		testResizeNoAspectRatio("gif", GIF_IMG);
	}
	@Test
	public void testResizeNoARImageBMP() throws Exception {
		testResizeNoAspectRatio("bmp", BMP_IMG);
	}

	private void testTranscodeAll(final String srcFormat, final byte[] bytes) throws Exception{
		final String names[] = new String[]{"png","JPEG","gif","bmp"};//ImageIO.getWriterFormatNames();
		for (final String aName : names) {
			if (!srcFormat.equalsIgnoreCase(aName)) {
				System.out.println("Format src/dest:"+srcFormat+"/"+aName);
				testTranscode(srcFormat, aName, bytes);
			}
		}
	}

	@Test
	public void testTranscodeImagePNG() throws Exception {
		testTranscodeAll("png", PNG_IMG);
	}

	@Test
	public void testTranscodeImageJPG() throws Exception {
		testTranscodeAll("JPEG", JPG_IMG);
	}

	@Test
	public void testTranscodeImageGIF() throws Exception {
		testTranscodeAll("gif", GIF_IMG);
	}
	@Test
	public void testTranscodeImageBMP() throws Exception {
		testTranscodeAll("bmp", BMP_IMG);
	}
}
