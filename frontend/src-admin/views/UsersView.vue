<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AdminDialog from '@admin/components/AdminDialog.vue'
import { useAdminDataStore } from '@admin/stores/data'
import type { AdminKycReviewStatus, AdminUserAccountStatus, AdminUserDraft } from '@admin/types'
import type { KycLevel, UserRole } from '@shared/types'

const dataStore = useAdminDataStore()
const loading = ref(false)
const keyword = ref('')
const accountFilter = ref<'全部' | AdminUserAccountStatus>('全部')
const kycFilter = ref<'全部' | AdminKycReviewStatus>('全部')
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const selectedIds = ref<string[]>([])

const roleOptions: UserRole[] = ['USER', 'SELLER', 'APPRAISER', 'ADMIN']
const kycOptions: KycLevel[] = ['L1', 'L2', 'L3']
const accountStatusOptions: AdminUserAccountStatus[] = ['正常', '已封禁']
const kycReviewOptions: AdminKycReviewStatus[] = ['待审核', '已通过', '已驳回']

const createEmptyUserForm = (): AdminUserDraft => ({
  name: '',
  phone: '',
  password: 'Test@123',
  avatar: '',
  city: '上海',
  bio: '',
  role: 'USER',
  kycLevel: 'L1',
  carbonPoints: 0,
  likedGoodsIds: [],
  accountStatus: '正常',
  kycReviewStatus: '待审核',
  registerAt: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
  lastActiveAt: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
})

const form = reactive<AdminUserDraft>(createEmptyUserForm())

const resetForm = () => {
  Object.assign(form, createEmptyUserForm())
  editingId.value = null
}

