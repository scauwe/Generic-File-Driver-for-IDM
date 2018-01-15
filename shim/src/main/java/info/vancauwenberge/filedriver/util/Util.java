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
package info.vancauwenberge.filedriver.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.nds.dirxml.driver.XmlDocument;
import com.novell.nds.dirxml.driver.xds.DocumentImpl;
import com.novell.nds.dirxml.driver.xds.ExceptionElement;
import com.novell.nds.dirxml.driver.xds.QueryScope;
import com.novell.nds.dirxml.driver.xds.StatusDocument;
import com.novell.nds.dirxml.driver.xds.StatusLevel;
import com.novell.nds.dirxml.driver.xds.XDSParameterException;
import com.novell.nds.dirxml.driver.xds.XDSQueryDocument;
import com.novell.nds.dirxml.driver.xds.XDSQueryElement;
import com.novell.nds.dirxml.driver.xds.XDSReadAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSSearchAttrElement;
import com.novell.nds.dirxml.driver.xds.XDSStatusElement;
import com.novell.nds.dirxml.driver.xds.util.StatusAttributes;

/**
 * @author nlv10194
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Util {
    static public StatusLevel getStatusLevel(String level){
        if(StatusLevel.FATAL.equals(level))
            return StatusLevel.FATAL;
        if(StatusLevel.ERROR.equals(level))
            return StatusLevel.ERROR;
        if(StatusLevel.WARNING.equals(level))
            return StatusLevel.WARNING;
        if(StatusLevel.SUCCESS.equals(level))
            return StatusLevel.SUCCESS;
        if(StatusLevel.RETRY.equals(level))
            return StatusLevel.RETRY;
        else
            return null;
    	
    }

    /**
     * Get the first statuslevel found in the given XmlDocument or null when no status level can be found.
     * @param xmlDoc
     * @return
     */
    static public StatusLevel getFirstStatus(XmlDocument xmlDoc){
    	StatusLevel level = null;
    	NodeList statusFields = xmlDoc.getDocument().getElementsByTagName("status");
    	
    	if (statusFields.getLength() > 0)
    	{
    		//We have at least one status element
    		Node aStatus = statusFields.item(0);
    		Node levelNode = aStatus.getAttributes().getNamedItem("level");
    		if (levelNode != null)
    			level = getStatusLevel(levelNode.getNodeValue());
    	}
    	return level;
    }

    /**
     * Get the description of the first status level found in the given XmlDocument or null when no description is associated with the first status level.
     * @param xmlDoc
     * @return the description
     */
    static public String getFirstStatusDescription(XmlDocument xmlDoc){
		String descr = null;
    	NodeList statusFields = xmlDoc.getDocument().getElementsByTagName("status");
    	
    	if (statusFields.getLength() > 0)
    	{
    		//We have at least one status element
    		Node aStatus = statusFields.item(0);
   			NodeList statusChilds = aStatus.getChildNodes();
   			int size = statusChilds.getLength();
   			for (int i = 0; (i < size); i++) {
   				Node aChild = statusChilds.item(i);
   				if (aChild.getLocalName().equals("description"))
   				{
   					descr=aChild.getFirstChild().getNodeValue();
   					break;
                }
					
   			}
    	}
    	return descr;
    }

    
    /**
     * Get the worst found status level from the XmlDocument (or SUCCESS when no status level can be found)
     * @param xmlDoc
     * @return
     */
    static public StatusLevel getWorstStatus(XmlDocument xmlDoc){
    	StatusLevel level = StatusLevel.SUCCESS;
    	NodeList statusFields = xmlDoc.getDocument().getElementsByTagName("status");
    	int length =statusFields.getLength();
    	for (int i = 0; i < length; i++) {
    		Node aStatus = statusFields.item(i);
    		Node levelNode = aStatus.getAttributes().getNamedItem("level");
    		if (levelNode != null){
    			StatusLevel thisLevel = getStatusLevel(levelNode.getNodeValue());
    			if (thisLevel.compareTo(level) < 0)
    				level = thisLevel;
    		}
    	}
    	return level;
    }
    
    public static void main(String[] args){
    	System.out.println(StatusLevel.ERROR.compareTo(StatusLevel.WARNING));
    	System.out.println(getSystemDefaultEncoding());
    }

	/**
	 * Prints the stacktrace of an exception to the trace object
	 * @param trace
	 * @param e
	 */
	public static void printStackTrace(Trace trace, Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.close();
		trace.trace(sw.toString(), TraceLevel.EXCEPTION);
	}
	
	public static String getSystemDefaultEncoding(){
		return (new OutputStreamWriter(new ByteArrayOutputStream())).getEncoding();
	}

	private static FileLock getFileLock(Trace trace, FileChannel fileChannel, int counter){
		FileLock lock=null;
		try {
			lock = fileChannel.tryLock();
		} catch (IOException e1) {
			Util.printStackTrace(trace, e1);
		}
		
        if (lock == null){
        	if (counter <=0){
            	trace.trace("Unable to get lock on file. Giving up.", TraceLevel.ERROR_WARN);
        		return null;
        	}
        	trace.trace("Failed to get write lock on file before copy. Will try again in 1 seconds", TraceLevel.ERROR_WARN);
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
        	return getFileLock(trace, fileChannel, counter-1);
        }
    	trace.trace("File locked.", TraceLevel.TRACE);
        return lock;
	}

	
	/**
	 * Copy a file from 'from' to 'to'. Returns true if the file was copied. False otherwise.
	 * @param from
	 * @param to
	 * @return
	 * @throws IOException
	 */
	private static boolean copyFile(Trace trace, File from, File to) {
	    FileInputStream fis = null;
        FileOutputStream fos = null;
        FileLock lock = null;
        try {
            fis = new FileInputStream(from);
            fos = new FileOutputStream(to);
            lock = getFileLock(trace, fos.getChannel(), 10);
            if (lock == null){
            	return false;
            }
            
            byte[] buffer = new byte[1024];
            int noOfBytes = 0;
            // read bytes from source file and write to destination file
            while ((noOfBytes = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, noOfBytes);
            }
            return true;
        }catch (Exception e) {
        	trace.trace("Exception while copying the file:", TraceLevel.ERROR_WARN);
        	Util.printStackTrace(trace, e);
        	return false;
        }
        finally {
            // close the streams using close method
            try {
                if (fos != null) {
                    fos.close();
                }
            }
            catch (Exception ioe) {
            	trace.trace("Error while closing copy output stream: ", TraceLevel.ERROR_WARN);
            	Util.printStackTrace(trace, ioe);
            }
            try {
                if (fis != null) {
                    fis.close();
                }
            }
            catch (Exception ioe) {
            	trace.trace("Error while closing copy input stream: ", TraceLevel.ERROR_WARN);
            	Util.printStackTrace(trace, ioe);
            }
            try{
            	if (lock != null && lock.isValid())//The lock should no longer be valid since we closed the fos
            		lock.release();
            }catch (Exception ioe) {
            	//It should have been closed by the fos.close();
            	trace.trace("Error while releasing file lock: ", TraceLevel.ERROR_WARN);
            	Util.printStackTrace(trace, ioe);
            }
        }
	  }
	
	
	/**
	 * Move a file from sourceFile to destFile.
	 * @param sourceFile
	 * @param destFile
	 * @return
	 */
	public static boolean moveFile(Trace trace, File sourceFile, File destFile) {
		trace.trace("Moving file from "+sourceFile.getAbsolutePath()+" to "+destFile.getAbsolutePath(),TraceLevel.TRACE);
		if (sourceFile.renameTo(destFile)) {
			trace.trace("Moved via java native.",TraceLevel.TRACE);
			return true;
		}else{
			trace.trace("Trying to move via copy-delete sequence.",TraceLevel.TRACE);
			if (copyFile(trace, sourceFile, destFile)){
				if (!sourceFile.delete()) {
					trace.trace("File was copied, but failed to delete the original file.",TraceLevel.ERROR_WARN);
				}
				trace.trace("Copy-delete sequence done.",TraceLevel.TRACE);					
				return true;
		    }else{
				trace.trace("Failed to move or copy the file to the destiantion.",TraceLevel.ERROR_WARN);					
		    	return false;
		    }
		}
/* Java 7 move not yet supported on all environments.
		try {
			// first try the renameTo method
			trace.trace("Moving file from "+sourceFile.getAbsolutePath()+" to "+destFile.getAbsolutePath(),TraceLevel.TRACE);
			Files.move(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			return true;
		}catch (Exception e) {
			try{
				trace.trace("Non-atomic move from "+sourceFile.getAbsolutePath()+" to "+destFile.getAbsolutePath(),TraceLevel.TRACE);
				Files.move(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				return true;
			}catch (Exception e2) {
				Util.printStackTrace(trace, e2);
				return false;
			}			
		}
			*/
			/*
			if (sourceFile.renameTo(destFile)) {
				trace.trace("Moved via java native.",TraceLevel.TRACE);
				return true;
			}
			else 
			{
				//assume linux. Call mv external
				trace.trace("Trying via unix: mv " + sourceFile.getAbsolutePath()+" "+destFile.getAbsolutePath(),TraceLevel.TRACE);
				Process p = Runtime.getRuntime().exec(new String[]{"/bin/sh","-c", "mv",sourceFile.getAbsolutePath(),destFile.getAbsolutePath()});
				trace.trace("Output from mv");
				String line;
				BufferedReader input =
				       new BufferedReader
				         (new InputStreamReader(p.getInputStream()));
				     while ((line = input.readLine()) != null) {
				       System.out.println(line);
				       }
				     input.close();
				     trace.trace("Waiting for mv",TraceLevel.TRACE);
	   			p.waitFor();
						
	   			trace.trace("Return code: "+p.exitValue(),TraceLevel.TRACE);
				// try to copy file, delete original
				//copyFile(sourceFile, destFile, true, true);
				//sourceFile.delete();
			}
			return true;
		} catch (IOException e) {
			trace.trace("IO Exception during moveFile "+sourceFile+" - "+destFile+" : "+  e.getMessage());
			return false;
		} catch (InterruptedException e) {
			trace.trace("InterruptedException during moveFile "+sourceFile+" - "+destFile+" : "+  e.getMessage());
			return false;
		}*/
	}


	/**
	 * Append the exception cause to the given status element
	 * @param statusElem
	 * @param paramThrowable
	 * @param printStackTrace
	 */
	static void appendCause(XDSStatusElement statusElem, Throwable paramThrowable, boolean printStackTrace) {
		if (paramThrowable != null) {
			Throwable cause = paramThrowable.getCause();
			if (cause != null) {
				DocumentImpl paramDocumentImpl = statusElem.documentImpl();
				Element paramElement = statusElem.domElement();
				
				ExceptionElement exceptionElem = new ExceptionElement(paramDocumentImpl, paramElement);
				exceptionElem.attributeValueSet("class-name",cause.getClass().getName());
				exceptionElem.messageAppend(cause);
				if (printStackTrace)
					exceptionElem.stackTraceAppend(cause);
			}
		}
	}

	/**
	 * Append the statusAttributes to the statusDoc and return the created statusElement
	 * @param statusDoc
	 * @param statusAttr
	 * @param description
	 * @return
	 */
	public static XDSStatusElement appendStatus(StatusDocument statusDoc, StatusAttributes statusAttr, String description) {
		XDSStatusElement statusElem = statusDoc.appendStatusElement();
		if (statusAttr != null) {
			statusElem.setLevel(statusAttr.getLevel());
			statusElem.setType(statusAttr.getType());
			statusElem.setEventID(statusAttr.getEventID());
		}
		statusElem.descriptionAppend(description);
		return statusElem;
	}


	/**
	 * Append the given statusattributes to the status document.
	 * 
	 * @param statusDoc
	 * @param statusAttr
	 * @param description
	 * @param paramException
	 * @param printStackTrace
	 * @param paramXmlDocument
	 * @return
	 */
	public static XDSStatusElement appendStatus(StatusDocument statusDoc, StatusAttributes statusAttr, String description,
			Throwable paramException, boolean printStackTrace, XmlDocument paramXmlDocument) {
		if (statusDoc != null) {
	  		XDSStatusElement statusElem = null;
	  		//Special processing for XDSParameterException: loop 
	  		if ((paramException instanceof XDSParameterException))
	  		{
	  			for (XDSParameterException localXDSParameterException = (XDSParameterException)paramException;
					localXDSParameterException != null; 
					localXDSParameterException = localXDSParameterException.getNext())
				{
					statusElem = appendStatus(statusDoc, statusAttr, localXDSParameterException.getMessage());
				}
				if ((statusElem != null) && (paramXmlDocument != null))
					statusElem.documentAppend(paramXmlDocument);
	  		}
	  		else
	  		{
	  			//Make sure we have a decent description
	  			if (description == null)
	  				description = paramException==null?null:paramException.toString();
	  			//Append the status element
	  			statusElem = appendStatus(statusDoc,statusAttr,description);
	  			//append the exception and the cause
	  			statusElem.exceptionAppend(paramException, printStackTrace);
	  			appendCause(statusElem, paramException, printStackTrace);
	  			statusElem.documentAppend(paramXmlDocument);
	  		}
			return statusElem;
		}
		return null;
	}

	public static XDSQueryDocument createQueryDoc(String className, String destDN, String association, QueryScope scope, Collection<String> readAttr, Map<String, String> matchAttributes) {
		XDSQueryDocument query = new XDSQueryDocument();
		XDSQueryElement queryElem = query.appendQueryElement();
		if (className != null)
			queryElem.setClassName(className);
		if (destDN != null)
			queryElem.setDestDN(destDN);
		if (scope != null)
			queryElem.setScope(scope);
		if (association != null)
			queryElem.appendAssociationElement(association);
		if (readAttr != null)
			for (String string : readAttr) {
				XDSReadAttrElement readAttrs = queryElem.appendReadAttrElement();
				readAttrs.setAttrName(string);			
			}
		else
			//adding an empty readattr element will make it read nothing
			queryElem.appendReadAttrElement();
		
		if (matchAttributes!= null){
			Set<String> fields = matchAttributes.keySet(); 
			for (Iterator<String> iterator = fields.iterator(); iterator.hasNext();) {
				String aFieldName = (String) iterator.next();
				String value = matchAttributes.get(aFieldName);
				XDSSearchAttrElement search = queryElem.appendSearchAttrElement();
				search.setAttrName(aFieldName);
				search.appendValueElement(value);
			}
		}
		return query;
	}

	/**
	 *  A non-interface method used to format trace values.
	 */
	public static String toLiteral(String string)
	{
	    return (string == null) ? null : "'" + string + "'";
	}//toLiteral(String):String

}
