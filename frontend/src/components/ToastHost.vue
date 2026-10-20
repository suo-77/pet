<script setup>
// 全局 Toast 渲染宿主：顶部居中弹出，成功绿/失败红
import { toastState } from '../utils/toast'

const state = toastState()
</script>

<template>
  <div class="toast-host">
    <transition-group name="toast">
      <div v-for="item in state.list" :key="item.id" class="toast-item" :class="item.type">
        <span class="toast-icon">{{ item.type === 'success' ? '✓' : '✕' }}</span>
        {{ item.msg }}
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  pointer-events: none;
}
.toast-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 18px;
  border-radius: 14px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.3px;
  max-width: 80vw;
  box-shadow: 0 8px 24px rgba(60, 40, 25, 0.16), 0 2px 8px rgba(60, 40, 25, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
}
.toast-item.success {
  background: linear-gradient(135deg, #7fc48e, #5ba872);
}
.toast-item.error {
  background: linear-gradient(135deg, #f08773, #e66a52);
}
.toast-icon {
  display: inline-flex;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.24);
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}
.toast-enter-active, .toast-leave-active { transition: all 0.25s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(-12px) scale(0.95); }
</style>
