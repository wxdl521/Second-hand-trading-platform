import { computed } from 'vue'
import { defineStore } from 'pinia'
import * as carbonApi from '@/api/carbon'
import { useAppStore } from '@/stores/app'

export const useCarbonStore = defineStore('carbon', () => {
  const appStore = useAppStore()
  const balance = computed(() => appStore.carbonBalance)
  const level = computed(() => appStore.carbonLevel)
  const monthCarbonSavedKg = computed(() => appStore.monthCarbonSavedKg)
  const records = computed(() => appStore.carbonRecords)
  const summary = computed(() => appStore.carbonSummary)

  const refreshSummary = () => carbonApi.getCarbonSummary()
  const refreshBalance = () => carbonApi.getCarbonBalance()
  const refreshRecords = () => carbonApi.listCarbonRecords()

  return {
    balance,
    level,
    monthCarbonSavedKg,
    records,
    summary,
    refreshSummary,
    refreshBalance,
    refreshRecords
  }
})
