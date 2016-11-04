xquery version "1.0-ml";
 
let $module := '
<reports>
	<report>
	  <name>TEST</name>
	  <process-module-uri>/get-output-row-by-URIs.xqy</process-module-uri>
	</report>
</reports>
'
return xdmp:document-insert("/report-config.xqy", document {$module} )