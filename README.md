MarkLogic Streaming Export Servlet
==============

Java web servlet that allows web app user to export large datasets from ML to a CSV/Excel spreadsheet.
The results are streamed to the browser as they are retrived from the ML database via XCC requests.

The basic idea is that large reports can be exported more quickly if the work is spread across the cluster in concurrent threads as in the example below:

<img src="StreamingExportDiagram.jpg"  />

## Setup

### Configuration

The servlet was developed and tested using a deployment running on Tomcat 8.

The web.xml file must be modified to point to 1 or more XDBC app servers linked to your database.  The entries in the file shown below can be added to or expanded to support the number of App Servers and/or hosts you would like to distribute the requests across.

In the example below, note the first setting defines how many XCC URLs to look for and below that the XCC URLs are defined.

Other configuration items include the following:
ml.xcc.batchsize - defines the number of URIs for the export report module to process at one time
ml.xcc.max_threads - defines the maximum number of threads for the Export Servlet's ThreadPool (typically corresponds to the XDBC AppServer max threads).
ml.config.uri - defines the initial module in the modules DB for the Export Servlet connection

web.xml
```
<env-entry> 
    <env-entry-name>ml.xcc.urlcount</env-entry-name>
    <env-entry-type>java.lang.Integer</env-entry-type>
    <env-entry-value>2</env-entry-value> 
</env-entry>
  
<env-entry> 
    <env-entry-name>ml.xcc.url1</env-entry-name>
    <env-entry-type>java.lang.String</env-entry-type>
    <env-entry-value>xdbc://username:password@localhost:9003/samples</env-entry-value> 
</env-entry>

<env-entry> 
    <env-entry-name>ml.xcc.url2</env-entry-name>
    <env-entry-type>java.lang.String</env-entry-type>
    <env-entry-value>xdbc://username:password@localhost:9004/samples</env-entry-value> 
</env-entry>

<env-entry>
    <env-entry-name>ml.xcc.batchsize</env-entry-name>
    <env-entry-type>java.lang.Integer</env-entry-type>
    <env-entry-value>10</env-entry-value>
</env-entry>

<env-entry>
    <env-entry-name>ml.xcc.max_threads</env-entry-name>
    <env-entry-type>java.lang.Integer</env-entry-type>
    <env-entry-value>28</env-entry-value>
</env-entry>
	
<env-entry>
    <env-entry-name>ml.config.uri</env-entry-name>
    <env-entry-type>java.lang.String</env-entry-type>
    <env-entry-value>/get-report-config.xqy</env-entry-value>
</env-entry>

```

### Dependencies
The Export Servlet requires [marklogic-xcc-8.0.*.jar](https://developer.marklogic.com/products/xcc) or later to run. Please note that marklogic-xcc 8 is backwards compatible up to MarkLogic 5 and runs on Java 1.6 or later.

Once the configuration changes are made, you're ready to build a WAR and deploy it to your app server (or just run it in Eclipse for testing, etc).


## Testing

There are a couple of ways to test the servlet with your own database.

### Option 1

With the servlet running, open [http://localhost:8080/MLExportServlet/demo.html](http://localhost:8080/MLExportServlet/demo.html)

<img src="demo.png" width="350" height="210" />

This page provides the option of testing the performance of building your CSV export file in either 1) a Single Thread and then prompting the user to open/save the file, or 2) running a multi-threaded process that will stream the results to the CSV as they are retrived from the database

Both option require that you set the following variables in the demo.html code:
a) Replace the uris in the line below with a real comma-separated URIs:
```
<input type="hidden" name="uris" value="uri1,uri2,uri3,uri4,uri5,uri6,uri7,uri8,uri9,uri10"/>
```
b) Replace the value of the module name with a module you already have on your XDBC App Server referenced in web.xml:
```
<input type="hidden" name="moduleName" value="get-output-row-by-URIs.xqy"/>
```

### Option 2
Add the 3 xquery modules below to the modules database used by your XDBC App Server referenced in web.xml
-insert-get-report-config.xqy
-insert-exportCSV.xqy
-insert-get-output-row-by-URIs.xqy
-insert-getURIs.xqy  ** modify this file first to use a query to pull relavent URIs on your test database

Once these are loaded, open [http://localhost:8080/MLExportServlet/startDemo.html](http://localhost:8080/MLExportServlet/startDemo.html) in your browser.

<img src="startDemo.png" width="390" height="100" />

Note: to get this feature to work, you'll have to set up an HTTP App Server on your ML instance to return the results of getURIs to the browser.  It requires more configuration, but it does give you the flexibility to quickly pull different counts of URIs to test the servlet performance.

<img src="getURIs.png" width="350" height="234" />


