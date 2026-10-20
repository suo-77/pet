<script setup>
/**
 * 他人主页：头像昵称简介 + 关注/粉丝数 + 关注/取关 + 发私信 + TA的动态分页
 * 关注状态与计数来自 userHome 接口（Redis 缓存优化）
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { followApi, unfollowApi, userHomeApi } from '../api/user'
import { postPageApi } from '../api/post'
import { useUserStore } from '../stores/user'
import { addToast } from '../utils/toast'
import { friendTime } from '../utils/time'
import EmptyState from '../components/EmptyState.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userId = Number(route.params.id)
const profile = ref(null)
const loading = ref(true)
const isMe = computed(() => userStore.userInfo?.id === userId)
const followPending = ref(false)

/* TA的动态 */
const posts = ref([])
const postsTotal = ref(0)
const page = ref(1)
const postsLoading = ref(false)
const finished = ref(false)

async function loadProfile() {
  try {
    profile.value = await userHomeApi(userId)
  } finally {
    loading.value = false
  }
}

async function loadPosts(reset = false) {
  if (postsLoading.value || (finished.value && !reset)) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  postsLoading.value = true
  try {
    const data = await postPageApi({ userId, page: page.value, pageSize: 5 })
    postsTotal.value = data.total
    if (reset) posts.value = data.list
    else posts.value.push(...data.list)
    if (posts.value.length >= data.total || data.list.length === 0) finished.value = true
    else page.value++
  } finally {
    postsLoading.value = false
  }
}

/** 关注 / 取关 */
async function toggleFollow() {
  if (!userStore.isLogin) {
    addToast('登录后才能关注', 'error')
    router.push('/login?redirect=/user/' + userId)
    return
  }
  if (followPending.value) return
  followPending.value = true
  try {
    if (profile.value.followed) {
      await unfollowApi(userId)
      profile.value.followed = false
      profile.value.fanCount = Math.max(0, profile.value.fanCount - 1)
      addToast('已取消关注', 'success')
    } else {
      await followApi(userId)
      profile.value.followed = true
      profile.value.fanCount++
      addToast('关注成功，去看看TA的动态吧', 'success')
    }
  } finally {
    followPending.value = false
  }
}

onMounted(() => {
  loadProfile()
  loadPosts(true)
})
</script>

<template>
  <div class="user-home-page">
    <header class="page-head">
      <span class="back" @click="router.back()">‹</span>
      <span class="head-title">{{ isMe ? '我的主页' : 'TA的主页' }}</span>
    </header>

    <LoadingSpinner v-if="loading" />
    <template v-else-if="profile">
      <!-- 资料卡 -->
      <section class="home-card">
        <div class="home-top">
          <img v-if="profile.avatar" class="home-avatar" :src="profile.avatar" alt="头像" />
          <span v-else class="home-avatar placeholder">🐱</span>
          <div class="home-info">
            <p class="home-name">{{ profile.nickname }}</p>
            <p class="home-intro">{{ profile.intro || '这个铲屎官很神秘' }}</p>
            <div class="home-stats">
              <span><b>{{ profile.followingCount }}</b> 关注</span>
              <span><b>{{ profile.fanCount }}</b> 粉丝</span>
              <span><b>{{ postsTotal }}</b> 动态</span>
            </div>
          </div>
        </div>
        <div v-if="!isMe" class="home-btns">
          <button class="follow-btn" :class="{ followed: profile.followed }" :disabled="followPending" @click="toggleFollow">
            {{ profile.followed ? '已关注' : '＋ 关注' }}
          </button>
          <button class="chat-btn" @click="router.push('/chat/' + userId)">💬 私信</button>
        </div>
      </section>

      <!-- TA的动态 -->
      <section class="posts-section">
        <p class="section-label">TA的动态</p>
        <LoadingSpinner v-if="postsLoading && !posts.length" />
        <EmptyState v-else-if="!posts.length" text="还没有发布过动态" icon="📝" />
        <article v-for="post in posts" :key="post.id" class="post-card">
          <p class="post-content">{{ post.content }}</p>
          <div v-if="post.images && post.images.length" class="post-imgs">
            <img v-for="img in post.images.slice(0, 3)" :key="img" :src="img" alt="图片" loading="lazy" />
          </div>
          <p class="post-foot">{{ friendTime(post.createTime) }} · ❤️ {{ post.likeCount }} · 💬 {{ post.commentCount }}</p>
        </article>
        <LoadingSpinner v-if="postsLoading && posts.length" text="加载更多..." />
        <button v-if="!finished && posts.length && !postsLoading" class="load-more" @click="loadPosts()">加载更多</button>
      </section>
    </template>
  </div>
