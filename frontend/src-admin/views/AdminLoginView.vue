<script setup lang="ts">
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '@admin/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAdminAuthStore()

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
    await authStore.login(form.phone, form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.push(redirect)
  } catch (error) {
    alert((error as Error).message)
  }
}
</script>

<template>
  <section class="admin-login">
    <div class="admin-login__hero">
      <div class="admin-login__hero-card">
        <span class="admin-login__eyebrow">Standalone Admin</span>
        <h1>尚有新生管理员后台</h1>
        <p>这是独立于用户前台之外的 PC 桌面管理站点，需要通过单独网址和管理员账号密码进入。</p>

        <div class="admin-login__feature-grid">
          <article>
            <strong>独立网址</strong>
            <span>单独后台入口，不放在用户 App 内</span>
          </article>
          <article>
            <strong>桌面后台</strong>
            <span>左侧菜单 + 右侧内容区的 PC 布局</span>
          </article>
          <article>
            <strong>账号密码登录</strong>
            <span>管理员单独鉴权，不与前台共用入口</span>
          </article>
          <article>
            <strong>运营总览</strong>
            <span>集中管理用户、商品、分类和订单</span>
          </article>
        </div>
      </div>
    </div>

    <form class="admin-login__panel" @submit.prevent="onSubmit">
      <div class="admin-login__panel-head">
        <strong>管理员登录</strong>
        <small>建议将后台部署在单独域名，如 `admin.example.com`。</small>
      </div>

      <label>
        <span>管理员账号</span>
        <input v-model="form.phone" placeholder="请输入管理员手机号" />
      </label>

      <label>
        <span>管理员密码</span>
        <input v-model="form.password" type="password" placeholder="请输入管理员密码" />
      </label>

      <button class="admin-login__submit" type="submit">进入管理系统</button>

      <div class="admin-login__tips">
        <span>后台入口地址请按部署环境配置</span>
        <span>当前为独立管理员站点，不从前台页面进入</span>
      </div>
    </form>
  </section>
</template>

<style scoped>
.admin-login {
  min-height: 100vh;
  padding: 36px;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) 430px;
  gap: 24px;
  align-items: stretch;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.22), transparent 24%),
    radial-gradient(circle at bottom right, rgba(20, 184, 166, 0.16), transparent 22%),
    linear-gradient(180deg, #f7fbff 0%, #eef4ff 100%);
}

.admin-login__hero-card,
.admin-login__panel {
  border-radius: 36px;
  border: 1px solid rgba(219, 228, 241, 0.95);
  background: rgba(255, 255, 255, 0.97);
  box-shadow: 0 24px 56px rgba(15, 23, 42, 0.08);
}

.admin-login__hero {
  display: grid;
}

.admin-login__hero-card {
  padding: 40px;
  display: grid;
  align-content: start;
  gap: 20px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(243, 248, 255, 0.96) 100%);
}

.admin-login__eyebrow {
  width: fit-content;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-login__hero-card h1,
.admin-login__hero-card p,
.admin-login__panel-head strong,
.admin-login__panel-head small,
.admin-login__tips span {
  margin: 0;
}

.admin-login__hero-card h1 {
  font-size: 52px;
  line-height: 1.06;
}

.admin-login__hero-card p {
  max-width: 720px;
  color: #64748b;
  font-size: 17px;
  line-height: 1.9;
}

.admin-login__feature-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.admin-login__feature-grid article {
  min-height: 132px;
  padding: 22px;
  border-radius: 26px;
  background: #f8fbff;
  border: 1px solid rgba(191, 219, 254, 0.75);
  display: grid;
  gap: 10px;
}

.admin-login__feature-grid strong {
  font-size: 18px;
  color: #0f172a;
}

.admin-login__feature-grid span,
.admin-login__panel-head small,
.admin-login__tips span {
  color: #64748b;
  line-height: 1.7;
}

.admin-login__panel {
  padding: 32px 30px;
  display: grid;
  align-content: center;
  gap: 18px;
}

.admin-login__panel-head {
  display: grid;
  gap: 8px;
}

.admin-login__panel-head strong {
  font-size: 30px;
}

.admin-login__panel label {
  display: grid;
  gap: 8px;
}

.admin-login__panel label span {
  color: #334155;
  font-weight: 700;
}

.admin-login__panel input {
  min-height: 56px;
  padding: 0 16px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: #fff;
  color: #0f172a;
}

.admin-login__submit {
  min-height: 56px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(135deg, #2563eb, #14b8a6);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  box-shadow: 0 18px 30px rgba(37, 99, 235, 0.2);
}

.admin-login__tips {
  display: grid;
  gap: 8px;
  padding-top: 6px;
}
</style>
