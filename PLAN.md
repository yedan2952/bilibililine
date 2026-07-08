# Bilibili极速版 — 项目计划

## 一、项目概述

开发一款兼容 **Android 4.2+ (API 17)** 的 Bilibili 客户端，原生高性能，UI 模仿原版 Bilibili App。

---

## 二、技术栈选型

| 类别 | 选型 | 版本 | 说明 |
|------|------|------|------|
| **语言** | 纯 Java | Java 7+ | 兼容性最好 |
| **最低SDK** | **minSdk = 17** | Android 4.2 | 核心约束 |
| **编译SDK** | compileSdk = 33 | | 平衡兼容性与新API |
| **目标SDK** | targetSdk = 28 | Android 9 | 对旧设备最友好 |
| **构建** | Gradle + AGP 7.x | | |
| **架构模式** | **MVVM** | ViewModel + LiveData + Repository | Google 官方推荐 |
| **应用ID** | `com.bilibili.lite` | | |
| **应用名** | BiliBili极速版 | | |
| **UI 框架** | **Android View 系统** | | 原生、最广泛兼容 |
| | AppCompat v7 | 28.0.0 | 兼容包基础 |
| | RecyclerView v7 | 28.0.0 | 视频列表 |
| | CardView v7 | 28.0.0 | 视频卡片 |
| | Design Support Library | 28.0.0 | TabLayout, BottomNavigationView |
| **Material 组件** | **XUI** | 1.0.9-support | 兼容 API 17，现成组件库 |
| **ABI 分拆** | 双包发布 | | **完整包**: armv7a + arm64；**轻量包**: 仅 armv7a |
| **图片加载** | **Glide v4** | 4.8.0+ | minSdk=14，兼容 |
| **视频播放** | **ijkplayer** (Bilibili 官方) | 0.7.9.1 | minSdk=9，硬件解码 MediaCodec (API 16+) |
| **网络请求** | OkHttp 3.x + Retrofit 2 | | 稳定高效 |
| **JSON 解析** | Gson / Moshi | | |
| **弹幕渲染** | 自定义 DanmakuView | | 基于 Canvas 自绘制 |
| **数据存储** | SQLite + Room (可选) | | 本地缓存/历史记录 |
| **轻量级数据库** | Realm (可选) | | 或 SQLite |

> **注意**: 由于 minSdk=17，无法使用 Jetpack Compose（需 API 21+），**必须使用传统 View 体系**。

---

## 三、Bilibili API 接口

以 [SocialSisterYi/bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect) 为权威参考。

### 3.1 核心 API

| 功能 | 方法 | 端点 | 鉴权 |
|------|------|------|------|
| **视频信息** | GET | `https://api.bilibili.com/x/web-interface/view?bvid={bvid}` | Cookie (可选) |
| **视频详情** | GET | `https://api.bilibili.com/x/web-interface/view/detail?bvid={bvid}` | WBI 签名 |
| **视频流地址** | GET | `https://api.bilibili.com/x/player/playurl?bvid={bvid}&cid={cid}&qn={quality}` | Cookie + WBI |
| **综合搜索** | GET | `https://api.bilibili.com/x/web-interface/wbi/search/all/v2?keyword={kw}` | WBI 签名 |
| **分类搜索** | GET | `https://api.bilibili.com/x/web-interface/wbi/search/type?keyword={kw}&search_type=video` | WBI 签名 |
| **热搜** | GET | `https://api.bilibili.com/x/web-interface/wbi/search/square` | WBI 签名 |
| **搜索建议** | GET | `https://s.search.bilibili.com/main/suggest?term={kw}` | 无 |
| **评论列表** | GET | `https://api.bilibili.com/x/v2/reply/wbi/main?type=1&oid={aid}&mode=3` | WBI 签名 |
| **评论回复** | GET | `https://api.bilibili.com/x/v2/reply/reply?type=1&oid={aid}&root={rpid}` | Cookie |
| **首页推荐** | GET | `https://api.bilibili.com/x/web-interface/popular` | Cookie |
| **用户信息** | GET | `https://api.bilibili.com/x/space/arc/search?mid={mid}` | Cookie |
| **动态列表** | GET | `https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/space?host_mid={mid}` | Cookie |
| **分区视频** | GET | `https://api.bilibili.com/x/web-interface/newlist?rid={tid}` | 无 |