const loadData = async () => {
  loading.value = true
  try {
    await dataStore.fetchUsers()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const totalCarbonPoints = computed(() =>
  dataStore.users.reduce((sum, item) => sum + item.carbonPoints, 0)
)
const adminCount = computed(() => dataStore.users.filter((item) => item.role === 'ADMIN').length)
const bannedCount = computed(() => dataStore.users.filter((item) => item.accountStatus === '已封禁').length)
const pendingKycCount = computed(() => dataStore.users.filter((item) => item.kycReviewStatus === '待审核').length)
const newUsersCount = computed(() => {
  const now = Date.now()
  const sevenDays = 7 * 24 * 60 * 60 * 1000
  return dataStore.users.filter((item) => {
    const timestamp = new Date(item.registerAt.replace(' ', 'T')).getTime()
    return Number.isFinite(timestamp) && now - timestamp <= sevenDays
  }).length
})

const filteredUsers = computed(() => {
  const query = keyword.value.trim().toLowerCase()

  return dataStore.users.filter((item) => {
    const matchesAccount = accountFilter.value === '全部' || item.accountStatus === accountFilter.value
    const matchesKyc = kycFilter.value === '全部' || item.kycReviewStatus === kycFilter.value
    const matchesQuery =
      !query ||
      [item.name, item.phone, item.city, item.bio, item.registerAt]
        .some((field) => field.toLowerCase().includes(query))

    return matchesAccount && matchesKyc && matchesQuery
  })
})

watch(
  () => dataStore.users.map((item) => item.id),
  (ids) => {
    const validIds = new Set(ids)
    selectedIds.value = selectedIds.value.filter((id) => validIds.has(id))
  },
  { immediate: true }
)

const visibleIds = computed(() => filteredUsers.value.map((item) => item.id))
const visibleSelectedCount = computed(() =>
  visibleIds.value.filter((id) => selectedIds.value.includes(id)).length
)
const selectedCount = computed(() => selectedIds.value.length)

const allVisibleSelected = computed({
  get: () => visibleIds.value.length > 0 && visibleSelectedCount.value === visibleIds.value.length,
  set: (checked: boolean) => {
    const next = new Set(selectedIds.value)

    if (checked) {
      visibleIds.value.forEach((id) => next.add(id))
    } else {
      visibleIds.value.forEach((id) => next.delete(id))
    }

    selectedIds.value = Array.from(next)
  }
})

const isSelectionIndeterminate = computed(
  () => visibleSelectedCount.value > 0 && !allVisibleSelected.value
)

const roleLabel = (role: string) => {
  const mapping: Record<string, string> = {
    ADMIN: '管理员',
    USER: '普通用户',
    SELLER: '卖家',
    APPRAISER: '鉴定师'
  }
  return mapping[role] ?? role
}

const roleClass = (role: string) => {
  if (role === 'ADMIN') return 'admin-badge admin-badge--primary'
  if (role === 'SELLER') return 'admin-badge admin-badge--success'
  if (role === 'APPRAISER') return 'admin-badge admin-badge--warning'
  return 'admin-badge admin-badge--neutral'
}

const accountClass = (status: AdminUserAccountStatus) =>
  status === '正常' ? 'admin-badge admin-badge--success' : 'admin-badge admin-badge--neutral'

const kycClass = (status: AdminKycReviewStatus) => {
  if (status === '已通过') return 'admin-badge admin-badge--success'
  if (status === '待审核') return 'admin-badge admin-badge--warning'
  return 'admin-badge admin-badge--neutral'
}

const previewText = (text: string) => (text && text.length > 44 ? `${text.slice(0, 44)}...` : text || '未填写个人简介')

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (id: string) => {
  const current = dataStore.users.find((item) => item.id === id)
  if (!current) return

  editingId.value = id
  Object.assign(form, JSON.parse(JSON.stringify(current)))
  dialogVisible.value = true
}

const closeDialog = () => {
  dialogVisible.value = false
  resetForm()
}

const submitForm = async () => {
  if (!form.name.trim() || !form.phone.trim()) {
    alert('请填写用户姓名和手机号。')
    return
  }

  const payload: AdminUserDraft = {
    ...form,
    name: form.name.trim(),
    phone: form.phone.trim(),
    city: form.city.trim(),
    bio: form.bio.trim(),
    avatar: form.avatar.trim(),
    password: form.password.trim() || (editingId.value ? '' : 'Test@123'),
    carbonPoints: Number(form.carbonPoints),
    likedGoodsIds: form.likedGoodsIds ?? []
  }

  try {
    if (editingId.value) {
      await dataStore.updateUser(editingId.value, payload)
    } else {
      await dataStore.addUser(payload)
    }
    closeDialog()
  } catch (error) {
    alert((error as Error).message)
  }
}

const approveKyc = async (id: string) => dataStore.approveUserKyc(id)
const rejectKyc = async (id: string) => dataStore.rejectUserKyc(id)
const toggleBan = async (id: string) => dataStore.toggleUserBan(id)

const clearSelection = () => {
  selectedIds.value = []
}

const toggleSelection = (id: string, checked: string | number | boolean) => {
  if (checked) {
    selectedIds.value = Array.from(new Set([...selectedIds.value, id]))
    return
  }

  selectedIds.value = selectedIds.value.filter((item) => item !== id)
}

const approveSelectedKyc = async () => {
  if (!selectedIds.value.length) {
    alert('请先勾选要通过 KYC 的用户。')
    return
  }

  await dataStore.approveUsersKycBatch(selectedIds.value)
  clearSelection()
}

const rejectSelectedKyc = async () => {
  if (!selectedIds.value.length) {
    alert('请先勾选要驳回 KYC 的用户。')
    return
  }

  await dataStore.rejectUsersKycBatch(selectedIds.value)
  clearSelection()
}

const banSelectedUsers = async () => {
  if (!selectedIds.value.length) {
    alert('请先勾选要封禁的用户。')
    return
  }

  await dataStore.setUsersBanStatus(selectedIds.value, true)
  clearSelection()
}

const unbanSelectedUsers = async () => {
  if (!selectedIds.value.length) {
    alert('请先勾选要解封的用户。')
    return
  }

  await dataStore.setUsersBanStatus(selectedIds.value, false)
  clearSelection()
}

const removeUser = async (id: string) => {
  const current = dataStore.users.find((item) => item.id === id)
  if (!current) return

  if (!confirm(`确定删除用户“${current.name}”吗？`)) {
    return
  }

  await dataStore.deleteUser(id)
  selectedIds.value = selectedIds.value.filter((item) => item !== id)
}
</script>

<template>
  <section class="admin-page">
    <section class="admin-page__hero">
      <div class="admin-page__hero-text">
        <span class="admin-page__eyebrow">User Center</span>
        <h2>用户管理</h2>
        <p>对齐 PRD 的后台用户管理页，补上 KYC 审核、账户封禁/解封和用户搜索筛选能力。</p>
        <div class="admin-page__meta">
          <span class="admin-pill">待审核 KYC {{ pendingKycCount }}</span>
          <span class="admin-pill">已封禁 {{ bannedCount }}</span>
          <span class="admin-pill">近 7 日新增 {{ newUsersCount }}</span>
        </div>
      </div>

      <div class="users-spotlight">
        <span class="users-spotlight__tag">用户概览</span>
        <strong>{{ totalCarbonPoints }}</strong>
        <p>累计碳积分</p>
        <span>管理员 {{ adminCount }} 人</span>
      </div>
    </section>

    <div class="admin-metric-grid users-metrics">
      <article class="admin-metric-card">
        <span>用户总数</span>
        <strong>{{ dataStore.users.length }}</strong>
        <small>后台可管理全部账户</small>
      </article>
      <article class="admin-metric-card">
        <span>待审核 KYC</span>
        <strong>{{ pendingKycCount }}</strong>
        <small>待人工审核实名认证</small>
      </article>
      <article class="admin-metric-card">
        <span>已封禁账户</span>
        <strong>{{ bannedCount }}</strong>
        <small>支持一键恢复正常</small>
      </article>
      <article class="admin-metric-card">
        <span>近 7 日新增</span>
        <strong>{{ newUsersCount }}</strong>
        <small>观察拉新趋势</small>
      </article>
    </div>

    <section class="admin-section-card">
      <div class="admin-section__head">
        <div>
          <h3 class="admin-section__title">用户列表</h3>
          <p class="admin-section__desc">支持搜索、增删改查、KYC 审核和封禁/解封。</p>
        </div>
      </div>

      <div class="admin-toolbar">
        <div class="admin-toolbar__group">
          <input v-model="keyword" class="admin-search" placeholder="搜索昵称、手机号、城市、简介或注册时间" />
          <select v-model="accountFilter" class="admin-select users-toolbar__select">
            <option value="全部">全部账户状态</option>
            <option v-for="item in accountStatusOptions" :key="item" :value="item">{{ item }}</option>
          </select>
          <select v-model="kycFilter" class="admin-select users-toolbar__select">
            <option value="全部">全部 KYC 状态</option>
            <option v-for="item in kycReviewOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </div>
        <div class="admin-toolbar__group">
          <span class="admin-pill">已选择 {{ selectedCount }} 位</span>
          <button class="admin-button" type="button" @click="approveSelectedKyc">批量通过 KYC</button>
          <button class="admin-button admin-button--danger" type="button" @click="rejectSelectedKyc">批量驳回 KYC</button>
          <button class="admin-button admin-button--danger" type="button" @click="banSelectedUsers">批量封禁</button>
          <button class="admin-button" type="button" @click="unbanSelectedUsers">批量解封</button>
          <button class="admin-button admin-button--primary" type="button" @click="openCreate">新增用户</button>
        </div>
      </div>

      <div v-if="filteredUsers.length" class="users-review-table">
        <div class="users-review-table__head">
          <label class="users-review-table__checkbox">
            <el-checkbox v-model="allVisibleSelected" :indeterminate="isSelectionIndeterminate" />
            <span>全选</span>
          </label>
          <span>用户信息</span>
          <span>角色</span>
          <span>KYC 状态</span>
          <span>账户状态</span>
          <span>注册时间</span>
          <span>最近活跃</span>
          <span>操作</span>
        </div>

        <article
          v-for="item in filteredUsers"
          :key="item.id"
          class="users-review-table__row"
          :class="{ 'users-review-table__row--selected': selectedIds.includes(item.id) }"
        >
          <div class="users-review-table__checkbox users-review-table__checkbox--cell">
            <el-checkbox
              :model-value="selectedIds.includes(item.id)"
              @change="(checked) => toggleSelection(item.id, checked)"
            />
          </div>

          <div class="users-review-table__profile">
            <img :src="item.avatar" :alt="item.name" class="users-review-table__avatar" />
            <div>
              <strong>{{ item.name }}</strong>
              <p>{{ item.phone }}</p>
              <small>{{ previewText(item.bio) }}</small>
            </div>
          </div>
          <span :class="roleClass(item.role)">{{ roleLabel(item.role) }}</span>
          <span :class="kycClass(item.kycReviewStatus)">{{ item.kycReviewStatus }}</span>
          <span :class="accountClass(item.accountStatus)">{{ item.accountStatus }}</span>
          <div class="users-review-table__meta">
            <strong>{{ item.registerAt }}</strong>
            <span>{{ item.city }}</span>
          </div>
          <div class="users-review-table__meta">
            <strong>{{ item.lastActiveAt }}</strong>
            <span>积分 {{ item.carbonPoints }}</span>
          </div>
          <div class="admin-actions">
            <button class="admin-action-button" type="button" @click="openEdit(item.id)">编辑</button>
            <button v-if="item.kycReviewStatus !== '已通过'" class="admin-action-button" type="button" @click="approveKyc(item.id)">KYC通过</button>
            <button v-if="item.kycReviewStatus !== '已驳回'" class="admin-action-button" type="button" @click="rejectKyc(item.id)">驳回</button>
            <button class="admin-action-button" type="button" @click="toggleBan(item.id)">{{ item.accountStatus === '已封禁' ? '解封' : '封禁' }}</button>
            <button class="admin-action-button admin-action-button--danger" type="button" @click="removeUser(item.id)">删除</button>
          </div>
        </article>
      </div>
      <p v-else class="admin-empty">暂无符合条件的用户</p>
    </section>

    <AdminDialog
      v-model="dialogVisible"
      :title="editingId ? '编辑用户' : '新增用户'"
      description="保存后会立即更新后台用户资料与审核状态。"
      width="920px"
    >
      <form class="admin-form" @submit.prevent="submitForm">
        <div class="admin-form-grid">
          <div class="admin-field">
            <span>用户姓名</span>
            <input v-model="form.name" class="admin-input" placeholder="请输入用户姓名" />
          </div>
          <div class="admin-field">
            <span>手机号</span>
            <input v-model="form.phone" class="admin-input" placeholder="请输入手机号" />
          </div>
          <div class="admin-field">
            <span>登录密码</span>
            <input v-model="form.password" class="admin-input" placeholder="请输入登录密码" />
          </div>
          <div class="admin-field">
            <span>城市</span>
            <input v-model="form.city" class="admin-input" placeholder="请输入城市" />
          </div>
          <div class="admin-field">
            <span>用户角色</span>
            <select v-model="form.role" class="admin-select">
              <option v-for="item in roleOptions" :key="item" :value="item">{{ roleLabel(item) }}</option>
            </select>
          </div>
          <div class="admin-field">
            <span>认证等级</span>
            <select v-model="form.kycLevel" class="admin-select">
              <option v-for="item in kycOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="admin-field">
            <span>KYC 审核状态</span>
            <select v-model="form.kycReviewStatus" class="admin-select">
              <option v-for="item in kycReviewOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="admin-field">
            <span>账户状态</span>
            <select v-model="form.accountStatus" class="admin-select">
              <option v-for="item in accountStatusOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="admin-field">
            <span>碳积分</span>
            <input v-model.number="form.carbonPoints" class="admin-input" type="number" min="0" />
          </div>
          <div class="admin-field">
            <span>头像地址</span>
            <input v-model="form.avatar" class="admin-input" placeholder="可填写头像 URL，不填则使用默认头像" />
          </div>
          <div class="admin-field">
            <span>注册时间</span>
            <input v-model="form.registerAt" class="admin-input" placeholder="如 2026-03-24 10:00" />
          </div>
          <div class="admin-field">
            <span>最近活跃</span>
            <input v-model="form.lastActiveAt" class="admin-input" placeholder="如 2026-03-24 18:00" />
          </div>
          <div class="admin-field admin-field--full">
            <span>个人简介</span>
            <textarea v-model="form.bio" class="admin-textarea" placeholder="请输入用户简介"></textarea>
          </div>
        </div>
      </form>

      <template #footer>
        <button class="admin-button" type="button" @click="closeDialog">取消</button>
        <button class="admin-button admin-button--primary" type="button" @click="submitForm">保存用户</button>
      </template>
    </AdminDialog>
  </section>
</template>

<style scoped>
.users-spotlight {
  width: 320px;
  padding: 24px;
  border-radius: 28px;
  background: linear-gradient(160deg, #0f172a 0%, #2563eb 55%, #22c55e 100%);
  color: #fff;
  display: grid;
  gap: 10px;
}

.users-spotlight__tag {
  width: fit-content;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.users-spotlight strong {
  font-size: 40px;
  line-height: 1.1;
}

.users-spotlight p,
.users-spotlight span {
  margin: 0;
  color: rgba(255, 255, 255, 0.84);
}

.users-toolbar__select {
  min-width: 160px;
}

.users-metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.users-review-table {
  display: grid;
}

.users-review-table__head,
.users-review-table__row {
  display: grid;
  grid-template-columns: 78px 1.9fr 0.8fr 0.8fr 0.8fr 1fr 1fr 1.2fr;
  gap: 18px;
  align-items: center;
}

.users-review-table__head {
  min-height: 54px;
  padding: 0 12px 12px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.users-review-table__row {
  min-height: 118px;
  padding: 18px 12px;
  border-top: 1px solid rgba(226, 232, 240, 0.95);
}

.users-review-table__row--selected {
  background: rgba(239, 246, 255, 0.68);
}

.users-review-table__checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #334155;
  font-weight: 700;
}

.users-review-table__checkbox--cell {
  justify-content: center;
}

.users-review-table__profile {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.users-review-table__avatar {
  width: 74px;
  height: 74px;
  border-radius: 24px;
  object-fit: cover;
}

.users-review-table__profile strong,
.users-review-table__profile p,
.users-review-table__profile small,
.users-review-table__meta strong,
.users-review-table__meta span {
  display: block;
  margin: 0;
}

.users-review-table__profile strong,
.users-review-table__meta strong {
  color: #0f172a;
}

.users-review-table__profile p,
.users-review-table__profile small,
.users-review-table__meta span {
  margin-top: 6px;
  color: #64748b;
  line-height: 1.6;
}
</style>
