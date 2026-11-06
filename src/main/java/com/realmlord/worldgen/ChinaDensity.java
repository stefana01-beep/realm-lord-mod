package com.realmlord.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * A density function that reads one terrain channel from the China map, with a vanilla noise
 * function as fine detail, and another (the fallback, by default the detail itself) used beyond
 * the map's edges. Used by the China world's noise settings.
 */
public record ChinaDensity(String channel, DensityFunction detail, double detailAmount, java.util.Optional<DensityFunction> fallback) implements DensityFunction {
    public static final MapCodec<ChinaDensity> DATA = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("channel").forGetter(ChinaDensity::channel),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("detail").forGetter(ChinaDensity::detail),
            Codec.DOUBLE.fieldOf("detail_amount").forGetter(ChinaDensity::detailAmount),
            DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("fallback").forGetter(ChinaDensity::fallback)
    ).apply(i, ChinaDensity::new));
    public static final KeyDispatchDataCodec<ChinaDensity> CODEC = KeyDispatchDataCodec.of(DATA);

    @Override
    public double compute(FunctionContext ctx) {
        double d = detail.compute(ctx);
        double f = fallback.isPresent() ? fallback.get().compute(ctx) : d;
        return ChinaMap.value(channel, ctx.blockX(), ctx.blockZ(), d, detailAmount, f);
    }

    @Override
    public void fillArray(double[] values, ContextProvider provider) {
        provider.fillAllDirectly(values, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ChinaDensity(channel, detail.mapAll(visitor), detailAmount, fallback.map(f -> f.mapAll(visitor))));
    }

    @Override
    public double minValue() {
        return -5.0;
    }

    @Override
    public double maxValue() {
        return 5.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
