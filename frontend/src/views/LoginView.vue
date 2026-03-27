<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'

const form = reactive({
  phone: '13800000001',
  otpCode: ''
})

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const cooldown = ref(0)
let timer: number | undefined

const canSendOtp = computed(() => Boolean(form.phone) && cooldown.value === 0)

const startCooldown = () => {
  cooldown.value = 60
  timer = window.setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0 && timer) {
      window.clearInterval(timer)
      timer = undefined
      cooldown.value = 0
    }
  }, 1000)
}

const onSendOtp = async () => {
  if (!form.phone) {
    alert('请先输入手机号')
    return
  }

  try {
    await authStore.sendOtp(form.phone)
    startCooldown()
    alert('验证码已发送，请查看手机短信或后端验证码服务日志')
  } catch (error) {
    alert((error as Error).message)
  }
}

const onSubmit = async () => {
  if (!form.phone || !form.otpCode) {
    alert('请输入手机号和验证码')
    return
  }

  try {
    await authStore.login(form.phone, form.otpCode)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/profile'
    router.push(redirect)
  } catch (error) {
    alert((error as Error).message)
  }
}

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <section class="auth-layout">
    <div class="auth-card">
      <span class="eyebrow">欢迎回来</span>
      <h1>登录尚有新生</h1>
      <p>验证码发送后请使用最新收到的动态验证码登录；如果短信服务未接通，可查看后端验证码服务日志。</p>

      <form class="form-grid" @submit.prevent="onSubmit">
        <label>
          <span>手机号</span>
          <input v-model="form.phone" placeholder="请输入手机号" />
        </label>
        <label>
          <span>验证码</span>
          <div class="otp-row">
            <input v-model="form.otpCode" placeholder="请输入验证码" />
            <button class="ghost-btn" type="button" :disabled="!canSendOtp" @click="onSendOtp">
              {{ cooldown > 0 ? `${cooldown}s 后重发` : '发送验证码' }}
            </button>
          </div>
        </label>
        <button class="primary-btn primary-btn--full" type="submit">登录</button>
      </form>

      <div class="auth-actions">
        <RouterLink class="ghost-btn auth-actions__register" to="/register">注册账号</RouterLink>
      </div>
    </div>
  </section>
</template>

<style scoped>
.auth-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-start;
}

.auth-actions__register {
  min-width: 132px;
  justify-content: center;
}

.otp-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}
</style>
