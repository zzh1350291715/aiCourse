// src/main/java/com/onlinestudy/controller/CourseOutlineController.java
package com.onlinestudy.controller;

import com.onlinestudy.dto.CourseOutlineGenerationRequest;
import com.onlinestudy.dto.CourseOutlineResponseDto;
import com.onlinestudy.service.CourseOutlineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/course-outlines")
public class CourseOutlineController {

    private final CourseOutlineService outlineService;

    public CourseOutlineController(CourseOutlineService outlineService) {
        this.outlineService = outlineService;
    }

    /** 自动生成大纲，需要 studentId 和 courseId */
    @PostMapping("/generate")
    public ResponseEntity<CourseOutlineResponseDto> generateOutline(
            @RequestParam Long studentId,
            @Valid @RequestBody CourseOutlineGenerationRequest req) {

        // 传入两个实参：studentId, courseId
        CourseOutlineResponseDto dto = outlineService.generateOutline(studentId, req.getCourseId());
        return ResponseEntity.ok(dto);
    }

    /** 基于已有大纲，提供智能“修改建议” */
    @PostMapping("/{outlineId}/suggest")
    public ResponseEntity<CourseOutlineResponseDto> suggestOutline(
            @RequestParam Long studentId,
            @PathVariable Long outlineId,
            @RequestBody Map<String, Object> editedOutline) {

        CourseOutlineResponseDto dto = outlineService.suggestOutline(
                studentId,
                outlineId,
                editedOutline
        );
        return ResponseEntity.ok(dto);
    }
}
