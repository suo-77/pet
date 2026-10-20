/**
 * 友好时间格式化：刚刚 / N分钟前 / N小时前 / 昨天 HH:mm / MM-DD HH:mm / YYYY-MM-DD
 */
export function friendTime(time) {
  if (!time) return ''
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return String(time)
  const diff = Date.now() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 2 * day) return '昨天 ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
  if (date.getFullYear() === new Date().getFullYear()) {
    return (date.getMonth() + 1) + '-' + date.getDate() + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
  }
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
}

/** 完整日期时间格式化 */
export function fullTime(time) {
  if (!time) return ''
  const date = new Date(time)
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) +
    ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
}

/** 补零 */
function pad(n) {
  return n < 10 ? '0' + n : '' + n
}
