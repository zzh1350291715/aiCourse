// src/main/java/com/onlinestudy/dto/CourseOutlineResponseDto.java
package com.onlinestudy.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CourseOutlineResponseDto {
    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private List<Map<String, Object>> chapters;
    private List<String> learningObjectives;
    private Map<String, String> timeAllocation;
    private String status;
    private Double estimatedHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---------- getters & setters ----------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Map<String, Object>> getChapters() { return chapters; }
    public void setChapters(List<Map<String, Object>> chapters) { this.chapters = chapters; }
    public List<String> getLearningObjectives() { return learningObjectives; }
    public void setLearningObjectives(List<String> learningObjectives) { this.learningObjectives = learningObjectives; }
    public Map<String, String> getTimeAllocation() { return timeAllocation; }
    public void setTimeAllocation(Map<String, String> timeAllocation) { this.timeAllocation = timeAllocation; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
