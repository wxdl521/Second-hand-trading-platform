import { defineStore } from 'pinia'
import {
  categories,
  demoUsers,
  seedAppointments,
  seedCarbonRecords,
  seedGoods,
  seedOrders
} from '@/data/mock'
import type {
  AppraiseAppointment,
  CarbonRecord,
  CarbonSummary,
  EstimateInput,
  EstimateResult,
  GoodsItem,
  OrderItem,
  UserProfile,
  UserRole
} from '@/types'

const STORAGE_KEY = 'syxs-local-state'

const roleByPhone: Record<string, UserRole> = {
  '13800000001': 'USER',
  '13800000002': 'SELLER',
  '13800000003': 'APPRAISER',
  '13800000099': 'ADMIN'
}

const normalizeUser = (user: UserProfile): UserProfile => ({
  ...user,
  role: user.role ?? roleByPhone[user.phone] ?? 'USER'
})

const mergeSeedAccounts = (accounts: UserProfile[]) => {
  const normalized = accounts.map(normalizeUser)

  demoUsers.forEach((seed) => {
    if (!normalized.some((item) => item.phone === seed.phone)) {
      normalized.push(seed)
    }
  })

  return normalized
}

const KG_PATTERN = /(\d+(?:\.\d+)?)kg/i

const extractCarbonKg = (record: CarbonRecord) => {
  const matched = record.description.match(KG_PATTERN)
  return matched ? Number(matched[1]) : Number((record.points / 15).toFixed(2))
}

const resolveCarbonLevel = (balance: number) => {
  if (balance >= 5000) {
    return 'Lv.5 绿色合伙人'
  }
  if (balance >= 3500) {
    return 'Lv.4 碳账户先锋'
  }
  if (balance >= 2000) {
    return 'Lv.3 循环达人'
  }
  if (balance >= 1000) {
    return 'Lv.2 低碳买家'
  }
  return 'Lv.1 绿色新手'
}

interface AppState {
  currentUser: UserProfile | null
  authToken: string | null
  refreshToken: string | null
  otpCodes: Record<string, string>
  accounts: UserProfile[]
  goods: GoodsItem[]
  orders: OrderItem[]
  carbonRecords: CarbonRecord[]
  carbonSummary: CarbonSummary | null
  appointments: AppraiseAppointment[]
  lastEstimate: EstimateResult | null
}

const buildInitialState = (): AppState => ({
  currentUser: null,
  authToken: null,
  refreshToken: null,
  otpCodes: {},
  accounts: mergeSeedAccounts(demoUsers),
  goods: seedGoods,
  orders: seedOrders,
  carbonRecords: seedCarbonRecords,
  carbonSummary: null,
  appointments: seedAppointments,
  lastEstimate: null
})

