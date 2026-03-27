🌱

**尚有新生**

**AI驱动高端闲置品绿色流转平台**

产品需求文档 v2.0 --- 本地开发版

Spring Boot 3 + Vue 3 + MySQL 8 \| 100% 本地运行，零云服务依赖

  -----------------------------------------------------------------------
  **属性**               **内容**
  ---------------------- ------------------------------------------------
  文档版本               v2.0（本地开发版）

  基于版本               v1.0 服务器版 --- 简化为本地优先开发版

  技术栈                 Spring Boot 3（单体应用）+ Vue 3 + MySQL 8

  已移除                 云OSS / 短信服务 / Redis / ES / RocketMQ /
                         区块链 / 微服务架构

  新增                   本地文件存储 / 控制台打印验证码 / Spring Cache /
                         Ollama AI（可选）

  一键启动               docker compose up（仅启动MySQL，应用通过 mvn
                         spring-boot:run 运行）

  目标读者               个人开发者 / 小型团队 / 黑客松 / 原型验证

  文档日期               2025年
  -----------------------------------------------------------------------

**版本对比 --- v1.0 服务器版 vs v2.0 本地版**

**0. 与服务器版的差异说明**

> 💡
> 本章节是最重要的，请优先阅读。所有架构决策均由以下简化原则推导而来。

  ---------------------------------------------------------------------------------------------
  **模块**       **v1.0 服务器版**    **v2.0 本地版**                 **简化原因**
  -------------- -------------------- ------------------------------- -------------------------
  架构           Spring Cloud         Spring Boot 单体应用            本地开发只需一个JVM进程
                 微服务（7个服务）                                    

  API网关        Spring Cloud Gateway 已移除，直接访问Controller      本地无需路由复杂度

  文件存储       阿里云 OSS           本地磁盘 ./uploads/ 目录        无需任何云账号

  短信验证码     阿里云短信 API       控制台日志打印验证码            无需短信额度

  缓存           Redis 7              Spring Cache +                  无需额外启动进程
                                      Caffeine（内存缓存）            

  搜索           Elasticsearch 8      MySQL FULLTEXT 索引 + LIKE 查询 本地数据量足够用

  消息队列       RocketMQ 5           同步调用 + \@Async 注解         本地测试异步场景较少

  区块链         蚂蚁链 / FISCO BCOS  已移除，用DB字段模拟            本地无法运行联盟链

  第三方登录     微信/支付宝 OAuth    已移除，仅支持手机+验证码       OAuth需要已备案域名

  AI估价         通义千问             Ollama（本地）+ 规则引擎兜底    完全免费，支持离线
                 VL（付费API）                                        

  支付           微信支付/支付宝 SDK  模拟支付，即时成功              无需商户账号

  CI/CD          GitLab CI + ArgoCD   已移除                          本地手动运行即可

  监控           Prometheus + Grafana 仅 Spring Actuator 端点         轻量替代方案

  容器化         Kubernetes 集群      docker-compose.yml（仅MySQL）   单机部署
  ---------------------------------------------------------------------------------------------

**第一章 --- 本地技术栈**

**1. 技术选型（本地开发版）**

**1.1 后端 --- Spring Boot 3 单体应用**

  -----------------------------------------------------------------------
  **依赖组件**           **版本**   **用途说明**
  ---------------------- ---------- -------------------------------------
  Spring Boot Starter    3.2.x      REST控制器，内嵌Tomcat
  Web                               

  Spring Boot Starter    3.2.x      ORM框架，本地版替代MyBatis-Plus
  Data JPA                          

  Spring Boot Starter    3.2.x      JWT认证，RBAC权限，CORS配置
  Security                          

  Spring Boot Starter    3.2.x      注解驱动缓存
  Cache                             

  Caffeine Cache         3.x        内存缓存（替代Redis）

  jjwt (JJWT)            0.12.x     JWT令牌生成与校验

  Lombok                 latest     减少样板代码

  MapStruct              1.5.x      DTO与实体类转换

  Spring Boot Starter    3.2.x      参数校验（@NotNull等）
  Validation                        

  MySQL Connector/J      8.0.x      JDBC驱动

  Flyway                 9.x        数据库版本迁移管理

  SpringDoc OpenAPI      2.x        自动生成API文档，访问
  (Swagger UI)                      /swagger-ui.html

  Spring Boot Actuator   3.2.x      健康检查，访问 /actuator/health

  OkHttp                 4.x        HTTP客户端，用于调用Ollama AI接口

  Commons IO / Commons   latest     文件工具类
  Lang3                             
  -----------------------------------------------------------------------

**1.2 前端 --- Vue 3 + TypeScript**

  ----------------------------------------------------------------------------
  **依赖包**             **版本**   **用途说明**
  ---------------------- ---------- ------------------------------------------
  Vue 3                  3.4+       核心框架，Composition API

  Vite                   5.x        开发服务器与构建工具

  TypeScript             5.x        类型安全

  Vue Router 4           4.x        客户端路由

  Pinia                  2.x        状态管理

  Element Plus           2.x        中文友好的企业级UI组件库

  Axios                  1.x        HTTP请求，含拦截器

  ECharts 5              5.x        碳账户数据图表

  \@vueuse/core          10.x       实用组合式函数（useStorage等）

  unplugin-auto-import   latest     Vue/Element Plus自动导入
  ----------------------------------------------------------------------------

