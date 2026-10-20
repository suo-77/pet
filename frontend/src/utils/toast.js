import { reactive } from 'vue'

/**
 * 轻量全局 Toast：模块级响应式队列，ToastHost 组件统一渲染
 * 用法：addToast('操作成功') / addToast('出错了', 'error')
 */
const state = reactive({
  list: [],
  id: 0
})

/** 弹出提示，默认 2.5 秒自动消失 */
export function addToast(msg, type = 'success', duration = 2500) {
  const item = { id: ++state.id, msg, type }
  state.list.push(item)
  setTimeout(() => {
    const index = state.list.findIndex((t) => t.id === item.id)
    if (index > -1) {
      state.list.splice(index, 1)
    }
  }, duration)
}

export function toastState() {
  return state
}

/** 便捷别名 */
export const toastSuccess = (msg) => addToast(msg, 'success')
export const toastError = (msg) => addToast(msg, 'error')
