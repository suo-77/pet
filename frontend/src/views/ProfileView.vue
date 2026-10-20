<script setup>
/**
 * 个人中心：资料编辑 / 我的动态（可删除）/ 我的订单 / 三态优惠券 / 我的收藏 / 退出登录
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { commentsApi, deletePostApi, myPostApi, postLikesApi } from '../api/post'
import { updateProfileApi, userHomeApi } from '../api/user'
import { myCouponsApi } from '../api/coupon'
import { myFavsApi } from '../api/goods'
import { myOrdersApi, cancelOrderApi } from '../api/order'
import { useUserStore } from '../stores/user'
import { addToast } from '../utils/toast'
import { friendTime, fullTime } from '../utils/time'
import EmptyState from '../components/EmptyState.vue'
import ImgUpload from '../components/ImgUpload.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import TabBar from '../components/TabBar.vue'

const router = useRouter()
const userStore = useUserStore()
const meId = computed(() => userStore.userInfo?.id)

/* ---------------- 主页统计 ---------------- */
const home = ref({ followingCount: 0, fanCount: 0 })

onMounted(async () => {
  if (meId.value) {
    home.value = await userHomeApi(meId.value)
  }
  loadPosts(true)
})

/* ---------------- 资料编辑 ---------------- */
const editVisible = ref(false)
const editForm = ref({ nickname: '', intro: '', avatar: [] })
const saving = ref(false)

function openEdit() {
  editForm.value = {
    nickname: userStore.userInfo?.nickname || '',
    intro: userStore.userInfo?.intro || '',
    avatar: userStore.userInfo?.avatar ? [userStore.userInfo.avatar] : []
  }
  editVisible.value = true
}

async function saveProfile() {
  const nickname = editForm.value.nickname.trim()
  if (!nickname) {
    addToast('昵称不能为空', 'error')
    return
  }
  saving.value = true
  try {
    const user = await updateProfileApi({
      nickname,
      intro: editForm.value.intro.trim(),
      avatar: editForm.value.avatar[0] || userStore.userInfo?.avatar
    })
    userStore.setUser(user)
    addToast('资料已更新', 'success')
    editVisible.value = false
  } finally {
    saving.value = false
  }
}

/* ---------------- 内容板块 Tab ---------------- */
const activeSection = ref('posts')

const sections = [
  { key: 'posts', label: '我的动态' },
  { key: 'orders', label: '我的订单' },
  { key: 'coupons', label: '优惠券' },
  { key: 'favs', label: '我的收藏' }
]

/* 我的动态 */
const posts = ref([])
const postsTotal = ref(0)
const postsPage = ref(1)
const postsLoading = ref(false)
const postsFinished = ref(false)

async function loadPosts(reset = false) {
  if (postsLoading.value || (postsFinished.value && !reset)) return
  if (reset) {
    postsPage.value = 1
    postsFinished.value = false
  }
  postsLoading.value = true
  try {
    const data = await myPostApi({ page: postsPage.value, pageSize: 5 })
    postsTotal.value = data.total
    if (reset) posts.value = data.list
    else posts.value.push(...data.list)
    if (posts.value.length >= data.total || data.list.length === 0) postsFinished.value = true
    else postsPage.value++
  } finally {
    postsLoading.value = false
  }
}

/** 删除自己的动态（后端权限校验仅作者可删） */
async function removePost(post) {
  if (!window.confirm('确定删除这条动态吗？')) return
  await deletePostApi(post.id)
  posts.value = posts.value.filter((p) => p.id !== post.id)
  postsTotal.value = Math.max(0, postsTotal.value - 1)
  addToast('已删除', 'success')
}

/* ---------------- 谁赞了 / 谁评论了 弹层 ---------------- */
const whoBox = reactive({
  visible: false,
  mode: 'like', // like=点赞用户列表 comment=评论列表
  postId: null,
  list: [],
  total: 0,
  page: 1,
  loading: false
})

/** 查看谁点赞了我的动态 */
async function openLikers(post) {
  whoBox.visible = true
  whoBox.mode = 'like'
  whoBox.postId = post.id
  whoBox.list = []
  whoBox.total = 0
  whoBox.loading = true
  try {
    whoBox.list = await postLikesApi(post.id)
    whoBox.total = whoBox.list.length
  } finally {
    whoBox.loading = false
  }
}

