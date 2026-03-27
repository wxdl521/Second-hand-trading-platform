<script setup lang="ts">
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  phone: '13800000099',
  password: 'Admin@123'
})

const onSubmit = async () => {
  if (!form.phone || !form.password) {
    alert('请输入管理员账号和密码')
    return
  }

  try {
    await authStore.adminLogin(form.phone, form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin/dashboard'
    router.push(redirect)
  } catch (error) {
    alert((error as Error).message)
  }
}
</script>

<template>
  <section class="admin-login">
    <div class="admin-login__hero">
      <span class="admin-login__eyebrow">Admin Access</span>
      <h1>尚有新生管理系统</h1>
      <p>管理员可通过独立后台入口进入 PC 端运营系统，处理商品审核、订单和用户管理。</p>
      <div class="admin-login__tips">
        <span>入口方式：登录页管理员入口 / 设置页管理员入口</span>
        <span>当前为独立后台界面</span>
      </div>
    </div>

    <form class="admin-login__card" @submit.prevent="onSubmit">
      <div class="admin-login__head">
        <strong>管理员登录</strong>
        <small>仅具备管理员权限的账号可进入</small>
      </div>

      <label>
        <span>管理员账号</span>
        <input v-model="form.phone" placeholder="请输入手机号" />
      </label>

      <label>
        <span>管理员密码</span>
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </label>

      <button class="admin-login__submit" type="submit">进入管理系统</button>

      <div class="admin-login__links">
        <RouterLink to="/login">返回用户登录</RouterLink>
        <RouterLink to="/">回到前台首页</RouterLink>
      </div>
    </form>
  </section>
</template>

<style scoped>
.admin-login {
  min-height: 100vh;
  padding: 48px 24px;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(380px, 460px);
  gap: 28px;
  align-items: center;
  background:
    radial-gradient(circle at top left, rgba(201, 151, 42, 0.24), transparent 24%),
    radial-gradient(circle at bottom right, rgba(27, 107, 58, 0.18), transparent 20%),
    linear-gradient(135deg, #203225 0%, #14512b 42%, #8a6416 100%);
}

.admin-login__hero,
.admin-login__card {
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 20px 60px rgba(20, 34, 24, 0.24);
}

.admin-login__hero {
  padding: 40px;
  color: #fff;
  background: rgba(15, 23, 42, 0.36);
  backdrop-filter: blur(14px);
}

.admin-login__eyebrow {
  display: inline-block;
  margin-bottom: 12px;
  color: #f7d98a;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: none;
}

.admin-login__hero h1 {
  margin: 0 0 14px;
  font-size: 42px;
  line-height: 1.15;
}

.admin-login__hero p {
  margin: 0;
  max-width: 520px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 17px;
  line-height: 1.8;
}

.admin-login__tips {
  margin-top: 26px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.admin-login__tips span {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.84);
}

.admin-login__card {
  padding: 34px 28px;
  background: rgba(255, 255, 255, 0.96);
  display: grid;
  gap: 18px;
}

.admin-login__head {
  display: grid;
  gap: 6px;
}

.admin-login__head strong {
  font-size: 26px;
  color: var(--text);
}

.admin-login__head small {
  color: var(--muted);
}

.admin-login__card label {
  display: grid;
  gap: 8px;
}

.admin-login__card label span {
  color: var(--text);
  font-weight: 600;
}

.admin-login__card input {
  min-height: 52px;
  padding: 0 16px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: var(--surface-strong);
}

.admin-login__submit {
  min-height: 52px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand));
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.admin-login__links {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--muted);
}

@media (max-width: 960px) {
  .admin-login {
    grid-template-columns: 1fr;
    padding: 24px 16px;
  }

  .admin-login__hero,
  .admin-login__card {
    padding: 24px;
  }

  .admin-login__hero h1 {
    font-size: 32px;
  }
}
</style>
