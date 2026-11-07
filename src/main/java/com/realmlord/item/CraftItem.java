package com.realmlord.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.work.TechManuals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.UUID;

/**
 * The things the technical manuals teach to make that do something: food and drink with
 * effects, medicine, animal care, tools and charms. One class, by kind.
 */
public class CraftItem extends Item {
    public enum Kind {
        FOOD, DRINK, DETOX, CLARITY, RHINO_PILL, PEIYUAN, NEEDLES, FODDER, HORSE_FEED, BAMBOO_FEED, ANIMAL_SALVE, HORSE_TONIC,
        WHISTLE, LUOPAN, NET, TALISMAN, CHARIOT, WOODEN_OX, DIVING_BELL, JADE, PLAIN
    }

    private static final UUID JADE_LUCK = UUID.fromString("5b3f6e1a-8d2c-4e7b-9a10-2f4c6d8e0a11");
    private static final UUID JADE_HEALTH = UUID.fromString("5b3f6e1a-8d2c-4e7b-9a10-2f4c6d8e0a12");

    public final Kind kind;

    public CraftItem(Properties p, Kind kind) {
        super(p);
        this.kind = kind;
    }

    // ------------------------------------------------------------------ eating and drinking

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return switch (kind) {
            case DRINK, DETOX -> UseAnim.DRINK;
            case CLARITY, RHINO_PILL, PEIYUAN, FOOD -> UseAnim.EAT;
            default -> UseAnim.NONE;
        };
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return switch (kind) {
            case DRINK, DETOX -> 32;
            case CLARITY, RHINO_PILL, PEIYUAN -> 20;
            case FOOD -> stack.getItem().getFoodProperties() != null && stack.getItem().getFoodProperties().isFastFood() ? 16 : 32;
            default -> 0;
        };
    }

    private boolean consumable() {
        return getUseDuration(ItemStack.EMPTY) > 0 || kind == Kind.FOOD;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity who) {
        if (!level.isClientSide) {
            switch (kind) {
                case DETOX -> {
                    who.removeEffect(MobEffects.POISON);
                    who.removeEffect(MobEffects.CONFUSION);
                }
                case CLARITY -> {
                    for (var e : List.of(MobEffects.CONFUSION, MobEffects.BLINDNESS, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS,
                            MobEffects.DIG_SLOWDOWN, MobEffects.HUNGER, MobEffects.DARKNESS)) who.removeEffect(e);
                }
                case RHINO_PILL -> {
                    who.removeEffect(MobEffects.POISON);
                    who.removeEffect(MobEffects.WITHER);
                    who.getPersistentData().putLong(com.realmlord.work.TradeArts.ANTIDOTE, level.getGameTime() + 2400);
                    if (who instanceof Player p) p.displayClientMessage(Component.translatable("realmlord.craft.antidote").withStyle(ChatFormatting.GREEN), true);
                }
                case PEIYUAN -> {
                    who.setHealth(who.getMaxHealth());
                    who.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 1));
                    who.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1));
                }
                case DRINK -> {
                    if (this == com.realmlord.registry.ModCrafts.GREEN_TEA.get()) who.removeEffect(MobEffects.HUNGER);
                }
                default -> { }
            }
        }
        boolean creative = who instanceof Player p && p.getAbilities().instabuild;
        if (stack.isEdible()) {
            ItemStack rest = super.finishUsingItem(stack, level, who);
            return container(rest, who);
        }
        if (!creative) stack.shrink(1);
        return container(stack, who);
    }

    /** Drinks leave their bottle, soup its bowl. */
    private ItemStack container(ItemStack stack, LivingEntity who) {
        Item back = kind == Kind.DRINK || kind == Kind.DETOX ? Items.GLASS_BOTTLE
                : this == com.realmlord.registry.ModCrafts.NOODLE_SOUP.get() ? Items.BOWL : null;
        if (back == null || who instanceof Player p && p.getAbilities().instabuild) return stack;
        if (stack.isEmpty()) return new ItemStack(back);
        if (who instanceof Player p && !p.getInventory().add(new ItemStack(back))) p.drop(new ItemStack(back), false);
        return stack;
    }

    // ------------------------------------------------------------------ using

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        switch (kind) {
            case DETOX, CLARITY, RHINO_PILL, PEIYUAN, DRINK -> {
                return net.minecraft.world.item.ItemUtils.startUsingInstantly(level, player, hand);
            }
            case NEEDLES -> {
                // On yourself, crouching.
                if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
                if (!level.isClientSide) treat(player, player, stack, hand);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            case WHISTLE -> {
                if (!level.isClientSide && player instanceof ServerPlayer sp) {
                    int n = 0;
                    for (LivingEntity e : sp.serverLevel().getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(128), x -> com.realmlord.work.TradeArts.ownedBy(x, sp))) {
                        e.teleportTo(sp.getX() + sp.getRandom().nextInt(5) - 2, sp.getY(), sp.getZ() + sp.getRandom().nextInt(5) - 2);
                        n++;
                    }
                    level.playSound(null, sp.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.PLAYERS, 0.7F, 1.6F);
                    sp.displayClientMessage(Component.translatable("realmlord.craft.whistle", n), true);
                    sp.getCooldowns().addCooldown(this, 200);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            case LUOPAN -> {
                if (!level.isClientSide && player instanceof ServerPlayer sp) {
                    var data = com.realmlord.territory.TerritoryData.get(sp.server);
                    var t = data.at(sp.level().dimension(), sp.blockPosition());
                    Component where = t == null ? Component.translatable("realmlord.craft.luopan.wild")
                            : Component.translatable("realmlord.craft.luopan.territory", t.name, (int) Math.sqrt(t.center.distSqr(sp.blockPosition())));
                    var biome = sp.level().getBiome(sp.blockPosition()).unwrapKey().map(k -> Component.translatable("biome." + k.location().getNamespace() + "." + k.location().getPath()))
                            .orElse(Component.empty());
                    sp.displayClientMessage(Component.translatable("realmlord.craft.luopan", where, sp.blockPosition().getY(), biome), false);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            case CHARIOT -> {
                if (!level.isClientSide && player instanceof ServerPlayer sp) {
                    var prof = com.realmlord.profile.Profiles.get(sp);
                    var t = prof.getTerritoryId() >= 0 ? com.realmlord.territory.TerritoryData.get(sp.server).byId(prof.getTerritoryId()) : null;
                    if (t == null) sp.displayClientMessage(Component.translatable("realmlord.craft.chariot.none"), false);
                    else {
                        double dx = t.center.getX() - sp.getX(), dz = t.center.getZ() - sp.getZ();
                        int dir = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(-dx, dz)) / 45.0), 8);
                        sp.displayClientMessage(Component.translatable("realmlord.craft.chariot", (int) Math.sqrt(dx * dx + dz * dz),
                                Component.translatable("realmlord.craft.dir." + dir)), false);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            case NET -> {
                BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
                if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(net.minecraft.tags.FluidTags.WATER)) {
                    if (!level.isClientSide) player.displayClientMessage(Component.translatable("realmlord.craft.net_water"), true);
                    return InteractionResultHolder.fail(stack);
                }
                if (!level.isClientSide && level instanceof ServerLevel sl) {
                    level.playSound(null, hit.getBlockPos(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.8F, 0.9F);
                    int tier = Math.max(0, TechManuals.tradeTier(player, WorkOccupation.FISHERMAN));
                    if (player.getRandom().nextFloat() < 0.45F + 0.1F * tier) {
                        LootParams params = new LootParams.Builder(sl).withParameter(LootContextParams.ORIGIN, hit.getLocation())
                                .withParameter(LootContextParams.TOOL, stack).withParameter(LootContextParams.THIS_ENTITY, player)
                                .withLuck(player.getLuck()).create(LootContextParamSets.FISHING);
                        for (ItemStack f : sl.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING_FISH).getRandomItems(params)) {
                            if (!player.getInventory().add(f)) player.drop(f, false);
                        }
                        com.realmlord.work.TradeArts.workXp(player, WorkOccupation.FISHERMAN, 3);
                    } else player.displayClientMessage(Component.translatable("realmlord.craft.net_empty"), true);
                    stack.hurtAndBreak(1, player, pl -> pl.broadcastBreakEvent(hand));
                    player.getCooldowns().addCooldown(this, 60);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            case WOODEN_OX -> {
                if (!level.isClientSide && level instanceof ServerLevel sl) {
                    Mule ox = EntityType.MULE.create(sl);
                    if (ox != null) {
                        ox.moveTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
                        ox.finalizeSpawn(sl, sl.getCurrentDifficultyAt(player.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                        ox.tameWithName(player);
                        ox.setChest(true);
                        ox.equipSaddle(null);
                        ox.setCustomName(Component.translatable("realmlord.craft.ox_name"));
                        sl.addFreshEntity(ox);
                        if (!player.getAbilities().instabuild) stack.shrink(1);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            case DIVING_BELL -> {
                if (!level.isClientSide) {
                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0));
                    stack.hurtAndBreak(1, player, pl -> pl.broadcastBreakEvent(hand));
                    player.getCooldowns().addCooldown(this, 100);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            default -> {
                return super.use(level, player, hand);
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (kind != Kind.TALISMAN) return super.useOn(ctx);
        Level level = ctx.getLevel();
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        BlockPos c = ctx.getClickedPos();
        for (int round = 0; round < 4; round++)
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-4, -1, -4), c.offset(4, 2, 4))) {
                BlockState s = sl.getBlockState(p);
                if (s.getBlock() instanceof BonemealableBlock b && !(s.getBlock() instanceof net.minecraft.world.level.block.GrassBlock)
                        && b.isValidBonemealTarget(sl, p, s, false)) b.performBonemeal(sl, sl.getRandom(), p.immutable(), s);
            }
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 40, 3, 0.5, 3, 0);
        if (ctx.getPlayer() != null) {
            ctx.getPlayer().displayClientMessage(Component.translatable("realmlord.craft.talisman").withStyle(ChatFormatting.GREEN), true);
            if (!ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();
        switch (kind) {
            case NEEDLES -> {
                if (!level.isClientSide) treat(player, target, stack, hand);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            case FODDER -> {
                if (!(target instanceof Animal a) || a instanceof AbstractHorse || a instanceof Panda) return InteractionResult.PASS;
                return breed(a, player, stack, level);
            }
            case BAMBOO_FEED -> {
                if (!(target instanceof Panda a)) return InteractionResult.PASS;
                return breed(a, player, stack, level);
            }
            case HORSE_FEED -> {
                if (!(target instanceof AbstractHorse h)) return InteractionResult.PASS;
                if (!level.isClientSide) {
                    h.heal(10);
                    h.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3 * 60 * 20, 0));
                    if (!h.isTamed()) h.modifyTemper(40);
                    use(player, stack);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            case ANIMAL_SALVE -> {
                if (!(target instanceof Animal a)) return InteractionResult.PASS;
                if (!level.isClientSide) {
                    a.setHealth(a.getMaxHealth());
                    a.removeAllEffects();
                    a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                    com.realmlord.work.TradeArts.workXp(player, WorkOccupation.VETERINARIAN, 4);
                    use(player, stack);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            case HORSE_TONIC -> {
                if (!(target instanceof AbstractHorse h)) return InteractionResult.PASS;
                if (!level.isClientSide) {
                    h.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 1));
                    h.addEffect(new MobEffectInstance(MobEffects.JUMP, 6000, 1));
                    h.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 0));
                    com.realmlord.work.TradeArts.workXp(player, WorkOccupation.VETERINARIAN, 4);
                    use(player, stack);
                    if (!player.getAbilities().instabuild && !player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            default -> {
                return super.interactLivingEntity(stack, player, target, hand);
            }
        }
    }

    private static InteractionResult breed(Animal a, Player player, ItemStack stack, Level level) {
        if (a.isBaby()) {
            if (!level.isClientSide) {
                a.ageUp(Math.abs(a.getAge() / 20) * 2, true);
                use(player, stack);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (a.getAge() != 0 || a.isInLove()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            a.setInLove(player);
            com.realmlord.work.TradeArts.workXp(player, WorkOccupation.BREEDER, 2);
            use(player, stack);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void use(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    /** Acupuncture: heals and clears harmful effects; a physician heals more. */
    private void treat(Player doctor, LivingEntity patient, ItemStack stack, InteractionHand hand) {
        int tier = Math.max(-1, TechManuals.tradeTier(doctor, WorkOccupation.PHYSICIAN));
        float amount = 6.0F + (tier + 1) * 2.0F;
        patient.heal(amount);
        for (MobEffectInstance e : List.copyOf(patient.getActiveEffects()))
            if (!e.getEffect().isBeneficial()) patient.removeEffect(e.getEffect());
        doctor.level().playSound(null, patient.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.6F);
        doctor.displayClientMessage(Component.translatable("realmlord.craft.healed", patient.getDisplayName()), true);
        com.realmlord.work.TradeArts.workXp(doctor, WorkOccupation.PHYSICIAN, 5);
        stack.hurtAndBreak(1, doctor, pl -> pl.broadcastBreakEvent(hand));
        doctor.getCooldowns().addCooldown(this, 40);
    }

    // ------------------------------------------------------------------ jade

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (kind == Kind.JADE && slot == EquipmentSlot.OFFHAND) {
            return ImmutableMultimap.of(Attributes.LUCK, new AttributeModifier(JADE_LUCK, "Jade pendant", 2.0, AttributeModifier.Operation.ADDITION),
                    Attributes.MAX_HEALTH, new AttributeModifier(JADE_HEALTH, "Jade pendant", 2.0, AttributeModifier.Operation.ADDITION));
        }
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
