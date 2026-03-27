import { defineStore } from 'pinia'
import {
  approveAdminGoods,
  approveAdminUserKyc,
  banAdminUser,
  createAdminCategory,
  createAdminGoods,
  createAdminOrder,
  createAdminUser,
  deleteAdminCategory,
  deleteAdminGoods,
  deleteAdminOrder,
  deleteAdminUser,
  listAdminCategories,
  listAdminGoods,
  listAdminOrders,
  listAdminUsers,
  rejectAdminGoods,
  rejectAdminUserKyc,
  unbanAdminUser,
  updateAdminCategory,
  updateAdminGoods,
  updateAdminOrder,
  updateAdminUser
} from '@admin/api/admin'
import type {
  AdminCategoryDraft,
  AdminCategoryItem,
  AdminDatasetSnapshot,
  AdminGoodsDraft,
  AdminGoodsItem,
  AdminOrderDraft,
  AdminOrderItem,
  AdminUserDraft,
  AdminUserItem
} from '@admin/types'
import { createDatasetSnapshot, mergeCategories, writeAdminDataset } from '@admin/utils/dataset'

const formatOrderTime = (value?: string) => {
  if (!value) {
    return new Date().toLocaleString('zh-CN', { hour12: false })
  }

  if (/^\d{4}-\d{2}-\d{2}T/.test(value)) {
    return value.replace('T', ' ').slice(0, 16)
  }

  return value
}

const createUncategorizedCategory = (): AdminCategoryItem => ({
  id: `category-uncategorized-${Date.now()}`,
  name: '未分类',
  description: '承接待整理或暂未归类的商品，方便管理员后续调整。',
  featuredBrands: [],
  coverImage: ''
})

