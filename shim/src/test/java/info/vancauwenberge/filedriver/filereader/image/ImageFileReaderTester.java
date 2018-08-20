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
import info.vancauwenberge.filedriver.exception.ReadException;
import info.vancauwenberge.filedriver.filepublisher.IPublisher;

public class ImageFileReaderTester extends AbstractStrategyTest{
	private static final Decoder decoder = Base64.getDecoder();
	//private static final Encoder encoder = Base64.getEncoder();
	private static byte[] BMP_IMG = decoder.decode("Qk12AQAAAAAAADYAAAAoAAAACgAAAAoAAAABABgAAAAAAEABAADEDgAAxA4AAAAAAAAAAAAA////////AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD///////8AAAAAAAAAAAAAAAAAAAD///////////8AAP///////wAAAAAAAAAAAAAAAAAAAP///////////wAA////////AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAkHO0kHO0AAADMSD/MSD/MSD/MSD/MSD/MSD/MSD8AACQc7SQc7QAAAMxIP8xIP8xIP8xIP8xIP8xIP8xIPwAAJBztJBztAAAAzEg/zEg/zEg/zEg/zEg/zEg/zEg/AAAkHO0kHO0AAADMSD/MSD/MSD/MSD/MSD/MSD/MSD8AACQc7SQc7SQc7cxIP8xIP8xIP8xIP8xIP8xIP8xIPwAAJBztJBztJBztzEg/zEg/zEg/zEg/zEg/zEg/zEg/AAA=");
	private static byte[] GIF_IMG = decoder.decode("R0lGODlhCgAKAPcAAAAAAAAAMwAAZgAAmQAAzAAA/wArAAArMwArZgArmQArzAAr/wBVAABVMwBVZgBVmQBVzABV/wCAAACAMwCAZgCAmQCAzACA/wCqAACqMwCqZgCqmQCqzACq/wDVAADVMwDVZgDVmQDVzADV/wD/AAD/MwD/ZgD/mQD/zAD//zMAADMAMzMAZjMAmTMAzDMA/zMrADMrMzMrZjMrmTMrzDMr/zNVADNVMzNVZjNVmTNVzDNV/zOAADOAMzOAZjOAmTOAzDOA/zOqADOqMzOqZjOqmTOqzDOq/zPVADPVMzPVZjPVmTPVzDPV/zP/ADP/MzP/ZjP/mTP/zDP//2YAAGYAM2YAZmYAmWYAzGYA/2YrAGYrM2YrZmYrmWYrzGYr/2ZVAGZVM2ZVZmZVmWZVzGZV/2aAAGaAM2aAZmaAmWaAzGaA/2aqAGaqM2aqZmaqmWaqzGaq/2bVAGbVM2bVZmbVmWbVzGbV/2b/AGb/M2b/Zmb/mWb/zGb//5kAAJkAM5kAZpkAmZkAzJkA/5krAJkrM5krZpkrmZkrzJkr/5lVAJlVM5lVZplVmZlVzJlV/5mAAJmAM5mAZpmAmZmAzJmA/5mqAJmqM5mqZpmqmZmqzJmq/5nVAJnVM5nVZpnVmZnVzJnV/5n/AJn/M5n/Zpn/mZn/zJn//8wAAMwAM8wAZswAmcwAzMwA/8wrAMwrM8wrZswrmcwrzMwr/8xVAMxVM8xVZsxVmcxVzMxV/8yAAMyAM8yAZsyAmcyAzMyA/8yqAMyqM8yqZsyqmcyqzMyq/8zVAMzVM8zVZszVmczVzMzV/8z/AMz/M8z/Zsz/mcz/zMz///8AAP8AM/8AZv8Amf8AzP8A//8rAP8rM/8rZv8rmf8rzP8r//9VAP9VM/9VZv9Vmf9VzP9V//+AAP+AM/+AZv+Amf+AzP+A//+qAP+qM/+qZv+qmf+qzP+q///VAP/VM//VZv/Vmf/VzP/V////AP//M///Zv//mf//zP///wAAAAAAAAAAAAAAACH5BAEAAPwALAAAAAAKAAoAAAhOAKW9gtXFiw4dNA5me2VozJiEZBJKg6Xi4UGDOqa9CpAjYkSEr16pyJHwIBkdrwoFGJODZMsc+vYBmBkAQM0A+2TOnJmz506ePXX+nBkQADs=");
	private static byte[] PNG_IMG = decoder.decode("iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsQAAA7EAZUrDhsAAAAxSURBVChTY3gro/Ifhu09zuDEpCtkYGDAqgCGB1LhfygAKcSLoeqwSiID6ihEYIb/AIv31YddzQXiAAAAAElFTkSuQmCC");
	private static byte[] JPG_IMG = decoder.decode("/9j/4AAQSkZJRgABAQEAYABgAAD/4QBaRXhpZgAATU0AKgAAAAgABQMBAAUAAAABAAAASgMDAAEAAAABAAAAAFEQAAEAAAABAQAAAFERAAQAAAABAAAOxFESAAQAAAABAAAOxAAAAAAAAYagAACxj//bAEMAAgEBAgEBAgICAgICAgIDBQMDAwMDBgQEAwUHBgcHBwYHBwgJCwkICAoIBwcKDQoKCwwMDAwHCQ4PDQwOCwwMDP/bAEMBAgICAwMDBgMDBgwIBwgMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDP/AABEIAAoACgMBIgACEQEDEQH/xAAfAAABBQEBAQEBAQAAAAAAAAAAAQIDBAUGBwgJCgv/xAC1EAACAQMDAgQDBQUEBAAAAX0BAgMABBEFEiExQQYTUWEHInEUMoGRoQgjQrHBFVLR8CQzYnKCCQoWFxgZGiUmJygpKjQ1Njc4OTpDREVGR0hJSlNUVVZXWFlaY2RlZmdoaWpzdHV2d3h5eoOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4eLj5OXm5+jp6vHy8/T19vf4+fr/xAAfAQADAQEBAQEBAQEBAAAAAAAAAQIDBAUGBwgJCgv/xAC1EQACAQIEBAMEBwUEBAABAncAAQIDEQQFITEGEkFRB2FxEyIygQgUQpGhscEJIzNS8BVictEKFiQ04SXxFxgZGiYnKCkqNTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqCg4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2dri4+Tl5ufo6ery8/T19vf4+fr/2gAMAwEAAhEDEQA/APjz4y32qfs5/tf+KPgx48t9QtfiLZ6xp9hpvh2wsn1S6m+2WFlNbwxfY1lE0kslwdqKzOTIFx0A7/8A4Z0+LP8A0Q/9oD/w1fiL/wCQq8S/4OYfFmq+Av8Ag4B+MGu6FqWoaLrei3fhu/0/ULC4e2urC4i0HS3imilQh45EdVZXUgqQCCCK+cP+HsX7U3/Ry37QH/hw9X/+SK/TOHfFTNslyyhlWEp03ToxUU5RldpdXaaV+9kjTiStPPM1xGcY1v2lecpyS2Tk72je7UVtFNuySVz/2Q==");
	private static byte[] JPG_META_IMG = decoder.decode("/9j/4AAQSkZJRgABAQEAYABgAAD/4RFMRXhpZgAATU0AKgAAAAgADgEOAAIAAAAHAAAIwgE7AAIAAAAJAAAIygMBAAUAAAABAAAI1AMDAAEAAAABAAAAAFEQAAEAAAABAQAAAFERAAQAAAABAAAOxFESAAQAAAABAAAOxIKYAAIAAAALAAAI3IdpAAQAAAABAAAI6JybAAEAAAAOAAARCJydAAEAAAASAAARFpyeAAEAAAAKAAARKJyfAAEAAAASAAARMuocAAcAAAgMAAAAtgAAAAAc6gAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGFUaXRsZQAAYW5BdXRob3IAAAABhqAAALGPYUNvcHlyaWdodAAAAAHqHAAHAAAIDAAACPoAAAAAHOoAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGEAVABpAHQAbABlAAAAYQBuAEEAdQB0AGgAbwByAAAAYQBUAGEAZwAAAGEAUwB1AGIAagBlAGMAdAAAAP/hDl1odHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvADw/eHBhY2tldCBiZWdpbj0n77u/JyBpZD0nVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkJz8+DQo8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIj48cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPjxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSJ1dWlkOmZhZjViZGQ1LWJhM2QtMTFkYS1hZDMxLWQzM2Q3NTE4MmYxYiIgeG1sbnM6ZGM9Imh0dHA6Ly9wdXJsLm9yZy9kYy9lbGVtZW50cy8xLjEvIi8+PHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9InV1aWQ6ZmFmNWJkZDUtYmEzZC0xMWRhLWFkMzEtZDMzZDc1MTgyZjFiIiB4bWxuczpkYz0iaHR0cDovL3B1cmwub3JnL2RjL2VsZW1lbnRzLzEuMS8iPjxkYzpjcmVhdG9yPjxyZGY6U2VxIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+PHJkZjpsaT5hbkF1dGhvcjwvcmRmOmxpPjwvcmRmOlNlcT4NCgkJCTwvZGM6Y3JlYXRvcj48ZGM6cmlnaHRzPjxyZGY6QWx0IHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+PHJkZjpsaSB4bWw6bGFuZz0ieC1kZWZhdWx0Ij5hQ29weXJpZ2h0PC9yZGY6bGk+PC9yZGY6QWx0Pg0KCQkJPC9kYzpyaWdodHM+PGRjOnN1YmplY3Q+PHJkZjpCYWcgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj48cmRmOmxpPmFUYWc8L3JkZjpsaT48L3JkZjpCYWc+DQoJCQk8L2RjOnN1YmplY3Q+PGRjOnRpdGxlPjxyZGY6QWx0IHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+PHJkZjpsaSB4bWw6bGFuZz0ieC1kZWZhdWx0Ij5hVGl0bGU8L3JkZjpsaT48L3JkZjpBbHQ+DQoJCQk8L2RjOnRpdGxlPjxkYzpkZXNjcmlwdGlvbj48cmRmOkFsdCB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPjxyZGY6bGkgeG1sOmxhbmc9IngtZGVmYXVsdCI+YVRpdGxlPC9yZGY6bGk+PC9yZGY6QWx0Pg0KCQkJPC9kYzpkZXNjcmlwdGlvbj48L3JkZjpEZXNjcmlwdGlvbj48cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0idXVpZDpmYWY1YmRkNS1iYTNkLTExZGEtYWQzMS1kMzNkNzUxODJmMWIiIHhtbG5zOk1pY3Jvc29mdFBob3RvPSJodHRwOi8vbnMubWljcm9zb2Z0LmNvbS9waG90by8xLjAvIi8+PHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9InV1aWQ6ZmFmNWJkZDUtYmEzZC0xMWRhLWFkMzEtZDMzZDc1MTgyZjFiIiB4bWxuczpNaWNyb3NvZnRQaG90bz0iaHR0cDovL25zLm1pY3Jvc29mdC5jb20vcGhvdG8vMS4wLyI+PE1pY3Jvc29mdFBob3RvOkxhc3RLZXl3b3JkWE1QPjxyZGY6QmFnIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+PHJkZjpsaT5hVGFnPC9yZGY6bGk+PC9yZGY6QmFnPg0KCQkJPC9NaWNyb3NvZnRQaG90bzpMYXN0S2V5d29yZFhNUD48L3JkZjpEZXNjcmlwdGlvbj48L3JkZjpSREY+PC94OnhtcG1ldGE+DQogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgIDw/eHBhY2tldCBlbmQ9J3cnPz7/2wBDAAIBAQIBAQICAgICAgICAwUDAwMDAwYEBAMFBwYHBwcGBwcICQsJCAgKCAcHCg0KCgsMDAwMBwkODw0MDgsMDAz/2wBDAQICAgMDAwYDAwYMCAcIDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAz/wAARCAAKAAoDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD48+Mt9qn7Of7X/ij4MePLfULX4i2esafYab4dsLJ9UupvtlhZTW8MX2NZRNJLJcHaiszkyBcdAO//AOGdPiz/ANEP/aA/8NX4i/8AkKvEv+DmHxZqvgL/AIOAfjBruhalqGi63ot34bv9P1CwuHtrqwuItB0t4popUIeORHVWV1IKkAggivnD/h7F+1N/0ct+0B/4cPV//kiv0zh3xUzbJcsoZVhKdN06MVFOUZXaXV2mlfvZI04krTzzNcRnGNb9pXnKcktk5O9o3u1FbRTbsklc/9k=");
	private static byte[] TIF_IMG = decoder.decode("SUkqAG4AAACAO0OCR/gCDQeDFIWKiEQ2HQ+DwKCRCFQyIReHxKCwcJuRuAAfkhmRiSQiNQiOx+QyOSyWTxyPSCRS2XQONwaUzKWTSLv+fTcAAGhTyaT+gUIA0QAT+e0yD0ilU6H0aEVClSSAgAAVAP4ABAABAAAAAAAAAAABBAABAAAACgAAAAEBBAABAAAACgAAAAIBAwAEAAAAcAEAAAMBAwABAAAABQAAAAYBAwABAAAAAgAAABEBBAABAAAACAAAABUBAwABAAAABAAAABYBBAABAAAACgAAABcBBAABAAAAZQAAABoBBQABAAAAeAEAABsBBQABAAAAgAEAABwBAwABAAAAAQAAACgBAwABAAAAAgAAAD0BAwABAAAAAgAAAFIBAwABAAAAAgAAAAEDBQABAAAAiAEAAAMDAQABAAAAAAAAABBRAQABAAAAAQAAABFRBAABAAAAxA4AABJRBAABAAAAxA4AAAAAAAAIAAgACAAIAAx3AQDoAwAADHcBAOgDAACghgEAj7EAAA==");
	private static byte[] PNG_IM_ILLEGAL = decoder.decode("iVBORw0KGgoAAAANSURSAAAACgAAAAoIBgAAAI0yz70AAAABc1JHQgCuzhzpAAAABGdBTUEAALGPC/xhBQAAAAlwSFlzAAAOxAAADsQBlSsOGwAAADFJREFUKFNjeCuj8h+G7T3O4MSkK2RgYMCqAIYHUuF/KAApxIuh6rBKIgPqKERghv8Ai/fVh13NBeIAAAAASUVORK5CYIINCg==");

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
		params.putParameter(ImageFileReader.Parameters.IMAGE_META.getParameterName(), false);
		params.putParameter(ImageFileReader.Parameters.RESIZE.getParameterName(), false);
		params.putParameter(ImageFileReader.Parameters.RESIZE_RESIZE_MODE.getParameterName(), "DYNAMIC");
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

