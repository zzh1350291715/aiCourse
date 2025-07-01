package com.onlinestudy.service;

import com.onlinestudy.domain.Course;
import com.onlinestudy.domain.CourseMaterial;
import com.onlinestudy.dto.CourseMaterialDto;
import com.onlinestudy.repository.CourseMaterialRepository;
import com.onlinestudy.repository.CourseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public interface CourseMaterialService {
    CourseMaterial addMaterialToCourse(CourseMaterialDto materialDto);
    CourseMaterial uploadMaterial(MultipartFile file, Long courseId, String title, String type, int order);
    List<CourseMaterial> getMaterialsForCourse(Long courseId);
    CourseMaterial updateMaterial(Long materialId, CourseMaterialDto materialDto);
    CourseMaterial getMaterial(Long materialId);
    void deleteMaterial(Long materialId);
}

@Service
class CourseMaterialServiceImpl implements CourseMaterialService {

    private final CourseMaterialRepository materialRepository;
    private final CourseRepository courseRepository;

    public CourseMaterialServiceImpl(CourseMaterialRepository materialRepository, CourseRepository courseRepository) {
        this.materialRepository = materialRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    @Override
    public CourseMaterial addMaterialToCourse(CourseMaterialDto materialDto) {
        Course course = courseRepository.findById(materialDto.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + materialDto.getCourseId()));

        CourseMaterial newMaterial = new CourseMaterial();
        newMaterial.setCourse(course);
        newMaterial.setTitle(materialDto.getTitle());
        newMaterial.setMaterialType(materialDto.getMaterialType());
        newMaterial.setContentUrl(materialDto.getContentUrl());
        newMaterial.setOrder(materialDto.getOrder());

        return materialRepository.save(newMaterial);
    }

    @Transactional
    @Override
    public CourseMaterial uploadMaterial(MultipartFile file, Long courseId, String title, String type, int order) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        try {
            // 创建上传目录
            String uploadDir = "uploads/course-materials/" + courseId;
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // 保存文件
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath);

            // 创建材料记录
            CourseMaterial material = new CourseMaterial();
            material.setCourse(course);
            material.setTitle(title);
            
            // 安全地转换材料类型
            try {
                material.setMaterialType(CourseMaterial.MaterialType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException e) {
                material.setMaterialType(CourseMaterial.MaterialType.OTHER);
            }
            
            material.setContentUrl("/uploads/course-materials/" + courseId + "/" + uniqueFilename);
            material.setOrder(order);
            material.setFileSize(file.getSize());

            return materialRepository.save(material);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public List<CourseMaterial> getMaterialsForCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new EntityNotFoundException("Course not found with id: " + courseId);
        }
        return materialRepository.findByCourse_IdOrderByOrderAsc(courseId);
    }

    @Transactional
    @Override
    public CourseMaterial updateMaterial(Long materialId, CourseMaterialDto materialDto) {
        CourseMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new EntityNotFoundException("CourseMaterial not found with id: " + materialId));

        material.setTitle(materialDto.getTitle());
        material.setMaterialType(materialDto.getMaterialType());
        material.setContentUrl(materialDto.getContentUrl());
        material.setOrder(materialDto.getOrder());

        return materialRepository.save(material);
    }

    @Override
    public CourseMaterial getMaterial(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new EntityNotFoundException("CourseMaterial not found with id: " + materialId));
    }

    @Transactional
    @Override
    public void deleteMaterial(Long materialId) {
        CourseMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new EntityNotFoundException("CourseMaterial not found with id: " + materialId));
        
        // 删除实际文件
        if (material.getContentUrl() != null && !material.getContentUrl().isEmpty()) {
            try {
                String filePath = material.getContentUrl().replace("/uploads/", "uploads/");
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    Files.delete(path);
                }
            } catch (IOException e) {
                // 记录错误但不阻止删除数据库记录
                System.err.println("Failed to delete file: " + material.getContentUrl() + ", error: " + e.getMessage());
            }
        }
        
        // 删除数据库记录
        materialRepository.deleteById(materialId);
    }
}