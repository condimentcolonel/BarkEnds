package com.condimentcolonel.barkends;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Bark Ends: trees grow with bark on their ends instead of showing cut log rings.
 *
 * Two parts:
 *   {@link BarkEndsDecorator}  turns each log with an open end into its wood (all-bark) block
 *   {@link BarkEndsEverywhere} adds that decorator to every tree the world knows, from any mod
 */
@Mod(BarkEnds.MOD_ID)
public class BarkEnds {
    public static final String MOD_ID = "barkends";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS =
            DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, MOD_ID);

    /** Usable from worldgen JSON too: { "type": "barkends:bark_ends" } */
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<BarkEndsDecorator>> BARK_ENDS =
            TREE_DECORATORS.register("bark_ends", () -> new TreeDecoratorType<>(BarkEndsDecorator.CODEC));

    /**
     * Logs in this block tag keep their normal ends. Modpacks and datapacks can add to
     * data/barkends/tags/block/keeps_log_ends.json, e.g. for a tree that looks wrong with bark ends.
     */
    public static final TagKey<Block> KEEPS_LOG_ENDS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, "keeps_log_ends"));

    public BarkEnds(IEventBus modEventBus) {
        TREE_DECORATORS.register(modEventBus);
    }
}
