package ic2_120_industrial_upgrade

import ic2_120.client.ClientScreenRegistrar
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import ic2_120_industrial_upgrade.content.fluid.NeutronFluid
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

object IC2IndustrialUpgradeClient : ClientModInitializer {
    override fun onInitializeClient() {
        // 1. 扫描 @ModScreen 注解类，注册到 HandledScreens（中子制造机 GUI 等）
        ClientScreenRegistrar.registerScreens(
            IC2IndustrialUpgrade.MOD_ID,
            listOf("ic2_120_industrial_upgrade.client.screen")
        )

        // 2. 注册中子流体的客户端渲染（贴图 + 半透明渲染层）
        val stillId = Identifier(IC2IndustrialUpgrade.MOD_ID, "block/fluid/neutron_still")
        val flowId = Identifier(IC2IndustrialUpgrade.MOD_ID, "block/fluid/neutron_flow")
        FluidRenderHandlerRegistry.INSTANCE.register(
            NeutronFluid.NEUTRON_STILL,
            NeutronFluid.NEUTRON_FLOWING,
            SimpleFluidRenderHandler(stillId, flowId)
        )
        BlockRenderLayerMap.INSTANCE.putFluids(
            RenderLayer.getTranslucent(),
            NeutronFluid.NEUTRON_STILL,
            NeutronFluid.NEUTRON_FLOWING
        )
    }
}
