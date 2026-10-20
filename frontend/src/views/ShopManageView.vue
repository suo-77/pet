<script setup>
/**
 * 店铺管理页：未开店→开店引导；已开店→店铺信息编辑 / 商品管理（增改上下架删除）/ 优惠券管理（创建+列表）
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { myShopApi, openShopApi, updateShopApi, shopCouponsApi } from '../api/shop'
import {
  myGoodsPageApi, createGoodsApi, updateGoodsApi, updateGoodsStatusApi, deleteGoodsApi
} from '../api/goods'
import { createCouponApi, deleteCouponApi } from '../api/coupon'
import { addToast } from '../utils/toast'
import EmptyState from '../components/EmptyState.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ImgUpload from '../components/ImgUpload.vue'

const loading = ref(true)
const shop = ref(null) // null=未开店

onMounted(async () => {
  try {
    shop.value = await myShopApi()
    if (shop.value) {
      fillInfoForm()
      loadGoods(true)
      loadCoupons()
    }
  } finally {
    loading.value = false
  }
})

/* ---------------- 开店引导 ---------------- */
const openForm = reactive({ shopName: '', shopDesc: '', cover: [] })
const opening = ref(false)

async function submitOpen() {
  if (!openForm.shopName.trim()) {
    addToast('给小店起个名字吧', 'error')
    return
  }
  opening.value = true
  try {
    shop.value = await openShopApi({
      shopName: openForm.shopName.trim(),
      shopDesc: openForm.shopDesc.trim(),
      cover: openForm.cover[0] || null
    })
    addToast('小店开通成功，快上架第一件宝贝吧', 'success')
    loadGoods(true)
    loadCoupons()
  } finally {
    opening.value = false
  }
}

/* ---------------- 管理板块 Tab ---------------- */
const activeTab = ref('info')
const tabs = [
  { key: 'info', label: '店铺信息' },
  { key: 'goods', label: '商品管理' },
  { key: 'coupons', label: '优惠券' }
]

/* ---------------- 店铺信息编辑 ---------------- */
const infoForm = reactive({ shopName: '', shopDesc: '', cover: [] })
const savingInfo = ref(false)

function fillInfoForm() {
  infoForm.shopName = shop.value.shopName
  infoForm.shopDesc = shop.value.shopDesc || ''
  infoForm.cover = shop.value.cover ? [shop.value.cover] : []
}

async function saveInfo() {
  if (!infoForm.shopName.trim()) {
    addToast('店铺名称不能为空', 'error')
    return
  }
  savingInfo.value = true
  try {
    shop.value = await updateShopApi({
      shopName: infoForm.shopName.trim(),
      shopDesc: infoForm.shopDesc.trim(),
      cover: infoForm.cover[0] || null
    })
    fillInfoForm()
    addToast('店铺信息已保存', 'success')
  } finally {
    savingInfo.value = false
  }
}

/* ---------------- 商品管理 ---------------- */
const goods = ref([])
const goodsTotal = ref(0)
const goodsPage = ref(1)
const goodsLoading = ref(false)
const goodsFinished = ref(false)

async function loadGoods(reset = false) {
  if (goodsLoading.value || (goodsFinished.value && !reset)) return
  if (reset) {
    goodsPage.value = 1
    goodsFinished.value = false
  }
  goodsLoading.value = true
  try {
    const data = await myGoodsPageApi({ page: goodsPage.value, pageSize: 6 })
    goodsTotal.value = data.total
    if (reset) goods.value = data.list
    else goods.value.push(...data.list)
    if (goods.value.length >= data.total || data.list.length === 0) goodsFinished.value = true
    else goodsPage.value++
  } finally {
    goodsLoading.value = false
  }
}

/** 商品编辑弹层：editId=null 表示新增 */
const goodsModalVisible = ref(false)
const editGoodsId = ref(null)
const goodsForm = reactive({
  petName: '', breed: '', age: '', price: '', stock: 1, description: '', images: []
})
const savingGoods = ref(false)

function openGoodsModal(item = null) {
  editGoodsId.value = item ? item.id : null
  goodsForm.petName = item?.petName || ''
  goodsForm.breed = item?.breed || ''
  goodsForm.age = item?.age || ''
  goodsForm.price = item?.price ?? ''
  goodsForm.stock = item?.stock ?? 1
  goodsForm.description = item?.description || ''
  goodsForm.images = item?.images ? [...item.images] : []
  goodsModalVisible.value = true
}