### 3.2 登录相关

| 功能 | 方法 | 端点 |
|------|------|------|
| **获取二维码** | GET | `https://passport.bilibili.com/x/passport-login/web/qrcode/generate` |
| **轮询扫码状态** | GET | `https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key={key}` |
| **获取密码盐** | GET | `https://passport.bilibili.com/x/passport-login/web/key` |
| **密码登录** | POST | `https://passport.bilibili.com/x/passport-login/web/login` |

### 3.3 弹幕

| 功能 | 端点 |
|------|------|
| **弹幕数据 (XML)** | `https://api.bilibili.com/x/v1/dm/list.so?oid={cid}` |
| **弹幕数据 (Protobuf)** | `https://api.bilibili.com/x/v2/dm/web/seg.so?type=1&oid={cid}&segment_index=1` |

### 3.4 WBI 签名机制

部分接口需要 WBI 签名：
1. 从 `https://api.bilibili.com/x/web-interface/nav` 获取 `img_key` 和 `sub_key`
2. 拼接密钥：`mix_key = sub_key[0:4] + img_key[0:4]`
3. 对参数排序拼接后加 mix_key，计算 md5
4. 添加 `w_rid` 和 `wts` (时间戳) 到请求参数

---

## 四、项目结构 (MVVM 架构)

```
bilibililine/
├── app/
│   ├── src/
│   │   ├── main/java/com/bilibili/lite/
│   │   │   ├── App.java                          # Application 入口
│   │   │   ├── data/                             # 数据层
│   │   │   │   ├── model/                        # 数据模型 (POJO)
│   │   │   │   │   ├── VideoInfo.java
│   │   │   │   │   ├── UserInfo.java
│   │   │   │   │   ├── CommentItem.java
│   │   │   │   │   ├── SearchResult.java
│   │   │   │   │   ├── LiveRoomInfo.java         # 直播房间信息
│   │   │   │   │   └── DanmakuItem.java
│   │   │   │   ├── remote/                       # 网络层
│   │   │   │   │   ├── ApiService.java           # Retrofit 接口定义
│   │   │   │   │   ├── RetrofitClient.java       # 网络客户端单例
│   │   │   │   │   ├── WbiSigner.java            # WBI 签名工具
│   │   │   │   │   └── CookieManager.java        # Cookie 持久化管理
│   │   │   │   ├── local/                        # 本地存储
│   │   │   │   │   └── AppDatabase.java          # Room 数据库
│   │   │   │   └── repository/                   # 仓库层
│   │   │   │       ├── VideoRepository.java
│   │   │   │       ├── SearchRepository.java
│   │   │   │       ├── UserRepository.java
│   │   │   │       ├── CommentRepository.java
│   │   │   │       └── LiveRepository.java       # 直播仓库
│   │   │   ├── ui/                               # UI 层
│   │   │   │   ├── MainActivity.java             # 主Activity (底部导航)
│   │   │   │   ├── home/                         # 首页
│   │   │   │   │   ├── HomeFragment.java
│   │   │   │   │   ├── HomeViewModel.java
│   │   │   │   │   └── adapter/
│   │   │   │   │       └── VideoFeedAdapter.java
│   │   │   │   ├── video/                        # 视频详情
│   │   │   │   │   ├── VideoDetailActivity.java
│   │   │   │   │   ├── VideoDetailViewModel.java
│   │   │   │   │   └── PlayerView.java
│   │   │   │   ├── live/                         # 直播
│   │   │   │   │   ├── LiveFragment.java
│   │   │   │   │   ├── LiveViewModel.java
│   │   │   │   │   ├── LivePlayerActivity.java
│   │   │   │   │   └── adapter/
│   │   │   │   │       └── LiveRoomAdapter.java
│   │   │   │   ├── search/                       # 搜索
│   │   │   │   │   ├── SearchActivity.java
│   │   │   │   │   ├── SearchViewModel.java
│   │   │   │   │   └── SearchSuggestFragment.java
│   │   │   │   ├── user/                         # 用户
│   │   │   │   │   ├── UserSpaceActivity.java
│   │   │   │   │   └── UserSpaceViewModel.java
│   │   │   │   ├── login/                        # 登录
│   │   │   │   │   ├── LoginActivity.java
│   │   │   │   │   └── LoginViewModel.java
│   │   │   │   ├── discover/                     # 发现/分区
│   │   │   │   │   ├── DiscoverFragment.java
│   │   │   │   │   └── DiscoverViewModel.java
│   │   │   │   ├── mine/                         # 我的
│   │   │   │   │   ├── MineFragment.java
│   │   │   │   │   └── MineViewModel.java
│   │   │   │   └── widget/                       # 自定义组件
│   │   │   │       ├── DanmakuView.java
│   │   │   │       └── VideoCardView.java
│   │   │   ├── player/                           # 播放器核心
│   │   │   │   ├── IjkPlayerWrapper.java
│   │   │   │   └── MediaManager.java
│   │   │   ├── danmaku/                          # 弹幕引擎
│   │   │   │   └── DanmakuParser.java
│   │   │   └── util/                             # 工具
│   │   │       ├── ImageLoader.java
│   │   │       ├── DateUtil.java
│   │   │       └── Constants.java
│   │   ├── main/res/
│   │   │   ├── layout/
│   │   │   ├── drawable/
│   │   │   ├── menu/
│   │   │   └── values/
│   │   ├── main/AndroidManifest.xml
│   │   ├── arm7/AndroidManifest.xml              # 仅 armv7a 包
│   │   └── arm8/AndroidManifest.xml              # armv7a + arm64 包
│   └── build.gradle
├── build.gradle                                  # 项目级构建
├── settings.gradle
├── gradle.properties
└── gradle/wrapper/
```

