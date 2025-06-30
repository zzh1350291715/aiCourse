package com.onlinestudy.domain;

/**
 * 授课大纲生成状态
 */
public enum GenerationStatus {
    GENERATED,   // 初版已生成
    SUGGESTED,   // 已应用智能修改建议
    EDITED       // 人工编辑过
}
