<template>
  <iframe
    v-if="viewerUrl"
    :src="viewerUrl"
    class="ppt-iframe"
    frameborder="0"
    @load="$emit('load')"
  ></iframe>
  <div v-else class="ppt-error">无法生成预览链接，请下载后查看。</div>
</template>

<script>
export default {
  name: 'PptPreview',
  props: {
    url: {
      type: String,
      required: true
    }
  },
  computed: {
    viewerUrl () {
      try {
        // 生成绝对地址，供 microsoft viewer 访问
        const absoluteUrl = this.url.startsWith('http') ? this.url : `${window.location.origin}${this.url}`
        return `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(absoluteUrl)}`
      } catch (e) {
        console.error('PPT 预览 URL 生成失败:', e)
        return ''
      }
    }
  }
}
</script>

<style scoped>
.ppt-iframe {
  width: 100%;
  height: 70vh;
}
.ppt-error {
  text-align: center;
  color: red;
}
</style> 