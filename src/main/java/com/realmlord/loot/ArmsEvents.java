package com.realmlord.loot;

import com.realmlord.RealmLord;
import com.realmlord.entity.BanditEntity;
import com.realmlord.entity.level.MobLevels;
import com.realmlord.registry.ModArms;
import com.realmlord.territory.TokenGrade;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What beasts and bandits leave for the arms of the novel's world, and the grade a crafted
 * piece comes out at.
 */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArmsEvents {
    private ArmsEvents() {}

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob m) || m.level().isClientSide) return;
        RandomSource r = m.getRandom();
        boolean king = com.realmlord.world.Dens.isKing(m);
        if (m instanceof BanditEntity b) {
            // A bandit's badge of the band, and now and then his sabre.
            boolean chief = b.getRole() == BanditEntity.Role.CHIEF;
            if (chief || r.nextFloat() < 0.35F) drop(event, m, new ItemStack(ModArms.BANDIT_INSIGNIA.get(), chief ? 3 : 1));
            if (r.nextFloat() < (chief ? 0.5F : 0.08F))
                drop(event, m, Gear.make(ModArms.BANDIT_SABRE.get(), chief && r.nextFloat() < 0.3F ? TokenGrade.BLACK_IRON : TokenGrade.BRONZE, r));
            // Bandits carry what they plundered: a chief now and then a forging manual, any of them rations.
            if (chief && r.nextFloat() < 0.25F) drop(event, m, Manuals.random(r));
            if (r.nextFloat() < 0.15F) drop(event, m, new ItemStack(ModArms.MILITARY_RATIONS.get(), 1 + r.nextInt(2)));
            return;
        }
        if (!MobLevels.leveled(m) || m instanceof com.realmlord.entity.beast.WildOxEntity || m instanceof com.realmlord.entity.beast.WildBoarEntity && !king) return;
        // Sinew from the hunting beasts; a beast core from a king, and rarely from an old, strong beast.
        if (r.nextFloat() < (king ? 1.0F : 0.35F)) drop(event, m, new ItemStack(ModArms.BEAST_SINEW.get(), king ? 4 : 1 + r.nextInt(2)));
        if (king || MobLevels.level(m) >= 10 && r.nextFloat() < 0.04F) drop(event, m, new ItemStack(ModArms.BEAST_CORE.get()));
    }

    private static void drop(LivingDropsEvent event, Mob m, ItemStack stack) {
        event.getDrops().add(new ItemEntity(m.level(), m.getX(), m.getY() + 0.5, m.getZ(), stack));
    }

    /**
     * A crafted piece is graded: hide Bronze, black iron Black Iron, refined steel Silver; a
     * blade forged again with a beast core comes out Gold.
     */
    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack out = event.getCrafting();
        TokenGrade grade = ModArms.craftedGrade(out.getItem());
        if (grade == null) return;
        boolean core = false;
        var grid = event.getInventory();
        for (int i = 0; i < grid.getContainerSize(); i++) if (grid.getItem(i).is(ModArms.BEAST_CORE.get())) core = true;
        if (core) grade = TokenGrade.GOLD;
        ItemStack graded = Gear.make(out.getItem(), grade, event.getEntity().getRandom());
        out.setTag(graded.getTag());
    }
}