export const useAdminDataStore = defineStore('admin-data', {
  state: () => ({
    initialized: false,
    goods: [] as AdminGoodsItem[],
    orders: [] as AdminOrderItem[],
    users: [] as AdminUserItem[],
    categories: [] as AdminCategoryItem[]
  }),
  actions: {
    applySnapshot(snapshot: AdminDatasetSnapshot) {
      this.goods = snapshot.goods
      this.orders = snapshot.orders
      this.users = snapshot.users
      this.categories = snapshot.categories
      this.initialized = true
    },

    persist() {
      writeAdminDataset({
        goods: this.goods,
        orders: this.orders,
        users: this.users,
        categories: this.categories
      })
    },

    syncCategories() {
      this.categories = mergeCategories(this.goods, this.categories)
    },

    ensureUncategorizedCategory() {
      if (this.categories.some((item) => item.name === '未分类')) {
        return
      }

      this.categories = mergeCategories(this.goods, [...this.categories, createUncategorizedCategory()])
    },

    async initializeData() {
      if (this.initialized) {
        return {
          goods: this.goods,
          orders: this.orders,
          users: this.users,
          categories: this.categories
        }
      }

      const [goods, orders, users, categories] = await Promise.all([
        listAdminGoods(),
        listAdminOrders(),
        listAdminUsers(),
        listAdminCategories()
      ])

      const snapshot = createDatasetSnapshot({ goods, orders, users, categories })
      this.applySnapshot(snapshot)
      this.persist()
      return snapshot
    },

    async fetchGoods() {
      await this.initializeData()
      return this.goods
    },

    async fetchOrders() {
      await this.initializeData()
      return this.orders
    },

    async fetchUsers() {
      await this.initializeData()
      return this.users
    },

    async fetchCategories() {
      await this.initializeData()
      return this.categories
    },

    async fetchAll() {
      return this.initializeData()
    },

    async addGoods(payload: AdminGoodsDraft) {
      const item = await createAdminGoods(payload)
      this.goods.unshift(item)
      this.syncCategories()
      this.persist()
      return item
    },

    async updateGoods(id: string, payload: AdminGoodsDraft) {
      const updated = await updateAdminGoods(id, payload)
      this.goods = this.goods.map((item) => (item.id === id ? updated : item))
      this.syncCategories()
      this.persist()
    },

    async approveGoods(id: string, reviewNote = '审核通过，允许上架流转。') {
      const updated = await approveAdminGoods(id, reviewNote)
      this.goods = this.goods.map((item) => (item.id === id ? updated : item))
      this.persist()
    },

    async approveGoodsBatch(ids: string[], reviewNote = '批量审核通过，允许上架流转。') {
      for (const id of ids) {
        await this.approveGoods(id, reviewNote)
      }
    },

    async approveAllPendingGoods() {
      const pendingIds = this.goods.filter((item) => item.auditStatus === '待审核').map((item) => item.id)
      await this.approveGoodsBatch(pendingIds, '批量审核通过，允许上架流转。')
    },

    async rejectGoods(id: string, reviewNote: string) {
      const updated = await rejectAdminGoods(id, reviewNote)
      this.goods = this.goods.map((item) => (item.id === id ? updated : item))
      this.persist()
    },

    async rejectGoodsBatch(ids: string[], reviewNote: string) {
      for (const id of ids) {
        await this.rejectGoods(id, reviewNote)
      }
    },

    async deleteGoods(id: string) {
      await deleteAdminGoods(id)
      this.goods = this.goods.filter((item) => item.id !== id)
      this.orders = this.orders.filter((item) => item.goodsId !== id)
      this.users = this.users.map((item) => ({
        ...item,
        likedGoodsIds: item.likedGoodsIds.filter((goodsId) => goodsId !== id)
      }))
      this.syncCategories()
      this.persist()
    },

    async addUser(payload: AdminUserDraft) {
      const item = await createAdminUser(payload)
      this.users.unshift(item)
      this.persist()
      return item
    },

    async updateUser(id: string, payload: AdminUserDraft) {
      const updated = await updateAdminUser(id, payload)
      this.users = this.users.map((item) => (item.id === id ? updated : item))
      this.persist()
    },

    async approveUserKyc(id: string) {
      const updated = await approveAdminUserKyc(id)
      this.users = this.users.map((item) => (item.id === id ? updated : item))
      this.persist()
    },

    async approveUsersKycBatch(ids: string[]) {
      for (const id of ids) {
        await this.approveUserKyc(id)
      }
    },

    async rejectUserKyc(id: string) {
      const updated = await rejectAdminUserKyc(id)
      this.users = this.users.map((item) => (item.id === id ? updated : item))
      this.persist()
    },

    async rejectUsersKycBatch(ids: string[]) {
      for (const id of ids) {
        await this.rejectUserKyc(id)
      }
    },

    async toggleUserBan(id: string) {
      const current = this.users.find((item) => item.id === id)
      if (!current) {
        return
      }

      await this.setUsersBanStatus([id], current.accountStatus !== '已封禁')
    },

    async setUsersBanStatus(ids: string[], banned: boolean) {
      for (const id of ids) {
        const updated = banned ? await banAdminUser(id) : await unbanAdminUser(id)
        this.users = this.users.map((item) => (item.id === id ? updated : item))
      }
      this.persist()
    },

    async deleteUser(id: string) {
      await deleteAdminUser(id)
      this.users = this.users.filter((item) => item.id !== id)
      this.persist()
    },

    async addOrder(payload: AdminOrderDraft) {
      const item = await createAdminOrder({ ...payload, createdAt: formatOrderTime(payload.createdAt) })
      this.orders.unshift(item)
      this.persist()
      return item
    },

    async updateOrder(id: string, payload: AdminOrderDraft) {
      const updated = await updateAdminOrder(id, { ...payload, createdAt: formatOrderTime(payload.createdAt) })
      this.orders = this.orders.map((item) => (item.id === id ? updated : item))
      this.persist()
    },

    async deleteOrder(id: string) {
      await deleteAdminOrder(id)
      this.orders = this.orders.filter((item) => item.id !== id)
      this.persist()
    },

    async addCategory(payload: AdminCategoryDraft) {
      const item = await createAdminCategory(payload)
      this.categories.push(item)
      this.syncCategories()
      this.persist()
      return item
    },

    async updateCategory(id: string, payload: AdminCategoryDraft) {
      const current = this.categories.find((item) => item.id === id)
      if (!current) {
        return
      }

      const updated = await updateAdminCategory(id, payload)
      if (current.name !== payload.name) {
        this.goods = this.goods.map((item) =>
          item.category === current.name ? { ...item, category: payload.name } : item
        )
      }
      this.categories = this.categories.map((item) => (item.id === id ? updated : item))
      this.syncCategories()
      this.persist()
    },

    async deleteCategory(id: string) {
      const current = this.categories.find((item) => item.id === id)
      if (!current) {
        return
      }

      await deleteAdminCategory(id)
      this.goods = this.goods.map((item) =>
        item.category === current.name ? { ...item, category: '未分类' } : item
      )
      this.categories = this.categories.filter((item) => item.id !== id)
      this.ensureUncategorizedCategory()
      this.syncCategories()
      this.persist()
    }
  }
})
