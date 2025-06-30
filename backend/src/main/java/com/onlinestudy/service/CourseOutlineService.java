// src/main/java/com/onlinestudy/service/CourseOutlineService.java
package com.onlinestudy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinestudy.domain.Course;
import com.onlinestudy.domain.CourseOutline;
import com.onlinestudy.domain.CourseOutline.GenerationStatus;
import com.onlinestudy.dto.CourseOutlineResponseDto;
import com.onlinestudy.dto.AiChatResponse;
import com.onlinestudy.repository.CourseOutlineRepository;
import com.onlinestudy.repository.CourseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Service
public class CourseOutlineService {

    private final CourseOutlineRepository outlineRepo;
    private final CourseRepository courseRepo;
    private final ObjectMapper mapper;
    private final AiChatService aiChatService;

    public CourseOutlineService(CourseOutlineRepository outlineRepo,
                                CourseRepository courseRepo,
                                ObjectMapper mapper,
                                AiChatService aiChatService) {
        this.outlineRepo = outlineRepo;
        this.courseRepo = courseRepo;
        this.mapper = mapper;
        this.aiChatService = aiChatService;
    }

    /**
     * 自动生成初版大纲
     *
     * @param studentId 调用 AI 的学生 ID
     * @param courseId  目标课程 ID
     */
    /**
     * 自动生成初版大纲
     */
    public CourseOutlineResponseDto generateOutline(Long studentId, Long courseId) {
        // 1. 根据 courseId 获取 Course 实体
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + courseId));

        // 2. 构造更严谨的 Prompt，要求只返回 JSON
        String prompt = String.format(
                "请基于课程《%s》及其描述，自动生成一份授课大纲，" +
                        "并且只返回一个 JSON 对象，不要包含任何额外文字。JSON 结构示例：\n" +
                        "{\n" +
                        "  \"chapters\": [ { \"title\": \"...\", \"topics\": [\"...\"], \"duration\": \"...\" }, ... ],\n" +
                        "  \"learningObjectives\": [\"...\", ...],\n" +
                        "  \"timeAllocation\": { \"章节1\": \"1h\", ... }\n" +
                        "}",
                course.getTitle()
        );
        AiChatResponse aiResponse = aiChatService.askQuestion(studentId, courseId, prompt);
        String aiJson = aiResponse.getAnswer().trim();
        // 打印原始返回，便于排查
        System.out.println("AI raw response for outline: " + aiJson);

        try {
            // 3. 先将 AI 返回的 JSON 解析为通用 Map
            Map<String, Object> result = mapper.readValue(
                    aiJson, new TypeReference<Map<String, Object>>() {}
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> chapters = (List<Map<String, Object>>) result.get("chapters");
            @SuppressWarnings("unchecked")
            List<String> objectives = (List<String>) result.get("learningObjectives");
            @SuppressWarnings("unchecked")
            Map<String, String> timeAlloc = (Map<String, String>) result.get("timeAllocation");

            // 4. 创建并初始化 CourseOutline 实体
            CourseOutline outline = new CourseOutline();
            outline.setCourse(course);
            outline.setTitle(course.getTitle());
            outline.setDescription(course.getDescription());
            outline.setChapters(mapper.writeValueAsString(chapters));
            outline.setLearningObjectives(mapper.writeValueAsString(objectives));
            outline.setTimeAllocation(mapper.writeValueAsString(timeAlloc));
            outline.setStatus(GenerationStatus.GENERATED);
            outline.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
            outline.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

            // 5. 持久化并转换为 DTO 返回
            outline = outlineRepo.save(outline);
            return toDto(outline);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "AI 返回的内容无法解析为 JSON，请检查 Prompt 或 AI 服务实现。原始内容：" + aiJson, e
            );
        } catch (Exception ex) {
            throw new RuntimeException("生成大纲时发生错误", ex);
        }
    }


    /**
     * 基于已有大纲，提供智能“修改建议”
     *
     * @param studentId      调用 AI 的学生 ID
     * @param outlineId      现有大纲实体 ID
     * @param editedOutline  前端编辑后的大纲结构（Map/JSON）
     */
    public CourseOutlineResponseDto suggestOutline(Long studentId,
                                                   Long outlineId,
                                                   Map<String, Object> editedOutline) {
        // 1. 加载原始大纲
        CourseOutline outline = outlineRepo.findById(outlineId)
                .orElseThrow(() -> new EntityNotFoundException("Outline not found: " + outlineId));

        try {
            // 2. 序列化用户编辑结果
            String editedJson = mapper.writeValueAsString(editedOutline);

            // 3. 构造提示并调用 AI
            String prompt = "这是我编辑后的课程大纲 JSON：" + editedJson
                    + "，请基于它给出优化后的章节结构，返回 JSON 数组。";
            AiChatResponse aiResponse = aiChatService.askQuestion(
                    studentId,
                    outline.getCourse().getId(),
                    prompt
            );
            String aiJson = aiResponse.getAnswer();

            // 4. 解析 AI 的优化结果
            List<Map<String, Object>> optimized = mapper.readValue(
                    aiJson, new TypeReference<>() {}
            );

            // 5. 更新并持久化
            outline.setChapters(mapper.writeValueAsString(optimized));
            outline.setStatus(GenerationStatus.GENERATED);
            outline.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
            outline = outlineRepo.save(outline);

        } catch (Exception ex) {
            throw new RuntimeException("处理智能建议失败", ex);
        }

        // 6. 转 DTO 返回
        return toDto(outline);
    }

    /**
     * 手写 + BeanUtils 将 Entity 转 DTO，剥离关联和 JSON 字段
     */
    private CourseOutlineResponseDto toDto(CourseOutline entity) {
        CourseOutlineResponseDto dto = new CourseOutlineResponseDto();

        // 复制同名普通字段，跳过关联和 JSON
        BeanUtils.copyProperties(
                entity, dto,
                "course", "chapters", "learningObjectives", "timeAllocation"
        );
        dto.setCourseId(entity.getCourse().getId());

        try {
            dto.setChapters(mapper.readValue(
                    entity.getChapters(),
                    new TypeReference<List<Map<String, Object>>>() {}
            ));
            dto.setLearningObjectives(mapper.readValue(
                    entity.getLearningObjectives(),
                    new TypeReference<List<String>>() {}
            ));
            dto.setTimeAllocation(mapper.readValue(
                    entity.getTimeAllocation(),
                    new TypeReference<Map<String, String>>() {}
            ));
        } catch (Exception ex) {
            throw new RuntimeException("解析大纲 JSON 失败", ex);
        }

        return dto;
    }
}
