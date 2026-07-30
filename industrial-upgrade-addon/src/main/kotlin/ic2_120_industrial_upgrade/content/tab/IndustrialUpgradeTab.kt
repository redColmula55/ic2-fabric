package ic2_120_industrial_upgrade.content.tab

import ic2_120.registry.annotation.ModCreativeTab

/**
 * 工业升级附属创造物品栏。
 *
 * 图标使用工业升级最高级的中子太阳能发电机（最有辨识度）。
 * name 必须对应 core 的 CreativeTab 枚举值 INDUSTRIAL_UPGRADE（id = "industrial_upgrade"）。
 */
@ModCreativeTab(name = "industrial_upgrade", iconItem = "neutron_solar_panel")
object IndustrialUpgradeTab
