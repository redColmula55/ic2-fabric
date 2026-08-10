// 反应堆器件（带寿命/热容组件）堆叠回归测试。
//
// 背景：vanilla 1.20.1 中 Settings.maxDamage(maxUse) 会把 maxCount 强制为 1，
// 且 ItemStack.isStackable() 对「可损耗且已损耗」的物品恒为 false，导致反应堆
// 组件即使寿命完全相同也无法堆叠。修复见
// core/src/main/java/ic2_120/mixin/ReactorComponentStackMixin.java：
//   - ItemStack.getMaxCount() 对 AbstractDamageableReactorComponent 返回 64
//   - ItemStack.isStackable() 对 AbstractDamageableReactorComponent 恒为 true
// 合并本身仍由 vanilla ItemStack.canCombine 把关（item + NBT 完全一致，
// 含 "use"/"Damage" 标签）：寿命相同（NBT 一致）堆叠，寿命不同永不合并。
//
// 注意：mcdebug 0.4.15 的 inv.insert 用 areItemsEqual（只比 item 不比 NBT），
// 无法用于验证 NBT 差异合并；因此合并用例全部走箱子 GUI shift-click
// （ScreenHandler.insertItem 的 isStackable + canCombine 真实 vanilla 路径）。
//
// 用例覆盖：
//   1. getMaxCount = 64（绕过 Settings.maxDamage 强制 maxCount=1）
//   2. 相同寿命（NBT 一致）的已损耗组件经 shift-click 合并
//   3. 不同寿命（NBT 不同）的组件不合并，落空槽
//   4. 全新与已损耗组件不合并
import assert from "node:assert/strict";
import { defineTest, setBlocks } from "@yu1745/mcdebug";
import { type TestContext } from "./helpers.js";

const CELL = "ic2_120:reactor_coolant_cell";
/** 消耗一半的冷却单元（10k 容量，已用 5000） */
const HALF_USE = { use: 5000, Damage: 5000 };
/** 另一档寿命：已用 6000 */
const OTHER_USE = { use: 6000, Damage: 6000 };

async function placeChest(ctx: TestContext): Promise<void> {
  await setBlocks(ctx, [{ pos: ctx.origin, block: "minecraft:chest" }]);
}

/** 打开箱子界面并把 10 个带 NBT 的冷却单元放进玩家主背包第一格，返回其 screen slot 索引。 */
async function openChestWithPlayerCells(
  ctx: TestContext,
  nbt?: Record<string, number>,
): Promise<{ screenId: string; playerIdx: number }> {
  const opened = await ctx.api.screen.openBlock(ctx.origin, { player: "fake" });
  await ctx.api.screen.setPlayerSlot(opened.screenId, 9, { item: CELL, count: 10, ...(nbt ? { nbt } : {}) });
  const snap = await ctx.api.screen.snapshot(opened.screenId);
  const playerIdx = snap.slots.findIndex((s) => s?.item === CELL && s?.count === 10);
  assert.ok(playerIdx >= 0, "expected seeded player slot to be visible in chest screen");
  return { screenId: opened.screenId, playerIdx };
}

// 正向：getMaxCount 应为 64（getSlot 的 maxCount 直接来自 ItemStack.getMaxCount()）。
export const reactorComponentMaxStackSize = defineTest("reactor:component max stack size is 64", async (ctx) => {
  await placeChest(ctx);
  await ctx.api.inv.setSlot(ctx.origin, 0, CELL, 32);

  const slot = await ctx.api.inv.getSlot(ctx.origin, 0);
  assert.equal(slot.maxCount, 64, "reactor component max stack should be 64 (getMaxCount mixin)");
});

// 正向：寿命完全相同（NBT 一致）的已损耗组件经 GUI shift-click 合并。
// 走 ScreenHandler.insertItem：isStackable()（mixin 放开）+ canCombine（NBT 一致）。
export const reactorComponentSameLifespanMerges = defineTest("reactor:component same-lifespan stacks merge", async (ctx) => {
  await placeChest(ctx);
  await ctx.api.inv.setSlot(ctx.origin, 0, CELL, 32, HALF_USE);

  const { screenId, playerIdx } = await openChestWithPlayerCells(ctx, HALF_USE);
  await ctx.api.screen.quickMove(screenId, playerIdx);

  const slot0 = await ctx.api.inv.getSlot(ctx.origin, 0);
  assert.equal(slot0.slot.count, 42, "shift-click should merge same-lifespan used cells onto chest stack");
  assert.deepEqual(slot0.slot.nbt, HALF_USE, "merged stack must keep the shared lifespan NBT");

  await ctx.api.screen.close(screenId);
});

// 反向：寿命不同（NBT 不同）的组件不得合并进已有堆叠，只能落空槽。
export const reactorComponentDifferentLifespanDoesNotMerge = defineTest("reactor:component different-lifespan stacks do not merge", async (ctx) => {
  await placeChest(ctx);
  await ctx.api.inv.setSlot(ctx.origin, 0, CELL, 32, HALF_USE);

  const { screenId, playerIdx } = await openChestWithPlayerCells(ctx, OTHER_USE);
  await ctx.api.screen.quickMove(screenId, playerIdx);

  const slot0 = await ctx.api.inv.getSlot(ctx.origin, 0);
  assert.equal(slot0.slot.count, 32, "existing stack must not absorb different-lifespan cells");

  const slot1 = await ctx.api.inv.getSlot(ctx.origin, 1);
  assert.equal(slot1.slot.item, CELL);
  assert.equal(slot1.slot.count, 10, "different-lifespan cells should land in an empty slot");
  assert.deepEqual(slot1.slot.nbt, OTHER_USE, "different-lifespan cells must keep their own NBT");

  await ctx.api.screen.close(screenId);
});

// 反向：全新（无 NBT）与已损耗组件不合并。
export const reactorComponentFreshAndUsedDoNotMerge = defineTest("reactor:component fresh and used stacks do not merge", async (ctx) => {
  await placeChest(ctx);
  await ctx.api.inv.setSlot(ctx.origin, 0, CELL, 32);

  const { screenId, playerIdx } = await openChestWithPlayerCells(ctx, HALF_USE);
  await ctx.api.screen.quickMove(screenId, playerIdx);

  const slot0 = await ctx.api.inv.getSlot(ctx.origin, 0);
  assert.equal(slot0.slot.count, 32, "fresh stack must not absorb used cells");

  const slot1 = await ctx.api.inv.getSlot(ctx.origin, 1);
  assert.equal(slot1.slot.item, CELL);
  assert.equal(slot1.slot.count, 10);

  await ctx.api.screen.close(screenId);
});
