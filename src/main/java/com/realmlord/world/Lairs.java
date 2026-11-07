package com.realmlord.world;

import com.realmlord.build.Design;
import com.realmlord.build.Designs;
import com.realmlord.entity.BanditEntity;
import com.realmlord.registry.ModEntities;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.territory.TokenGrade;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Deeper lairs and larger ruins for a town's adventurers. As the novel's world opened up past the
 * first villages, players found bandit strongholds that took an army to break, and the ruins of
 * ancient cities with their guardians and treasures.
 *
 * <p>Around a town, now and then: a <b>bandit fortress</b>, stone-walled with corner towers and a
 * keep, held by twenty bandits of tier 4 and 5 and their chief; or the <b>ruins of an ancient
 * city</b>, its broken walls watched over by armoured skeleton guardians. Each holds two treasure
 * chests: gold, Gold-grade arms and secret manuals, town blueprints, and now and then a Summoning
 * Token.
 */
public final class Lairs {
    private Lairs() {}

    public static final int RANGE = 400;

    public static ListTag lairs(WorldState w) {
        return w.ext.getList("lairs", Tag.TAG_COMPOUND);
    }

    public static boolean near(WorldState w, BlockPos at, int range) {
        for (Tag tg : lairs(w)) if (BlockPos.of(((CompoundTag) tg).getLong("pos")).distSqr(at) < (double) range * range) return true;
        return false;
    }

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        if (!t.level.atLeast(TerritoryLevel.TOWN_1) || t.dimension != level.dimension() || rnd.nextInt(20) != 0 || !level.isLoaded(t.center)) return;
        WorldState w = WorldState.get(level.getServer());
        if (near(w, t.center, RANGE)) return;
        boolean ruins = rnd.nextBoolean();
        Design d = Designs.builtin(ruins ? "ruined_city" : "bandit_fortress");
        if (d == null) return;
        for (int tries = 0; tries < 30; tries++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            int dist = t.radius() + 80 + rnd.nextInt(200);
            int x = t.center.getX() + (int) (Math.cos(a) * dist), z = t.center.getZ() + (int) (Math.sin(a) * dist);
            if (!level.isLoaded(new BlockPos(x, 64, z)) || !level.isLoaded(new BlockPos(x + d.w, 64, z + d.d))) continue;
            BlockPos origin = Tribes.dryGround(level, x, z, d.w, d.d);
            if (origin == null || TerritoryData.get(level.getServer()).at(level.dimension(), origin) != null) continue;
            Tribes.place(level, d, origin);
            stock(level, origin, d, rnd, ruins);
            BlockPos mid = origin.offset(d.w / 2, 0, d.d / 2);
            if (ruins) guardians(level, mid, rnd);
            else bandits(level, mid, rnd);
            CompoundTag l = new CompoundTag();
            l.putLong("pos", mid.asLong());
            l.putString("kind", ruins ? "ruins" : "fortress");
            ListTag list = lairs(w);
            list.add(l);
            w.ext.put("lairs", list);
            w.touch();
            Village.tell(level, t, Component.translatable(ruins ? "realmlord.lair.ruins" : "realmlord.lair.fortress", t.name, mid.getX(), mid.getZ()));
            return;
        }
    }

    private static void stock(ServerLevel level, BlockPos origin, Design d, RandomSource rnd, boolean ruins) {
        for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(d.w, d.h, d.d))) {
            if (!level.getBlockState(p).is(Blocks.CHEST) || !(level.getBlockEntity(p) instanceof Container c)) continue;
            int slot = 0;
            c.setItem(slot++, new ItemStack(ModItems.GOLD_COIN.get(), 3 + rnd.nextInt(8)));
            c.setItem(slot++, new ItemStack(ModItems.SILVER_COIN.get(), 20 + rnd.nextInt(40)));
            var arms = com.realmlord.registry.ModArms.arms();
            c.setItem(slot++, com.realmlord.loot.Gear.make(arms.get(rnd.nextInt(arms.size())).get(), rnd.nextInt(3) == 0 ? TokenGrade.GOLD : TokenGrade.SILVER, rnd));
            if (rnd.nextBoolean()) c.setItem(slot++, com.realmlord.martial.Arts.random(rnd, ruins ? com.realmlord.martial.ArtGrade.GOLD : com.realmlord.martial.ArtGrade.SILVER));
            if (rnd.nextBoolean()) c.setItem(slot++, com.realmlord.loot.Blueprints.random(rnd, 6));
            if (rnd.nextInt(3) == 0) c.setItem(slot++, com.realmlord.work.TechManuals.random(rnd, 2));
            if (rnd.nextInt(ruins ? 6 : 10) == 0) c.setItem(slot, com.realmlord.item.SummoningTokenItem.of(0));
        }
    }

    private static void bandits(ServerLevel level, BlockPos at, RandomSource rnd) {
        for (int i = 0; i <= 20; i++) {
            BanditEntity b = ModEntities.BANDIT.get().create(level);
            if (b == null) continue;
            int x = at.getX() + rnd.nextInt(15) - 7, z = at.getZ() + rnd.nextInt(15) - 7;
            b.moveTo(x + 0.5, Village.groundAt(level, x, z), z + 0.5, rnd.nextFloat() * 360F, 0F);
            b.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null, null);
            b.getPersistentData().remove("realmlord_wild");
            b.configure(i == 0 ? BanditEntity.Role.CHIEF : rnd.nextInt(10) < 4 ? BanditEntity.Role.ARCHER : BanditEntity.Role.SWORDSMAN, i == 0 ? 6 : 4 + rnd.nextInt(2));
            b.setPersistenceRequired();
            level.addFreshEntity(b);
        }
    }

    private static void guardians(ServerLevel level, BlockPos at, RandomSource rnd) {
        for (int i = 0; i < 10; i++) {
            Skeleton s = EntityType.SKELETON.create(level);
            if (s == null) continue;
            int x = at.getX() + rnd.nextInt(17) - 8, z = at.getZ() + rnd.nextInt(17) - 8;
            s.moveTo(x + 0.5, Village.groundAt(level, x, z), z + 0.5, rnd.nextFloat() * 360F, 0F);
            s.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null, null);
            s.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            s.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            if (rnd.nextBoolean()) s.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            s.setCustomName(Component.translatable("entity.realmlord.ancient_guardian"));
            var hp = s.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.setBaseValue(40);
                s.setHealth(40);
            }
            s.setPersistenceRequired();
            level.addFreshEntity(s);
        }
    }
}
