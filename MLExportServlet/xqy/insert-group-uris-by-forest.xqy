xquery version "1.0-ml";
 
let $module := '
xquery version "1.0-ml";
import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy";

declare variable $URIS as xs:string external;

let $config := admin:get-configuration()
let $forest-ids := admin:database-get-attached-forests($config, xdmp:database("samples"))

let $uri-list := fn:tokenize($URIS, ",")

let $groups :=
  for $forest-id in $forest-ids
  return fn:string-join( ( cts:uris( (), (), cts:document-query($uri-list), (), $forest-id) ), ",")
  
return fn:string-join($groups, ";")
'
return xdmp:document-insert("/group-uris-by-forest.xqy", document {$module} )