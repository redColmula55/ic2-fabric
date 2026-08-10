package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.getBlockId
import com.mcdebug.runner.setBlocks
import com.mcdebug.runner.setSlot
import com.mcdebug.runner.waitTicks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * mcdebug API 基础功能测试（红石控制 / 实体操作）。
 * 这些用例验证 mcdebug 自身的 API 行为，与 IC2 机器逻辑无关。
 */
@McDebugTest
class McdebugBasicApiTest {

    /** 红石拉杆：setLever / isPowered / getPower / pulse。 */
    @Test
    fun redstoneLeverPowerAndPulse(ctx: TestContext) {
        val base = ctx.origin
        val lever = ctx.pos(0, 1, 0)
        ctx.api.world.setBlock(base, "minecraft:stone")
        ctx.api.world.setBlock(lever, "minecraft:lever", mapOf("face" to "floor", "facing" to "north", "powered" to "false"))

        val on = ctx.api.redstone.setLever(lever, true).asJsonObject
        assertEquals(true, on.get("powered").asBoolean)

        val powered = ctx.api.redstone.isPowered(base).asJsonObject
        assertEquals(true, powered.get("powered").asBoolean)
        assertEquals(15, powered.get("received").asInt)

        val power = ctx.api.redstone.getPower(base, side = "up").asJsonObject
        assertEquals(15, power.get("sideInput").asInt)

        val off = ctx.api.redstone.setLever(lever, false).asJsonObject
        assertEquals(false, off.get("powered").asBoolean)
        assertEquals(false, ctx.api.redstone.isPowered(base).asJsonObject.get("powered").asBoolean)

        ctx.api.redstone.pulse(lever, 2)
        assertEquals(true, ctx.api.redstone.isPowered(base).asJsonObject.get("powered").asBoolean)
        waitTicks(ctx, 4)
        assertEquals(false, ctx.api.redstone.isPowered(base).asJsonObject.get("powered").asBoolean)
    }

    /** 实体：spawn / listItems / teleport / getNbt / collectItems。 */
    @Test
    fun entityItemSpawnTeleportCollect(ctx: TestContext) {
        // prepareArea 清成 air，item 实体会受重力下落——放石头当地板
        setBlocks(ctx, listOf(ctx.origin to "minecraft:stone"))

        val spawned = ctx.api.entity.spawn("minecraft:item", ctx.pos(0, 1, 0), stack = mapOf("item" to "minecraft:diamond", "count" to 3))
            .asJsonObject
        assertEquals(true, spawned.get("spawned").asBoolean)
        val entityJson = spawned.get("entity").asJsonObject
        assertEquals("minecraft:diamond", entityJson.get("stack").asJsonObject.get("item").asString)
        assertEquals(3, entityJson.get("stack").asJsonObject.get("count").asInt)
        val uuid = entityJson.get("uuid").asString

        val box = mapOf(
            "from" to listOf(ctx.origin[0] - 2, ctx.origin[1] - 2, ctx.origin[2] - 2),
            "to" to listOf(ctx.origin[0] + 2, ctx.origin[1] + 2, ctx.origin[2] + 2),
        )
        val listed = ctx.api.entity.listItems(box, item = "minecraft:diamond").asJsonObject
        assertEquals(1, listed.get("count").asInt)
        assertEquals(uuid, listed.get("items").asJsonArray.get(0).asJsonObject.get("uuid").asString)

        // teleport 到东 2 格上方（同样需要地板）
        val target = ctx.pos(2, 1, 0)
        setBlocks(ctx, listOf(ctx.pos(2, 0, 0) to "minecraft:stone"))
        val moved = ctx.api.entity.teleport(uuid, target).asJsonObject
        assertEquals(true, moved.get("teleported").asBoolean)

        val nbt = ctx.api.entity.getNbt(uuid).asJsonObject
        assertEquals(uuid, nbt.get("entity").asJsonObject.get("uuid").asString)

        val collected = ctx.api.entity.collectItems(box, item = "minecraft:diamond").asJsonObject
        assertEquals(1, collected.get("count").asInt)

        // 收集后原地应无该实体
        val after = ctx.api.entity.listItems(box, item = "minecraft:diamond").asJsonObject
        assertEquals(0, after.get("count").asInt)
    }
}

/**
 * 方块放置/读取 API 测试。
 */
@McDebugTest
class McdebugWorldApiTest {

    @Test
    fun setBlockGetBlockRoundTrip(ctx: TestContext) {
        ctx.api.world.setBlock(ctx.origin, "minecraft:furnace", mapOf("facing" to "south"))
        assertBlockId(ctx, ctx.origin, "minecraft:furnace")
        val state = ctx.api.world.getBlock(ctx.origin).asJsonObject.get("state").asJsonObject
        assertEquals("south", state.get("props").asJsonObject.get("facing").asString)
    }

    @Test
    fun fillBoxAndClearBox(ctx: TestContext) {
        val box = mapOf(
            "from" to listOf(ctx.origin[0], ctx.origin[1], ctx.origin[2]),
            "to" to listOf(ctx.origin[0] + 2, ctx.origin[1], ctx.origin[2] + 2),
        )
        ctx.api.world.fillBox(box, "minecraft:stone")
        assertEquals("minecraft:stone", getBlockId(ctx, ctx.pos(2, 0, 2)))
        ctx.api.world.clearBox(box)
        assertEquals("minecraft:air", getBlockId(ctx, ctx.pos(2, 0, 2)))
    }

    @Test
    fun forceloadRoundTrip(ctx: TestContext) {
        val cx = ctx.origin[0] / 16
        val cz = ctx.origin[2] / 16
        val r = ctx.api.world.forceloadChunk(cx, cz).asJsonObject
        assertEquals(true, r.get("forced").asBoolean)
        val r2 = ctx.api.world.unforceloadChunk(cx, cz).asJsonObject
        assertEquals(true, r2.get("changed").asBoolean)
    }

    @Test
    fun beNbtRoundTrip(ctx: TestContext) {
        ctx.api.world.setBlock(ctx.origin, "minecraft:furnace")
        ctx.api.be.setNbt(ctx.origin, mapOf("CustomName" to "\"mcdebug-test\""))
        val nbt = ctx.api.be.getNbt(ctx.origin).asJsonObject.get("nbt").asJsonObject
        assertTrue(nbt.toString().contains("mcdebug-test"), "custom name should round-trip: $nbt")
    }

    @Test
    fun invInsertExtractRoundTrip(ctx: TestContext) {
        ctx.api.world.setBlock(ctx.origin, "minecraft:chest")
        ctx.api.inv.insert(ctx.origin, "minecraft:coal", 5, slot = 0)
        assertEquals(5, ctx.api.inv.getSlot(ctx.origin, 0).asJsonObject.get("slot").asJsonObject.get("count").asInt)
        val extracted = ctx.api.inv.extract(ctx.origin, "minecraft:coal", 3, slot = 0).asJsonObject
        assertEquals(3, extracted.get("extracted").asInt)
        assertEquals(2, ctx.api.inv.getSlot(ctx.origin, 0).asJsonObject.get("slot").asJsonObject.get("count").asInt)
    }

    @Test
    fun waitUntilTickPredicate(ctx: TestContext) {
        // tick >= N 谓词：先取当前 tick，断言 wait.until 能等到 tick 前进
        val status = ctx.api.server.status().asJsonObject
        val now = status.get("tick").asLong
        ctx.api.wait.until("tick >= ${now + 2}", 100)
        val after = ctx.api.server.status().asJsonObject.get("tick").asLong
        assertTrue(after >= now + 2, "tick should advance: $now -> $after")
    }
}
