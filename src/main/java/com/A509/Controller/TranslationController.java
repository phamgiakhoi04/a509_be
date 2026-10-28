package com.A509.Controller;
import com.A509.Service.TranslationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestController @RequestMapping("/api/admin/translations")
public class TranslationController {
 private final TranslationService translations;
 public TranslationController(TranslationService translations){this.translations=translations;}
 @GetMapping("/{type}") public Object catalog(@PathVariable String type,@RequestParam(defaultValue="0") int page){return translations.catalog(type,page);}
 @GetMapping("/{type}/{id}") public Object read(@PathVariable String type,@PathVariable long id){return translations.read(type,id);}
 @PutMapping("/{type}/{id}") public ResponseEntity<?> save(@PathVariable String type,@PathVariable long id,@RequestBody TranslationService.Update update){translations.save(type,id,update);return ResponseEntity.noContent().build();}
}
