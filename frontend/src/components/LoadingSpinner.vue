<script setup>
// 局部加载状态组件：列表/详情请求中使用
defineProps({
  text: { type: String, default: '加载中...' }
})
</script>

<template>
  <div class="loading-box">
    <span class="loading-spinner"></span>
    <span class="loading-text">{{ text }}</span>
  </div>
</template>

<style scoped>
.loading-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 36px 0;
  color: #a49a8f;
  font-size: 13px;
}
.loading-spinner {
  position: relative;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: conic-gradient(from 0deg, #f2a268, transparent 65%);
  animation: spin 0.9s linear infinite;
  -webkit-mask: radial-gradient(farthest-side, transparent calc(100% - 3px), #000 calc(100% - 2px));
          mask: radial-gradient(farthest-side, transparent calc(100% - 3px), #000 calc(100% - 2px));
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
