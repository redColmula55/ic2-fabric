// 诚实首周期模拟：从全新冷却单元出发逐 cycle 跑到「第一个单元吸满（各自容量）」，
// 统计直发 EU 总量、存储热量、周期长度。杜绝稳态/瞬态/造热偏差。
import { simulateCycle, type Grid } from './src/sim';

const HEAT_EU_MULT = 20; // jadx: heat→40×HU→2HU:1EU = 20×heat
const CELL_CAP: Record<string, number> = { reactor_coolant_cell: 10000, triple_reactor_coolant_cell: 30000, sextuple_reactor_coolant_cell: 60000 };

export function firstSwap(grid: Grid, chambers: number, maxCyc = 40000) {
  let g = grid.map(s => (s ? { ...s } : null));
  let heat = 0;
  let directEU = 0;
  let t = 0;
  let firstFull: { slot: number; use: number; cap: number } | null = null;
  let exploded = false;
  let depleted = false;
  for (; t < maxCyc; t++) {
    const r = simulateCycle(g, chambers, 'electric', heat);
    g = r.grid; heat = r.heat;
    directEU += r.stats.euPerTick * 20;
    for (let i = 0; i < g.length; i++) {
      const s = g[i];
      if (s && s.id in CELL_CAP && s.use >= CELL_CAP[s.id]) {
        firstFull = { slot: i, use: s.use, cap: CELL_CAP[s.id] };
        break;
      }
    }
    if (firstFull) break;
    if (r.stats.exploded) { exploded = true; break; }
    if (!r.stats.hasFuelRods) { depleted = true; break; }
  }
  const heatStored = g.reduce((a, s) => a + (s && s.id in CELL_CAP ? (s as { use: number }).use : 0), 0);
  const x = firstFull ? Math.floor(firstFull.slot / 9) : -1;
  const y = firstFull ? firstFull.slot % 9 : -1;
  return {
    T: t,
    hours: t / 3600, // 1 cycle = 20 tick = 1 秒
    directEU,
    heatStored,
    heatEU: heatStored * HEAT_EU_MULT,
    totalEU: directEU + heatStored * HEAT_EU_MULT,
    avgEuPerTick: t > 0 ? (directEU + heatStored * HEAT_EU_MULT) / (t * 20) : 0,
    firstFull: firstFull ? `(${x},${y})use=${firstFull.use}/${firstFull.cap}` : '无',
    hull: heat,
    exploded,
    depleted,
  };
}
