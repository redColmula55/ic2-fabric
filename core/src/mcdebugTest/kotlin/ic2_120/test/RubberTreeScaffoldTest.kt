package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.getBlockId
import com.mcdebug.runner.place
import com.mcdebug.runner.setBlocks
import com.mcdebug.runner.waitTicks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 橡胶树生长回归测试：覆盖"骨粉催熟树苗不应删除周围原木"这个 bug。
 *
 * 背景：原 `RubberTreeFeature.isSaplingGrowth` 用 origin 方块判别场景，但
 * vanilla SaplingGenerator 在调用 feature 前把树苗位置替换成流体状态（air），
 * 判别永远 false → 走了"自然世界生成"分支 → 删除/替换周围原木。
 * 修复：覆写 `RubberSaplingGenerator.generate` 用 ThreadLocal 显式标记生长路径。
 */
@McDebugTest
class RubberTreeTest {

    /** 骨粉催熟一次；用 face=south 命中树苗所在方块。 */
    private fun boneMeal(ctx: TestContext) {
        ctx.api.world.useOnBlock(ctx.origin, "south", item = "minecraft:bone_meal", count = 1)
    }

    @Test
    fun saplingBoneMealDoesNotRemoveSurroundingLogs(ctx: TestContext) {
        val origin = ctx.origin
        val dirt = ctx.pos(0, -1, 0)

        // 1) 清掉覆盖橡胶树整个高度的区域（树能长到 y+8，超出默认清理范围），
        //    再摆地板 + stage=1 树苗（stage=1 模拟已被第一次骨粉推到可生长状态）。
        ctx.api.world.clearBox(
            mapOf("from" to listOf(origin[0] - 4, origin[1] - 1, origin[2] - 4), "to" to listOf(origin[0] + 4, origin[1] + 12, origin[2] + 4)),
            maxBlocks = 4096,
        )
        place(ctx, dirt, "minecraft:dirt")
        // 必须用 world.setBlock + stateProps（server 端只读 stateProps 字段）
        ctx.api.world.setBlock(origin, "ic2_120:rubber_sapling", mapOf("stage" to "1"))

        // 2) 摆 4 块周围原木（x/z 各偏移 ±1，同 y）
        val surroundings = listOf(ctx.pos(-1, 0, 0), ctx.pos(1, 0, 0), ctx.pos(0, 0, -1), ctx.pos(0, 0, 1))
        setBlocks(ctx, surroundings.map { it to "ic2_120:rubber_log" })

        // 3) 多次骨粉（canGrow 45% 概率），直到 origin 变成 rubber_log（树长出来）
        var treeGrew = false
        for (i in 0 until 20) {
            boneMeal(ctx)
            waitTicks(ctx, 1)
            if (getBlockId(ctx, origin) == "ic2_120:rubber_log") {
                treeGrew = true
                break
            }
        }
        assertTrue(treeGrew, "rubber sapling failed to grow after 20 bone meal applications")

        // 4) **核心断言**：4 块周围原木必须全部还在
        surroundings.forEachIndexed { i, s ->
            assertEquals(
                "ic2_120:rubber_log",
                getBlockId(ctx, s),
                "surrounding log #$i at $s was removed — rubber tree bug regression",
            )
        }

        // 5) 树苗位置已被替换为树干
        assertBlockId(ctx, origin, "ic2_120:rubber_log")

        // 6) y=floorY 层 rubber_log 总数应 ≥ 4（4 块周围 + 树干）
        val yFloorFrom = listOf(origin[0] - 1, origin[1], origin[2] - 1)
        val yFloorTo = listOf(origin[0] + 1, origin[1], origin[2] + 1)
        val counts = ctx.api.scan.countByBlock(mapOf("from" to yFloorFrom, "to" to yFloorTo)).asJsonObject
            .get("counts").asJsonObject
        val logCount = counts.get("ic2_120:rubber_log")?.asInt ?: 0
        assertTrue(logCount >= 4, "expected at least 4 logs at y=${origin[1]}, got $logCount")
    }
}

/**
 * 脚手架 (Scaffold) 测试：移除接地底座应导致整列坍塌。
 */
@McDebugTest
class ScaffoldTest {

    @Test
    fun removingGroundedBaseCollapsesTheVerticalColumn(ctx: TestContext) {
        val ground = ctx.origin
        val bottom = ctx.pos(0, 1, 0)
        val middle = ctx.pos(0, 2, 0)
        val top = ctx.pos(0, 3, 0)

        setBlocks(
            ctx,
            listOf(
                ground to "minecraft:stone",
                bottom to "ic2_120:wooden_scaffold",
                middle to "ic2_120:wooden_scaffold",
                top to "ic2_120:wooden_scaffold",
            ),
        )
        waitTicks(ctx, 3)

        assertEquals("ic2_120:wooden_scaffold", getBlockId(ctx, bottom))
        assertEquals("ic2_120:wooden_scaffold", getBlockId(ctx, middle))
        assertEquals("ic2_120:wooden_scaffold", getBlockId(ctx, top))

        ctx.api.world.setBlock(bottom, "minecraft:air")
        waitTicks(ctx, 20)

        assertEquals("minecraft:air", getBlockId(ctx, middle), "middle scaffold should collapse")
        assertEquals("minecraft:air", getBlockId(ctx, top), "top scaffold should collapse")
    }
}
