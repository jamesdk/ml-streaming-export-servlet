xquery version "1.0-ml";
 
let $module := '
xquery version "1.0-ml";

declare variable $REPORT-NAME as xs:string external;

xdmp:invoke("/report-config.xqy")//report[name eq $REPORT-NAME]/process-module-uri/text()
'
return xdmp:document-insert("/get-report-config.xqy", document {$module} )