**1.3 基础设施（本地运行）**

  -------------------------------------------------------------------------------------------------
  **服务**         **启动方式**       **端口**               **说明**
  ---------------- ------------------ ---------------------- --------------------------------------
  MySQL 8          docker compose up  3306                   唯一需要Docker的服务
                   -d mysql                                  

  Spring Boot 应用 mvn                8080                   需要 JDK 17+
                   spring-boot:run                           

  Vue 开发服务器   npm run dev        5173                   代理 /api 请求到 :8080

  Ollama（可选）   ollama serve       11434                  本地AI估价；不启动则使用规则引擎兜底

  Swagger UI       自动（内置）       8080/swagger-ui.html   交互式API文档
  -------------------------------------------------------------------------------------------------

> 💡 启动全部服务：(1) docker compose up -d (2) mvn spring-boot:run (3)
> npm run dev --- 访问 http://localhost:5173

**第二章 --- 代码结构**

**2. 项目目录结构**

**2.1 仓库根目录**

> shangyouxinsheng/
>
> ├── backend/ \# Spring Boot 3 单体后端
>
> ├── frontend/ \# Vue 3 + TypeScript 前端
>
> ├── docker-compose.yml \# 仅MySQL
>
> ├── .env.example \# 环境变量模板
>
> └── README.md \# 快速启动指南

**2.2 后端结构**

> backend/
>
> ├── pom.xml
>
> └── src/main/
>
> ├── java/com/syxs/
>
> │ ├── SyxsApplication.java \# 应用启动入口 \@SpringBootApplication
>
> │ ├── config/
>
> │ │ ├── SecurityConfig.java \# JWT过滤链，CORS，公开路由配置
>
> │ │ ├── JwtConfig.java \# Token密钥及有效期配置
>
> │ │ ├── CacheConfig.java \# Caffeine缓存管理器Bean
>
> │ │ ├── FileStorageConfig.java \# 本地上传目录初始化
>
> │ │ └── OpenApiConfig.java \# Swagger/SpringDoc配置
>
> │ ├── common/
>
> │ │ ├── result/R.java \# 统一响应体 R\<T\>
>
> │ │ ├── exception/
>
> │ │ │ ├── GlobalExceptionHandler.java \# 全局异常处理
> \@RestControllerAdvice
>
> │ │ │ └── BusinessException.java
>
> │ │ ├── enums/ \# 枚举：GoodsStatus, OrderStatus, KycLevel\...
>
> │ │ └── utils/ \# 工具类：JwtUtil, FileUtil, CarbonCalcUtil
>
> │ ├── module/
>
> │ │ ├── user/
>
> │ │ │ ├── controller/UserController.java
>
> │ │ │ ├── controller/AuthController.java
>
> │ │ │ ├── service/UserService.java + impl/
>
> │ │ │ ├── repository/UserRepository.java \# JPA数据仓库
>
> │ │ │ ├── entity/User.java
>
> │ │ │ └── dto/ (LoginRequest, RegisterRequest, UserProfileDTO)
> 数据传输对象
>
> │ │ ├── goods/
>
> │ │ │ ├── controller/GoodsController.java
>
> │ │ │ ├── service/GoodsService.java + impl/
>
> │ │ │ ├── repository/GoodsRepository.java
>
> │ │ │ ├── entity/Goods.java + GoodsImage.java
>
> │ │ │ └── dto/ (GoodsCreateDTO, GoodsDetailVO, GoodsListVO)
> 数据传输对象
>
> │ │ ├── ai/
>
> │ │ │ ├── controller/AiEstimateController.java
>
> │ │ │ ├── service/AiEstimateService.java
>
> │ │ │ ├── service/OllamaClient.java \# 调用Ollama API的HTTP客户端
>
> │ │ │ └── service/RuleBasedPricingEngine.java \# 规则引擎兜底估价
>
> │ │ ├── order/
>
> │ │ │ ├── controller/OrderController.java
>
> │ │ │ ├── service/OrderService.java + impl/
>
> │ │ │ ├── repository/OrderRepository.java
>
> │ │ │ └── entity/Order.java
>
> │ │ ├── appraise/
>
> │ │ │ ├── controller/AppraiseController.java
>
> │ │ │ ├── service/AppraiseService.java + impl/
>
> │ │ │ └── entity/AppraiseOrder.java
>
> │ │ ├── carbon/
>
> │ │ │ ├── controller/CarbonController.java
>
> │ │ │ ├── service/CarbonService.java + impl/
>
> │ │ │ └── entity/ (CarbonAccount, CarbonRecord) 实体类
>
> │ │ └── file/ \# 文件模块
>
> │ │ ├── controller/FileController.java \# 文件上传与静态访问
>
> │ │ └── service/LocalFileStorageService.java
>
> └── resources/ \# 资源目录
>
> ├── application.yml \# 主配置文件
>
> ├── application-dev.yml \# 本地开发配置覆盖
>
> └── db/migration/ \# Flyway数据库迁移脚本
>
> ├── V1\_\_init_schema.sql
>
> ├── V2\_\_seed_categories.sql
>
> └── V3\_\_seed_brands.sql

**2.3 前端结构**

