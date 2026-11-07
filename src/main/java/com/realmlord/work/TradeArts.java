package com.realmlord.work;

import com.realmlord.RealmLord;
import com.realmlord.item.CraftItem;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.registry.ModArms;
import com.realmlord.registry.ModCrafts;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;

/**
 * What the technical manuals' techniques do, and what the things they teach to make do when
 * carried or worn; and the trades' own hand in what they make from their manuals.
 */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TradeArts {
    private TradeArts() {}

    public static final String ANTIDOTE = "realmlord_antidote";
    private static final UUID BEAST_HEALTH = UUID.fromString("0c9d4e2a-77b1-4f3c-8a55-3e1d2b6f9c01");
    private static final UUID BEAST_ARMOUR = UUID.fromString("0c9d4e2a-77b1-4f3c-8a55-3e1d2b6f9c02");

    private static boolean knows(Player p, String tech) {
        return TechManuals.knows(p, tech);
    }

    public static void workXp(Player p, WorkOccupation w, int amount) {
        if (p instanceof ServerPlayer sp && TechManuals.tradeTier(p, w) >= 0) Profiles.grantWorkXp(sp, amount);
    }

    /** True if a tamed animal or horse belongs to this player. */
    public static boolean ownedBy(LivingEntity e, Player p) {
        if (e instanceof TamableAnimal t) return t.isTame() && t.isOwnedBy(p);
        if (e instanceof AbstractHorse h) return h.isTamed() && p.getUUID().equals(h.getOwnerUUID());
        return false;
    }

    private static Player owner(LivingEntity e) {
        if (e instanceof OwnableEntity o && o.getOwner() instanceof Player p) return p;
        return null;
    }

    private static boolean carrying(Player p, net.minecraft.world.item.Item item) {
        return p.getInventory().contains(new ItemStack(item));
    }

    private static void takeOne(Player p, net.minecraft.world.item.Item item) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                inv.getItem(i).shrink(1);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ the trades at the bench

    /** A recipe from a manual, made by one of its trade: experience, and now and then one more. */
    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || !(event.getInventory() instanceof CraftingContainer grid)) return;
        var recipe = p.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, p.level());
        if (recipe.isEmpty() || !(recipe.get() instanceof ManualRecipe mr)) return;
        TechData.Tech t = TechManuals.byId(mr.manual());
        if (t == null) return;
        int tier = TechManuals.tradeTier(p, t.trade());
        if (tier < 0) return;
        workXp(p, t.trade(), 6 + 6 * t.tier());
        ItemStack out = event.getCrafting();
        if (!out.isDamageableItem() && out.getMaxStackSize() > 1 && p.getRandom().nextFloat() < 0.1F * (tier + 1)) {
            ItemStack extra = new ItemStack(out.getItem(), 1);
            if (!p.getInventory().add(extra)) p.drop(extra, false);
            p.displayClientMessage(Component.translatable("realmlord.tech.bonus").withStyle(ChatFormatting.GREEN), true);
        }
    }

    /** At Advanced rank and above, a trade's own arms and armour now and then come out a grade higher. */
    public static boolean gradeBump(Player p, net.minecraft.world.item.Item item) {
        WorkOccupation w = item instanceof net.minecraft.world.item.ArmorItem a
                && (a.getMaterial() == ModArms.BRIGANDINE || a.getMaterial() == ModArms.NIGHT_CLOTHES || a.getMaterial() == ModArms.DRAGON_ROBE
                || a.getMaterial() == ModArms.HIDE) ? WorkOccupation.TAILOR
                : item instanceof net.minecraft.world.item.BowItem || item instanceof net.minecraft.world.item.CrossbowItem ? WorkOccupation.CARPENTER
                : WorkOccupation.BLACKSMITH;
        return TechManuals.tradeTier(p, w) >= 2 && p.getRandom().nextFloat() < 0.25F;
    }

    // ------------------------------------------------------------------ medicine

    @SubscribeEvent
    public static void effect(MobEffectEvent.Applicable event) {
        var e = event.getEffectInstance().getEffect();
        if ((e == MobEffects.POISON || e == MobEffects.WITHER)
                && event.getEntity().getPersistentData().getLong(ANTIDOTE) > event.getEntity().level().getGameTime()) {
            event.setResult(Event.Result.DENY);
        }
    }

    // ------------------------------------------------------------------ animals

    private static final Set<CraftItem.Kind> ON_ANIMALS = Set.of(CraftItem.Kind.HORSE_FEED, CraftItem.Kind.HORSE_TONIC, CraftItem.Kind.NEEDLES,
            CraftItem.Kind.ANIMAL_SALVE, CraftItem.Kind.FODDER, CraftItem.Kind.BAMBOO_FEED);

    /**
     * Craft items used on a creature act before the creature's own reaction (a wild horse would
     * otherwise rear); and the Wild Beast Taming Technique tames at the first try.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        Player p = event.getEntity();
        ItemStack s = event.getItemStack();
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        if (s.getItem() instanceof CraftItem ci && ON_ANIMALS.contains(ci.kind)) {
            InteractionResult r = ci.interactLivingEntity(s, p, target, event.getHand());
            if (r.consumesAction()) {
                event.setCanceled(true);
                event.setCancellationResult(r);
            }
            return;
        }
        if (p.level().isClientSide || !knows(p, "wild_beast_taming")) return;
        boolean food = target instanceof Wolf && s.is(Items.BONE)
                || target instanceof Cat && (s.is(Items.COD) || s.is(Items.SALMON))
                || target instanceof Parrot && (s.is(Items.WHEAT_SEEDS) || s.is(Items.MELON_SEEDS) || s.is(Items.PUMPKIN_SEEDS) || s.is(Items.BEETROOT_SEEDS))
                || target instanceof AbstractHorse h && h.isFood(s);
        if (!food) return;
        if (target instanceof TamableAnimal t && !t.isTame() && !t.isBaby()) {
            if (!net.minecraftforge.event.ForgeEventFactory.onAnimalTame(t, p)) {
                t.tame(p);
                t.setOrderedToSit(false);
                t.level().broadcastEntityEvent(t, (byte) 7);
            }
        } else if (target instanceof AbstractHorse h && !h.isTamed() && !h.isBaby()) {
            if (!net.minecraftforge.event.ForgeEventFactory.onAnimalTame(h, p)) {
                h.tameWithName(p);
                h.level().broadcastEntityEvent(h, (byte) 7);
            }
        } else return;
        if (!p.getAbilities().instabuild) s.shrink(1);
        p.displayClientMessage(Component.translatable("realmlord.craft.tame", target.getDisplayName()).withStyle(ChatFormatting.GREEN), true);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Iron Armored Beast Domestication: what you tame comes out tougher. */
    @SubscribeEvent
    public static void tamed(AnimalTameEvent event) {
        if (event.isCanceled() || !knows(event.getTamer(), "iron_armored_beast")) return;
        LivingEntity a = event.getAnimal();
        var hp = a.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hp.getModifier(BEAST_HEALTH) == null) hp.addPermanentModifier(new AttributeModifier(BEAST_HEALTH, "Iron armored beast", 20, AttributeModifier.Operation.ADDITION));
        var ar = a.getAttribute(Attributes.ARMOR);
        if (ar != null && ar.getModifier(BEAST_ARMOUR) == null) ar.addPermanentModifier(new AttributeModifier(BEAST_ARMOUR, "Iron armored beast", 6, AttributeModifier.Operation.ADDITION));
        a.setHealth(a.getMaxHealth());
    }

    /** Hundred Beasts Classic: your animals strike harder and heal as they fight. */
    @SubscribeEvent
    public static void beastsHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker instanceof Player) return;
        Player owner = owner(attacker);
        if (owner == null || !knows(owner, "hundred_beasts")) return;
        event.setAmount(event.getAmount() * 1.33F);
        attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
    }

    /** The guardian charm saves an animal; the substitute doll saves its bearer. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void death(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide) return;
        if (e instanceof ServerPlayer p) {
            if (!carrying(p, ModCrafts.SUBSTITUTE_DOLL.get())) return;
            takeOne(p, ModCrafts.SUBSTITUTE_DOLL.get());
            event.setCanceled(true);
            p.setHealth(p.getMaxHealth() / 2);
            for (MobEffectInstance i : java.util.List.copyOf(p.getActiveEffects())) if (!i.getEffect().isBeneficial()) p.removeEffect(i.getEffect());
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
            p.level().broadcastEntityEvent(p, (byte) 35);
            p.displayClientMessage(Component.translatable("realmlord.craft.doll").withStyle(ChatFormatting.GOLD), false);
            return;
        }
        Player owner = owner(e);
        if (owner == null && e instanceof AbstractHorse h && h.getOwnerUUID() != null) owner = e.level().getPlayerByUUID(h.getOwnerUUID());
        if (owner == null || !carrying(owner, ModCrafts.GUARDIAN_CHARM.get())) return;
        takeOne(owner, ModCrafts.GUARDIAN_CHARM.get());
        event.setCanceled(true);
        e.setHealth(e.getMaxHealth() / 2);
        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
        e.level().broadcastEntityEvent(e, (byte) 35);
        owner.displayClientMessage(Component.translatable("realmlord.craft.charm", e.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
    }

    // ------------------------------------------------------------------ building and the land

    /** Yingzao Fashi: one block in five placed is not used up. */
    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.getAbilities().instabuild || !knows(p, "yingzao_fashi")) return;
        if (p.getRandom().nextInt(5) != 0) return;
        ItemStack back = new ItemStack(event.getPlacedBlock().getBlock().asItem());
        if (!back.isEmpty() && !p.getInventory().add(back)) p.drop(back, false);
    }

    /** Seed selection replants; the coal mining manual brings up more ore. */
    @SubscribeEvent
    public static void broke(BlockEvent.BreakEvent event) {
        Player p = event.getPlayer();
        if (p == null || !(event.getLevel() instanceof ServerLevel level) || p.getAbilities().instabuild) return;
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state) && knows(p, "seed_selection")) {
            level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () -> {
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.FARMLAND))
                    level.setBlock(pos, crop.getStateForAge(0), 3);
            }));
        }
        if (knows(p, "coal_mining") && (state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.GOLD_ORES))
                && net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, p.getMainHandItem()) == 0
                && p.getRandom().nextFloat() < 0.3F) {
            var drops = Block.getDrops(state, level, pos, null, p, p.getMainHandItem());
            if (!drops.isEmpty()) Block.popResource(level, pos, drops.get(0).copy());
        }
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        BlockState s = event.getState();
        if ((s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Tags.Blocks.STONE)) && knows(event.getEntity(), "great_wall"))
            event.setNewSpeed(event.getNewSpeed() * 1.5F);
    }

    // ------------------------------------------------------------------ fishing

    @SubscribeEvent
    public static void fished(ItemFishedEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || event.getDrops().isEmpty()) return;
        if (knows(p, "cormorant_fishing") && p.getRandom().nextFloat() < 0.33F) {
            ItemStack extra = event.getDrops().get(0).copy();
            if (!p.getInventory().add(extra)) p.drop(extra, false);
        }
        if (knows(p, "mazu_charts") && p.getRandom().nextFloat() < 0.1F && p.level() instanceof ServerLevel sl) {
            LootParams params = new LootParams.Builder(sl).withParameter(LootContextParams.ORIGIN, p.position())
                    .withParameter(LootContextParams.TOOL, p.getMainHandItem()).withParameter(LootContextParams.THIS_ENTITY, p)
                    .withLuck(p.getLuck()).create(LootContextParamSets.FISHING);
            for (ItemStack f : sl.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING_TREASURE).getRandomItems(params)) {
                if (!p.getInventory().add(f)) p.drop(f, false);
            }
        }
    }

    // ------------------------------------------------------------------ worn and carried, and techniques over time

    /** Night clothes: a crouching figure in the dark goes unseen beyond a few paces. */
    @SubscribeEvent
    public static void target(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof Player p)) return;
        if (!p.getItemBySlot(EquipmentSlot.HEAD).is(ModArms.NIGHT_HOOD.get()) || !p.getItemBySlot(EquipmentSlot.CHEST).is(ModArms.NIGHT_SUIT.get())) return;
        if (!p.isCrouching() || p.level().getMaxLocalRawBrightness(p.blockPosition()) > 7) return;
        if (event.getEntity().distanceToSqr(p) > 16) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 40 != 0) return;
        boolean wet = p.isInWater();
        if (wet && p.getOffhandItem().is(ModCrafts.LIFEBUOY.get())) {
            p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false));
        }
        if (p.getItemBySlot(EquipmentSlot.CHEST).is(ModArms.DRAGON_ROBE_ITEM.get()))
            p.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 100, 0, true, false));
        var prof = Profiles.get(p);
        if (prof.techs().isEmpty()) return;
        if (prof.knowsTech("star_guiding") && (wet || p.getVehicle() instanceof Boat)) {
            if (!p.level().isDay()) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false));
            if (wet) p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false));
        }
        if (prof.knowsTech("pearl_diving") && p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER))
            p.setAirSupply(Math.min(p.getMaxAirSupply(), p.getAirSupply() + 20));
        if (prof.knowsTech("meng_chong") && wet) p.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 100, 0, true, false));
        if (prof.knowsTech("fan_shengzhi") && p.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 6; i++) {
                BlockPos at = p.blockPosition().offset(p.getRandom().nextInt(11) - 5, p.getRandom().nextInt(3) - 1, p.getRandom().nextInt(11) - 5);
                BlockState s = sl.getBlockState(at);
                if (s.getBlock() instanceof CropBlock c && !c.isMaxAge(s) && p.getRandom().nextBoolean()) s.randomTick(sl, at, sl.getRandom());
            }
        }
    }

    @SuppressWarnings("unused")
    private static void sound(Player p) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.2F);
    }
}
