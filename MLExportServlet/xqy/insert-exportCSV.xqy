let $module := '

let $uris := xdmp:get-request-field("uris")
let $module-name := xdmp:get-request-field("moduleName")

let $output-content :=
  for $uri in fn:tokenize($uris, ",")
  let $output-row := xdmp:invoke($module-name, (xs:QName("URIS"), $uri) )
  return $output-row

let $filename := xdmp:get-request-field( "filename", "download_file.csv" )
let $contentType := xdmp:get-request-field( "type", "application/x-download" )
return
(
  xdmp:set-response-content-type( $contentType ),
  xdmp:add-response-header("Content-Disposition", fn:concat("filename=", $filename)),
  xdmp:set-response-encoding("UTF-8"),
  $output-content
)'

return xdmp:document-insert("/exportCSV.xqy", document {$module} )