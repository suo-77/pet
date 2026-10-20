<script setup>
/**
 * 宠物商品详情页：宠物信息 + 收藏 + 店铺优惠券领取区 + 评价区（分页/发表）+ 加购/立即购买
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { evaluateApi, evaluatesApi, favApi, goodsDetailApi, unfavApi } from '../api/goods'
import { shopCouponsApi } from '../api/shop'
import { receiveCouponApi, seckillCouponApi, usableCouponsApi } from '../api/coupon'
import { cartAddApi } from '../api/cart'
import { buyGoodsApi } from '../api/order'
import { useUserStore } from '../stores/user'
import { addToast } from '../utils/toast'
import { friendTime, fullTime } from '../utils/time'
import EmptyState from '../components/EmptyState.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ImgUpload from '../components/ImgUpload.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const goodsId = route.params.id
const goods = ref(null)
const mainImage = ref('')
const loading = ref(true)
const coupons = ref([])
const favLoading = ref(false)
const buyNum = ref(1)

/* 评价区状态 */
const evaluates = ref([])
const evaluateTotal = ref(0)
const evaluatePage = ref(1)
const evaluateLoading = ref(false)
const showEvaluateForm = ref(false)
const evaluateForm = ref({ content: '', images: [] })
const evaluateSubmitting = ref(false)

/** 当前图集 */
const imageList = computed(() => goods.value?.images || [])

onMounted(async () => {
  try {
    goods.value = await goodsDetailApi(goodsId)
    mainImage.value = imageList.value[0] || ''
    // 优惠券领取区（登录才有数据）
    if (userStore.isLogin) {
      coupons.value = await shopCouponsApi(goods.value.shopId)
      loadUsableCoupons()
    }
    loadEvaluates(true)
  } finally {
    loading.value = false
  }
})

/* ---------------- 收藏 ---------------- */
async function toggleFav() {
  if (!userStore.isLogin) {
    addToast('登录后才能收藏', 'error')
    router.push('/login?redirect=/goods/' + goodsId)
    return
  }
  favLoading.value = true
  try {
    if (goods.value.fav) {
      await unfavApi(goodsId)
      goods.value.fav = false
      addToast('已取消收藏', 'success')
    } else {
      await favApi(goodsId)
      goods.value.fav = true
      addToast('已加入我的收藏', 'success')
    }
  } finally {
    favLoading.value = false
  }
}

/* ---------------- 评价 ---------------- */
async function loadEvaluates(reset = false) {
  evaluateLoading.value = true
  try {
    if (reset) evaluatePage.value = 1
    const params = { page: evaluatePage.value, pageSize: 5 }
    const data = await evaluatesApi(goodsId, params)
    evaluateTotal.value = data.total
    if (reset) evaluates.value = data.list
    else evaluates.value.push(...data.list)
    if (evaluates.value.length < data.total && data.list.length) evaluatePage.value++
  } finally {
    evaluateLoading.value = false
  }
}

/** 发表评价（仅登录用户） */
async function submitEvaluate() {
  const content = evaluateForm.value.content.trim()
  if (!content) {
    addToast('写两句评价内容吧', 'error')
    return
  }
  evaluateSubmitting.value = true
  try {
    await evaluateApi(goodsId, { content, images: evaluateForm.value.images })
    addToast('评价成功', 'success')
    evaluateForm.value = { content: '', images: [] }
    showEvaluateForm.value = false
    loadEvaluates(true)
  } finally {
    evaluateSubmitting.value = false
  }
}

/* ---------------- 优惠券 ---------------- */
const receivedIds = ref(new Set())

async function onReceive(coupon) {
  if (coupon.category === 0) {
    await receiveCouponApi(coupon.id)
    receivedIds.value.add(coupon.id)
    addToast('领取成功', 'success')
  } else {
    await seckillCouponApi(coupon.id)
    receivedIds.value.add(coupon.id)
    addToast('抢购成功！券稍后自动到账', 'success')
  }
}

/* ---------------- 购物车 / 立即购买 ---------------- */
function changeNum(delta) {
  const next = buyNum.value + delta
  if (next < 1) return
  if (goods.value && next > goods.value.stock) {
    addToast('已达库存上限', 'error')
    return
  }
  buyNum.value = next
}

/** 我的可用优惠券（下单选择使用） */
const usableCoupons = ref([])
const selectedCouponId = ref('')

async function loadUsableCoupons() {
  try {
    const all = await usableCouponsApi()
    // 只展示该商品所属店铺的优惠券（券仅限对应店铺商品使用）
    usableCoupons.value = all.filter((c) => c.shopId === goods.value.shopId)
  } catch (e) {
    usableCoupons.value = []
  }
}

