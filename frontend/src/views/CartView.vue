<script setup>
/**
 * 购物车页：列表（Redis 实时库存）/ 勾选结算 / 数量修改 / 删除 / 优惠券选择 / 结算下单
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { cartListApi, cartUpdateApi, cartRemoveApi, cartCheckoutApi } from '../api/cart'
import { usableCouponsApi } from '../api/coupon'
import { addToast } from '../utils/toast'
import EmptyState from '../components/EmptyState.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import TabBar from '../components/TabBar.vue'

const router = useRouter()

const loading = ref(true)
const items = ref([])
const checkedIds = ref(new Set())

/* 优惠券状态 */
const coupons = ref([])
const selectedCoupon = ref(null) // {id, couponName, money}
const couponPickerVisible = ref(false)

onMounted(async () => {
  try {
    items.value = await cartListApi()
    items.value.forEach((it) => checkedIds.value.add(it.goodsId))
    try {
      coupons.value = await usableCouponsApi()
    } catch (e) {
      coupons.value = []
    }
  } finally {
    loading.value = false
  }
})

/* ---------------- 勾选与统计 ---------------- */
const checkedItems = computed(() => items.value.filter((it) => checkedIds.value.has(it.goodsId)))
const allChecked = computed(() => items.value.length > 0 && checkedItems.value.length === items.value.length)

/** 小计：勾选商品价格合计 */
const subtotal = computed(() =>
  checkedItems.value.reduce((sum, it) => sum + Number(it.price) * it.num, 0)
)

/** 优惠抵扣：作用于勾选商品中金额最高的一单，抵扣不超过该单金额 */
const discount = computed(() => {
  if (!selectedCoupon.value || !checkedItems.value.length) return 0
  const maxAmount = Math.max(...checkedItems.value.map((it) => Number(it.price) * it.num))
  return Math.min(Number(selectedCoupon.value.money), maxAmount)
})

const payable = computed(() => Math.max(0, subtotal.value - discount.value))

function toggleCheck(it) {
  const s = new Set(checkedIds.value)
  s.has(it.goodsId) ? s.delete(it.goodsId) : s.add(it.goodsId)
  checkedIds.value = s
}

function toggleAll() {
  checkedIds.value = allChecked.value ? new Set() : new Set(items.value.map((it) => it.goodsId))
}

/* ---------------- 数量与删除 ---------------- */
async function changeNum(it, delta) {
  const next = it.num + delta
  if (next < 1) return
  if (next > it.stock) {
    addToast('已达库存上限', 'error')
    return
  }
  await cartUpdateApi({ goodsId: it.goodsId, num: next })
  it.num = next
}

async function removeItem(it) {
  if (!window.confirm(`确定把「${it.petName}」移出购物车吗？`)) return
  await cartRemoveApi(it.goodsId)
  items.value = items.value.filter((x) => x.goodsId !== it.goodsId)
  const s = new Set(checkedIds.value)
  s.delete(it.goodsId)
  checkedIds.value = s
  addToast('已移除', 'success')
}

/* ---------------- 优惠券选择 ---------------- */
function openCouponPicker() {
  if (!coupons.value.length) {
    addToast('暂无可用优惠券，去店铺领一张吧', 'error')
    return
  }
  couponPickerVisible.value = true
}

function pickCoupon(c) {
  selectedCoupon.value = selectedCoupon.value?.id === c.id ? null : c
  couponPickerVisible.value = false
}

/* ---------------- 结算 ---------------- */
const settling = ref(false)

async function checkout() {
  if (!checkedItems.value.length) {
    addToast('请先勾选要结算的商品', 'error')
    return
  }
  settling.value = true
  try {
    await cartCheckoutApi({
      goodsIds: checkedItems.value.map((it) => it.goodsId),
      userCouponId: selectedCoupon.value?.id || null
    })
    addToast('结算成功，订单生成中，可在"我的-我的订单"查看', 'success')
    const settled = new Set(checkedItems.value.map((it) => it.goodsId))
    items.value = items.value.filter((it) => !settled.has(it.goodsId))
    checkedIds.value = new Set()
    selectedCoupon.value = null
    // 结算成功后券已被核销，刷新可用列表
    coupons.value = await usableCouponsApi().catch(() => [])
  } finally {
    settling.value = false
  }
}

