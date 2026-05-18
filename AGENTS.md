# 触灵工坊工程说明

## 项目定位

触灵工坊是一个 Android 脚本工具原型工程，目标是提供：

- 面向用户的脚本编辑器
- 中文伪代码 DSL
- `DSL -> AST -> IR -> Lua` 的编译链
- 基于 `AccessibilityService` 的自动化执行能力
- 悬浮窗式的脚本控制入口

当前项目仍处于原型阶段，重点是把脚本编辑、脚本校验、脚本执行链路和悬浮窗控制条打通。

## 当前已实现功能

### 1. 页面结构

当前 App 采用底部导航，包含 3 个主页面：

- `首页`
  - 加载本地脚本文件
  - 读取当前已选脚本
  - 对脚本做 DSL 编译校验
  - 校验通过后，“启用悬浮窗”按钮可用并显示为绿色

- `脚本编辑器`
  - 独立编辑页面
  - 第一行是标题“脚本编辑器”
  - 第一行右上角有独立的“编译”按钮
  - 第二行是工具栏：`新建 / 插入模板 / 保存 / 另存为 / 打开文件 / 撤销`
  - 下方为脚本编辑区
  - 下方可查看编译结果和生成的 Lua

- `设置`
  - 打开无障碍服务设置
  - 打开悬浮窗权限设置
  - 打开通知权限设置

### 2. 脚本编辑与文件存储

脚本和模板目前采用本地文件存储，不再以页面主流程依赖 Room：

- 用户脚本目录：应用私有目录下的 `scripts`
- 模板脚本目录：应用私有目录下的 `templates`
- 默认会自动生成欢迎脚本和若干模板脚本
- 支持：
  - 新建脚本
  - 保存
  - 另存为
  - 打开本地脚本
  - 插入模板
  - 撤销

### 3. DSL 与编译链

当前 DSL 已实现基础语法解析和编译，支持：

- `点击 x y`
- `长按 x y duration`
- `滑动 x1 y1 x2 y2 duration`
- `等待 ms`
- `启动应用 "包名"`
- `记录 "文本"`
- `设 变量 = 表达式`
- `循环 N 次 ... 结束循环`
- `如果 条件 ... 否则 ... 结束如果`
- `返回`
- `主页`

编译链为：

- `ScriptParser`
- `AST`
- `IR`
- `LuaGenerator`
- `LuaJ` 执行

### 4. 自动化执行

当前自动化能力基于：

- `AccessibilityService`
- `Foreground Service`
- `LuaJ Runtime`
- Host API 桥接

Lua 侧会调用受控宿主 API，例如：

- `touch.click(...)`
- `touch.longPress(...)`
- `touch.swipe(...)`
- `device.sleep(...)`
- `device.back()`
- `device.home()`
- `app.launch(...)`
- `log.info(...)`

### 5. 悬浮窗

当前已实现悬浮窗控制条原型：

- 样式为圆角横条
- 已包含按钮：
  - 启动/停止
  - 暂停/继续
  - 日志
  - 退出悬浮窗
- 状态与日志已迁移到悬浮窗逻辑中维护

注意：

- 悬浮窗显示依赖系统悬浮窗权限
- 脚本动作执行依赖无障碍服务权限

### 6. 数据与状态

当前工程同时包含两类数据存储：

- `FileScriptRepository`
  - 负责脚本文件与模板文件
  - 是当前页面主流程使用的脚本来源

- `Room + ScriptRepository`
  - 当前仍保留
  - 主要用于运行记录等结构化数据
  - 后续可继续精简或统一

设置项使用 `DataStore`，当前主要保存：

- 编辑器字体缩放
- 默认延迟
- 当前选中的脚本路径与脚本名

## 当前工程结构

### 核心入口

- `app/src/main/java/com/lulucloud/touchscript/MainActivity.kt`
- `app/src/main/java/com/lulucloud/touchscript/TouchWorkshopApplication.kt`
- `app/src/main/java/com/lulucloud/touchscript/app/AppContainer.kt`

### 主要模块

- `feature/home`
  - 首页逻辑

- `feature/editor`
  - 脚本编辑器

- `feature/settings`
  - 设置页

- `core/script`
  - DSL 解析、AST、IR、Lua 生成

- `core/runtime`
  - Lua 运行时与宿主桥接

- `core/automation`
  - 无障碍执行、执行状态、悬浮窗、前台服务

- `data/repository`
  - 文件脚本仓库、设置仓库、运行记录仓库

## 已验证内容

目前工程已验证过以下构建链路：

- `.\gradlew.bat assembleDebug`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat installDebug`

并且已经在真机上解决过一次 Compose 启动闪退问题。

## 当前已知限制

- 悬浮窗交互虽然已落代码，但仍属于原型阶段，细节体验还需要继续打磨
- 首页、导航、弹窗等中文文案在部分文件中经历过多轮改动，后续建议统一做一次文案和编码清理
- 脚本文件目前保存在应用私有目录，尚未接入系统文件选择器
- Room 仍保留了一部分原型阶段数据结构，后续可以决定是否继续保留
- 还没有接入识图、OCR、Root、Shizuku、手势录制等更高级能力

## 后续协作建议

如果后续继续开发，建议优先按下面顺序推进：

1. 清理页面与仓库中的中文乱码/旧文案
2. 把悬浮窗按钮交互完整联调一遍
3. 补齐脚本文件管理能力
4. 优化编辑器体验
5. 再考虑更高级的自动化能力

## 说明

本文件用于帮助后续协作者快速理解当前工程状态，不代表最终产品规格。
