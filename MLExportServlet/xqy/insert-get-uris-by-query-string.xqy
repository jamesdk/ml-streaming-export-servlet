let $module := 'xquery version "1.0-ml";

declare variable $queryString as xs:string external;
let $paramMap := map:map()

let $params := fn:tokenize($queryString, ";")
let $_set_params :=
  for $param in $params
  let $param-tokens := fn:tokenize($param, ":")
  let $param-name := $param-tokens[1]
  let $param-value := $param-tokens[2]
  return map:put($paramMap, $param-name, $param-value)


let $coll-name := map:get($paramMap,"coll")
let $uri-count := map:get($paramMap,"limit")

let $query := cts:collection-query($coll-name)
let $uris := cts:uris( (), (fn:concat("limit=",$uri-count)), $query)
return fn:string-join(($uris),",")'

return xdmp:document-insert("/get-uris-by-query-string.xqy", document {$module} )