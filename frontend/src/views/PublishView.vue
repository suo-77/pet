<script setup>
/**
 * 社区发布页：文字 + 多图（最多9张，未传图允许纯文字）
 * 图片经统一上传接口存本地磁盘，上传完成由 MQ 异步压缩
 * 发布成功回到首页
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createPostApi } from '../api/post'
import { addToast } from '../utils/toast'
import ImgUpload from '../components/ImgUpload.vue'

const router = useRouter()

const content = ref('')
const images = ref([])
const submitting = ref(false)

const MAX_CONTENT = 1000
/** 后端要求文字与图片至少其一 */
const canSubmit = computed(() => (content.value.trim() || images.value.length) && !submitting.value)

async function onSubmit() {
  if (!content.value.trim() && images.value.length === 0) {
    addToast('写点文字或上传图片再发布吧', 'error')
    return
  }
  submitting.value = true
  try {
    await createPostApi({ content: content.value.trim(), images: images.value })
    addToast('发布成功', 'success')
    router.replace('/')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="publish-page">
    <header class="page-head">
      <span class="back" @click="router.back()">‹</span>
      <span class="head-title">发布动态</span>
      <button class="submit-btn" :disabled="!canSubmit" @click="onSubmit">
        {{ submitting ? '发布中...' : '发布' }}
      </button>
    </header>

    <div class="publish-card">
      <textarea
        v-model="content"
        class="content-input"
        :maxlength="MAX_CONTENT"
        rows="7"
        placeholder="分享你家毛孩子的日常吧～（可纯文字发布）"
      ></textarea>
      <div class="counter">{{ content.length }}/{{ MAX_CONTENT }}</div>
      <ImgUpload v-model="images" :limit="9" />
      <p class="hint">支持 jpg/png/gif/webp，单张最大10MB，上传后自动压缩</p>
    </div>
  </div>
</template>

<style scoped>
.publish-page { min-height: 100vh; background: #fff6ec; }
.page-head {
  display: flex;
  align-items: center;
  padding: 14px 16px 12px;
  position: sticky;
  top: 0;
  background: rgba(255, 246, 236, 0.94);
  backdrop-filter: blur(8px);
  z-index: 10;
}
.back { font-size: 26px; color: #6b4f3a; cursor: pointer; line-height: 1; padding-right: 12px; }
.head-title { flex: 1; font-size: 17px; font-weight: 600; color: #6b4f3a; }
.submit-btn {
  border: none;
  background: linear-gradient(135deg, #f7a76b, #f2a268);
  color: #fff;
  border-radius: 14px;
  padding: 9px 22px;
  font-size: 14px;
  cursor: pointer;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
  transition: all .15s;
}
.submit-btn:hover:not(:disabled) { filter: brightness(1.05); transform: translateY(-1px); box-shadow: 0 7px 18px rgba(242, 162, 104, 0.4); }
.submit-btn:active:not(:disabled) { transform: scale(0.96); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.publish-card {
  margin: 16px 16px 0;
  background: #fff;
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1);
}
.content-input {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 15px;
  color: #4a3a2c;
  line-height: 1.7;
  background: transparent;
}
.counter { text-align: right; color: #b5a48e; font-size: 12px; margin: 8px 0 14px; }
.hint { font-size: 12px; color: #b5a48e; margin-top: 14px; }
</style>
