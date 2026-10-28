package com.A509.Service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.text.Normalizer;
import java.util.*;

@Service
public class SearchService {
 private final JdbcTemplate jdbc;private final TranslationService translations;
 public SearchService(JdbcTemplate jdbc,TranslationService translations){this.jdbc=jdbc;this.translations=translations;}
 public record Result(String key,String type,String title,String description,String href,int rank) {}
 private record Document(long id,String resource,String type,Map<String,String> fields,String href) {}
 public static String normalize(String value){return Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).trim();}
 public static String slug(String value){return normalize(value).replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");}
 public Object search(String query,String filter,int requestedPage,int requestedSize,boolean english){
  if(query==null)query="";query=query.trim();if(query.length()>120)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Từ khóa tối đa 120 ký tự.");
  if(!Set.of("all","article","uniform","reenactment","country","force","period").contains(filter))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid search type");
  int page=Math.max(0,Math.min(1000,requestedPage)),size=Math.max(1,Math.min(50,requestedSize));
  if(query.isBlank())return Map.of("items",List.of(),"total",0,"page",page,"size",size);
  List<Document> documents=new ArrayList<>();
  // Public index excludes drafts/deleted articles, user data and image URLs.
  jdbc.query("select article_id,title,excerpt,content,slug from articles where status='PUBLISHED' and deleted_at is null",(org.springframework.jdbc.core.RowCallbackHandler)rs->{documents.add(new Document(rs.getLong("article_id"),"ARTICLE","article",fields("title",rs.getString("title"),"excerpt",rs.getString("excerpt"),"content",rs.getString("content")),"/tin-tuc/"+rs.getString("slug")));});
  jdbc.query("select country_id,country_name,continent,description from countries",(org.springframework.jdbc.core.RowCallbackHandler)rs->{documents.add(new Document(rs.getLong("country_id"),"COUNTRY","country",fields("countryName",rs.getString("country_name"),"continent",rs.getString("continent"),"description",rs.getString("description")),"/hoat-dong/phuc-dung/"+slug(rs.getString("country_name"))));});
  jdbc.query("select u.uniform_id,u.name,u.description,u.history,u.material,u.period,c.country_name,cat.category_name,cat.category_type,p.slug as period_slug from uniforms u join countries c on c.country_id=u.country_id left join uniform_categories cat on cat.category_id=u.category_id left join uniform_categories p on p.category_id=u.period_category_id",(org.springframework.jdbc.core.RowCallbackHandler)rs->{
   boolean reenactment="REENACTMENT".equals(rs.getString("category_type"));String href="/tai-lieu/quan-trang/"+rs.getLong("uniform_id");
   if(reenactment){String period=rs.getString("period_slug");if(period==null||period.isBlank())period=rs.getString("period");href="/hoat-dong/phuc-dung/"+slug(rs.getString("country_name"))+"/"+slug(rs.getString("category_name"))+(period==null||period.isBlank()?"":"/"+slug(period))+"#content-"+rs.getLong("uniform_id");}
   documents.add(new Document(rs.getLong("uniform_id"),"UNIFORM",reenactment?"reenactment":"uniform",fields("name",rs.getString("name"),"description",rs.getString("description"),"history",rs.getString("history"),"material",rs.getString("material")),href));
  });
  jdbc.query("select cat.category_id,cat.category_name,cat.description,cat.slug,p.category_name as parent_name,p.parent_id,c.country_name from uniform_categories cat join uniform_categories p on p.category_id=cat.parent_id join countries c on c.country_id=coalesce(cat.country_id,p.country_id) where cat.category_type='REENACTMENT'",(org.springframework.jdbc.core.RowCallbackHandler)rs->{
   boolean period=rs.getObject("parent_id")!=null;String path="/hoat-dong/phuc-dung/"+slug(rs.getString("country_name"));if(period)path+="/"+slug(rs.getString("parent_name"));String segment=period?rs.getString("slug"):null;path+="/"+(segment==null||segment.isBlank()?slug(rs.getString("category_name")):segment);
   documents.add(new Document(rs.getLong("category_id"),"UNIFORM_CATEGORY",period?"period":"force",fields("categoryName",rs.getString("category_name"),"description",rs.getString("description")),path));
  });
  Map<String,Set<Long>> ids=new HashMap<>();documents.forEach(d->ids.computeIfAbsent(d.resource,k->new HashSet<>()).add(d.id));var loaded=translations.load(ids);
  String needle=normalize(query);String[] terms=needle.split("\\s+");List<Result> matches=new ArrayList<>();
  for(var d:documents){if(!filter.equals("all")&&!filter.equals(d.type))continue;Map<String,String> localized=new LinkedHashMap<>();StringBuilder searchable=new StringBuilder();
   d.fields.forEach((field,value)->{String translated=translations.localized(d.resource,d.id,field,value,true,loaded);localized.put(field,english?translated:value);searchable.append(' ').append(value).append(' ').append(translated);});
   String haystack=normalize(searchable.toString());if(!Arrays.stream(terms).allMatch(haystack::contains))continue;
   String title=localized.getOrDefault("title",localized.getOrDefault("name",localized.getOrDefault("countryName",localized.getOrDefault("categoryName",""))));String description=localized.getOrDefault("excerpt",localized.getOrDefault("description",""));if(description.isBlank())description=localized.getOrDefault("content","");
   description=description.replaceAll("<[^>]+>"," ").replaceAll("\\s+"," ").trim();if(description.length()>240)description=description.substring(0,240)+"…";
   int rank=normalize(title).equals(needle)?100:normalize(title).startsWith(needle)?75:normalize(title).contains(needle)?50:10;matches.add(new Result(d.resource+"-"+d.id,d.type,title,description,d.href,rank));
  }
  matches.sort(Comparator.comparingInt(Result::rank).reversed().thenComparing(Result::title).thenComparing(Result::key));int start=Math.min(matches.size(),page*size);return Map.of("items",matches.subList(start,Math.min(matches.size(),start+size)),"total",matches.size(),"page",page,"size",size);
 }
 private Map<String,String> fields(String...values){Map<String,String> result=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)result.put(values[i],values[i+1]==null?"":values[i+1]);return result;}
}
