package com.onlinestudy.controller;

import com.onlinestudy.domain.CourseMaterial;
import com.onlinestudy.dto.CourseMaterialDto;
import com.onlinestudy.service.CourseMaterialService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course-materials")
@CrossOrigin(origins = "http://localhost:8080", maxAge = 3600)
public class CourseMaterialController {

    private final CourseMaterialService materialService;

    public CourseMaterialController(CourseMaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping
    public ResponseEntity<CourseMaterial> addMaterial(@RequestBody CourseMaterialDto materialDto) {
        CourseMaterial newMaterial = materialService.addMaterialToCourse(materialDto);
        return new ResponseEntity<>(newMaterial, HttpStatus.CREATED);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId,
            @RequestParam("title") String title,
            @RequestParam("type") String type,
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestParam(value = "order", defaultValue = "0") int order) {

        try {
            System.out.println("收到上传请求:");
            System.out.println("courseId: " + courseId);
            System.out.println("title: " + title);
            System.out.println("type: " + type);
            System.out.println("description: " + description);
            System.out.println("fileName: " + file.getOriginalFilename());
            System.out.println("fileSize: " + file.getSize());

            // 参数验证
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "文件不能为空"));
            }

            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "标题不能为空"));
            }

            if (type == null || type.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "类型不能为空"));
            }

            CourseMaterial uploadedMaterial = materialService.uploadMaterial(file, courseId, title, type, order);
            return new ResponseEntity<>(uploadedMaterial, HttpStatus.CREATED);
        } catch (Exception e) {
            System.err.println("上传失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "上传失败: " + e.getMessage()));
        }
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseMaterial>> getMaterialsByCourse(@PathVariable Long courseId) {
        List<CourseMaterial> materials = materialService.getMaterialsForCourse(courseId);
        return ResponseEntity.ok(materials);
    }

    @PutMapping("/{materialId}")
    public ResponseEntity<CourseMaterial> updateMaterial(@PathVariable Long materialId, @RequestBody CourseMaterialDto materialDto) {
        CourseMaterial updatedMaterial = materialService.updateMaterial(materialId, materialDto);
        return ResponseEntity.ok(updatedMaterial);
    }

    @DeleteMapping("/{materialId}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long materialId) {
        materialService.deleteMaterial(materialId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/download/{materialId}")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long materialId) {
        try {
            CourseMaterial material = materialService.getMaterial(materialId);
            String url = material.getContentUrl();

            // 解析文件系统中的实际路径
            String relativePath;
            if (url.startsWith("http")) {
                int idx = url.indexOf("/uploads/");
                relativePath = idx != -1 ? url.substring(idx + "/uploads/".length()) : url;
            } else if (url.startsWith("/uploads/")) {
                relativePath = url.substring("/uploads/".length());
            } else {
                relativePath = url;
            }
            Path filePath = Paths.get("uploads").resolve(relativePath).normalize();
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String filename = material.getTitle();
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
