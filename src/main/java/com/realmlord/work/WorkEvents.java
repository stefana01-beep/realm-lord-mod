package com.realmlord.work;

import com.realmlord.RealmLord;
import com.realmlord.profile.Profile;
import com.realmlord.profile.ProfileCapability;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.Stat;
import com.realmlord.profile.WorkOccupation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerBrewedPotionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Work occupations learn by doing their trade, and get better at it as they rise from
 * Junior to Master. Lords whose sub-profession is a trade share in this too.
 */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorkEvents {
    private WorkEvents() {}

    private static Profile prof(Player p) {
        return ProfileCapability.get(p).orElse(null);
    }

    /** The player's occupation tier (0-3) if it is the given trade, otherwise -1. */
    public static int tier(Player p, WorkOccupation w) {
        Profile prof = prof(p);
        if (prof == null || prof.getWork() != w) return -1;
        return WorkOccupation.tier(prof.getWorkLevel());
    }

    private static void xp(Player p, WorkOccupation w, int amount) {
        if (p instanceof ServerPlayer sp && tier(p, w) >= 0) Profiles.grantWorkXp(sp, amount);
    }

    // ------------------------------------------------------------------ crafting

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide) return;
        ItemStack out = event.getCrafting();
        if (out.isEmpty()) return;
        // Blacksmith: metal weapons, tools and armour. Higher ranks forge them sturdier.
        if (isMetalGear(out)) {
            int t = tier(p, WorkOccupation.BLACKSMITH);
            if (t >= 0) {
                xp(p, WorkOccupation.BLACKSMITH, 12);
                if (t >= 1 && out.isDamageableItem()) out.enchant(Enchantments.UNBREAKING, t);
            }
        }
        // Tailor: leather armour and cloth goods. Higher ranks stitch in protection.
        if (out.getItem() instanceof ArmorItem a && a.getMaterial() == ArmorMaterials.LEATHER) {
            int t = tier(p, WorkOccupation.TAILOR);
            if (t >= 0) {
                xp(p, WorkOccupation.TAILOR, 10);
                if (t >= 1) out.enchant(Enchantments.ALL_DAMAGE_PROTECTION, t);
            }
        } else if (out.is(ItemTags.WOOL) || out.is(ItemTags.WOOL_CARPETS) || out.is(ItemTags.BEDS) || out.is(ItemTags.BANNERS)) {
            xp(p, WorkOccupation.TAILOR, 3);
        }
        // Carpenter: wooden goods.
        if (out.is(ItemTags.PLANKS) || out.is(Items.STICK) || out.is(ItemTags.WOODEN_DOORS) || out.is(ItemTags.WOODEN_FENCES)
                || out.is(ItemTags.WOODEN_STAIRS) || out.is(ItemTags.WOODEN_SLABS) || out.is(Items.CHEST) || out.is(Items.CRAFTING_TABLE)
                || out.is(Items.BARREL) || out.is(ItemTags.SIGNS)) {
            xp(p, WorkOccupation.CARPENTER, 2);
        }
        // Shipbuilder: boats. Higher ranks waste less timber.
        if (out.is(ItemTags.BOATS) || out.is(ItemTags.CHEST_BOATS)) {
            int t = tier(p, WorkOccupation.SHIPBUILDER);
            if (t >= 0) {
                xp(p, WorkOccupation.SHIPBUILDER, 15);
                if (p.getRandom().nextFloat() < 0.15F * (t + 1)) p.getInventory().add(new ItemStack(Items.OAK_PLANKS, 2));
            }
        }
        // Mason: dressed stone.
        if (out.is(ItemTags.STONE_BRICKS) || out.is(ItemTags.WALLS) || out.is(Items.STONE_BRICK_STAIRS) || out.is(Items.STONE_BRICK_SLAB)
                || out.is(Items.POLISHED_ANDESITE) || out.is(Items.POLISHED_DIORITE) || out.is(Items.POLISHED_GRANITE)) {
            xp(p, WorkOccupation.MASON, 2);
        }
        // Cook: dishes.
        if (out.isEdible()) xp(p, WorkOccupation.COOK, 4);
        // Winemaker: honey, sugar and brewing ingredients.
        if (out.is(Items.HONEY_BOTTLE) || out.is(Items.SUGAR) || out.is(Items.FERMENTED_SPIDER_EYE) || out.is(Items.GLISTERING_MELON_SLICE)) {
            xp(p, WorkOccupation.WINEMAKER, 4);
        }
    }

    private static boolean isMetalGear(ItemStack s) {
        if (s.getItem() instanceof TieredItem ti) return ti.getTier() != Tiers.WOOD && ti.getTier() != Tiers.STONE;
        if (s.getItem() instanceof ArmorItem a) return a.getMaterial() != ArmorMaterials.LEATHER;
        return s.is(Items.SHIELD) || s.is(Items.SHEARS) || s.is(Items.FLINT_AND_STEEL);
    }

    @SubscribeEvent
    public static void smelted(PlayerEvent.ItemSmeltedEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide) return;
        ItemStack out = event.getSmelting();
        if (out.isEdible()) xp(p, WorkOccupation.COOK, 3 * Math.max(1, out.getCount()));
        if (out.is(Tags.Items.INGOTS)) xp(p, WorkOccupation.BLACKSMITH, 2 * Math.max(1, out.getCount()));
        if (out.is(Items.STONE) || out.is(Items.SMOOTH_STONE) || out.is(Items.BRICK)) xp(p, WorkOccupation.MASON, Math.max(1, out.getCount()));
    }

    // ------------------------------------------------------------------ brewing and drinking

    @SubscribeEvent
    public static void brewed(PlayerBrewedPotionEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide) return;
        xp(p, WorkOccupation.PHYSICIAN, 8);
        xp(p, WorkOccupation.WINEMAKER, 8);
        int t = tier(p, WorkOccupation.PHYSICIAN);
        if (t >= 1 && p.getRandom().nextFloat() < 0.1F * t) p.getInventory().add(event.getStack().copy());
    }

    @SubscribeEvent
    public static void finishedUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack used = event.getItem();
        // Cook: meals fill a cook more.
        if (used.isEdible()) {
            int t = tier(p, WorkOccupation.COOK);
            if (t >= 0) {
                var food = used.getFoodProperties(p);
                if (food != null) p.getFoodData().eat(1 + t, 0.1F * (t + 1));
            }
        }
        // Winemaker: brews last longer in a winemaker's hands.
        if (used.getItem() instanceof PotionItem) {
            int t = tier(p, WorkOccupation.WINEMAKER);
            if (t >= 0) {
                for (MobEffectInstance i : PotionUtils.getMobEffects(used)) {
                    if (i.getEffect().isInstantenous()) continue;
                    MobEffectInstance cur = p.getEffect(i.getEffect());
                    if (cur != null) {
                        p.addEffect(new MobEffectInstance(cur.getEffect(), (int) (cur.getDuration() * (1.0 + 0.15 * (t + 1))),
                                cur.getAmplifier(), cur.isAmbient(), cur.isVisible(), cur.showIcon()));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void heal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide) return;
        int t = tier(p, WorkOccupation.PHYSICIAN);
        if (t >= 0) event.setAmount(event.getAmount() * (1.0F + 0.1F * (t + 1)));
    }

    // ------------------------------------------------------------------ the land

    @SubscribeEvent
    public static void broke(BlockEvent.BreakEvent event) {
        Player p = event.getPlayer();
        if (p == null || p.level().isClientSide || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockState state = event.getState();
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            int t = tier(p, WorkOccupation.FARMER);
            if (t >= 0) {
                xp(p, WorkOccupation.FARMER, 3);
                if (p.getRandom().nextFloat() < 0.12F * (t + 1)) {
                    List<ItemStack> drops = Block.getDrops(state, level, event.getPos(), null);
                    if (!drops.isEmpty()) Block.popResource(level, event.getPos(), drops.get(p.getRandom().nextInt(drops.size())).copy());
                }
            }
        } else if (state.is(BlockTags.LOGS)) {
            xp(p, WorkOccupation.CARPENTER, 1);
        } else if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.STONE)) {
            xp(p, WorkOccupation.MASON, 1);
        }
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        BlockState state = event.getState();
        int t = -1;
        if (state.is(BlockTags.LOGS)) t = tier(p, WorkOccupation.CARPENTER);
        else if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.STONE)) t = tier(p, WorkOccupation.MASON);
        if (t >= 0) event.setNewSpeed(event.getNewSpeed() * (1.0F + 0.15F * (t + 1)));
    }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide) {
            if (p.getRandom().nextInt(3) == 0) xp(p, WorkOccupation.ARCHITECT, 1);
        }
    }

    @SubscribeEvent
    public static void tilled(BlockEvent.BlockToolModificationEvent event) {
        if (event.getPlayer() != null && !event.isSimulated() && !event.getPlayer().level().isClientSide
                && event.getToolAction() == net.minecraftforge.common.ToolActions.HOE_TILL) {
            xp(event.getPlayer(), WorkOccupation.FARMER, 1);
        }
    }

    @SubscribeEvent
    public static void fished(ItemFishedEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide) return;
        int t = tier(p, WorkOccupation.FISHERMAN);
        if (t < 0) return;
        xp(p, WorkOccupation.FISHERMAN, 8);
        if (!event.getDrops().isEmpty() && p.getRandom().nextFloat() < 0.15F * (t + 1)) {
            ItemStack extra = event.getDrops().get(0).copy();
            if (!p.getInventory().add(extra)) p.drop(extra, false);
        }
    }

    // ------------------------------------------------------------------ animals

    @SubscribeEvent
    public static void bred(BabyEntitySpawnEvent event) {
        Player p = event.getCausedByPlayer();
        if (p == null || p.level().isClientSide || !(p.level() instanceof ServerLevel level)) return;
        int t = tier(p, WorkOccupation.BREEDER);
        xp(p, WorkOccupation.VETERINARIAN, 3);
        if (t < 0) return;
        xp(p, WorkOccupation.BREEDER, 10);
        if (event.getParentA() instanceof AgeableMob a && event.getParentB() instanceof AgeableMob b
                && event.getChild() != null && p.getRandom().nextFloat() < 0.1F * (t + 1)) {
            AgeableMob twin = a.getBreedOffspring(level, b);
            if (twin != null) {
                twin.setBaby(true);
                twin.moveTo(a.getX(), a.getY(), a.getZ(), 0.0F, 0.0F);
                level.addFreshEntity(twin);
            }
        }
    }

    @SubscribeEvent
    public static void fed(PlayerInteractEvent.EntityInteract event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || !(event.getTarget() instanceof Animal animal)) return;
        if (!animal.isFood(event.getItemStack())) return;
        int t = tier(p, WorkOccupation.VETERINARIAN);
        if (t < 0) return;
        xp(p, WorkOccupation.VETERINARIAN, 2);
        animal.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100 * (t + 1), 1));
    }

    @SubscribeEvent
    public static void tamed(AnimalTameEvent event) {
        xp(event.getTamer(), WorkOccupation.VETERINARIAN, 15);
    }

    /** Veterinarians' animals are hardier; a commander's animals hit harder. */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (victim instanceof OwnableEntity own && own.getOwner() instanceof Player owner) {
            int t = tier(owner, WorkOccupation.VETERINARIAN);
            if (t >= 0) event.setAmount(event.getAmount() * (1.0F - 0.1F * (t + 1)));
        }
        if (event.getSource().getEntity() instanceof OwnableEntity own && own.getOwner() instanceof Player owner) {
            Profile prof = prof(owner);
            if (prof != null && prof.hasChosen()) {
                event.setAmount(event.getAmount() * (1.0F + Math.min(1.0F, prof.stat(Stat.COMMAND) * 0.01F)));
            }
        }
    }

    /** Placing blocks counts toward Architect, used by later construction. */
    public static boolean isBuildingBlock(ItemStack s) {
        return s.getItem() instanceof BlockItem;
    }
}