async function saveGoods() {
  if (!goodsForm.petName.trim()) {
    addToast('宠物名称不能为空', 'error')
    return
  }
  const price = Number(goodsForm.price)
  if (!(price > 0)) {
    addToast('请输入正确的价格', 'error')
    return
  }
  const stock = Number(goodsForm.stock)
  if (!(stock >= 0)) {
    addToast('库存不能为负', 'error')
    return
  }
  savingGoods.value = true
  const payload = {
    petName: goodsForm.petName.trim(),
    breed: goodsForm.breed.trim(),
    age: goodsForm.age.trim(),
    price,
    stock,
    description: goodsForm.description.trim(),
    images: goodsForm.images
  }
  try {
    if (editGoodsId.value) {
      await updateGoodsApi(editGoodsId.value, payload)
      addToast('商品已更新', 'success')
    } else {
      await createGoodsApi(payload)
      addToast('商品已上架', 'success')
    }
    goodsModalVisible.value = false
    loadGoods(true)
  } finally {
    savingGoods.value = false
  }
}

async function toggleShelf(item) {
  const next = item.status === 0 ? 1 : 0
  await updateGoodsStatusApi(item.id, next)
  item.status = next
  addToast(next === 0 ? '已上架' : '已下架', 'success')
}

async function removeGoods(item) {
  if (!window.confirm(`确定删除「${item.petName}」吗？删除后不可恢复。`)) return
  await deleteGoodsApi(item.id)
  goods.value = goods.value.filter((g) => g.id !== item.id)
  goodsTotal.value = Math.max(0, goodsTotal.value - 1)
  addToast('已删除', 'success')
}

/* ---------------- 优惠券管理 ---------------- */
const shopCoupons = ref([])
const couponsLoading = ref(false)

async function loadCoupons() {
  if (!shop.value) return
  couponsLoading.value = true
  try {
    shopCoupons.value = await shopCouponsApi(shop.value.id)
  } finally {
    couponsLoading.value = false
  }
}

const couponModalVisible = ref(false)
const savingCoupon = ref(false)
const couponForm = reactive({
  couponName: '', category: 0, money: '', totalStock: 100, perLimit: 1,
  startTime: '', expireTime: ''
})

function openCouponModal() {
  couponForm.couponName = ''
  couponForm.category = 0
  couponForm.money = ''
  couponForm.totalStock = 100
  couponForm.perLimit = 1
  couponForm.startTime = ''
  couponForm.expireTime = ''
  couponModalVisible.value = true
}

async function saveCoupon() {
  if (!couponForm.couponName.trim()) {
    addToast('券名称不能为空', 'error')
    return
  }
  const money = Number(couponForm.money)
  if (!(money > 0)) {
    addToast('请输入正确的券面额', 'error')
    return
  }
  if (!couponForm.expireTime) {
    addToast('请选择过期时间', 'error')
    return
  }
  if (couponForm.category === 1 && !couponForm.startTime) {
    addToast('秒杀券需选择开始时间', 'error')
    return
  }
  savingCoupon.value = true
  try {
    await createCouponApi({
      shopId: shop.value.id,
      couponName: couponForm.couponName.trim(),
      category: couponForm.category,
      money,
      totalStock: Number(couponForm.totalStock),
      perLimit: Number(couponForm.perLimit),
      startTime: couponForm.startTime ? couponForm.startTime : null,
      expireTime: couponForm.expireTime
    })
    addToast('优惠券创建成功', 'success')
    couponModalVisible.value = false
    loadCoupons()
  } finally {
    savingCoupon.value = false
  }
}

const shopLink = computed(() => shop.value ? '/shops/' + shop.value.id : '')

/** 删除优惠券（逻辑删除，已领用户的券也会同步隐藏） */
async function removeCoupon(c) {
  if (!window.confirm(`确定删除优惠券「${c.couponName}」吗？删除后用户端不可见。`)) return
  await deleteCouponApi(c.id)
  shopCoupons.value = shopCoupons.value.filter((x) => x.id !== c.id)
  addToast('优惠券已删除', 'success')
}
</script>

