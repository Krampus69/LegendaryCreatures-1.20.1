package sfiomn.legendarycreatures.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import sfiomn.legendarycreatures.LegendaryCreatures;
import sfiomn.legendarycreatures.blocks.HoundTrapBlockEntity;

public class BlockEntityRegistry {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LegendaryCreatures.MOD_ID);

    public static final RegistryObject<BlockEntityType<HoundTrapBlockEntity>> HOUND_TRAP = BLOCK_ENTITIES.register("hound_trap",
            () -> BlockEntityType.Builder.of(HoundTrapBlockEntity::new, BlockRegistry.HOUND_TRAP.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
