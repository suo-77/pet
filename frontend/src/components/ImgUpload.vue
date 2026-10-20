<script setup>
/**
 * 统一图片上传组件：本地磁盘存储（后端统一上传接口，上传后 MQ 异步压缩）
 * props:
 *  modelValue 已上传的图片URL列表（v-model）
 *  limit      最多上传张数（默认9，头像传1）
 * emits: update:modelValue
 */
import { ref } from 'vue'
import { uploadImageApi } from '../api/common'
import { addToast } from '../utils/toast'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  limit: { type: Number, default: 9 }
})
const emit = defineEmits(['update:modelValue'])

const uploading = ref(false)
const fileInput = ref(null)

/** 允许的图片格式与大小上限（与后端一致：10MB） */
const ALLOW_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
const MAX_SIZE = 10 * 1024 * 1024

function triggerSelect() {
  if (props.modelValue.length >= props.limit) {
    addToast(`最多上传${props.limit}张图片`, 'error')
    return
  }
  fileInput.value && fileInput.value.click()
}

async function onFileChange(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) return
  const remain = props.limit - props.modelValue.length
  if (files.length > remain) {
    addToast(`最多还能上传${remain}张`, 'error')
  }
  uploading.value = true
  try {
    for (const file of files.slice(0, remain)) {
      // 大小与格式校验
      if (!ALLOW_TYPES.includes(file.type)) {
        addToast(`不支持的图片格式：${file.name}`, 'error')
        continue
      }
      if (file.size > MAX_SIZE) {
        addToast(`图片超过10MB：${file.name}`, 'error')
        continue
      }
      const res = await uploadImageApi(file)
      // 后端返回 {url: "/images/..."}，取 url 字段；兼容直接返回字符串
      const url = typeof res === 'string' ? res : res?.url
      if (!url) {
        addToast('上传返回数据异常', 'error')
        continue
      }
      emit('update:modelValue', [...props.modelValue, url])
    }
  } finally {
    uploading.value = false
  }
}

function removeImage(index) {
  const next = [...props.modelValue]
  next.splice(index, 1)
  emit('update:modelValue', next)
}
</script>

<template>
  <div class="img-upload">
    <!-- 已上传预览 -->
    <div v-for="(url, index) in modelValue" :key="url" class="upload-item">
      <img :src="url" alt="预览图" />
      <span class="remove-btn" @click="removeImage(index)">×</span>
    </div>
    <!-- 上传入口 -->
    <div v-if="modelValue.length < limit" class="upload-trigger" :class="{ uploading }" @click="triggerSelect">
      <span v-if="!uploading">＋<small>上传</small></span>
      <span v-else class="mini-spinner"></span>
    </div>
    <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple hidden @change="onFileChange" />
  </div>
</template>

<style scoped>
.img-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.upload-item {
  position: relative;
  width: 84px;
  height: 84px;
  border-radius: 14px;
  overflow: hidden;
  background: #fff4e8;
  box-shadow: 0 4px 12px rgba(242, 162, 104, 0.14);
}
.upload-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(60, 40, 25, 0.62);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
  transition: transform 0.15s ease, background 0.15s ease;
}
.remove-btn:active { transform: scale(0.85); background: rgba(242, 102, 63, 0.85); }
.upload-trigger {
  width: 84px;
  height: 84px;
  border: 1.6px dashed #ffd9b8;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #f2a268;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  background: #fffaf4;
  transition: all 0.2s ease;
  -webkit-tap-highlight-color: transparent;
}
.upload-trigger:hover {
  border-color: #f2a268;
  background: #fff4e8;
  box-shadow: 0 4px 12px rgba(242, 162, 104, 0.18);
}
.upload-trigger:active { transform: scale(0.94); }
.upload-trigger small {
  font-size: 12px;
  margin-top: 3px;
  color: #c9a27f;
}
.mini-spinner {
  width: 22px;
  height: 22px;
  border: 3px solid #ffe3c9;
  border-top-color: #f2a268;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
