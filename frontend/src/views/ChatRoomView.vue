<script setup>
/**
 * 一对一聊天窗：WebSocket 实时收发（文字+图片），聊天记录 MySQL 持久化分页
 * 上行: {toUserId, msgType, content, imageUrl}；下行: connected/chat/read/error
 * 发送消息由服务端回显给发送方，保证多端一致
 */
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { markReadApi, messagesApi } from '../api/chat'
import { userHomeApi } from '../api/user'
import { uploadImageApi } from '../api/common'
import { useUserStore } from '../stores/user'
import { addToast } from '../utils/toast'
import { friendTime, fullTime } from '../utils/time'
import { connectWs, onWs, sendWs } from '../utils/ws'
import LoadingSpinner from '../components/LoadingSpinner.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const peerId = Number(route.params.peerId)
const meId = computed(() => userStore.userInfo?.id)

const peer = ref({ nickname: '对方', avatar: '' })
const history = ref([])
const historyTotal = ref(0)
const historyPage = ref(1)
const loadingHistory = ref(false)
const input = ref('')
const preview = ref('')
const listRef = ref(null)
const fileRef = ref(null)
const sendingImage = ref(false)

/* ---------------- 历史记录（时间倒序分页，前端翻转正序渲染） ---------------- */
async function loadHistory(reset = false) {
  if (loadingHistory.value) return
  loadingHistory.value = true
  try {
    if (reset) historyPage.value = 1
    const data = await messagesApi(peerId, { page: historyPage.value, pageSize: 10 })
    historyTotal.value = data.total
    const ordered = [...data.list].reverse()
    if (reset) history.value = ordered
    else history.value = [...ordered, ...history.value]
    if (history.value.length < data.total && data.list.length) historyPage.value++
    if (reset) await nextTick(scrollBottom)
  } finally {
    loadingHistory.value = false
  }
}

/** 加载更早消息后保持视口位置（近似处理：滚到底部前一条位置） */
async function loadEarlier() {
  const el = listRef.value
  const prevHeight = el ? el.scrollHeight : 0
  await loadHistory()
  await nextTick(() => {
    if (el) el.scrollTop = el.scrollHeight - prevHeight
  })
}

function scrollBottom() {
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

/* ---------------- WebSocket 实时消息 ---------------- */
const unsubscribe = onWs(async (msg) => {
  if (msg.type === 'chat' && msg.message) {
    const m = msg.message
    const inSession = m.fromUserId === peerId || m.toUserId === peerId
    if (!inSession) return
    history.value.push(m)
    historyTotal.value++
    await nextTick(scrollBottom)
    // 收到对方消息：在会话窗口内视为已读
    if (m.fromUserId === peerId) {
      markReadApi(peerId).catch(() => {})
    }
  } else if (msg.type === 'read' && msg.readerId === peerId) {
    // 对方已读：把我发出去的消息标记为已读
    history.value.forEach((m) => {
      if (m.toUserId === peerId) m.isRead = 1
    })
  } else if (msg.type === 'error') {
    addToast(msg.message || '消息发送失败', 'error')
  }
})

/** 发送文字消息：由服务端落库后回显，避免本地乐观追加造成多端不一致 */
function sendText() {
  const content = input.value.trim()
  if (!content) return
  const ok = sendWs({ toUserId: peerId, msgType: 0, content })
  if (ok) input.value = ''
  else addToast('连接未建立，请稍后重试', 'error')
}

/** 选择图片即发送（统一上传接口 -> 图片消息） */
async function onPickImage(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
    addToast('不支持的图片格式', 'error')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    addToast('图片不能超过10MB', 'error')
    return
  }
  sendingImage.value = true
  try {
    const res = await uploadImageApi(file)
    // 后端返回 {url: "/images/..."}，取 url 字段
    const url = typeof res === 'string' ? res : res?.url
    const ok = url ? sendWs({ toUserId: peerId, msgType: 1, imageUrl: url }) : false
    if (!ok) addToast('连接未建立，请稍后重试', 'error')
  } finally {
    sendingImage.value = false
  }
}

