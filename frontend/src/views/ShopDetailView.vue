<script setup>
/**
 * 店铺详情页：店铺简介 + 优惠券领取区（普通券/秒杀券）+ 该店宠物商品列表
 * 秒杀券展示倒计时（Redis 预扣库存，MQ 异步发放）
 */
import { computed, onUnmounted, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { shopCouponsApi, shopDetailApi } from '../api/shop'
import { goodsByShopApi } from '../api/goods'
import { receiveCouponApi, seckillCouponApi } from '../api/coupon'
import { useUserStore } from '../stores/user'
import { addToast } from '../utils/toast'
import { countdownText, isValidPhone } from '../utils/validate'
import { fullTime } from '../utils/time'
import EmptyState from '../components/EmptyState.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const shopId = route.params.id
const shop = ref(null)
const coupons = ref([])
const goods = ref([])
const goodsTotal = ref(0)
const loading = ref(true)
const goodsLoading = ref(false)
const goodsPage = ref(1)
const receivedIds = ref(new Set())
const seckilling = ref(false)

const normalCoupons = computed(() => coupons.value.filter((c) => c.category === 0))
const seckillCoupons = computed(() => coupons.value.filter((c) => c.category === 1))

/** 是否本人店铺（展示管理入口） */
const isMyShop = computed(() =>
  shop.value && userStore.userInfo?.id && shop.value.ownerId === userStore.userInfo.id
)

/* ---------------- 数据加载 ---------------- */
onMounted(async () => {
  try {
    shop.value = await shopDetailApi(shopId)
  } finally {
    loading.value = false
  }
  loadCoupons()
  loadGoods(true)
})

async function loadCoupons() {
  if (!userStore.isLogin) {
    coupons.value = []
    return
  }
  coupons.value = await shopCouponsApi(shopId)
}

async function loadGoods(reset = false) {
  goodsLoading.value = true
  try {
    if (reset) goodsPage.value = 1
    const params = { page: goodsPage.value, pageSize: 10 }
    const data = await goodsByShopApi(shopId, params)
    goodsTotal.value = data.total
    if (reset) goods.value = data.list
    else goods.value.push(...data.list)
    if (goods.value.length < data.total && data.list.length) goodsPage.value++
  } finally {
    goodsLoading.value = false
  }
}

/* ---------------- 秒杀倒计时（每秒刷新） ---------------- */
const now = ref(Date.now())
const timer = setInterval(() => (now.value = Date.now()), 1000)
onUnmounted(() => clearInterval(timer))

function couponCountdown(coupon) {
  return countdownText(coupon.expireTime, now.value)
}

/** 秒杀是否已开始 */
function seckillStarted(coupon) {
  return !coupon.startTime || new Date(coupon.startTime).getTime() <= now.value
}

/* ---------------- 领券 ---------------- */
async function onReceive(coupon) {
  if (!userStore.isLogin) {
    addToast('登录后才能领券', 'error')
    router.push('/login?redirect=/shops/' + shopId)
    return
  }
  if (coupon.category === 0) {
    await receiveCouponApi(coupon.id)
    receivedIds.value.add(coupon.id)
    addToast('领取成功，可在"我的-优惠券"查看', 'success')
  } else {
    seckilling.value = true
    try {
      await seckillCouponApi(coupon.id)
      addToast('抢购成功！券稍后自动到账', 'success')
      receivedIds.value.add(coupon.id)
    } finally {
      seckilling.value = false
    }
  }
}

/** 领取按钮文案 */
function receiveText(coupon) {
  if (coupon.category === 1) {
    return seckilling.value ? '抢购中...' : '立即抢券'
  }
  return receivedIds.value.has(coupon.id) ? '已领取' : '领取'
}

/** 领取按钮是否禁用 */
function receiveDisabled(coupon) {
  if (coupon.category === 1) {
    return seckilling.value || countdownText(coupon.expireTime, now.value).ended || !seckillStarted(coupon)
  }
  return receivedIds.value.has(coupon.id) || coupon.remainStock <= 0
}
</script>

<template>
  <div class="shop-detail-page">
    <header class="page-head">
      <span class="back" @click="router.push('/shops')">‹</span>
      <span class="head-title">店铺详情</span>
      <span v-if="isMyShop" class="manage-entry" @click="router.push('/shop/manage')">管理店铺</span>
    </header>

    <LoadingSpinner v-if="loading" />
    <template v-else-if="shop">
      <!-- 店铺信息卡 -->
      <section class="shop-hero">
        <img v-if="shop.cover" class="hero-bg" :src="shop.cover" alt="店铺封面" />
        <div v-else class="hero-bg placeholder">🏪</div>
        <div class="hero-body">
          <img v-if="shop.logo" class="shop-logo" :src="shop.logo" alt="店铺logo" />
          <span v-else class="shop-logo placeholder">🐾</span>
          <div>
            <p class="shop-name">{{ shop.shopName }}</p>
            <p class="shop-desc">{{ shop.shopDesc || '店家很懒，还没写简介' }}</p>
          </div>
        </div>
      </section>

      <!-- 优惠券领取区 -->
      <section v-if="userStore.isLogin" class="coupon-section">
        <div class="section-title">店铺优惠券</div>

        <!-- 普通券 -->
        <div v-for="coupon in normalCoupons" :key="coupon.id" class="coupon-card">
          <div class="coupon-left">
            <span class="money">￥{{ coupon.money }}</span>
            <span class="cond">无门槛</span>
          </div>
          <div class="coupon-mid">
            <p class="coupon-name">{{ coupon.couponName }}</p>
            <p class="coupon-meta">每人限领{{ coupon.perLimit }}张 · 剩余{{ coupon.remainStock }}/{{ coupon.totalStock }} · {{ fullTime(coupon.expireTime) }} 前有效</p>
          </div>
          <button class="receive-btn" :disabled="receiveDisabled(coupon)" @click="onReceive(coupon)">
            {{ receiveDisabled(coupon) && !receivedIds.has(coupon.id) && coupon.remainStock <= 0 ? '已抢完' : receiveText(coupon) }}
          </button>
        </div>

        <!-- 秒杀券 -->
        <div v-for="coupon in seckillCoupons" :key="coupon.id" class="coupon-card seckill">
          <div class="coupon-left">
            <span class="money">￥{{ coupon.money }}</span>
            <span class="cond">秒杀专享</span>
          </div>
          <div class="coupon-mid">
            <p class="coupon-name">{{ coupon.couponName }}</p>
            <p class="coupon-meta">
              剩余{{ coupon.remainStock }}/{{ coupon.totalStock }}
              <span v-if="!seckillStarted(coupon)" class="countdown">未开始</span>
              <span v-else-if="!couponCountdown(coupon).ended" class="countdown">距结束 {{ couponCountdown(coupon).text }}</span>
              <span v-else class="countdown ended">已结束</span>
            </p>
          </div>
          <button class="receive-btn seckill-btn" :disabled="receiveDisabled(coupon)" @click="onReceive(coupon)">
            {{ coupon.remainStock <= 0 ? '已抢完' : receiveText(coupon) }}
          </button>
        </div>

        <EmptyState v-if="!coupons.length" text="店铺暂无可领优惠券" icon="🎫" />
      </section>

      <!-- 未登录提示领券 -->
      <section v-else class="coupon-login-tip" @click="router.push('/login?redirect=/shops/' + shopId)">
        🎫 登录后可领取店铺优惠券与秒杀券 ›
      </section>

      <!-- 商品列表 -->
      <section class="goods-section">
        <div class="section-title">本店宠物 ({{ goodsTotal }})</div>
        <LoadingSpinner v-if="goodsLoading && !goods.length" />
        <EmptyState v-else-if="!goods.length" text="店铺暂无在售宠物" icon="🐾" />
        <div class="goods-grid">
          <div v-for="g in goods" :key="g.id" class="goods-card" @click="router.push('/goods/' + g.id)">
            <img v-if="g.images && g.images.length" :src="g.images[0]" alt="宠物图片" loading="lazy" />
            <div v-else class="goods-img-placeholder">�</div>
            <p class="goods-name">{{ g.petName }}</p>
            <div class="goods-foot">
              <span class="goods-price">￥{{ g.price }}</span>
              <span class="goods-breed">{{ g.breed }}</span>
            </div>
          </div>
        </div>
        <button v-if="goods.length < goodsTotal && !goodsLoading" class="load-more" @click="loadGoods()">加载更多</button>
      </section>
    </template>
  </div>
</template>

<style scoped>
/* 店铺详情：暖橘渐变风格 */
.shop-detail-page { min-height: 100vh; background: #fff6ec; padding-bottom: 30px; }
.page-head {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  background: rgba(255, 246, 236, 0.94);
  backdrop-filter: blur(8px);
  z-index: 10;
}
.back { font-size: 26px; color: #6b4f3a; cursor: pointer; line-height: 1; }
.head-title { margin-left: 12px; font-size: 17px; font-weight: 600; color: #6b4f3a; }
.manage-entry {
  margin-left: auto;
  font-size: 12px; color: #fff; background: linear-gradient(135deg, #f7a76b, #f2a268);
  border-radius: 14px; padding: 5px 12px; cursor: pointer;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
}
.manage-entry:hover { filter: brightness(1.05); }
.manage-entry:active { transform: scale(0.95); }
.shop-hero { margin: 16px 16px 12px; border-radius: 14px; overflow: hidden; background: #fff; box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1); transition: all 0.2s; }
.shop-hero:hover { transform: translateY(-2px); }
.hero-bg { width: 100%; height: 120px; object-fit: cover; display: block; }
.hero-bg.placeholder { display: flex; align-items: center; justify-content: center; font-size: 44px; background: #fff1e0; color: #df7e38; }
.hero-body { display: flex; align-items: center; gap: 14px; padding: 0 14px 16px; margin-top: -26px; }
.shop-logo { width: 54px; height: 54px; border-radius: 14px; object-fit: cover; border: 3px solid #fff; box-shadow: 0 3px 10px rgba(242, 162, 104, 0.15); background: #fff; }
.shop-logo.placeholder { display: flex; align-items: center; justify-content: center; font-size: 26px; color: #df7e38; }
.shop-name { font-size: 18px; font-weight: 700; color: #6b4f3a; }
.shop-desc { font-size: 13px; color: #b5a48e; margin-top: 4px; line-height: 1.5; }
.coupon-section { padding: 0 16px; }
.section-title { font-size: 16px; font-weight: 600; color: #6b4f3a; margin: 18px 0 12px; }
.coupon-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: linear-gradient(130deg, #fff, #fff4e4);
  border: 1.5px dashed #ffd9b0;
  border-radius: 14px;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1);
}
.coupon-card.seckill { background: linear-gradient(135deg, #fff, #ffe2c5); border-color: #f2a268; }
.coupon-left { text-align: center; min-width: 74px; }
.money { display: block; font-size: 24px; font-weight: 700; color: #ea7a38; }
.cond { font-size: 11px; color: #c96a1f; background: #ffe3c9; border-radius: 8px; padding: 1px 6px; }
.coupon-mid { flex: 1; min-width: 0; }
.coupon-name { font-size: 14px; font-weight: 600; color: #6b4f3a; }
.coupon-meta { font-size: 11px; color: #b5a48e; margin-top: 5px; }
.countdown { color: #c96a1f; font-weight: 600; }
.countdown.ended { color: #b5a48e; }
.receive-btn {
  border: none;
  background: linear-gradient(135deg, #f7a76b, #f2a268);
  color: #fff;
  border-radius: 14px;
  padding: 9px 16px;
  font-size: 13px;
  cursor: pointer;
  flex-shrink: 0;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
  transition: all 0.15s;
}
.receive-btn:hover { filter: brightness(1.05); }
.receive-btn:active { transform: scale(0.95); }
.receive-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.receive-btn.seckill-btn { background: linear-gradient(135deg, #f7a76b, #f2a268); }
.coupon-login-tip {
  margin: 14px 16px 0;
  background: #fff;
  border-radius: 14px;
  padding: 14px;
  text-align: center;
  color: #e07e35;
  font-size: 13px;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1);
}
.goods-section { padding: 12px 16px 0; }
.goods-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 14px; }
.goods-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1);
  cursor: pointer;
  transition: all 0.2s;
}
.goods-card:hover { transform: translateY(-2px); }
.goods-card img, .goods-img-placeholder { width: 100%; height: 118px; object-fit: cover; display: flex; align-items: center; justify-content: center; font-size: 38px; background: #fff1e0; color: #df7e38; }
.goods-name {
  font-size: 14px;
  color: #4a3a2c;
  padding: 10px 12px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.goods-foot { display: flex; justify-content: space-between; align-items: center; padding: 6px 12px 12px; }
.goods-price { color: #ea7a38; font-weight: 700; font-size: 15px; }
.goods-breed { font-size: 11px; color: #b5a48e; }
.load-more {
  display: block;
  margin: 12px auto;
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
@media (max-width: 480px) {
  .goods-grid { grid-template-columns: 1fr 1fr; }
}
</style>
