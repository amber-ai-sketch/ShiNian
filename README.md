# 拾念 (ShiNian) - 极简智能语音笔记

🔥 **一款 Android 原生形态的极简智能语音笔记产品**

## ✨ 产品定位

「拾念」旨在重塑"录音 - 转化 - 行动"的工作流，作为一个极简的思维容器，帮助用户以最低的摩擦力捕捉稍纵即逝的灵感与长篇会议，并交由 AI 自动完成结构化整理。

## 🎨 核心特性


### 1️⃣ 双模式录音
- **会议模式** (短按)：适合长时录音，自动识别为会议纪要
- **闪念模式** (长按)：适合短时录音，快速捕捉灵感

### 2️⃣ AI 智能结构化
- **语音识别**：火山引擎 ASR，精准转写
- **意图识别**：自动区分会议/灵感/待办
- **智能格式化**：LLM 生成结构化笔记

### 3️⃣ 极简设计
- **温暖极简风格**：陶土红配色，高级感
- **纯文本排版**：无图无插画，专注内容
- **双 Tab 结构**：输入/回顾物理隔离

### 4️⃣ 本地优先 + 离线容错
- 录音文件本地落盘，弱网可用
- 离线自动 Pending，联网后静默同步

## 🏗️ 技术架构

```
Kotlin + Jetpack Compose + MVVM + Hilt
├── UI: Jetpack Compose (声明式 UI)
├── DI: Hilt (依赖注入)
├── DB: Room (本地数据)
├── Net: Retrofit + OkHttp (网络)
├── ASR: 火山引擎 (语音识别)
└── LLM: 火山引擎 Ark (结构化)
```

## 📦 项目结构

```
com.shinian.app/
├── core/          # 核心模块
├── data/            # 数据层 (Repository)
├── domain/          # 领域层 (UseCase)
├── asr/             # 语音识别
├── llm/             # LLM 处理
├── audio/             # 音频管理
├── widget/            # 桌面小组件
└── presentation/      # 表现层 (UI)
    ├── theme/         # 主题
    ├── components/    # 组件
    ├── home/          # 首页
    ├── notes/         # 笔记列表
    └── detail/        # 笔记详情
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17
- Android SDK 34

### 配置步骤

1. **克隆项目**
```bash
git clone https://github.com/amber-ai-sketch/ShiNian.git
```

2. **配置密钥**  
在 `local.properties` 中添加：
```properties
VOLC_ASR_APP_ID=your_app_id
VOLC_ASR_ACCESS_TOKEN=your_token
ARK_API_KEY=your_api_key
```

3. **运行项目**
```bash
./gradlew :app:assembleDebug
```

## 📝 TODO

- [ ] 音频播放器功能完善
- [ ] 笔记搜索功能
- [ ] 标签分类
- [ ] 云端同步
- [ ] iOS 版本

## 📄 License

MIT License © 2024 ShiNian Team

---

🔥 **拾念** - 让每一次思考都被温柔记录