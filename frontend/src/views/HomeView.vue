<script setup>
/**
 * 首页：社区信息流 + 聚合搜索 + 店铺入口 + 悬浮发布按钮
 * 1. 信息流分页加载，卡片含作者信息/多图九宫格/点赞(Redis计数)/评论(弹层分页)
 * 2. 搜索框聚合搜索店铺/宠物/用户
 * 3. 店铺入口卡片展示在信息流下方
 */
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { commentPostApi, commentsApi, likePostApi, postPageApi, unlikePostApi } from '../api/post'
import { sharePostApi } from '../api/chat'
import { followingApi } from '../api/user'
import { searchApi } from '../api/common'
import { shopPageApi } from '../api/shop'
import { useUserStore } from '../stores/user'
import { addToast } from '../utils/toast'
import { friendTime } from '../utils/time'
import EmptyState from '../components/EmptyState.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import TabBar from '../components/TabBar.vue'

const router = useRouter()
const userStore = useUserStore()

/* ---------------- 信息流 ---------------- */
const feed = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 5
const loading = ref(false)
const finished = ref(false)

async function loadFeed(reset = false) {
  if (loading.value || (finished.value && !reset)) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  loading.value = true
  try {
    const data = await postPageApi({ page: page.value, pageSize })
    total.value = data.total
    if (reset) feed.value = data.list
    else feed.value.push(...data.list)
    if (feed.value.length >= data.total || data.list.length === 0) finished.value = true
    else page.value++
  } finally {
    loading.value = false
  }
}

/* ---------------- 点赞（Redis 计数 + Stream 异步落库） ---------------- */
async function toggleLike(post) {
  if (!userStore.isLogin) {
    addToast('登录后才能点赞哦', 'error')
    router.push('/login?redirect=/')
    return
  }
  if (post.liked) {
    await unlikePostApi(post.id)
    post.liked = false
    post.likeCount = Math.max(0, post.likeCount - 1)
  } else {
    await likePostApi(post.id)
    post.liked = true
    post.likeCount++
  }
}

/* ---------------- 评论弹层 ---------------- */
const commentBox = reactive({ visible: false, postId: null, list: [], total: 0, page: 1, content: '', loading: false, sending: false })

async function openComments(post) {
  commentBox.visible = true
  commentBox.postId = post.id
  commentBox.list = []
  commentBox.total = 0
  commentBox.page = 1
  commentBox.content = ''
  await loadComments()
}

async function loadComments() {
  commentBox.loading = true
  try {
    const data = await commentsApi(commentBox.postId, { page: commentBox.page, pageSize: 10 })
    commentBox.total = data.total
    if (commentBox.page === 1) commentBox.list = data.list
    else commentBox.list.push(...data.list)
  } finally {
    commentBox.loading = false
  }
}

async function sendComment() {
  if (!userStore.isLogin) {
    addToast('登录后才能评论哦', 'error')
    return
  }
  const content = commentBox.content.trim()
  if (!content) {
    addToast('评论内容不能为空', 'error')
    return
  }
  commentBox.sending = true
  try {
    await commentPostApi(commentBox.postId, { content })
    commentBox.content = ''
    // 回到第一页刷新，评论数 +1
    commentBox.page = 1
    await loadComments()
    const post = feed.value.find((p) => p.id === commentBox.postId)
    if (post) post.commentCount++
  } finally {
    commentBox.sending = false
  }
}

/* ---------------- 分享动态给好友私信 ---------------- */
const shareBox = reactive({ visible: false, postId: null, friends: [], page: 1, total: 0, loading: false, finished: false, sendingId: null })

async function openShare(post) {
  if (!userStore.isLogin) {
    addToast('登录后才能分享哦', 'error')
    router.push('/login?redirect=/')
    return
  }
  shareBox.visible = true
  shareBox.postId = post.id
  shareBox.page = 1
  shareBox.total = 0
  shareBox.finished = false
  await loadShareFriends(true)
}

async function loadShareFriends(reset = false) {
  if (shareBox.loading) return
  shareBox.loading = true
  try {
    const data = await followingApi({ page: shareBox.page, pageSize: 10 })
    shareBox.total = data.total
    if (reset) shareBox.friends = data.list
    else shareBox.friends.push(...data.list)
    if (shareBox.friends.length >= data.total || data.list.length === 0) shareBox.finished = true
    else shareBox.page++
  } finally {
    shareBox.loading = false
  }
}

