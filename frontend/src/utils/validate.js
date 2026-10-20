/**
 * 前端实时校验工具（与后端规则保持一致）
 */
export const PHONE_REG = /^1[3-9]\d{9}$/

/** 手机号校验 */
export function isValidPhone(phone) {
  return PHONE_REG.test(phone || '')
}

/** 密码校验：6-20位 */
export function isValidPassword(pwd) {
  return typeof pwd === 'string' && pwd.length >= 6 && pwd.length <= 20
}

/** 短信验证码校验：6位数字 */
export function isValidSmsCode(code) {
  return /^\d{6}$/.test(code || '')
}

/** 用户名校验：3-20位字母数字下划线 */
export function isValidUsername(name) {
  return /^\w{3,20}$/.test(name || '')
}

/**
 * 秒杀倒计时：返回 {ended, text}，格式 HH:MM:SS（跨天则 N天 HH:MM:SS）
 */
export function countdownText(targetTime, now = Date.now()) {
  const diff = new Date(targetTime).getTime() - now
  if (diff <= 0) return { ended: true, text: '已结束' }
  const s = Math.floor(diff / 1000)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const text = (d > 0 ? d + '天' : '') + pad2(h) + ':' + pad2(m) + ':' + pad2(sec)
  return { ended: false, text }
}

function pad2(n) {
  return n < 10 ? '0' + n : '' + n
}
