// 核反应堆红石中继回归测试。
//
// 背景：反应堆 EU 模式此前只读中心方块自身的 isReceivingRedstonePower，
// 红石块贴在反应仓上无效（红石线必须贴中心方块才有用），与 IC2 原版
// TileEntityReactorChamberElectric.redstone.linkTo(reactor) 的 OR 语义
// 及 guidebook「EU 模式下任意结构方块都能接收红石信号」的承诺不符。
// 修复后：中心反应堆或任一相邻反应仓收到红石信号即可激活反应堆。
import {
  beFieldGreaterThan,
  defineTest,
  getBeNumber,
  place,
  setBlocks,
  setSlot,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";

const REACTOR = 'ic2_120:nuclear_reactor';
const CHAMBER = 'ic2_120:reactor_chamber';

// 正向：红石块只贴反应仓（距中心反应堆 2 格，不接触中心方块），反应堆应被激活并产出 EU。
// 回归点：反应仓红石中继生效。
export const reactorRedstoneViaChamber = defineTest('reactor:redstone relay via chamber', async (ctx) => {
  await place(ctx, ctx.origin, REACTOR);
  await place(ctx, ctx.origin.east(), CHAMBER);
  // 红石块放在仓的东侧：与中心反应堆不相邻，只可能通过仓的中继生效
  await setBlocks(ctx, [{ pos: ctx.origin.east(2), block: 'minecraft:redstone_block' }]);
  await setSlot(ctx, ctx.origin, 0, 'ic2_120:uranium_fuel_rod', 1);
  await waitUntil(ctx, beFieldGreaterThan(ctx.origin, 'EnergyStored', 0), 100);
});

// 负向：有反应仓但无任何红石信号时，反应堆必须保持停机（中继不是"永远开启"）。
export const reactorRedstoneNoSignal = defineTest('reactor:chamber without signal stays off', async (ctx) => {
  await place(ctx, ctx.origin, REACTOR);
  await place(ctx, ctx.origin.east(), CHAMBER);
  await setSlot(ctx, ctx.origin, 0, 'ic2_120:uranium_fuel_rod', 1);
  await waitTicks(ctx, 60);
  const energy = await getBeNumber(ctx, ctx.origin, 'EnergyStored');
  if (energy !== 0) throw new Error(`expected 0 energy without redstone signal, got ${energy}`);
});