/** 预估优惠：券面额不超过订单金额 */
const buyDiscount = computed(() => {
  if (!selectedCouponId.value || !goods.value) return 0
  const c = usableCoupons.value.find((x) => x.id === Number(selectedCouponId.value))
  if (!c) return 0
  return Math.min(Number(c.money), Number(goods.value.price) * buyNum.value)
})

async function addCart() {
  await cartAddApi({ goodsId: Number(goodsId), num: buyNum.value })
  addToast('已加入购物车', 'success')
}

async function buyNow() {
  await buyGoodsApi({
    goodsId: Number(goodsId),
    num: buyNum.value,
    userCouponId: selectedCouponId.value ? Number(selectedCouponId.value) : null
  })
  addToast('下单成功，可在"我的-我的订单"查看', 'success')
}
</script>

<template>
  <div class="goods-page">
    <header class="page-head">
      <span class="back" @click="router.back()">‹</span>
      <span class="head-title">宠物详情</span>
      <span class="fav-head" :class="{ active: goods && goods.fav }" @click="toggleFav">{{ goods && goods.fav ? '❤️' : '🤍' }}</span>
    </header>

    <LoadingSpinner v-if="loading" />
    <template v-else-if="goods">
      <!-- 图片区 -->
      <section class="gallery">
        <img class="main-img" :src="mainImage" alt="宠物图片" />
        <div v-if="imageList.length > 1" class="thumbs">
          <img
            v-for="(img, i) in imageList"
            :key="img"
            :src="img"
            alt="缩略图"
            :class="{ active: img === mainImage }"
            @click="mainImage = img"
          />
        </div>
      </section>

      <!-- 宠物信息 -->
      <section class="info-card">
        <div class="price-row">
          <span class="price">￥{{ goods.price }}</span>
          <span class="stock">库存 {{ goods.stock }}</span>
        </div>
        <p class="pet-name">{{ goods.petName }}</p>
        <div class="tags">
          <span class="tag">品种：{{ goods.breed }}</span>
          <span class="tag">年龄：{{ goods.age }}</span>
          <span class="tag">来源：{{ goods.shopName }}</span>
        </div>
        <p class="desc">{{ goods.description }}</p>
      </section>

      <!-- 数量与操作 -->
      <section class="buy-card">
        <div class="num-row">
          <span>购买数量</span>
          <div class="stepper">
            <button :disabled="buyNum <= 1" @click="changeNum(-1)">−</button>
            <span>{{ buyNum }}</span>
            <button @click="changeNum(1)">＋</button>
          </div>
        </div>
        <!-- 下单用券（登录且有可用券时展示） -->
        <div v-if="userStore.isLogin && usableCoupons.length" class="use-coupon-row">
          <span>使用优惠券</span>
          <select v-model="selectedCouponId" class="coupon-select">
            <option value="">不使用</option>
            <option v-for="c in usableCoupons" :key="c.id" :value="String(c.id)">
              ￥{{ c.money }} {{ c.couponName }}
            </option>
          </select>
        </div>
        <p v-if="buyDiscount > 0" class="discount-tip">已选优惠：抵扣 ￥{{ buyDiscount }}，应付 ￥{{ (Number(goods.price) * buyNum - buyDiscount).toFixed(2) }}</p>
        <div class="buy-btns">
          <button class="cart-btn" @click="addCart">加入购物车</button>
          <button class="buy-btn" @click="buyNow">立即购买</button>
        </div>
      </section>

      <!-- 优惠券领取区 -->
      <section v-if="userStore.isLogin" class="coupon-section">
        <div class="section-title">优惠券</div>
        <div v-for="coupon in coupons" :key="coupon.id" class="coupon-row">
          <span class="coupon-money">￥{{ coupon.money }}</span>
          <span class="coupon-name">{{ coupon.couponName }}</span>
          <span class="coupon-left">剩{{ coupon.remainStock }}</span>
          <button class="coupon-btn" :disabled="receivedIds.has(coupon.id) || coupon.remainStock <= 0" @click="onReceive(coupon)">
            {{ receivedIds.has(coupon.id) ? '已领' : (coupon.remainStock <= 0 ? '已抢完' : (coupon.category === 1 ? '抢券' : '领取')) }}
          </button>
        </div>
        <EmptyState v-if="!coupons.length" text="暂无可领优惠券" icon="🎫" />
      </section>

      <!-- 评价区 -->
      <section class="evaluate-section">
        <div class="section-title">
          用户评价 ({{ evaluateTotal }})
          <button v-if="userStore.isLogin" class="write-btn" @click="showEvaluateForm = !showEvaluateForm">
            {{ showEvaluateForm ? '收起' : '写评价' }}
          </button>
          <span v-else class="login-tip" @click="router.push('/login?redirect=/goods/' + goodsId)">登录后写评价 ›</span>
        </div>

        <!-- 评价表单 -->
        <div v-if="showEvaluateForm" class="evaluate-form">
          <textarea v-model="evaluateForm.content" rows="3" maxlength="300" placeholder="说说这只毛孩子的状况吧（必填）"></textarea>
          <ImgUpload v-model="evaluateForm.images" :limit="3" />
          <button class="btn-primary submit-evaluate" :disabled="evaluateSubmitting" @click="submitEvaluate">
            {{ evaluateSubmitting ? '发布中...' : '发布评价' }}
          </button>
        </div>

        <LoadingSpinner v-if="evaluateLoading && !evaluates.length" />
        <EmptyState v-else-if="!evaluates.length" text="暂无评价，快来抢首评" icon="💬" />
        <div v-for="ev in evaluates" :key="ev.id" class="evaluate-item">
          <div class="evaluate-head">
            <img v-if="ev.avatar" class="ev-avatar" :src="ev.avatar" alt="头像" @click="router.push('/user/' + ev.userId)" />
            <span v-else class="ev-avatar placeholder" @click="router.push('/user/' + ev.userId)">🐱</span>
            <span class="ev-nickname">{{ ev.nickname }}</span>
            <span class="ev-time">{{ friendTime(ev.createTime) }}</span>
          </div>
          <p class="ev-content">{{ ev.content }}</p>
          <div v-if="ev.images && ev.images.length" class="ev-imgs">
            <img v-for="img in ev.images" :key="img" :src="img" alt="评价图片" loading="lazy" />
          </div>
        </div>
        <button v-if="evaluates.length < evaluateTotal && !evaluateLoading" class="load-more" @click="loadEvaluates()">
          更多评价
        </button>
      </section>
      <p class="meta-time">上架时间：{{ fullTime(goods.createTime) }}</p>
    </template>
  </div>
