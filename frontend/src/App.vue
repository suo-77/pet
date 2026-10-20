<script setup>
// 根组件：页面出口 + 全局 Toast 提示挂载点 + 全局 WebSocket 连接管理
import { watch } from 'vue'
import ToastHost from './components/ToastHost.vue'
import { connectWs } from './utils/ws'
import { useUserStore } from './stores/user'

const userStore = useUserStore()

// 已登录则建立 WS 连接（断线自动重连在 ws.js 内处理）
if (userStore.isLogin) {
  connectWs(userStore.token)
}

// 登录状态变化时同步连接/断开
watch(
  () => userStore.token,
  (token) => {
    if (token) connectWs(token)
  }
)
</script>

<template>
  <router-view />
  <ToastHost />
</template>