---

## 五、UI 模仿方案

### 5.1 主框架 (MainActivity)
- **底部导航**: BottomNavigationView (5 tabs: 首页/动态/发现/我的)
- **顶部栏**: Toolbar + 搜索框入口
- **内容**: ViewPager + Fragment 切换

### 5.2 首页 (HomeFragment)
- **顶部 TabLayout**: 推荐/热门/分区Tab
- **视频流**: RecyclerView + StaggeredGridLayoutManager (瀑布流)
- **视频卡片**: CardView 包裹，封面+标题+UP主+播放量

### 5.3 视频详情 (VideoDetailActivity)
- **播放器**: ijkplayer 全屏/竖屏切换
- **弹幕层**: 自定义 DanmakuView Canvas 绘制
- **信息区**: 标题、UP主、点赞/投币/收藏
- **评论列表**: RecyclerView + 评论Adapter

### 5.4 搜索结果
- **搜索框**: Toolbar 内嵌 EditText
- **搜索建议**: PopupWindow 或 Fragment
- **搜索结果**: RecyclerView 网格/列表切换

### 5.5 主题配色
- 主色: #FB7299 (Bilibili 粉)
- 背景: #F5F5F5 (浅灰)
- 暗色: #1E1E1E (暗黑模式可选)

---

## 六、开发里程碑

### 阶段一：项目骨架 (Week 1)
- 创建 Android 项目，配置 build.gradle (minSdk=17)
- 集成所有依赖库 (Glide, ijkplayer, Retrofit, OkHttp, Carbon/XUI)
- 实现 MainActivity 底部导航框架
- 实现网络层 (RetrofitClient, ApiService, WbiSigner)

### 阶段二：核心功能 — 视频流 (Week 2)
- 实现 HomeFragment + VideoFeedAdapter
- 接入首页推荐 API (/popular)
- 视频卡片 UI（封面、标题、UP主、播放数）
- 下拉刷新 (SwipeRefreshLayout)

### 阶段三：视频播放 (Week 3)
- 集成 ijkplayer
- VideoDetailActivity 布局
- 播放器控制条（播放/暂停、进度、全屏）
- 清晰度切换
- 视频信息展示

### 阶段四：弹幕系统 (Week 4)
- DanmakuView 自定义 View
- 弹幕解析 (Protobuf/XML)
- 弹幕渲染（滚动、顶部、底部）
- 弹幕开关/设置

### 阶段五：搜索功能 (Week 5)
- SearchActivity 实现
- 热搜榜单
- 搜索建议
- 搜索结果展示
- 分类筛选