</template>

<style scoped>
/* 商品详情：暖橘渐变风格 */
.goods-page { min-height: 100vh; background: #fff6ec; padding-bottom: 20px; }
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
.head-title { flex: 1; margin-left: 12px; font-size: 17px; font-weight: 600; color: #6b4f3a; }
.fav-head { font-size: 22px; cursor: pointer; }
.fav-head.active { filter: drop-shadow(0 2px 6px rgba(255, 77, 77, 0.5)); }
.gallery { padding: 0 16px; }
.main-img { width: 100%; height: 260px; object-fit: cover; border-radius: 14px; box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1); }
.thumbs { display: flex; gap: 10px; margin-top: 12px; overflow-x: auto; padding-bottom: 2px; }
.thumbs img { width: 64px; height: 64px; border-radius: 12px; object-fit: cover; cursor: pointer; opacity: 0.65; border: 2px solid transparent; }
.thumbs img.active { opacity: 1; border-color: #e07e35; }
.info-card, .buy-card, .coupon-section, .evaluate-section {
  margin: 16px 16px 0;
  background: #fff;
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1);
  transition: transform 0.2s;
}
.info-card:hover, .buy-card:hover, .coupon-section:hover, .evaluate-section:hover { transform: translateY(-2px); }
.price-row { display: flex; justify-content: space-between; align-items: baseline; }
.price { font-size: 26px; font-weight: 700; color: #ea7a38; }
.stock { font-size: 12px; color: #b5a48e; }
.pet-name { font-size: 18px; font-weight: 600; color: #6b4f3a; margin-top: 8px; }
.tags { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 12px; }
.tag { font-size: 12px; color: #c96a1f; background: #fff1e0; border-radius: 10px; padding: 4px 10px; }
.desc { font-size: 14px; color: #4a3a2c; line-height: 1.7; margin-top: 14px; white-space: pre-wrap; }
.num-row { display: flex; justify-content: space-between; align-items: center; font-size: 14px; color: #6b4f3a; }
.use-coupon-row {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 14px; color: #6b4f3a; margin-top: 16px;
}
.coupon-select {
  border: 1.5px solid #ffcda5; border-radius: 10px; padding: 8px 12px;
  font-size: 13px; color: #6b4f3a; background: #fffaf4; outline: none; max-width: 70%;
}
.discount-tip { font-size: 12px; color: #52a86a; margin-top: 12px; }
.stepper { display: flex; align-items: center; gap: 16px; }
.stepper button {
  width: 30px; height: 30px; border-radius: 50%; border: 1.5px solid #ffcda5;
  background: #fffaf4; color: #e07e35; font-size: 16px; cursor: pointer;
}
.stepper button:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.stepper button:active { transform: scale(0.94); }
.stepper button:disabled { opacity: 0.4; cursor: not-allowed; }
.stepper span { min-width: 22px; text-align: center; font-weight: 600; color: #4a3a2c; }
.buy-btns { display: flex; gap: 14px; margin-top: 18px; }
.cart-btn, .buy-btn { flex: 1; height: 44px; border-radius: 14px; border: none; font-size: 15px; cursor: pointer; }
.cart-btn { background: #ffe3c9; color: #c96a1f; }
.cart-btn:active { filter: brightness(0.95); }
.buy-btn {
  background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
  transition: transform 0.15s, filter 0.15s, box-shadow 0.15s;
}
.buy-btn:hover { filter: brightness(1.05); transform: translateY(-1px); box-shadow: 0 7px 18px rgba(242, 162, 104, 0.4); }
.buy-btn:active { transform: scale(0.96); }
.section-title { display: flex; align-items: center; justify-content: space-between; font-size: 16px; font-weight: 600; color: #6b4f3a; margin-bottom: 14px; }
.write-btn {
  border: 1.5px solid #f2d9c0; background: #fff; color: #b9824f;
  border-radius: 14px; padding: 5px 14px; font-size: 12px; cursor: pointer;
}
.write-btn:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.write-btn:active { transform: scale(0.94); }
.login-tip { font-size: 12px; color: #e07e35; font-weight: normal; cursor: pointer; }
.coupon-row { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px dashed #f5e8d8; font-size: 13px; }
.coupon-money { color: #ea7a38; font-weight: 700; min-width: 56px; }
.coupon-name { flex: 1; color: #6b4f3a; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.coupon-left { color: #b5a48e; font-size: 12px; }
.coupon-btn {
  border: none; background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff;
  border-radius: 14px; padding: 7px 16px; font-size: 12px; cursor: pointer;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
  transition: transform 0.15s, filter 0.15s, box-shadow 0.15s;
}
.coupon-btn:hover { filter: brightness(1.05); transform: translateY(-1px); box-shadow: 0 7px 18px rgba(242, 162, 104, 0.4); }
.coupon-btn:active { transform: scale(0.96); }
.coupon-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.evaluate-form { background: #fffaf4; border-radius: 14px; padding: 14px; margin-bottom: 14px; }
.evaluate-form textarea {
  width: 100%; border: 1.5px solid #ffe0c7; border-radius: 10px; padding: 12px;
  font-size: 14px; resize: none; outline: none; background: #fff; margin-bottom: 12px;
}
.submit-evaluate {
  border: none; border-radius: 14px; padding: 9px 22px; font-size: 13px; cursor: pointer;
  background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
  transition: transform 0.15s, filter 0.15s, box-shadow 0.15s;
}
.submit-evaluate:hover { filter: brightness(1.05); transform: translateY(-1px); box-shadow: 0 7px 18px rgba(242, 162, 104, 0.4); }
.submit-evaluate:active { transform: scale(0.96); }
.submit-evaluate:disabled { opacity: 0.5; cursor: not-allowed; }
.evaluate-item { padding: 14px 0; border-bottom: 1px dashed #f5e8d8; }
.evaluate-head { display: flex; align-items: center; gap: 10px; }
.ev-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; cursor: pointer; flex-shrink: 0; }
.ev-nickname { font-size: 13px; font-weight: 600; color: #6b4f3a; }
.ev-time { margin-left: auto; font-size: 12px; color: #b5a48e; }
.ev-content { font-size: 14px; color: #4a3a2c; margin-top: 10px; word-break: break-word; line-height: 1.6; }
.ev-imgs { display: flex; gap: 10px; margin-top: 10px; }
.ev-imgs img { width: 72px; height: 72px; border-radius: 12px; object-fit: cover; }
.load-more {
  display: block; margin: 14px auto 0; border: 1.5px solid #f2d9c0; background: #fff;
  color: #b9824f; border-radius: 14px; padding: 8px 26px; font-size: 13px; cursor: pointer;
}
.load-more:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.load-more:active { transform: scale(0.94); }
.placeholder { display: flex; align-items: center; justify-content: center; background: #fff1e0; font-size: 16px; }
.meta-time { text-align: center; font-size: 11px; color: #cdbba4; padding: 20px 0 8px; }
</style>
