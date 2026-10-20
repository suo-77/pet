import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

/**
 * 路由表：模块9逐个替换为真实页面组件，当前统一指向占位页
 * 需要登录的页面 meta.requiresLogin = true（默认需要，公开页显式标记 false）
 */
const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { requiresLogin: false } },
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
  { path: '/publish', name: 'publish', component: () => import('../views/PublishView.vue') },
  { path: '/shops', name: 'shops', component: () => import('../views/ShopListView.vue') },
  { path: '/shops/:id', name: 'shopDetail', component: () => import('../views/ShopDetailView.vue') },
  { path: '/goods/:id', name: 'goodsDetail', component: () => import('../views/GoodsDetailView.vue') },
  { path: '/cart', name: 'cart', component: () => import('../views/CartView.vue') },
  { path: '/shop/manage', name: 'shopManage', component: () => import('../views/ShopManageView.vue') },
  { path: '/friends', name: 'friends', component: () => import('../views/FriendsView.vue') },
  { path: '/chat', name: 'chat', component: () => import('../views/ChatListView.vue') },
  { path: '/chat/:peerId', name: 'chatRoom', component: () => import('../views/ChatRoomView.vue') },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue') },
  { path: '/user/:id', name: 'userHome', component: () => import('../views/UserHomeView.vue') },
  // 兜底：未匹配跳首页
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

/** 路由守卫：未登录访问受保护页面自动跳转登录页，登录后回跳原页面 */
router.beforeEach((to) => {
  const userStore = useUserStore()
  const requiresLogin = to.meta.requiresLogin !== false
  if (requiresLogin && !userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 已登录访问登录页直接回首页
  if (to.path === '/login' && userStore.token) {
    return { path: '/' }
  }
  return true
})

export default router
