<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile } from '@/api/auth'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth.store'

const store = useAppStore()
const authStore = useAuthStore()
const router = useRouter()

onMounted(async () => {
  await getProfile()
})

const accountEntries = [
  { title: '编辑资料', desc: '修改头像、昵称、城市和简介', to: '/profile/edit' },
  { title: '实名认证', desc: '提升 KYC 等级，解锁高价值商品流转', to: '/profile/kyc' },
  { title: '修改密码', desc: '更新账户密码，增强账户安全', to: '/profile/password' }
]

const serviceEntries = [
  { title: '我的商品', desc: '管理草稿、待审核和在售中的商品', to: '/goods/manage' },
  { title: '我的收藏', desc: '集中查看关注中的商品与当前状态', to: '/profile/favorites' },
  { title: '消息通知', desc: '查看订单、审核和退款进度提醒', to: '/profile/messages' },
  { title: '订单中心', desc: '查看购买、出售订单与物流轨迹', to: '/order/list' },
  { title: '碳账户', desc: '查看积分流水和下载碳减排证书', to: '/carbon' },
  { title: '发布闲置', desc: '继续发布新的高价值商品', to: '/goods/publish' },
  { title: '鉴定预约', desc: '预约 AI、视频或线下鉴定服务', to: '/appraise' }
]

const onLogout = async () => {
  await authStore.logout()
  router.push('/')
}
</script>

<template>
  <section v-if="store.currentUser" class="profile-page profile-page--compact settings-page">
    <div class="profile-topbar">
      <div>
        <span class="eyebrow">设置</span>
        <h1>设置</h1>
      </div>
      <RouterLink class="profile-settings-btn profile-settings-btn--plain" to="/profile">返回</RouterLink>
    </div>

    <section class="panel settings-panel">
      <span class="settings-section-title">账户</span>
      <div class="settings-list">
        <RouterLink v-for="item in accountEntries" :key="item.title" class="settings-row" :to="item.to">
          <div class="settings-row__content">
            <span class="settings-row__label">{{ item.title }}</span>
            <small class="settings-row__desc">{{ item.desc }}</small>
          </div>
          <span class="settings-row__arrow">›</span>
        </RouterLink>
      </div>
    </section>

    <section class="panel settings-panel">
      <span class="settings-section-title">交易服务</span>
      <div class="settings-list">
        <RouterLink v-for="item in serviceEntries" :key="item.title" class="settings-row" :to="item.to">
          <div class="settings-row__content">
            <span class="settings-row__label">{{ item.title }}</span>
            <small class="settings-row__desc">{{ item.desc }}</small>
          </div>
          <span class="settings-row__arrow">›</span>
        </RouterLink>
      </div>
    </section>

    <section class="panel settings-panel">
      <span class="settings-section-title">安全</span>
      <div class="settings-list">
        <button class="settings-row settings-row--danger" type="button" @click="onLogout">
          <div class="settings-row__content">
            <span class="settings-row__label">退出登录</span>
            <small class="settings-row__desc">清除当前登录态并返回首页</small>
          </div>
        </button>
      </div>
    </section>
  </section>
</template>

<style scoped>
.settings-row__content {
  display: grid;
  gap: 6px;
  text-align: left;
}

.settings-row__desc {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.6;
}
</style>
