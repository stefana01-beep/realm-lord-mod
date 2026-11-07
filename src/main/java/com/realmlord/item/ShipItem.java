package com.realmlord.item;

import com.realmlord.ship.ShipKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.function.Supplier;

/** A ship, launched like a boat: right-click on water. */
public class ShipItem extends Item {
    private final ShipKind kind;
    private final Supplier<? extends EntityType<? extends Boat>> type;

    public ShipItem(Properties p, ShipKind kind, Supplier<? extends EntityType<? extends Boat>> type) {
        super(p);
        this.kind = kind;
        this.type = type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        Boat boat = type.get().create(level);
        if (boat == null) return InteractionResultHolder.fail(stack);
        var at = hit.getLocation();
        boat.setPos(at.x, at.y, at.z);
        boat.setYRot(player.getYRot());
        if (!level.noCollision(boat, boat.getBoundingBox())) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.realmlord." + kind.id() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("realmlord.ship.stats", kind.seats, Math.round(kind.topSpeed / 0.4 * 100), Math.round(kind.toughness * 100))
                .withStyle(ChatFormatting.YELLOW));
    }
}