> frontend/
>
> ├── index.html
>
> ├── vite.config.ts \# Vite配置，代理 /api 到 localhost:8080
>
> ├── tsconfig.json
>
> ├── package.json
>
> └── src/
>
> ├── main.ts \# 应用入口，挂载Vue，注册插件
>
> ├── App.vue
>
> ├── api/ \# 各模块Axios请求封装
>
> │ ├── index.ts \# 基础URL，拦截器，Token注入
>
> │ ├── auth.ts
>
> │ ├── goods.ts
>
> │ ├── ai.ts
>
> │ ├── order.ts
>
> │ └── carbon.ts
>
> ├── router/
>
> │ ├── index.ts \# 路由定义
>
> │ └── guards.ts \# 路由守卫，无Token跳转登录页
>
> ├── stores/ \# Pinia 状态管理
>
> │ ├── auth.store.ts \# 用户信息，Token，登录/退出 action
>
> │ ├── cart.store.ts
>
> │ └── carbon.store.ts
>
> ├── components/
>
> │ ├── layout/ \# 顶栏、底栏、侧边导航组件
>
> │ ├── goods/ \# 商品卡片、图片轮播组件
>
> │ ├── ai/ \# AI估价面板、价格仪表盘组件
>
> │ └── carbon/ \# 碳贡献看板、积分卡组件
>
> ├── views/
>
> │ ├── HomeView.vue
>
> │ ├── LoginView.vue
>
> │ ├── RegisterView.vue
>
> │ ├── goods/
>
> │ │ ├── GoodsList.vue
>
> │ │ ├── GoodsDetail.vue
>
> │ │ └── GoodsPublish.vue \# 发布闲置（5步向导）
>
> │ ├── ai/EstimateView.vue
>
> │ ├── order/
>
> │ │ ├── OrderList.vue
>
> │ │ └── OrderDetail.vue
>
> │ ├── carbon/CarbonView.vue
>
> │ ├── appraise/AppraiseView.vue
>
> │ ├── profile/ProfileView.vue
>
> │ └── admin/ \# 运营后台管理页面
>
> ├── styles/
>
> │ ├── variables.scss \# 颜色与字体CSS变量
>
> │ └── global.scss
>
> └── types/ \# TypeScript 类型定义

**第三章 --- 配置文件**

**3. 关键配置文件**

**3.1 docker-compose.yml（仅MySQL）**

> \# docker-compose.yml
>
> version: \'3.8\'
>
> services:
>
> mysql:
>
> image: mysql:8.0
>
> environment:
>
> MYSQL_ROOT_PASSWORD: syxs_local_2025
>
> MYSQL_DATABASE: syxs_db
>
> MYSQL_USER: syxs
>
> MYSQL_PASSWORD: syxs123
>
> ports:
>
> \- \"3306:3306\"
>
> volumes:
>
> \- mysql_data:/var/lib/mysql
>
> volumes:
>
> mysql_data:

**3.2 application.yml（主配置）**

> \# backend/src/main/resources/application.yml
>
> spring:
>
> profiles:
>
> active: dev
>
> datasource:
>
> url:
> jdbc:mysql://localhost:3306/syxs_db?useSSL=false&characterEncoding=UTF-8
>
> username: syxs
>
> password: syxs123
>
> driver-class-name: com.mysql.cj.jdbc.Driver
>
> jpa:
>
> hibernate.ddl-auto: validate \# Flyway管理建表，JPA只做校验
>
> show-sql: true
>
> flyway:
>
> enabled: true
>
> locations: classpath:db/migration
>
> cache:
>
> type: caffeine
>
> caffeine.spec: maximumSize=1000,expireAfterWrite=300s
>
> servlet:
>
> multipart:
>
> max-file-size: 20MB
>
> max-request-size: 100MB
>
> syxs:
>
> jwt:
>
> secret: syxs-local-jwt-secret-change-in-prod-32chars
>
> access-expiry: 7200 \# 访问令牌有效期2小时（秒）
>
> refresh-expiry: 604800 \# 刷新令牌有效期7天
>
> file:
>
> upload-dir: ./uploads \# 相对于mvn运行目录
>
> serve-path: /files/\*\* \# 映射为静态资源路径
>
> otp:
>
> mode: console \# console=控制台打印验证码；sms=真实短信
>
> expiry-seconds: 300
>
> ai:
>
> provider: ollama \# ollama=本地AI \| disabled=仅用规则引擎
>
> ollama-url: http://localhost:11434
>
> ollama-model: llava \# 多模态图像理解模型
>
> carbon:
>
> points-per-yuan: 0.05 \# 每1元交易额 = 0.05积分
>
> co2-per-yuan: 0.0008 \# 每1元交易额节省碳排放(kgCO2e)

**3.3 vite.config.ts（API代理）**