/** 查看谁评论了我的动态（评论内容+时间，分页） */
async function openWhoComments(post) {
  whoBox.visible = true
  whoBox.mode = 'comment'
  whoBox.postId = post.id
  whoBox.list = []
  whoBox.total = 0
  whoBox.page = 1
  await loadWhoComments()
}

async function loadWhoComments() {
  whoBox.loading = true
  try {
    const data = await commentsApi(whoBox.postId, { page: whoBox.page, pageSize: 10 })
    whoBox.total = data.total
    if (whoBox.page === 1) whoBox.list = data.list
    else whoBox.list.push(...data.list)
  } finally {
    whoBox.loading = false
  }
}

/* 我的订单 */
const orders = ref([])
const ordersTotal = ref(0)
const ordersPage = ref(1)
const ordersLoading = ref(false)
const ordersFinished = ref(false)
const ordersLoaded = ref(false)

/** 订单状态文案：0待付款 1已付款 2已取消 */
function orderStatusText(status) {
  return { 0: '待付款', 1: '已付款', 2: '已取消' }[status] || '未知'
}

/** 取消订单：恢复库存 + 退回优惠券（后端校验仅本人已付款订单可取消） */
async function cancelOrder(order) {
  if (!window.confirm(`确定取消订单 ${order.orderNo} 吗？库存与优惠券将自动退回。`)) return
  await cancelOrderApi(order.id)
  order.status = 2
  addToast('订单已取消', 'success')
}

async function loadOrders(reset = false) {
  if (ordersLoading.value || (ordersFinished.value && !reset)) return
  if (reset) {
    ordersPage.value = 1
    ordersFinished.value = false
  }
  ordersLoading.value = true
  try {
    const data = await myOrdersApi({ page: ordersPage.value, pageSize: 5 })
    ordersTotal.value = data.total
    if (reset) orders.value = data.list
    else orders.value.push(...data.list)
    if (orders.value.length >= data.total || data.list.length === 0) ordersFinished.value = true
    else ordersPage.value++
    ordersLoaded.value = true
  } finally {
    ordersLoading.value = false
  }
}

/* 我的优惠券（0未使用 1已使用 2已过期） */
const couponStatus = ref(0)
const couponTabs = [
  { key: 0, label: '未使用' },
  { key: 1, label: '已使用' },
  { key: 2, label: '已过期' }
]
const coupons = ref([])
const couponsTotal = ref(0)
const couponsPage = ref(1)
const couponsLoading = ref(false)
const couponsFinished = ref(false)

async function loadCoupons(reset = false) {
  if (couponsLoading.value || (couponsFinished.value && !reset)) return
  if (reset) {
    couponsPage.value = 1
    couponsFinished.value = false
  }
  couponsLoading.value = true
  try {
    const data = await myCouponsApi({ status: couponStatus.value, page: couponsPage.value, pageSize: 6 })
    couponsTotal.value = data.total
    if (reset) coupons.value = data.list
    else coupons.value.push(...data.list)
    if (coupons.value.length >= data.total || data.list.length === 0) couponsFinished.value = true
    else couponsPage.value++
  } finally {
    couponsLoading.value = false
  }
}

function switchCouponTab(status) {
  couponStatus.value = status
  loadCoupons(true)
}

/* 我的收藏 */
const favs = ref([])
const favsTotal = ref(0)
const favsPage = ref(1)
const favsLoading = ref(false)
const favsFinished = ref(false)

async function loadFavs(reset = false) {
  if (favsLoading.value || (favsFinished.value && !reset)) return
  if (reset) {
    favsPage.value = 1
    favsFinished.value = false
  }
  favsLoading.value = true
  try {
    const data = await myFavsApi({ page: favsPage.value, pageSize: 6 })
    favsTotal.value = data.total
    if (reset) favs.value = data.list
    else favs.value.push(...data.list)
    if (favs.value.length >= data.total || data.list.length === 0) favsFinished.value = true
    else favsPage.value++
  } finally {
    favsLoading.value = false
  }
}

/** 板块切换时懒加载 */
function switchSection(key) {
  activeSection.value = key
  if (key === 'posts') loadPosts(true)
  else if (key === 'orders' && !ordersLoaded.value) loadOrders(true)
  else if (key === 'coupons') loadCoupons(true)
  else if (key === 'favs') loadFavs(true)
}

/* ---------------- 退出登录 ---------------- */
async function onLogout() {
  if (!window.confirm('确定退出登录吗？')) return
  await userStore.logout()
  addToast('已退出登录', 'success')
  router.replace('/login')
}
</script>

