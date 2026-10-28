package com.A509.Controller;

import com.A509.DTO.ActivityLogDTO;
import com.A509.Service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(
            ActivityLogService activityLogService
    ) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<List<ActivityLogDTO>> getLatestActivities() {

        return ResponseEntity.ok(
                activityLogService.getLatestActivities()
        );
    }
}
