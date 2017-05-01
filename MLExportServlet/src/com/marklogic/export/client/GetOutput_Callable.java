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

public class GetOutput_Callable implements Callable<String> {
	
	private String URI;
	private int threadNumber;
	private String result;
	private String xccURL;
	private String mlModuleName;
	private String mlParams;
	
	private ContentSource contentSource;
	
	public GetOutput_Callable(ContentSource contentSource, String URI, int threadNumber, String xccURL, String mlModuleName, String mlParams) {
		this.URI = URI;
		this.threadNumber = threadNumber;
		this.xccURL = xccURL;
		this.mlModuleName = mlModuleName;
		this.contentSource = contentSource;
		this.mlParams = mlParams;
	}
	
	public String get() {
		return result;
	}
	
	public void setResult() {
		this.result = result;
	}

	@Override
	public String call() throws Exception {
		
		StringBuffer sb = null;
		
		try {
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
			
			
			// create an unnamed xs:string value
			XdmValue value2 = ValueFactory.newXSString("mainReport");

			// create a new XName object referencing the above namespace
			XName xname2 = new XName("", "OUTPUT-TYPE");

			// Create a Variable (name + value) instance
			XdmVariable myVariable2 = ValueFactory.newVariable(xname2, value2);

			// bind the Variable to the Request
			request.setVariable (myVariable2);			
			
			// pass the ML params to the Xquery Module
			if(mlParams == null) {
				mlParams = "";
			}
			XdmValue value3 = ValueFactory.newXSString(mlParams);
			XName xname3 = new XName("", "MLPARAMS");
			XdmVariable mlParamsVar = ValueFactory.newVariable(xname3, value3);
			request.setVariable(mlParamsVar);			
			
			ResultSequence rs = session.submitRequest(request);
			
			String result = rs.asString();			
			
			sb = new StringBuffer(result);
			sb.append("\n");	
			
		//} catch (XccConfigException | URISyntaxException | RequestException e) {
		} catch (RequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
		return sb.toString();
	}
}