export const useAppStore = defineStore('app', {
  state: (): AppState => buildInitialState(),
  getters: {
    isLoggedIn: (state) => Boolean(state.currentUser),
    isAdmin: (state) => state.currentUser?.role === 'ADMIN',
    featuredGoods: (state) => state.goods.slice(0, 6),
    favoriteGoods: (state) =>
      state.currentUser
        ? state.goods.filter((item) => state.currentUser?.likedGoodsIds.includes(item.id))
        : [],
    carbonBalance: (state) => state.carbonSummary?.balance ?? state.currentUser?.carbonPoints ?? 0,
    carbonLevel: (state) =>
      state.carbonSummary?.level ?? resolveCarbonLevel(state.currentUser?.carbonPoints ?? 0),
    monthCarbonSavedKg: (state) => {
      if (state.carbonSummary) {
        return state.carbonSummary.monthCarbonSavedKg
      }

      const currentMonth = new Date().toISOString().slice(0, 7)
      return Number(
        state.carbonRecords
          .filter((item) => item.type === '收入' && item.date.startsWith(currentMonth))
          .reduce((sum, item) => sum + extractCarbonKg(item), 0)
          .toFixed(2)
      )
    }
  },
  actions: {
    hydrate() {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (!saved) {
        this.persist()
        return
      }

      const parsed = JSON.parse(saved) as AppState
      const accounts = mergeSeedAccounts(parsed.accounts ?? [])
      this.$patch({
        ...parsed,
        currentUser: null,
        authToken: null,
        refreshToken: null,
        carbonSummary: null,
        accounts
      })
      this.persist()
    },
    persist() {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          currentUser: null,
          authToken: null,
          refreshToken: null,
          otpCodes: this.otpCodes,
          accounts: this.accounts,
          goods: this.goods,
          orders: this.orders,
          carbonRecords: this.carbonRecords,
          carbonSummary: null,
          appointments: this.appointments,
          lastEstimate: this.lastEstimate
        })
      )
    },
    login(phone: string, password: string) {
      const matched = this.accounts.find((item) => item.phone === phone && item.password === password)
      if (!matched) {
        throw new Error('手机号或密码不正确')
      }

      this.currentUser = { ...matched }
      this.authToken = `local-token-${matched.phone}`
      this.refreshToken = `local-refresh-${matched.phone}`
      this.carbonSummary = null
      this.persist()
    },
    register(name: string, phone: string, password: string) {
      if (this.accounts.some((item) => item.phone === phone)) {
        throw new Error('该手机号已注册')
      }

      const newUser: UserProfile = {
        id: `user-${Date.now()}`,
        name,
        phone,
        password,
        avatar: demoUsers[0].avatar,
        city: '未设置',
        bio: '欢迎来到尚有新生，开启你的绿色循环之旅。',
        role: 'USER',
        kycLevel: 'L1',
        carbonPoints: 520,
        likedGoodsIds: []
      }

      this.accounts = [newUser, ...this.accounts]
      this.currentUser = { ...newUser }
      this.authToken = `local-token-${newUser.phone}`
      this.refreshToken = `local-refresh-${newUser.phone}`
      this.carbonSummary = null
      this.persist()
    },
    sendOtp(phone: string) {
      if (!phone) {
        throw new Error('请先输入手机号')
      }

      const otpCode = '123456'
      this.otpCodes = {
        ...this.otpCodes,
        [phone]: otpCode
      }
      console.info(`[Local OTP] ${phone}: ${otpCode}`)
      this.persist()
    },
    loginWithOtp(phone: string, otpCode: string) {
      const matched = this.accounts.find((item) => item.phone === phone)
      if (!matched) {
        throw new Error('该手机号尚未注册')
      }
      if (!this.otpCodes[phone] || this.otpCodes[phone] !== otpCode) {
        throw new Error('验证码不正确')
      }

      const { [phone]: _, ...restOtpCodes } = this.otpCodes
      this.otpCodes = restOtpCodes
      this.currentUser = { ...matched }
      this.authToken = `local-token-${matched.phone}`
      this.refreshToken = `local-refresh-${matched.phone}`
      this.carbonSummary = null
      this.persist()
    },
    registerWithOtp(name: string, phone: string, otpCode: string, password: string) {
      if (this.accounts.some((item) => item.phone === phone)) {
        throw new Error('该手机号已注册')
      }
      if (!this.otpCodes[phone] || this.otpCodes[phone] !== otpCode) {
        throw new Error('验证码不正确')
      }

      const newUser: UserProfile = {
        id: `user-${Date.now()}`,
        name,
        phone,
        password,
        avatar: demoUsers[0].avatar,
        city: '未设置',
        bio: '欢迎来到尚有新生，开启你的绿色循环之旅。',
        role: 'USER',
        kycLevel: 'L1',
        carbonPoints: 520,
        likedGoodsIds: []
      }

      const { [phone]: _, ...restOtpCodes } = this.otpCodes
      this.otpCodes = restOtpCodes
      this.accounts = [newUser, ...this.accounts]
      this.currentUser = { ...newUser }
      this.authToken = `local-token-${newUser.phone}`
      this.refreshToken = `local-refresh-${newUser.phone}`
      this.carbonSummary = null
      this.persist()
    },
    logout() {
      this.currentUser = null
      this.authToken = null
      this.refreshToken = null
      this.carbonSummary = null
      this.persist()
    },
    setSession(user: UserProfile | null, token: string | null, refreshToken: string | null = null) {
      const previousPhone = this.currentUser?.phone ?? null
      const normalizedUser = user ? normalizeUser(user) : null
      this.currentUser = normalizedUser ? { ...normalizedUser } : null
      this.authToken = token
      this.refreshToken = refreshToken
      if (!normalizedUser || previousPhone !== normalizedUser.phone) {
        this.carbonSummary = null
      }
      if (normalizedUser) {
        const exists = this.accounts.some((item) => item.phone === normalizedUser.phone)
        if (!exists) {
          this.accounts = mergeSeedAccounts([normalizedUser, ...this.accounts])
        } else {
          this.accounts = mergeSeedAccounts(
            this.accounts.map((item) =>
              item.phone === normalizedUser.phone ? { ...normalizedUser } : item
            )
          )
        }
        if (this.carbonSummary && previousPhone === normalizedUser.phone) {
          this.syncCurrentUserCarbonPoints(this.carbonSummary.balance)
        }
      }
      this.persist()
    },
    setGoods(goods: GoodsItem[]) {
      this.goods = goods
      this.persist()
    },
    upsertGoods(goods: GoodsItem) {
      const exists = this.goods.some((item) => item.id === goods.id)
      this.goods = exists
        ? this.goods.map((item) => (item.id === goods.id ? goods : item))
        : [goods, ...this.goods]
      this.persist()
    },
    setOrders(orders: OrderItem[]) {
      this.orders = orders
      this.persist()
    },
    upsertOrder(order: OrderItem) {
      const exists = this.orders.some((item) => item.id === order.id)
      this.orders = exists
        ? this.orders.map((item) => (item.id === order.id ? order : item))
        : [order, ...this.orders]
      this.persist()
    },
    syncCurrentUserCarbonPoints(points: number) {
      if (!this.currentUser) {
        return
      }

      this.currentUser = {
        ...this.currentUser,
        carbonPoints: points
      }
      this.accounts = this.accounts.map((item) =>
        item.id === this.currentUser?.id ? { ...this.currentUser } : item
      )
    },
    setCarbonSummary(summary: CarbonSummary | null) {
      this.carbonSummary = summary
      if (summary) {
        this.syncCurrentUserCarbonPoints(summary.balance)
      }
      this.persist()
    },
    applyCarbonDelta(delta: number) {
      if (!delta || !this.currentUser) {
        return
      }

      const nextBalance = Math.max(0, (this.carbonSummary?.balance ?? this.currentUser.carbonPoints ?? 0) + delta)
      if (this.carbonSummary) {
        this.carbonSummary = {
          ...this.carbonSummary,
          balance: nextBalance,
          level: resolveCarbonLevel(nextBalance)
        }
      }
      this.syncCurrentUserCarbonPoints(nextBalance)
      this.persist()
    },
    setCarbonRecords(records: CarbonRecord[]) {
      this.carbonRecords = records
      this.persist()
    },
    setAppointments(appointments: AppraiseAppointment[]) {
      this.appointments = appointments
      this.persist()
    },
    setLastEstimate(result: EstimateResult | null) {
      this.lastEstimate = result
      this.persist()
    },
    toggleFavorite(goodsId: string) {
      if (!this.currentUser) {
        throw new Error('请先登录后再收藏')
      }

      const liked = new Set(this.currentUser.likedGoodsIds)
      if (liked.has(goodsId)) {
        liked.delete(goodsId)
      } else {
        liked.add(goodsId)
      }

      this.currentUser.likedGoodsIds = Array.from(liked)
      this.accounts = this.accounts.map((item) =>
        item.id === this.currentUser?.id ? { ...this.currentUser } : item
      )
      this.persist()
    },
    publishGoods(payload: {
      title: string
      category: string
      brand: string
      condition: string
      price: number
      description: string
      story: string
      tags: string[]
      imageUrls?: string[]
    }) {
      if (!this.currentUser) {
        throw new Error('请先登录')
      }

      const heroImage =
        payload.imageUrls?.[0] ??
        seedGoods[Math.floor(Math.random() * seedGoods.length)]?.heroImage ??
        demoUsers[0].avatar
      const gallery =
        payload.imageUrls?.length
          ? payload.imageUrls
          : seedGoods[Math.floor(Math.random() * seedGoods.length)]?.gallery ?? [heroImage]
      const item: GoodsItem = {
        id: `goods-${Date.now()}`,
        title: payload.title,
        category: payload.category,
        brand: payload.brand,
        condition: payload.condition,
        price: payload.price,
        originalPrice: Math.round(payload.price * 1.7),
        carbonSavedKg: Math.max(8, Math.round(payload.price / 560)),
        aiPrice: Math.round(payload.price * 0.98),
        city: this.currentUser.city,
        sellerName: this.currentUser.name,
        sellerLevel: this.currentUser.kycLevel,
        story: payload.story,
        description: payload.description,
        status: '在售中',
        tags: payload.tags,
        heroImage,
        gallery,
        createdAt: new Date().toISOString().slice(0, 10)
      }

      this.goods = [item, ...this.goods]
      this.carbonRecords = [
        {
          id: `carbon-${Date.now()}`,
          title: '发布高价值闲置',
          points: 80,
          type: '收入',
          date: new Date().toISOString().slice(0, 10),
          description: `成功发布 ${payload.title}，平台已计入循环贡献。`
        },
        ...this.carbonRecords
      ]
      this.applyCarbonDelta(80)
      return item
    },
    createEstimate(input: EstimateInput) {
      const categoryFactor = (categories.indexOf(input.category) + 1 || 2) * 1800
      const conditionFactor =
        {
          '99 新': 1.08,
          '95 新': 1,
          '90 新': 0.88,
          '85 新': 0.78
        }[input.condition] ?? 0.84

      const yearsFactor = Math.max(0.7, 1 - input.yearsUsed * 0.05)
      const rarityFactor = 0.9 + input.rarity * 0.08
      const brandFactor = Math.max(1, input.brand.length) * 120
      const imageFactor = 1 + Math.min(input.imageUrls?.length ?? 0, 4) * 0.02
      const descriptionFactor = input.description?.trim() ? 1.03 : 1

      const price = Math.round(
        categoryFactor * conditionFactor * yearsFactor * rarityFactor * imageFactor * descriptionFactor + brandFactor
      )
      const result: EstimateResult = {
        price,
        low: Math.round(price * 0.92),
        high: Math.round(price * 1.08),
        confidence: Math.min(
          97,
          Math.max(76, 78 + input.rarity * 3 - input.yearsUsed + Math.min(input.imageUrls?.length ?? 0, 4) * 2)
        ),
        carbonSavedKg: Math.max(6, Math.round(price / 780)),
        summary: `${input.title} 更适合定位在 ${input.category} 的中高意向成交段，建议结合图片和附件信息继续校准。`,
        tips: [
          (input.imageUrls?.length ?? 0) < 2
            ? '补充正反面与细节近景，可显著提升 AI 置信度。'
            : '继续补充附件、编号或保卡特写，有助于缩小价格区间。',
          input.description?.trim()
            ? '描述信息较完整，建议同步突出保卡、发票和维修记录。'
            : '补充购入时间、使用频率和附件信息，AI 会给出更稳定的区间。',
          '支持继续发起视频连线鉴定，提升成交效率。'
        ]
      }

      this.lastEstimate = result
      this.persist()
      return result
    },
    reserveAppraise(payload: { goodsTitle: string; mode: AppraiseAppointment['mode']; date: string; note: string }) {
      const item: AppraiseAppointment = {
        id: `appraise-${Date.now()}`,
        goodsTitle: payload.goodsTitle,
        mode: payload.mode,
        date: payload.date,
        note: payload.note,
        status: payload.mode === 'AI 快速鉴定' ? '已预约' : '待确认'
      }

      this.appointments = [item, ...this.appointments]
      this.persist()
      return item
    },
    createOrder(goodsId: string) {
      if (!this.currentUser) {
        throw new Error('请先登录')
      }

      const goods = this.goods.find((item) => item.id === goodsId)
      if (!goods) {
        throw new Error('商品不存在')
      }

      const item: OrderItem = {
        id: `order-${Date.now()}`,
        goodsId,
        amount: goods.price,
        buyerName: this.currentUser.name,
        status: '待付款',
        createdAt: new Date().toLocaleString('zh-CN', { hour12: false }),
        timeline: [{ label: '订单创建', time: new Date().toLocaleString('zh-CN', { hour12: false }) }]
      }

      this.orders = [item, ...this.orders]
      this.persist()
      return item
    },
    payOrder(orderId: string) {
      this.orders = this.orders.map((item) =>
        item.id === orderId
          ? {
              ...item,
              status: '待发货',
              timeline: [
                ...item.timeline,
                { label: '模拟支付成功', time: new Date().toLocaleString('zh-CN', { hour12: false }) }
              ]
            }
          : item
      )
      this.persist()
    },
    changePassword(payload: { currentPassword: string; nextPassword: string }) {
      if (!this.currentUser) {
        throw new Error('请先登录')
      }

      if (this.currentUser.password && this.currentUser.password !== payload.currentPassword) {
        throw new Error('当前密码不正确')
      }

      this.currentUser = {
        ...this.currentUser,
        password: payload.nextPassword
      }
      this.accounts = this.accounts.map((item) =>
        item.id === this.currentUser?.id ? { ...this.currentUser } : item
      )
      this.persist()
    },
    submitKyc(payload: { realName: string; idNumber: string }) {
      if (!this.currentUser) {
        throw new Error('请先登录')
      }

      if (!payload.realName || !payload.idNumber) {
        throw new Error('请完整填写实名认证信息')
      }

      this.currentUser = {
        ...this.currentUser,
        kycLevel: 'L3'
      }
      this.accounts = this.accounts.map((item) =>
        item.id === this.currentUser?.id ? { ...this.currentUser } : item
      )
      this.persist()
      return this.currentUser
    },
    updateProfile(payload: { name: string; city: string; bio: string; avatar?: string }) {
      if (!this.currentUser) {
        throw new Error('请先登录')
      }

      this.currentUser = {
        ...this.currentUser,
        name: payload.name,
        city: payload.city,
        bio: payload.bio,
        avatar: payload.avatar ?? this.currentUser.avatar
      }
      this.accounts = this.accounts.map((item) =>
        item.id === this.currentUser?.id ? { ...this.currentUser } : item
      )
      this.persist()
    }
  }
})
