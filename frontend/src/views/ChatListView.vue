<script setup>
/**
 * 私信列表页：全部聊天会话，显示对方昵称头像/最后消息/时间/未读红点，分页加载
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { sessionsApi } from '../api/chat'
import { friendTime } from '../utils/time'
import EmptyState from '../components/EmptyState.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import TabBar from '../components/TabBar.vue'

const router = useRouter()

const sessions = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const finished = ref(false)

async function load(reset = false) {
  if (loading.value || (finished.value && !reset)) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  loading.value = true
  try {
    const data = await sessionsApi({ page: page.value, pageSize: 10 })
    total.value = data.total
    if (reset) sessions.value = data.list
    else sessions.value.push(...data.list)
    if (sessions.value.length >= data.total || data.list.length === 0) finished.value = true
    else page.value++
  } finally {
    loading.value = false
  }
}

onMounted(() => load(true))
</script>

<template>
  <div class="chat-list-page">
    <header class="page-head">
      <h1>私信</h1>
      <p>和铲屎官们聊聊毛孩子吧</p>
    </header>

    <main>
      <LoadingSpinner v-if="loading && !sessions.length" />
      <EmptyState v-else-if="!sessions.length" text="暂无消息，去社区找铲屎官聊天吧" icon="💌" />
      <div v-for="session in sessions" :key="session.sessionId" class="session-card" @click="router.push('/chat/' + session.peerId)">
        <img v-if="session.peerAvatar" class="peer-avatar" :src="session.peerAvatar" alt="头像" />
        <span v-else class="peer-avatar placeholder">🐱</span>
        <div class="session-body">
          <div class="session-top">
            <span class="peer-name">{{ session.peerNickname }}</span>
            <span class="last-time">{{ friendTime(session.lastTime) }}</span>
          </div>
          <div class="session-bottom">
            <p class="last-message">{{ session.lastMessage }}</p>
            <span v-if="session.unreadCount > 0" class="unread">{{ session.unreadCount > 99 ? '99+' : session.unreadCount }}</span>
          </div>
        </div>
      </div>
      <LoadingSpinner v-if="loading && sessions.length" text="加载更多..." />
      <button v-if="!finished && sessions.length && !loading" class="load-more" @click="load()">加载更多</button>
    </main>

    <TabBar />
  </div>
</template>

<style scoped>
.chat-list-page { min-height: 100vh; background: #fff6ec; padding-bottom: 76px; }
.page-head { padding: 22px 16px 14px; }
.page-head h1 { font-size: 22px; color: #6b4f3a; }
.page-head p { font-size: 13px; color: #b5a48e; margin-top: 6px; }
main { padding: 8px 16px; }
.session-card {
  display: flex;
  gap: 14px;
  background: #fff;
  border-radius: 14px;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.session-card:hover { transform: translateY(-2px); box-shadow: 0 8px 26px rgba(242, 162, 104, 0.16); }
.peer-avatar { width: 50px; height: 50px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.placeholder { display: flex; align-items: center; justify-content: center; background: #fff1e0; font-size: 22px; }
.session-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px; }
.session-top { display: flex; justify-content: space-between; align-items: center; }
.peer-name { font-size: 15px; font-weight: 600; color: #6b4f3a; }
.last-time { font-size: 11px; color: #b5a48e; }
.session-bottom { display: flex; justify-content: space-between; align-items: center; }
.last-message {
  font-size: 13px;
  color: #b5a48e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.unread {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #ff4d2e;
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-left: 8px;
}
.load-more {
  display: block;
  margin: 10px auto;
  border: 1.5px solid #f2d9c0;
  background: #fff;
  color: #b9824f;
  border-radius: 14px;
  padding: 8px 26px;
  font-size: 13px;
  cursor: pointer;
  transition: all .15s;
}
.load-more:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.load-more:active { transform: scale(0.94); }
</style>