</template>

<style scoped>
/* 他人主页：暖橘渐变风格 */
.user-home-page { min-height: 100vh; background: #fff6ec; padding-bottom: 30px; }
.page-head {
  display: flex; align-items: center; padding: 12px 16px;
  position: sticky; top: 0; background: rgba(255, 246, 236, 0.94);
  backdrop-filter: blur(8px); z-index: 10;
}
.back { font-size: 26px; color: #6b4f3a; cursor: pointer; line-height: 1; }
.head-title { margin-left: 12px; font-size: 17px; font-weight: 600; color: #6b4f3a; }
.home-card {
  margin: 16px 16px 0; background: #fff; border-radius: 14px; padding: 20px 18px;
  box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1);
}
.home-top { display: flex; gap: 14px; }
.home-avatar { width: 68px; height: 68px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.home-info { flex: 1; min-width: 0; }
.home-name { font-size: 19px; font-weight: 700; color: #6b4f3a; }
.home-intro { font-size: 13px; color: #b5a48e; margin-top: 5px; line-height: 1.5; }
.home-stats { display: flex; gap: 18px; margin-top: 14px; font-size: 12px; color: #9b8b76; }
.home-stats b { color: #ea7a38; font-size: 15px; }
.home-btns { display: flex; gap: 14px; margin-top: 18px; }
.follow-btn {
  flex: 1; height: 42px; border-radius: 14px; border: 1px solid #f2a268;
  background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff; font-size: 14px; cursor: pointer;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
  transition: all 0.15s;
}
.follow-btn:hover { filter: brightness(1.05); transform: translateY(-1px); box-shadow: 0 7px 18px rgba(242, 162, 104, 0.4); }
.follow-btn:active { transform: scale(0.96); }
.follow-btn.followed { background: #f5efe4; border-color: #e5d4bd; color: #b5a48e; box-shadow: none; }
.follow-btn.followed:hover { filter: none; transform: none; }
.follow-btn:disabled { opacity: 0.6; cursor: wait; }
.chat-btn {
  flex: 1; height: 42px; border-radius: 14px; border: 1.5px solid #f2d9c0;
  background: #fff; color: #b9824f; font-size: 14px; cursor: pointer;
}
.chat-btn:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.chat-btn:active { transform: scale(0.94); }
.posts-section { padding: 16px 16px 0; }
.section-label { font-size: 15px; font-weight: 600; color: #6b4f3a; margin: 0 0 14px; }
.post-card { background: #fff; border-radius: 16px; padding: 16px; margin-bottom: 12px; box-shadow: 0 4px 14px rgba(255, 140, 66, 0.1); }
.post-content { font-size: 14px; color: #4a3a2c; white-space: pre-wrap; word-break: break-word; line-height: 1.7; }
.post-imgs { display: flex; gap: 10px; margin-top: 12px; }
.post-imgs img { width: 84px; height: 84px; border-radius: 12px; object-fit: cover; }
.post-foot { margin-top: 14px; font-size: 12px; color: #b5a48e; }
.load-more {
  display: block; margin: 14px auto; border: 1.5px solid #ffcda5; background: #fff;
  color: #df7e38; border-radius: 18px; padding: 8px 26px; font-size: 13px; cursor: pointer;
}
.load-more:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.load-more:active { transform: scale(0.94); }
.placeholder { display: flex; align-items: center; justify-content: center; background: #fff1e0; font-size: 28px; }
</style>