<template>
  <div class="profile-page">
    <!-- 头部资料卡 -->
    <section class="profile-card">
      <img v-if="userStore.userInfo?.avatar" class="my-avatar" :src="userStore.userInfo.avatar" alt="头像" />
      <span v-else class="my-avatar placeholder">🐱</span>
      <div class="profile-info">
        <p class="my-name">{{ userStore.userInfo?.nickname }}</p>
        <p class="my-intro">{{ userStore.userInfo?.intro || '这个铲屎官很神秘' }}</p>
        <div class="stats">
          <span @click="router.push('/friends')"><b>{{ home.followingCount }}</b> 关注</span>
          <span @click="router.push('/friends')"><b>{{ home.fanCount }}</b> 粉丝</span>
        </div>
      </div>
      <div class="profile-ops">
        <button class="shop-btn" @click="router.push('/shop/manage')">我的店铺</button>
        <button class="edit-btn" @click="openEdit">编辑资料</button>
        <button class="logout-btn" @click="onLogout">退出</button>
      </div>
    </section>

    <!-- 板块 Tab -->
    <div class="section-tabs">
      <span
        v-for="s in sections"
        :key="s.key"
        :class="{ active: activeSection === s.key }"
        @click="switchSection(s.key)"
      >{{ s.label }}</span>
    </div>

    <!-- 我的动态 -->
    <main v-if="activeSection === 'posts'" class="section-body">
      <LoadingSpinner v-if="postsLoading && !posts.length" />
      <EmptyState v-else-if="!posts.length" text="暂无动态，去发布第一条吧" icon="📝" />
      <article v-for="post in posts" :key="post.id" class="post-item">
        <p class="post-content">{{ post.content }}</p>
        <div v-if="post.images && post.images.length" class="post-thumb">
          <img v-for="img in post.images.slice(0, 3)" :key="img" :src="img" alt="图片" />
        </div>
        <div class="post-foot">
          <span>{{ friendTime(post.createTime) }}</span>
          <div class="post-stats">
            <span class="stat-btn" @click="openLikers(post)">❤️ {{ post.likeCount }} 人赞过</span>
            <span class="stat-btn" @click="openWhoComments(post)">💬 {{ post.commentCount }} 条评论</span>
            <span class="del" @click="removePost(post)">删除</span>
          </div>
        </div>
      </article>
      <LoadingSpinner v-if="postsLoading && posts.length" text="加载更多..." />
      <button v-if="!postsFinished && posts.length && !postsLoading" class="load-more" @click="loadPosts()">加载更多</button>
    </main>

    <!-- 我的订单 -->
    <main v-else-if="activeSection === 'orders'" class="section-body">
      <LoadingSpinner v-if="ordersLoading && !orders.length" />
      <EmptyState v-else-if="!orders.length" text="暂无订单，去店铺看看毛孩子吧" icon="🧾" />
      <div v-for="order in orders" :key="order.id" class="order-item">
        <div class="order-top">
          <span class="order-no">订单号 {{ order.orderNo }}</span>
          <span class="order-status" :class="{ done: order.status === 1, cancelled: order.status === 2 }">{{ orderStatusText(order.status) }}</span>
        </div>
        <div class="order-body">
          <span class="order-name">{{ order.petName }} × {{ order.goodsNum }}</span>
          <span class="order-amount">￥{{ order.totalAmount }}</span>
        </div>
        <p v-if="order.discountAmount > 0" class="order-discount">优惠券已抵扣 ￥{{ order.discountAmount }}</p>
        <p class="order-time">{{ fullTime(order.createTime) }}</p>
        <div v-if="order.status === 1" class="order-ops">
          <button class="cancel-order-btn" @click="cancelOrder(order)">取消订单</button>
        </div>
      </div>
      <LoadingSpinner v-if="ordersLoading && orders.length" text="加载更多..." />
      <button v-if="!ordersFinished && orders.length && !ordersLoading" class="load-more" @click="loadOrders()">加载更多</button>
    </main>

    <!-- 优惠券三态 -->
    <main v-else-if="activeSection === 'coupons'" class="section-body">
      <div class="coupon-status-tabs">
        <span
          v-for="t in couponTabs"
          :key="t.key"
          :class="{ active: couponStatus === t.key }"
          @click="switchCouponTab(t.key)"
        >{{ t.label }}</span>
      </div>
      <LoadingSpinner v-if="couponsLoading && !coupons.length" />
      <EmptyState v-else-if="!coupons.length" text="这里还没有券，去店铺领一张吧" icon="🎫" />
      <div v-for="coupon in coupons" :key="coupon.id" class="coupon-item" :class="{ disabled: couponStatus !== 0 }">
        <div class="coupon-amount">￥{{ coupon.money }}</div>
        <div class="coupon-detail">
          <p class="coupon-title">{{ coupon.couponName }}</p>
          <p class="coupon-expire">{{ fullTime(coupon.expireTime) }} 前有效</p>
        </div>
        <span class="coupon-state">{{ ['未使用', '已使用', '已过期'][couponStatus] }}</span>
      </div>
      <LoadingSpinner v-if="couponsLoading && coupons.length" text="加载更多..." />
      <button v-if="!couponsFinished && coupons.length && !couponsLoading" class="load-more" @click="loadCoupons()">加载更多</button>
    </main>

    <!-- 我的收藏 -->
    <main v-else class="section-body">
      <LoadingSpinner v-if="favsLoading && !favs.length" />
      <EmptyState v-else-if="!favs.length" text="暂无收藏，去商品页点个❤️吧" icon="⭐" />
      <div class="fav-grid">
        <div v-for="fav in favs" :key="fav.id" class="fav-card" @click="router.push('/goods/' + fav.id)">
          <img v-if="fav.images && fav.images.length" :src="fav.images[0]" alt="宠物图片" />
          <div v-else class="fav-img-placeholder">🐱</div>
          <p class="fav-name">{{ fav.petName }}</p>
          <span class="fav-price">￥{{ fav.price }}</span>
        </div>
      </div>
      <LoadingSpinner v-if="favsLoading && favs.length" text="加载更多..." />
      <button v-if="!favsFinished && favs.length && !favsLoading" class="load-more" @click="loadFavs()">加载更多</button>
    </main>

    <!-- 编辑资料弹层 -->
    <div v-if="editVisible" class="modal-mask" @click.self="editVisible = false">
      <div class="edit-modal">
        <p class="edit-title">编辑资料</p>
        <div class="edit-avatar">
          <ImgUpload v-model="editForm.avatar" :limit="1" />
          <span class="edit-avatar-tip">更换头像</span>
        </div>
        <input v-model.trim="editForm.nickname" maxlength="20" placeholder="昵称" />
        <textarea v-model="editForm.intro" rows="3" maxlength="100" placeholder="个人简介"></textarea>
        <div class="edit-btns">
          <button class="cancel-btn" @click="editVisible = false">取消</button>
          <button class="btn-primary save-btn" :disabled="saving" @click="saveProfile">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 谁赞了 / 谁评论了 弹层 -->
    <div v-if="whoBox.visible" class="modal-mask" @click.self="whoBox.visible = false">
      <div class="who-modal">
        <div class="modal-head">
          <span>{{ whoBox.mode === 'like' ? `点赞的人 (${whoBox.total})` : `评论详情 (${whoBox.total})` }}</span>
          <span class="close" @click="whoBox.visible = false">×</span>
        </div>
        <div class="who-list">
          <LoadingSpinner v-if="whoBox.loading && !whoBox.list.length" />
          <EmptyState v-else-if="!whoBox.list.length" :text="whoBox.mode === 'like' ? '还没有人点赞' : '还没有人评论'" icon="🌱" />
          <!-- 点赞用户列表 -->
          <template v-else-if="whoBox.mode === 'like'">
            <div v-for="u in whoBox.list" :key="u.userId" class="who-item" @click="router.push('/user/' + u.userId)">
              <img v-if="u.avatar" class="who-avatar" :src="u.avatar" alt="头像" />
              <span v-else class="who-avatar placeholder">🐱</span>
              <div class="who-info">
                <p class="who-name">{{ u.nickname }}</p>
                <p v-if="u.createTime" class="who-time">{{ fullTime(u.createTime) }}</p>
              </div>
              <span class="who-icon">❤️</span>
            </div>
          </template>
          <!-- 评论列表 -->
          <template v-else>
            <div v-for="c in whoBox.list" :key="c.id" class="who-item" @click="router.push('/user/' + c.userId)">
              <img v-if="c.avatar" class="who-avatar" :src="c.avatar" alt="头像" />
              <span v-else class="who-avatar placeholder">🐱</span>
              <div class="who-info">
                <p class="who-name">{{ c.nickname }} <span class="who-time">· {{ friendTime(c.createTime) }}</span></p>
                <p class="who-content">{{ c.content }}</p>
              </div>
            </div>
            <button v-if="whoBox.list.length < whoBox.total" class="load-more" :disabled="whoBox.loading" @click="whoBox.page++; loadWhoComments()">
              查看更多评论
            </button>
          </template>
        </div>
      </div>
    </div>

    <TabBar />
  </div>
