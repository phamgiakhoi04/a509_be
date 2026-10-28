package com.A509.Controller;
import com.A509.Service.SearchService;
import org.springframework.web.bind.annotation.*;
@RestController
public class SearchController {
 private final SearchService search;
 public SearchController(SearchService search){this.search=search;}
 @GetMapping("/api/search") public Object search(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="all") String type,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestHeader(value="X-A509-Language",defaultValue="vi") String language){return search.search(q,type,page,size,language.equals("en"));}
}
