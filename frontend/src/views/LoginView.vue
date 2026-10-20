<script setup>
/**
 * 登录注册页：三种方式
 * 1. 账号密码登录  2. 短信验证码登录（无账号自动注册） 3. 账号密码注册（含头像上传）
 * 前端实时校验手机号/用户名/密码/验证码格式，发送验证码 60s 倒计时防频繁
 */
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loginApi, loginBySmsApi, registerApi, sendSmsCodeApi } from '../api/auth'
import { useUserStore } from '../stores/user'
import { addToast } from '../utils/toast'
import { isValidPassword, isValidPhone, isValidSmsCode, isValidUsername } from '../utils/validate'
import ImgUpload from '../components/ImgUpload.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 当前激活方式：login | sms | register */
const mode = ref('login')
const submitting = ref(false)

/** 密码登录表单 */
const loginForm = reactive({ username: '', password: '' })
/** 验证码登录表单 */
const smsForm = reactive({ phone: '', code: '' })
/** 注册表单 */
const registerForm = reactive({ username: '', password: '', confirmPassword: '', phone: '', code: '', avatar: [] })

/** 表单实时校验错误提示 */
const errors = reactive({ login: '', sms: '', register: '' })

/** 验证码倒计时 */
const countdown = ref(0)
let timer = null
const sending = ref(false)
const canSendCode = computed(() => isValidPhone(smsOrRegPhone.value) && countdown.value === 0 && !sending.value)
const smsOrRegPhone = computed(() => (mode.value === 'sms' ? smsForm.phone : registerForm.phone))

/** 发送短信验证码：后端 Redis 60s 频控 + Stream 异步发送，接口立即返回 */
async function onSendCode() {
  if (!isValidPhone(smsOrRegPhone.value)) {
    addToast('请输入正确的手机号', 'error')
    return
  }
  sending.value = true
  try {
    await sendSmsCodeApi(smsOrRegPhone.value)
    addToast('验证码已发送（演示环境看后端日志）', 'success')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } finally {
    sending.value = false
  }
}

/** 切换方式清空错误提示 */
function switchMode(m) {
  mode.value = m
  errors.login = errors.sms = errors.register = ''
}

/** 登录成功统一处理：写状态 + 回跳 */
function loginSuccess(data) {
  userStore.setLogin(data.token, data.user)
  addToast('欢迎回来，' + (data.user?.nickname || '铲屎官'), 'success')
  router.replace(route.query.redirect || '/')
}

/** 账号密码登录 */
async function onLogin() {
  errors.login = ''
  if (!isValidUsername(loginForm.username)) {
    errors.login = '用户名为3-20位字母数字下划线'
    return
  }
  if (!isValidPassword(loginForm.password)) {
    errors.login = '密码为6-20位'
    return
  }
  submitting.value = true
  try {
    loginSuccess(await loginApi({ ...loginForm }))
  } catch (e) {
    // 业务失败 request 层已 toast，这里仅保持表单
  } finally {
    submitting.value = false
  }
}