</template>

<style scoped>
/* 个人中心：暖橘渐变风格 */
.profile-page { min-height: 100vh; background: #fff6ec; padding-bottom: 76px; }
.profile-card {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 16px 16px 0;
  background: linear-gradient(135deg, #fff, #fff2e2);
  border-radius: 20px;
  padding: 20px 18px;
  box-shadow: 0 6px 20px rgba(255, 140, 66, 0.12);
}
.my-avatar { width: 64px; height: 64px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.profile-info { flex: 1; min-width: 0; }
.my-name { font-size: 18px; font-weight: 700; color: #6b4f3a; }
.my-intro {
  font-size: 12px; color: #b5a48e; margin-top: 6px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.stats { display: flex; gap: 18px; margin-top: 12px; font-size: 12px; color: #9b8b76; }
.stats b { color: #ea7a38; font-size: 14px; }
.stats span { cursor: pointer; }
.profile-ops { display: flex; flex-direction: column; gap: 10px; }
.shop-btn {
  border: none; background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff;
  border-radius: 14px; padding: 7px 14px; font-size: 12px; cursor: pointer;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
}
.shop-btn:hover { filter: brightness(1.05); }
.shop-btn:active { transform: scale(0.95); }
.edit-btn {
  border: 1.5px solid #ffcda5; background: #fff; color: #df7e38;
  border-radius: 14px; padding: 7px 14px; font-size: 12px; cursor: pointer;
}
.edit-btn:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.edit-btn:active { transform: scale(0.94); }
.logout-btn {
  border: 1.5px solid #e8dcc8; background: #f3ede2; color: #9b8b76;
  border-radius: 14px; padding: 7px 14px; font-size: 12px; cursor: pointer;
}
.logout-btn:hover { border-color: #e5533c; color: #e5533c; background: #fff5f2; }
.logout-btn:active { transform: scale(0.94); }
.section-tabs { display: flex; gap: 10px; padding: 16px 16px 0; }
.section-tabs span {
  flex: 1; text-align: center; padding: 11px 0; background: #fff;
  border-radius: 14px; font-size: 13px; color: #9b8b76; cursor: pointer;
  box-shadow: 0 2px 8px rgba(255, 140, 66, 0.08);
}
.section-tabs span.active { background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff; }
.section-body { padding: 16px; }
.post-item { background: #fff; border-radius: 14px; padding: 16px; margin-bottom: 12px; box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1); }
.post-content { font-size: 14px; color: #4a3a2c; white-space: pre-wrap; word-break: break-word; line-height: 1.7; }
.post-thumb { display: flex; gap: 8px; margin-top: 12px; }
.post-thumb img { width: 74px; height: 74px; border-radius: 12px; object-fit: cover; }
.post-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 14px; font-size: 12px; color: #b5a48e; }
.del { color: #e5533c; cursor: pointer; }
.order-item { background: #fff; border-radius: 16px; padding: 16px; margin-bottom: 12px; box-shadow: 0 4px 14px rgba(255, 140, 66, 0.1); }
.order-top { display: flex; justify-content: space-between; font-size: 12px; color: #b5a48e; }
.order-status { color: #df7e38; }
.order-status.done { color: #52a86a; }
.order-status.cancelled { color: #b5a48e; }
.order-discount { font-size: 11px; color: #52a86a; margin-top: 8px; }
.order-ops { display: flex; justify-content: flex-end; margin-top: 12px; }
.cancel-order-btn {
  border: 1.5px solid #f2d9c0; background: #fff; color: #b9824f;
  border-radius: 14px; padding: 6px 16px; font-size: 12px; cursor: pointer;
}
.cancel-order-btn:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.cancel-order-btn:active { transform: scale(0.94); }
.order-body { display: flex; justify-content: space-between; margin-top: 10px; font-size: 15px; color: #4a3a2c; }
.order-amount { color: #ea7a38; font-weight: 700; }
.order-time { font-size: 11px; color: #cdbba4; margin-top: 10px; }
.coupon-status-tabs { display: flex; gap: 10px; margin-bottom: 14px; }
.coupon-status-tabs span {
  flex: 1; text-align: center; padding: 9px 0; background: #fff;
  border-radius: 12px; font-size: 13px; color: #9b8b76; cursor: pointer;
}
.coupon-status-tabs span.active { background: #ffe3c9; color: #c96a1f; font-weight: 600; }
.coupon-item {
  display: flex; align-items: center; gap: 14px;
  background: #fff; border-radius: 14px; padding: 14px; margin-bottom: 10px;
  box-shadow: 0 4px 14px rgba(255, 140, 66, 0.1);
}
.coupon-item.disabled { opacity: 0.55; }
.coupon-amount { font-size: 22px; font-weight: 700; color: #ea7a38; min-width: 64px; }
.coupon-detail { flex: 1; }
.coupon-title { font-size: 14px; color: #6b4f3a; }
.coupon-expire { font-size: 11px; color: #b5a48e; margin-top: 4px; }
.coupon-state { font-size: 12px; color: #df7e38; }
.fav-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 14px; }
.fav-card { background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 14px rgba(255, 140, 66, 0.1); cursor: pointer; }
.fav-card img, .fav-img-placeholder { width: 100%; height: 100px; object-fit: cover; display: flex; align-items: center; justify-content: center; font-size: 32px; background: #fff1e0; }
.fav-name { font-size: 13px; color: #4a3a2c; padding: 8px 10px 0; }
.fav-price { display: block; padding: 2px 10px 10px; color: #ea7a38; font-weight: 700; font-size: 14px; }
.load-more {
  display: block; margin: 10px auto; border: 1.5px solid #ffcda5; background: #fff;
  color: #df7e38; border-radius: 18px; padding: 8px 26px; font-size: 13px; cursor: pointer;
}
.load-more:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.load-more:active { transform: scale(0.94); }
.modal-mask { position: fixed; inset: 0; background: rgba(60, 40, 20, 0.45); z-index: 120; display: flex; align-items: center; justify-content: center; padding: 20px; }
.who-modal {
  width: 100%; max-width: 420px; max-height: 78vh;
  background: #fff; border-radius: 16px; padding: 18px 16px;
  display: flex; flex-direction: column;
  box-shadow: 0 16px 48px rgba(60, 40, 20, 0.18);
}
.who-list {
  overflow-y: auto; flex: 1; min-height: 0;
  margin: 0 -4px; padding: 4px;
}
.who-item { display: flex; align-items: flex-start; gap: 14px; padding: 14px 0; border-bottom: 1px dashed #f5e8d8; cursor: pointer; }
.who-avatar { width: 42px; height: 42px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.who-info { flex: 1; min-width: 0; }
.who-name { font-size: 15px; font-weight: 600; color: #6b4f3a; display: flex; flex-wrap: wrap; align-items: center; }
.who-time { font-size: 11px; color: #b5a48e; font-weight: normal; }
.who-info .who-time { margin-left: 6px; }
.who-content {
  font-size: 14px; color: #4a3a2c; line-height: 1.5; word-break: break-word;
  margin-top: 8px; background: #fff7ef; border-radius: 10px; padding: 9px 12px;
}
.who-icon { margin-left: auto; align-self: center; flex-shrink: 0; font-size: 16px; }
.edit-modal { width: 100%; max-width: 380px; background: #fff; border-radius: 20px; padding: 24px 20px; }
.edit-title { text-align: center; font-weight: 600; color: #6b4f3a; margin-bottom: 18px; }
.edit-avatar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.edit-avatar-tip { font-size: 12px; color: #b5a48e; }
.edit-modal input, .edit-modal textarea {
  width: 100%; border: 1.5px solid #ffe0c7; border-radius: 12px; padding: 12px 14px;
  font-size: 14px; outline: none; background: #fffaf4; margin-bottom: 14px; resize: none;
}
.edit-btns { display: flex; gap: 12px; }
.cancel-btn { flex: 1; height: 40px; border-radius: 20px; border: 1.5px solid #e8dcc8; background: #fff; color: #9b8b76; cursor: pointer; }
.save-btn { flex: 1; height: 40px; border: none; border-radius: 20px; cursor: pointer; background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff; }
.save-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.placeholder { display: flex; align-items: center; justify-content: center; background: #fff1e0; font-size: 26px; }
@media (max-width: 480px) {
  .fav-grid { grid-template-columns: 1fr 1fr; }
}
</style>
