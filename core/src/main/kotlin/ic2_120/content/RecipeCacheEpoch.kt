package ic2_120.content

/**
 * 配方缓存全局失效代（epoch）。
 *
 * 各机器 BlockEntity 的配方匹配缓存（含 null 缓存与 isRecipeInput 布尔缓存）在
 * `ServerLifecycleEvents.END_DATA_PACK_RELOAD` 时不会收到任何回调，若不失效，
 * `/reload` 或数据包更新后已加载的机器会继续用旧配方（旧物品产出/旧路由判定）。
 *
 * 方案：reload 结束后全局代号 +1；每台机器缓存命中时校验自己记录的代号，
 * 不一致即视为过期（代价仅为一次 int 比较，远低于每 tick 查询 recipeManager）。
 * 机器实例字段随区块卸载/重建自然重置，此处只处理“实例存活期间配方表被替换”。
 */
object RecipeCacheEpoch {

    @Volatile
    private var epoch = 0

    /** 当前代号。缓存命中时与写入缓存时记录的代号比较，不等即失效。 */
    fun current(): Int = epoch

    /** 数据包重载完成后调用：使所有已加载机器的配方缓存过期。 */
    fun invalidate() {
        epoch++
    }
}