### 阶段六：登录与用户系统 (Week 6)
- 二维码扫码登录
- Cookie 持久化
- 用户空间页面
- 我的页面（浏览历史、收藏）

### 阶段七：直播模块 (Week 7)
- LiveFragment 直播列表
- 直播房间信息接入 (`/xlive/room/v1/index/getInfoByRoom`)
- 直播播放器 (ijkplayer + flv)
- 直播间弹幕 WebSocket 连接
- 直播礼物/关注交互

### 阶段八：评论与交互 (Week 8)
- 评论列表加载
- 评论回复层级
- 点赞/投币/收藏（需登录）
- 评论输入

### 阶段九：性能优化与兼容性测试 (Week 9)
- Android 4.2 (API 17) 真机测试
- 内存优化（Bitmap 复用、列表回收）
- 网络缓存策略
- 包体积优化 (ABI 拆分、资源缩减)
- 双包构建验证（armv7a-only / armv7a+arm64）
- Android 4.2 (API 17) 真机测试
- 内存优化（Bitmap 复用、列表回收）
- 网络缓存策略
- 包体积优化 (ABI 拆分、资源缩减)

---

## 七、关键依赖版本约束

```groovy
android {
    compileSdk 33
    defaultConfig {
        applicationId "com.bilibili.lite"
        minSdk 17
        targetSdk 28  // 对旧设备最友好
        versionCode 1
        versionName "1.0.0"
    }

    // 双 ABI 包构建配置
    flavorDimensions "abi"
    productFlavors {
        arm8 {
            dimension "abi"
            ndk { abiFilters "armeabi-v7a", "arm64-v8a" }
            versionNameSuffix "-arm8"
        }
        arm7 {
            dimension "abi"
            ndk { abiFilters "armeabi-v7a" }
            versionNameSuffix "-arm7"
        }
    }
}

dependencies {
    // Android Support (API 17 兼容)
    implementation 'com.android.support:appcompat-v7:28.0.0'
    implementation 'com.android.support:recyclerview-v7:28.0.0'
    implementation 'com.android.support:cardview-v7:28.0.0'
    implementation 'com.android.support:design:28.0.0'
    implementation 'com.android.support:support-annotations:28.0.0'

    // XUI (兼容 API 17)
    implementation 'com.github.xuexiangjys:XUI:1.0.9-support'
    implementation 'com.android.support:appcompat-v7:28.0.0' // XUI 所需

    // MVVM (Android Architecture Components)
    implementation 'android.arch.lifecycle:extensions:1.1.1'
    implementation 'android.arch.lifecycle:viewmodel:1.1.1'

    // Image Loading
    implementation 'com.github.bumptech.glide:glide:4.8.0'

    // Network
    implementation 'com.squareup.okhttp3:okhttp:3.12.13'  // 最后支持 API 16+ 的版本
    implementation 'com.squareup.okhttp3:logging-interceptor:3.12.13'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

    // Video Player (ijkplayer)
    implementation 'tv.danmaku.ijk.media:ijkplayer-java:0.7.9.1'
    arm7Implementation 'tv.danmaku.ijk.media:ijkplayer-armv7a:0.7.9.1'
    arm8Implementation 'tv.danmaku.ijk.media:ijkplayer-armv7a:0.7.9.1'
    arm8Implementation 'tv.danmaku.ijk.media:ijkplayer-arm64:0.7.9.1'

    // JSON
    implementation 'com.google.code.gson:gson:2.8.9'
}
```

> **注意**: OkHttp 3.12.x 是最后一个支持 Android API 16+ 的版本。Retrofit 2.9.0 兼容 OkHttp 3.x。

---

## 八、注意事项

1. **WBI 签名**: 2024年起B站强制部分API需要WBI签名，需实现签名算法
2. **Cookie/Header**: 需模拟浏览器 User-Agent 和 Referer，避免被风控
3. **buvid3**: 很多 API 需要 Cookie 中包含 buvid3，首次需从主页获取
4. **ijkplayer 编译**: 建议直接使用 Maven 依赖，避免自行编译 FFmpeg
5. **低版本兼容**: Android 4.2 不支持 VectorDrawable 和某些属性，需用兼容方案
6. **HTTPS**: 确保支持 TLS 1.2（Android 4.2 默认支持）
