package com.realmlord.registry;

import com.realmlord.RealmLord;
import com.realmlord.block.LordsManorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RealmLord.MODID);

    /** The heart of a territory. Placed when a village is founded; it cannot be broken. */
    public static final RegistryObject<Block> LORDS_MANOR = BLOCKS.register("lords_manor",
            () -> new LordsManorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(-1.0F, 3_600_000.0F).sound(SoundType.WOOD).noLootTable()));
}
