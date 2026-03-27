<script setup lang="ts">
import { computed, onMounted, reactive, ref, watchEffect } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile } from '@/api/auth'
import { uploadFile } from '@/api/file'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth.store'
import { applyImageFallback, resolveAssetUrl } from '@/utils/assets'

const store = useAppStore()
const authStore = useAuthStore()
const router = useRouter()

const form = reactive({
  name: '',
  city: '',
  bio: '',
  avatar: ''
})

const uploading = ref(false)
const saving = ref(false)

watchEffect(() => {
  if (!store.currentUser) return
  form.name = store.currentUser.name
  form.city = store.currentUser.city
  form.bio = store.currentUser.bio
  form.avatar = store.currentUser.avatar
})

onMounted(async () => {
  await getProfile()
})

const avatarUrl = computed(() => resolveAssetUrl(form.avatar || store.currentUser?.avatar, 'avatar'))

const onSelectAvatar = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const payload = await uploadFile(file)
    form.avatar = payload.url
  } catch (error) {
    alert((error as Error).message)
  } finally {
    uploading.value = false
    target.value = ''
  }
}

const onSave = async () => {
  saving.value = true
  try {
    await authStore.updateProfile(form)
    alert('资料已更新')
    router.push('/profile/settings')
  } catch (error) {
    alert((error as Error).message)
  } finally {
    saving.value = false
  }
}

const onAvatarError = (event: Event) => applyImageFallback(event, 'avatar')
</script>

<template>
  <section v-if="store.currentUser" class="profile-page profile-page--compact">
    <div class="section-head">
      <div>
        <span class="eyebrow">编辑资料</span>
        <h1>更新你的主页信息</h1>
        <p>头像、昵称、城市和个人简介会同步到个人主页展示。</p>
      </div>
      <RouterLink class="ghost-btn" to="/profile/settings">返回设置</RouterLink>
    </div>

    <div class="profile-edit-layout">
      <section class="panel profile-edit-card">
        <img
          :src="avatarUrl"
          :alt="form.name || store.currentUser.name"
          class="profile-avatar profile-avatar--xl"
          @error="onAvatarError"
        />
        <div class="profile-edit-card__meta">
          <strong>{{ form.name || store.currentUser.name }}</strong>
          <span>{{ form.city || store.currentUser.city || '未设置城市' }}</span>
        </div>
        <label class="upload-dropzone">
          <input type="file" accept="image/*" @change="onSelectAvatar" />
          <strong>{{ uploading ? '头像上传中...' : '更换头像' }}</strong>
          <span>支持 JPG、PNG，上传完成后会立即展示。</span>
        </label>
      </section>

      <section class="panel">
        <form class="form-grid" @submit.prevent="onSave">
          <label>
            <span>昵称</span>
            <input v-model.trim="form.name" placeholder="请输入昵称" />
          </label>
          <label>
            <span>城市</span>
            <input v-model.trim="form.city" placeholder="请输入所在城市" />
          </label>
          <label>
            <span>个人简介</span>
            <textarea v-model.trim="form.bio" rows="6" placeholder="写点你的偏好、风格和生活方式" />
          </label>
          <div class="form-actions">
            <button class="primary-btn" type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存资料' }}</button>
            <RouterLink class="ghost-btn" to="/profile/settings">取消</RouterLink>
          </div>
        </form>
      </section>
    </div>
  </section>
</template>
