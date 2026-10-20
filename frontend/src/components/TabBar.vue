<script setup>
/**
 * 底部导航栏：首页 / 店铺 / 购物车 / 私信(未读红点) / 我的
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ref, onMounted } from 'vue'
import { unreadTotalApi } from '../api/chat'

const route = useRoute()
const router = useRouter()
const unread = ref(0)

/** 当前激活项 */
const active = computed(() => {
  if (route.path.startsWith('/shops')) return '/shops'
  if (route.path.startsWith('/cart')) return '/cart'
  if (route.path.startsWith('/chat')) return '/chat'
  if (route.path.startsWith('/profile') || route.path.startsWith('/user/')) return '/profile'
  return '/'
})

const tabs = [
  { path: '/', icon: '🏠', label: '首页' },
  { path: '/shops', icon: '🏪', label: '店铺' },
  { path: '/cart', icon: '🛒', label: '购物车' },
  { path: '/chat', icon: '💬', label: '私信' },
  { path: '/profile', icon: '🐾', label: '我的' }
]

/** 拉取未读总数（私信红点） */
async function loadUnread() {
  try {
    const data = await unreadTotalApi()
    unread.value = data.unread || 0
  } catch (e) {
    // 静默失败，红点保持上次值
  }
}

onMounted(loadUnread)
defineExpose({ loadUnread })
</script>

<template>
  <nav class="tabbar">
    <div
      v-for="tab in tabs"
      :key="tab.path"
      class="tabbar-item"
      :class="{ active: active === tab.path }"
      @click="router.push(tab.path)"
    >
      <span class="tab-icon-wrap">
        <span class="tab-icon">{{ tab.icon }}</span>
        <span v-if="tab.path === '/chat' && unread > 0" class="badge">{{ unread > 99 ? '99+' : unread }}</span>
      </span>
      <span class="tab-label">{{ tab.label }}</span>
    </div>
  </nav>
</template>

<style scoped>
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  box-shadow: 0 -4px 18px rgba(242, 162, 104, 0.12);
  padding: 6px 0 calc(6px + env(safe-area-inset-bottom));
}

.tabbar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 3px 0;
  color: #b0a89f;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: transform 0.15s ease, color 0.15s ease;
}

.tabbar-item:active { transform: scale(0.92); }

.tabbar-item.active { color: #f2a268; }

.tab-icon-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 28px;
}

.tab-icon {
  font-size: 20px;
  line-height: 1;
  filter: grayscale(0.9) opacity(0.75);
  transition: transform 0.2s ease, filter 0.2s ease;
}

.tabbar-item.active .tab-icon {
  filter: none;
  transform: translateY(-1px);
}

.tab-label {
  font-size: 11px;
  line-height: 1;
  letter-spacing: 0.5px;
}

/* 未读红点角标 */
.badge {
  position: absolute;
  top: -2px;
  right: -6px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff8b5a, #f2663f);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
  box-shadow: 0 2px 6px rgba(242, 102, 63, 0.45);
}

.badge::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 10px;
  background: rgba(242, 102, 63, 0.25);
  animation: pulse 1.6s ease-out infinite;
}

@keyframes pulse {
  0% { transform: scale(0.85); opacity: 0.9; }
  70% { transform: scale(1.25); opacity: 0; }
  100% { transform: scale(1.25); opacity: 0; }
}
</style>