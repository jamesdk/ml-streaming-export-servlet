xquery version "1.0-ml";
 
let $module := '
xquery version "1.0-ml";

declare variable $URIS as xs:string external;
declare variable $OUTPUT-TYPE as xs:string external;

declare function local:get-output-row($uri) {
  let $doc1 := fn:doc($uri)
  let $field1 := $uri
  let $md5-doc-hash := xdmp:md5($doc1)
  let $_ := xdmp:sleep(1)
  let $field2 := xs:string(xdmp:random(10000))
  let $fields := ($field1, $field2)
  let $output-row := fn:string-join($fields, ",")
  return $output-row
};

declare function local:get-header() {
	"Header Column 1"
};
declare function local:get-footer() {
	"Footer Column 1"
};
declare function local:process-URIs($URIs) {
	for $uri in fn:tokenize($URIS, ",")
	return local:get-output-row($uri)
};

let $output :=
  switch($OUTPUT-TYPE)
   	case "type" return "csv"
	case "header" return local:get-header()
	case "footer" return local:get-footer()
	default return local:process-URIs($URIS)
	
return $output
'
return xdmp:document-insert("/get-output-row-by-URIs.xqy", document {$module} )