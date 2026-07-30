// 流体/固体装罐机 (Canner) 测试。
//
// 槽位：
//   slot 0 = 容器输入
//   slot 1 = 混合材料
//   slot 2 = 物品输出
//   slot 3 = 放电槽
//   slot 4..7 = 升级槽
import {
  assertSlotEmpty,
  assertSlotHas,
  defineTest,
  defineTests,
  fluidInsert,
  insertItem,
  setBeField,
  waitUntil,
  invItemEquals,
} from "@yu1745/mcdebug";
import { setupAdjacentBatbox, type TestContext } from "./helpers.js";

const BUCKET = 81_000;

async function setupMixingCanner(ctx: TestContext): Promise<void> {
  await setupAdjacentBatbox(ctx, 'ic2_120:canner');
  await setBeField(ctx, ctx.origin, 'Mode', 3); // ENRICH_LIQUID
  await insertItem(ctx, ctx.origin, 'ic2_120:transformer_upgrade', 1, 4);
  await insertItem(ctx, ctx.origin, 'ic2_120:overclocker_upgrade', 2, 5);
  const inserted = await fluidInsert(ctx, ctx.origin, 'minecraft:water', BUCKET);
  if (inserted !== BUCKET) throw new Error(`failed to insert water: ${inserted}/${BUCKET}`);
}

export const cannerTests = defineTests([
  defineTest('canner:mixing fills empty cell directly', async (ctx) => {
    await setupMixingCanner(ctx);
    await insertItem(ctx, ctx.origin, 'ic2_120:empty_cell', 1, 0);
    await insertItem(ctx, ctx.origin, 'ic2_120:lapis_dust', 8, 1);

    await waitUntil(ctx, invItemEquals(ctx.origin, 2, 'ic2_120:coolant_cell'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 2, 'ic2_120:coolant_cell');
    await assertSlotEmpty(ctx, ctx.origin, 0);
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),
]);
