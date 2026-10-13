package com.condimentcolonel.barkends;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Gives every tree in the game bark ends: vanilla, or any other mod's.
 *
 * When a world starts loading (before any chunks generate), this walks through every tree
 * definition the world knows about, including trees tucked inside "pick one of these"
 * features, and adds the bark_ends decorator to the end of its decorator list. Sapling-grown
 * trees use the same definitions, so they get it too.
 *
 * Any decorator that already turns logs into wood (for example an older copy of this one
 * under another mod's name) is recognised by class name, so trees are never decorated twice.
 *
 * This only works because META-INF/accesstransformer.cfg unlocks the
 * TreeConfiguration.decorators field, which Minecraft normally makes final.
 */
@EventBusSubscriber(modid = BarkEnds.MOD_ID)
public class BarkEndsEverywhere {

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        RegistryAccess registries = event.getServer().registryAccess();
        Set<TreeConfiguration> trees = Collections.newSetFromMap(new IdentityHashMap<>());

        // Named tree features, plus anything nested inside them...
        registries.registryOrThrow(Registries.CONFIGURED_FEATURE)
                .forEach(feature -> collectTrees(feature, trees));
        // ...and trees written inline inside placed features, which have no name of their own.
        registries.registryOrThrow(Registries.PLACED_FEATURE)
                .forEach(placed -> placed.getFeatures().forEach(feature -> collectTrees(feature, trees)));

        int added = 0;
        for (TreeConfiguration tree : trees) {
            boolean alreadyHasIt = tree.decorators.stream()
                    .anyMatch(d -> d.getClass().getSimpleName().equals("BarkEndsDecorator"));
            if (!alreadyHasIt) {
                // Added last, so vines, cocoa and beehives are placed first.
                tree.decorators = ImmutableList.<TreeDecorator>builder()
                        .addAll(tree.decorators)
                        .add(BarkEndsDecorator.INSTANCE)
                        .build();
                added++;
            }
        }
        BarkEnds.LOGGER.info("Bark Ends: added to {} tree types.", added);
    }

    private static void collectTrees(ConfiguredFeature<?, ?> feature, Set<TreeConfiguration> trees) {
        feature.getFeatures().forEach(part -> {
            if (part.config() instanceof TreeConfiguration tree) {
                trees.add(tree);
            }
        });
    }
}