function goGoods(it) {
  router.push('/goods/' + it.goodsId)
}
</script>

<template>
  <div class="cart-page">
    <header class="page-head">
      <span class="head-title">购物车</span>
      <span class="head-count">{{ items.length }} 件宝贝</span>
    </header>

    <LoadingSpinner v-if="loading" />
    <template v-else>
      <EmptyState v-if="!items.length" text="购物车空空的，去店铺看看毛孩子吧" icon="🛒" />
      <div v-else class="cart-list">
        <div v-for="it in items" :key="it.goodsId" class="cart-item">
          <span class="check" :class="{ on: checkedIds.has(it.goodsId) }" @click="toggleCheck(it)">
            {{ checkedIds.has(it.goodsId) ? '✓' : '' }}
          </span>
          <img v-if="it.image" class="item-img" :src="it.image" alt="商品图" @click="goGoods(it)" />
          <div v-else class="item-img placeholder" @click="goGoods(it)">🐾</div>
          <div class="item-info">
            <p class="item-name" @click="goGoods(it)">{{ it.petName }}</p>
            <p class="item-price">￥{{ it.price }} <span class="item-stock">库存 {{ it.stock }}</span></p>
            <div class="item-foot">
              <div class="stepper">
                <button :disabled="it.num <= 1" @click="changeNum(it, -1)">−</button>
                <span>{{ it.num }}</span>
                <button :disabled="it.num >= it.stock" @click="changeNum(it, 1)">＋</button>
              </div>
              <span class="item-del" @click="removeItem(it)">删除</span>
            </div>
          </div>
        </div>

        <!-- 优惠券入口 -->
        <div class="coupon-entry" @click="openCouponPicker">
          <span>优惠券</span>
          <span class="coupon-val" :class="{ picked: selectedCoupon }">
            {{ selectedCoupon ? `-￥${Math.min(Number(selectedCoupon.money), subtotal)}（${selectedCoupon.couponName}）` : (coupons.length ? `${coupons.length} 张可用 ›` : '暂无可用 ›') }}
          </span>
        </div>
      </div>

      <!-- 结算栏 -->
      <footer v-if="items.length" class="settle-bar">
        <label class="all-check" @click="toggleAll">
          <span class="check" :class="{ on: allChecked }">{{ allChecked ? '✓' : '' }}</span>
          全选
        </label>
        <div class="settle-total">
          <p class="total-line">合计：<b>￥{{ payable.toFixed(2) }}</b></p>
          <p v-if="discount > 0" class="discount-line">已优惠 ￥{{ discount.toFixed(2) }}</p>
        </div>
        <button class="settle-btn" :disabled="settling || !checkedItems.length" @click="checkout">
          {{ settling ? '结算中...' : `结算(${checkedItems.length})` }}
        </button>
      </footer>
    </template>

    <!-- 优惠券选择弹层 -->
    <div v-if="couponPickerVisible" class="modal-mask" @click.self="couponPickerVisible = false">
      <div class="coupon-modal">
        <p class="modal-title">选择优惠券</p>
        <div class="coupon-option" :class="{ active: !selectedCoupon }" @click="selectedCoupon = null; couponPickerVisible = false">
          <span>不使用优惠券</span>
        </div>
        <div
          v-for="c in coupons"
          :key="c.id"
          class="coupon-option"
          :class="{ active: selectedCoupon && selectedCoupon.id === c.id }"
          @click="pickCoupon(c)"
        >
          <span class="opt-money">￥{{ c.money }}</span>
          <div class="opt-info">
            <p class="opt-name">{{ c.couponName }}</p>
            <p class="opt-expire">{{ (c.expireTime || '').slice(0, 10) }} 前有效</p>
          </div>
          <span v-if="selectedCoupon && selectedCoupon.id === c.id" class="opt-check">✓</span>
        </div>
      </div>
    </div>

    <TabBar />
  </div>
</template>