/** 验证码登录（无账号自动注册） */
async function onSmsLogin() {
  errors.sms = ''
  if (!isValidPhone(smsForm.phone)) {
    errors.sms = '请输入正确的手机号'
    return
  }
  if (!isValidSmsCode(smsForm.code)) {
    errors.sms = '验证码为6位数字'
    return
  }
  submitting.value = true
  try {
    loginSuccess(await loginBySmsApi({ ...smsForm }))
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

/** 注册 */
async function onRegister() {
  errors.register = ''
  if (!isValidUsername(registerForm.username)) {
    errors.register = '用户名为3-20位字母数字下划线'
    return
  }
  if (!isValidPassword(registerForm.password)) {
    errors.register = '密码为6-20位'
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    errors.register = '两次密码输入不一致'
    return
  }
  if (!isValidPhone(registerForm.phone)) {
    errors.register = '请输入正确的手机号'
    return
  }
  if (!isValidSmsCode(registerForm.code)) {
    errors.register = '验证码为6位数字'
    return
  }
  submitting.value = true
  try {
    loginSuccess(await registerApi({
      username: registerForm.username,
      password: registerForm.password,
      phone: registerForm.phone,
      code: registerForm.code,
      avatar: registerForm.avatar[0] || null
    }))
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

onUnmounted(() => timer && clearInterval(timer))
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 品牌区 -->
      <div class="brand">
        <span class="brand-icon">🐾</span>
        <h1>宠物小镇</h1>
        <p>遇见毛孩子，从这里开始</p>
      </div>

      <!-- 方式切换 -->
      <div class="mode-tabs">
        <span :class="{ active: mode === 'login' }" @click="switchMode('login')">密码登录</span>
        <span :class="{ active: mode === 'sms' }" @click="switchMode('sms')">验证码登录</span>
        <span :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</span>
      </div>

      <!-- 账号密码登录 -->
      <form v-if="mode === 'login'" class="form" @submit.prevent="onLogin">
        <input v-model.trim="loginForm.username" placeholder="用户名" maxlength="20" />
        <input v-model="loginForm.password" type="password" placeholder="密码" maxlength="20" @keyup.enter="onLogin" />
        <p v-if="errors.login" class="err">{{ errors.login }}</p>
        <button class="btn-primary btn-block" :disabled="submitting" type="submit">
          {{ submitting ? '登录中...' : '登 录' }}
        </button>
      </form>

      <!-- 验证码登录 -->
      <form v-else-if="mode === 'sms'" class="form" @submit.prevent="onSmsLogin">
        <input v-model.trim="smsForm.phone" placeholder="手机号" maxlength="11" />
        <div class="code-row">
          <input v-model.trim="smsForm.code" placeholder="6位验证码" maxlength="6" @keyup.enter="onSmsLogin" />
          <button class="code-btn" :disabled="!canSendCode" type="button" @click="onSendCode">
            {{ countdown > 0 ? countdown + 's' : (sending ? '发送中' : '获取验证码') }}
          </button>
        </div>
        <p class="tip">未注册的手机号将自动创建账号</p>
        <p v-if="errors.sms" class="err">{{ errors.sms }}</p>
        <button class="btn-primary btn-block" :disabled="submitting" type="submit">
          {{ submitting ? '登录中...' : '登录 / 注册' }}
        </button>
      </form>

      <!-- 注册 -->
      <form v-else class="form" @submit.prevent="onRegister">
        <div class="avatar-row">
          <ImgUpload v-model="registerForm.avatar" :limit="1" />
          <span class="avatar-tip">上传头像（可选）</span>
        </div>
        <input v-model.trim="registerForm.username" placeholder="用户名（3-20位字母数字下划线）" maxlength="20" />
        <input v-model="registerForm.password" type="password" placeholder="密码（6-20位）" maxlength="20" />
        <input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" maxlength="20" />
        <input v-model.trim="registerForm.phone" placeholder="手机号" maxlength="11" />
        <div class="code-row">
          <input v-model.trim="registerForm.code" placeholder="6位验证码" maxlength="6" @keyup.enter="onRegister" />
          <button class="code-btn" :disabled="!canSendCode" type="button" @click="onSendCode">
            {{ countdown > 0 ? countdown + 's' : (sending ? '发送中' : '获取验证码') }}
          </button>
        </div>
        <p v-if="errors.register" class="err">{{ errors.register }}</p>
        <button class="btn-primary btn-block" :disabled="submitting" type="submit">
          {{ submitting ? '注册中...' : '注 册' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #ffe9d2 0%, #fff6ec 55%, #ffdfc0 100%);
  padding: 20px;
}
.login-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(60, 40, 20, 0.18);
  padding: 40px 30px 34px;
}
.brand { text-align: center; margin-bottom: 28px; }
.brand-icon { font-size: 44px; }
.brand h1 { font-size: 24px; color: #6b4f3a; margin: 10px 0 6px; }
.brand p { color: #b5a48e; font-size: 13px; }
.mode-tabs {
  display: flex;
  background: #fff4e8;
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 24px;
}
.mode-tabs span {
  flex: 1;
  text-align: center;
  padding: 9px 0;
  font-size: 14px;
  color: #9b8b76;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.mode-tabs span.active {
  background: linear-gradient(135deg, #f7a76b, #f2a268);
  color: #fff;
  box-shadow: 0 3px 10px rgba(242, 162, 104, 0.35);
}
.form { display: flex; flex-direction: column; gap: 16px; }
.form input {
  height: 48px;
  border: 1.5px solid #ffe0c7;
  border-radius: 12px;
  padding: 0 16px;
  font-size: 15px;
  background: #fffaf4;
  outline: none;
  transition: border-color 0.2s;
}
.form input:focus { border-color: #f2a268; }
.code-row { display: flex; gap: 12px; }
.code-row input { flex: 1; }
.code-btn {
  min-width: 110px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #f7a76b, #f2a268);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
  transition: all .15s;
}
.code-btn:hover:not(:disabled) { filter: brightness(1.05); transform: translateY(-1px); box-shadow: 0 7px 18px rgba(242, 162, 104, 0.4); }
.code-btn:active:not(:disabled) { transform: scale(0.96); }
.code-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.tip { font-size: 12px; color: #b5a48e; margin-top: -6px; }
.err { color: #e5533c; font-size: 13px; margin-top: -6px; }
.avatar-row { display: flex; align-items: center; gap: 14px; }
.avatar-tip { font-size: 12px; color: #b5a48e; }
.btn-primary {
  height: 48px;
  border: none;
  border-radius: 14px;
  font-size: 15px;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, #f7a76b, #f2a268);
  box-shadow: 0 5px 14px rgba(242, 162, 104, 0.32);
  transition: all .15s;
}
.btn-primary:hover:not(:disabled) { filter: brightness(1.05); transform: translateY(-1px); box-shadow: 0 7px 18px rgba(242, 162, 104, 0.4); }
.btn-primary:active:not(:disabled) { transform: scale(0.96); }
.btn-primary:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-block { width: 100%; }
</style>
