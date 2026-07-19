# 映屿 CineIsle

> 一个自部署的双人本地观影同步工具：本地导入视频，同步播放进度、聊天、弹幕、时间轴笔记，并生成电影票根/片尾回执/观影明信片。

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/linzhi-524/cineisle)

## 这是什么？

映屿 CineIsle 是一个轻量的 watch-together 工具。两个人各自在自己的手机里导入本地视频文件，服务端只负责同步房间状态、播放进度、聊天、弹幕、时间轴笔记和观影卡片。

**它不提供任何影视资源，也不会上传你的视频文件。**

适合：

- 两个人远程一起看同一部本地视频
- 想留下观影票根、台词摘录和观后感
- 想让 AI 通过 MCP 参与观影：发弹幕、记笔记、生成小卡片

## 功能

- 创建/加入观影房间
- 本地导入视频，不上传视频文件
- 播放、暂停、进度同步
- 聊天与弹幕
- 观影邀请卡：电影名、观影人、氛围、开场备注
- 时间轴笔记：每条笔记可绑定当前播放时间
- 金句摘录：手动记录台词/高光瞬间
- 三套观影卡片模板：电影票根、片尾回执、观影明信片
- 档案馆：收藏每一次观影卡片
- 多主题：奶油白、夜航蓝、星河紫、雾岛绿、胶片黑、暮光紫
- MCP 接口：让 ChatGPT / 其他支持 MCP 的 AI 读房间、发弹幕、控制播放、生成卡片

## 文件结构

```text
.
├─ android/              # Android App 源码
├─ server/               # Node.js 后端 + MCP 接口
├─ render.yaml           # Render 一键部署配置
├─ .github/workflows/    # GitHub Actions 自动打包 APK
└─ README.md
```

---

# 傻瓜教程：最快跑起来

## 方案 A：Render 一键部署后端（推荐）

### 1. Fork 或上传到自己的 GitHub 仓库

把这个项目放到你的 GitHub 仓库里。仓库可以叫：

```text
cineisle
```

### 2. 点一键部署按钮

README 顶部有按钮：

```text
Deploy to Render
```

点开后，Render 会读取 `render.yaml`，自动创建一个 Web Service。

如果你把仓库名改了，需要把 README 顶部按钮里的链接改成你的仓库地址，例如：

```markdown
[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/你的用户名/你的仓库名)
```

### 3. 设置后端 Token

Render 会自动生成环境变量：

```text
CINEISLE_TOKEN
```

你也可以手动改成自己记得住的值。这个 Token 用来保护写入接口和 MCP 操作。

### 4. 拿到后端地址

部署成功后，Render 会给你一个地址，例如：

```text
https://cineisle-server.onrender.com
```

打开这个地址，能看到 `映屿 CineIsle Server` 页面，就说明后端成功了。

### 5. App 里填写后端地址

打开 Android App → 设置：

```text
后端地址：https://你的 Render 地址
Token：你的 CINEISLE_TOKEN
昵称：观影人A / 你自己的名字
```

另一台手机也填同一个后端地址和 Token。

### 6. 创建房间并导入视频

一台手机创建房间，另一台手机输入房间号加入。

两边都点「导入视频」，选择本地同一部视频文件。之后就可以同步播放、暂停、跳转、发弹幕和写笔记。

---

## 方案 B：局域网部署后端（同一 Wi‑Fi 内使用）

这个方案适合宿舍、家里、同一个 Wi‑Fi 下测试，不需要 Render。

### 1. 电脑安装 Node.js

建议 Node.js 18 或更新版本。

### 2. 在电脑上启动后端

进入项目的 `server` 文件夹：

```bash
cd server
npm install
```

Windows PowerShell：

```powershell
$env:CINEISLE_TOKEN="change-me"
npm start
```

macOS / Linux：

```bash
export CINEISLE_TOKEN=change-me
npm start
```

看到类似下面的输出就成功了：

```text
CineIsle server: http://localhost:8787
```

### 3. 查电脑局域网 IP

Windows：

```cmd
ipconfig
```

找 `IPv4 地址`，一般像这样：

```text
192.168.1.5
```

macOS：

```bash
ipconfig getifaddr en0
```

Linux：

```bash
ip addr
```

### 4. 手机 App 填局域网地址

手机和电脑必须连同一个 Wi‑Fi。App 设置里填写：

```text
后端地址：http://电脑IP:8787
Token：change-me
```

例如：

```text
http://192.168.1.5:8787
```

浏览器打开下面这个地址能看到页面，就说明手机能连到电脑后端：

```text
http://电脑IP:8787
```

如果打不开，常见原因是电脑防火墙拦截了 8787 端口，允许 Node.js 通过防火墙即可。

---

# Android APK 打包

## 用 GitHub Actions 自动打包

项目自带 `.github/workflows/build-apk.yml`。推送到 GitHub 后，进入：

```text
Actions → Build Android APK → Run workflow
```

构建成功后，在 Artifacts 下载：

```text
cineisle-debug-apk
```

里面会有：

```text
app-debug.apk
```

把 APK 发到手机安装即可。

## 本地打包

如果你本地有 Android 构建环境：

```bash
cd android
gradle :app:assembleDebug
```

APK 输出位置：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

---

# MCP 接入教程

后端自带 MCP 接口：

```text
https://你的后端地址/mcp?token=你的 CINEISLE_TOKEN
```

例如：

```text
https://cineisle-server.onrender.com/mcp?token=change-me
```

支持的工具：

- `create_room`：创建观影房间
- `get_room_state`：读取房间状态
- `send_room_message`：发送聊天/弹幕
- `control_playback`：同步播放、暂停、跳转
- `add_note`：添加时间轴笔记
- `generate_card`：生成观影卡片

给 AI 的示例指令：

```text
请创建一个 CineIsle 观影房间，电影名是 Her，主题是 night，观影人是 A × B。
```

```text
请读取房间 ABC123 的当前状态，然后发一条弹幕：这一幕很漂亮。
```

```text
请根据房间 ABC123 的观影笔记生成一张电影票根。
```

---

# 安全和隐私说明

- CineIsle 不提供影视资源。
- CineIsle 不上传本地视频文件。
- 后端只保存房间状态、聊天、弹幕、笔记、观影卡片等文本信息。
- Render 免费服务可能会休眠，首次打开可能需要等待几十秒。
- 公开部署时请设置 `CINEISLE_TOKEN`，不要把 Token 发到公开评论区或截图里。

---

# 版本

当前公开版：

```text
CineIsle Public v0.2.0
```

公开版已移除私人称呼和私密标识，适合开源、自部署和二次定制。
