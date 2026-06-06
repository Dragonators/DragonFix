# DragonFix

DragonFix 是一个面向 Minecraft 1.7.10 / Forge 的轻量级修复模组。

本项目主要用于 GT New Horizons 2.8.4 环境中，通过 Mixin 集中修复在使用新版 Angelica 以及若干自用附加模组时可能遇到的兼容性、渲染和服务端稳定性问题。

## 适用范围

- GT New Horizons 2.8.4 + Angelica 2.x
- MyCTMLib
- Programmable Hatches / 可编程舱室

新版 Angelica 可用于支持 Complementary Shaders 5.7.1 以及更多现代光影特性，但在旧版 1.7.10 模组生态中可能暴露额外兼容性问题。DragonFix 的目标就是把这些问题集中收敛到一个独立修复模组中。

## 当前修复

- Minecraft / Forge network
  - 仅 `DragonFixMM` 通道使用 Forge VarShort 长度编码放宽客户端自定义载荷包的 32 KiB 限制；其他模组通道仍保持原版/Forge 行为。

- Programmable Hatches / 可编程舱室
  - 修复 IO Hub 无法合成的问题。
  - 修复 IO Hub 无法被 OpenComputers 分析器识别节点的问题。
  - 修复 ME Stocking Dual Input Hatch 复制/粘贴配置时读写方向错误的问题。
  - 修复 ME Stocking Dual Input Hatch 无法通过 Data Stick 保存和加载完整配置的问题，现在允许使用潜行左键复制配置本身
  - 修复无线控制覆盖板无法保存配置的问题。
- MyCTMLib
  - 修复 CTM 连接处可能出现黑色缝隙的问题。
- OpenComputers + Angelica
  - 修复新版 Angelica 下 OpenComputers 屏幕文字显示异常的兼容性问题。
  - 修复客户端收到 OpenComputers 屏幕调色板颜色同步包时，因本地缓冲区仍为 1-bit 色深而记录 `color palette not supported` 坏包警告的问题。
- Angelica
  - 修复部分光影包重复记录 `angelica_ClipPlanesEnabled` 外部 uniform 类型不匹配错误的问题。
  - 修复部分光影包自定义 uniform 与 Angelica 内置 uniform 重名时重复打印 `Variable shadows build in uniform` 异常栈的问题。
  - 修复开启光影时，光影包自带的选择框描边功能无法接管 Angelica 选择框渲染的问题。
  - 修复 Angelica 2.1.30 自动扫描 emissive 贴图时，遇到资源名包含 Windows 非法路径字符会中断资源加载的问题。
  - 修复 CTM `matchTiles` 中带路径的图标名只按 `.png` 资源路径注册，无法命中运行时 exact icon name 的兼容性问题。
- GalaxySpace
  - 修复下单 GalaxySpace 火箭相关物品时可能导致服务端 AE 崩溃的问题。
- AE2
  - 修复网络中某物品总数超过 long 上限时计数溢出为负数的问题。
- AE2Things
  - 修复 GT 配方通过无线双接口终端追加非消耗品信息时，非集成电路物品只显示 damage 数字而无法区分物品名称的问题。
- GTNHLib
  - 修复使用 GTNHLib 0.9.x（Angelica 2.x 必需版本）时，物质操纵者无法渲染选区的问题。
  - 修复旧版 `QuadView` 调用 GTNHLib `VertexFormat.writeQuad` 时可能因 CEL 包名迁移导致的二进制兼容问题。
- MatterManipulator
  - 为 MatterManipulator 新增 LittleTiles 方块复制、旋转和建造时的 tile 数据适配。
  - 为 Carpenter's Blocks 方块补充复制、旋转和建造时的 data/covers 适配。
  - 为 ForgeMultipart / Project Red multipart 方块补充复制、旋转、物品消耗、建造和预览适配。
  - 修复 Forge Microblocks 的 ForgeMultipart 容器方块在区域分析阶段被当作跳过方块的问题；同时让 multipart 在普通支撑方块之后建造。
  - 生存模式下调整 Forge Microblocks 按免费结构恢复，ArchitectureCraft shape item 与切割基础材料本身免费但仍会消耗/返还真实嵌入材料。
  - 修复 Biomes O' Plenty 植物在 MatterManipulator 建造预检流程中的问题，包括代理世界缺少 provider 时可能崩溃的问题。
  - 修复 Biomes O' Plenty 睡莲等水面植物在粘贴中放置成掉落物的问题。
  - 修复 OpenComputers 可旋转方块复制/粘贴时朝向未按 MM 变换同步的问题。
  - 修复 OpenComputers 组件带地址或数据 NBT 时，复制/粘贴库存可能错误要求原始带地址物品的问题。
  - 修复 OpenComputers 无法粘贴 APU 系列组件的问题。
  - 修复 MatterManipulator 生存模式粘贴 OpenComputers 微控制器时，因要求微控制器物品 NBT 完全一致而找不到等价物品的问题；现在微控制器按同等级机箱与等价内部组件消耗，组件数据由粘贴结果恢复。
  - 修复 Avaritiaddons 梦魇工作台右侧标记区的复制/恢复。
  - 修复 AE2 物质聚合器存储组件槽位的复制/恢复。
  - 修复 Ender IO 灵魂绑定器电容槽位的复制/恢复。
  - 修复 OpenComputers 微控制器在 MatterManipulator 持久选区粘贴时组件列表未同步写回方块物品，或组件使用不稳定数字物品 ID 导致 CPU/内存等组件丢失的问题。
  - 修复 LittleTiles 与 ArchitectureCraft 方块在 MatterManipulator 预览提示中无法按实际材料/小方块形状显示的问题。
  - 修复 ArchitectureCraft 真实形状预览可能超出 MatterManipulator 原始 hint VBO 预分配容量，导致整批预览不渲染的问题。
  - 修复 ArchitectureCraft 方块在 MatterManipulator 复制结构旋转/镜像后未同步更新朝向与偏移的问题。
  - 修复 MatterManipulator 对门进行复制粘贴处理中的问题。
  - 修复 MalisisDoors 自定义门在复制粘贴时缺少门材质 NBT 的问题。
  - 修复带小数角度的方块属性旋转/镜像时无法解析的问题。
  - 修复粘贴区域与已完成区域大面积重合时，MatterManipulator 会在单次建造调用中扫描大量已完成方块导致长时间卡顿的问题。
  - 修复 AE2 编码样板在 MatterManipulator 持久选区文件中使用不稳定数字物品 ID，导致跨存档粘贴后样板无效的问题。
  - 修复 MatterManipulator 安装 AE2 升级卡时可能把已提取的升级卡数量归零，导致 ME 接口、存储总线等方块或部件无法恢复升级卡的问题。
  - 为 MatterManipulator 复制/粘贴水和岩浆源方块增加支持，并在生存模式下从本次建造已回收的流体、AE 流体、液滴或桶中消耗对应流体；同时保存流动水/岩浆用于预览，但不实际粘贴非源流体。
  - 修复 MatterManipulator 持久粘贴加载的同名 `.mmschematic` 被持久复制覆盖后，客户端仍保留旧预览渲染缓存的问题。
  - 复用 MatterManipulator 选区 BoxRenderer 的 VBO，减少每帧渲染选区时的缓冲对象创建。
  - 修复 MatterManipulator 坐标/变换编辑界面在复制或粘贴选区不完整时按住 Ctrl 调整坐标会因缺少可计算区域尺寸而崩溃的问题。
  - 增加 MatterManipulator 持久选区文件功能，可将分析后的 `PendingBlock` 数据保存为 `.mmschematic` 并跨存档加载粘贴。

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
