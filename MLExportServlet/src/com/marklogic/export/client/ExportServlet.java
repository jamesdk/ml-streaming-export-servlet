package com.marklogic.export.client;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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

/**
 * Servlet implementation class ExportServlet
 */
@WebServlet("/ExportServlet")
public class ExportServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;	
	private static int MAX_THREADS = 16;
	private String xccURL = null;
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
			env = (Context)new InitialContext().lookup("java:comp/env");
			urlCount = (Integer)env.lookup("ml.xcc.urlcount");
			
			xccURLs = new ArrayList<String>();
			for(int i=1; i<=urlCount; i++){
				xccURL = (String)env.lookup("ml.xcc.url"+i);
				xccURLs.add(xccURL);
			}
			for(int i=0; i<xccURLs.size(); i++) {
				System.out.println("xccURL"+i+": "+xccURLs.get(i));
			}
			
		} catch (NamingException e1) {
			e1.printStackTrace();
		}
    }
    
    public void init(ServletConfig config) throws ServletException {
    	
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		OutputStream out = response.getOutputStream();
		out.write("standard response to get request".getBytes("UTF-8"));
		out.flush();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		java.util.Date start = new java.util.Date();
		System.out.println("Start time: "+ start.getTime());
		response.setContentType("application/vnd.ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=download.csv");
		// get request parameters
		String mlModuleName = request.getParameter("moduleName");
		String uris = request.getParameter("uris");
		System.out.println("URIs:"+uris);
		String[] uriList = uris.split(",");
		ServletOutputStream os = response.getOutputStream();
		os.write("header".getBytes());
		int batchsize = 10;
		String uriBatchString = null;

		ExecutorService exec = Executors.newFixedThreadPool(MAX_THREADS);
		ExecutorCompletionService<String> compServ = new ExecutorCompletionService<String>(exec);
		// build URI csvVar
		int threadNumber = 0;
		
		for(int j=0; j<uriList.length; j=j+batchsize) {
			StringBuffer uriBatch = new StringBuffer();
			for(int k=0; (k < batchsize && (j+k) < uriList.length); k++) {
				if(k > 0) {
					uriBatch.append(",").append(uriList[j+k]);
				}
				else {
					uriBatch.append(uriList[j+k]);
				}
			}
			uriBatchString = uriBatch.toString();
			threadNumber++;
			System.out.println("threadNumber:"+threadNumber);
			int xccURLIndex = threadNumber % xccURLs.size();
			System.out.println("xccURLIndex:"+xccURLIndex);
			xccURL = xccURLs.get(xccURLIndex);
			System.out.println("xccURL:"+xccURL);
			GetOutput_Callable task = new GetOutput_Callable(uriBatchString, threadNumber, xccURL, mlModuleName);
			compServ.submit(task);
		}
		
		for(int j=0; j<uriList.length; j=j+batchsize) {
			Future<String> future;
			try {
				future = compServ.take();
				String output = (String)future.get();
				response.getOutputStream().write(output.getBytes());
				
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		exec.shutdown();
		while(!exec.isTerminated()) {
			// do nothing
		}
		
		java.util.Date end = new java.util.Date();
		System.out.println("End time: "+ end.getTime());
		long diffInMilliseconds = (end.getTime() - start.getTime());
		String footer = "Runtime in Millisconds: "+ diffInMilliseconds;
		System.out.println(footer);
		
		os.flush();
		os.close();
	}
}