<style scoped>
.cart-page { min-height: 100vh; background: #fff6ec; padding-bottom: 150px; }
.page-head {
  display: flex; align-items: baseline; gap: 12px;
  padding: 20px 16px 12px;
}
.head-title { font-size: 20px; font-weight: 700; color: #6b4f3a; }
.head-count { font-size: 12px; color: #b5a48e; }
.cart-list { padding: 8px 16px 0; }
.cart-item {
  display: flex; align-items: center; gap: 14px;
  background: #fff; border-radius: 16px; padding: 14px; margin-bottom: 12px;
  box-shadow: 0 4px 14px rgba(255, 140, 66, 0.1);
}
.check {
  width: 20px; height: 20px; border-radius: 50%;
  border: 1.5px solid #e0cdb4; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; color: #fff; cursor: pointer;
}
.check.on { background: linear-gradient(135deg, #f7a76b, #f2a268); border-color: #f2a268; }
.item-img { width: 84px; height: 84px; border-radius: 14px; object-fit: cover; cursor: pointer; flex-shrink: 0; }
.item-img.placeholder {
  display: flex; align-items: center; justify-content: center;
  background: #fff1e0; font-size: 30px;
}
.item-info { flex: 1; min-width: 0; }
.item-name { font-size: 15px; color: #4a3a2c; font-weight: 600; cursor: pointer; }
.item-price { font-size: 15px; color: #ff6b2c; font-weight: 700; margin-top: 6px; }
.item-stock { font-size: 11px; color: #b5a48e; font-weight: normal; margin-left: 8px; }
.item-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }
.stepper { display: flex; align-items: center; gap: 12px; }
.stepper button {
  width: 26px; height: 26px; border-radius: 50%; border: 1.5px solid #ffcda5;
  background: #fffaf4; color: #ff8c42; font-size: 14px; cursor: pointer;
}
.stepper button:disabled { opacity: 0.35; cursor: not-allowed; }
.stepper span { min-width: 20px; text-align: center; font-weight: 600; color: #4a3a2c; }
.item-del { font-size: 12px; color: #e5533c; cursor: pointer; }
.coupon-entry {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-radius: 16px; padding: 14px; margin-top: 8px;
  box-shadow: 0 4px 14px rgba(255, 140, 66, 0.1);
  font-size: 14px; color: #6b4f3a; cursor: pointer;
}
.coupon-val { font-size: 12px; color: #b5a48e; }
.coupon-val.picked { color: #ff6b2c; font-weight: 600; font-size: 13px; }
.settle-bar {
  position: fixed; left: 0; right: 0; bottom: 62px;
  display: flex; align-items: center; gap: 14px;
  background: rgba(255, 255, 255, 0.97);
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  box-shadow: 0 -4px 16px rgba(255, 140, 66, 0.12);
  z-index: 20;
}
.all-check {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #9b8b76; cursor: pointer;
}
.settle-total { flex: 1; text-align: right; }
.total-line { font-size: 13px; color: #6b4f3a; }
.total-line b { color: #ff6b2c; font-size: 18px; }
.discount-line { font-size: 11px; color: #52a86a; margin-top: 4px; }
.settle-btn {
  border: none; border-radius: 22px; padding: 11px 24px;
  background: linear-gradient(135deg, #f7a76b, #f2a268); color: #fff;
  font-size: 14px; font-weight: 600; cursor: pointer;
  box-shadow: 0 6px 16px rgba(255, 110, 40, 0.35);
}
.settle-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.modal-mask {
  position: fixed; inset: 0; background: rgba(60, 40, 20, 0.45);
  z-index: 120; display: flex; align-items: flex-end; justify-content: center;
}
.coupon-modal {
  width: 100%; max-width: 520px; max-height: 65vh; overflow-y: auto;
  background: #fff; border-radius: 22px 22px 0 0; padding: 22px 18px 28px;
}
.modal-title { text-align: center; font-weight: 600; color: #6b4f3a; margin-bottom: 16px; }
.coupon-option {
  display: flex; align-items: center; gap: 14px;
  border: 1.5px solid #f5e8d8; border-radius: 14px;
  padding: 14px; margin-bottom: 12px; cursor: pointer; font-size: 14px; color: #6b4f3a;
}
.coupon-option.active { border-color: #df7e38; background: #fff5ec; }
.opt-money { font-size: 20px; font-weight: 700; color: #ea7a38; min-width: 58px; }
.opt-info { flex: 1; }
.opt-name { font-size: 14px; }
.opt-expire { font-size: 11px; color: #b5a48e; margin-top: 3px; }
.opt-check { color: #e07e35; font-weight: 700; }
</style>
