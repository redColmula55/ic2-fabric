# mcdebug Kotlin 测试指南

mcdebug 测试是 **Kotlin + JUnit 5** 的服务器内黑盒集成测试，运行在
`core/src/mcdebugTest/kotlin/`（旧 TS 版测试与 `pnpm`/`tsx` 工具链已随
`c9352bd7` 移除）。通过 `@yu1745/mcdebug`（JitPack v0.5.0）的 RPC 接口驱动
一个真实的 dev MC 服务器，产物是标准 JUnit 报告。

依赖（`core/build.gradle`）：

- `modRuntimeOnly("com.github.yu1745:mcdebug:v0.5.0")` — 游戏内 mcdebug mod；
- `mcdebugTestImplementation "com.github.yu1745.mcdebug:mcdebug-cli:0.5.0"` —
  `com.mcdebug.runner`（`@McDebugTest` / `TestContext` / 扩展函数）与
  JUnit 5 扩展；
- JUnit Jupiter 5.10.2 + junit-platform-launcher。

## Run

```bash
./gradlew :core:mcdebugTest
```

`mcdebugTest` 任务（`core/build.gradle`）做的事：

1. 复用 Loom 已配好的 `runServer` 启动参数（main class / classpath / JVM args），
   fork 一个 `java` 子进程拉起 dev server（`--nogui`），工作目录 `core/run`；
2. 轮询 `core/run/mcdebug/port` socket 发现文件（120s 超时，超时抛异常并给出
   server.log 路径）；
3. 在测试 JVM（工作目录 `core/`）里跑 JUnit 5，socket 自动命中端口；方法级并行
   由 mcdebug-cli 自带的 `junit-platform.properties` 控制
   （`parallel.mode.default=concurrent`，对齐旧 TS runner 的并行网格）；
4. 无论成败都走 `stopMcdebugServer` finalizer 停服，避免残留 `session.lock`。

服务器日志：`core/build/mcdebugTest/server.log`。

## 结构

- 测试类加 `@McDebugTest` 注解（`com.mcdebug.runner`），由 `McDebugExtension`
  注入 `TestContext`；测试方法是标准 JUnit 5：`@Test fun \`name\`(ctx: TestContext)`。
- `ctx.origin` = 待测机器位置；`ctx.pos(dx, dy, dz)` = 相对坐标。
- 常用扩展函数：`place` / `setBlocks` / `setBeField` / `setSlot` / `insertItem` /
  `assertBlockId` / `assertSlotCount` / `assertSlotEmpty` / `assertSlotHas`。
- **驱动方式**（消除 flaky 的关键）：
  - 机器内部逻辑（配方进度、能量获取）用 `ctx.api.be.tick(origin, ticks)`
    确定性驱动——毫秒级，走同一 BlockEntityTicker 路径，不受并行负载影响；
  - 依赖邻居/世界 tick 的场景（能量网络、红石）才用
    `waitUntil(ctx, predicate, timeoutTicks)` 等自然 tick。
- 共享 helper 在 `Helpers.kt`：`setupAdjacentBatbox`（东侧 BatBox 面朝西供电 +
  预充 40 000 EU + 放置机器）、`setHeat`（32 位热量写入 `Heat_Low` / `Heat_High`
  两个 16 位字段）、`batboxEast`。

## 测试矩阵

每台机器至少覆盖：

- placement sanity
- 一条经典成功配方
- 无电 / 无燃料空转
- 非法输入空转
- 输出满 / 阻塞输出（如适用）

供电类机器把隐藏约束写进 setup helper：储能等级、朝向、变压器/超频升级、槽位号。

## 槽位约定

槽位布局与对应 BlockEntity 的 `SLOT_*` 常量保持一致（如 Macerator：slot 0 输入、
slot 1 输出、slot 2 放电、slot 3+ 升级）。测试里写死槽位号前先核对
`*BlockEntity` 的常量（参考 `MaceratorTest` 顶部注释的示例）。
