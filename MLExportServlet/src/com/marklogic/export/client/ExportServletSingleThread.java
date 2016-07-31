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
@WebServlet("/ExportServletSingleThread")
public class ExportServletSingleThread extends HttpServlet {
	private static final long serialVersionUID = 1L;	
	private String xccURL = null;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ExportServletSingleThread() {
        super();
        
		Context env;
		xccURL = null;
		
		int urlCount = 0;		
		try {
			env = (Context)new InitialContext().lookup("java:comp/env");
			urlCount = 1;
			
			List<String> urls = new ArrayList<String>();
			for(int i=1; i<=urlCount; i++){
				xccURL = (String)env.lookup("ml.xcc.url"+i);
				urls.add(xccURL);
			}
			for(int i=0; i<urls.size(); i++) {
				System.out.println(urls.get(i));
			}
			
			System.out.println("ml-url="+xccURL);
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
		
		xccHelper xcc = new xccHelper();
		String output = null;
		try {
			output = xcc.getMLResults(uris, xccURL, mlModuleName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		response.getOutputStream().write(output.getBytes());
		
		java.util.Date end = new java.util.Date();
		System.out.println("End time: "+ end.getTime());
		long diffInMilliseconds = (end.getTime() - start.getTime());
		String footer = "Runtime in Millisconds: "+ diffInMilliseconds;
		System.out.println(footer);
		
		os.flush();
		os.close();
	}
}
