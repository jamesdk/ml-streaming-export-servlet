let $module := '
let $request-doc-count := xdmp:get-request-field("docCount")
let $report-name := "TEST"

let $my-query := cts:and-query((
                      cts:collection-query("socialMedia"),
                      cts:element-value-query(xs:QName("displayName"), "Twitter"),
                      cts:not-query(cts:element-value-query(xs:QName("platform"), ("disqus", "intense-debate")[1]))
                  ))               
let $limit-string := "limit="||$request-doc-count                  
let $uris := cts:uris( (), ($limit-string), $my-query)
let $uri-list := fn:string-join($uris, ",")

let $html-output :=
<html>
<head><title>Export Demo</title></head>
<body>
  <div>{fn:count($uris)} uris selected to export.  <a href="http://localhost:8080/MLExportServlet/startDemo.html">Change number</a></div>
  <br/>
  <div>Export using single request to build CSV</div>
    <form action="http://localhost:8080/MLExportServlet/ExportServletSingleThread" method="post">
    <input type="hidden" name="uris" value="{$uri-list}"/>
    <input type="hidden" name="reportName" value="{$report-name}"/>
    <input type="submit" value="Export (Single Thread)" />
  </form>
  <br/><br/>
  <div>Export using multi-threaded request to build CSV using URIs:</div>
  <form action="http://localhost:8080/MLExportServlet/ExportServlet" method="post">
    <input type="hidden" name="uris" value="{$uri-list}"/>
    <input type="hidden" name="reportName" value="{$report-name}"/>
    <input type="submit" value="Export (Multi-Thread)" />
  </form>
  <br/><br/>
  <div>Export using multi-threaded request to build CSV using search text</div>
  <form action="http://localhost:8080/MLExportServlet/ExportServlet" method="post">
    <input type="hidden" name="q" value="coll:socialMedia;limit:1000"/>
    <input type="hidden" name="reportName" value="{$report-name}"/>
    <input type="submit" value="Export (Multi-Thread)" />
  </form>
</body>
</html>

return (
  xdmp:set-response-content-type("text/html; charset=utf-8"),
  $html-output
)'

return xdmp:document-insert("/getURIs.xqy", document {$module} )