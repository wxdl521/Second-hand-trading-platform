# 尚有新生 | Local Dev Edition

基于 `D:\BaiduNetdiskDownload\尚有新生_PRD_v2.0_本地开发版_中文.md` 搭建的本地开发版仓库，目录结构按 PRD 对齐为：

```text
shangyouxinsheng/
├─ backend/                # Spring Boot 3 单体后端
├─ frontend/               # Vue 3 + TypeScript 前端
├─ docker-compose.yml      # MySQL 8
├─ .env.example            # 环境变量模板
└─ README.md               # 启动说明
```

## 当前实现

- `backend/`：已补齐 PRD 中的配置层、公共层、`user / goods / ai / order / appraise / carbon / file` 模块骨架，以及 Flyway 迁移脚本
- `frontend/`：已按 PRD 增补 `api/`、`router/guards.ts`、`stores/`、`components/layout|goods|ai|carbon/`、`styles/variables.scss`、`styles/global.scss`
- 交互层已可体验：首页、商品列表/详情、发布闲置、AI 估价、订单中心、碳账户、鉴定预约、个人中心

## 本地运行

### 1. 启动 MySQL

```bash
cp .env.example .env
docker compose up -d
```

### 2. 启动后端

```bash
cd backend
mvn -gs .\maven-settings.xml -s .\maven-settings.xml spring-boot:run
```

说明：

- `backend/maven-settings.xml` 用来绕过当前机器损坏的全局 Maven `settings.xml`
- `Spring Boot 3` 需要 `JDK 17+`

### 3. 启动用户前台

```bash
cd frontend
npm install
npm run dev
```

访问地址：`http://localhost:5173`

### 4. 启动独立管理员站

```bash
cd frontend
npm run dev:admin
```

访问地址：`http://localhost:5174`

说明：

- 管理员站为独立入口，不走用户前台路由
- 建议部署时单独绑定后台域名，例如 `admin.your-domain.com`

## 构建校验

- 用户前台：`npm run build` 已通过
- 管理员站：`npm run build:admin` 已通过
- 后端：当前机器 Maven 全局配置损坏且本地仓库存在锁文件权限问题，未完成编译校验

## 演示账号

- 用户：`13800000001 / Test@123`
- 卖家：`13800000002 / Test@123`
- 管理员站：`13800000099 / Admin@123`
