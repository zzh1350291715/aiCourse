<template>
  <div class="ai-generation">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-info">
        <h1>
          <i class="el-icon-cpu"></i>
          AI智能生成
        </h1>
        <p class="header-desc">基于知识库内容，智能生成课程授课大纲、PPT纲要和考试试卷</p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.back()">
          <i class="el-icon-arrow-left"></i>
          返回
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <!-- 课程大纲生成 -->
      <el-tab-pane label="课程大纲" name="outline">
        <el-card class="generation-card">
          <div slot="header">
            <span>自动生成课程授课大纲</span>
            <el-button 
              style="float: right; padding: 3px 0" 
              type="text"
              @click="generateOutline"
              :loading="generating.outline"
            >
              <i class="el-icon-magic-stick"></i>
              生成大纲
            </el-button>
          </div>

          <!-- 生成选项 -->
          <el-form :model="outlineOptions" label-width="120px" class="generation-options">
            <el-form-item label="知识库选择">
              <el-select v-model="outlineOptions.knowledgeBaseId" placeholder="请选择知识库">
                <el-option 
                  v-for="kb in knowledgeBases" 
                  :key="kb.id" 
                  :label="kb.name" 
                  :value="kb.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="预计课时">
              <el-input-number v-model="outlineOptions.estimatedHours" :min="1" :max="200" />
              <span style="margin-left: 10px;">小时</span>
            </el-form-item>
          </el-form>

          <!-- 生成结果 -->
          <div v-if="generatedOutline" class="generation-result">
            <h3>生成的课程大纲</h3>
            <div class="outline-content">
              <div class="outline-info">
                <p><strong>标题：</strong>{{ generatedOutline.title }}</p>
                <p><strong>描述：</strong>{{ generatedOutline.description }}</p>
                <p><strong>预计课时：</strong>{{ generatedOutline.estimatedHours }}小时</p>
              </div>
              
              <div class="chapters-section">
                <h4>章节安排</h4>
                <el-timeline>
                  <el-timeline-item 
                    v-for="(chapter, index) in parsedChapters" 
                    :key="index"
                    :timestamp="chapter.duration"
                  >
                    <div class="chapter-item">
                      <h5>{{ chapter.title }}</h5>
                      <ul>
                        <li v-for="topic in chapter.topics" :key="topic">{{ topic }}</li>
                      </ul>
                    </div>
                  </el-timeline-item>
                </el-timeline>
              </div>

              <div class="objectives-section">
                <h4>学习目标</h4>
                <el-tag 
                  v-for="objective in parsedObjectives" 
                  :key="objective"
                  class="objective-tag"
                >
                  {{ objective }}
                </el-tag>
              </div>

              <div class="actions">
                <el-button type="primary" @click="saveOutline">保存大纲</el-button>
                <el-button @click="editOutline">编辑大纲</el-button>
                <el-button
                    type="warning"
                    @click="suggestOutline"
                    :loading="generating.suggest"
                >获取智能建议</el-button>
                <el-button type="success" @click="exportOutline">导出大纲</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- PPT纲要生成 -->
      <el-tab-pane label="PPT纲要" name="ppt">
        <el-card class="generation-card">
          <div slot="header">
            <span>自动生成PPT页面结构纲要</span>
            <el-button 
              style="float: right; padding: 3px 0" 
              type="text"
              @click="generatePPT"
              :loading="generating.ppt"
            >
              <i class="el-icon-present"></i>
              生成PPT纲要
            </el-button>
          </div>

          <!-- 生成选项 -->
          <el-form :model="pptOptions" label-width="120px" class="generation-options">
            <el-form-item label="知识库选择">
              <el-select v-model="pptOptions.knowledgeBaseId" placeholder="请选择知识库">
                <el-option 
                  v-for="kb in knowledgeBases" 
                  :key="kb.id" 
                  :label="kb.name" 
                  :value="kb.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="章节名称">
              <el-input v-model="pptOptions.chapterName" placeholder="请输入章节名称" />
            </el-form-item>
          </el-form>

          <!-- PPT纲要结果 -->
          <div v-if="generatedPPT" class="generation-result">
            <h3>生成的PPT纲要</h3>
            <div class="ppt-content">
              <div class="ppt-info">
                <p><strong>标题：</strong>{{ generatedPPT.title }}</p>
                <p><strong>章节：</strong>{{ generatedPPT.chapterName }}</p>
                <p><strong>幻灯片数量：</strong>{{ generatedPPT.slideCount }}张</p>
                <p><strong>预计时长：</strong>{{ generatedPPT.estimatedDuration }}分钟</p>
              </div>

              <div class="slides-section">
                <h4>幻灯片结构</h4>
                <el-collapse v-model="activeSlides">
                  <el-collapse-item 
                    v-for="(slide, index) in parsedSlides" 
                    :key="index"
                    :title="`幻灯片${slide.number}: ${slide.title}`"
                    :name="index.toString()"
                  >
                    <div class="slide-detail">
                      <p><strong>类型：</strong>{{ slide.type }}</p>
                      <p><strong>内容：</strong>{{ slide.content }}</p>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>

              <div class="key-points-section">
                <h4>关键点</h4>
                <el-tag 
                  v-for="point in parsedKeyPoints" 
                  :key="point"
                  type="warning"
                  class="key-point-tag"
                >
                  {{ point }}
                </el-tag>
              </div>

              <div class="actions">
                <el-button type="primary" @click="savePPT">保存PPT纲要</el-button>
                <el-button @click="editPPT">编辑纲要</el-button>
                <el-button type="success" @click="exportPPT">导出纲要</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 考试试卷生成 -->
      <el-tab-pane label="考试试卷" name="quiz">
        <el-card class="generation-card">
          <div slot="header">
            <span>自动生成考试试卷</span>
            <el-button 
              style="float: right; padding: 3px 0" 
              type="text"
              @click="generateQuiz"
              :loading="generating.quiz"
            >
              <i class="el-icon-edit"></i>
              生成试卷
            </el-button>
          </div>

          <!-- 生成选项 -->
          <el-form :model="quizOptions" label-width="120px" class="generation-options">
            <el-form-item label="知识库选择">
              <el-select v-model="quizOptions.knowledgeBaseId" placeholder="请选择知识库">
                <el-option 
                  v-for="kb in knowledgeBases" 
                  :key="kb.id" 
                  :label="kb.name" 
                  :value="kb.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="题目数量">
              <el-input-number v-model="quizOptions.questionCount" :min="5" :max="50" />
            </el-form-item>
            <el-form-item label="题型分布">
              <el-checkbox-group v-model="quizOptions.questionTypes">
                <el-checkbox label="MULTIPLE_CHOICE">选择题</el-checkbox>
                <el-checkbox label="TRUE_FALSE">判断题</el-checkbox>
                <el-checkbox label="SHORT_ANSWER">简答题</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-form>

          <!-- 试卷结果 -->
          <div v-if="generatedQuiz" class="generation-result">
            <h3>生成的考试试卷</h3>
            <div class="quiz-content">
              <div class="quiz-info">
                <p><strong>试卷标题：</strong>{{ generatedQuiz.title }}</p>
                <p><strong>题目数量：</strong>{{ generatedQuiz.questions?.length || 0 }}题</p>
              </div>

              <div class="questions-section">
                <h4>试题内容</h4>
                <div v-for="(question, index) in generatedQuiz.questions" :key="index" class="question-item">
                  <div class="question-header">
                    <span class="question-number">{{ index + 1 }}.</span>
                    <el-tag :type="getQuestionTypeColor(question.questionType)" size="mini">
                      {{ getQuestionTypeName(question.questionType) }}
                    </el-tag>
                  </div>
                  <div class="question-content">
                    <p class="question-text">{{ question.questionText }}</p>
                    <div v-if="question.options && question.options !== '[]'" class="question-options">
                      <p v-for="option in parseOptions(question.options)" :key="option" class="option">
                        {{ option }}
                      </p>
                    </div>
                    <div class="correct-answer">
                      <strong>参考答案：</strong>{{ question.correctAnswer }}
                    </div>
                  </div>
                </div>
              </div>

              <div class="actions">
                <el-button type="primary" @click="saveQuiz">保存试卷</el-button>
                <el-button @click="editQuiz">编辑试卷</el-button>
                <el-button type="success" @click="exportQuiz">导出试卷</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 自动评分 -->
      <el-tab-pane label="自动评分" name="grading">
        <el-card class="generation-card">
          <div slot="header">
            <span>学生答卷自动评分</span>
          </div>

          <!-- 评分功能 -->
          <div class="grading-section">
            <el-form :model="gradingForm" label-width="120px">
              <el-form-item label="选择试卷">
                <el-select v-model="gradingForm.quizId" placeholder="请选择试卷">
                  <el-option 
                    v-for="quiz in availableQuizzes" 
                    :key="quiz.id" 
                    :label="quiz.title" 
                    :value="quiz.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="学生答卷">
                <el-upload
                  ref="answerUpload"
                  :action="uploadAnswerAction"
                  :headers="uploadHeaders"
                  :data="uploadAnswerData"
                  :on-success="handleAnswerUploadSuccess"
                  :on-error="handleAnswerUploadError"
                  :before-upload="beforeAnswerUpload"
                  :auto-upload="false"
                  accept=".pdf,.doc,.docx,.jpg,.png"
                >
                  <el-button slot="trigger" size="small" type="primary">选择答卷文件</el-button>
                  <div slot="tip" class="el-upload__tip">
                    支持PDF、Word、图片格式，单个文件不超过20MB
                  </div>
                </el-upload>
              </el-form-item>
            </el-form>

            <el-button 
              type="primary" 
              @click="startGrading" 
              :loading="grading"
              :disabled="!gradingForm.quizId"
            >
              开始自动评分
            </el-button>
          </div>

          <!-- 评分结果 -->
          <div v-if="gradingResults.length > 0" class="grading-results">
            <h3>评分结果</h3>
            <el-table :data="gradingResults" border>
              <el-table-column prop="student.username" label="学生姓名" />
              <el-table-column prop="totalScore" label="总分" />
              <el-table-column prop="scorePercentage" label="得分率" />
              <el-table-column prop="status" label="状态">
                <template slot-scope="scope">
                  <el-tag :type="getGradingStatusColor(scope.row.status)">
                    {{ getGradingStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作">
                <template slot-scope="scope">
                  <el-button type="text" @click="viewGradingDetail(scope.row)">查看详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 评分详情对话框 -->
    <el-dialog
      title="评分详情"
      :visible.sync="gradingDetailVisible"
      width="80%"
    >
      <div v-if="currentGradingResult" class="grading-detail">
        <div class="basic-info">
          <h4>基本信息</h4>
          <p><strong>学生：</strong>{{ currentGradingResult.student?.username }}</p>
          <p><strong>总分：</strong>{{ currentGradingResult.totalScore }} / {{ currentGradingResult.maxScore }}</p>
          <p><strong>得分率：</strong>{{ currentGradingResult.scorePercentage }}%</p>
        </div>
        
        <div class="detailed-scores">
          <h4>详细评分</h4>
          <div v-if="parsedDetailedScores">
            <div v-for="(score, key) in parsedDetailedScores" :key="key" class="score-item">
              <p><strong>{{ key }}：</strong>{{ score.score }} / {{ score.maxScore }}</p>
              <p class="feedback">{{ score.feedback }}</p>
            </div>
          </div>
        </div>
        
        <div class="ai-analysis">
          <h4>AI分析</h4>
          <div v-if="parsedAiAnalysis">
            <div class="analysis-section">
              <h5>优点</h5>
              <ul>
                <li v-for="strength in parsedAiAnalysis.strengths" :key="strength">{{ strength }}</li>
              </ul>
            </div>
            <div class="analysis-section">
              <h5>不足</h5>
              <ul>
                <li v-for="weakness in parsedAiAnalysis.weaknesses" :key="weakness">{{ weakness }}</li>
              </ul>
            </div>
            <div class="analysis-section">
              <h5>建议</h5>
              <ul>
                <li v-for="suggestion in parsedAiAnalysis.suggestions" :key="suggestion">{{ suggestion }}</li>
              </ul>
            </div>
          </div>
        </div>
        
        <div class="overall-feedback">
          <h4>整体反馈</h4>
          <p>{{ currentGradingResult.feedback }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { mapGetters } from 'vuex'

export default {
  name: 'AIGeneration',
  data() {
    return {
      activeTab: 'outline',
      activeSlides: [],
      
      // 知识库列表
      knowledgeBases: [],
      
      // 生成状态
      generating: {
        outline: false,
        ppt: false,
        quiz: false,
        suggest: false
      },
      
      // 课程大纲相关
      outlineOptions: {
        knowledgeBaseId: null,
        estimatedHours: 20
      },
      generatedOutline: null,
      
      // PPT纲要相关
      pptOptions: {
        knowledgeBaseId: null,
        chapterName: ''
      },
      generatedPPT: null,
      
      // 考试试卷相关
      quizOptions: {
        knowledgeBaseId: null,
        questionCount: 10,
        questionTypes: ['MULTIPLE_CHOICE', 'TRUE_FALSE', 'SHORT_ANSWER']
      },
      generatedQuiz: null,
      
      // 自动评分相关
      gradingForm: {
        quizId: null,
        studentId: null
      },
      grading: false,
      gradingResults: [],
      availableQuizzes: [],
      gradingDetailVisible: false,
      currentGradingResult: null
    }
  },
  computed: {
    ...mapGetters(['user']),
    courseId() {
      return this.$route.params.courseId || this.$route.query.courseId
    },
    uploadAnswerAction() {
      return `http://localhost:8080/api/knowledge-base/auto-grade-answer`
    },
    uploadHeaders() {
      return {
        'Authorization': `Bearer ${this.$store.getters.token}`
      }
    },
    uploadAnswerData() {
      return {
        quizId: this.gradingForm.quizId,
        studentId: this.user?.id || 1 // 临时使用固定值
      }
    },
    parsedChapters() {
      if (!this.generatedOutline?.chapters) return []
      try {
        return JSON.parse(this.generatedOutline.chapters)
      } catch (e) {
        return []
      }
    },
    parsedObjectives() {
      if (!this.generatedOutline?.learningObjectives) return []
      try {
        return JSON.parse(this.generatedOutline.learningObjectives)
      } catch (e) {
        return []
      }
    },
    parsedSlides() {
      if (!this.generatedPPT?.slides) return []
      try {
        return JSON.parse(this.generatedPPT.slides)
      } catch (e) {
        return []
      }
    },
    parsedKeyPoints() {
      if (!this.generatedPPT?.keyPoints) return []
      try {
        return JSON.parse(this.generatedPPT.keyPoints)
      } catch (e) {
        return []
      }
    },
    parsedDetailedScores() {
      if (!this.currentGradingResult?.detailedScores) return null
      try {
        return JSON.parse(this.currentGradingResult.detailedScores)
      } catch (e) {
        return null
      }
    },
    parsedAiAnalysis() {
      if (!this.currentGradingResult?.aiAnalysis) return null
      try {
        return JSON.parse(this.currentGradingResult.aiAnalysis)
      } catch (e) {
        return null
      }
    }
  },
  created() {
    this.fetchKnowledgeBases()
    this.fetchAvailableQuizzes()
  },
  methods: {
    async fetchKnowledgeBases() {
      try {
        const data = await request.get('/api/knowledge-base')
        this.knowledgeBases = data
      } catch (error) {
        console.error('获取知识库失败:', error)
      }
    },
    
    async fetchAvailableQuizzes() {
      try {
        // 这里需要根据课程获取可用的测验
        // 暂时使用模拟数据
        this.availableQuizzes = [
          { id: 1, title: '第一章测验' },
          { id: 2, title: '第二章测验' }
        ]
      } catch (error) {
        console.error('获取测验列表失败:', error)
      }
    },
    
    handleTabClick(tab) {
      // 标签页切换处理
    },

    async generateOutline() {
      if (!this.courseId) {
        this.$message.warning('无法获取课程 ID')
        return
      }

      this.generating.outline = true
      try {
        const response = await request.post(
            `/api/course-outlines/generate`,
            { courseId: this.courseId },
            { params: { studentId: this.user.id } }
        )
        this.generatedOutline = response
        this.$message.success('课程大纲生成成功')
      } catch (error) {
        console.error('生成大纲失败:', error)
        this.$message.error('生成大纲失败')
      } finally {
        this.generating.outline = false
      }
    },
    
    async generatePPT() {
      if (!this.pptOptions.knowledgeBaseId || !this.pptOptions.chapterName) {
        this.$message.warning('请先选择知识库并输入章节名称')
        return
      }
      
      this.generating.ppt = true
      try {
        const response = await request.post(
          `/api/knowledge-base/generate-ppt-outline/${this.pptOptions.knowledgeBaseId}?chapterName=${this.pptOptions.chapterName}`,
          {}
        )
        this.generatedPPT = response
        this.$message.success('PPT纲要生成成功')
      } catch (error) {
        console.error('生成PPT纲要失败:', error)
        this.$message.error('生成PPT纲要失败')
      } finally {
        this.generating.ppt = false
      }
    },
    
    async generateQuiz() {
      if (!this.quizOptions.knowledgeBaseId) {
        this.$message.warning('请先选择知识库')
        return
      }
      
      this.generating.quiz = true
      try {
        const response = await request.post(
          `/api/knowledge-base/generate-quiz-advanced/${this.quizOptions.knowledgeBaseId}`,
          this.quizOptions
        )
        this.generatedQuiz = response
        this.$message.success('考试试卷生成成功')
      } catch (error) {
        console.error('生成试卷失败:', error)
        this.$message.error('生成试卷失败')
      } finally {
        this.generating.quiz = false
      }
    },
    
    async startGrading() {
      // 启动上传组件
      this.$refs.answerUpload.submit()
    },
    
    beforeAnswerUpload(file) {
      const isValidType = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'image/jpeg', 'image/png'].includes(file.type)
      const isLt20M = file.size / 1024 / 1024 < 20
      
      if (!isValidType) {
        this.$message.error('只支持PDF、Word、图片格式的文件!')
        return false
      }
      if (!isLt20M) {
        this.$message.error('文件大小不能超过 20MB!')
        return false
      }
      this.grading = true
      return true
    },
    
    handleAnswerUploadSuccess(response, file) {
      this.grading = false
      this.$message.success('答卷上传并评分成功')
      this.gradingResults.unshift(response)
    },
    
    handleAnswerUploadError(error, file) {
      this.grading = false
      console.error('答卷上传失败:', error)
      this.$message.error('答卷上传失败')
    },
    
    viewGradingDetail(result) {
      this.currentGradingResult = result
      this.gradingDetailVisible = true
    },
    
    // 保存和编辑方法（暂时为空）
    saveOutline() {
      this.$message.success('大纲已保存')
    },
    editOutline() {
      this.$message.info('编辑功能开发中')
    },
    exportOutline() {
      this.$message.success('大纲已导出')
    },
    savePPT() {
      this.$message.success('PPT纲要已保存')
    },
    editPPT() {
      this.$message.info('编辑功能开发中')
    },
    exportPPT() {
      this.$message.success('PPT纲要已导出')
    },
    saveQuiz() {
      this.$message.success('试卷已保存')
    },
    editQuiz() {
      this.$message.info('编辑功能开发中')
    },
    exportQuiz() {
      this.$message.success('试卷已导出')
    },
    
    // 辅助方法
    parseOptions(options) {
      try {
        return JSON.parse(options)
      } catch (e) {
        return []
      }
    },
    
    getQuestionTypeColor(type) {
      const colors = {
        'MULTIPLE_CHOICE': 'primary',
        'TRUE_FALSE': 'success',
        'SHORT_ANSWER': 'warning'
      }
      return colors[type] || 'info'
    },
    
    getQuestionTypeName(type) {
      const names = {
        'MULTIPLE_CHOICE': '选择题',
        'TRUE_FALSE': '判断题',
        'SHORT_ANSWER': '简答题'
      }
      return names[type] || '未知'
    },
    
    getGradingStatusColor(status) {
      const colors = {
        'PENDING': 'info',
        'PROCESSING': 'warning',
        'COMPLETED': 'success',
        'FAILED': 'danger'
      }
      return colors[status] || 'info'
    },
    
    getGradingStatusText(status) {
      const texts = {
        'PENDING': '等待处理',
        'PROCESSING': '评分中',
        'COMPLETED': '已完成',
        'FAILED': '评分失败'
      }
      return texts[status] || '未知'
    },

    /**
     * 获取智能修改建议
     */
    async suggestOutline() {
      if (!this.generatedOutline?.id) {
        this.$message.warning('请先生成大纲后再获取建议')
        return
      }
      this.generating.suggest = true
      try {
        const response = await request.post(
            `/api/course-outlines/${this.generatedOutline.id}/suggest`,
            this.generatedOutline,
            { params: { studentId: this.user.id } }
        )
        this.generatedOutline = response
        this.$message.success('智能建议应用成功')
      } catch (error) {
        console.error('获取智能建议失败:', error)
        this.$message.error('获取智能建议失败')
      } finally {
        this.generating.suggest = false
      }
    }

  }
}
</script>

<style scoped>
.ai-generation {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-info h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
  display: flex;
  align-items: center;
}

.header-info h1 i {
  margin-right: 8px;
  color: #409EFF;
}

.header-desc {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.generation-card {
  margin-bottom: 20px;
}

.generation-options {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 4px;
}

.generation-result {
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
}

.outline-content, .ppt-content, .quiz-content {
  margin-top: 20px;
}

.outline-info, .ppt-info, .quiz-info {
  background: #f0f9ff;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.chapters-section, .slides-section, .questions-section {
  margin: 20px 0;
}

.chapter-item h5 {
  margin: 0 0 8px 0;
  color: #303133;
}

.chapter-item ul {
  margin: 0;
  padding-left: 20px;
}

.objectives-section, .key-points-section {
  margin: 20px 0;
}

.objective-tag, .key-point-tag {
  margin: 5px 5px 5px 0;
}

.question-item {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 15px;
}

.question-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.question-number {
  font-weight: bold;
  margin-right: 10px;
}

.question-text {
  font-weight: 500;
  margin: 10px 0;
}

.question-options .option {
  margin: 5px 0;
  padding-left: 20px;
}

.correct-answer {
  margin-top: 10px;
  padding: 8px;
  background: #f0f9ff;
  border-radius: 4px;
  font-size: 14px;
}

.grading-section {
  margin-bottom: 30px;
}

.grading-results {
  margin-top: 30px;
}

.grading-detail .basic-info,
.grading-detail .detailed-scores,
.grading-detail .ai-analysis,
.grading-detail .overall-feedback {
  margin-bottom: 20px;
}

.score-item {
  border: 1px solid #ebeef5;
  padding: 10px;
  margin: 10px 0;
  border-radius: 4px;
}

.feedback {
  color: #606266;
  font-size: 14px;
  margin: 5px 0 0 0;
}

.analysis-section {
  margin: 15px 0;
}

.analysis-section h5 {
  margin: 0 0 10px 0;
  color: #303133;
}

.analysis-section ul {
  margin: 0;
  padding-left: 20px;
}

.actions {
  margin-top: 20px;
  text-align: center;
}

.actions .el-button {
  margin: 0 10px;
}

.slide-detail {
  padding: 10px;
  background: #f8f9fa;
  border-radius: 4px;
}
</style> 