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
设 结果 = 识图 "登录按钮.png" 0.85
如果 结果.找到
点击 结果.x 结果.y
记录 结果.置信度
结束如果
设 次数 = 5
循环 次数 次
点击 540 1600
键盘输入 "hello"
等待 120
结束循环
无限循环
等待 1000
结束循环
如果 次数 > 0
启动应用 "com.android.settings"
停止运行
结束如果
```

## 首版范围

- 已支持：点击、长按、滑动、键盘输入、等待、识图、变量、循环、无限循环、条件、启动应用、返回、主页、停止运行、日志
- 未纳入首版：手势录制、Root/Shizuku、OCR、MediaProjection

识图图片会统一放在脚本工作目录的 `Images` 子文件夹下，脚本中只需要填写图片文件名。`识图` 作为表达式使用时会返回结果对象，可读取 `找到`、`x`、`y`、`置信度`。

## 本地验证

```powershell
$env:JAVA_HOME='D:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
```
