# 更改记录

## 2026-07-08 修复视频播放

### 问题

- 播放接口按旧的 DURL/渐进式 MP4 思路请求和解析，只把单个视频地址交给 ExoPlayer。
- B站播放接口常返回 DASH 分离流，视频轨和音频轨需要分别加载后合并播放；原实现会出现无法播放、无声或备用线路不可用。
- 播放失败提示里部分中文字符串曾出现乱码，影响代码可读性和错误提示。

### 修改

- `app/src/main/java/com/bilibili/lite/data/repository/VideoRepository.java`
  - 将播放地址请求改为优先请求 DASH 流。
  - 新增视频轨、音频轨选择逻辑：按目标清晰度选择合适视频流，并选择最高码率音频流。
  - 保留 DURL 作为兼容回退路径。
  - 解析 DASH 备用视频地址，供播放器失败时切换备用线路。

- `app/src/main/java/com/bilibili/lite/data/remote/ApiService.java`
  - 为 DASH 流模型补充 `backupUrl` / `backup_url` 字段。

- `app/src/main/java/com/bilibili/lite/ui/video/VideoDetailActivity.java`
  - 使用 `MergingMediaSource` 合并 DASH 视频轨和音频轨。
  - DURL 场景继续使用 `ProgressiveMediaSource` 播放。
  - 备用线路切换时保留当前音频轨，并替换备用视频轨。
  - 修复备用线路索引初始化，避免跳过第一个备用地址。
  - 修复网络错误、超时、播放失败等 Toast 提示文案。

### 验证

- 已执行静态检查：`git diff --check` 通过。
- 按要求未执行本地 Gradle 构建，也未提交 CI。
