package com.marklogic.export.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import com.marklogic.xcc.*;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.exceptions.XccConfigException;
import com.marklogic.xcc.types.XName;
import com.marklogic.xcc.types.XdmValue;
import com.marklogic.xcc.types.XdmVariable;

public class xccHelper {
	
	public xccHelper() {
	}

	public String getMLResults(String URI, String xccURL, String mlModuleName) throws Exception {
		
		StringBuffer sb = null;
		
		try {
			ContentSource contentSource = ContentSourceFactory.newContentSource(new URI(xccURL));
			contentSource.setAuthenticationPreemptive(true);
			Session session = contentSource.newSession();
			Request request = session.newModuleInvoke(mlModuleName);
			
			// create an unnamed xs:string value
			XdmValue value = ValueFactory.newXSString(URI);

			// create a new XName object referencing the above namespace
			XName xname = new XName("", "URIS");

			// Create a Variable (name + value) instance
			XdmVariable myVariable = ValueFactory.newVariable(xname, value);

			// bind the Variable to the Request
			request.setVariable (myVariable);
			
			ResultSequence rs = session.submitRequest(request);
			
			String result = rs.asString();			
//			System.out.println(result);
			
			sb = new StringBuffer(result);
			sb.append("\n");	
			
		} catch (XccConfigException | URISyntaxException | RequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
		return sb.toString();
	}
}