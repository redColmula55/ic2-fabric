package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.getBlockId
import com.mcdebug.runner.waitTicks
import com.mcdebug.runner.waitUntil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * 蒸汽流体世界行为测试（依赖自然 tick 的流体调度）。
 */
@McDebugTest
class SteamWorldTest {

    private val STEAM = "ic2_120:steam"
    private val SUPERHEATED_STEAM = "ic2_120:superheated_steam"

    private fun steamArea(ctx: TestContext): Map<String, List<Int>> = mapOf(
        "from" to listOf(ctx.origin[0] - 8, ctx.origin[1] - 2, ctx.origin[2] - 8),
        "to" to listOf(ctx.origin[0] + 8, ctx.origin[1] + 9, ctx.origin[2] + 8),
    )

    private fun clearSteamArea(ctx: TestContext) {
        ctx.api.world.clearBox(steamArea(ctx), maxBlocks = 4096)
    }

    private fun setSteam(ctx: TestContext, pos: List<Int>, block: String = STEAM, age: Int = 0) {
        ctx.api.world.setBlock(pos, block, mapOf("level" to "0", "age" to age.toString()))
    }

    /** 蒸汽上升 + 扩散（替换非空气方块后）。 */
    @Test
    fun risesAndSpreadsAfterReplacingNonAir(ctx: TestContext) {
        clearSteamArea(ctx)
        try {
            ctx.api.world.setBlock(ctx.pos(0, -1, 0), "minecraft:dirt")
            ctx.api.world.setBlock(ctx.origin, "minecraft:tall_grass")
            setSteam(ctx, ctx.origin)

            waitUntil(ctx, "block[${ctx.pos(0, 2, 0)[0]},${ctx.pos(0, 2, 0)[1]},${ctx.pos(0, 2, 0)[2]}].id == \"$STEAM\"", 40)

            assertEquals(STEAM, getBlockId(ctx, ctx.origin))
            assertEquals(STEAM, getBlockId(ctx, ctx.pos(0, 1, 0)))
            assertEquals(STEAM, getBlockId(ctx, ctx.pos(0, 2, 0)))
            assertEquals(STEAM, getBlockId(ctx, ctx.pos(1, 0, 0)))
            assertEquals("minecraft:dirt", getBlockId(ctx, ctx.pos(0, -1, 0)))

            val flowing = ctx.api.world.getBlock(ctx.pos(0, 1, 0)).asJsonObject.get("state").asJsonObject
            assertNotEquals("0", flowing.get("props").asJsonObject.get("level").asString, "上方蒸汽必须是有距离衰减的流动状态")
        } finally {
            clearSteamArea(ctx)
        }
    }

    /** 源块过期（age=39 到 40）不应删除上方独立的源。 */
    @Test
    fun sourceExpiryDoesNotDeleteIndependentSource(ctx: TestContext) {
        clearSteamArea(ctx)
        try {
            setSteam(ctx, ctx.origin, STEAM, 39)
            setSteam(ctx, ctx.pos(0, 1, 0), STEAM, 0)

            waitUntil(ctx, "block[${ctx.origin[0]},${ctx.origin[1]},${ctx.origin[2]}].id == \"minecraft:air\"", 60)

            assertEquals("minecraft:air", getBlockId(ctx, ctx.origin))
            assertEquals(STEAM, getBlockId(ctx, ctx.pos(0, 1, 0)))
        } finally {
            clearSteamArea(ctx)
        }
    }

    /** 过热蒸汽原地冷却为普通蒸汽（不传播）。 */
    @Test
    fun superheatedCoolsLocally(ctx: TestContext) {
        clearSteamArea(ctx)
        try {
            setSteam(ctx, ctx.origin, SUPERHEATED_STEAM, 39)
            setSteam(ctx, ctx.pos(0, 1, 0), SUPERHEATED_STEAM, 0)

            waitUntil(ctx, "block[${ctx.origin[0]},${ctx.origin[1]},${ctx.origin[2]}].id == \"$STEAM\"", 60)

            assertEquals(STEAM, getBlockId(ctx, ctx.origin))
            assertEquals(SUPERHEATED_STEAM, getBlockId(ctx, ctx.pos(0, 1, 0)))
        } finally {
            clearSteamArea(ctx)
        }
    }

    /** 普通蒸汽 200 tick（10 秒）后消失。 */
    @Test
    fun ordinarySteamDisappearsAfterTenSeconds(ctx: TestContext) {
        clearSteamArea(ctx)
        try {
            // 封住所有传播方向，只验证源块本身的 200 tick 生命周期。
            for (pos in listOf(ctx.pos(0, 1, 0), ctx.pos(0, 0, -1), ctx.pos(0, 0, 1), ctx.pos(-1, 0, 0), ctx.pos(1, 0, 0))) {
                ctx.api.world.setBlock(pos, "minecraft:stone")
            }
            setSteam(ctx, ctx.origin)

            waitTicks(ctx, 195)
            assertEquals(STEAM, getBlockId(ctx, ctx.origin))

            waitUntil(ctx, "block[${ctx.origin[0]},${ctx.origin[1]},${ctx.origin[2]}].id == \"minecraft:air\"", 40)
            assertEquals("minecraft:air", getBlockId(ctx, ctx.origin))
        } finally {
            clearSteamArea(ctx)
        }
    }
}
