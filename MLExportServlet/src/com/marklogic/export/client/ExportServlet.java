package com.marklogic.export.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.marklogic.xcc.*;
import com.marklogic.xcc.types.XName;
import com.marklogic.xcc.types.XdmValue;
import com.marklogic.xcc.types.XdmVariable;

/**
 * Servlet implementation class ExportServlet
 */
@WebServlet("/ExportServlet")
public class ExportServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private int MAX_THREADS;
	private int batchsize;
	private String xccURL = null;
	private String mlConfigURI = null;
	private List<String> xccURLs;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ExportServlet() {
		super();

		Context env;
		xccURL = null;

		int urlCount = 0;
		try {
			env = (Context) new InitialContext().lookup("java:comp/env");
			urlCount = (Integer) env.lookup("ml.xcc.urlcount");

			xccURLs = new ArrayList<String>();
			for (int i = 1; i <= urlCount; i++) {
				xccURL = (String) env.lookup("ml.xcc.url" + i);
				xccURLs.add(xccURL);
			}
			for (int i = 0; i < xccURLs.size(); i++) {
				System.out.println("xccURL" + i + ": " + xccURLs.get(i));
			}

		} catch (NamingException e1) {
			e1.printStackTrace();
		}
	}

	public void init(ServletConfig config) throws ServletException {
		Context env;
		int urlCount = 0;
		try {
			env = (Context) new InitialContext().lookup("java:comp/env");
			urlCount = (Integer) env.lookup("ml.xcc.urlcount");
			MAX_THREADS = (Integer) env.lookup("ml.xcc.max_threads");
			mlConfigURI = (String) env.lookup("ml.config.uri");
			batchsize = (Integer) env.lookup("ml.xcc.batchsize");
			
			System.out.println("urlCount:" + urlCount);
			System.out.println("MAX_THREADS:" + MAX_THREADS);
			System.out.println("mlConfigURI:" + mlConfigURI);
			System.out.println("batchsize:" + batchsize);

			xccURLs = new ArrayList<String>();
			for (int i = 1; i <= urlCount; i++) {
				xccURL = (String) env.lookup("ml.xcc.url" + i);
				xccURLs.add(xccURL);
			}
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		OutputStream out = response.getOutputStream();
		out.write("standard response to get request".getBytes("UTF-8"));
		out.flush();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		try {
			java.util.Date start = new java.util.Date();
			System.out.println("Start time: " + start.getTime());
			
			// get request parameters
			//String mlModuleName = request.getParameter("moduleName");
			String reportName = request.getParameter("reportName");
			String mlModuleName = getReportModuleURI(reportName);
			
			String mlParams = request.getParameter("mlParams");
			System.out.println("mlParams:" + mlParams);
			
			String inputURIs = request.getParameter("uris");			
			String queryString = request.getParameter("q");
			
			if(queryString != null && !queryString.equals("") ) {
				inputURIs = getURIsByQueryString(queryString);
			}
			
			//System.out.println("inputURIs:" + inputURIs);
			String groupedURIs = getURIsGroupedByForest(inputURIs);
			//System.out.println("groupedURIs:" + groupedURIs);			
			String[] uriList = inputURIs.split(",");
			int uriCount = uriList.length;
			System.out.println("uriCount: "+uriCount);
			String reportType = getReportType(mlModuleName);
			String filename = reportName+ "-"+ uriCount + "." + reportType;
			
			response.setContentType("application/vnd.ms-excel");
			response.setHeader("Content-Disposition",
					"attachment; filename="+filename);
			
			ServletOutputStream os = response.getOutputStream();
			String header = getReportHeader(mlModuleName, "csv");
			os.write(header.getBytes());
			String uriBatchString = null;
			
			ContentSource contentSource = ContentSourceFactory.newContentSource(new URI(xccURL));
			contentSource.setAuthenticationPreemptive(true);
	
			ExecutorService exec = Executors.newFixedThreadPool(MAX_THREADS);
			ExecutorCompletionService<String> compServ = new ExecutorCompletionService<String>(exec);
			// build URI csvVar
			int threadNumber = 0;
	
			for (int j = 0; j < uriList.length; j = j + batchsize) {
				StringBuffer uriBatch = new StringBuffer();
				for (int k = 0; (k < batchsize && (j + k) < uriList.length); k++) {
					if (k > 0) {
						uriBatch.append(",").append(uriList[j + k]);
					} else {
						uriBatch.append(uriList[j + k]);
					}
				}
				uriBatchString = uriBatch.toString();
				threadNumber++;
				System.out.println("threadNumber:" + threadNumber);
				int xccURLIndex = threadNumber % xccURLs.size();
				System.out.println("xccURLIndex:" + xccURLIndex);
				xccURL = xccURLs.get(xccURLIndex);
				System.out.println("xccURL:" + xccURL);
				GetOutput_Callable task = new GetOutput_Callable(contentSource, uriBatchString,
						threadNumber, xccURL, mlModuleName, mlParams);
				compServ.submit(task);
			}
	
			for (int j = 0; j < uriList.length; j = j + batchsize) {
				Future<String> future;
				try {
					future = compServ.take();
					String output = (String) future.get();
					response.getOutputStream().write(output.getBytes());
	
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
			exec.shutdown();
			while (!exec.isTerminated()) {
				// do nothing
			}
	
			java.util.Date end = new java.util.Date();
			System.out.println("End time: " + end.getTime());
			long diffInMilliseconds = (end.getTime() - start.getTime());
			String footer = "Runtime in Milliseconds: " + diffInMilliseconds;
			//String footer = getReportFooter(mlModuleName);
			System.out.println(footer);
			os.write(footer.getBytes());
	
			os.flush();
			os.close();
		
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private String getURIsByQueryString(String queryString) throws Exception {
		  Map<String, String> params = new HashMap<String, String>();
		  params.put("queryString", queryString);
		  String MLoutput = callMLModule("/get-uris-by-query-string.xqy", params);
		  return MLoutput;
	}

	private String callMLModule(String moduleURI, Map<String, String> args)
			throws Exception {
		String mlResponse = null;
		ContentSource cs;

		cs = ContentSourceFactory.newContentSource(new URI(xccURL));
		cs.setAuthenticationPreemptive(true);
		Session session = cs.newSession();
		Request request = session.newModuleInvoke(moduleURI);

		for (Entry<String, String> entry : args.entrySet()) {
			String mapKey = entry.getKey();
			String mapValue = entry.getValue();
			XdmValue value = ValueFactory.newXSString(mapValue);
			XName xname = new XName("", mapKey);
			XdmVariable myVar = ValueFactory.newVariable(xname, value);
			request.setVariable(myVar);
		}

		ResultSequence rs = session.submitRequest(request);
		mlResponse = rs.asString();

		return mlResponse;
	}

	private String getReportModuleURI(String reportName) throws Exception {
		  Map<String, String> params = new HashMap<String, String>();
		  params.put("REPORT-NAME", reportName);
		  String reportModuleURI = callMLModule(mlConfigURI, params);
		  return reportModuleURI;  
		}

	private String getReportHeader(String moduleURI, String rptType) throws Exception {
		  Map<String, String> params = new HashMap<String, String>();
		  params.put("OUTPUT-TYPE", "header");
		  String output = callMLModule(moduleURI, params);
		  if(rptType.equals("csv")) {
		    output = output + "\n";
		  }
		  return output;
		}

	private String getReportFooter(String moduleURI) throws Exception {
		  Map<String, String> params = new HashMap<String, String>();
		  params.put("OUTPUT-TYPE", "footer");
		  String output = callMLModule(moduleURI, params);
		  return output;
		}

	private String getReportType(String moduleURI) throws Exception {
		  Map<String, String> params = new HashMap<String, String>();
		  params.put("OUTPUT-TYPE", "type");
		  String output = callMLModule(moduleURI, params);
		  return output;
		}
	
	private String getURIsGroupedByForest(String inputURIstring) throws Exception {
		  Map<String, String> params = new HashMap<String, String>();
		  params.put("URIS", inputURIstring);
		  String MLoutput = callMLModule("/group-uris-by-forest.xqy", params);
		  String groupedOutput = buildNewURIOrderedList(MLoutput);
		  return groupedOutput;
	}
	private String buildNewURIOrderedList(String inputURIstringFromML) {
		int batchSize = batchsize;
		
		String[] forestGroups = inputURIstringFromML.split(";");
		int forestCount = forestGroups.length;
		StringBuffer sb = new StringBuffer();
		Boolean done = false;
		String delim = ",";
		while(!done) {
			for(int i=0; i<forestCount; i++) {
				String group = forestGroups[i];
				String[] batchValues = group.split(",");
				StringBuffer stringToRemove = new StringBuffer();
				for(int j=0; (j<batchSize && j<batchValues.length && group.length()>0); j++) {
					String value = batchValues[j];
					sb.append(value).append(delim);
					if(j+1 == batchValues.length) {
						stringToRemove.append(value);						
					} else {
						stringToRemove.append(value).append(delim);
					}

				}
				forestGroups[i] = forestGroups[i].replaceAll(stringToRemove.toString(), "");
			}
			// check to see if all the arrays are empty
			done = true;  // reset to false if any is not empty
			for(int i=0; i<forestCount; i++) {
				String group = forestGroups[i];
				if(!group.equals("")) {
					done = false;
					break;
				}
			}
		}

		String finalValues = sb.toString().substring(0, sb.toString().length()-1);
		return finalValues;
	}
}
