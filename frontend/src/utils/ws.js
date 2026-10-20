/**
 * WebSocket 封装：连接 /ws/chat?token=xxx，断线自动重连，消息事件订阅分发
 * 服务端下行：{type:"connected"|"chat"|"read"|"error", ...}
 */
let socket = null
let reconnectTimer = null
let currentToken = ''
const handlers = new Set()

/** 建立连接（已连接则忽略） */
export function connectWs(token) {
  if (!token || (socket && socket.readyState === WebSocket.OPEN)) return
  currentToken = token
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  socket = new WebSocket(`${protocol}://${window.location.host}/ws/chat?token=${encodeURIComponent(token)}`)

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      handlers.forEach((fn) => fn(data))
    } catch (e) {
      // 非 JSON 消息忽略
    }
  }

  socket.onclose = () => {
    socket = null
    // token 仍存在时 3 秒后重连
    if (localStorage.getItem('token')) {
      reconnectTimer = setTimeout(() => connectWs(currentToken), 3000)
    }
  }

  socket.onerror = () => {
    socket && socket.close()
  }
}

/** 发送消息对象 */
export function sendWs(obj) {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(obj))
    return true
  }
  return false
}

/** 订阅服务端消息，返回取消订阅函数 */
export function onWs(fn) {
  handlers.add(fn)
  return () => handlers.delete(fn)
}

/** 关闭连接并停止重连 */
export function closeWs() {
  clearTimeout(reconnectTimer)
  currentToken = ''
  if (socket) {
    socket.onclose = null
    socket.close()
    socket = null
  }
}
