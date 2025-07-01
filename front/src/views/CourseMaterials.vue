<template>
  <div class="course-materials">
    <!-- 页面头部 -->
    <div class="materials-header">
      <div class="header-info">
        <h1>
          <i class="el-icon-folder-opened"></i>
          {{ course.title || '课程资料' }}
        </h1>
        <p class="course-desc">{{ course.description || '浏览和学习课程相关资料' }}</p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.back()">
          <i class="el-icon-arrow-left"></i>
          返回课程
        </el-button>
        <el-button 
          v-if="userRole === 'INSTRUCTOR' && isOwner"
          type="primary"
          @click="uploadDialogVisible = true"
        >
          <i class="el-icon-upload"></i>
          上传资料
        </el-button>
      </div>
    </div>

    <!-- 筛选和搜索栏 -->
    <el-card class="filter-section" shadow="never">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索资料..."
            prefix-icon="el-icon-search"
            clearable
            @input="handleSearch"
          />
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-select
            v-model="selectedType"
            placeholder="筛选类型"
            clearable
            @change="handleFilter"
            style="width: 100%"
          >
            <el-option label="全部类型" value="" />
            <el-option label="PDF文档" value="PDF" />
            <el-option label="文档" value="DOCUMENT" />
            <el-option label="视频" value="VIDEO" />
            <el-option label="音频" value="AUDIO" />
            <el-option label="图片" value="IMAGE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-radio-group v-model="viewMode" size="medium">
            <el-radio-button label="list">
              <i class="el-icon-s-unfold"></i>
              列表
            </el-radio-button>
            <el-radio-button label="grid">
              <i class="el-icon-s-grid"></i>
              网格
            </el-radio-button>
          </el-radio-group>
        </el-col>
      </el-row>
    </el-card>

    <!-- 资料列表/网格 -->
    <div v-loading="loading">
      <!-- 列表视图 -->
      <el-card v-if="viewMode === 'list'" class="materials-table">
        <el-table :data="filteredMaterials" style="width: 100%">
          <el-table-column width="60">
            <template slot-scope="scope">
              <i :class="getMaterialIcon(scope.row.materialType)" class="material-icon"></i>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="资料名称" min-width="200">
            <template slot-scope="scope">
              <div class="material-title">
                <span class="title-text">{{ scope.row.title }}</span>
                <el-tag 
                  v-if="userRole === 'STUDENT' && scope.row.isCompleted"
                  size="mini" 
                  type="success"
                >
                  已完成
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="materialType" label="类型" width="100">
            <template slot-scope="scope">
              <el-tag size="mini" :type="getMaterialTagType(scope.row.materialType)">
                {{ getMaterialTypeName(scope.row.materialType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="100">
            <template slot-scope="scope">
              {{ formatFileSize(scope.row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="上传时间" width="150">
            <template slot-scope="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template slot-scope="scope">
              <el-button-group>
                <el-button 
                  size="mini" 
                  type="primary"
                  @click="previewMaterial(scope.row)"
                >
                  <i class="el-icon-view"></i>
                  预览
                </el-button>
                <el-button 
                  size="mini" 
                  type="success"
                  @click="downloadMaterial(scope.row)"
                >
                  <i class="el-icon-download"></i>
                  下载
                </el-button>
                <el-button 
                  v-if="userRole === 'INSTRUCTOR' && isOwner"
                  size="mini" 
                  type="danger"
                  @click="deleteMaterial(scope.row)"
                >
                  <i class="el-icon-delete"></i>
                </el-button>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 网格视图 -->
      <div v-else class="materials-grid">
        <el-row :gutter="20">
          <el-col 
            :xs="24" 
            :sm="12" 
            :md="8" 
            :lg="6"
            v-for="material in filteredMaterials"
            :key="material.id"
            class="grid-item"
          >
            <el-card class="material-card" @click.native="previewMaterial(material)">
              <div class="card-content">
                <div class="material-preview">
                  <i :class="getMaterialIcon(material.materialType)" class="preview-icon"></i>
                  <el-tag 
                    v-if="userRole === 'STUDENT' && material.isCompleted"
                    class="completed-badge"
                    size="mini" 
                    type="success"
                  >
                    ✓
                  </el-tag>
                </div>
                <div class="material-info">
                  <h4 class="material-title">{{ material.title }}</h4>
                  <p class="material-meta">
                    <el-tag size="mini" :type="getMaterialTagType(material.materialType)">
                      {{ getMaterialTypeName(material.materialType) }}
                    </el-tag>
                    <span class="file-size">{{ formatFileSize(material.fileSize) }}</span>
                  </p>
                  <p class="upload-time">{{ formatDate(material.createdAt) }}</p>
                </div>
                <div class="card-actions" @click.stop>
                  <el-button 
                    size="mini" 
                    type="text"
                    @click="previewMaterial(material)"
                  >
                    预览
                  </el-button>
                  <el-button 
                    size="mini" 
                    type="text"
                    @click="downloadMaterial(material)"
                  >
                    下载
                  </el-button>
                  <el-button 
                    v-if="userRole === 'INSTRUCTOR' && isOwner"
                    size="mini" 
                    type="text"
                    @click="deleteMaterial(material)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty 
      v-if="!loading && filteredMaterials.length === 0" 
      description="暂无课程资料"
    >
      <el-button 
        v-if="userRole === 'INSTRUCTOR' && isOwner"
        type="primary" 
        @click="uploadDialogVisible = true"
      >
        上传第一个资料
      </el-button>
    </el-empty>

    <!-- 分页 -->
    <div v-if="filteredMaterials.length > 0" class="pagination-container">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalMaterials"
      />
    </div>

    <!-- 文件上传对话框 -->
    <el-dialog
      :visible.sync="uploadDialogVisible"
      title="上传课程资料"
      width="600px"
      :before-close="handleUploadClose"
    >
      <el-form :model="uploadForm" :rules="uploadRules" ref="uploadForm" label-width="100px">
        <el-form-item label="资料标题" prop="title">
          <el-input v-model="uploadForm.title" placeholder="请输入资料标题" />
        </el-form-item>
        <el-form-item label="资料类型" prop="type">
          <el-select v-model="uploadForm.type" placeholder="请选择资料类型" style="width: 100%">
            <el-option label="PDF文档" value="PDF" />
            <el-option label="Word文档" value="DOCUMENT" />
            <el-option label="PPT演示" value="DOCUMENT" />
            <el-option label="视频" value="VIDEO" />
            <el-option label="音频" value="AUDIO" />
            <el-option label="图片" value="IMAGE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件上传" prop="file">
          <el-upload
            ref="upload"
            :action="uploadAction"
            :headers="uploadHeaders"
            :data="uploadData"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            :file-list="fileList"
            :auto-upload="false"
            drag
          >
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            <div class="el-upload__tip" slot="tip">支持PDF、Word、PPT、视频、音频等格式，大小不超过100MB</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading">上传</el-button>
      </div>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog
      :visible.sync="previewDialogVisible"
      :title="currentMaterial.title"
      width="80%"
      :before-close="handlePreviewClose"
    >
      <div class="preview-container">
        <!-- PDF预览 -->
        <iframe 
          v-if="currentMaterial.materialType === 'PDF'"
          :src="currentMaterial.contentUrl"
          class="preview-iframe"
          frameborder="0"
        ></iframe>
        
        <!-- 视频预览 -->
        <video 
          v-else-if="currentMaterial.materialType === 'VIDEO'"
          :src="currentMaterial.contentUrl"
          controls
          class="preview-video"
          @play="markAsViewed"
        ></video>
        
        <!-- 音频预览 -->
        <audio 
          v-else-if="currentMaterial.materialType === 'AUDIO'"
          :src="currentMaterial.contentUrl"
          controls
          class="preview-audio"
          @play="markAsViewed"
        ></audio>
        
        <!-- 图片预览 -->
        <img 
          v-else-if="currentMaterial.materialType === 'IMAGE'"
          :src="currentMaterial.contentUrl"
          class="preview-image"
          @load="markAsViewed"
        />
        
        <!-- 文档预览（Word） -->
        <docx-preview 
          v-else-if="isDocxFile(currentMaterial)"
          :url="currentMaterial.contentUrl"
          @loaded="markAsViewed"
        />
        
        <!-- PPT 预览 -->
        <ppt-preview
          v-else-if="isPptFile(currentMaterial)"
          :url="currentMaterial.contentUrl"
          @load="markAsViewed"
        />
        
        <!-- 其他文件类型 -->
        <div v-else class="preview-placeholder">
          <i class="el-icon-document-copy"></i>
          <p>此文件类型不支持在线预览，请下载后查看</p>
          <el-button type="primary" @click="downloadMaterial(currentMaterial)">
            <i class="el-icon-download"></i>
            下载文件
          </el-button>
        </div>
      </div>
      
      <!-- 学习进度跟踪 -->
      <div v-if="userRole === 'STUDENT'" class="progress-tracker">
        <el-button 
          v-if="!currentMaterial.isCompleted"
          type="success"
          @click="markAsCompleted"
        >
          <i class="el-icon-check"></i>
          标记为已完成
        </el-button>
        <el-tag v-else type="success">
          <i class="el-icon-check"></i>
          已完成学习
        </el-tag>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { mapGetters } from 'vuex'
import DocxPreview from '@/components/DocxPreview.vue'
import PptPreview from '@/components/PptPreview.vue'

export default {
  name: 'CourseMaterials',
  components: {
    DocxPreview,
    PptPreview
  },
  data() {
    return {
      course: {},
      materials: [],
      filteredMaterials: [],
      loading: true,
      searchKeyword: '',
      selectedType: '',
      viewMode: 'list',
      currentPage: 1,
      pageSize: 20,
      totalMaterials: 0,
      
      // 上传相关
      uploadDialogVisible: false,
      uploading: false,
      uploadForm: {
        title: '',
        type: '',
        file: null
      },
      uploadRules: {
        title: [{ required: true, message: '请输入资料标题', trigger: 'blur' }],
        type: [{ required: true, message: '请选择资料类型', trigger: 'change' }]
      },
      fileList: [],
      
      // 预览相关
      previewDialogVisible: false,
      currentMaterial: {}
    }
  },
  computed: {
    ...mapGetters(['user', 'userRole']),
    courseId() {
      return this.$route.params.courseId || this.$route.query.courseId
    },
    isOwner() {
      return this.course && this.user && this.course.instructorId === this.user.id
    },
    uploadAction() {
      return `http://localhost:8080/api/course-materials/upload`
    },
    uploadHeaders() {
      return {
        'Authorization': `Bearer ${this.$store.getters.token}`
      }
    },
    uploadData() {
      return {
        courseId: this.courseId,
        title: this.uploadForm.title,
        type: this.uploadForm.type,
        order: 0
      }
    }
  },
  created() {
    this.fetchCourseInfo()
    this.fetchMaterials()
  },
  methods: {
    async fetchCourseInfo() {
      try {
        const courseData = await request.get(`/api/courses/${this.courseId}`)
        this.course = courseData
      } catch (error) {
        console.error('获取课程信息失败:', error)
      }
    },

    async fetchMaterials() {
      this.loading = true
      try {
        const materials = await request.get(`/api/course-materials/course/${this.courseId}`)
        console.log('获取到的课程资料数据:', materials)
        if (materials && materials.length > 0) {
          console.log('第一个资料的详细信息:', materials[0])
          console.log('materialType:', materials[0].materialType)
          console.log('createdAt:', materials[0].createdAt)
        }
        this.materials = materials
        
        // 如果是学生，获取学习进度
        if (this.userRole === 'STUDENT') {
          await this.fetchLearningProgress()
        }
        
        this.applyFilters()
      } catch (error) {
        console.error('获取课程资料失败:', error)
        this.$message.error('获取课程资料失败')
      } finally {
        this.loading = false
      }
    },

    async fetchLearningProgress() {
      try {
        const progress = await request.get(`/api/student-progress/materials/${this.courseId}`)
        // 将进度信息合并到materials中
        this.materials.forEach(material => {
          const progressItem = progress.find(p => p.materialId === material.id)
          material.isCompleted = progressItem ? progressItem.completed : false
        })
      } catch (error) {
        console.error('获取学习进度失败:', error)
      }
    },

    handleSearch() {
      this.applyFilters()
    },

    handleFilter() {
      this.applyFilters()
    },

    applyFilters() {
      let filtered = this.materials

      // 搜索筛选
      if (this.searchKeyword) {
        filtered = filtered.filter(material => 
          material.title.toLowerCase().includes(this.searchKeyword.toLowerCase())
        )
      }

      // 类型筛选
      if (this.selectedType) {
        filtered = filtered.filter(material => material.materialType === this.selectedType)
      }

      this.filteredMaterials = filtered
      this.totalMaterials = filtered.length
    },

    handleSizeChange(size) {
      this.pageSize = size
    },

    handleCurrentChange(page) {
      this.currentPage = page
    },

    previewMaterial(material) {
      // 处理相对路径，转为绝对地址
      let url = material.contentUrl
      if (url && !url.startsWith('http')) {
        const base = process.env.VUE_APP_FILE_BASE || 'http://localhost:8081'
        url = base.replace(/\/$/, '') + url
      }
      this.currentMaterial = { ...material, contentUrl: url }
      this.previewDialogVisible = true
      
      // 如果是学生，记录查看行为
      if (this.userRole === 'STUDENT') {
        this.recordViewActivity(material.id)
      }
    },

    async downloadMaterial(material) {
      try {
        const response = await request.get(`/api/course-materials/download/${material.id}`, {
          responseType: 'blob'
        })
        
        // 创建下载链接
        const url = window.URL.createObjectURL(new Blob([response]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', material.title)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)
        
        this.$message.success('下载成功')
        
        // 如果是学生，记录下载行为
        if (this.userRole === 'STUDENT') {
          this.recordViewActivity(material.id)
        }
      } catch (error) {
        console.error('下载失败:', error)
        this.$message.error('下载失败')
      }
    },

    async deleteMaterial(material) {
      this.$confirm(`确定要删除资料"${material.title}"吗？`, '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await request.delete(`/api/course-materials/${material.id}`)
          this.$message.success('删除成功')
          this.fetchMaterials()
        } catch (error) {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }).catch(() => {
        // 用户取消删除
      })
    },

    async recordViewActivity(materialId) {
      try {
        await request.post(`/api/student-progress/view-material`, {
          courseId: this.courseId,
          materialId: materialId
        })
      } catch (error) {
        console.error('记录学习活动失败:', error)
      }
    },

    async markAsViewed() {
      if (this.userRole === 'STUDENT' && this.currentMaterial.id) {
        await this.recordViewActivity(this.currentMaterial.id)
      }
    },

    async markAsCompleted() {
      try {
        await request.post(`/api/student-progress/complete-material`, {
          courseId: this.courseId,
          materialId: this.currentMaterial.id
        })
        
        this.currentMaterial.isCompleted = true
        // 更新本地数据
        const materialIndex = this.materials.findIndex(m => m.id === this.currentMaterial.id)
        if (materialIndex !== -1) {
          this.materials[materialIndex].isCompleted = true
        }
        
        this.$message.success('已标记为完成')
      } catch (error) {
        console.error('标记完成失败:', error)
        this.$message.error('标记完成失败')
      }
    },

    beforeUpload(file) {
      const isLt100M = file.size / 1024 / 1024 < 100
      if (!isLt100M) {
        this.$message.error('上传文件大小不能超过 100MB!')
        return false
      }
      return true
    },

    submitUpload() {
      this.$refs.uploadForm.validate((valid) => {
        if (valid) {
          this.uploading = true
          this.$refs.upload.submit()
        }
      })
    },

    handleUploadSuccess(response) {
      this.uploading = false
      this.uploadDialogVisible = false
      this.$message.success('上传成功')
      this.resetUploadForm()
      this.fetchMaterials()
    },

    handleUploadError(error) {
      this.uploading = false
      console.error('上传失败:', error)
      this.$message.error('上传失败')
    },

    handleUploadClose() {
      this.resetUploadForm()
    },

    handlePreviewClose() {
      this.previewDialogVisible = false
      this.currentMaterial = {}
    },

    resetUploadForm() {
      this.uploadForm = {
        title: '',
        type: '',
        file: null
      }
      this.fileList = []
      this.$refs.uploadForm && this.$refs.uploadForm.resetFields()
    },

    formatDate(date) {
      if (!date) return '未知'
      try {
        return new Date(date).toLocaleDateString('zh-CN')
      } catch (error) {
        console.error('日期格式化错误:', error, date)
        return '未知'
      }
    },

    formatFileSize(bytes) {
      if (!bytes) return '未知'
      const sizes = ['Bytes', 'KB', 'MB', 'GB']
      if (bytes === 0) return '0 Bytes'
      const i = Math.floor(Math.log(bytes) / Math.log(1024))
      return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i]
    },

    getMaterialIcon(type) {
      const icons = {
        'PDF': 'el-icon-document',
        'DOCUMENT': 'el-icon-edit-outline',
        'VIDEO': 'el-icon-video-play',
        'AUDIO': 'el-icon-microphone',
        'IMAGE': 'el-icon-picture',
        'OTHER': 'el-icon-paperclip'
      }
      return icons[type] || 'el-icon-document'
    },

    getMaterialTagType(type) {
      const types = {
        'PDF': 'danger',
        'DOCUMENT': 'primary',
        'VIDEO': 'success',
        'AUDIO': 'info',
        'IMAGE': 'success',
        'OTHER': ''
      }
      return types[type] || ''
    },

    getMaterialTypeName(type) {
      const names = {
        'PDF': 'PDF',
        'DOCUMENT': '文档',
        'VIDEO': '视频',
        'AUDIO': '音频',
        'IMAGE': '图片',
        'OTHER': '其他'
      }
      return names[type] || '未知'
    },

    isDocxFile(material) {
      if (!material || !material.contentUrl) return false
      const ext = material.contentUrl.split('.').pop().toLowerCase()
      return ['doc', 'docx'].includes(ext)
    },

    isPptFile(material) {
      if (!material || !material.contentUrl) return false
      const ext = material.contentUrl.split('.').pop().toLowerCase()
      return ['ppt', 'pptx'].includes(ext)
    }
  }
}
</script>

<style scoped>
.course-materials {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.materials-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-wrap: wrap;
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
  color: #409eff;
}

.course-desc {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.filter-section {
  margin-bottom: 20px;
}

.materials-table {
  margin-bottom: 20px;
}

.material-icon {
  font-size: 24px;
  color: #409eff;
}

.material-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  flex: 1;
}

.materials-grid {
  margin-bottom: 20px;
}

.grid-item {
  margin-bottom: 20px;
}

.material-card {
  height: 100%;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.material-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.card-content {
  text-align: center;
}

.material-preview {
  position: relative;
  margin-bottom: 15px;
}

.preview-icon {
  font-size: 48px;
  color: #409eff;
}

.completed-badge {
  position: absolute;
  top: -5px;
  right: -5px;
}

.material-info h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
  line-height: 1.4;
}

.material-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.file-size {
  font-size: 12px;
  color: #909399;
}

.upload-time {
  margin: 0;
  font-size: 12px;
  color: #c0c4cc;
}

.card-actions {
  margin-top: 10px;
  display: flex;
  justify-content: center;
  gap: 5px;
}

.pagination-container {
  text-align: center;
  margin-top: 20px;
}

.preview-container {
  min-height: 400px;
}

.preview-iframe {
  width: 100%;
  height: 500px;
}

.preview-video {
  width: 100%;
  max-height: 500px;
}

.preview-audio {
  width: 100%;
}

.preview-image {
  max-width: 100%;
  max-height: 500px;
  object-fit: contain;
  display: block;
  margin: 0 auto;
}

.preview-placeholder {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.preview-placeholder i {
  font-size: 64px;
  margin-bottom: 20px;
  color: #c0c4cc;
}

.progress-tracker {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

@media (max-width: 768px) {
  .materials-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .header-actions {
    margin-top: 15px;
    justify-content: stretch;
  }
  
  .header-actions .el-button {
    flex: 1;
  }
}
</style>