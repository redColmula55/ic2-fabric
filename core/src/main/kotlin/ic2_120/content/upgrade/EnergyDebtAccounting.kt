package ic2_120.content.upgrade

/**
 * 超频耗能债务记账（全机器共用）。
 *
 * 背景：超频下每 tick 需求 = 基础能耗 × 1.6^n（指数增长），支付是"全有全无"
 * （缓冲不足整笔则不付款、不走进度），小数余数以浮点债务跨 tick 结转。
 *
 * 死锁 bug（2026-08-15 引入，2026-08 修复）：旧实现中付不起时债务照样每 tick
 * 累加且无上限——只要连续约 (有效容量 / 每 tick 债务) 个 tick 供不上电，债务就
 * 越过缓冲容量天花板，此后即使电力恢复、缓冲充满也永远付不起，机器永久停摆。
 *
 * 修复：债务封顶到 [EnergyDebtAccounting.clamp] 传入的有效容量。债务最多等于
 * 容量，而缓冲充满时恰好够支付整笔，电力恢复即自愈。
 */
object EnergyDebtAccounting {

    /**
     * 累加一个 tick 的能耗需求并封顶。
     *
     * @param current 当前未结清债务（EU，含小数余数）
     * @param perTick 本 tick 新增需求（基础能耗 × 超频倍率，可含小数）
     * @param effectiveCapacity 机器当前有效缓冲容量（EU）；债务以此为上限，
     *   保证缓冲充满时必然能够全额支付
     */
    fun accrue(current: Float, perTick: Float, effectiveCapacity: Long): Float =
        (current + perTick).coerceAtMost(effectiveCapacity.coerceAtLeast(1L).toFloat())
}