> // frontend/vite.config.ts
>
> export default defineConfig({
>
> server: {
>
> proxy: {
>
> \'/api\': { target: \'http://localhost:8080\', changeOrigin: true },
>
> \'/files\': { target: \'http://localhost:8080\', changeOrigin: true },
>
> }
>
> },
>
> resolve: { alias: { \'@\': \'/src\' } }
>
> })

**第四章 --- 数据库设计（MySQL 8）**

**4. 数据库表设计**

> 💡 所有DDL由Flyway统一管理，切勿手动改表------请新建 V{n}\_\_xxx.sql
> 迁移文件。

**4.1 核心数据表清单**

  ----------------------------------------------------------------------------
  **表名**           **中文说明**   **关键字段**
  ------------------ -------------- ------------------------------------------
  t_user             用户表         id, phone, password_hash, nickname,
                                    avatar_path, kyc_level(0-3), status,
                                    created_at

  t_otp              验证码表       phone, code, expires_at, used ---
                                    本地模式控制台打印

  t_category         商品分类树     id, parent_id, name, level(1-3), sort,
                                    icon_path（分类树结构）

  t_brand            品牌库         id, name（英文名）, name_cn（中文名）,
                                    logo_path, luxury_level(1-5)

  t_goods            商品发布表     id, title, category_id, brand_id, price,
                                    status, seller_id, ai_price_min/max,
                                    condition_level, transfer_type,
                                    carbon_saved_kg

  t_goods_image      商品图片表     id, goods_id, path, sort, is_main

  t_ai_estimate      AI估价记录     id, goods_id, provider(ollama/rule),
                                    price_min/max, confidence, raw_response,
                                    created_at

  t_order            交易订单表     id, buyer_id, seller_id, goods_id,
                                    pay_price, status, created_at, paid_at,
                                    confirmed_at

  t_appraise_order   鉴定订单表     id, goods_id, type(ai/video/offline),
                                    appraiser_id, status, result(pass/fail),
                                    note, fee

  t_carbon_account   碳账户表       user_id(PK), total_points,
                                    total_carbon_kg, level(1-5)

  t_carbon_record    积分流水表     id, user_id, delta, carbon_kg,
                                    action_type, ref_id, created_at

  t_message          消息通知表     id, user_id, type, title, content,
                                    is_read, created_at

  t_favorite         收藏表         user_id, goods_id, created_at ---
                                    composite PK
  ----------------------------------------------------------------------------

**4.2 t_goods --- 商品表字段详细说明**

  -----------------------------------------------------------------------------------------
  **字段名**        **数据类型**    **约束**         **说明**
  ----------------- --------------- ---------------- --------------------------------------
  id                BIGINT          主键             本地使用自增ID或雪花算法
                                    AUTO_INCREMENT   

  title             VARCHAR(200)    NOT NULL         商品标题，AI生成后可编辑

  category_id       BIGINT          NOT NULL FK      三级分类ID

  brand_id          BIGINT          NOT NULL FK      品牌库外键

  model_name        VARCHAR(100)                     型号/系列名称

  condition_level   TINYINT         NOT NULL         成色：1=全新 2=几乎全新 3=九成新
                                                     4=八成新 5=七成以下

  accessories       JSON            DEFAULT \'\[\]\' Array of string tags

  description       TEXT                             商品描述，最多2000字

  price             DECIMAL(12,2)   NOT NULL         挂牌价格（人民币）

  ai_price_min      DECIMAL(12,2)                    AI估价下限

  ai_price_max      DECIMAL(12,2)                    AI估价上限

  seller_id         BIGINT          NOT NULL FK      卖家用户ID（关联t_user）

  status            TINYINT         NOT NULL DEFAULT 状态：0=草稿 1=待鉴定 2=鉴定中
                                    0                3=已上架 4=已售出 5=已下架

  view_count        INT             DEFAULT 0        浏览量

  favor_count       INT             DEFAULT 0        收藏数

  transfer_type     TINYINT         NOT NULL         流转方式：1=自卖 2=寄卖 3=极速回收

  carbon_saved_kg   DECIMAL(6,2)    DEFAULT 0        预估减碳量（kgCO2e）

  mock_certified    TINYINT         DEFAULT 0        本地模拟认证：1=已认证（替代区块链）

  created_at        DATETIME        NOT NULL DEFAULT 创建时间
                                    NOW()            

  updated_at        DATETIME        NOT NULL         更新时间（@PreUpdate自动维护）

  deleted           TINYINT         DEFAULT 0        逻辑删除标记
  -----------------------------------------------------------------------------------------

**第五章 --- API 接口设计**

**5. RESTful API 设计**

**5.1 接口规范**

  -----------------------------------------------------------------------
  **规范项**            **说明**
  --------------------- -------------------------------------------------
  本地Base URL          http://localhost:8080/api/v1

  认证请求头            Authorization: Bearer {accessToken}

  统一响应体            { code: 0, message: \'成功\', data: T, timestamp:
                        long }

  状态码                code=0 表示成功，非0表示业务异常

  分页参数              ?page=0&size=20（Spring Data 从0开始计页）

  金额格式              人民币小数字符串，如 \'9200.00\'

  文件路径              返回相对路径，前端拼接 http://localhost:8080

  接口文档              http://localhost:8080/swagger-ui.html
  -----------------------------------------------------------------------

**5.2 认证与用户接口**

  -------------------------------------------------------------------------------------------------
  **请求方式**   **路径**              **需要认证**   **接口说明**
  -------------- --------------------- -------------- ---------------------------------------------
  POST           /auth/otp/send        否             发送验证码------本地模式打印至控制台

  POST           /auth/register        否             手机号+验证码+密码注册

  POST           /auth/login           否             登录------返回accessToken和refreshToken

  POST           /auth/refresh         否             刷新访问令牌

  POST           /auth/logout          是             退出登录（内存黑名单使令牌失效）

  GET            /user/me              是             获取当前用户信息

  PUT            /user/me              是             修改昵称/头像

  POST           /user/kyc/submit      是             提交实名认证------本地模式1秒内自动审核通过
  -------------------------------------------------------------------------------------------------

**5.3 商品接口**

  -----------------------------------------------------------------------------------
  **请求方式**   **路径**              **需要认证**   **接口说明**
  -------------- --------------------- -------------- -------------------------------
  GET            /goods                否             商品列表------参数：category,
                                                      brand, priceMin, priceMax,
                                                      condition, keyword, sort, page,
                                                      size

  GET            /goods/{id}           否             商品详情（含图片和AI估价）

  POST           /goods                是（卖家）     创建商品草稿

  PUT            /goods/{id}           是（发布者）   修改商品（草稿或在售状态）

  DELETE         /goods/{id}           是（发布者）   软删除/下架商品

  POST           /goods/{id}/publish   是（发布者）   草稿转为待鉴定状态

  GET            /goods/my             是             我发布的商品（支持状态筛选）
  -----------------------------------------------------------------------------------

**5.4 AI估价接口**

  ----------------------------------------------------------------------------------------------------------
  **请求方式**   **路径**                       **需要认证**   **接口说明**
  -------------- ------------------------------ -------------- ---------------------------------------------
  POST           /ai/estimate/upload            是             上传图片（multipart）触发异步估价

  GET            /ai/estimate/{estimateId}      是             轮询估价结果------状态：PENDING/DONE/FAILED

  GET            /ai/estimate/goods/{goodsId}   是             获取商品最新估价记录
  ----------------------------------------------------------------------------------------------------------

> ⚠️
> 本地AI流程：若Ollama已启动且llava模型已拉取，则调用视觉推理；否则自动降级到规则引擎（品牌×成色×品类权重）。

**5.5 订单接口**

  ---------------------------------------------------------------------------------------
  **请求方式**   **路径**              **需要认证**   **接口说明**
  -------------- --------------------- -------------- -----------------------------------
  POST           /order                是（买家）     创建订单------商品状态变为锁定中

  POST           /order/{id}/pay       是（买家）     模拟支付------立即标记为已支付

  POST           /order/{id}/ship      是（卖家）     标记已发货，填写物流单号

  POST           /order/{id}/confirm   是（买家）     确认收货------触发结算+碳积分入账

  POST           /order/{id}/refund    是（买家）     申请退款------本地模式自动通过

  GET            /order/my/buy         是             我购买的订单

  GET            /order/my/sell        是             我出售的订单

  GET            /order/{id}           是             订单详情
  ---------------------------------------------------------------------------------------

**5.6 碳账户与文件接口**

  ---------------------------------------------------------------------------------------------------
  **请求方式**   **路径**              **需要认证**   **接口说明**
  -------------- --------------------- -------------- -----------------------------------------------
  GET            /carbon/account       是             碳账户总量与等级

  GET            /carbon/records       是             积分流水（分页）

  POST           /carbon/certificate   是             生成碳减排证书PNG（服务端生成，存入/uploads）

  POST           /file/upload          是             上传文件------返回相对路径

  GET            /files/{filename}     否             访问已上传的静态文件
  ---------------------------------------------------------------------------------------------------

**第六章 --- 功能模块（本地版适配说明）**

**6. 功能模块详解**

**6.1 认证模块 --- 控制台验证码模式**

本地模式下，当用户调用 POST /auth/otp/send
发送手机号时，后端会将验证码打印到控制台日志，而不是真实发送短信。开发阶段无需购买阿里云短信服务。

控制台输出示例：

> INFO \[AuthService\] ========================================
>
> INFO \[AuthService\] OTP for 138\*\*\*\*8888 : 742916 (expires 5 min)
>
> INFO \[AuthService\] ========================================
>
> 💡 切换为真实短信：将 application.yml 中 syxs.otp.mode 改为
> sms，并填写短信服务商配置即可。

**6.2 文件存储 --- 本地磁盘**

-   上传文件保存至 ./uploads/{yyyy}/{MM}/{uuid}.{ext}

-   Spring Boot 通过 /files/\*\* 路径提供静态文件访问

-   文件大小限制：单文件20MB，单次请求100MB（可配置）

-   支持格式：JPEG、PNG、WEBP、HEIC（服务端通过ImageIO转换）

-   迁移云存储：实现 CloudFileStorageService 接口（继承
    FileStorageService），无需改动Controller层

**6.3 AI估价引擎 --- Ollama + 规则引擎兜底**

**6.3.1 已启动Ollama（llava模型）时的流程**

1.  用户上传商品图片至 /ai/estimate/upload

2.  后端读取图片字节，Base64编码后携带结构化Prompt发送给 Ollama
    /api/generate

3.  Prompt引导模型识别：品牌、型号、成色，并以JSON格式返回价格区间

4.  解析响应，存入 t_ai_estimate，返回给前端展示

**6.3.2 规则引擎兜底（未启动Ollama）**

  -------------------------------------------------------------------------------
  **评估因子**     **权重**   **本地实现方式**
  ---------------- ---------- ---------------------------------------------------
  品牌奢华等级     30%        t_brand.luxury_level(1-5) 映射为价格系数

  商品成色         25%        线性比例：全新=1.0，几乎全新=0.9，九成新=0.75\...

  品类基准价       25%        初始化数据写入 t_category.avg_resale_price 字段

  市场随机波动     20%        ±10%随机噪声模拟市场价差
  -------------------------------------------------------------------------------

> 💡 仅需一次拉取：ollama pull llava（约4GB）。之后估价完全离线运行。

**6.4 支付模块 --- 模拟模式**

本地版以模拟服务替代真实支付SDK。调用 POST /order/{id}/pay
后，订单立即变为已支付状态，并触发下游结算流程（卖家余额增加+碳积分入账），无需商户账号即可完整测试交易链路。

> ⚠️ 严禁将模拟支付部署到生产环境！生产配置文件中 syxs.payment.mock
> 必须设为 false。

**6.5 区块链模块 --- 数据库字段模拟**

区块链模块替换为 t_goods 表上的 mock_certified 布尔字段。鉴定通过后
mock_certified=1。商品详情API返回 certified: true
字段，前端展示绿色认证徽标，与真实区块链流程视觉一致。

接口层完全保留------实现 ChainService 接口即可接入蚂蚁链或FISCO
BCOS，无需改动Controller和前端代码。

**6.6 绿色积分与碳账户**

碳积分模块在本地版完整可用，无任何外部依赖。积分于买家确认收货时触发计算：

> carbonSavedKg = order.payPrice \* syxs.carbon.co2-per-yuan
>
> pointsEarned = order.payPrice \* syxs.carbon.points-per-yuan

-   积分写入 t_carbon_account（汇总）和 t_carbon_record（明细流水）

-   碳减排证书在服务端生成PNG图片（使用 Java2D / Graphics2D）

-   证书存入 /uploads/certificates/ 并通过静态路径对外提供访问

**第七章 --- 中文界面设计规范**

**7. UI设计规范（中文界面）**

**7.1 设计变量（Design Tokens）**

  -------------------------------------------------------------------------------
  **变量名**          **取值**               **用途**
  ------------------- ---------------------- ------------------------------------
  \--color-primary    #1B6B3A                主色调------按钮、激活导航、H1标题

  \--color-accent     #2E9E5B                辅助色------H2标题、边框、高亮

  \--color-gold       #C9972A                金色------高端徽标、价格标签

  \--color-bg-light   #E8F5EC                浅绿背景------卡片底色、提示块

  \--font-cn          PingFang SC, 微软雅黑, 全部中文文本
                      sans-serif             

  \--font-mono        JetBrains Mono,        代码、ID显示
                      Courier New, monospace 

  \--radius-card      12px                   商品卡片、弹窗

  \--radius-btn       8px                    所有按钮

  \--shadow-card      0 2px 12px             默认卡片投影
                      rgba(0,0,0,0.08)       
  -------------------------------------------------------------------------------

**7.2 页面清单**

  ---------------------------------------------------------------------------------------------------------------------
  **页面名称**    **路由**           **组件文件**            **核心元素**
  --------------- ------------------ ----------------------- ----------------------------------------------------------
  首页            /                  HomeView.vue            Banner轮播、品类导航格、推荐商品瀑布流、碳贡献实时计数器

  商品列表        /goods             GoodsList.vue           筛选侧边栏、商品卡片、价格区间滑块、排序栏

  商品详情        /goods/:id         GoodsDetail.vue         图片轮播、AI估价徽标、成色详情、悬浮购买底栏

  AI智能估价      /estimate          EstimateView.vue        拖拽上传区、加载动效、价格仪表盘、估价分项图表

  发布闲置        /goods/publish     GoodsPublish.vue        五步向导：图片/信息/定价/鉴定/确认

  我的订单        /order/list        OrderList.vue           状态Tab分类、时间轴、物流追踪

  订单详情        /order/:id         OrderDetail.vue         完整订单信息、支付操作、确认收货

  绿色碳账户      /carbon            CarbonView.vue          环形图、积分流水、证书下载

  鉴定预约        /appraise          AppraiseView.vue        鉴定方式选择（AI/视频/线下）、预约时间选择

  个人中心        /profile           ProfileView.vue         头像上传、KYC认证状态、修改密码、我的收藏

  登录            /login             LoginView.vue           手机号+验证码表单、记住登录

  注册            /register          RegisterView.vue        手机号+验证码+密码+用户协议

  后台-商品审核   /admin/goods       admin/GoodsReview.vue   待审核队列、通过/驳回、批量操作

  后台-用户管理   /admin/users       admin/UserList.vue      用户搜索、KYC审核、封禁/解封

  后台-数据看板   /admin/dashboard   admin/Dashboard.vue     GMV趋势图、新用户数、碳贡献汇总、订单漏斗
  ---------------------------------------------------------------------------------------------------------------------

**7.3 中文文案规范**

  ----------------------------------------------------------------------------
  **使用场景**          **推荐文案**
  --------------------- ------------------------------------------------------
  商品空状态            还没有符合条件的宝贝，换个关键词试试？

  加载中                正在加载中，稍等一下\...

  AI估价中              AI正在分析您的商品，大约需要30秒\...

  发布成功              发布成功！您的闲置品已提交鉴定，审核通过后将自动上架
                        🎉

  确认收货              收货确认成功！绿色积分已入账，感谢您的绿色消费 🌱

  模拟支付完成          支付成功（本地测试模式）

  验证码提示（本地）    验证码已发送（本地开发模式：请查看后端控制台日志）

  KYC自动通过           认证信息已提交，本地模式将在1秒内自动审核通过

  证书已生成            碳减排证书已生成，点击下载保存
  ----------------------------------------------------------------------------

**第八章 --- 双端适配方案（方案A：响应式设计）**

**8. 双端适配设计规范**

> 💡 采用方案A：一套 Vue 3 代码，通过 Element Plus 响应式栅格 + CSS 媒体查询自动适配 PC 与移动端，开发成本低，适合本地版原型阶段及后续迭代。

**8.1 断点系统（Breakpoints）**

全局遵循以下五档断点，与 Element Plus 栅格系统完全对齐：

  -----------------------------------------------------------------------
  **断点名**   **屏幕宽度**     **典型设备**         **布局策略**
  ------------ ---------------- -------------------- --------------------
  xs           < 768px          手机竖屏             单列，全宽

  sm           768px ~ 991px    手机横屏 / 小平板    单列或2列

  md           992px ~ 1199px   平板                 2列 / 侧边栏折叠

  lg           1200px ~ 1919px  PC 笔记本            3列 / 侧边栏展开

  xl           ≥ 1920px         PC 大屏              4列 / 宽内容区
  -----------------------------------------------------------------------

在 `styles/variables.scss` 中统一定义：

```scss
$bp-xs: 768px;
$bp-sm: 992px;
$bp-md: 1200px;
$bp-lg: 1920px;

@mixin mobile { @media (max-width: #{$bp-xs - 1px}) { @content; } }
@mixin tablet { @media (min-width: $bp-xs) and (max-width: #{$bp-sm - 1px}) { @content; } }
@mixin desktop { @media (min-width: $bp-sm) { @content; } }
```

**8.2 各页面双端布局策略**

  -----------------------------------------------------------------------
  **页面**         **PC端布局**                  **移动端布局**
  ---------------- ----------------------------- -------------------------
  首页             顶部导航栏 + 左侧品类 +        底部Tab导航 + 竖向滚动
                   右侧内容区（3列瀑布流）        单列瀑布流

  商品列表         左侧筛选抽屉（固定） +         顶部筛选栏（横向滚动） +
                   右侧3列商品卡片                单列商品卡片

  商品详情         左图右文两栏布局，             图片轮播在上，详情在下，
                   底部悬浮购买栏                 底部固定购买栏

  AI智能估价       左侧上传区 + 右侧结果区        上传区在上，结果展示在下

  发布闲置         居中五步向导（max-width        全屏五步向导，步骤条
                   860px）                        压缩为图标

  我的订单         顶部Tab + 表格式订单列表       顶部Tab + 卡片式订单列表

  绿色碳账户       左侧圆环图 + 右侧积分明细      上方圆环图 + 下方积分列表

  个人中心         左侧菜单 + 右侧内容区          底部Tab导航 + 全屏内容区

  运营后台         固定左侧边栏（240px） +        不做移动端适配（仅PC）
                   右侧内容区
  -----------------------------------------------------------------------

**8.3 导航结构双端差异**

PC端：

```
顶部导航栏（AppHeader）
├── Logo
├── 主导航：首页 / 全部商品 / AI估价 / 发布闲置
├── 搜索框（展开式）
└── 右侧：消息 / 购物车 / 个人中心（下拉菜单）
```

移动端：

```
顶部简化栏（仅 Logo + 搜索图标 + 消息图标）
底部 Tab 导航（AppTabBar）
├── 首页
├── 商品
├── 发布（中间凸起大按钮）
├── 消息
└── 我的
```

在 `App.vue` 中通过 `useBreakpoints` 动态切换：

```vue
<template>
  <AppHeader v-if="isDesktop" />
  <router-view />
  <AppTabBar v-if="!isDesktop" />
</template>

<script setup lang="ts">
import { useBreakpoints, breakpointsTailwind } from '@vueuse/core'
const bp = useBreakpoints(breakpointsTailwind)
const isDesktop = bp.greaterOrEqual('md')
</script>
```

**8.4 组件级响应式规范**

**商品卡片 GoodsCard.vue：**

```vue
<el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
  <GoodsCard />
</el-col>
```

**表单输入框：**

- PC端：`width: 480px`，左右居中
- 移动端：`width: 100%`，全宽填满

**弹窗 Dialog：**

- PC端：`width: 520px`
- 移动端：`width: 92vw`，底部抽屉式（`el-drawer` 替代 `el-dialog`）

**表格：**

- PC端：完整列展示
- 移动端：隐藏次要列（通过 `v-if="isDesktop"` 控制），保留核心信息列

**8.5 触控与交互规范（移动端专项）**

  -----------------------------------------------------------------------
  **规范项**          **要求**
  ------------------- ---------------------------------------------------
  最小点击目标        宽高不小于 44×44px（苹果 HIG 标准）

  按钮间距            相邻可点击元素间距 ≥ 8px，防误触

  上拉加载            商品列表使用 IntersectionObserver 实现无限滚动，
                      替代PC端分页器

  下拉刷新            首页、订单列表支持 touch 下拉刷新
                      （`@vueuse/core` useSwipe 实现）

  图片上传            移动端唤起系统相机或相册
                      （`<input type="file" accept="image/*" capture>`）

  长按操作            商品卡片长按弹出快捷操作菜单（收藏/分享）

  手势返回            路由切换支持左滑返回（History API + CSS transition）
  -----------------------------------------------------------------------

**8.6 性能优化（移动端专项）**

- **图片懒加载**：所有商品图片使用 `loading="lazy"` + IntersectionObserver
- **按需加载**：路由级代码分割，每个页面独立 chunk（Vite 默认支持）
- **骨架屏**：列表页、详情页在数据加载期间展示 `el-skeleton`，避免布局抖动
- **字体裁剪**：仅引入常用汉字子集（通过 `vite-plugin-fonts` 裁剪），减少字体包体积
- **Touch 事件优化**：为滚动容器添加 `touch-action: pan-y`，消除 300ms 点击延迟

**8.7 新增前端依赖**

  -----------------------------------------------------------------------
  **依赖包**              **版本**   **用途**
  ----------------------- ---------- -------------------------------------
  @vueuse/core            10.x       useBreakpoints / useSwipe /
                                     useIntersectionObserver

  sass                    1.x        SCSS 变量与 mixin 支持

  vite-plugin-compression latest     Gzip/Brotli 压缩（移动端网络优化）
  -----------------------------------------------------------------------

**8.8 测试要求**

  -----------------------------------------------------------------------
  **测试类型**      **工具**              **覆盖范围**
  ----------------- --------------------- --------------------------------
  响应式视觉测试    Chrome DevTools        xs/sm/md/lg/xl 五档断点全覆盖

  真机测试          iOS Safari /           首页、商品详情、发布流程、
                    Android Chrome         订单确认

  点击目标验证      手动测试               所有交互元素 ≥ 44px

  性能测试          Lighthouse Mobile      LCP < 3s，FID < 100ms
  -----------------------------------------------------------------------

**第九章 --- 快速启动指南**

**9. 本地开发快速启动**

**9.1 环境要求**

  --------------------------------------------------------------------------
  **工具**         **版本要求**     **安装方式**
  ---------------- ---------------- ----------------------------------------
  JDK              17 或 21（LTS）  https://adoptium.net

  Maven            3.9+             项目已内置 mvnw，无需单独安装

  Node.js          20 LTS+          https://nodejs.org

  Docker + Docker  Docker 24+       https://docs.docker.com/get-docker
  Compose                           

  Ollama（可选）   最新版           https://ollama.com --- 仅AI估价功能需要

  IDE              IntelliJ IDEA /  推荐安装 Spring Boot + Vue 插件
                   VS Code          
  --------------------------------------------------------------------------

**9.2 分步启动流程**

5.  克隆仓库： git clone https://github.com/yourorg/shangyouxinsheng.git

6.  复制环境变量： cp .env.example .env --- 本地默认配置无需修改

7.  启动MySQL： docker compose up -d --- 等待约10秒MySQL就绪

8.  启动后端： cd backend && ./mvnw spring-boot:run --- Flyway自动建表

9.  启动前端： cd frontend && npm install && npm run dev

10. 打开浏览器访问： http://localhost:5173

11. （可选）拉取AI模型： ollama pull llava --- 开启真实AI估价

12. 查看接口文档： http://localhost:8080/swagger-ui.html

> 💡 首次启动会通过Flyway
> V2、V3脚本自动初始化品类和品牌数据，无需手动执行SQL。

**9.3 默认测试账号（Flyway V4 初始化）**

  ------------------------------------------------------------------------
  **角色**      **手机号**       **密码**     **说明**
  ------------- ---------------- ------------ ----------------------------
  普通用户      13800000001      Test@123     L1实名认证，无商品

  卖家          13800000002      Test@123     L3认证，已有3件示例商品

  鉴定师        13800000003      Test@123     已开启鉴定师权限

  管理员        13800000099      Admin@123    完整后台权限
  ------------------------------------------------------------------------

**9.4 常见问题排查**

  ------------------------------------------------------------------------------------------------------
  **问题现象**             **可能原因**         **解决方法**
  ------------------------ -------------------- --------------------------------------------------------
  端口3306被占用           本地已有MySQL运行    关闭本地MySQL，或将docker-compose端口改为3307

  Flyway迁移报错           数据库状态不干净     执行 docker compose down -v 再 up -d 重置

  浏览器CORS报错           Vite代理未生效       检查 vite.config.ts 代理配置，确保前端运行在5173端口

  AI估价始终使用规则引擎   Ollama未启动         在新终端执行 ollama serve

  文件上传413错误          Tomcat默认大小限制   已在application.yml中配置------确认激活了正确的profile

  JWT频繁过期              默认2小时有效期      在dev配置中设置 syxs.jwt.access-expiry=86400（24小时）
  ------------------------------------------------------------------------------------------------------

**第十章 --- 附录**

**10. 附录**

**10.1 本地版与生产版对比**

  ---------------------------------------------------------------------------
  **对比维度**     **本地版 v2.0**            **生产版 v1.0（参考）**
  ---------------- -------------------------- -------------------------------
  架构             Spring Boot 单体           Spring Cloud 微服务

  文件存储         本地磁盘 ./uploads/        阿里云 OSS

  短信验证码       控制台打印                 阿里云短信

  缓存             Caffeine 内存缓存          Redis 7 集群

  搜索             MySQL FULLTEXT + LIKE      Elasticsearch 8

  异步事件         \@Async + Spring Events    RocketMQ 5

  AI估价           Ollama（本地）或规则引擎   通义千问 VL API

  支付             模拟即时成功               微信支付 + 支付宝 SDK

  区块链存证       DB字段（mock_certified）   蚂蚁链 / FISCO BCOS

  第三方登录       不支持                     微信/支付宝 OAuth 2.0

  配置管理         application-dev.yml        配置中心（Nacos）

  服务发现         无（单进程）               Nacos 服务注册中心

  部署方式         mvn spring-boot:run        K8s + Helm Charts

  监控             Spring Actuator /health    Prometheus + Grafana
  ---------------------------------------------------------------------------

**10.2 本地版升级至生产版路线**

13. 将 LocalFileStorageService 替换为
    OssFileStorageService（实现同一接口）

14. 将 syxs.otp.mode 改为 sms，配置短信服务商密钥

15. 引入 Redis 依赖，将 spring.cache.type 改为 redis

16. 引入 Elasticsearch 依赖，替换MySQL全文检索查询

17. 引入 RocketMQ 依赖，将 \@Async 替换为消息生产者/消费者

18. 将模拟支付服务替换为微信支付/支付宝 SDK

19. 用蚂蚁链 SDK 实现 ChainService 接口（去掉 mock_certified 字段）

20. 注册微信/支付宝开发者应用，实现 OAuth 登录

21. 将各模块拆分为独立 Spring Boot 服务，接入 Nacos 服务发现

22. Docker打包，编写 Helm Charts，部署到 K8s 集群

**10.3 文档变更记录**

  -------------------------------------------------------------------------
  **版本**   **日期**      **变更内容**
  ---------- ------------- ------------------------------------------------
  v1.0       2025-03       初始服务器版------微服务架构，全量云服务依赖

  v2.0       2025-03       本地版------单体架构，移除云服务，新增Ollama
                           AI，Docker Compose一键启动
  -------------------------------------------------------------------------

--- 文档终止 ---

尚有新生 \| Local Dev Edition \| Spring Boot 3 + Vue 3 + MySQL 8