<template>
  <div class="manage-page">
    <header class="page-head">
      <span class="back" @click="$router.back()">‹</span>
      <span class="head-title">我的店铺</span>
      <span v-if="shop" class="view-shop" @click="$router.push(shopLink)">进店看看 ›</span>
    </header>

    <LoadingSpinner v-if="loading" />

    <!-- 未开店：开店引导 -->
    <section v-else-if="!shop" class="open-card">
      <p class="open-title">开店啦</p>
      <p class="open-sub">一人一店，把心仪的毛孩子分享给更多铲屎官</p>
      <div class="form-item">
        <label>店铺封面（可选）</label>
        <ImgUpload v-model="openForm.cover" :limit="1" />
      </div>
      <div class="form-item">
        <label>店铺名称 *</label>
        <input v-model.trim="openForm.shopName" maxlength="30" placeholder="例如：橘座的小窝" />
      </div>
      <div class="form-item">
        <label>店铺简介</label>
        <textarea v-model="openForm.shopDesc" rows="3" maxlength="200" placeholder="介绍一下你的小店"></textarea>
      </div>
      <button class="btn-primary open-btn" :disabled="opening" @click="submitOpen">
        {{ opening ? '开通中...' : '立即开店' }}
      </button>
    </section>

    <!-- 已开店：管理面板 -->
    <template v-else>
      <div class="manage-tabs">
        <span
          v-for="t in tabs"
          :key="t.key"
          :class="{ active: activeTab === t.key }"
          @click="activeTab = t.key"
        >{{ t.label }}</span>
      </div>

      <!-- 店铺信息 -->
      <section v-if="activeTab === 'info'" class="panel">
        <div class="form-item">
          <label>店铺封面</label>
          <ImgUpload v-model="infoForm.cover" :limit="1" />
        </div>
        <div class="form-item">
          <label>店铺名称</label>
          <input v-model.trim="infoForm.shopName" maxlength="30" />
        </div>
        <div class="form-item">
          <label>店铺简介</label>
          <textarea v-model="infoForm.shopDesc" rows="3" maxlength="200"></textarea>
        </div>
        <button class="btn-primary save-info-btn" :disabled="savingInfo" @click="saveInfo">
          {{ savingInfo ? '保存中...' : '保存修改' }}
        </button>
      </section>

      <!-- 商品管理 -->
      <section v-else-if="activeTab === 'goods'" class="panel">
        <div class="panel-head">
          <span>共 {{ goodsTotal }} 件商品</span>
          <button class="add-btn" @click="openGoodsModal()">＋ 新增商品</button>
        </div>
        <EmptyState v-if="!goods.length && !goodsLoading" text="还没有商品，点右上角新增吧" icon="📦" />
        <div v-for="g in goods" :key="g.id" class="goods-row">
          <img v-if="g.images && g.images.length" class="g-img" :src="g.images[0]" alt="商品图" />
          <div v-else class="g-img placeholder">🐾</div>
          <div class="g-info">
            <p class="g-name">{{ g.petName }} <span class="g-status" :class="{ off: g.status === 1 }">{{ g.status === 0 ? '上架中' : '已下架' }}</span></p>
            <p class="g-meta">￥{{ g.price }} · 库存 {{ g.stock }}</p>
          </div>
          <div class="g-ops">
            <button @click="openGoodsModal(g)">编辑</button>
            <button @click="toggleShelf(g)">{{ g.status === 0 ? '下架' : '上架' }}</button>
            <button class="danger" @click="removeGoods(g)">删除</button>
          </div>
        </div>
        <LoadingSpinner v-if="goodsLoading && goods.length" text="加载更多..." />
        <button v-if="!goodsFinished && goods.length && !goodsLoading" class="load-more" @click="loadGoods()">加载更多</button>
      </section>

      <!-- 优惠券管理 -->
      <section v-else class="panel">
        <div class="panel-head">
          <span>店铺有效优惠券 {{ shopCoupons.length }} 张</span>
          <button class="add-btn" @click="openCouponModal">＋ 发优惠券</button>
        </div>
        <LoadingSpinner v-if="couponsLoading && !shopCoupons.length" />
        <EmptyState v-else-if="!shopCoupons.length" text="还没有优惠券，发一张吸引顾客吧" icon="🎫" />
        <div v-for="c in shopCoupons" :key="c.id" class="coupon-row">
          <span class="c-money">￥{{ c.money }}</span>
          <div class="c-info">
            <p class="c-name">{{ c.couponName }}</p>
            <p class="c-meta">
              {{ c.category === 1 ? '秒杀券' : '普通券' }} · 剩 {{ c.remainStock }}/{{ c.totalStock }}
            </p>
          </div>
          <span class="c-expire">{{ (c.expireTime || '').slice(0, 10) }} 到期</span>
          <button class="c-del" @click="removeCoupon(c)">删除</button>
        </div>
      </section>
    </template>

    <!-- 商品编辑弹层 -->
    <div v-if="goodsModalVisible" class="modal-mask" @click.self="goodsModalVisible = false">
      <div class="modal">
        <p class="modal-title">{{ editGoodsId ? '编辑商品' : '新增商品' }}</p>
        <div class="form-item">
          <label>宠物图片（最多6张）</label>
          <ImgUpload v-model="goodsForm.images" :limit="6" />
        </div>
        <div class="form-item">
          <label>宠物名称 *</label>
          <input v-model.trim="goodsForm.petName" maxlength="30" placeholder="例如：小橘" />
        </div>
        <div class="form-grid">
          <div class="form-item">
            <label>品种</label>
            <input v-model.trim="goodsForm.breed" maxlength="30" placeholder="中华田园猫" />
          </div>
          <div class="form-item">
            <label>年龄</label>
            <input v-model.trim="goodsForm.age" maxlength="20" placeholder="3个月" />
          </div>
        </div>
        <div class="form-grid">
          <div class="form-item">
            <label>价格（￥）*</label>
            <input v-model="goodsForm.price" type="number" min="0.01" step="0.01" />
          </div>
          <div class="form-item">
            <label>库存 *</label>
            <input v-model="goodsForm.stock" type="number" min="0" max="9999" />
          </div>
        </div>
        <div class="form-item">
          <label>描述</label>
          <textarea v-model="goodsForm.description" rows="3" maxlength="500" placeholder="性格、健康状况、疫苗接种情况等"></textarea>
        </div>
        <div class="modal-btns">
          <button class="cancel-btn" @click="goodsModalVisible = false">取消</button>
          <button class="btn-primary" :disabled="savingGoods" @click="saveGoods">
            {{ savingGoods ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 优惠券创建弹层 -->
    <div v-if="couponModalVisible" class="modal-mask" @click.self="couponModalVisible = false">
      <div class="modal">
        <p class="modal-title">发放优惠券</p>
        <div class="form-item">
          <label>券名称 *</label>
          <input v-model.trim="couponForm.couponName" maxlength="50" placeholder="例如：新客立减券" />
        </div>
        <div class="form-grid">
          <div class="form-item">
            <label>类型</label>
            <select v-model.number="couponForm.category" class="sel">
              <option :value="0">普通券</option>
              <option :value="1">秒杀券</option>
            </select>
          </div>
          <div class="form-item">
            <label>面额（￥）*</label>
            <input v-model="couponForm.money" type="number" min="0.01" step="0.01" />
          </div>
        </div>
        <div class="form-grid">
          <div class="form-item">
            <label>发行量 *</label>
            <input v-model="couponForm.totalStock" type="number" min="1" max="10000" />
          </div>
          <div class="form-item">
            <label>每人限领</label>
            <input v-model="couponForm.perLimit" type="number" min="1" max="10" />
          </div>
        </div>
        <div v-if="couponForm.category === 1" class="form-item">
          <label>秒杀开始时间 *</label>
          <input v-model="couponForm.startTime" type="datetime-local" />
        </div>
        <div class="form-item">
          <label>过期时间 *</label>
          <input v-model="couponForm.expireTime" type="datetime-local" />
        </div>
        <div class="modal-btns">
          <button class="cancel-btn" @click="couponModalVisible = false">取消</button>
          <button class="btn-primary" :disabled="savingCoupon" @click="saveCoupon">
            {{ savingCoupon ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 店铺管理：暖橘渐变风格 */
.manage-page { min-height: 100vh; background: #fff6ec; padding-bottom: 40px; }
.page-head {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px 12px;
}
.back { font-size: 26px; color: #6b4f3a; cursor: pointer; line-height: 1; }
.head-title { flex: 1; font-size: 18px; font-weight: 700; color: #6b4f3a; }
.view-shop {
  font-size: 13px; color: #df7e38; cursor: pointer;
  border: 1.5px solid #ffcda5; background: #fff; border-radius: 14px;
  padding: 4px 12px;
}
.open-card, .panel {
  margin: 16px 16px 0; background: linear-gradient(135deg, #fff, #fff3e3); border-radius: 16px; padding: 20px 18px;
  box-shadow: 0 6px 20px rgba(255, 140, 66, 0.12);
}
.open-title { font-size: 20px; font-weight: 700; color: #6b4f3a; }
.open-sub { font-size: 13px; color: #b5a48e; margin: 6px 0 18px; }
.form-item { margin-bottom: 16px; }
.form-item label { display: block; font-size: 13px; color: #9b8b76; margin-bottom: 8px; }
.form-item input, .form-item textarea, .sel {
  width: 100%; border: 1.5px solid #ffe0c7; border-radius: 12px; padding: 12px 14px;
  font-size: 14px; outline: none; background: #fffaf4; resize: none; color: #4a3a2c;
}
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
@media (max-width: 480px) {
  .form-grid { grid-template-columns: 1fr; gap: 12px; }
}
.btn-primary {
  border: none; border-radius: 14px; padding: 11px 26px; cursor: pointer;
  background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff; font-size: 14px;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
}
.btn-primary:hover:not(:disabled) { filter: brightness(1.05); }
.btn-primary:active:not(:disabled) { transform: scale(0.96); }
.btn-primary:disabled { opacity: 0.55; cursor: not-allowed; }
.open-btn, .save-info-btn { width: 100%; margin-top: 4px; }
.manage-tabs { display: flex; gap: 10px; padding: 16px 16px 0; }
.manage-tabs span {
  flex: 1; text-align: center; padding: 11px 0; background: #fff;
  border-radius: 14px; font-size: 13px; color: #9b8b76; cursor: pointer;
  box-shadow: 0 2px 8px rgba(255, 140, 66, 0.08);
}
.manage-tabs span.active { background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff; }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; font-size: 13px; color: #9b8b76; }
.add-btn {
  border: 1.5px solid #ffcda5; background: #fff; color: #df7e38;
  border-radius: 14px; padding: 7px 16px; font-size: 12px; cursor: pointer;
}
.add-btn:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.add-btn:active { transform: scale(0.94); }
.goods-row {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 0; border-bottom: 1px dashed #f5e8d8;
}
.g-img { width: 64px; height: 64px; border-radius: 12px; object-fit: cover; flex-shrink: 0; }
.g-img.placeholder { display: flex; align-items: center; justify-content: center; background: #fff1e0; font-size: 24px; color: #df7e38; }
.g-info { flex: 1; min-width: 0; }
.g-name { font-size: 14px; color: #4a3a2c; font-weight: 600; }
.g-status { font-size: 11px; color: #52a86a; font-weight: normal; margin-left: 6px; }
.g-status.off { color: #b5a48e; }
.g-meta { font-size: 13px; color: #ea7a38; margin-top: 6px; }
.g-ops { display: flex; gap: 8px; }
.g-ops button {
  border: 1.5px solid #ffcda5; background: #fff; color: #df7e38;
  border-radius: 12px; padding: 6px 12px; font-size: 12px; cursor: pointer;
}
.g-ops button:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.g-ops button:active { transform: scale(0.94); }
.g-ops button.danger { color: #e5533c; border-color: #ffcdbf; background: #fff5f2; }
.coupon-row {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 0; border-bottom: 1px dashed #f5e8d8;
}
.c-money { font-size: 20px; font-weight: 700; color: #ea7a38; min-width: 56px; }
.c-info { flex: 1; min-width: 0; }
.c-name { font-size: 14px; color: #6b4f3a; }
.c-meta { font-size: 11px; color: #b5a48e; margin-top: 4px; }
.c-expire { font-size: 11px; color: #b5a48e; }
.c-del {
  border: 1.5px solid #ffcdbf; background: #fff5f2; color: #e5533c;
  border-radius: 12px; padding: 6px 12px; font-size: 12px; cursor: pointer;
}
.c-del:hover { border-color: #e5533c; background: #ffeae4; }
.c-del:active { transform: scale(0.94); }
.modal-mask {
  position: fixed; inset: 0; background: rgba(60, 40, 20, 0.45);
  z-index: 120; display: flex; align-items: center; justify-content: center; padding: 20px;
}
.modal {
  width: 100%; max-width: 420px; max-height: 85vh; overflow-y: auto;
  background: #fff; border-radius: 20px; padding: 24px 20px;
  box-shadow: 0 16px 48px rgba(60, 40, 20, 0.18);
}
.modal-title { text-align: center; font-weight: 600; color: #6b4f3a; margin-bottom: 18px; }
.modal-btns { display: flex; gap: 12px; }
.cancel-btn {
  flex: 1; height: 44px; border-radius: 14px; border: 1.5px solid #e8dcc8;
  background: #fff; color: #9b8b76; cursor: pointer;
}
.cancel-btn:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.modal-btns .btn-primary { flex: 1; }
.load-more {
  display: block; margin: 10px auto 0; border: 1.5px solid #ffcda5; background: #fff;
  color: #df7e38; border-radius: 14px; padding: 8px 26px; font-size: 13px; cursor: pointer;
}
.load-more:hover { border-color: #f2a268; color: #df7e38; background: #fff5ec; }
.load-more:active { transform: scale(0.94); }
</style>
