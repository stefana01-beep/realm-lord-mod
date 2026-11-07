package com.realmlord.work;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A crafting recipe that only works for a player who has learned its technical manual. It wraps
 * an ordinary shaped or shapeless recipe: {"type": "realmlord:manual", "manual": id, "recipe": {...}}.
 */
public class ManualRecipe implements CraftingRecipe {
    private final ResourceLocation id;
    private final String manual;
    private final CraftingRecipe inner;

    public ManualRecipe(ResourceLocation id, String manual, CraftingRecipe inner) {
        this.id = id;
        this.manual = manual;
        this.inner = inner;
    }

    public String manual() {
        return manual;
    }

    public CraftingRecipe inner() {
        return inner;
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        if (!inner.matches(inv, level)) return false;
        if (level.isClientSide) return TechManuals.CLIENT_KNOWN.contains(manual);
        ServerPlayer p = crafter(inv, level);
        return p != null && TechManuals.knows(p, manual);
    }

    /** The player whose open crafting grid this is. */
    public static ServerPlayer crafter(CraftingContainer inv, Level level) {
        if (!(level instanceof ServerLevel sl)) return null;
        for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
            for (Slot s : p.containerMenu.slots) if (s.container == inv) return p;
            for (Slot s : p.inventoryMenu.slots) if (s.container == inv) return p;
        }
        return null;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        return inner.assemble(inv, access);
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return inner.canCraftInDimensions(w, h);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return inner.getResultItem(access);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inner.getIngredients();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        return inner.getRemainingItems(inv);
    }

    @Override
    public String getGroup() {
        return inner.getGroup();
    }

    @Override
    public CraftingBookCategory category() {
        return inner.category();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return com.realmlord.registry.ModRecipes.MANUAL.get();
    }

    public static class Serializer implements RecipeSerializer<ManualRecipe> {
        @Override
        public ManualRecipe fromJson(ResourceLocation id, JsonObject json) {
            String manual = GsonHelper.getAsString(json, "manual");
            JsonObject in = GsonHelper.getAsJsonObject(json, "recipe");
            ResourceLocation type = new ResourceLocation(GsonHelper.getAsString(in, "type"));
            RecipeSerializer<?> s = ForgeRegistries.RECIPE_SERIALIZERS.getValue(type);
            if (s == null) throw new IllegalArgumentException("Unknown recipe type " + type);
            Recipe<?> r = s.fromJson(id, in);
            if (!(r instanceof CraftingRecipe c)) throw new IllegalArgumentException("Not a crafting recipe: " + type);
            return new ManualRecipe(id, manual, c);
        }

        @Override
        @SuppressWarnings("unchecked")
        public ManualRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String manual = buf.readUtf();
            ResourceLocation type = buf.readResourceLocation();
            RecipeSerializer<?> s = ForgeRegistries.RECIPE_SERIALIZERS.getValue(type);
            Recipe<?> r = s.fromNetwork(id, buf);
            return new ManualRecipe(id, manual, (CraftingRecipe) r);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void toNetwork(FriendlyByteBuf buf, ManualRecipe r) {
            buf.writeUtf(r.manual);
            RecipeSerializer s = r.inner.getSerializer();
            buf.writeResourceLocation(ForgeRegistries.RECIPE_SERIALIZERS.getKey(s));
            s.toNetwork(buf, r.inner);
        }
    }
}
