/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.classic.worldgen;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import net.dries007.tfc.objects.blocks.BlocksTFC;

public class WorldGenPumpkinTFC extends WorldGenerator
{
    private final Block pumpkin;

    public WorldGenPumpkinTFC(Block pumpkin)
    {
        this.pumpkin = pumpkin;
    }

    @Override
    public boolean generate(World world, Random rng, BlockPos start)
    {
        for (int i = 0; i < 32; ++i)
        {
            BlockPos pos = start.add(rng.nextInt(8) - rng.nextInt(8), rng.nextInt(4) - rng.nextInt(4), rng.nextInt(8) - rng.nextInt(8));

            if (!world.isAirBlock(pos) && !pumpkin.canPlaceBlockAt(world, pos)) continue;
            if (!BlocksTFC.isSoil(world.getBlockState(pos.add(0, -1, 0)))) continue;

            world.setBlockState(pos, pumpkin.getDefaultState().withProperty(BlockPumpkin.FACING, EnumFacing.Plane.HORIZONTAL.random(rng)), 2);
        }

        return true;
    }
}
