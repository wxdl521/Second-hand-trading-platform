<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getProfile } from '@/api/auth'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth.store'

const store = useAppStore()
const authStore = useAuthStore()

const form = reactive({
  realName: '',
  idNumber: ''
})

const submitting = ref(false)

onMounted(async () => {
  await getProfile()
})

const isVerified = computed(() => store.currentUser?.kycLevel === 'L3')

const kycBenefits = [
  '完成实名认证后，可直接发布高价值商品并提升信任度。',
  '订单成交和鉴定预约会优先展示认证身份。',
  '当前版本提交后会同步更新认证等级，可继续前往发布、交易与鉴定流程。'
]

const onSubmit = async () => {
  if (!form.realName || !form.idNumber) {
    alert('请填写真实姓名和证件号码')
    return
  }

  submitting.value = true

  try {
    await authStore.submitKyc({
      realName: form.realName,
      idNumber: form.idNumber
    })
    alert('实名认证已通过，当前为 L3 认证用户')
  } catch (error) {
    alert((error as Error).message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section v-if="store.currentUser" class="profile-page profile-page--compact">
    <div class="section-head">
      <div>
        <span class="eyebrow">实名认证</span>
        <h1>完善认证信息，提升账户信任等级</h1>
        <p>个人中心、商品详情和订单页都会展示当前 KYC 等级，认证完成后自动升级为 L3。</p>
      </div>
      <RouterLink class="ghost-btn" to="/profile/settings">返回设置</RouterLink>
    </div>

    <div class="two-column">
      <section class="panel">
        <div class="section-head section-head--compact">
          <div>
            <span class="eyebrow">当前状态</span>
            <h2>KYC {{ store.currentUser.kycLevel }}</h2>
          </div>
        </div>

        <div v-if="isVerified" class="kyc-success">
          <strong>认证已完成</strong>
          <p>你当前已经是 L3 认证用户，可以继续发布闲置、预约鉴定或查看订单中心。</p>
          <div class="action-row">
            <RouterLink class="primary-btn" to="/goods/publish">去发布闲置</RouterLink>
            <RouterLink class="ghost-btn" to="/order/list">查看订单</RouterLink>
          </div>
        </div>

        <form v-else class="form-grid" @submit.prevent="onSubmit">
          <label>
            <span>真实姓名</span>
            <input v-model.trim="form.realName" placeholder="请输入真实姓名" />
          </label>
          <label>
            <span>证件号码</span>
            <input v-model.trim="form.idNumber" placeholder="请输入身份证号或证件号" />
          </label>
          <button class="primary-btn primary-btn--full" type="submit" :disabled="submitting">
            {{ submitting ? '审核中...' : '提交实名认证' }}
          </button>
        </form>
      </section>

      <section class="panel">
        <div class="section-head section-head--compact">
          <div>
            <span class="eyebrow">认证权益</span>
            <h2>为什么要完成实名认证</h2>
          </div>
        </div>

        <ul class="kyc-benefits">
          <li v-for="item in kycBenefits" :key="item">{{ item }}</li>
        </ul>
      </section>
    </div>
  </section>
</template>

<style scoped>
.kyc-success {
  display: grid;
  gap: 14px;
}

.kyc-success strong {
  color: #0f172a;
  font-size: 24px;
}

.kyc-success p {
  margin: 0;
  color: #475569;
  line-height: 1.8;
}

.kyc-benefits {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.9;
}
</style>