	private void testMetaData(final String format, final byte[] srcImgBytes) throws Exception{
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(ImageFileReader.Parameters.IMAGE_META.getParameterName(), true);
		params.putParameter(ImageFileReader.Parameters.RESIZE.getParameterName(), false);
		params.putParameter(ImageFileReader.Parameters.RESIZE_RESIZE_MODE.getParameterName(), "DYNAMIC");

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
		params.putParameter(ImageFileReader.Parameters.IMAGE_META.getParameterName(), false);
		params.putParameter(ImageFileReader.Parameters.RESIZE.getParameterName(), true);
		params.putParameter(ImageFileReader.Parameters.RESIZE_RESIZE_MODE.getParameterName(), "DYNAMIC");
		params.putParameter(ImageFileReader.Parameters.RESIZE_HEIGTH.getParameterName(), 0);
		params.putParameter(ImageFileReader.Parameters.RESIZE_WIDTH.getParameterName(), 30);
		params.putParameter(ImageFileReader.Parameters.RESIZE_RESIZE_PADDING.getParameterName(), "NOPADDING");
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

	private void testResizeWithPadding(final String format, final byte[] srcImgBytes) throws Exception{
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(ImageFileReader.Parameters.IMAGE_META.getParameterName(), false);
		params.putParameter(ImageFileReader.Parameters.RESIZE.getParameterName(), true);
		params.putParameter(ImageFileReader.Parameters.RESIZE_RESIZE_MODE.getParameterName(), "DYNAMIC");
		params.putParameter(ImageFileReader.Parameters.RESIZE_HEIGTH.getParameterName(), 100);
		params.putParameter(ImageFileReader.Parameters.RESIZE_WIDTH.getParameterName(), 30);
		params.putParameter(ImageFileReader.Parameters.RESIZE_RESIZE_PADDING.getParameterName(), "PADDING");
		params.putParameter(ImageFileReader.Parameters.RESIZE_RESIZE_PADDING_COLOR.getParameterName(), "0000FF");
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
		assertEquals(record.get(ImageFileReader.FIELD_IMAGE_HEIGHT), "100");
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
		assertEquals(img.getHeight(), 100);
		assertEquals(imgReader.getFormatName(), format);

		final File tempFile = File.createTempFile("prefix-", format);
		System.out.println("Saving to file:"+tempFile.getAbsolutePath());
		final FileOutputStream fos = new FileOutputStream(tempFile);
		fos.write(imgBytes);
		fos.close();


	}

	private void testResizeNoARImage(final String format, final byte[] srcImgBytes) throws Exception{
		final Trace trace = new Trace(">");
		final ParamMap params = new ParamMap();
		//No clue how the Parameter works, so just overwrite what we need...
		params.putParameter(ImageFileReader.Parameters.IMAGE_META.getParameterName(), false);
		params.putParameter(ImageFileReader.Parameters.RESIZE.getParameterName(), true);
		params.putParameter(ImageFileReader.Parameters.RESIZE_RESIZE_MODE.getParameterName(), "EXACT");
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
		params.putParameter(ImageFileReader.Parameters.IMAGE_META.getParameterName(), false);
		params.putParameter(ImageFileReader.Parameters.RESIZE.getParameterName(), false);
		params.putParameter(ImageFileReader.Parameters.RESIZE_RESIZE_MODE.getParameterName(), "DYNAMIC");
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
		assertEquals(imgReader.getFormatName().toLowerCase(), destFormat.toLowerCase());
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
	public void testMetaDataBMP() throws Exception {
		testMetaData("bmp", BMP_IMG);
	}
	@Test
	public void testMetaDataJPG() throws Exception {
		testMetaData("JPEG", JPG_IMG);
	}
	@Test
	public void testMetaDataJPG2() throws Exception {
		testMetaData("JPEG", JPG_META_IMG);
	}
	@Test
	public void testMetaDataGIF() throws Exception {
		testMetaData("gif", GIF_IMG);
	}
	@Test
	public void testMetaDataPNG() throws Exception {
		testMetaData("png", PNG_IMG);
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
		testResizeNoARImage("png", PNG_IMG);
	}

	@Test
	public void testResizeNoARImageJPG() throws Exception {
		testResizeNoARImage("JPEG", JPG_IMG);
	}

	@Test
	public void testResizeNoARImageGIF() throws Exception {
		testResizeNoARImage("gif", GIF_IMG);
	}
	@Test
	public void testResizeNoARImageBMP() throws Exception {
		testResizeNoARImage("bmp", BMP_IMG);
	}
	@Test
	public void testResizeWithPaddingBMP() throws Exception {
		testResizeWithPadding("bmp", BMP_IMG);
	}

	@Test
	public void testResizeWithPaddingGIF() throws Exception {
		testResizeWithPadding("gif", GIF_IMG);
	}
	@Test
	public void testResizeWithPaddingJPG() throws Exception {
		testResizeWithPadding("JPEG", JPG_IMG);
	}
	@Test
	public void testResizeWithPaddingPNG() throws Exception {
		testResizeWithPadding("png", PNG_IMG);
	}

	@Test
	public void testTranscodeImagePNG() throws Exception {
		testTranscode("png", "gif", PNG_IMG);
		testTranscode("png", "JPEG", PNG_IMG);
	}

	@Test
	public void testTranscodeImageJPG() throws Exception {
		testTranscode("JPEG", "gif", JPG_IMG);
		testTranscode("JPEG", "bmp", JPG_IMG);
		testTranscode("JPEG", "png", JPG_IMG);
	}

	@Test
	public void testTranscodeImageGIF() throws Exception {
		testTranscode("gif", "png", GIF_IMG);
		testTranscode("gif", "bmp", GIF_IMG);
	}
	@Test
	public void testTranscodeImageBMP() throws Exception {
		testTranscode("bmp","PNG", BMP_IMG);
		testTranscode("bmp","jpeg", BMP_IMG);
		testTranscode("bmp","GIF", BMP_IMG);
	}

	@Test(expected = ReadException.class)
	public void testUnsuppotedSourceTIF() throws Exception {
		testPlain("tif", TIF_IMG);
	}

	@Test(expected = ReadException.class)
	public void testUnsuppotedDestTIF() throws Exception {
		testTranscode("bmp", "tif", BMP_IMG);
	}

	@Test(expected = ReadException.class)
	public void testIllegalPNG() throws Exception {
		testPlain("png", PNG_IM_ILLEGAL);
	}
}
