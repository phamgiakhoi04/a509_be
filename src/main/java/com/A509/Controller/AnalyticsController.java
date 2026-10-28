package com.A509.Controller;
import com.A509.Service.AnalyticsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
@RestController
public class AnalyticsController {
    private final AnalyticsService analytics;
    public AnalyticsController(AnalyticsService analytics) { this.analytics=analytics; }
    @PostMapping("/api/analytics/pageviews")
    public ResponseEntity<?> record(@RequestBody AnalyticsService.Visit visit,HttpServletRequest request) {
        analytics.record(visit,request);return ResponseEntity.noContent().build();
    }
    @GetMapping("/api/admin/overview")
    public Object overview(@RequestParam(defaultValue="30") int days) { return analytics.overview(days); }
}
