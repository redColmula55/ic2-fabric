// 9×9 满仓(6腔室)摆法评估器：单根四联铀棒。
// 用法: tsx layout_cycle.ts '<9行×9列摆法>'   (字符映射见 CHARMAP)
// 输出:
//   每 reactor cycle: 直发 EU/t、产热/cycle
//   首周期(一次换单元): T(最先吸满的冷却单元周期数)、直发总EU、热副产折EU(20×heat)、总EU、平均EU/t
//   铀棒高效性: 每铀cell 直发EU/t、单周期总EU/cell
// 语义: 电模式模拟(与 jadx 燃料棒/元件逻辑等价)；热副产按 jadx 40×HU 且 2HU:1EU = 20×heat 折算。
// 冷却单元不允许超上限(会消失)：T = 第一个单元吸满(各自容量)的周期，吸满即停。
// ⚠ 已实测: 元件热交换器网格会因钳位逻辑凭空造热(原版行为)，长期会把单元撑爆→堆温爆炸；
//   因此带 CHE 网格的摆法不可长期运行，本工具如实报告爆炸。
import { simulateCycle, emptyGrid, type ComponentId, type Grid } from './src/sim';
import { firstSwap } from './firstswap';

const CELL_CAP: Record<string, number> = { reactor_coolant_cell: 10000, triple_reactor_coolant_cell: 30000, sextuple_reactor_coolant_cell: 60000 };
const CHARMAP: Record<string, ComponentId> = {
  '*': 'quad_uranium_fuel_rod', 'Q': 'uranium_fuel_rod', 'D': 'dual_uranium_fuel_rod',
  'R': 'iridium_neutron_reflector', 'r': 'thick_neutron_reflector', 'n': 'neutron_reflector',
  'C': 'reactor_coolant_cell', 'T': 'triple_reactor_coolant_cell', 'S': 'sextuple_reactor_coolant_cell',
  'H': 'heat_exchanger', 'V': 'reactor_heat_exchanger', 'X': 'component_heat_exchanger', 'A': 'advanced_heat_exchanger',
  'h': 'heat_vent', 'v': 'component_heat_vent', 'O': 'overclocked_heat_vent', 'a': 'advanced_heat_vent', 's': 'reactor_heat_vent',
};

export function parseLayout(spec: string): Grid {
  const rows = spec.trim().split('\n').map(r => r.trim()).filter(r => r.length > 0);
  const cols = rows[0].length;
  if (rows.length !== 9 || cols < 3 || cols > 9) throw new Error(`需要 9 行、3..9 列，收到 ${rows.length}×${cols}`);
  const chambers = cols - 3;
  const g = emptyGrid(chambers);
  for (let y = 0; y < 9; y++) {
    for (let x = 0; x < cols; x++) {
      const ch = rows[y][x];
      if (ch === '.' || ch === ' ') continue;
      const id = CHARMAP[ch];
      if (!id) throw new Error(`未知字符 '${ch}' @ (${x},${y})`);
      g[x * 9 + y] = { id, use: 0 };
    }
  }
  return g;
}

export interface CycleEval {
  label: string;
  chambers: number;
  euPerTick: number;
  heatPerCycle: number;
  T: number;
  Thours: number;
  swapDirect: number;
  swapHeat: number;
  swapTotal: number;
  avgEuPerTick: number;
  perCellEuTick: number;
  perCellSwapTotal: number;
  cells: number;
  balance: number;
  hullSteady: number;
  firstFull: string;
  exploded: boolean;
}

export function evaluateLayout(grid: Grid, chambers: number, label = ''): CycleEval {
  const fs = firstSwap(grid, chambers);
  // 稳态速率（平衡度参考）
  let g = grid.map(s => (s ? { ...s } : null));
  let heat = 0;
  for (let i = 0; i < 60; i++) { const r = simulateCycle(g, chambers, 'electric', heat); g = r.grid; heat = r.heat; }
  const rates = new Map<number, number>();
  for (let i = 0; i < g.length; i++) if (g[i] && g[i]!.id in CELL_CAP) rates.set(i, 0);
  for (let i = 0; i < 30; i++) {
    const prev = new Map<number, number>();
    for (const [slot] of rates) prev.set(slot, g[slot]?.use ?? 0);
    const r = simulateCycle(g, chambers, 'electric', heat);
    g = r.grid; heat = r.heat;
    for (const [slot, v] of rates) rates.set(slot, v + ((g[slot]?.use ?? 0) - (prev.get(slot) ?? 0)));
  }
  const st = simulateCycle(g, chambers, 'electric', heat);
  const euPerTick = st.stats.euPerTick;
  const heatPerCycle = st.stats.heatProduced;
  const vals = [...rates.values()].filter(v => v > 0.005).map(v => v / 30);
  const balance = vals.length > 1 ? Math.max(...vals) / Math.min(...vals) : 1;
  const rodCells = 4;
  return {
    label, chambers, euPerTick, heatPerCycle,
    T: fs.T, Thours: fs.hours,
    swapDirect: fs.directEU, swapHeat: fs.heatEU, swapTotal: fs.totalEU,
    avgEuPerTick: fs.avgEuPerTick,
    perCellEuTick: euPerTick / rodCells,
    perCellSwapTotal: fs.totalEU / rodCells,
    cells: vals.length, balance, hullSteady: fs.hull,
    firstFull: fs.firstFull, exploded: fs.exploded,
  };
}

export function report(e: CycleEval): void {
  console.log(`\n=== ${e.label || '(unnamed)'} (${3 + e.chambers}×9) ===`);
  console.log(`直发: ${e.euPerTick.toFixed(1)} EU/t | 产热: ${e.heatPerCycle}/cycle | 受热单元 ${e.cells} 个 | 不平衡 ${e.balance.toFixed(1)}x`);
  console.log(`首周期 T=${e.T.toFixed(0)} cycle (${e.Thours.toFixed(1)}h) | 先满: ${e.firstFull} | 稳态堆温 ${e.hullSteady}${e.exploded ? ' ⚠爆炸!' : ''}`);
  console.log(`一周期: 直发 ${(e.swapDirect / 1e6).toFixed(2)}M EU + 热副产 ${(e.swapHeat / 1e6).toFixed(2)}M EU = 总 ${(e.swapTotal / 1e6).toFixed(2)}M EU`);
  console.log(`平均功率 ${e.avgEuPerTick.toFixed(0)} EU/t | 铀棒高效: 直发 ${e.perCellEuTick.toFixed(1)} EU/t/cell, 单周期 ${(e.perCellSwapTotal / 1e6).toFixed(2)}M EU/cell`);
}

if (import.meta.url.endsWith(process.argv[1] ?? '')) {
  const spec = process.argv.slice(2).join('\n');
  if (!spec.trim()) {
    console.log('用法: tsx layout_cycle.ts \'<9行×9列摆法>\'');
    console.log('字符: *=四联铀棒 R=铱反射板 S=60k单元 T=30k C=10k V=反应堆热交换器 X=元件热交换器 H=热交换器 A=高级热交换器 O=超频散热片 h=散热片 v=元件散热片 .=空');
    process.exit(1);
  }
  const grid = parseLayout(spec);
  const cols = spec.trim().split('\n')[0].trim().length;
  report(evaluateLayout(grid, cols - 3, 'custom'));
}
