package ic2_120.content.upgrade

import ic2_120.content.item.EjectorUpgrade
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import kotlin.math.pow

object EjectorUpgradeComponent {
    private const val NBT_ITEM_FILTER = "PipeItemFilter"
    private const val NBT_DIRECTION = "PipeItemDirection"
    private const val NBT_DIRECTIONS = "PipeItemDirections"

    private data class EjectorConfig(val filter: Item?, val sides: Set<Direction>, val count: Int)

    /**
     * 对齐 ic2_origin：物品传输速率 = 4^(min(count, 4) - 1) 个/tick/候选，count=0 时返回 0。
     */
    fun itemTransferRate(upgradeCount: Int): Int {
        if (upgradeCount <= 0) return 0
        val capped = minOf(upgradeCount, 4)
        return 4.0.pow(capped - 1).toInt()
    }

    /**
     * 统一入口：扫描升级槽中的所有弹出升级，逐个独立弹出 outputSlotIndices 中的物品。
     * 每个弹出升级使用自己的过滤和方向配置。
     * 轮询语义（对齐 ic2_origin）：每 tick 遍历全部 n 个候选方向，每个候选至多传输
     * itemTransferRate(count) 个物品（配额跨输出槽共享）；每 tick 起始方向轮转一次，
     * 避免低吞吐时第一个候选独占。开启方向过滤时，轮转只在过滤后的方向集内进行。
     * 使用 Fabric Transfer API 查找目标容器，兼容 vanilla Inventory 和 modded Storage。
     */
    fun ejectIfUpgraded(
        world: World,
        pos: BlockPos,
        inventory: Inventory,
        upgradeSlotIndices: IntArray,
        outputSlotIndices: IntArray
    ) {
        if (outputSlotIndices.isEmpty()) return

        val configs = mutableListOf<EjectorConfig>()
        for (idx in upgradeSlotIndices) {
            val stack = inventory.getStack(idx)
            if (stack.isEmpty) continue
            if (stack.item is EjectorUpgrade) {
                configs.add(EjectorConfig(readFilter(stack), readDirections(stack), stack.count))
            }
        }
        if (configs.isEmpty()) return

        for (config in configs) {
            val rate = itemTransferRate(config.count)
            if (rate <= 0) continue
            val dirs = if (config.sides.isEmpty()) {
                Direction.values().toList()
            } else {
                Direction.values().filter { it in config.sides }
            }
            if (dirs.isEmpty()) continue

            // 每 tick 轮转起始方向，使 n 个候选轮流获得优先服务
            val start = Math.floorMod(world.time, dirs.size.toLong()).toInt()
            for (i in 0 until dirs.size) {
                val dir = dirs[(start + i) % dirs.size]
                val target = ItemStorage.SIDED.find(world, pos.offset(dir), dir.opposite) ?: continue

                // 该候选本次 tick 的配额，跨所有输出槽共享（对齐原版 transfer(amount) 语义）
                var remainingQuota = rate
                for (slotIndex in outputSlotIndices) {
                    if (remainingQuota <= 0) break
                    val stack = inventory.getStack(slotIndex)
                    if (stack.isEmpty) continue
                    if (config.filter != null && stack.item != config.filter) continue

                    val variant = ItemVariant.of(stack)
                    val move = minOf(remainingQuota.toLong(), stack.count.toLong())
                    val tx = Transaction.openOuter()
                    val moved = target.insert(variant, move, tx)
                    tx.commit()
                    if (moved <= 0) continue

                    remainingQuota -= moved.toInt()
                    val left = stack.count - moved.toInt()
                    if (left <= 0) {
                        inventory.setStack(slotIndex, ItemStack.EMPTY)
                    } else {
                        val newStack = stack.copy()
                        newStack.count = left
                        inventory.setStack(slotIndex, newStack)
                    }
                }
            }
        }
    }

    fun readFilter(stack: ItemStack): Item? {
        val nbt = stack.nbt ?: return null
        val raw = nbt.getString(NBT_ITEM_FILTER)
        if (raw.isNullOrBlank()) return null
        val id = Identifier.tryParse(raw) ?: return null
        return if (Registries.ITEM.containsId(id)) Registries.ITEM.get(id) else null
    }

    fun writeFilter(stack: ItemStack, item: Item?) {
        val nbt = stack.orCreateNbt
        if (item == null) {
            nbt.remove(NBT_ITEM_FILTER)
            return
        }
        val id = Registries.ITEM.getId(item)
        if (id.path != "air") nbt.putString(NBT_ITEM_FILTER, id.toString())
        else nbt.remove(NBT_ITEM_FILTER)
    }

    fun readDirection(stack: ItemStack): Direction? {
        return readDirections(stack).singleOrNull()
    }

    fun writeDirection(stack: ItemStack, side: Direction?) {
        writeDirections(stack, if (side == null) emptySet() else setOf(side))
    }

    /** 空集合表示任意方向；同时兼容旧版本的单方向 NBT。 */
    fun readDirections(stack: ItemStack): Set<Direction> {
        val nbt = stack.nbt ?: return emptySet()
        val list = nbt.getList(NBT_DIRECTIONS, net.minecraft.nbt.NbtElement.STRING_TYPE.toInt())
        if (!list.isEmpty()) {
            return list.mapNotNull { Direction.byName(it.asString()) }.toSet()
        }

        val raw = nbt.getString(NBT_DIRECTION)
        if (raw.isNullOrBlank()) return emptySet()
        return Direction.byName(raw.lowercase())?.let { setOf(it) } ?: emptySet()
    }

    fun writeDirections(stack: ItemStack, sides: Set<Direction>) {
        val nbt = stack.orCreateNbt
        nbt.remove(NBT_DIRECTION)
        if (sides.isEmpty()) {
            nbt.remove(NBT_DIRECTIONS)
            nbt.remove(NBT_DIRECTION)
            return
        }
        val list = net.minecraft.nbt.NbtList()
        for (side in sides) {
            list.add(net.minecraft.nbt.NbtString.of(side.name.lowercase()))
        }
        nbt.put(NBT_DIRECTIONS, list)
    }

    fun nextDirection(current: Direction?): Direction? {
        return when (current) {
            null -> Direction.DOWN
            Direction.DOWN -> Direction.UP
            Direction.UP -> Direction.NORTH
            Direction.NORTH -> Direction.SOUTH
            Direction.SOUTH -> Direction.WEST
            Direction.WEST -> Direction.EAST
            Direction.EAST -> null
        }
    }
}
