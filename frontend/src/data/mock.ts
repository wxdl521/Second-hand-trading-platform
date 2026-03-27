import type {
  AppraiseAppointment,
  CarbonRecord,
  GoodsItem,
  OrderItem,
  UserProfile
} from '@/types'

const makeCover = (title: string, colorA: string, colorB: string) => {
  const svg = `
  <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="900" viewBox="0 0 1200 900">
    <defs>
      <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="${colorA}" />
        <stop offset="100%" stop-color="${colorB}" />
      </linearGradient>
    </defs>
    <rect width="1200" height="900" fill="url(#g)" rx="48" />
    <circle cx="970" cy="180" r="120" fill="rgba(255,255,255,0.18)" />
    <circle cx="180" cy="720" r="160" fill="rgba(255,255,255,0.1)" />
    <text x="90" y="420" font-size="74" font-family="Arial, Microsoft Yahei" fill="white" font-weight="700">${title}</text>
    <text x="90" y="510" font-size="34" font-family="Arial, Microsoft Yahei" fill="rgba(255,255,255,0.88)">尚有新生 · AI 循环交易</text>
  </svg>
  `

  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

const makeBanner = (colorA: string, colorB: string, accent: string) => {
  const svg = `
  <svg xmlns="http://www.w3.org/2000/svg" width="1600" height="820" viewBox="0 0 1600 820">
    <defs>
      <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="${colorA}" />
        <stop offset="100%" stop-color="${colorB}" />
      </linearGradient>
      <radialGradient id="glow" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="rgba(255,255,255,0.58)" />
        <stop offset="100%" stop-color="rgba(255,255,255,0)" />
      </radialGradient>
    </defs>
    <rect width="1600" height="820" fill="url(#bg)" rx="64" />
    <ellipse cx="290" cy="540" rx="240" ry="220" fill="rgba(255,255,255,0.12)" />
    <ellipse cx="1260" cy="210" rx="180" ry="170" fill="rgba(255,255,255,0.16)" />
    <path d="M0 650 C220 540 360 520 560 610 C760 700 940 730 1200 610 C1380 525 1490 500 1600 540 L1600 820 L0 820 Z" fill="${accent}" opacity="0.52" />
    <circle cx="1160" cy="470" r="190" fill="url(#glow)" opacity="0.35" />
    <circle cx="520" cy="200" r="120" fill="rgba(255,255,255,0.14)" />
  </svg>
  `

  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

export const categories = ['奢品箱包', '珠宝腕表', '数码设备', '艺术收藏', '家居好物']

export const demoUsers: UserProfile[] = [
  {
    id: 'user-001',
    name: '林知夏',
    phone: '13800000001',
    password: 'Test@123',
    avatar: makeCover('林知夏', '#1b6b3a', '#14512b'),
    city: '上海',
    bio: '关注循环时尚与高质感生活方式。',
    role: 'USER',
    kycLevel: 'L1',
    carbonPoints: 1280,
    likedGoodsIds: ['goods-001', 'goods-004']
  },
  {
    id: 'user-002',
    name: '宋屿',
    phone: '13800000002',
    password: 'Test@123',
    avatar: makeCover('宋屿', '#f97316', '#ea580c'),
    city: '杭州',
    bio: '专注高端数码与奢侈品循环交易。',
    role: 'SELLER',
    kycLevel: 'L3',
    carbonPoints: 2560,
    likedGoodsIds: ['goods-002']
  },
  {
    id: 'user-099',
    name: '平台管理员',
    phone: '13800000099',
    password: 'Admin@123',
    avatar: makeCover('ADMIN', '#203225', '#8a6416'),
    city: '上海',
    bio: '负责平台审核、用户运营与内容巡检。',
    role: 'ADMIN',
    kycLevel: 'L3',
    carbonPoints: 3000,
    likedGoodsIds: []
  }
]

export const homeBanners = [
  { id: 'banner-001', goodsId: 'goods-008', image: makeBanner('#203225', '#14512b', '#c9972a') },
  { id: 'banner-002', goodsId: 'goods-007', image: makeBanner('#111827', '#f59e0b', '#fde68a') },
  { id: 'banner-003', goodsId: 'goods-011', image: makeBanner('#7f1d1d', '#fb7185', '#fda4af') },
  { id: 'banner-004', goodsId: 'goods-009', image: makeBanner('#14532d', '#22c55e', '#bbf7d0') }
]

export const seedGoods: GoodsItem[] = [
  {
    id: 'goods-001',
    title: '香奈儿 Classic Flap 中号链条包',
    category: '奢品箱包',
    brand: 'Chanel',
    condition: '95 新',
    price: 27800,
    originalPrice: 58999,
    carbonSavedKg: 36,
    aiPrice: 28100,
    city: '上海',
    sellerName: '苏黎',
    sellerLevel: 'L3',
    story: '陪伴过两次晚宴，保养得很好，五金成色优秀。',
    description: '附原盒、防尘袋、购入小票，支持视频验货与线下复核。',
    status: '在售中',
    tags: ['经典款', '全套附件', '保值款'],
    heroImage: makeCover('Classic Flap', '#203225', '#8a6416'),
    gallery: [
      makeCover('正面成色', '#2c4732', '#c9972a'),
      makeCover('链条细节', '#203225', '#b88924'),
      makeCover('内里结构', '#4a3520', '#c9972a')
    ],
    mockCertified: true,
    createdAt: '2026-03-19'
  },
  {
    id: 'goods-002',
    title: '劳力士 Datejust 36 蓝盘腕表',
    category: '珠宝腕表',
    brand: 'Rolex',
    condition: '98 新',
    price: 56800,
    originalPrice: 75999,
    carbonSavedKg: 28,
    aiPrice: 55200,
    city: '深圳',
    sellerName: '周衡',
    sellerLevel: 'L3',
    story: '2024 年购入，仅日常通勤佩戴，走时稳定。',
    description: '支持平台寄售与鉴定预约，附保卡和原装表节。',
    status: '在售中',
    tags: ['蓝盘', '保卡齐全', '支持寄售'],
    heroImage: makeCover('Rolex Datejust', '#14512b', '#c9972a'),
    gallery: [
      makeCover('腕表正面', '#1b6b3a', '#d7b55b'),
      makeCover('表扣细节', '#2c4732', '#c9972a'),
      makeCover('夜光显示', '#14512b', '#e3c97f')
    ],
    mockCertified: true,
    createdAt: '2026-03-18'
  },
  {
    id: 'goods-003',
    title: '徕卡 Q3 全画幅相机套装',
    category: '数码设备',
    brand: 'Leica',
    condition: '92 新',
    price: 31800,
    originalPrice: 46888,
    carbonSavedKg: 42,
    aiPrice: 32600,
    city: '北京',
    sellerName: '江沅',
    sellerLevel: 'L2',
    story: '主要用于旅行拍摄，快门次数很低。',
    description: '带手柄与备用电池，平台 AI 估价波动较小。',
    status: '在售中',
    tags: ['低快门', '摄影好物', '配件齐'],
    heroImage: makeCover('Leica Q3', '#b91c1c', '#ef4444'),
    gallery: [
      makeCover('机身正面', '#7f1d1d', '#dc2626'),
      makeCover('机顶拨盘', '#991b1b', '#f87171'),
      makeCover('随附配件', '#450a0a', '#fb7185')
    ],
    createdAt: '2026-03-17'
  },
  {
    id: 'goods-004',
    title: 'Hermès 小飞马丝巾 90cm 限定色',
    category: '艺术收藏',
    brand: 'Hermès',
    condition: '99 新',
    price: 5680,
    originalPrice: 8999,
    carbonSavedKg: 11,
    aiPrice: 5520,
    city: '成都',
    sellerName: '陆栀',
    sellerLevel: 'L2',
    story: '收藏为主，未正式使用。',
    description: '适合入门收藏，平台支持 AI 图像比对。',
    status: '在售中',
    tags: ['限定色', '收藏级', '礼盒装'],
    heroImage: makeCover('Hermès Scarf', '#ea580c', '#fb923c'),
    gallery: [
      makeCover('整体铺陈', '#9a3412', '#f97316'),
      makeCover('边角细节', '#c2410c', '#fdba74'),
      makeCover('礼盒包装', '#7c2d12', '#f59e0b')
    ],
    mockCertified: true,
    createdAt: '2026-03-16'
  },
  {
    id: 'goods-005',
    title: 'B&O Beoplay A9 家居音响',
    category: '家居好物',
    brand: 'Bang & Olufsen',
    condition: '90 新',
    price: 12999,
    originalPrice: 21999,
    carbonSavedKg: 54,
    aiPrice: 12450,
    city: '广州',
    sellerName: '齐远',
    sellerLevel: 'L1',
    story: '换新装修风格后闲置，希望找到懂它的主人。',
    description: '适合客厅陈设，支持到店试听与配送。',
    status: '在售中',
    tags: ['客厅氛围', '北欧设计', '支持试听'],
    heroImage: makeCover('Beoplay A9', '#3f4d2f', '#c9972a'),
    gallery: [
      makeCover('客厅摆拍', '#4a5b38', '#d6bb67'),
      makeCover('支架细节', '#5e6d43', '#e3cf8e'),
      makeCover('音响界面', '#314228', '#b88924')
    ],
    createdAt: '2026-03-20'
  },
  {
    id: 'goods-006',
    title: 'Dior Book Tote 中号刺绣款',
    category: '奢品箱包',
    brand: 'Dior',
    condition: '93 新',
    price: 16900,
    originalPrice: 30999,
    carbonSavedKg: 31,
    aiPrice: 16300,
    city: '南京',
    sellerName: '顾柠',
    sellerLevel: 'L2',
    story: '出差使用过几次，包型很好。',
    description: '包身洁净，刺绣完整，适合通勤与旅行。',
    status: '已预订',
    tags: ['通勤', '刺绣款', '附防尘袋'],
    heroImage: makeCover('Dior Book Tote', '#6b3f24', '#c9972a'),
    gallery: [
      makeCover('正面展示', '#7a4d2f', '#d4b057'),
      makeCover('肩带细节', '#8b613f', '#e6cd88'),
      makeCover('内里状态', '#5a3821', '#b88924')
    ],
    createdAt: '2026-03-15'
  },
  {
    id: 'goods-007',
    title: 'Celine Triomphe 亮面牛皮手袋',
    category: '奢品箱包',
    brand: 'Celine',
    condition: '96 新',
    price: 19800,
    originalPrice: 33999,
    carbonSavedKg: 29,
    aiPrice: 19150,
    city: '苏州',
    sellerName: '温澜',
    sellerLevel: 'L2',
    story: '保养细致，金扣状态很好，适合日常通勤。',
    description: '附防尘袋与购入凭证，支持平台复核验货。',
    status: '在售中',
    tags: ['通勤经典', '亮面牛皮', '成色佳'],
    heroImage: makeCover('Celine Triomphe', '#111827', '#f59e0b'),
    gallery: [
      makeCover('正面展示', '#1f2937', '#fbbf24'),
      makeCover('金扣细节', '#374151', '#fcd34d'),
      makeCover('肩带状态', '#0f172a', '#f59e0b')
    ],
    createdAt: '2026-03-24'
  },
  {
    id: 'goods-008',
    title: 'MacBook Pro 14 M3 Pro 深空黑',
    category: '数码设备',
    brand: 'Apple',
    condition: '98 新',
    price: 14200,
    originalPrice: 18999,
    carbonSavedKg: 23,
    aiPrice: 14500,
    city: '杭州',
    sellerName: '沈聿',
    sellerLevel: 'L3',
    story: '仅作为备用机，电池循环次数很低。',
    description: '附原装充电器和包装盒，支持当面验机。',
    status: '在售中',
    tags: ['M3 Pro', '低循环', '办公创作'],
    heroImage: makeCover('MacBook Pro', '#020617', '#475569'),
    gallery: [
      makeCover('机身外观', '#111827', '#64748b'),
      makeCover('屏幕展示', '#1e293b', '#94a3b8'),
      makeCover('配件齐全', '#334155', '#cbd5e1')
    ],
    createdAt: '2026-03-23'
  },
  {
    id: 'goods-009',
    title: 'Van Cleef 幸运四叶草项链',
    category: '珠宝腕表',
    brand: 'Van Cleef & Arpels',
    condition: '97 新',
    price: 12600,
    originalPrice: 17800,
    carbonSavedKg: 14,
    aiPrice: 12350,
    city: '上海',
    sellerName: '安禾',
    sellerLevel: 'L2',
    story: '日常搭配频率不高，保存盒和证书都在。',
    description: '适合送礼或自用，支持视频细节展示。',
    status: '在售中',
    tags: ['证书齐全', '轻奢首饰', '送礼推荐'],
    heroImage: makeCover('Van Cleef', '#14532d', '#4ade80'),
    gallery: [
      makeCover('项链细节', '#166534', '#86efac'),
      makeCover('链扣状态', '#15803d', '#bbf7d0'),
      makeCover('包装展示', '#14532d', '#22c55e')
    ],
    createdAt: '2026-03-22'
  },
  {
    id: 'goods-010',
    title: 'Kartell Masters 设计师餐椅',
    category: '家居好物',
    brand: 'Kartell',
    condition: '91 新',
    price: 2280,
    originalPrice: 3999,
    carbonSavedKg: 18,
    aiPrice: 2190,
    city: '宁波',
    sellerName: '陶青',
    sellerLevel: 'L1',
    story: '换了餐厅风格后闲置，椅面无明显划痕。',
    description: '可同城自提，也支持平台预约配送。',
    status: '在售中',
    tags: ['设计家具', '同城自提', '客餐厅'],
    heroImage: makeCover('Kartell Chair', '#3f5b40', '#c9972a'),
    gallery: [
      makeCover('正面摆拍', '#58744f', '#d8bf70'),
      makeCover('椅背细节', '#44593e', '#e2cd90'),
      makeCover('家居搭配', '#32452f', '#b88924')
    ],
    createdAt: '2026-03-21'
  },
  {
    id: 'goods-011',
    title: 'Sony A7R V 全画幅微单机身',
    category: '数码设备',
    brand: 'Sony',
    condition: '94 新',
    price: 19600,
    originalPrice: 25999,
    carbonSavedKg: 26,
    aiPrice: 19280,
    city: '武汉',
    sellerName: '顾北',
    sellerLevel: 'L2',
    story: '升级器材后出手，快门控制得比较少。',
    description: '机身外观完整，附双电池和肩带。',
    status: '在售中',
    tags: ['高像素', '视频创作', '双电池'],
    heroImage: makeCover('Sony A7R V', '#7c2d12', '#fb7185'),
    gallery: [
      makeCover('机身展示', '#881337', '#f472b6'),
      makeCover('按键细节', '#9f1239', '#fda4af'),
      makeCover('包装附件', '#500724', '#fb7185')
    ],
    createdAt: '2026-03-24'
  },
  {
    id: 'goods-012',
    title: 'LOEWE Puzzle 小号拼色手袋',
    category: '奢品箱包',
    brand: 'LOEWE',
    condition: '95 新',
    price: 15800,
    originalPrice: 26999,
    carbonSavedKg: 25,
    aiPrice: 15450,
    city: '厦门',
    sellerName: '裴宁',
    sellerLevel: 'L2',
    story: '配色特别，通勤和旅行都很好搭。',
    description: '附肩带和购物票据，支持平台保价寄售。',
    status: '在售中',
    tags: ['拼色设计', '小号包', '附肩带'],
    heroImage: makeCover('LOEWE Puzzle', '#5d4328', '#c9972a'),
    gallery: [
      makeCover('包型展示', '#6d5131', '#d8bb66'),
      makeCover('边角细节', '#7b603c', '#e7cf8e'),
      makeCover('内里状态', '#52361f', '#b88924')
    ],
    createdAt: '2026-03-20'
  }
]

export const seedOrders: OrderItem[] = [
  {
    id: 'order-001',
    goodsId: 'goods-006',
    amount: 16900,
    buyerName: '林知夏',
    status: '待付款',
    createdAt: '2026-03-22 10:30',
    timeline: [
      { label: '订单创建', time: '2026-03-22 10:30' },
      { label: '卖家确认库存', time: '2026-03-22 10:42' }
    ]
  },
  {
    id: 'order-002',
    goodsId: 'goods-003',
    amount: 31800,
    buyerName: '林知夏',
    status: '运输中',
    createdAt: '2026-03-18 09:18',
    timeline: [
      { label: '订单创建', time: '2026-03-18 09:18' },
      { label: '完成支付', time: '2026-03-18 09:22' },
      { label: '卖家发货', time: '2026-03-19 15:08' },
      { label: '干线运输中', time: '2026-03-20 08:41' }
    ]
  }
]

export const seedCarbonRecords: CarbonRecord[] = [
  {
    id: 'carbon-001',
    title: '完成闲置交易',
    points: 180,
    type: '收入',
    date: '2026-03-20',
    description: '完成一笔高价值循环交易，累计减碳 12kg。'
  },
  {
    id: 'carbon-002',
    title: 'AI 估价任务',
    points: 45,
    type: '收入',
    date: '2026-03-21',
    description: '提交 3 次有效估价请求，获得探索积分。'
  },
  {
    id: 'carbon-003',
    title: '兑换顺丰保价券',
    points: 60,
    type: '支出',
    date: '2026-03-22',
    description: '用于高价值寄售商品的物流权益。'
  }
]

export const seedAppointments: AppraiseAppointment[] = [
  {
    id: 'appraise-001',
    goodsTitle: '香奈儿 Classic Flap 中号链条包',
    mode: '视频连线鉴定',
    date: '2026-03-25 19:30',
    note: '重点确认五金磨损与内里编码。',
    status: '已预约'
  }
]
