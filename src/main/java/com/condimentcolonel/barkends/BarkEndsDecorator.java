package com.condimentcolonel.barkends;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Bark on the "bookends" of living trees.
 *
 * Runs after a tree has grown (in world generation or from a sapling). For each log, it looks
 * at the two blocks at the ends of the log (along its axis). If an end is open, meaning air,
 * leaves, water or a plant is there, you could see the cut end grain, so that log becomes its
 * all-bark "wood" version instead. Logs with both ends covered stay normal logs.
 *
 * Works for any tree whose logs follow the naming pattern X_log -> X_wood (or X_stem ->
 * X_hyphae for nether fungi), so modded trees work automatically. Logs in the
 * barkends:keeps_log_ends tag are left alone.
 */
public class BarkEndsDecorator extends TreeDecorator {
    public static final BarkEndsDecorator INSTANCE = new BarkEndsDecorator();
    public static final MapCodec<BarkEndsDecorator> CODEC = MapCodec.unit(INSTANCE);

    @Override
    protected TreeDecoratorType<?> type() {
        return BarkEnds.BARK_ENDS.get();
    }

    @Override
    public void place(TreeDecorator.Context context) {
        Set<BlockPos> treeLogs = new HashSet<>(context.logs());

        for (BlockPos pos : context.logs()) {
            BlockState log = getState(context, pos);
            if (log == null || !log.hasProperty(BlockStateProperties.AXIS) || log.is(BarkEnds.KEEPS_LOG_ENDS)) continue;

            Optional<Block> bark = barkVersionOf(log.getBlock());
            if (bark.isEmpty()) continue;

            Direction.Axis axis = log.getValue(BlockStateProperties.AXIS);
            if (hasOpenEnd(context, treeLogs, pos, axis)) {
                BlockState barkState = bark.get().defaultBlockState();
                if (barkState.hasProperty(BlockStateProperties.AXIS)) {
                    barkState = barkState.setValue(BlockStateProperties.AXIS, axis);
                }
                context.setBlock(pos, barkState);
            }
        }
    }

    /** True if either end of this log faces something you can see through. */
    private static boolean hasOpenEnd(TreeDecorator.Context context, Set<BlockPos> treeLogs,
                                      BlockPos pos, Direction.Axis axis) {
        for (Direction.AxisDirection sign : Direction.AxisDirection.values()) {
            BlockPos neighbour = pos.relative(Direction.fromAxisAndDirection(axis, sign));
            if (treeLogs.contains(neighbour)) continue; // another log of this tree covers it
            boolean open = context.level().isStateAtPosition(neighbour,
                    s -> s.isAir() || s.is(BlockTags.LEAVES) || s.canBeReplaced());
            if (open) return true;
        }
        return false;
    }

    /** oak_log -> oak_wood, crimson_stem -> crimson_hyphae, if that block exists. */
    private static Optional<Block> barkVersionOf(Block logBlock) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(logBlock);
        String path = id.getPath();
        String barkPath;
        if (path.endsWith("_log")) {
            barkPath = path.substring(0, path.length() - "_log".length()) + "_wood";
        } else if (path.endsWith("_stem")) {
            barkPath = path.substring(0, path.length() - "_stem".length()) + "_hyphae";
        } else {
            return Optional.empty();
        }
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), barkPath));
    }

    /** The world-generation view only lets us test blocks, so we capture the state while testing. */
    private static BlockState getState(TreeDecorator.Context context, BlockPos pos) {
        BlockState[] found = new BlockState[1];
        context.level().isStateAtPosition(pos, s -> {
            found[0] = s;
            return true;
        });
        return found[0];
    }
}
