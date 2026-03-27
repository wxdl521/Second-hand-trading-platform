<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile } from '@/api/auth'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth.store'

const store = useAppStore()
const authStore = useAuthStore()
const router = useRouter()

const saving = ref(false)

const form = reactive({
  currentPassword: '',
  nextPassword: '',
  confirmPassword: ''
})

onMounted(async () => {
  await getProfile()
})

const hasExistingPassword = computed(() => Boolean(store.currentUser?.password))

const onSubmit = async () => {
  if (hasExistingPassword.value && !form.currentPassword) {
    alert('请先输入当前密码')
    return
  }

  if (!form.nextPassword || form.nextPassword.length < 6) {
    alert('新密码至少需要 6 位')
    return
  }

  if (form.nextPassword !== form.confirmPassword) {
    alert('两次输入的新密码不一致')
    return
  }

  saving.value = true

  try {
    await authStore.changePassword({
      currentPassword: form.currentPassword,
      nextPassword: form.nextPassword
    })
    alert('密码修改成功')
    router.push('/profile/settings')
  } catch (error) {
    alert((error as Error).message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section v-if="store.currentUser" class="profile-page profile-page--compact">
    <div class="section-head">
      <div>
        <span class="eyebrow">修改密码</span>
        <h1>{{ hasExistingPassword ? '更新账户密码' : '为账户设置密码' }}</h1>
        <p>密码修改后会立即同步到当前登录账户，建议完成后重新确认账户信息是否正确。</p>
      </div>
      <RouterLink class="ghost-btn" to="/profile/settings">返回设置</RouterLink>
    </div>

    <div class="two-column">
      <section class="panel">
        <form class="form-grid" @submit.prevent="onSubmit">
          <label v-if="hasExistingPassword">
            <span>当前密码</span>
            <input v-model="form.currentPassword" type="password" placeholder="请输入当前密码" />
          </label>
          <label>
            <span>新密码</span>
            <input v-model="form.nextPassword" type="password" placeholder="请输入新的登录密码" />
          </label>
          <label>
            <span>确认新密码</span>
            <input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" />
          </label>
          <button class="primary-btn primary-btn--full" type="submit" :disabled="saving">
            {{ saving ? '保存中...' : '保存新密码' }}
          </button>
        </form>
      </section>

      <section class="panel">
        <div class="section-head section-head--compact">
          <div>
            <span class="eyebrow">安全建议</span>
            <h2>密码设置规范</h2>
          </div>
        </div>
        <ul class="password-tips">
          <li>建议使用 8 位以上密码，并包含字母和数字。</li>
          <li>不要与管理员后台密码或其他站点密码复用。</li>
          <li>修改后建议重新确认设置页中的账号信息是否正确。</li>
        </ul>
      </section>
    </div>
  </section>
</template>

<style scoped>
.password-tips {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.9;
}
</style>
