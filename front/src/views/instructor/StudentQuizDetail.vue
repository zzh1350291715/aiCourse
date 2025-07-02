<template>
  <div class="student-quiz-detail">
    <el-card>
      <div slot="header" class="clearfix">
        <span>学生答题详情</span>
        <el-button style="float: right;" @click="$router.back()">返回</el-button>
      </div>
      <div v-if="answers && answers.length">
        <el-table :data="answers" style="width:100%">
          <el-table-column label="题目" prop="questionText"/>
          <el-table-column label="学生答案" prop="studentAnswer"/>
          <el-table-column label="正确答案" prop="correctAnswer"/>
          <el-table-column label="判分">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.isCorrect" type="success">正确</el-tag>
              <el-tag v-else type="danger">错误</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-else class="loading">
        <el-spinner></el-spinner>
        <p>暂无答题数据</p>
      </div>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request'
export default {
  name: 'StudentQuizDetail',
  data() {
    return {
      answers: []
    }
  },
  async mounted() {
    const quizId = this.$route.params.quizId
    const studentId = this.$route.params.studentId
    const res = await request.get(`/api/quizzes/instructor/${quizId}/student/${studentId}/detail`)
    this.answers = res || []
  }
}
</script>

<style scoped>
.student-quiz-detail {
  padding: 20px;
}
.loading {
  text-align: center;
  padding: 40px;
}
</style> 