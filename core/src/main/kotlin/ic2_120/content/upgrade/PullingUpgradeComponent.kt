package ic2_120.content.upgrade

import ic2_120.content.item.PullingUpgrade
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.item.Item
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

object PullingUpgradeComponent {

    /** 静态全方向列表，避免每次调用重新分配 Direction.values().toList() */
    private val ALL_DIRECTIONS: List<Direction> = Direction.values().toList()

    /**
     * 扫描升级槽中的所有抽入升级，从相邻容器抽取物品到机器的输入槽。
     * 每个抽入升级使用自己的过滤和方向配置。
     * 轮询语义（对齐 ic2_origin）：每 tick 遍历全部 n 个候选方向，每个候选至多抽取
     * itemTransferRate(count) 个物品（配额跨输入槽共享）；每 tick 起始方向轮转一次，
     * 避免低吞吐时第一个候选独占。开启方向过滤时，轮转只在过滤后的方向集内进行。
     * 复用 EjectorUpgradeComponent 的 NBT 读写方法（过滤和方向配置格式一致）。
     */
    fun pullIfUpgraded(
        world: World,
        pos: BlockPos,
        inventory: Inventory,
        upgradeSlotIndices: IntArray,
        inputSlotIndices: IntArray
    ) {
        if (inputSlotIndices.isEmpty()) return

        // 预解析每个抽入升级的有效方向与速率，并收集全部需要查找的方向——
        // 多个抽入升级共享同一方向的 capability 查找结果，避免重复 find。
        val active = mutableListOf<Triple<Int, Item?, List<Direction>>>()  // (rate, filter, dirs)
        val neededDirs = LinkedHashSet<Direction>()
        for (idx in upgradeSlotIndices) {
            val upgradeStack = inventory.getStack(idx)
            if (upgradeStack.isEmpty || upgradeStack.item !is PullingUpgrade) continue

            val rate = EjectorUpgradeComponent.itemTransferRate(upgradeStack.count)
            if (rate <= 0) continue
            val filter = EjectorUpgradeComponent.readFilter(upgradeStack)
            val configuredSides = EjectorUpgradeComponent.readDirections(upgradeStack)
            val dirs = if (configuredSides.isEmpty()) {
                ALL_DIRECTIONS
            } else {
                ALL_DIRECTIONS.filter { it in configuredSides }
            }
            if (dirs.isEmpty()) continue
            active.add(Triple(rate, filter, dirs))
            neededDirs.addAll(dirs)
        }
        if (active.isEmpty()) return

        // 一次性查找所有需要方向的源容器（内容实时读取，引用可复用）
        val sources = HashMap<Direction, Storage<ItemVariant>?>(neededDirs.size)
        for (dir in neededDirs) {
            sources[dir] = ItemStorage.SIDED.find(world, pos.offset(dir), dir.opposite)
        }

        for ((rate, filter, dirs) in active) {
            // 每 tick 轮转起始方向，使 n 个候选轮流获得优先服务
            val start = Math.floorMod(world.time, dirs.size.toLong()).toInt()
            for (i in 0 until dirs.size) {
                val dir = dirs[(start + i) % dirs.size]
                val source = sources[dir] ?: continue

                // 该候选本次 tick 的配额，跨所有输入槽共享（对齐原版 transfer(amount) 语义）
                var remainingQuota = rate
                // 遍历源容器中的所有物品变体
                for (view in source) {
                    if (remainingQuota <= 0) break
                    if (view.isResourceBlank()) continue
                    val variant = view.resource
                    val item = variant.item

                    // 应用过滤
                    if (filter != null && item != filter) continue

                    // 找到可以接受此物品的输入槽
                    for (slotIndex in inputSlotIndices) {
                        if (remainingQuota <= 0) break
                        val current = inventory.getStack(slotIndex)
                        if (!current.isEmpty) {
                            if (current.item != item) continue
                            if (current.count >= current.maxCount) continue
                        }

                        // 检查机器是否接受该物品
                        if (!inventory.isValid(slotIndex, variant.toStack(1))) continue

                        val space = if (current.isEmpty) current.maxCount else current.maxCount - current.count
                        if (space <= 0) continue

                        // 受每候选配额限制，从源容器中抽取物品
                        val amount = minOf(remainingQuota.toLong(), space.toLong(), view.amount).toInt()
                        if (amount <= 0) continue

                        Transaction.openOuter().use { tx ->
                            val extracted = source.extract(variant, amount.toLong(), tx)
                            if (extracted > 0) {
                                tx.commit()
                                remainingQuota -= extracted.toInt()
                                // 将物品放入输入槽
                                if (current.isEmpty) {
                                    inventory.setStack(slotIndex, variant.toStack(extracted.toInt()))
                                } else {
                                    val newStack = current.copy()
                                    newStack.count += extracted.toInt()
                                    inventory.setStack(slotIndex, newStack)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
