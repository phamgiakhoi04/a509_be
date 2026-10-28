package com.A509.Config;
import com.A509.Service.TranslationService;
import tools.jackson.databind.*;
import tools.jackson.databind.node.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import java.util.*;

/** Translate only text fields already authorized and serialized by the content APIs. */
@ControllerAdvice
public class TranslationResponseAdvice implements ResponseBodyAdvice<Object> {
 private final ObjectMapper mapper;private final TranslationService service;
 public TranslationResponseAdvice(ObjectMapper mapper,TranslationService service){this.mapper=mapper;this.service=service;}
 public boolean supports(MethodParameter param,Class<? extends HttpMessageConverter<?>> converter){return true;}
 public Object beforeBodyWrite(Object body,MethodParameter param,MediaType media,Class<? extends HttpMessageConverter<?>> converter,ServerHttpRequest request,ServerHttpResponse response){
  String path=request.getURI().getPath();
  if(body==null||body instanceof String||request.getMethod()!=HttpMethod.GET||!"en".equals(request.getHeaders().getFirst("X-A509-Language"))||path.contains("/admin"))return body;
  if(path.equals("/api/activity")) {
   JsonNode activityTree=mapper.valueToTree(body);
   Set<Long> articleIds=new HashSet<>();for(JsonNode item:activityTree)if(item.path("articleId").isNumber())articleIds.add(item.path("articleId").asLong());
   var titles=service.load(Map.of("ARTICLE",articleIds));
   for(JsonNode item:activityTree)if(item instanceof ObjectNode object&&object.path("articleId").isNumber()){
    String title=service.localized("ARTICLE",object.path("articleId").asLong(),"title",object.path("articleTitle").asText(),true,titles);
    object.put("articleTitle",title);object.put("description",switch(object.path("action").asText()){case "CREATE" -> "New article: ";case "ADD_IMAGE" -> "Images added: ";default -> "Updated: ";}+title);
   }
   response.getHeaders().add("Vary","X-A509-Language");return activityTree;
  }
  String root=(path.startsWith("/api/articles/images") || path.matches("/api/articles/[^/]+/images"))?"ARTICLE_IMAGE":path.startsWith("/api/articles")?"ARTICLE":path.startsWith("/api/uniform-categories")?"UNIFORM_CATEGORY":path.startsWith("/api/uniforms")?"UNIFORM":path.startsWith("/api/countries")?"COUNTRY":path.startsWith("/api/categories")?"CATEGORY":path.startsWith("/api/images")?"IMAGE":null;
  if(root==null)return body;
  response.getHeaders().add("Vary","X-A509-Language");
  JsonNode tree=mapper.valueToTree(body);List<Map.Entry<String,ObjectNode>> nodes=new ArrayList<>();collect(tree,root,nodes);
  Map<String,Set<Long>> ids=new HashMap<>();for(var item:nodes)ids.computeIfAbsent(item.getKey(),k->new HashSet<>()).add(item.getValue().path("id").asLong());
  var loaded=service.load(ids);
  for(var item:nodes){String type=item.getKey();ObjectNode node=item.getValue();long id=node.path("id").asLong();int required=0,complete=0;
   for(String field:TranslationService.SOURCES.get(type).fields().keySet()){
    JsonNode value=node.get(field);if(value==null||!value.isString()||value.asText().isBlank())continue;
    required++;String original=value.asText();String translated=service.localized(type,id,field,original,true,loaded);
    var row=loaded.getOrDefault(type,Map.of()).getOrDefault(id,Map.of()).get(field);
    if(row!=null&&TranslationService.hash(original).equals(row.get("source_hash")))complete++;
    if(Set.of("name","countryName","categoryName").contains(field))node.put("sourceName",original);
    node.put(field,translated);
   }
   node.put("translationStatus",required==complete?"complete":complete==0?"missing":"partial");
  }
  return tree;
 }
 private void collect(JsonNode node,String type,List<Map.Entry<String,ObjectNode>> nodes){
  if(node.isArray()){for(JsonNode child:node)collect(child,type,nodes);return;}
  if(!(node instanceof ObjectNode object))return;
  if(object.has("id")&&object.path("id").isNumber()&&TranslationService.SOURCES.containsKey(type))nodes.add(Map.entry(type,object));
  for(var entry:object.properties()){
   String nested=switch(entry.getKey()){
    case "country" -> "COUNTRY";
    case "category","periodCategory" -> type.equals("ARTICLE")?"CATEGORY":"UNIFORM_CATEGORY";
    case "categories" -> "CATEGORY";
    case "images" -> type.equals("ARTICLE")?"ARTICLE_IMAGE":"IMAGE";
    default -> type;
   };if(entry.getValue().isContainer())collect(entry.getValue(),nested,nodes);
  }
 }
}
