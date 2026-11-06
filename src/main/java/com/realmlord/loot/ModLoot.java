package com.realmlord.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.realmlord.RealmLord;
import com.realmlord.territory.TokenGrade;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Loot functions for the mod's chests: graded gear ("realmlord:graded", with a grade or
 * "random"), a blueprint for a random building ("realmlord:blueprint", up to a grade) and a
 * Summoning Token of a rank ("realmlord:summoning").
 */
public final class ModLoot {
    private ModLoot() {}

    public static final DeferredRegister<LootItemFunctionType> FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, RealmLord.MODID);

    public static final RegistryObject<LootItemFunctionType> GRADED = FUNCTIONS.register("graded", () -> new LootItemFunctionType(new Graded.Serializer()));
    public static final RegistryObject<LootItemFunctionType> BLUEPRINT = FUNCTIONS.register("blueprint", () -> new LootItemFunctionType(new Blueprint.Serializer()));
    public static final RegistryObject<LootItemFunctionType> SUMMONING = FUNCTIONS.register("summoning", () -> new LootItemFunctionType(new Summoning.Serializer()));

    /** Grade weights for "random": Bronze 50, Black Iron 30, Silver 15, Gold 5. */
    public static TokenGrade randomGrade(RandomSource r, int maxGrade) {
        int x = r.nextInt(100);
        int g = x < 50 ? 0 : x < 80 ? 1 : x < 95 ? 2 : 3;
        return TokenGrade.values()[Math.min(g, maxGrade)];
    }

    public static class Graded extends LootItemConditionalFunction {
        final String grade;

        Graded(LootItemCondition[] conds, String grade) {
            super(conds);
            this.grade = grade;
        }

        @Override
        protected ItemStack run(ItemStack stack, LootContext ctx) {
            TokenGrade g = "random".equals(grade) ? randomGrade(ctx.getRandom(), 3) : TokenGrade.valueOf(grade.toUpperCase());
            return Gear.make(stack.getItem(), g, ctx.getRandom());
        }

        @Override
        public LootItemFunctionType getType() {
            return GRADED.get();
        }

        public static class Serializer extends LootItemConditionalFunction.Serializer<Graded> {
            @Override
            public void serialize(JsonObject json, Graded f, JsonSerializationContext ctx) {
                super.serialize(json, f, ctx);
                json.addProperty("grade", f.grade);
            }

            @Override
            public Graded deserialize(JsonObject json, JsonDeserializationContext ctx, LootItemCondition[] conds) {
                return new Graded(conds, GsonHelper.getAsString(json, "grade", "random"));
            }
        }
    }

    public static class Blueprint extends LootItemConditionalFunction {
        final int maxGrade;

        Blueprint(LootItemCondition[] conds, int maxGrade) {
            super(conds);
            this.maxGrade = maxGrade;
        }

        @Override
        protected ItemStack run(ItemStack stack, LootContext ctx) {
            return Blueprints.random(ctx.getRandom(), maxGrade);
        }

        @Override
        public LootItemFunctionType getType() {
            return BLUEPRINT.get();
        }

        public static class Serializer extends LootItemConditionalFunction.Serializer<Blueprint> {
            @Override
            public void serialize(JsonObject json, Blueprint f, JsonSerializationContext ctx) {
                super.serialize(json, f, ctx);
                json.addProperty("max_grade", f.maxGrade);
            }

            @Override
            public Blueprint deserialize(JsonObject json, JsonDeserializationContext ctx, LootItemCondition[] conds) {
                return new Blueprint(conds, GsonHelper.getAsInt(json, "max_grade", 1));
            }
        }
    }

    public static class Summoning extends LootItemConditionalFunction {
        final int rank;

        Summoning(LootItemCondition[] conds, int rank) {
            super(conds);
            this.rank = rank;
        }

        @Override
        protected ItemStack run(ItemStack stack, LootContext ctx) {
            return com.realmlord.item.SummoningTokenItem.of(rank);
        }

        @Override
        public LootItemFunctionType getType() {
            return SUMMONING.get();
        }

        public static class Serializer extends LootItemConditionalFunction.Serializer<Summoning> {
            @Override
            public void serialize(JsonObject json, Summoning f, JsonSerializationContext ctx) {
                super.serialize(json, f, ctx);
                json.addProperty("rank", f.rank);
            }

            @Override
            public Summoning deserialize(JsonObject json, JsonDeserializationContext ctx, LootItemCondition[] conds) {
                return new Summoning(conds, GsonHelper.getAsInt(json, "rank", 0));
            }
        }
    }
}
