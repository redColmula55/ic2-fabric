package ic2_120

import ic2_120.client.ArmorKeybinds
import ic2_120.client.ArmorTooltipHandler
import ic2_120.client.AnimalmatronTooltipHandler
import ic2_120.client.DrillTooltipHandler
import ic2_120.client.FoamSprayerTooltipHandler
import ic2_120.client.MiningLaserTooltipHandler
import ic2_120.client.ModeKeybinds
import ic2_120.client.BatteryModelPredicates
import ic2_120.client.ModItemTooltip
import ic2_120.client.ModFluidClient
import ic2_120.client.ClientBlockRenderLayers
import ic2_120.client.ClientEntityRenderers
import ic2_120.client.ClientScreenRegistrar
import ic2_120.client.colorprovider.FluidCellColorProvider
import ic2_120.client.RubberLogModelPlugin
import ic2_120.client.colorprovider.StorageBoxColorProvider
import ic2_120.client.colorprovider.PipeColorProvider
import ic2_120.client.PainterModelPredicates
import ic2_120.client.PeatOreTooltipHandler
import ic2_120.client.QuantumLeggingsSpeedController
import ic2_120.client.ClientBlockEntityRenderers
import ic2_120.client.JetpackSoundController
import ic2_120.client.MachineLoopSoundController
import ic2_120.client.IridiumDrillModeHandler
import ic2_120.client.ChainsawModeHandler
import ic2_120.client.MiningLaserModeHandler
import ic2_120.client.SodiumCompatibilityWarning
import ic2_120.client.WindMeterClientInitializer
import ic2_120.client.network.NetworkManager
import ic2_120.analytics.AnalyticsClientReporter
import ic2_120.integration.jei.ClientLiveRecipeSource
import ic2_120.integration.jei.LiveRecipeSource
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient

object Ic2_120Client : ClientModInitializer {
	override fun onInitializeClient() {
		// 注入客户端 RecipeManager 提供者，供 JEI plugin 收集运行时实际加载的配方
		// （与机器判定逻辑同源，覆盖 core + 所有附属命名空间）
		LiveRecipeSource.instance = ClientLiveRecipeSource

		// 注入客户端主线程调度器：JEI RecipeManager 操作必须在客户端主线程执行，
		// 而 UU 索引重建由 SERVER_STARTED 在 Server thread 触发 refreshReplicatorRecipes，
		// 需经此切回主线程，否则 ErrorUtil.assertMainThread 抛异常导致进世界必崩。
		//
		// 无条件注入（不判 isModLoaded("jei")）：本 lambda 只用 MinecraftClient，不引用任何 JEI 类；
		// 真正引用 JEI 类的是 refreshReplicatorRecipes 方法体，已由 jeiRuntime==null 兑底。
		// 在 Forge+Sinytra Connector 环境下，jei 是 Forge 原生 mod，FabricLoader 视野里可能
		// 查不到 "jei"（Ic2_120.kt 的 onRebuild 回调却查得到，行为不一致），导致本该注入的
		// 调度器被跳过 → refreshReplicatorRecipes 同步在 Server thread 调 hideRecipes 而崩溃。
		ic2_120.integration.jei.Ic2JeiPlugin.scheduleOnClientThread = { action ->
			val client = MinecraftClient.getInstance()
			if (client.isOnThread) false else { client.execute(action); true }
		}

		ModFluidClient.register()
		ClientScreenRegistrar.registerScreens(Ic2_120.MOD_ID, listOf("ic2_120.client"))
		ModItemTooltip.register()
		ClientEntityRenderers.register()
		ClientBlockEntityRenderers.register()
		ClientBlockRenderLayers.register()
		BatteryModelPredicates.register() // 注册电池模型 predicate

		// 注册网络管理器
		NetworkManager.register()

		// 匿名使用统计：客户端每次加入世界上报一次（每会话一次）
		AnalyticsClientReporter.register()
		ModeKeybinds.register()
		ArmorKeybinds.register()
		QuantumLeggingsSpeedController.register()
		ArmorTooltipHandler.register()
		AnimalmatronTooltipHandler.register()
		DrillTooltipHandler.register()
		IridiumDrillModeHandler.register()
		ChainsawModeHandler.register()
		MiningLaserModeHandler.register()
		FoamSprayerTooltipHandler.register()
		MiningLaserTooltipHandler.register()
		PeatOreTooltipHandler.register()
		// 手动发版阶段：UpdateNotifier 依赖 CI 的 GITHUB_RUN_NUMBER 注入版本号，
		// 本地/手动构建时恒为 0，导致更新检查永远不触发（ciRunNumber<=0 直接 return）。
		// 暂时禁用，待迁移到语义版本号比较后重新启用。
		// UpdateNotifier.register()
		SodiumCompatibilityWarning.register()
		JetpackSoundController.register()
		MachineLoopSoundController.register()
		WindMeterClientInitializer.init()

		// 注册储物箱着色器
		StorageBoxColorProvider.register()
		// 注册通用流体单元着色器（流体颜色渲染到中心）
		FluidCellColorProvider.register()
		// 橡胶树原木动态模型（替代 243 个 blockstate 变体）
		RubberLogModelPlugin.register()
		// 注册管道着色器（青铜和碳纤维材质）
		PipeColorProvider.register()
		PainterModelPredicates.register()
	}
}
