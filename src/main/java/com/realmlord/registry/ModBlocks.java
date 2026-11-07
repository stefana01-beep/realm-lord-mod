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

    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RealmLord.MODID);

    /** The bed crossbow and the trebuchet. */
    public static final RegistryObject<Block> BED_CROSSBOW = BLOCKS.register("bed_crossbow",
            () -> new com.realmlord.block.SiegeEngineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 6.0F)
                    .sound(SoundType.WOOD).noOcclusion(), com.realmlord.block.SiegeEngineBlock.Kind.BED_CROSSBOW));
    public static final RegistryObject<Block> TREBUCHET = BLOCKS.register("trebuchet",
            () -> new com.realmlord.block.SiegeEngineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 6.0F)
                    .sound(SoundType.WOOD).noOcclusion(), com.realmlord.block.SiegeEngineBlock.Kind.TREBUCHET));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.realmlord.block.SiegeEngineBlockEntity>> SIEGE_ENGINE =
            BLOCK_ENTITIES.register("siege_engine", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(com.realmlord.block.SiegeEngineBlockEntity::new, BED_CROSSBOW.get(), TREBUCHET.get()).build(null));
}