/** 分享给指定好友 */
async function doShare(friend) {
  shareBox.sendingId = friend.id
  try {
    await sharePostApi(shareBox.postId, friend.id)
    addToast(`已分享给「${friend.nickname}」`, 'success')
    shareBox.visible = false
  } finally {
    shareBox.sendingId = null
  }
}

/* ---------------- 聚合搜索 ---------------- */
const keyword = ref('')
const searching = ref(false)
const searchResult = ref(null)

async function onSearch() {
  const kw = keyword.value.trim()
  if (!kw) {
    searchResult.value = null
    return
  }
  searching.value = true
  try {
    searchResult.value = await searchApi(kw)
  } finally {
    searching.value = false
  }
}

function clearSearch() {
  keyword.value = ''
  searchResult.value = null
}

/* ---------------- 店铺入口 ---------------- */
const shops = ref([])

onMounted(async () => {
  await loadFeed(true)
  try {
    const data = await shopPageApi({ page: 1, pageSize: 3 })
    shops.value = data.list
  } catch (e) {
    // 店铺加载失败不阻塞信息流
  }
})
</script>

<template>
  <div class="home-page">
    <!-- 顶部搜索 -->
    <header class="home-header">
      <div class="search-bar">
        <span class="search-icon">🔍</span>
        <input v-model="keyword" placeholder="搜索店铺 / 宠物 / 铲屎官" @keyup.enter="onSearch" />
        <span v-if="keyword" class="clear-btn" @click="clearSearch">×</span>
        <button class="search-btn" :disabled="searching" @click="onSearch">搜索</button>
      </div>
    </header>

    <!-- 搜索结果 -->
    <section v-if="searchResult" class="search-result">
      <div class="section-title">
        搜索结果
        <span class="link" @click="clearSearch">收起</span>
      </div>
      <EmptyState v-if="!searchResult.shops.length && !searchResult.goods.length && !searchResult.users.length" text="没有找到相关内容" icon="🔎" />
      <template v-else>
        <div v-if="searchResult.shops.length" class="result-group">
          <p class="group-label">宠物店铺</p>
          <div v-for="s in searchResult.shops" :key="'s' + s.id" class="result-item" @click="router.push('/shops/' + s.id)">
            <span class="result-icon">🏪</span>{{ s.shopName }}
          </div>
        </div>
        <div v-if="searchResult.goods.length" class="result-group">
          <p class="group-label">宠物宝贝</p>
          <div v-for="g in searchResult.goods" :key="'g' + g.id" class="result-item" @click="router.push('/goods/' + g.id)">
            <span class="result-icon">🐱</span>{{ g.petName }}<small class="price">￥{{ g.price }}</small>
          </div>
        </div>
        <div v-if="searchResult.users.length" class="result-group">
          <p class="group-label">铲屎官</p>
          <div v-for="u in searchResult.users" :key="'u' + u.id" class="result-item" @click="router.push('/user/' + u.id)">
            <img v-if="u.avatar" class="mini-avatar" :src="u.avatar" alt="头像" />
            <span v-else class="mini-avatar placeholder">🐱</span>{{ u.nickname }}
          </div>
        </div>
      </template>
    </section>

    <!-- 社区信息流 -->
    <main class="feed">
      <LoadingSpinner v-if="loading && feed.length === 0" />
      <EmptyState v-else-if="feed.length === 0" text="还没有动态，点右下角发布第一条吧" icon="📝" />
      <article v-for="post in feed" :key="post.id" class="post-card">
        <div class="post-head">
          <img v-if="post.avatar" class="post-avatar" :src="post.avatar" alt="头像" @click="router.push('/user/' + post.userId)" />
          <span v-else class="post-avatar placeholder" @click="router.push('/user/' + post.userId)">🐱</span>
          <div class="post-user" @click="router.push('/user/' + post.userId)">
            <span class="post-nickname">{{ post.nickname }}</span>
            <span class="post-time">{{ friendTime(post.createTime) }}</span>
          </div>
        </div>
        <p class="post-content">{{ post.content }}</p>
        <!-- 图片九宫格 -->
        <div v-if="post.images && post.images.length" class="post-imgs" :class="'n' + Math.min(post.images.length, 9)">
          <img v-for="img in post.images" :key="img" :src="img" alt="动态图片" loading="lazy" />
        </div>
        <div class="post-actions">
          <span :class="{ liked: post.liked }" @click="toggleLike(post)">
            {{ post.liked ? '❤️' : '🤍' }} {{ post.likeCount }}
          </span>
          <span class="share-btn" @click="openShare(post)">↗️ 分享</span>
          <span @click="openComments(post)">💬 {{ post.commentCount }}</span>
        </div>
      </article>
      <LoadingSpinner v-if="loading && feed.length > 0" text="加载更多..." />
      <button v-if="!finished && feed.length > 0 && !loading" class="load-more" @click="loadFeed()">加载更多</button>
      <p v-else-if="finished && feed.length > 0" class="feed-end">— 到底啦 —</p>
    </main>

    <!-- 店铺入口 -->
    <section class="shop-entry">
      <div class="section-title">
        宠物店铺
        <span class="link" @click="router.push('/shops')">全部店铺 ›</span>
      </div>
      <div class="shop-grid">
        <div v-for="shop in shops" :key="shop.id" class="shop-card" @click="router.push('/shops/' + shop.id)">
          <img v-if="shop.cover" :src="shop.cover" alt="店铺封面" />
          <div v-else class="shop-cover-placeholder">🏪</div>
          <div class="shop-info">
            <p class="shop-name">{{ shop.shopName }}</p>
            <p class="shop-desc">{{ shop.shopDesc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 悬浮发布按钮 -->
    <div class="fab" @click="router.push('/publish')">＋</div>

    <!-- 评论弹层 -->
    <div v-if="commentBox.visible" class="modal-mask" @click.self="commentBox.visible = false">
      <div class="comment-modal">
        <div class="modal-head">
          <span>评论 ({{ commentBox.total }})</span>
          <span class="close" @click="commentBox.visible = false">×</span>
        </div>
        <div class="comment-list">
          <LoadingSpinner v-if="commentBox.loading && !commentBox.list.length" />
          <EmptyState v-else-if="!commentBox.list.length" text="暂无评论，快来抢沙发" icon="💬" />
          <div v-for="c in commentBox.list" :key="c.id" class="comment-item">
            <img v-if="c.avatar" class="comment-avatar" :src="c.avatar" alt="头像" />
            <span v-else class="comment-avatar placeholder">🐱</span>
            <div class="comment-body">
              <p class="comment-meta">{{ c.nickname }} · {{ friendTime(c.createTime) }}</p>
              <p class="comment-content">{{ c.content }}</p>
            </div>
          </div>
          <button v-if="commentBox.list.length < commentBox.total" class="load-more" :disabled="commentBox.loading" @click="commentBox.page++; loadComments()">
            查看更多评论
          </button>
        </div>
        <div class="comment-input">
          <input v-model="commentBox.content" :placeholder="userStore.isLogin ? '说点什么...' : '登录后才能评论'" maxlength="200" @keyup.enter="sendComment" />
          <button class="btn-primary" :disabled="commentBox.sending" @click="sendComment">发送</button>
        </div>
      </div>
    </div>

    <!-- 分享给好友弹层 -->
    <div v-if="shareBox.visible" class="modal-mask" @click.self="shareBox.visible = false">
      <div class="share-modal">
        <div class="modal-head">
          <span>分享给好友</span>
          <span class="close" @click="shareBox.visible = false">×</span>
        </div>
        <div class="share-list">
          <LoadingSpinner v-if="shareBox.loading && !shareBox.friends.length" />
          <EmptyState v-else-if="!shareBox.friends.length" text="还没有可分享的好友，先去关注几个铲屎官吧" icon="👥" />
          <div v-for="f in shareBox.friends" :key="f.id" class="share-item" @click="doShare(f)">
            <img v-if="f.avatar" class="share-avatar" :src="f.avatar" alt="头像" />
            <span v-else class="share-avatar placeholder">🐱</span>
            <div class="share-info">
              <p class="share-name">{{ f.nickname }}</p>
              <p class="share-intro">{{ f.intro || '这个铲屎官很神秘' }}</p>
            </div>
            <span class="share-btn-mini" :class="{ sending: shareBox.sendingId === f.id }">
              {{ shareBox.sendingId === f.id ? '分享中...' : '分享' }}
            </span>
          </div>
          <button v-if="!shareBox.finished && shareBox.friends.length && !shareBox.loading" class="load-more" @click="loadShareFriends()">加载更多</button>
        </div>
      </div>
    </div>

    <TabBar />
  </div>
</template>

<style scoped>
.home-page { padding-bottom: 76px; }
.home-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 246, 236, 0.92);
  backdrop-filter: blur(8px);
  padding: 14px 16px 8px;
}
.search-bar {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 22px;
  padding: 6px 6px 6px 16px;
  box-shadow: 0 4px 14px rgba(255, 140, 66, 0.12);
}
.search-icon { margin-right: 8px; }
.search-bar input { flex: 1; border: none; outline: none; font-size: 14px; background: transparent; }
.clear-btn { color: #b5a48e; padding: 0 8px; cursor: pointer; display: flex; align-items: center; }
.search-btn {
  border: none;
  background: linear-gradient(135deg, #f7a76b, #f2a268);
  color: #fff;
  border-radius: 14px;
  padding: 7px 16px;
  font-size: 13px;
  cursor: pointer;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
  transition: transform 0.15s, filter 0.15s, box-shadow 0.15s;
}
.search-btn:hover { filter: brightness(1.05); transform: translateY(-1px); box-shadow: 0 7px 18px rgba(242, 162, 104, 0.4); }
.search-btn:active { transform: scale(0.96); }
.btn-primary {
  border: none;
  background: linear-gradient(135deg, #f7a76b, #f2a268);
  color: #fff;
  border-radius: 14px;
  min-width: 64px;
  cursor: pointer;
  transition: transform 0.15s, filter 0.15s, box-shadow 0.15s;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.3);
}
.btn-primary:hover { filter: brightness(1.05); transform: translateY(-1px); }
.btn-primary:active { transform: scale(0.96); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #6b4f3a;
  margin: 24px 0 14px;
}
.link { font-size: 12px; color: #df7e38; font-weight: normal; cursor: pointer; }
.search-result { padding: 8px 16px 0; }
.result-group { margin-bottom: 14px; }
.group-label { font-size: 12px; color: #b5a48e; margin-bottom: 8px; }
.result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #6b4f3a;
  cursor: pointer;
}
.result-item .price { margin-left: auto; color: #ea7a38; }
.mini-avatar { width: 24px; height: 24px; border-radius: 50%; object-fit: cover; }
.feed { padding: 16px 16px 8px; }
.post-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 14px;
  box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1);
}
.post-head { display: flex; align-items: center; gap: 12px; }
.post-avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.post-user { display: flex; flex-direction: column; cursor: pointer; gap: 2px; }
.post-nickname { font-size: 15px; font-weight: 600; color: #6b4f3a; }
.post-time { font-size: 12px; color: #b5a48e; }
.post-content { font-size: 15px; color: #4a3a2c; margin: 14px 0 0; white-space: pre-wrap; word-break: break-word; line-height: 1.7; }
.post-imgs { display: grid; gap: 8px; margin-top: 14px; }
.post-imgs.n1 { grid-template-columns: 1fr; }
.post-imgs.n1 img { height: auto; max-height: 320px; object-fit: cover; }
.post-imgs.n2, .post-imgs.n4 { grid-template-columns: 1fr 1fr; }
.post-imgs.n3, .post-imgs.n5, .post-imgs.n6, .post-imgs.n7, .post-imgs.n8, .post-imgs.n9 { grid-template-columns: 1fr 1fr 1fr; }
.post-imgs img { width: 100%; height: 110px; object-fit: cover; border-radius: 12px; }
.post-actions {
  display: flex;
  gap: 30px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed #ffe9d2;
  color: #9b8b76;
  font-size: 14px;
}
.post-actions span { cursor: pointer; user-select: none; }
.post-actions .liked { color: #ff4d4d; }
.post-actions .share-btn {
  color: #6b9d6b;
  background: #f0f7ef;
  border-radius: 14px;
  padding: 2px 12px;
  font-weight: 600;
}
.load-more {
  display: block;
  margin: 6px auto 0;
  border: 1.5px solid #f2d9c0;
  background: #fff;
  color: #b9824f;
  border-radius: 14px;
  padding: 8px 26px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}
.load-more:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.load-more:active { transform: scale(0.97); }
.load-more:disabled { opacity: 0.5; cursor: default; }
.feed-end { text-align: center; color: #cdbba4; font-size: 12px; padding: 10px 0; }
.shop-entry { margin-top: 16px; padding: 4px 16px 20px; }
.shop-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 14px; }
.shop-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.shop-card:hover { transform: translateY(-3px); box-shadow: 0 10px 26px rgba(242, 162, 104, 0.16); }
.shop-card img, .shop-cover-placeholder { width: 100%; height: 90px; object-fit: cover; display: flex; align-items: center; justify-content: center; font-size: 34px; background: #fff1e0; }
.shop-info { padding: 12px 14px 14px; }
.shop-name { font-size: 15px; font-weight: 600; color: #6b4f3a; }
.shop-desc {
  font-size: 12px;
  color: #b5a48e;
  margin-top: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fab {
  position: fixed;
  right: 22px;
  bottom: 96px;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f7a76b, #f2a268);
  color: #fff;
  font-size: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 22px rgba(242, 162, 104, 0.45);
  cursor: pointer;
  z-index: 30;
  transition: transform 0.2s, box-shadow 0.2s;
}
.fab:hover { transform: scale(1.06) translateY(-1px); box-shadow: 0 10px 26px rgba(242, 162, 104, 0.5); }
.fab:active { transform: scale(0.94); }
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(60, 40, 20, 0.45);
  z-index: 120;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.comment-modal {
  width: 100%;
  max-width: 560px;
  max-height: 70vh;
  background: #fff;
  border-radius: 16px 16px 0 0;
  box-shadow: 0 -8px 32px rgba(60, 40, 20, 0.16);
  display: flex;
  flex-direction: column;
  padding: 18px 16px 20px;
}
.modal-head {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
  color: #6b4f3a;
  margin-bottom: 12px;
}
.close { cursor: pointer; font-size: 20px; color: #b5a48e; padding: 0 4px; }
.comment-list { overflow-y: auto; flex: 1; padding: 4px 0; }
.comment-item { display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px dashed #f5e8d8; }
.comment-avatar { width: 34px; height: 34px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.comment-meta { font-size: 12px; color: #b5a48e; }
.comment-content { font-size: 14px; color: #4a3a2c; margin-top: 4px; word-break: break-word; line-height: 1.6; }
.comment-input { display: flex; gap: 12px; padding-top: 14px; border-top: 1px solid #f5e8d8; }
.comment-input input {
  flex: 1;
  height: 40px;
  border: 1.5px solid #ffe0c7;
  border-radius: 12px;
  padding: 0 12px;
  outline: none;
  background: #fffaf4;
}
.share-modal {
  width: 100%;
  max-width: 420px;
  max-height: 78vh;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(60, 40, 20, 0.18);
  padding: 18px;
  display: flex;
  flex-direction: column;
}
.share-list { overflow-y: auto; flex: 1; min-height: 0; margin: 0 -4px; padding: 0 4px; }
.share-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px dashed #f5e8d8;
  cursor: pointer;
}
.share-avatar { width: 42px; height: 42px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.share-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.share-name { font-size: 14px; font-weight: 600; color: #6b4f3a; }
.share-intro {
  font-size: 12px; color: #b5a48e;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.share-btn-mini {
  border: 1.5px solid #f2d9c0; background: #fff; color: #b9824f;
  border-radius: 14px; padding: 5px 14px; font-size: 12px; font-weight: 600;
  flex-shrink: 0; transition: all 0.15s;
}
.share-btn-mini:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.share-btn-mini:active { transform: scale(0.94); }
.share-btn-mini.sending { border-color: #e8dcc8; background: #f5efe4; color: #b5a48e; }
.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff1e0;
  font-size: 18px;
}
@media (max-width: 480px) {
  .shop-grid { grid-template-columns: 1fr 1fr; }
  .post-imgs img { height: 96px; }
}
</style>
