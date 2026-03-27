<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'

const authStore = useAuthStore()
const router = useRouter()
const cooldown = ref(0)
let timer: number | undefined

const form = reactive({
  nickname: '',
  phone: '',
  otpCode: '',
  password: '',
  confirmPassword: ''
})

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
  if (!form.nickname || !form.phone || !form.otpCode || !form.password) {
    alert('请完整填写注册信息')
    return
  }
  if (form.password !== form.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }

  try {
    await authStore.register(form.nickname, form.phone, form.otpCode, form.password)
    router.push('/profile')
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
      <span class="eyebrow">创建账号</span>
      <h1>开启你的绿色循环资产账户</h1>
      <form class="form-grid" @submit.prevent="onSubmit">
        <label>
          <span>昵称</span>
          <input v-model="form.nickname" placeholder="请输入昵称" />
        </label>
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
        <label>
          <span>密码</span>
          <input v-model="form.password" type="password" placeholder="至少 6 位" />
        </label>
        <label>
          <span>确认密码</span>
          <input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" />
        </label>
        <button class="primary-btn primary-btn--full" type="submit">立即注册</button>
      </form>
      <div class="inline-links">
        <RouterLink to="/login">已有账号，去登录</RouterLink>
      </div>
    </div>
  </section>
</template>

<style scoped>
.otp-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}
</style>
