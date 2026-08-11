package ic2_120.content.block.machines

import ic2_120.content.RecipeCacheEpoch
import ic2_120.content.item.IBlockCuttingBlade
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.recipes.blockcutter.BlockCutterRecipe
import ic2_120.content.recipes.blockcutter.BlockCutterRecipeSerializer
import ic2_120.content.recipes.getRecipeType
import ic2_120.content.sync.BlockCutterSync
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.BlockCutterBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.screen.BlockCutterScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.ModMachineRecipeBinding
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SimpleInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.RecipeManager
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlockEntity(block = BlockCutterBlock::class)
@ModMachineRecipeBinding(BlockCutterRecipeSerializer::class)
class BlockCutterBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = BlockCutterBlock.ACTIVE

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override val tier: Int = BLOCK_CUTTER_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val BLOCK_CUTTER_TIER = 1
        const val SLOT_BLADE = 0
        const val SLOT_INPUT = 1
        const val SLOT_DISCHARGING = 2
        const val SLOT_OUTPUT = 3
        const val SLOT_UPGRADE_0 = 4
        const val SLOT_UPGRADE_1 = 5
        const val SLOT_UPGRADE_2 = 6
        const val SLOT_UPGRADE_3 = 7
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT)
        const val INVENTORY_SIZE = 8
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_BLADE), matcher = { it.item is IBlockCuttingBlade }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isRecipeInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()
    @RegisterEnergy
    val sync = BlockCutterSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(BLOCK_CUTTER_TIER + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { tier },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        BlockCutterBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if ((slot == SLOT_BLADE || slot == SLOT_DISCHARGING) && stack.count > 1) {
            stack.count = 1
        }
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_BLADE -> !stack.isEmpty && stack.item is IBlockCuttingBlade
        SLOT_INPUT -> isRecipeInput(stack)
        SLOT_OUTPUT -> false
        SLOT_DISCHARGING -> isBatteryItem(stack) || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.block_cutter")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        BlockCutterScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(BlockCutterSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(BlockCutterSync.NBT_ENERGY_STORED, sync.amount)
    }

    /** 锯片硬度，无锯片或非锯片时为 -1f */
    fun getBladeHardness(): Float {
        val blade = getStack(SLOT_BLADE)
        if (blade.isEmpty) return -1f
        return (blade.item as? IBlockCuttingBlade)?.getBladeHardness(blade) ?: -1f
    }

    /** 锯片是否足够硬（或无配方时不检查） */
    fun isBladeTooWeak(): Boolean {
        val bladeHardness = getBladeHardness()
        if (bladeHardness < 0f) return true
        val input = getStack(SLOT_INPUT)
        if (input.isEmpty) return false
        val recipe = getRecipeForInput(input) ?: return false
        return !recipe.isBladeSufficient(bladeHardness)
    }

    // 配方缓存——安全处理双配方系统（同一输入可有 inputCount=1/2 两个配方）：
    // BlockCutterRecipe.matches 检查 stack.count >= inputCount，匹配结果依赖输入数量。
    // 规则：非 null 缓存仅在 input.count >= recipe.inputCount 时有效；
    // null 缓存仅在输入数量未增加时有效（数量增加可能让此前不足量的配方匹配）。
    private var cachedRecipeItem: net.minecraft.item.Item? = null
    private var cachedRecipe: BlockCutterRecipe? = null
    private var cachedRecipeQueryCount = 0
    private var cachedRecipeEpoch = -1

    /**
     * 获取当前输入的配方（不考虑刀片硬度）
     */
    private fun getRecipeForInput(input: ItemStack): BlockCutterRecipe? {
        if (input.isEmpty) return null
        if (RecipeCacheEpoch.current() == cachedRecipeEpoch && cachedRecipeItem === input.item) {
            val cached = cachedRecipe
            if (cached != null && input.count >= cached.inputCount) return cached
            if (cached == null && input.count <= cachedRecipeQueryCount) return null
        }
        val w = world ?: return null
        val inv = SimpleInventory(input)
        val recipe = w.recipeManager.getFirstMatch(getRecipeType<BlockCutterRecipe>(), inv, w).orElse(null)
        cachedRecipeEpoch = RecipeCacheEpoch.current()
        cachedRecipeItem = input.item
        cachedRecipe = recipe
        cachedRecipeQueryCount = input.count
        return recipe
    }

    /**
     * 获取配方（检查刀片硬度）
     */
    private fun getRecipe(): BlockCutterRecipe? {
        val bladeHardness = getBladeHardness()
        if (bladeHardness < 0f) return null

        val input = getStack(SLOT_INPUT)
        if (input.isEmpty) return null

        val recipe = getRecipeForInput(input) ?: return null

        // 检查刀片硬度
        if (!recipe.isBladeSufficient(bladeHardness)) return null

        return recipe
    }

    /** 获取材料硬度（用于锯片检查） */
    fun getMaterialHardness(): Float {
        val input = getStack(SLOT_INPUT)
        if (input.isEmpty) return 0f

        val recipe = getRecipeForInput(input)
        return recipe?.materialHardness ?: 0f
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()

        sync.bladeTooWeak = if (isBladeTooWeak()) 1 else 0

        val bladeHardness = getBladeHardness()
        if (bladeHardness < 0f) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val input = getStack(SLOT_INPUT)
        if (input.isEmpty) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val recipe = getRecipe() ?: run {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val inputCount = recipe.inputCount
        if (input.count < inputCount) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val result = recipe.output
        val outputSlot = getStack(SLOT_OUTPUT)
        val maxStack = result.maxCount
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (!canAccept) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        if (sync.progress >= BlockCutterSync.PROGRESS_MAX) {
            input.decrement(inputCount)
            if (outputSlot.isEmpty()) setStack(SLOT_OUTPUT, result.copy())
            else outputSlot.increment(result.count)
            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
        val need = (BlockCutterSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress = (sync.progress + progressIncrement).coerceAtMost(BlockCutterSync.PROGRESS_MAX)
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)

        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return

        val inserted = sync.insertEnergy(extracted)
        if (extracted > inserted) sync.forceInsertEnergy(extracted - inserted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

    // 类型判定缓存：isRecipeInput 用 maxCount 查询（仅依赖物品类型），按 item 缓存安全。
    private var cachedInputItem: net.minecraft.item.Item? = null
    private var cachedIsInput = false
    private var cachedInputEpoch = -1

    private fun isRecipeInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack) || stack.item is IUpgradeItem || stack.item is IBlockCuttingBlade) return false
        if (RecipeCacheEpoch.current() == cachedInputEpoch && cachedInputItem === stack.item) return cachedIsInput
        val w = world ?: return true
        val inv = SimpleInventory(stack.copyWithCount(stack.maxCount))
        val found = w.recipeManager.getFirstMatch(getRecipeType<BlockCutterRecipe>(), inv, w).isPresent
        cachedInputEpoch = RecipeCacheEpoch.current()
        cachedInputItem = stack.item
        cachedIsInput = found
        return found
    }
}
