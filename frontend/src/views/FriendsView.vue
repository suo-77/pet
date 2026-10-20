<script setup>
/**
 * 好友/关注页：我关注的人 / 我的粉丝 / 推荐用户 三个 Tab，全部分页
 * 支持关注/取关；点击卡片进入对方主页
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fansApi, followApi, followingApi, recommendApi, unfollowApi } from '../api/user'
import { useUserStore } from '../stores/user'
import { addToast } from '../utils/toast'
import EmptyState from '../components/EmptyState.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import TabBar from '../components/TabBar.vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('following')
const loading = ref(false)
const lists = reactive({
  following: { items: [], total: 0, page: 1, finished: false },
  fans: { items: [], total: 0, page: 1, finished: false },
  recommend: { items: [], total: 0, page: 1, finished: false }
})

const tabs = [
  { key: 'following', label: '我关注的' },
  { key: 'fans', label: '我的粉丝' },
  { key: 'recommend', label: '推荐用户' }
]

/** 每个状态返回对应列表状态对象 */
function stateOf(key) {
  return lists[key]
}

async function load(key, reset = false) {
  const state = stateOf(key)
  if (loading.value || (state.finished && !reset)) return
  if (reset) {
    state.page = 1
    state.finished = false
  }
  loading.value = true
  try {
    const params = { page: state.page, pageSize: 10 }
    const api = key === 'following' ? followingApi : key === 'fans' ? fansApi : recommendApi
    const data = await api(params)
    state.total = data.total
    if (reset) state.items = data.list
    else state.items.push(...data.list)
    if (state.items.length >= data.total || data.list.length === 0) state.finished = true
    else state.page++
  } finally {
    loading.value = false
  }
}

function switchTab(key) {
  activeTab.value = key
  if (!stateOf(key).items.length) load(key, true)
}

/** 关注 / 取关（关注列表与粉丝列表展示已关注态） */
const followPending = ref(new Set())

async function toggleFollow(user, listKey) {
  if (followPending.value.has(user.id)) return
  followPending.value.add(user.id)
  try {
    if (user.followed) {
      await unfollowApi(user.id)
      user.followed = false
      addToast('已取消关注', 'success')
      // 从"我关注的"列表移除
      if (listKey === 'following') {
        const state = stateOf('following')
        state.items = state.items.filter((u) => u.id !== user.id)
        state.total = Math.max(0, state.total - 1)
      }
    } else {
      await followApi(user.id)
      user.followed = true
      addToast('关注成功', 'success')
    }
  } finally {
    followPending.value.delete(user.id)
  }
}

onMounted(() => load('following', true))
</script>

<template>
  <div class="friends-page">
    <header class="page-head">
      <h1>好友与关注</h1>
    </header>

    <div class="tab-row">
      <span
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >{{ tab.label }}</span>
    </div>

    <main class="user-list">
      <LoadingSpinner v-if="loading && !stateOf(activeTab).items.length" />
      <EmptyState
        v-else-if="!stateOf(activeTab).items.length"
        :text="activeTab === 'following' ? '还没有关注任何铲屎官' : activeTab === 'fans' ? '还没有粉丝，发布动态涨粉吧' : '暂无推荐用户'"
        icon="👥"
      />
      <div v-for="user in stateOf(activeTab).items" :key="user.id" class="user-card">
        <div class="user-info" @click="router.push('/user/' + user.id)">
          <img v-if="user.avatar" class="user-avatar" :src="user.avatar" alt="头像" />
          <span v-else class="user-avatar placeholder">🐱</span>
          <div>
            <p class="user-name">{{ user.nickname }}</p>
            <p class="user-intro">{{ user.intro || '这个铲屎官很神秘' }}</p>
          </div>
        </div>
        <button
          class="follow-btn"
          :class="{ followed: user.followed }"
          :disabled="followPending.has(user.id)"
          @click="toggleFollow(user, activeTab)"
        >
          {{ user.followed ? '已关注' : '＋ 关注' }}
        </button>
      </div>
      <LoadingSpinner v-if="loading && stateOf(activeTab).items.length" text="加载更多..." />
      <button
        v-if="!stateOf(activeTab).finished && stateOf(activeTab).items.length && !loading"
        class="load-more"
        @click="load(activeTab)"
      >加载更多</button>
    </main>

    <TabBar />
  </div>
</template>

<style scoped>
/* 好友/关注：暖橘渐变风格（匹配模板类名） */
.friends-page { min-height: 100vh; background: #fff6ec; padding-bottom: 76px; }
.page-head { padding: 22px 16px 6px; }
.page-head h1 { font-size: 20px; color: #6b4f3a; }
.tab-row {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  position: sticky;
  top: 0;
  background: rgba(255, 246, 236, 0.96);
  backdrop-filter: blur(8px);
  z-index: 10;
}
.tab-row span {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  background: #fff;
  border-radius: 20px;
  font-size: 14px;
  color: #9b8b76;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(255, 140, 66, 0.08);
  transition: all 0.2s;
}
.tab-row span:hover { color: #df7e38; }
.tab-row span.active { background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff; font-weight: 600; }
.user-list { padding: 8px 16px 0; }
.user-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-radius: 16px;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: 0 4px 14px rgba(255, 140, 66, 0.1);
  transition: all 0.2s;
}
.user-card:hover { transform: translateY(-2px); }
.user-info { display: flex; align-items: center; gap: 14px; flex: 1; min-width: 0; }
.user-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.user-avatar.placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff1e0;
  font-size: 26px;
  color: #df7e38;
}
.user-name { font-size: 16px; font-weight: 600; color: #4a3a2c; }
.user-intro {
  font-size: 12px;
  color: #b5a48e;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.follow-btn {
  flex-shrink: 0;
  border: 1.5px solid #f2a268;
  background: linear-gradient(135deg, #f7a76b, #f2a268);
  color: #fff;
  border-radius: 20px;
  padding: 7px 18px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.follow-btn:hover { filter: brightness(1.05); }
.follow-btn:active { transform: scale(0.95); }
.follow-btn:disabled { opacity: 0.5; cursor: default; }
.follow-btn.followed { background: #f5efe4; border-color: #e8dcc8; color: #b5a48e; }
.follow-btn.followed:hover { filter: none; }
.load-more {
  display: block;
  margin: 10px auto 0;
  border: 1.5px solid #ffcda5;
  background: #fff;
  color: #df7e38;
  border-radius: 14px;
  padding: 8px 26px;
  font-size: 13px;
  cursor: pointer;
}
.load-more:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.load-more:active { transform: scale(0.94); }
.load-more:disabled { opacity: 0.5; cursor: default; }
</style>
