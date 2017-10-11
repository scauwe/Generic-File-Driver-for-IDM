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
package info.vancauwenberge.filedriver.util;


import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.novell.nds.dirxml.driver.Trace;
import com.novell.soa.script.mozilla.javascript.Context;
import com.novell.soa.script.mozilla.javascript.ImporterTopLevel;
import com.novell.soa.script.mozilla.javascript.Scriptable;
import com.novell.soa.script.mozilla.javascript.ScriptableObject;

public class EcmascriptBuilder {
	private ScriptableObject sharedScope = null;
	private JsProcessing jsProcessingMode = null;
	private String jsIdentifierReplacement = null;
	
	public EcmascriptBuilder(JsProcessing jsProcessingMode, String jsIdentifierReplacement){
		this.jsProcessingMode = jsProcessingMode;
		this.jsIdentifierReplacement = jsIdentifierReplacement;

		Context cx = Context.enter();
		//sharedScope = cx.initStandardObjects();
		//ImporterTopLevel imports importClass and importPackage
		sharedScope = new ImporterTopLevel(cx);
		Context.exit();
	}

	/**
	 * Add the given library (name/content) to the shared ecma evaluator scope.
	 * @param libContent The actual ecma script
	 * @param libName The name of the script (used in error reporting)
	 */
	public void addSharedLibrary(String libContent, String libName){
		Context cx = Context.enter();
		cx.evaluateString(sharedScope, libContent, libName, 1, null);//1=lineNumber				
		Context.exit();
	}
	
	
	/**
	 * What to do with the javascript identifier names (based on the schema names)
	 * 
	 */
	public enum JsProcessing{
		NOPROCESSING,REPLACE;
	}

	private static final String javascriptIdentifierRegExpStartStr = "^[^\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}_$]";
	private static final String javascriptIdentifierRegExpStr = "^[^\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}\\p{Mn}\\p{Mc}\\p{Nd}\\p{Pc}\u200C\u200D_$]";

	private static final Pattern javascriptIdentifierRegexp = Pattern.compile(javascriptIdentifierRegExpStr);
	private static final Pattern javascriptIdentifierRegexpStart = Pattern.compile(javascriptIdentifierRegExpStartStr);

	private String convert2JavascriptIdentifier(final Trace trace, final String name){
		if (jsProcessingMode==JsProcessing.NOPROCESSING)
			return name;
		
		/*
		 * An identifier must start with $, _, or any character in the Unicode categories 
		 * “Uppercase letter (Lu)”, 
		 * “Lowercase letter (Ll)”, 
		 * “Titlecase letter (Lt)”, 
		 * “Modifier letter (Lm)”, 
		 * “Other letter (Lo)”, or 
		 * “Letter number (Nl)”.
		 * 
		 * The rest of the string can contain the same characters, plus 
		 * any U+200C zero width non-joiner characters, 
		 * U+200D zero width joiner characters, 
		 * and characters in the Unicode categories 
		 * “Non-spacing mark (Mn)”, 
		 * “Spacing combining mark (Mc)”, 
		 * “Decimal digit number (Nd)”, or 
		 * “Connector punctuation (Pc)”.
		 */
		//replace the first character if needed
		Matcher matcher = javascriptIdentifierRegexpStart.matcher(name);
		String resultName = matcher.replaceAll(jsIdentifierReplacement);
		//replace in the string if needed.
		matcher = javascriptIdentifierRegexp.matcher(resultName);
		resultName =  matcher.replaceAll(jsIdentifierReplacement);
		if (trace.getTraceLevel()>=TraceLevel.DEBUG){
			if (!resultName.equals(name))
				trace.trace(new StringBuilder("Converted field ").append(name).append(" to javascript field ").append(resultName).toString());
		}
		return resultName;
	}

	/**
	 * Evaluate the given ECMA script
	 * @param trace
	 * @param jsProcessingMode
	 * @param jsIdentifierReplacement
	 * @param paramMap
	 * @param ecmaScript
	 * @param scriptName
	 * @return
	 */
	public Object evaluateEcmaWithParams(final Trace trace, final Map<String,String> paramMap, final String ecmaScript, final String scriptName) {
		//Create a context. Note: contexts are thread specific and not expensive to create.
		//See https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Scopes_and_Contexts
		final Context cx = Context.enter();
		final Object result;
		try{
			final Scriptable scope = getScope(cx);
			//Append all parameters, escaping as needed.
			for (Iterator<String> iterator = paramMap.keySet().iterator(); iterator.hasNext();) {
				final String key = iterator.next();
				scope.put(convert2JavascriptIdentifier(trace, key), scope, paramMap.get(key));
			}
			result = cx.evaluateString(scope, ecmaScript, scriptName, 1, null);
		}finally{
			//Context enter/exit is not expensive, the scope creation is. For this, we use a shared scope. See getScope()
			Context.exit();
		}
		return result;
	}

	/**
	 * Create a scope with the shared scope as the prototype
	 * @param cx
	 * @return
	 */
	private Scriptable getScope(final Context cx) {
		Scriptable newScope = cx.newObject(sharedScope);
		newScope.setPrototype(sharedScope);
		newScope.setParentScope(null);
		return newScope;
	}
	
}
