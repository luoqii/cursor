# PhotoCapture

一个最小但完整的 Android 拍照示例应用，演示如何：

- 申请运行时相机权限
- 使用 `ActivityResultContracts.TakePicture` 调用系统相机拍照
- 借助 `FileProvider` 在应用私有目录中安全共享文件
- 通过 ViewBinding 更新界面并展示最近一次拍照时间

## 构建与运行

1. 安装 [Android Studio](https://developer.android.com/studio) 或确保本地有 JDK 17 及以上版本。
2. 克隆本仓库后，在根目录执行：

   ```bash
   ./gradlew assembleDebug
   ```

   首次运行会自动下载 Gradle 与依赖，成功后可在 `app/build/outputs/apk/debug/` 找到 APK。
3. Android Studio 用户可以直接 `File > Open` 选择项目根目录，通过“运行”按钮部署到模拟器或真机。

## 目录结构

```
.
├── app
│   ├── build.gradle.kts        # app 模块配置
│   └── src/main
│       ├── AndroidManifest.xml # 权限、Activity、FileProvider 配置
│       ├── java/com/example/photocapture/MainActivity.kt
│       └── res/...             # 布局、字符串、主题等资源
├── build.gradle.kts            # 根级插件版本
├── gradle/                     # Gradle wrapper 配置
├── gradlew / gradlew.bat       # 脚本入口
├── gradle.properties
└── settings.gradle.kts
```

## 功能概览

- 点击“拍照”按钮后自动检查/请求相机权限
- 成功拍照后显示预览与时间戳，再次点击按钮可重拍
- 权限被永久拒绝时提供跳转到系统设置的引导

欢迎继续扩展诸如图库列表、图像编辑等能力。
