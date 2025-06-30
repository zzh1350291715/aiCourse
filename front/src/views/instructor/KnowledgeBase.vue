<template>
  <div class="knowledge-base">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-info">
        <h1>
          <i class="el-icon-collection"></i>
          知识库管理
        </h1>
        <p class="header-desc">管理课程知识库，自动生成教学内容</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog = true">
          <i class="el-icon-plus"></i>
          创建知识库
        </el-button>
      </div>
    </div>

    <!-- 知识库列表 -->
    <div v-loading="loading">
      <el-row :gutter="20">
        <el-col 
          :xs="24" 
          :sm="12" 
          :lg="8"
          v-for="kb in knowledgeBases"
          :key="kb.id"
          class="kb-item"
        >
          <el-card class="kb-card" @click.native="selectKnowledgeBase(kb)">
            <div class="kb-header">
              <div class="kb-icon">
                <i class="el-icon-collection"></i>
              </div>
              <div class="kb-status">
                <el-tag v-if="kb.status === 'READY'" type="success" size="mini">就绪</el-tag>
                <el-tag v-else-if="kb.status === 'PROCESSING'" type="warning" size="mini">处理中</el-tag>
                <el-tag v-else type="info" size="mini">创建中</el-tag>
              </div>
            </div>
            <div class="kb-content">
              <h3 class="kb-title">{{ kb.name }}</h3>
              <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
              <div class="kb-stats">
                <span class="stat-item">
                  <i class="el-icon-document"></i>
                  {{ kb.documentCount || 0 }} 文档
                </span>
                <span class="stat-item">
                  <i class="el-icon-time"></i>
                  {{ formatDate(kb.createdAt) }}
                </span>
              </div>
            </div>
            <div class="kb-actions" @click.stop>
              <el-button-group>
                <el-button size="mini" type="primary" @click="selectKnowledgeBase(kb)">
                  <i class="el-icon-view"></i>
                  管理
                </el-button>
                <el-button size="mini" type="danger" @click="deleteKnowledgeBase(kb)">
                  <i class="el-icon-delete"></i>
                </el-button>
              </el-button-group>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <el-empty v-if="!loading && knowledgeBases.length === 0" description="暂无知识库">
        <el-button type="primary" @click="showCreateDialog = true">
          创建第一个知识库
        </el-button>
      </el-empty>
    </div>

    <!-- 知识库详情面板 -->
    <el-drawer
      :visible.sync="drawerVisible"
      :title="currentKb.name"
      direction="rtl"
      size="60%"
      :before-close="handleDrawerClose"
    >
      <div class="drawer-content" v-if="currentKb.id">
        <!-- 操作栏 -->
        <div class="action-bar">
          <el-tabs v-model="activeTab" @tab-click="handleTabClick">
            <el-tab-pane label="文档管理" name="documents">
              <div class="tab-actions">
                <el-button type="primary" @click="showUploadDialog = true">
                  <i class="el-icon-upload"></i>
                  上传文档
                </el-button>
              </div>
            </el-tab-pane>
            <el-tab-pane label="智能生成" name="generate">
              <div class="tab-actions">
                <el-button-group>
                  <el-button @click="generateOutline" :loading="generating.outline">
                    <i class="el-icon-menu"></i>
                    生成大纲
                  </el-button>
                  <el-button @click="generatePPT" :loading="generating.ppt">
                    <i class="el-icon-present"></i>
                    生成PPT
                  </el-button>
                  <el-button @click="generateQuiz" :loading="generating.quiz">
                    <i class="el-icon-edit"></i>
                    生成题目
                  </el-button>
                </el-button-group>
              </div>
            </el-tab-pane>
            <el-tab-pane label="问答测试" name="qa">
              <div class="tab-actions">
                <el-input
                  v-model="testQuestion"
                  placeholder="输入问题测试知识库..."
                  @keyup.enter="testQA"
                >
                  <el-button slot="append" @click="testQA" :loading="testing">
                    <i class="el-icon-search"></i>
                  </el-button>
                </el-input>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 标签页内容 -->
        <div class="tab-content">
          <!-- 文档管理 -->
          <div v-if="activeTab === 'documents'" class="documents-panel">
            <el-table :data="currentDocuments" v-loading="documentsLoading">
              <el-table-column width="60">
                <template slot-scope="scope">
                  <i :class="getDocumentIcon(scope.row.fileType)" class="doc-icon"></i>
                </template>
              </el-table-column>
              <el-table-column prop="fileName" label="文档名称" min-width="200">
                <template slot-scope="scope">
                  <div class="doc-name">
                    <span>{{ scope.row.fileName }}</span>
                    <el-tag v-if="scope.row.status === 'PROCESSED'" type="success" size="mini">已处理</el-tag>
                    <el-tag v-else-if="scope.row.status === 'PROCESSING'" type="warning" size="mini">处理中</el-tag>
                    <el-tag v-else type="info" size="mini">待处理</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="fileSize" label="大小" width="100">
                <template slot-scope="scope">
                  {{ formatFileSize(scope.row.fileSize) }}
                </template>
              </el-table-column>
              <el-table-column prop="uploadTime" label="上传时间" width="150">
                <template slot-scope="scope">
                  {{ formatDate(scope.row.uploadTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150">
                <template slot-scope="scope">
                  <el-button-group>
                    <el-button size="mini" type="primary" @click="previewDocument(scope.row)">
                      <i class="el-icon-view"></i>
                    </el-button>
                    <el-button 
                      size="mini" 
                      type="warning"
                      @click="reprocessDocument(scope.row)"
                      :disabled="scope.row.status === 'PROCESSING'"
                    >
                      <i class="el-icon-refresh"></i>
                    </el-button>
                    <el-button size="mini" type="danger" @click="deleteDocument(scope.row)">
                      <i class="el-icon-delete"></i>
                    </el-button>
                  </el-button-group>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 智能生成 -->
          <div v-if="activeTab === 'generate'" class="generate-panel">
            <el-row :gutter="20">
              <el-col :span="24">
                <div class="generate-section">
                  <h3>
                    <i class="el-icon-magic-stick"></i>
                    智能内容生成
                  </h3>
                  <p class="section-desc">基于知识库文档自动生成教学内容</p>
                  
                  <div class="generate-options">
                    <el-card class="option-card" @click.native="generateOutline">
                      <div class="option-content">
                        <i class="el-icon-menu option-icon"></i>
                        <h4>课程大纲</h4>
                        <p>自动分析文档内容，生成结构化的课程大纲</p>
                      </div>
                      <div class="option-status">
                        <el-button v-if="!generating.outline" type="primary">生成</el-button>
                        <el-button v-else loading>生成中...</el-button>
                      </div>
                    </el-card>

                    <el-card class="option-card" @click.native="generatePPT">
                      <div class="option-content">
                        <i class="el-icon-present option-icon"></i>
                        <h4>PPT演示</h4>
                        <p>基于文档内容自动生成PowerPoint演示文稿</p>
                      </div>
                      <div class="option-status">
                        <el-button v-if="!generating.ppt" type="primary">生成</el-button>
                        <el-button v-else loading>生成中...</el-button>
                      </div>
                    </el-card>

                    <el-card class="option-card" @click.native="generateQuiz">
                      <div class="option-content">
                        <i class="el-icon-edit option-icon"></i>
                        <h4>测试题目</h4>
                        <p>根据文档内容自动生成各种类型的测试题目</p>
                      </div>
                      <div class="option-status">
                        <el-button v-if="!generating.quiz" type="primary">生成</el-button>
                        <el-button v-else loading>生成中...</el-button>
                      </div>
                    </el-card>
                  </div>
                </div>
              </el-col>
            </el-row>

            <!-- 生成结果展示 -->
            <div v-if="generatedContent" class="generated-content">
              <el-card>
                <div slot="header">
                  <span>生成结果</span>
                  <el-button style="float: right;" type="text" @click="downloadResult">
                    <i class="el-icon-download"></i>
                    下载
                  </el-button>
                </div>
                <div class="content-preview">
                  <pre v-if="generatedContent.type === 'outline'">{{ generatedContent.content }}</pre>
                  <div v-else-if="generatedContent.type === 'quiz'" class="quiz-preview">
                    <div v-for="(question, index) in generatedContent.questions" :key="index" class="question-item">
                      <h4>{{ index + 1 }}. {{ question.question }}</h4>
                      <div v-if="question.type === 'MULTIPLE_CHOICE'" class="options">
                        <p v-for="(option, optIndex) in question.options" :key="optIndex">
                          {{ String.fromCharCode(65 + optIndex) }}. {{ option }}
                        </p>
                        <p class="answer">答案：{{ question.correctAnswer }}</p>
                      </div>
                    </div>
                  </div>
                  <div v-else class="other-content">
                    {{ generatedContent.content }}
                  </div>
                </div>
              </el-card>
            </div>
          </div>

          <!-- 问答测试 -->
          <div v-if="activeTab === 'qa'" class="qa-panel">
            <div class="qa-history">
              <div v-for="(qa, index) in qaHistory" :key="index" class="qa-item">
                <div class="question">
                  <i class="el-icon-user"></i>
                  <span>{{ qa.question }}</span>
                </div>
                <div class="answer">
                  <i class="el-icon-cpu"></i>
                  <div class="answer-content">{{ qa.answer }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 创建知识库对话框 -->
    <el-dialog title="创建知识库" :visible.sync="showCreateDialog" width="500px" @open="handleCreateDialogOpen">
      <el-form :model="createForm" :rules="createRules" ref="createForm" label-width="100px">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="createForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入知识库描述（可选）" 
          />
        </el-form-item>
        <el-form-item label="关联课程" prop="courseId">
          <el-select 
            v-model="createForm.courseId" 
            placeholder="请选择关联课程"
            style="width: 100%"
            v-loading="coursesLoading"
          >
            <el-option 
              v-for="course in availableCourses" 
              :key="course.id" 
              :label="course.title" 
              :value="course.id"
            >
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createKnowledgeBase" :loading="creating">创建</el-button>
      </div>
    </el-dialog>

    <!-- 文档上传对话框 -->
    <el-dialog title="上传文档" :visible.sync="showUploadDialog" width="600px">
      <el-upload
        ref="documentUpload"
        :action="uploadAction"
        :headers="uploadHeaders"
        :data="uploadData"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUploadDocument"
        :file-list="uploadFileList"
        :auto-upload="false"
        drag
        multiple
        accept=".pdf,.doc,.docx,.txt,.md"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文档拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">
          支持PDF、Word、TXT、Markdown格式，单个文件不超过50MB
        </div>
      </el-upload>
      <div slot="footer">
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading">开始上传</el-button>
      </div>
    </el-dialog>

    <!-- 文档预览对话框 -->
    <el-dialog
      :visible.sync="previewDialogVisible"
      :title="previewDocument.fileName"
      width="80%"
    >
      <div class="document-preview">
        <iframe 
          v-if="previewDocument.contentUrl"
          :src="previewDocument.contentUrl"
          class="preview-iframe"
          frameborder="0"
        ></iframe>
        <div v-else class="preview-placeholder">
          <i class="el-icon-document"></i>
          <p>文档预览功能开发中</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { mapGetters } from 'vuex'

export default {
  name: 'KnowledgeBase',
  data() {
    return {
      loading: false,
      creating: false,
      uploading: false,
      testing: false,
      documentsLoading: false,
      
      knowledgeBases: [],
      currentKb: {},
      currentDocuments: [],
      drawerVisible: false,
      activeTab: 'documents',
      
      // 创建表单
      showCreateDialog: false,
      createForm: {
        name: '',
        description: '',
        courseId: ''
      },
      createRules: {
        name: [
          { required: true, message: '请输入知识库名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        courseId: [
          { required: true, message: '请选择关联课程', trigger: 'change' }
        ]
      },
      
      // 课程列表
      availableCourses: [],
      coursesLoading: false,
      
      // 上传相关
      showUploadDialog: false,
      uploadFileList: [],
      
      // 生成相关
      generating: {
        outline: false,
        ppt: false,
        quiz: false
      },
      generatedContent: null,
      
      // 问答测试
      testQuestion: '',
      qaHistory: [],
      
      // 预览相关
      previewDialogVisible: false,
      previewDocument: {}
    }
  },
  computed: {
    ...mapGetters(['token', 'currentUser']),
    uploadAction() {
      return `http://localhost:8081/api/knowledge-base/upload/${this.currentKb.id}`
    },
    uploadHeaders() {
      return {
        'sessionId': this.token || ''
      }
    },
    uploadData() {
      return {}
    }
  },
  created() {
    this.fetchKnowledgeBases()
  },
  methods: {
    async fetchKnowledgeBases() {
      this.loading = true
      try {
        const response = await request.get('/api/knowledge-base')
        this.knowledgeBases = response
      } catch (error) {
        console.error('获取知识库列表失败:', error)
        this.$message.error('获取知识库列表失败')
      } finally {
        this.loading = false
      }
    },
    
    async createKnowledgeBase() {
      this.$refs.createForm.validate(async (valid) => {
        if (!valid) return
        
        this.creating = true
        try {
          const response = await request.post('/api/knowledge-base/create', this.createForm)
          this.$message.success('知识库创建成功')
          this.showCreateDialog = false
          this.createForm = { name: '', description: '', courseId: '' }
          await this.fetchKnowledgeBases()
        } catch (error) {
          console.error('创建知识库失败:', error)
          this.$message.error('创建知识库失败')
        } finally {
          this.creating = false
        }
      })
    },
    
    async deleteKnowledgeBase(kb) {
      this.$confirm(`确定要删除知识库"${kb.name}"吗？此操作不可恢复。`, '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await request.delete(`/api/knowledge-base/${kb.id}`)
          this.$message.success('删除成功')
          await this.fetchKnowledgeBases()
        } catch (error) {
          console.error('删除知识库失败:', error)
          this.$message.error('删除失败')
        }
      }).catch(() => {
        // 用户取消
      })
    },
    
    async selectKnowledgeBase(kb) {
      this.currentKb = kb
      this.drawerVisible = true
      await this.fetchDocuments()
    },
    
    async fetchDocuments() {
      if (!this.currentKb.id) return
      
      this.documentsLoading = true
      try {
        const response = await request.get(`/api/knowledge-base/documents/${this.currentKb.id}`)
        this.currentDocuments = response
      } catch (error) {
        console.error('获取文档列表失败:', error)
        this.$message.error('获取文档列表失败')
      } finally {
        this.documentsLoading = false
      }
    },
    
    handleTabClick(tab) {
      if (tab.name === 'documents') {
        this.fetchDocuments()
      }
    },
    
    beforeUploadDocument(file) {
      const isValidType = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain', 'text/markdown'].includes(file.type)
      const isLt50M = file.size / 1024 / 1024 < 50
      
      if (!isValidType) {
        this.$message.error('只支持PDF、Word、TXT、Markdown格式的文件!')
        return false
      }
      if (!isLt50M) {
        this.$message.error('文件大小不能超过 50MB!')
        return false
      }
      return true
    },
    
    submitUpload() {
      this.uploading = true
      this.$refs.documentUpload.submit()
    },
    
    handleUploadSuccess(response, file) {
      this.$message.success(`${file.name} 上传成功`)
      this.uploading = false
      this.showUploadDialog = false
      this.uploadFileList = []
      this.fetchDocuments()
    },
    
    handleUploadError(error, file) {
      console.error('上传失败:', error)
      this.$message.error(`${file.name} 上传失败`)
      this.uploading = false
    },
    
    async reprocessDocument(doc) {
      try {
        await request.post(`/api/knowledge-base/reprocess/${doc.id}`)
        this.$message.success('重新处理任务已提交')
        this.fetchDocuments()
      } catch (error) {
        console.error('重新处理失败:', error)
        this.$message.error('重新处理失败')
      }
    },
    
    async deleteDocument(doc) {
      this.$confirm(`确定要删除文档"${doc.fileName}"吗？`, '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await request.delete(`/api/knowledge-base/documents/${doc.id}`)
          this.$message.success('删除成功')
          this.fetchDocuments()
        } catch (error) {
          console.error('删除文档失败:', error)
          this.$message.error('删除失败')
        }
      }).catch(() => {
        // 用户取消
      })
    },
    
    previewDocument(doc) {
      this.previewDocument = doc
      this.previewDialogVisible = true
    },

    // KnowledgeBase.vue 中 methods 里，替换原有的 generateOutline()

    async generateOutline() {
      if (!this.currentKb.id) {
        this.$message.warning('请选择知识库');
        return;
      }
      this.generating.outline = true;
      try {
        // 调新接口：POST /api/course-outlines/generate?studentId=...
        const outlineDto = await request.post(
            '/api/course-outlines/generate',
            { courseId: this.currentKb.id },
            { params: { studentId: this.currentUser.id } }
        );
        // 将服务端返回的 DTO 结构保存在 generatedContent
        this.generatedContent = {
          type: 'outline',
          content: outlineDto
        };
        this.$message.success('课程大纲生成成功');
      } catch (error) {
        console.error('生成大纲失败:', error);
        this.$message.error('生成大纲失败');
      } finally {
        this.generating.outline = false;
      }
    },
    
    async generatePPT() {
      this.generating.ppt = true
      try {
        const response = await request.post(`/api/knowledge-base/generate-ppt/${this.currentKb.id}`)
        this.generatedContent = {
          type: 'ppt',
          content: response.pptUrl,
          downloadUrl: response.downloadUrl
        }
        this.$message.success('PPT生成成功')
      } catch (error) {
        console.error('生成PPT失败:', error)
        this.$message.error('生成PPT失败')
      } finally {
        this.generating.ppt = false
      }
    },
    
    async generateQuiz() {
      this.generating.quiz = true
      try {
        const response = await request.post(`/api/knowledge-base/generate-quiz/${this.currentKb.id}`)
        this.generatedContent = {
          type: 'quiz',
          questions: response.questions
        }
        this.$message.success('题目生成成功')
      } catch (error) {
        console.error('生成题目失败:', error)
        this.$message.error('生成题目失败')
      } finally {
        this.generating.quiz = false
      }
    },
    
    async testQA() {
      if (!this.testQuestion.trim()) {
        this.$message.warning('请输入问题')
        return
      }
      
      this.testing = true
      try {
        const response = await request.post(`/api/knowledge-base/qa/${this.currentKb.id}`, {
          question: this.testQuestion
        })
        
        this.qaHistory.unshift({
          question: this.testQuestion,
          answer: response.answer
        })
        
        this.testQuestion = ''
      } catch (error) {
        console.error('问答测试失败:', error)
        this.$message.error('问答测试失败')
      } finally {
        this.testing = false
      }
    },
    
    downloadResult() {
      if (this.generatedContent && this.generatedContent.downloadUrl) {
        window.open(this.generatedContent.downloadUrl, '_blank')
      } else {
        this.$message.info('暂无可下载内容')
      }
    },
    
    handleDrawerClose(done) {
      // 清理数据
      this.currentKb = {}
      this.currentDocuments = []
      this.generatedContent = null
      this.qaHistory = []
      this.activeTab = 'documents'
      
      // 调用done()方法来实际关闭抽屉
      if (typeof done === 'function') {
        done()
      }
    },
    
    formatDate(date) {
      if (!date) return '未知'
      return new Date(date).toLocaleDateString('zh-CN')
    },
    
    formatFileSize(bytes) {
      if (!bytes) return '未知'
      const sizes = ['Bytes', 'KB', 'MB', 'GB']
      if (bytes === 0) return '0 Bytes'
      const i = Math.floor(Math.log(bytes) / Math.log(1024))
      return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i]
    },
    
    getDocumentIcon(fileType) {
      const icons = {
        'pdf': 'el-icon-document',
        'doc': 'el-icon-edit-outline',
        'docx': 'el-icon-edit-outline',
        'txt': 'el-icon-tickets',
        'md': 'el-icon-tickets'
      }
      return icons[fileType] || 'el-icon-document'
    },
    
         async fetchAvailableCourses() {
       this.coursesLoading = true
       try {
         // 获取当前用户ID
         let userId = null
         if (this.currentUser && this.currentUser.id) {
           userId = this.currentUser.id
         } else {
           const userStr = localStorage.getItem('user')
           if (userStr) {
             const user = JSON.parse(userStr)
             userId = user.id
           }
         }
         
         if (!userId) {
           this.$message.error('无法获取用户信息，请重新登录')
           return
         }
         
         // 只获取当前讲师的课程
         const response = await request.get(`/api/courses/instructor/${userId}`)
         this.availableCourses = response || []
       } catch (error) {
         console.error('获取课程列表失败:', error)
         this.$message.error('获取课程列表失败')
       } finally {
         this.coursesLoading = false
       }
     },
     
     handleCreateDialogOpen() {
       // 对话框打开时获取课程列表
       this.fetchAvailableCourses()
     }
  }
}
</script>

