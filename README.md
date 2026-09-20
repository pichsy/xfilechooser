# XFileChooser

一个轻量、易用的 Android **文件选择 / 系统分享** 封装库。内置高兼容性的 Uri 工具类，
帮你处理 `FileProvider`、`Uri` 与真实文件路径之间的转换，适配 Android 8.0（API 26）及以上，
并针对 Android 10+ 分区存储做了增强兜底。

[![minSdk](https://img.shields.io/badge/minSdk-26-blue)](https://developer.android.com/about/versions/oreo)
[![targetSdk](https://img.shields.io/badge/targetSdk-35-green)](https://developer.android.com/about/versions/15)
[![version](https://img.shields.io/badge/version-2.0.0-orange)](https://central.sonatype.com)
[![license](https://img.shields.io/badge/license-Apache--2.0-brightgreen)](https://www.apache.org/licenses/LICENSE-2.0.txt)

## 目录

- [功能特性](#功能特性)
- [环境要求](#环境要求)
- [引入依赖](#引入依赖)
- [初始化配置](#初始化配置)
  - [1. 配置 FileProvider](#1-配置-fileprovider)
  - [2. 配置权限](#2-配置权限)
- [使用文档](#使用文档)
  - [一、文件 / 图片选择：FileChooser](#一文件--图片选择filechooser)
  - [二、系统分享：FileShare](#二系统分享fileshare)
  - [三、Uri 工具类：FileChooseUriUtils](#三uri-工具类filechooseuriutils)
  - [四、ContentType 常量](#四contenttype-常量)
- [完整示例](#完整示例)
- [兼容性与注意事项](#兼容性与注意事项)
- [更新记录 Changelog](#更新记录-changelog)
- [License](#license)

## 功能特性

- ✅ **系统分享**：分享文字、单张 / 多张图片、文件，支持指定目标 App 或强制系统选择器。
- ✅ **相册选择**：调用系统相册选择图片，可回调 `Uri` 和 `Bitmap`。
- ✅ **拍照选择**：调用系统相机拍照，自动通过 `FileProvider` 生成 Uri。
- ✅ **文件选择**：`ACTION_GET_CONTENT` / `ACTION_OPEN_DOCUMENT` 选择任意文件。
- ✅ **图片剪裁**：内置正方形剪裁（基于 `com.android.camera.action.CROP`）。
- ✅ **高兼容 Uri 工具**：`getFileRealPath()` 终极版，适配主流厂商 FileProvider 与 Android 10+ 分区存储。

## 环境要求

| 项目       | 要求            |
| ---------- | --------------- |
| minSdk     | 26（Android 8.0）|
| targetSdk  | 35（Android 15）|
| compileSdk | 35              |
| Java       | 17              |
| AGP        | 8.3.2+          |
| Gradle     | 8.9+            |

## 引入依赖

在项目根目录的 `settings.gradle`（或 `settings.gradle.kts`）中确保包含 `mavenCentral()`：

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

然后在模块的 `build.gradle` 中添加依赖：

```groovy
dependencies {
    api 'com.gitee.pichs:xfilechooser:2.0.0'
}
```

> 该库会通过 `api` 传递引入 `androidx.core:core-ktx`、`androidx.appcompat:appcompat`、
> `androidx.annotation:annotation`、`androidx.fragment:fragment-ktx`，一般无需重复添加。

## 初始化配置

### 1. 配置 FileProvider

库内部使用 `FileProvider` 生成可被系统访问的 `content://` Uri，因此**必须**在你的
`AndroidManifest.xml` 中声明 Provider，并且 `authorities` 要与库中使用的一致。

```xml
<application>
    ...
    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="${applicationId}.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_provider" />
    </provider>
</application>
```

在 `res/xml/file_provider.xml` 中定义路径（可按需裁剪，但相机/剪裁输出目录需要被覆盖到）：

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <root-path name="root" path="" />
    <files-path name="files" path="files" />
    <cache-path name="cache" path="cache" />
    <external-path name="external" path="external" />
    <external-files-path name="external_files" path="external_files" />
    <external-cache-path name="external_cache" path="external_cache" />
</paths>
```

> 库默认使用的 authority 为 `context.getPackageName() + ".fileprovider"`。
> 若你使用自定义 authority，请通过 `.authority(...)` 传入，并保持与清单文件一致。

### 2. 配置权限

库本身不强制要求权限，但根据你的使用场景建议按需声明：

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.CAMERA" />
```

> Android 13（API 33）起读写外部存储改用系统照片/媒体选择器，
> 相册、相机、文件选择均无需 `READ/WRITE_EXTERNAL_STORAGE` 权限。
> 若你声明了 `CAMERA` 权限，请在调用相机前自行完成运行时权限申请。

## 使用文档

### 一、文件 / 图片选择：FileChooser

`FileChooser` 支持三种打开方式，通过链式调用配置：

| 方法                     | 说明 |
| ------------------------ | ---- |
| `.gallery()`             | 打开相册（默认） |
| `.camera()`              | 打开相机拍照 |
| `.file()`                | 打开系统文件选择器 |
| `.withCrop()`            | 启用正方形剪裁 |
| `.cropWidth(int)`        | 设置剪裁宽度（正方形，默认 200） |
| `.asBitmap()`            | 回调时同时返回 `Bitmap` |
| `.authority(String)`     | 指定 FileProvider 的 authority（默认 `包名 + ".fileprovider"`） |
| `.cameraOutputUri(Uri)`  | 拍照输出 Uri（默认 App 外部公有 Pictures 目录） |
| `.cropOutputFile(File)`  | 剪裁输出文件（必须是公有路径） |
| `.requestCodeForGallery(int)` | 相册请求码（默认 102） |
| `.requestCodeForCamera(int)`  | 相机请求码（默认 103） |
| `.requestCodeForCrop(int)`    | 剪裁请求码（默认 104） |
| `.requestCodeForFile(int)`    | 文件请求码（默认 105） |
| `.reset()`               | 重置 `isCrop`、`asBitmap` 等状态 |
| `.listener(callback)`    | 设置结果回调 |
| `.open()`                | 触发打开 |

#### 相册选择

```java
FileChooser.get().with(this)
        .gallery()
        .listener(new FileChooser.OnFileChooseCallBack() {
            @Override
            public void onCallBack(Uri uri, Bitmap bitmap, String message) {
                if (uri != null) {
                    imageView.setImageURI(uri);
                }
            }
        })
        .open();
```

#### 相机拍照

```java
FileChooser.get().with(this)
        .camera()
        .listener((uri, bitmap, message) -> {
            if (uri != null) {
                imageView.setImageURI(uri);
            }
        })
        .open();
```

#### 文件选择

```java
FileChooser.get().with(this)
        .file()
        .listener((uri, bitmap, message) -> {
            // 非图片文件 bitmap 为 null
            if (uri != null) {
                String realPath = FileChooseUriUtils.getFileRealPath(this, uri);
                // do something with realPath
            }
        })
        .open();
```

#### 剪裁 + 返回 Bitmap

```java
FileChooser.get().with(this)
        .withCrop()
        .cropWidth(400)
        .asBitmap()
        .gallery()
        .listener((uri, bitmap, message) -> {
            imageView.setImageBitmap(bitmap);
        })
        .open();
```

#### 在 onActivityResult 中转发结果

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    FileChooser.get().with(this).onActivityResult(requestCode, resultCode, data);
}
```

#### 释放资源

`FileChooser` 内部使用弱引用保存 Builder，一般无需手动释放；如需立即释放可调用：

```java
FileChooser.get().release(this);
```

### 二、系统分享：FileShare

`FileShare` 用于调用系统分享，支持文字、图片、文件。

| 方法                                 | 说明 |
| ------------------------------------ | ---- |
| `.setContentType(String)`            | 分享内容类型，见 [ContentType](#四contenttype-常量) |
| `.setTitle(String)`                  | 分享标题 |
| `.setTextContent(String)`            | 分享的文字内容 |
| `.addShareFileUri(Uri)`              | 添加单个分享 Uri |
| `.addShareFileUri(Uri...)`           | 添加多个分享 Uri |
| `.addShareFileUri(Collection<Uri>)`  | 批量添加分享 Uri |
| `.clearShareFileUri()`               | 清空分享 Uri |
| `.setShareToComponent(pkg, cls)`     | 指定分享到某个组件（优先级最高） |
| `.setTargetPackage(pkg)`             | 指定分享到某个包名 |
| `.setOnActivityResult(int)`          | 设置结果回调请求码（默认 -1） |
| `.forcedUseSystemChooser(boolean)`   | 是否强制使用系统选择器（默认 true） |
| `.build()`                           | 构建 `FileShare` |
| `.share()`                           | 执行分享 |

#### 分享文字

```java
FileShare.with(this)
        .setContentType(ContentType.TEXT)
        .setTextContent("这是一段分享文字")
        .setTitle("分享")
        .forcedUseSystemChooser(true)
        .build()
        .share();
```

#### 分享单张 / 多张图片

```java
FileShare.with(this)
        .setContentType(ContentType.IMAGE)
        .addShareFileUri(uri1)
        .addShareFileUri(uri2, uri3)      // 多张
        .setTextContent("快来看看这些图")
        .setTitle("分享图片")
        .forcedUseSystemChooser(true)
        .setOnActivityResult(1901)
        .build()
        .share();
```

#### 分享到指定 App

```java
FileShare.with(this)
        .setContentType(ContentType.FILE)
        .addShareFileUri(fileUri)
        .setShareToComponent("com.twitter.android", "com.twitter.android.PostActivity")
        // 或 .setTargetPackage("com.twitter.android")
        .build()
        .share();
```

### 三、Uri 工具类：FileChooseUriUtils

`FileChooseUriUtils` 提供了一组静态工具方法，用来处理 `Uri` 与文件之间的转换。

#### getFileRealPath —— 获取真实路径（终极版）

```java
String realPath = FileChooseUriUtils.getFileRealPath(context, uri);
```

> 兼容 `file://`、`content://`、Document Uri、主流厂商 FileProvider 前缀，
> 并通过 `StorageVolume` 反射支持 SD 卡等非主存储。
> 当 Android 10+ 分区存储下无法直接拿到真实路径时，会自动把内容拷贝到应用缓存并返回缓存路径。

#### 生成 Uri

```java
// 根据文件类型自动生成 Uri（file:// 或 content://）
Uri uri = FileChooseUriUtils.getUri(context, ContentType.IMAGE, file);

// 指定 FileProvider authority
Uri uri2 = FileChooseUriUtils.getUri(context, ContentType.FILE, file, authority);

// 从 File 生成 Uri
Uri uri3 = FileChooseUriUtils.getUriFromFile(context, file);
Uri uri4 = FileChooseUriUtils.getUriFromFile(context, authority, file);

// 从相册图片名生成 Uri
Uri uri5 = FileChooseUriUtils.getUriFromImage(context, "photo.jpg");
```

#### 保存 Bitmap 并返回 Uri

```java
Uri uri = FileChooseUriUtils.saveBitmapAndReturnUri(context, bitmap, "avatar.png");
```

#### 生成不重复的文件

```java
File file = FileChooseUriUtils.generateFileName("photo.jpg", directory);
```

### 四、ContentType 常量

| 常量             | 值           | 说明   |
| ---------------- | ------------ | ------ |
| `ContentType.TEXT`  | `text/plain` | 文本   |
| `ContentType.IMAGE` | `image/*`    | 图片   |
| `ContentType.AUDIO` | `audio/*`    | 音频   |
| `ContentType.VIDEO` | `video/*`    | 视频   |
| `ContentType.FILE`  | `*/*`        | 任意文件 |

## 完整示例

```java
public class MainActivity extends AppCompatActivity {

    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 选择图片（带剪裁）
        findViewById(R.id.btnPick).setOnClickListener(v ->
                FileChooser.get().with(MainActivity.this)
                        .withCrop()
                        .gallery()
                        .listener((uri, bitmap, message) -> {
                            if (uri != null) {
                                mUri = uri;
                                ((ImageView) findViewById(R.id.ivPreview)).setImageURI(uri);
                            }
                        })
                        .open());

        // 系统分享
        findViewById(R.id.btnShare).setOnClickListener(v ->
                FileShare.with(MainActivity.this)
                        .addShareFileUri(mUri)
                        .setContentType(ContentType.IMAGE)
                        .setTextContent("谢谢支持")
                        .setTitle("分享")
                        .forcedUseSystemChooser(true)
                        .build()
                        .share());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FileChooser.get().with(this).onActivityResult(requestCode, resultCode, data);
    }
}
```

## 兼容性与注意事项

1. **FileProvider authority 必须一致**：清单文件中的 `android:authorities` 与代码中的
   `.authority(...)` 必须一致，否则会抛出 `IllegalArgumentException`。
2. **剪裁仅支持正方形**：内置剪裁为 1:1 正方形，其他比例请自行实现。
3. **剪裁 Intent 为非标准协议**：`com.android.camera.action.CROP` 并非所有机型都支持，
   部分厂商 ROM 可能无法完成剪裁，建议做好降级处理。
4. **相机 / 剪裁输出路径必须是公有路径**：系统相机与剪裁程序无法访问 App 私有目录，
   请勿将 `cameraOutputUri` / `cropOutputFile` 指向 `getCacheDir()` 等私有路径。
5. **Android 10+ 分区存储**：`getFileRealPath()` 已在 Android 10+ 下做增强兜底，
   无法解析真实路径时会自动拷贝到缓存目录。
6. **Android 13+ 权限**：相册 / 相机 / 文件选择无需存储权限；如需拍照请自行申请 `CAMERA` 权限。

## 更新记录 Changelog

所有值得注意的变更都记录在此。

### [2.0.0] - 2026-09-20

**构建与工程现代化**

- 迁移构建体系：AGP 4.1.1 / Gradle 6.5 → **AGP 8.3.2 / Gradle 8.9 / Java 17**。
- 使用 **Kotlin DSL + Gradle Version Catalog** 管理依赖，与 xwidget 工程对齐。
- `compileSdk` / `targetSdk` 升级至 **35（Android 15）**，`minSdk` 调整为 **26**。

**Uri 工具增强**

- `FileChooseUriUtils.getFileRealPath()` 对齐 xbase 高兼容实现：
  - 新增 `File(uri.toString()).exists()` 直接路径判断。
  - 新增 `uri2File` / `copyUri2Cache` 缓存拷贝兜底，解决 Android 10+ 分区存储下
    `MediaStore.DATA` 列为空导致的路径获取失败。
  - 支持 SD 卡等非主存储（`StorageVolume` 反射解析）。
  - 兼容更多厂商 FileProvider 前缀（华为、QQ 浏览器、Google Photos 等）与 `msf:` / `raw:` 下载前缀。

**其他**

- 移除 README 中「项目已迁移至 gitee」提示。
- 修复 `maven.gradle` 在构建时打印 `local.properties` 密钥的问题。
- 完善 Maven Central 发布脚本。

### [1.1] - 2021-03-18

- 完善文档说明。

### [1.0] - 2021-01-05

- 首个版本发布，提供：
  - 系统分享（`FileShare`）。
  - 文件 / 图片选择（`FileChooser`），支持相册、相机、文件、正方形剪裁。
  - Uri 工具类 `FileChooseUriUtils`（原 `FileUriUtils`）。

## License

```
Copyright 2021 pichs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
