你正在维护一个 Minecraft 1.7.10 Forge 模组项目：DragonFix。

项目定位：
DragonFix 是一个独立的轻量级修复型模组，类似 RandomFix，用于集中放置多个针对旧版 Minecraft 1.7.10 模组生态的兼容性、稳定性、服务端崩溃修复。未来会持续加入更多 mod 的小型修复。

当前项目状态：
- 模组名：DragonFix
- modId：dragonfix
- Java 根包：com.dragonfix
- 主类：com.dragonfix.DragonFix
- Mixin 配置：src/main/resources/mixins.dragonfix.json
- 项目使用 GTNHGradle / Forge 1.7.10 构建体系。
- 版本号由 Git tag 决定，已启用：
  - gtnh.modules.gitVersion = true
- 不要在 build.gradle.kts 中硬编码 modVersion。
- 发布版本应通过 Git tag 生成，例如 0.1.0、0.2.0、1.0.0。

开发要求：
1. 所有新增修复都应尽量小而清晰，避免大范围重构。
2. 每个修复应优先使用 Mixin，除非 Forge 事件或普通初始化逻辑更合适。
3. 新增 Mixin 放在：
  - com.dragonfix.mixin.mixins
  - 并按目标 mod 建立子包，例如 com.dragonfix.mixin.mixins.opencomputers
4. 若需要按目标 mod 是否存在来控制 Mixin，应优先扩展：
  - com.dragonfix.mixin.MixinPlugin
5. Mixin 方法前缀使用 dragonfix$，避免与目标类方法冲突。
6. 需要访问目标类非公开字段或方法时，优先使用 Mixin @Accessor / @Invoker，不要直接使用反射。
7. 如果目标方法是公开稳定 API，应优先直接调用原方法，并通过 compileOnly 补齐必要的签名依赖。
8. 只有在目标类/成员必须动态发现、Mixin 无法稳定描述目标、或为了保持可选目标的类加载边界时才使用反射；使用反射时优先考虑 MethodHandle / VarHandle，尽量避免热路径上的 Method.invoke / Field.get。
9. 新增针对某个 mod 的 Mixin 修复时，必须同步在 com.dragonfix.DragonFix 的 @Mod dependencies 中增加对应 modId 与最低兼容版本：
  - 必选目标使用 required-after:<modid>@[version,)
  - 可选目标使用 after:<modid>@[version,)
  - Forge/FML 检查的是目标 mod 在 @Mod 或 mcmod.info 中声明的版本，不是 Maven/JitPack/GitHub tag 坐标。
10. 新增可选目标 mod 依赖时，放入 dependencies.gradle，优先使用 compileOnly，除非运行测试确实需要 runtimeOnlyNonPublishable。
11. 不要重新引入 ExampleMod、GalaFix、mymodid、myname 等模板占位内容。
12. 不要把客户端专用类直接用于服务端路径，特别注意 dedicated server 兼容性。
13. 每次修改后至少运行：
  - ./gradlew build
14. 如果修改 Mixin、资源、mcmod.info 或版本配置，优先运行：
- ./gradlew clean build
15. 每次增加 Mixin 修复时，必须同步更新 README.md 的当前修复列表。
16. 发布或推送修复时，必须确认目标 Git tag 指向最终提交，并同步推送 main 与 tag 到远程。
17. 若为第三方类通过 Mixin 增加会影响持久化、传输、hashCode/equals、clone 或 migrate 的字段，必须同步检查并更新对应的显式序列化/反序列化逻辑，例如 PersistentSchematic 中的手写 Gson TypeAdapter，避免新增字段被静默遗漏。

代码风格：
- 使用 Java。
- 保持包名为 com.dragonfix。
- 保持 ASCII，除非已有文件明确需要非 ASCII。
- 注释只解释不明显的兼容性原因，不要写模板化废话。
- 不要保留无用的示例配置、示例 proxy 或 Hello World 逻辑。

版本与 Git：
- 当前仓库应是 DragonFix 独立仓库，不应继承 ExampleMod 历史标签。
- 构建产物名应类似：
  - DragonFix-0.1.0.jar
- 若需要发布新版本：
  1. 提交修改
  2. 创建 Git tag，例如 git tag 0.2.0
  3. 推送 main 和 tag

你的任务：
根据用户提出的新修复需求，先分析目标崩溃或行为问题，确认目标类、方法、客户端/服务端环境，然后以最小改动实现修复。完成后说明修改了哪些文件、修复了什么问题、如何验证。