<style scoped>
.knowledge-base {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.header-info h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  color: #303133;
  display: flex;
  align-items: center;
}

.header-info h1 i {
  margin-right: 8px;
  color: #409eff;
}

.header-desc {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.kb-item {
  margin-bottom: 20px;
}

.kb-card {
  height: 100%;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.kb-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.kb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.kb-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.kb-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #303133;
  font-weight: 600;
}

.kb-desc {
  margin: 0 0 15px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.kb-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.stat-item i {
  margin-right: 4px;
}

.kb-actions {
  margin-top: 15px;
  text-align: center;
}

.drawer-content {
  padding: 0 20px;
}

.action-bar {
  margin-bottom: 20px;
}

.tab-actions {
  padding: 10px 0;
}

.tab-content {
  min-height: 400px;
}

.documents-panel .doc-icon {
  font-size: 20px;
  color: #409eff;
}

.doc-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.generate-section h3 {
  margin: 0 0 8px 0;
  color: #303133;
  display: flex;
  align-items: center;
}

.generate-section h3 i {
  margin-right: 8px;
  color: #409eff;
}

.section-desc {
  margin: 0 0 20px 0;
  color: #606266;
  font-size: 14px;
}

.generate-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.option-card {
  cursor: pointer;
  transition: all 0.3s;
}

.option-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.option-content {
  text-align: center;
  padding: 20px;
}

.option-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 15px;
}

.option-content h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
}

