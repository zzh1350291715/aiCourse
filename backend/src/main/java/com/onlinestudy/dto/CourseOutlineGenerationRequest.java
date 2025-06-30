package com.onlinestudy.dto;

import jakarta.validation.constraints.NotNull;

public class CourseOutlineGenerationRequest {

    @NotNull
    private Long courseId;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
}
