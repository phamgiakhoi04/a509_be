package com.A509.Service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.time.*;
import java.sql.Timestamp;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Service
public class TranslationService {
 public record Source(String table,String id,Map<String,String> fields) {}
 public static final Map<String,Source> SOURCES=Map.of(
  "ARTICLE",new Source("articles","article_id",fields("title","title","excerpt","excerpt","content","content")),
  "UNIFORM",new Source("uniforms","uniform_id",fields("name","name","description","description","history","history","material","material")),
  "COUNTRY",new Source("countries","country_id",fields("countryName","country_name","continent","continent","description","description")),
  "UNIFORM_CATEGORY",new Source("uniform_categories","category_id",fields("categoryName","category_name","description","description")),
  "CATEGORY",new Source("categories","category_id",fields("name","name","description","description")),
  "IMAGE",new Source("images","image_id",fields("description","description")),
  "ARTICLE_IMAGE",new Source("article_images","article_image_id",fields("description","description")));
 private final JdbcTemplate jdbc;
 public TranslationService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 private static Map<String,String> fields(String...args){Map<String,String> m=new LinkedHashMap<>();for(int i=0;i<args.length;i+=2)m.put(args[i],args[i+1]);return Collections.unmodifiableMap(m);}
 private Source source(String type){Source s=SOURCES.get(type);if(s==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown translation resource");return s;}
 private String select(Source s){return s.id+" as id,"+String.join(",",s.fields.entrySet().stream().map(e->e.getValue()+" as "+e.getKey()).toList());}
 public Map<String,Object> catalog(String type,int requestedPage){
  Source s=source(type);int page=Math.max(0,requestedPage);
  var items=jdbc.queryForList("select "+select(s)+" from "+s.table+(type.equals("ARTICLE")?" where deleted_at is null":"")+" order by "+s.id+" desc limit 25 offset ?",(long)page*25).stream().map(row->canonical(s,row)).toList();
  long total=Optional.ofNullable(jdbc.queryForObject("select count(*) from "+s.table+(type.equals("ARTICLE")?" where deleted_at is null":""),Long.class)).orElse(0L);
  return Map.of("items",items,"total",total,"page",page,"size",25,"fields",s.fields.keySet());
 }
 public Map<String,Object> read(String type,long id){
  Source s=source(type);var rows=jdbc.queryForList("select "+select(s)+" from "+s.table+" where "+s.id+"=?",id);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  var original=canonical(s,rows.getFirst());var translations=new LinkedHashMap<String,String>();var stale=new ArrayList<String>();
  for(var row:jdbc.queryForList("select field_name,translated_text,source_hash from content_translations where resource_type=? and resource_id=? and language='en'",type,id)){
   String field=(String)row.get("field_name");translations.put(field,(String)row.get("translated_text"));
   if(!hash(text(original.get(field))).equals(row.get("source_hash")))stale.add(field);
  }
  return Map.of("source",original,"translations",translations,"stale",stale,"fields",s.fields.keySet());
 }
 public record Update(Map<String,String> fields,Map<String,String> source) {}
 @Transactional
 public void save(String type,long id,Update update){
  Source s=source(type);if(update==null||update.fields==null||update.source==null||!s.fields.keySet().containsAll(update.fields.keySet()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid translation fields");
  @SuppressWarnings("unchecked") Map<String,Object> current=(Map<String,Object>)read(type,id).get("source");
  for(var entry:update.fields.entrySet()){
   String field=entry.getKey(),value=entry.getValue();
   if(!text(current.get(field)).equals(text(update.source.get(field))))throw new ResponseStatusException(HttpStatus.CONFLICT,"Nội dung gốc đã thay đổi. Hãy tải lại trước khi lưu bản dịch.");
   int max=Set.of("title","name","countryName","categoryName").contains(field)?500:200000;
   if(value!=null&&value.length()>max)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Translation too long");
   jdbc.update("delete from content_translations where resource_type=? and resource_id=? and field_name=? and language='en'",type,id,field);
   if(value!=null&&!value.isBlank())jdbc.update("insert into content_translations(resource_type,resource_id,field_name,language,translated_text,source_hash,updated_at) values(?,?,?,'en',?,?,?)",type,id,field,value.trim(),hash(text(current.get(field))),Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)));
  }
 }
 public Map<String,Map<Long,Map<String,Map<String,Object>>>> load(Map<String,Set<Long>> ids){
  Map<String,Map<Long,Map<String,Map<String,Object>>>> result=new HashMap<>();
  ids.forEach((type,values)->{if(values.isEmpty())return;List<Object> args=new ArrayList<>();args.add(type);args.addAll(values);
   for(var row:jdbc.queryForList("select resource_id,field_name,translated_text,source_hash from content_translations where resource_type=? and language='en' and resource_id in ("+String.join(",",Collections.nCopies(values.size(),"?"))+")",args.toArray()))
    result.computeIfAbsent(type,k->new HashMap<>()).computeIfAbsent(((Number)row.get("resource_id")).longValue(),k->new HashMap<>()).put((String)row.get("field_name"),row);
  });return result;
 }
 public String localized(String type,long id,String field,String original,boolean english,Map<String,Map<Long,Map<String,Map<String,Object>>>> loaded){
  if(!english)return original;
  var row=loaded.getOrDefault(type,Map.of()).getOrDefault(id,Map.of()).get(field);
  return row!=null&&hash(text(original)).equals(row.get("source_hash"))?(String)row.get("translated_text"):original;
 }
 public static String text(Object o){return o==null?"":o.toString();}
 private Map<String,Object> canonical(Source s,Map<String,Object> row){Map<String,Object> result=new LinkedHashMap<>();result.put("id",row.get("id"));s.fields.keySet().forEach(k->result.put(k,row.get(k)));return result;}
 public static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
