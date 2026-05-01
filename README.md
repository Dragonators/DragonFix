# DragonFix

DragonFix 是一个面向 Minecraft 1.7.10 / Forge 的轻量级修复模组。

本项目主要用于 GT New Horizons 2.8.4 环境中，通过 Mixin 集中修复在使用新版 Angelica 以及若干自用附加模组时可能遇到的兼容性、渲染和服务端稳定性问题。

## 适用范围

- GT New Horizons 2.8.4 + Angelica 2.x
- MyCTMLib
- Programmable Hatches / 可编程舱室

新版 Angelica 可用于支持 Complementary Shaders 5.7.1 以及更多现代光影特性，但在旧版 1.7.10 模组生态中可能暴露额外兼容性问题。DragonFix 的目标就是把这些问题集中收敛到一个独立修复模组中。

## 当前修复

- Programmable Hatches / 可编程舱室
  - 修复 IO Hub 无法合成的问题。
  - 修复 IO Hub 无法被 OpenComputers 分析器识别节点的问题。
  - 修复 ME Stocking Dual Input Hatch 复制/粘贴配置时读写方向错误的问题。
  - 修复无线控制覆盖板从网络包读取数据时没有更新当前配置的问题。
- MyCTMLib
  - 修复 CTM 连接处可能出现黑色缝隙的问题。
- OpenComputers + Angelica
  - 修复新版 Angelica 下 OpenComputers 屏幕文字显示异常的兼容性问题。
- GalaxySpace
  - 修复下单 GalaxySpace 火箭相关物品时可能导致服务端 AE 崩溃的问题。

## 安装

将 DragonFix 放入客户端和服务端的 `mods` 目录。

建议仅在 GTNH 2.8.4 以及上述组件组合下使用。若服务端安装了 DragonFix，客户端也应安装相同版本，以避免 Mixin 和资源环境不一致。

## 构建

```bash
./gradlew build
```

如果修改了 Mixin、资源、`mcmod.info` 或版本相关配置，优先使用：

```bash
./gradlew clean build
```

构建产物位于 `build/libs/`，文件名由 GTNHGradle 和 Git tag 派生，例如 `DragonFix-0.5.3.jar`。

## 版本发布

项目启用 GTNHGradle 的 Git tag 版本：

```properties
gtnh.modules.gitVersion = true
```
发布新版本时创建并推送 tag：

```bash
git tag 0.6.0
git push origin main
git push origin 0.6.0
```

仓库包含 GitHub Actions 配置：

- `.github/workflows/build-and-test.yml`: push 或 pull request 时自动构建测试。
- `.github/workflows/release-tags.yml`: 推送 tag 时自动构建并创建 GitHub Release。
