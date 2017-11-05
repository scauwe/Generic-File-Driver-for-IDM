/*******************************************************************************
 * Copyright (c) 2007-2017 Stefaan Van Cauwenberge
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
package info.vancauwenberge.filedriver.filereader.csv;

import java.io.*;

/**
 * Generic unicode textreader, which will use BOM mark
 * to identify the encoding to be used.
 */
public class UnicodeReader extends Reader {
   PushbackInputStream internalIn;
	InputStreamReader   internalIn2 = null;
	String              defaultEnc;

	private static final int BOM_SIZE = 4;

/*
Default encoding is used only if BOM is not found. If
defaultEncoding is NULL then systemdefault is used.
*/
	UnicodeReader(InputStream in, String defaultEnc) {
		internalIn = new PushbackInputStream(in, BOM_SIZE);
		this.defaultEnc = defaultEnc;
	}

	public String getDefaultEncoding() {
      return defaultEnc;
   }

   public String getEncoding() {
      if (internalIn2 == null) return null;
      return internalIn2.getEncoding();
   }

   /**
    * Read-ahead four bytes and check for BOM marks. Extra bytes are
    * unread back to the stream, only BOM bytes are skipped.
    */
	protected void init() throws IOException {
      if (internalIn2 != null) return;

      String encoding;
		byte bom[] = new byte[BOM_SIZE];
		int n, unread;
		n = internalIn.read(bom, 0, bom.length);

      if (  (bom[0] == (byte)0xEF) && (bom[1] == (byte)0xBB) &&
            (bom[2] == (byte)0xBF) ) {
         encoding = "UTF-8";
         unread = n - 3;
      } else if ( (bom[0] == (byte)0xFE) && (bom[1] == (byte)0xFF) ) {
         encoding = "UTF-16BE";
         unread = n - 2;
      } else if ( (bom[0] == (byte)0xFF) && (bom[1] == (byte)0xFE) ) {
         encoding = "UTF-16LE";
         unread = n - 2;
      } else if ( (bom[0] == (byte)0x00) && (bom[1] == (byte)0x00) &&
                  (bom[2] == (byte)0xFE) && (bom[3] == (byte)0xFF)) {
         encoding = "UTF-32BE";
         unread = n - 4;
      } else if ( (bom[0] == (byte)0xFF) && (bom[1] == (byte)0xFE) &&
                  (bom[2] == (byte)0x00) && (bom[3] == (byte)0x00)) {
         encoding = "UTF-32LE";
         unread = n - 4;
      } else {
         // Unicode BOM mark not found, unread all bytes
         encoding = defaultEnc;
         unread = n;
      }
//      System.out.println("read=" + n + ", unread=" + unread);

      if (unread > 0) internalIn.unread(bom, (n - unread), unread);
      else if (unread < -1) internalIn.unread(bom, 0, 0);

      // Use given encoding
      if (encoding == null) {
         internalIn2 = new InputStreamReader(internalIn);
      } else {
         internalIn2 = new InputStreamReader(internalIn, encoding);
      }
	}

   public void close() throws IOException {
      init();
      internalIn2.close();
   }

   public int read(char[] cbuf, int off, int len) throws IOException {
      init();
      return internalIn2.read(cbuf, off, len);
   }

}
