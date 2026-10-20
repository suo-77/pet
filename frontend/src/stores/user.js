import { defineStore } from 'pinia'
import { getInfoApi, logoutApi } from '../api/auth'
import { closeWs } from '../utils/ws'

/**
 * 用户状态：token + 用户信息，持久化到 localStorage（刷新不丢失）
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: safeParse(localStorage.getItem('userInfo'))
  }),
  getters: {
    isLogin: (state) => !!state.token
  },
  actions: {
    /** 登录/注册成功后写入状态 */
    setLogin(token, user) {
      this.token = token
      this.userInfo = user
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(user || null))
    },
    setUser(user) {
      this.userInfo = user
      localStorage.setItem('userInfo', JSON.stringify(user || null))
    },
    /** 拉取最新个人信息（路由守卫进入受保护页时刷新） */
    async fetchUserInfo() {
      const user = await getInfoApi()
      this.setUser(user)
      return user
    },
    /** 退出登录：断开 WS + 清除本地状态，删除 Redis token 由后端接口完成 */
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 接口失败也继续本地清理
      }
      closeWs()
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },
    /** 本地清理（401 时调用） */
    clearLocal() {
      closeWs()
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})

/** 安全解析 JSON，异常返回 null */
function safeParse(str) {
  try {
    return JSON.parse(str)
  } catch (e) {
    return null
  }
}