.option-content p {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.4;
}

.option-status {
  text-align: center;
  padding: 15px;
  border-top: 1px solid #ebeef5;
}

.generated-content {
  margin-top: 20px;
}

.content-preview {
  max-height: 400px;
  overflow-y: auto;
}

.content-preview pre {
  white-space: pre-wrap;
  font-family: 'Arial', sans-serif;
  line-height: 1.6;
}

.quiz-preview .question-item {
  margin-bottom: 20px;
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.quiz-preview h4 {
  margin: 0 0 10px 0;
  color: #303133;
}

.quiz-preview .options p {
  margin: 5px 0;
  color: #606266;
}

.quiz-preview .answer {
  color: #67c23a;
  font-weight: bold;
}

.qa-panel {
  padding: 20px 0;
}

.qa-history {
  max-height: 500px;
  overflow-y: auto;
}

.qa-item {
  margin-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 15px;
}

.qa-item:last-child {
  border-bottom: none;
}

.question, .answer {
  display: flex;
  align-items: flex-start;
  margin-bottom: 10px;
}

.question i, .answer i {
  margin-right: 10px;
  margin-top: 3px;
  font-size: 16px;
}

.question i {
  color: #409eff;
}

.answer i {
  color: #67c23a;
}

.answer-content {
  flex: 1;
  line-height: 1.6;
  color: #303133;
}

.document-preview {
  height: 500px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #c0c4cc;
}

.preview-placeholder i {
  font-size: 64px;
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .header-actions {
    margin-top: 15px;
  }
  
  .generate-options {
    grid-template-columns: 1fr;
  }
}
</style> 
