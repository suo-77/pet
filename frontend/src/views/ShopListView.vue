<script setup>
/**
 * 店铺列表页：分页展示全部宠物店铺，点击进入店铺详情
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { shopPageApi } from '../api/shop'
import EmptyState from '../components/EmptyState.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import TabBar from '../components/TabBar.vue'

const router = useRouter()

const shops = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
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
    const data = await shopPageApi({ page: page.value, pageSize })
    total.value = data.total
    if (reset) shops.value = data.list
    else shops.value.push(...data.list)
    if (shops.value.length >= data.total || data.list.length === 0) finished.value = true
    else page.value++
  } finally {
    loading.value = false
  }
}

onMounted(() => load(true))
</script>

<template>
  <div class="shop-list-page">
    <header class="page-head">
      <h1>宠物店铺</h1>
      <p>挑选心仪的毛孩子之家</p>
    </header>

    <main class="list">
      <LoadingSpinner v-if="loading && !shops.length" />
      <EmptyState v-else-if="!shops.length" text="暂无店铺入驻" icon="🏪" />
      <div v-for="shop in shops" :key="shop.id" class="shop-card" @click="router.push('/shops/' + shop.id)">
        <img v-if="shop.cover" class="shop-cover" :src="shop.cover" alt="店铺封面" />
        <div v-else class="shop-cover placeholder">🏪</div>
        <div class="shop-body">
          <p class="shop-name">{{ shop.shopName }}</p>
          <p class="shop-desc">{{ shop.shopDesc || '店家很懒，还没写简介' }}</p>
          <span class="enter">进店逛逛 ›</span>
        </div>
      </div>
      <LoadingSpinner v-if="loading && shops.length" text="加载更多..." />
      <button v-if="!finished && shops.length && !loading" class="load-more" @click="load()">加载更多</button>
    </main>

    <TabBar />
  </div>
</template>

<style scoped>
/* 店铺列表：暖橘渐变风格 */
.shop-list-page { min-height: 100vh; background: #fff6ec; padding-bottom: 76px; }
.page-head { padding: 22px 16px 14px; }
.page-head h1 { font-size: 22px; color: #6b4f3a; }
.page-head p { font-size: 13px; color: #b5a48e; margin-top: 6px; }
.list { padding: 8px 16px; }
.shop-card {
  display: flex;
  gap: 14px;
  background: #fff;
  border-radius: 14px;
  padding: 14px;
  margin-bottom: 14px;
  box-shadow: 0 6px 20px rgba(242, 162, 104, 0.1);
  cursor: pointer;
  transition: all 0.2s;
}
.shop-card:hover { transform: translateY(-2px); }
.shop-cover { width: 104px; height: 84px; border-radius: 14px; object-fit: cover; flex-shrink: 0; }
.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff1e0;
  font-size: 32px;
  color: #df7e38;
}
.shop-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px; }
.shop-name { font-size: 16px; font-weight: 600; color: #6b4f3a; }
.shop-desc {
  flex: 1;
  font-size: 13px;
  color: #b5a48e;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.enter { color: #e07e35; font-size: 13px; align-self: flex-end; }
.load-more {
  display: block;
  margin: 10px auto;
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
</style>
