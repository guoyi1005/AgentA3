# xfyun avatar sdk vendor

将讯飞官方 Web SDK 文件放到该目录。

当前加载器会按以下顺序尝试入口：

1. `VITE_XFY_AVATAR_SDK_ENTRY`（如果配置）
2. `/vendor/xf-avatar-sdk/index.js`
3. `/vendor/xf-avatar-sdk/sdk/3.1.2.1002/avatar-sdk-web_3.1.2.1002/index.js`（语雀文档示例路径）

如果你下载的是官方 demo/sdk 压缩包，建议优先保持原目录结构并放到：

- `public/vendor/xf-avatar-sdk/sdk/3.1.2.1002/avatar-sdk-web_3.1.2.1002/index.js`

如果你只想用单入口，也可以直接放：

- `public/vendor/xf-avatar-sdk/index.js`
