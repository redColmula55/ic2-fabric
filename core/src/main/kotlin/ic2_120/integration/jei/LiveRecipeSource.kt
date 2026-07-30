package ic2_120.integration.jei

import net.minecraft.recipe.RecipeManager

/**
 * 提供运行时 RecipeManager 的抽象（供 JEI plugin 收集实际加载的配方）。
 *
 * main sourceSet 无法直接引用 net.minecraft.client.MinecraftClient（client-only 类），
 * 故由 client sourceSet 在 [ic2_120.Ic2_120Client.onInitializeClient] 注入实现。
 *
 * 这样 JEI 的配方展示与机器实际判定逻辑（均基于 vanilla recipeManager）使用同一数据源，
 * 可覆盖 core + 所有附属命名空间的配方，而不仅限于 core 硬编码的 Entry 列表。
 */
interface LiveRecipeSource {
    /** 客户端当前世界的 RecipeManager；level 未就绪时返回 null */
    fun clientRecipeManager(): RecipeManager?

    companion object {
        @Volatile
        var instance: LiveRecipeSource? = null
    }
}
