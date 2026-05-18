# 触灵工坊

触灵工坊是一个面向 Android 的脚本开发工具原型，目标是让用户通过中文伪代码 DSL 编写自动化脚本，并在底层编译为 Lua 执行。

## 当前实现

- `Kotlin + Jetpack Compose + Material 3`
- `MVVM + StateFlow`
- `Room` 保存脚本、模板和运行记录
- `DataStore` 保存基础设置
- `DSL -> AST -> IR -> Lua` 编译链
- `LuaJ` 运行时与宿主 API 桥接
- `AccessibilityService + Foreground Service + 悬浮窗` 自动化执行骨架

## DSL 示例

```text
记录 "开始执行"
设 次数 = 5
循环 次数 次
点击 540 1600
等待 120
结束循环
如果 次数 > 0
启动应用 "com.android.settings"
结束如果
```

## 首版范围

- 已支持：点击、长按、滑动、等待、变量、循环、条件、启动应用、返回、主页、日志
- 未纳入首版：手势录制、Root/Shizuku、识图、OCR、MediaProjection

## 本地验证

```powershell
$env:JAVA_HOME='D:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```
