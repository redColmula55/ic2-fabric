// 临时模拟器：按 jadx（原版 IC2 2.8.221-ex112 反编译）语义实现，用户要求以 jadx 为准。
// 与移植版模拟器的差异：
//   1. 流体模式产热冷却液：jadx 无「产热上限」——散热片/元件散热片散出的热直接进 EmitHeatbuffer，
//      huOtput = 40 × EmitHeatbuffer（huOutputModifier = 40×outputModifier，outputModifier 默认 1.0），
//      热冷却液 mB = huOtput / huPerMB(20)。无燃料棒也能产。
//   2. 散热片只在实际散出热时才 addEmitHeat（self<=0 时 emit = self+selfVent）。
// 燃料棒/冷却单元/反射板/热交换器参数与移植版一致（已核对 jadx BlocksItems.java 构造参数）。
// 燃料棒 EU：反射板反射脉冲 → 源棒 addOutput(1.0)，等价于 (basePulses+neighborPulses)×cells×100/cycle。

import { simulateCycle, emptyGrid, type ComponentId, type Grid } from './src/sim';

// ===== jadx 常量 =====
const HU_OUTPUT_MODIFIER = 40.0;   // 40.0 × outputModifier(1.0)
const HU_PER_MB = 20;              // coolant→hot_coolant: Math.round(20×1.0) HU/mB
const TICKS = 20;
const EU_PER_CYCLE = 100;          // 移植版 outputAccumulator×100；等价于 jadx output×5×20

function put(g: Grid, x: number, y: number, id: ComponentId, use = 0): void { g[x * 9 + y] = { id, use }; }
function makeGrid(c = 0): Grid { return emptyGrid(c); }

// 用移植版 sim 计算电堆（燃料棒/反射板/冷却单元逻辑两版等价）
function electricStats(grid: Grid, chambers: number) {
  const { stats } = simulateCycle(grid, chambers, 'electric', 0);
  return stats;
}

// ===== 1. 电堆布局扫描（验证 EU/热与公式） =====
console.log('========== 电堆（jadx 语义：EU/热公式与移植版等价） ==========');
const NEIGHBORS: Array<[number, number]> = [[0, 4], [2, 4], [1, 3], [1, 5]];
const configs = [
  { label: '4冷却0反射', coolants: 4, reflectors: 0 },
  { label: '3冷却1反射', coolants: 3, reflectors: 1 },
  { label: '2冷却2反射', coolants: 2, reflectors: 2 },
  { label: '1冷却3反射', coolants: 1, reflectors: 3 },
  { label: '0冷却4反射', coolants: 0, reflectors: 4 },
];
for (const cfg of configs) {
  const np = cfg.reflectors;
  const g = makeGrid();
  put(g, 1, 4, 'quad_uranium_fuel_rod');
  NEIGHBORS.forEach(([nx, ny], i) => {
    if (i < cfg.coolants) put(g, nx, ny, 'reactor_coolant_cell');
    else put(g, nx, ny, 'iridium_neutron_reflector');
  });
  const st = electricStats(g, 0);
  const euCycle = st.euPerTick * TICKS;
  const heatCycle = st.heatProduced;
  // jadx: heat = cells × triangularNumber(basePulses+np) × 4，triangular=(x²+x)/2 → 8(3+np)(4+np)
  const expHeat = 8 * (3 + np) * (4 + np);
  const expEu = 400 * (3 + np);
  console.log(`[${cfg.label}] np=${np} | EU/t=${st.euPerTick} (${euCycle}/cycle, 公式${expEu}) | heat=${heatCycle}/cycle (公式${expHeat}) | ${euCycle===expEu&&heatCycle===expHeat?'✓':'✗'}`);
}

// ===== 2. 热堆（jadx 流体模式）：散热 → EmitHeatbuffer → 40×HU → 用户 2HU:1EU = 20×heat EU =====
console.log('\n========== 热堆（jadx 流体模式，无燃料棒也能产热冷却液） ==========');
console.log('换算：heat → EmitHeatbuffer → huOtput=40×heat(HU) → 2HU:1EU → EU = 20×heat');
for (const cfg of configs) {
  const np = cfg.reflectors;
  const heat = 8 * (3 + np) * (4 + np);
  const hu = heat * HU_OUTPUT_MODIFIER;
  const eu = hu / 2;
  const mb = hu / HU_PER_MB;
  console.log(`np=${np}: heat=${heat}/cycle → HU=${hu}/cycle → 热冷却液 ${mb}mB/cycle → 2:1 EU=${eu}/cycle = ${eu / TICKS}EU/t`);
}

// ===== 3. 汇总：一根四联铀棒最大发电（直触冷却单元约束下 np≤3） =====
console.log('\n========== 汇总（一根四联铀燃料棒，非 MOX，全寿命 20000 cycle） ==========');
const LIFETIME = 20_000;
for (const cfg of configs) {
  const np = cfg.reflectors;
  const euCycle = 400 * (3 + np);
  const heat = 8 * (3 + np) * (4 + np);
  const totalCycle = euCycle + heat * HU_OUTPUT_MODIFIER / 2;
  console.log(
    `[${cfg.label}] 直发=${euCycle}EU/cycle(${euCycle / TICKS}EU/t) + 热副产=${(heat * HU_OUTPUT_MODIFIER / 2).toFixed(0)}EU/cycle(${(heat * HU_OUTPUT_MODIFIER / 2 / TICKS).toFixed(1)}EU/t) ` +
    `= 合计 ${totalCycle.toFixed(0)}EU/cycle = ${(totalCycle / TICKS).toFixed(1)}EU/t | 全寿命 ${(totalCycle * LIFETIME / 1e6).toFixed(1)}M EU`
  );
}

// ===== 4. 热堆散热能力验证（jadx 语义：手动连续推进） =====
console.log('\n========== 热堆散热能力（元件热交换器+散热片链，手动循环） ==========');
{
  const g = makeGrid(3);
  put(g, 3, 4, 'sextuple_reactor_coolant_cell', 60000);
  for (const [x, y] of [[2,4],[4,4],[3,3],[3,5]] as Array<[number,number]>) put(g, x, y, 'component_heat_exchanger');
  for (const [x, y] of [[1,4],[5,4],[3,2],[3,6],[2,3],[2,5],[4,3],[4,5]] as Array<[number,number]>) put(g, x, y, 'overclocked_heat_vent');
  let grid = g.map(s => (s ? { ...s } : null));
  let heat = 0;
  for (let i = 0; i < 500; i++) {
    const r = simulateCycle(grid, 3, 'fluid', heat, { hasCoolant: true });
    grid = r.grid; heat = r.heat;
  }
  const out = 60000 - (grid[3*9+4]?.use ?? 0);
  console.log(`[60k单元+4交换器+8超频散热片] 500cycle 抽出 ${out}HU → ${(out/500).toFixed(1)}HU/cycle（电堆产热 336/cycle 需配套 ~3 组）`);
}