/* ---------------- 生命周期 ---------------- */
onMounted(async () => {
  // 确保连接存在（App.vue 已做全局连接，这里兜底）
  if (userStore.isLogin) connectWs(userStore.token)
  // 对方资料（公开接口）
  try {
    const home = await userHomeApi(peerId)
    peer.value = { nickname: home.nickname, avatar: home.avatar }
  } catch (e) {
    // 对方不存在页面仍可展示历史
  }
  await loadHistory(true)
  // 进入会话清零未读
  markReadApi(peerId).catch(() => {})
})

onUnmounted(() => {
  unsubscribe()
})
</script>

<template>
  <div class="chat-room">
    <header class="room-head">
      <span class="back" @click="router.back()">‹</span>
      <img v-if="peer.avatar" class="peer-mini" :src="peer.avatar" alt="头像" @click="router.push('/user/' + peerId)" />
      <span v-else class="peer-mini placeholder" @click="router.push('/user/' + peerId)">🐱</span>
      <span class="peer-name">{{ peer.nickname }}</span>
    </header>

    <!-- 消息列表 -->
    <div ref="listRef" class="msg-list">
      <button
        v-if="history.length < historyTotal"
        class="earlier-btn"
        :disabled="loadingHistory"
        @click="loadEarlier"
      >{{ loadingHistory ? '加载中...' : '查看更早消息' }}</button>
      <p v-if="!history.length && !loadingHistory" class="empty-tip">打个招呼吧～ 👋</p>

      <div
        v-for="msg in history"
        :key="msg.id"
        class="msg-row"
        :class="{ mine: msg.fromUserId === meId }"
      >
        <!-- 对方消息 -->
        <template v-if="msg.fromUserId !== meId">
          <img v-if="peer.avatar" class="msg-avatar" :src="peer.avatar" alt="头像" />
          <span v-else class="msg-avatar placeholder">🐱</span>
        </template>
        <div class="msg-bubble-group">
          <!-- 分享动态卡片 -->
          <div
            v-if="msg.msgType === 2"
            class="share-card"
            @click="router.push('/')"
          >
            <div class="share-tag">🔗 分享了一条动态</div>
            <img v-if="msg.imageUrl" class="share-card-img" :src="msg.imageUrl" alt="分享封面" />
            <p class="share-card-text">{{ msg.content }}</p>
          </div>
          <!-- 图片消息 -->
          <img
            v-else-if="msg.msgType === 1"
            class="msg-image"
            :src="msg.imageUrl"
            alt="图片消息"
            @click="preview = msg.imageUrl"
          />
          <!-- 文字消息 -->
          <div v-else class="msg-bubble">{{ msg.content }}</div>
          <span class="msg-meta">
            {{ friendTime(msg.sendTime) }}
            <template v-if="msg.fromUserId === meId">· {{ msg.isRead ? '已读' : '未读' }}</template>
          </span>
        </div>
        <!-- 自己消息头像 -->
        <img v-if="msg.fromUserId === meId && userStore.userInfo?.avatar" class="msg-avatar" :src="userStore.userInfo.avatar" alt="头像" />
        <span v-else-if="msg.fromUserId === meId" class="msg-avatar placeholder">🐾</span>
      </div>
    </div>

    <!-- 输入区 -->
    <footer class="input-bar">
      <button class="img-btn" :disabled="sendingImage" @click="fileRef && fileRef.click()">
        {{ sendingImage ? '...' : '🖼' }}
      </button>
      <input v-model="input" maxlength="500" placeholder="说点什么..." @keyup.enter="sendText" />
      <button class="send-btn" :disabled="!input.trim()" @click="sendText">发送</button>
      <input ref="fileRef" type="file" accept="image/jpeg,image/png,image/gif,image/webp" hidden @change="onPickImage" />
    </footer>

    <!-- 图片预览 -->
    <div v-if="preview" class="preview-mask" @click="preview = ''">
      <img :src="preview" alt="图片预览" />
    </div>
  </div>
</template>

