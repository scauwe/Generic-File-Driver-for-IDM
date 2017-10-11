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
package info.vancauwenberge.filedriver;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Build {
	
	/** The build of your driver. */
    public static final String BUILD_NO;

    /** The version of your driver. */
	static public final String PRODUCT_VERSION;
	
	/** Your company name. */
	static public final String COMPANY_NAME;
	
	/** The name of your driver. */
	static public final String PRODUCT_NAME;
	
	static{
		Class<Build> clazz = Build.class;
		String className = clazz.getSimpleName() + ".class";
		Manifest manifest = null;
		try {
			URLConnection connection = clazz.getResource(className).openConnection();
			if (connection instanceof JarURLConnection){
				manifest = ((JarURLConnection)connection).getManifest();
			}else{
				// Class not from JAR, but from the file system.
				String classPath = clazz.getResource(className).toString();
				String packageName = clazz.getPackage().getName();
				String manifestPath = classPath.substring(0, classPath.length()-className.length()-packageName.length()-1) + "META-INF" + File.separatorChar + "MANIFEST.MF";
				manifest = new Manifest(new URL(manifestPath).openStream());
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		Attributes attr  = manifest==null?null:manifest.getMainAttributes();
		BUILD_NO = getAttribute("Built-Date",attr);
		PRODUCT_VERSION = getProductVersion(BUILD_NO,getAttribute("Implementation-Version",attr));
		COMPANY_NAME  = getAttribute("Implementation-Vendor",attr);
		PRODUCT_NAME = getAttribute("Implementation-Title",attr);
	}
	/*
	 * Get an attribute from the manifest, or unknown if no manifest is defined.
	 */
	private static String getAttribute(String name, Attributes attr ){
		if (attr!= null){
			String value = attr.getValue(name);
			if (value != null)
				return value;
		}
		return "unknown";
	}

	/**
	 * We reformat the date time in buildNo to YYYYMMDD-HHMMSS and append that to the productVersion
	 * @param buildNo
	 * @param productVesion
	 * @return
	 */
	private static String getProductVersion(String buildNo, String productVesion) {
		SimpleDateFormat fromDf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		fromDf.setTimeZone(TimeZone.getTimeZone("UTC"));
		SimpleDateFormat toDf = new SimpleDateFormat("yyyyMMddHHmm");
		toDf.setTimeZone(TimeZone.getTimeZone("UTC"));
		try{
			return productVesion+"."+toDf.format(fromDf.parse(buildNo));
		}catch (Exception e) {
			e.printStackTrace();
			return productVesion;
		}
	}
}
