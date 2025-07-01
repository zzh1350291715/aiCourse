<template>
  <div class="docx-preview" v-html="htmlContent"></div>
</template>

<script>
import mammoth from 'mammoth'

export default {
  name: 'DocxPreview',
  props: {
    url: {
      type: String,
      required: true
    }
  },
  data () {
    return {
      htmlContent: '<p>加载中...</p>'
    }
  },
  async mounted () {
    try {
      const arrayBuffer = await fetch(this.url).then(res => res.arrayBuffer())
      const result = await mammoth.convertToHtml({ arrayBuffer })
      this.htmlContent = result.value
      this.$emit('loaded')
    } catch (e) {
      console.error('DOCX 预览失败:', e)
      this.htmlContent = '<p style="color:red">DOCX 文件预览失败，请下载后查看。</p>'
    }
  }
}
</script>

<style scoped>
.docx-preview {
  /* 让 word 内容自适应滚动 */
  max-height: 70vh;
  overflow-y: auto;
  padding: 10px;
  background: #f8f8f8;
}
.docx-preview img {
  max-width: 100%;
}
</style> 