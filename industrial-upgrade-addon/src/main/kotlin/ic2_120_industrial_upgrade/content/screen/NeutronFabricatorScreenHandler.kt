package ic2_120_industrial_upgrade.content.screen

import ic2_120.content.item.EmptyCell
import ic2_120.content.item.FluidCellItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import ic2_120_industrial_upgrade.content.block.NeutronFabricatorBlockEntity
import ic2_120_industrial_upgrade.content.sync.NeutronFabricatorSync
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.Direction

@ModScreenHandler(names = ["neutron_fabricator"])
class NeutronFabricatorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext,
    propertyDelegate: PropertyDelegate,
    blockInventory: Inventory? = null,
    private val machineSlotCount: Int = 0
) : ScreenHandler(NeutronFabricatorScreenHandler::class.type(), syncId) {

    /** 客户端通过 SyncedDataView 包装 propertyDelegate 读取同步字段，供 Screen 渲染使用 */
    val sync = NeutronFabricatorSync(
        schema = SyncedDataView(propertyDelegate),
        capacity = 1L,
        tier = 1,
        getFacing = { Direction.NORTH },
        currentTickProvider = { null }
    )

    /** 升级槽：仅接受 UpgradeItemRegistry 注册且本机实现了对应接口的升级（与物质生成机一致） */
    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    companion object {
        private const val SLOT_SIZE = 18
        // 升级槽：贴图左侧 152 列，纵向 8/26/44/62（对齐物质生成机的 4 个升级格子）
        private const val UPGRADE_SLOT_X = 152
        private val UPGRADE_SLOT_YS = intArrayOf(8, 26, 44, 62)
        // 容器输入/输出槽：贴图右侧（125 列）
        private const val CONTAINER_INPUT_X = 125
        private const val CONTAINER_INPUT_Y = 23
        private const val CONTAINER_OUTPUT_Y = 59
        // 玩家背包
        private const val PLAYER_INV_X = 8
        private const val PLAYER_INV_Y = 84
        private const val HOTBAR_Y = 142

        /** 容器输入槽：接受空桶 / 空单元 / 流体单元（具体是否可装由 BlockEntity 服务端逻辑判定） */
        private val containerInputSpec = SlotSpec(
            canInsert = { stack ->
                val item = stack.item
                item == net.minecraft.item.Items.BUCKET ||
                    item is EmptyCell ||
                    item is FluidCellItem
            }
        )

        @ScreenFactory
        @JvmStatic
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): NeutronFabricatorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val propertyDelegate = ArrayPropertyDelegate(propertyCount)
            // 客户端用临时 SimpleInventory 占位（真实数据由服务端同步）
            val inv = SimpleInventory(NeutronFabricatorBlockEntity.INVENTORY_SIZE)
            return NeutronFabricatorScreenHandler(syncId, playerInventory, context, propertyDelegate, inv, NeutronFabricatorBlockEntity.INVENTORY_SIZE)
        }
    }

    init {
        addProperties(propertyDelegate)

        if (blockInventory != null) {
            checkSize(blockInventory, NeutronFabricatorBlockEntity.INVENTORY_SIZE)
            // 升级槽（BlockEntity slot 0-3）：贴图左侧 152 列，纵向 8/26/44/62
            for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
                addSlot(PredicateSlot(blockInventory, NeutronFabricatorBlockEntity.SLOT_UPGRADE_INDICES[i], UPGRADE_SLOT_X, UPGRADE_SLOT_YS[i], upgradeSlotSpec))
            }
            // 容器输入槽（BlockEntity slot 4）：125, 23
            addSlot(PredicateSlot(blockInventory, NeutronFabricatorBlockEntity.SLOT_CONTAINER_INPUT, CONTAINER_INPUT_X, CONTAINER_INPUT_Y, containerInputSpec))
            // 容器输出槽（BlockEntity slot 5）：125, 59 —— 禁物流插入（仅配方产物落槽），否则 shift+左键可污染输出
            addSlot(PredicateSlot(blockInventory, NeutronFabricatorBlockEntity.SLOT_CONTAINER_OUTPUT, CONTAINER_INPUT_X, CONTAINER_OUTPUT_Y, SlotSpec(canInsert = { false })))
        }
        // 玩家背包（3×9）
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * SLOT_SIZE, PLAYER_INV_Y + row * SLOT_SIZE))
            }
        }
        // 快捷栏（9）
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * SLOT_SIZE, HOTBAR_Y))
        }
    }

    private val hotbarEnd: Int get() = machineSlotCount + 36

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        if (index !in slots.indices) return ItemStack.EMPTY
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            if (index < machineSlotCount) {
                // 机器槽 → 玩家背包
                if (!insertItem(stackInSlot, machineSlotCount, hotbarEnd, true)) return ItemStack.EMPTY
            } else {
                // 玩家背包 → 机器槽（由 PredicateSlot 的 spec 自动判定能否放入对应槽）
                if (!insertItem(stackInSlot, 0, machineSlotCount, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)
}