<style scoped>
.chat-room { height: 100vh; display: flex; flex-direction: column; background: #fff6ec; }
.room-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(242, 162, 104, 0.1);
  z-index: 5;
}
.back { font-size: 26px; color: #6b4f3a; cursor: pointer; line-height: 1; }
.peer-mini { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; cursor: pointer; }
.peer-name { font-size: 16px; font-weight: 600; color: #6b4f3a; }
.msg-list { flex: 1; overflow-y: auto; padding: 16px 14px 10px; }
.earlier-btn {
  display: block;
  margin: 0 auto 14px;
  border: 1.5px solid #f2d9c0;
  background: #fff;
  color: #b9824f;
  border-radius: 14px;
  padding: 6px 18px;
  font-size: 12px;
  cursor: pointer;
  transition: all .15s;
}
.earlier-btn:hover:not(:disabled) { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.earlier-btn:active:not(:disabled) { transform: scale(0.94); }
.empty-tip { text-align: center; color: #cdbba4; font-size: 13px; margin-top: 40px; }
.msg-row { display: flex; gap: 10px; margin-bottom: 16px; }
.msg-row.mine { flex-direction: row-reverse; }
.msg-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.msg-bubble-group { display: flex; flex-direction: column; max-width: 70%; }
.msg-row.mine .msg-bubble-group { align-items: flex-end; }
.msg-bubble {
  background: #fff;
  color: #4a3a2c;
  font-size: 14px;
  line-height: 1.6;
  padding: 10px 14px;
  border-radius: 4px 14px 14px 14px;
  box-shadow: 0 2px 10px rgba(242, 162, 104, 0.15);
  word-break: break-word;
  white-space: pre-wrap;
}
.msg-row.mine .msg-bubble {
  background: linear-gradient(135deg, #f7a76b, #f2a268);
  color: #fff;
  border-radius: 14px 4px 14px 14px;
  box-shadow: 0 3px 12px rgba(242, 162, 104, 0.35);
}
.msg-image { max-width: 180px; max-height: 220px; border-radius: 14px; cursor: zoom-in; box-shadow: 0 3px 12px rgba(0,0,0,0.12); }
.share-card {
  background: #fff;
  border: 1.5px solid #f2d9c0;
  border-radius: 14px;
  padding: 12px 14px;
  cursor: pointer;
  min-width: 160px;
  max-width: 220px;
  box-shadow: 0 2px 8px rgba(242, 162, 104, 0.1);
}
.msg-row.mine .share-card { border-color: #f2c89e; }
.share-tag { font-size: 12px; color: #e07e35; font-weight: 600; margin-bottom: 8px; }
.share-card-img { width: 100%; max-height: 120px; object-fit: cover; border-radius: 12px; margin-bottom: 8px; }
.share-card-text {
  font-size: 13px; color: #4a3a2c; line-height: 1.5; word-break: break-word;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.msg-meta { font-size: 10px; color: #b5a48e; margin-top: 6px; }
.input-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px calc(12px + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2px 10px rgba(242, 162, 104, 0.08);
}
.img-btn {
  width: 44px; height: 44px; border-radius: 14px; border: 1.5px solid #f2d9c0;
  background: #fff; font-size: 18px; cursor: pointer; flex-shrink: 0; transition: all .15s;
}
.img-btn:hover { border-color: #f2a268; background: #fff5ec; }
.img-btn:active { transform: scale(0.94); }
.input-bar input {
  flex: 1; height: 44px; border: 1.5px solid #ffe0c7; border-radius: 22px;
  padding: 0 16px; outline: none; background: #fffaf4; font-size: 14px;
}
.send-btn {
  height: 44px; padding: 0 20px; border: none; border-radius: 14px;
  background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff; font-size: 14px; cursor: pointer;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32); transition: all .15s;
}
.send-btn:hover:not(:disabled) { filter: brightness(1.05); transform: translateY(-1px); box-shadow: 0 7px 18px rgba(242, 162, 104, 0.4); }
.send-btn:active:not(:disabled) { transform: scale(0.96); }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.preview-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.75); z-index: 60;
  display: flex; align-items: center; justify-content: center; cursor: zoom-out;
}
.preview-mask img { max-width: 92vw; max-height: 88vh; border-radius: 12px; }
.placeholder { display: flex; align-items: center; justify-content: center; background: #fff1e0; font-size: 16px; }
</style>
