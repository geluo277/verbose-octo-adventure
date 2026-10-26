# NeoXLab Mobile — NXPK/NPK 全资源解包实验版

Android 工程，目标是提供类似 NeoXLab 桌面工具的资源列表与导出体验。当前这一版先完成全资源原始导出核心流程，后续再扩展资源预览和 CISC 特殊格式解析。

## 当前实现

- 通过 Android 文件选择器选择 `.npk` 文件。
- 使用项目内的 NXPK Mode 3 索引解析与路径词典。
- 遍历索引中的全部条目，而不是只筛选贴图。
- 支持已知条目压缩类型：0（原始存储）与 3（Zstandard）。
- 根据文件头及路径扩展名，将条目归类到 `贴图`、`3D模型`、`UI`、`代码与配置`、`音频`、`其他`。
- 不认识的资源会保留解压后的原始字节；遇到未知压缩类型时保留压缩字节，避免静默丢失。
- 结果打包成 ZIP，供用户通过 Android 文件选择器保存。
- 对每个条目记录失败原因摘要；不会在零文件时假报成功。

## 已知限制

- 当前实现基于 NXPK Mode 3 索引；不能保证所有 NPK/CISC 变体都兼容。
- `script(1).npk` 样本的 Mode 3 索引可解析且 1525 个条目均能按索引范围读取，但这只验证了容器条目导出，不代表已解码其中可能存在的脚本指令、加密资源或专有模型格式。
- 分类是基于资源文件头和路径后缀的启发式识别；未知类型放入 `其他`，不会伪装成已解码的模型或代码。
- 当前还没有实现完整的右侧图片/3D 模型预览器，也没有实现脚本反编译。
- 某些特殊纹理、模型、音频或代码容器需要单独的格式解析器才能进一步转换。
- 处理大型资源包时，手机需要预留输入缓存、导出文件和 ZIP 的额外空间。

## 在线构建

1. 将本工程提交到 GitHub 仓库。
2. 打开 Actions，运行 `Build NOTX UnZip Android APK`。
3. 成功后下载 `NeoXLab-Mobile-debug-apk` artifact，解压并安装 `app-debug.apk`。

## 构建环境

- Android Gradle Plugin 8.6.1
- Gradle 8.7
- Java 17
- Chaquopy 16.1.0 / Python 3.10
- Python packages: Pillow、zstandard

## 许可

请阅读 `LICENSE-NPK-X.txt`、`LICENSE-TOOLKIT.txt` 和 `THIRD_PARTY_NOTICES.md`，遵守原项目及第三方依赖许可。仅对你有权访问的资源进行研究和备份。
