<template>
  <el-card 
    class="course-card" 
    :body-style="{ padding: '0px' }"
    shadow="hover"
  >
    <img 
      :src="course.imageUrl || defaultImage" 
      class="course-image"
      @error="handleImageError"
    >
    <div class="course-content">
      <div class="course-header">
        <h3 class="course-title">{{ course.title }}</h3>
        <el-tag 
          :type="course.status === 'ACTIVE' ? 'success' : 'info'" 
          size="mini"
        >
          {{ getStatusText(course.status) }}
        </el-tag>
      </div>
      
      <p class="course-description">{{ course.description }}</p>
      
      <div class="course-meta">
        <div class="instructor">
          <i class="el-icon-user-solid"></i>
          <span>{{ course.instructor?.username || '未知讲师' }}</span>
        </div>
        <div class="create-time">
          <i class="el-icon-time"></i>
          <span>{{ formatDate(course.createdAt) }}</span>
        </div>
      </div>
      
      <div class="course-actions">
        <el-button 
          type="primary" 
          size="mini" 
          @click="$emit('view-detail', course)"
        >
          查看详情
        </el-button>
        <el-button 
          type="success" 
          size="mini" 
          @click="$emit('enroll', course)"
          v-if="!isEnrolled"
        >
          立即报名
        </el-button>
        <el-button 
          type="info" 
          size="mini" 
          disabled
          v-else
        >
          已报名
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'CourseCard',
  props: {
    course: {
      type: Object,
      required: true,
      default: () => ({})
    },
    isEnrolled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      defaultImage: require('@/pic/classlogo.png')
    }
  },
  methods: {
    handleImageError(event) {
      event.target.src = this.defaultImage
    },
    getStatusText(status) {
      const statusMap = {
        'ACTIVE': '进行中',
        'DRAFT': '草稿',
        'COMPLETED': '已完成',
        'ARCHIVED': '已归档'
      }
      return statusMap[status] || '未知状态'
    },
    formatDate(dateString) {
      if (!dateString) return '未知时间'
      const date = new Date(dateString)
      return date.toLocaleDateString('zh-CN')
    }
  }
}
</script>

<style scoped>
.course-card {
  width: 100%;
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.course-card:hover {
  transform: translateY(-2px);
}

.course-image {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

.course-content {
  padding: 16px;
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.course-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
  flex: 1;
  margin-right: 8px;
}

.course-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.course-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  font-size: 12px;
  color: #909399;
}

.course-meta > div {
  display: flex;
  align-items: center;
}

.course-meta i {
  margin-right: 4px;
}

.course-actions {
  display: flex;
  gap: 8px;
}

.course-actions .el-button {
  flex: 1;
}

@media (max-width: 768px) {
  .course-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .course-title {
    margin-right: 0;
    margin-bottom: 8px;
  }
  
  .course-meta {
    flex-direction: column;
    gap: 4px;
  }
}
</style> 