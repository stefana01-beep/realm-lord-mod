package com.realmlord.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.OpenScreenPacket;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.Stat;
import com.realmlord.quest.QuestLogic;
import com.realmlord.quest.VillageQuest;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.territory.TokenGrade;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Locale;

/**
 * /realm info · /realm territories (anyone) and admin commands: reset, level, worklevel,
 * merit, points, token, quest, role.
 */
public final class RealmCommand {
    private RealmCommand() {}

    /** Approves or dismisses an official's proposal (the links in their chat message). */
    private static int council(CommandContext<CommandSourceStack> c, boolean approve) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer p = c.getSource().getPlayerOrException();
        int tid = Profiles.get(p).getTerritoryId();
        Territory t = tid >= 0 ? TerritoryData.get(p.server).byId(tid) : null;
        if (t == null) {
            c.getSource().sendFailure(Component.translatable("realmlord.council.err.no_territory"));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(c, "id");
        String err = approve ? com.realmlord.village.Council.approve(p, t, id) : com.realmlord.village.Council.dismiss(p, t, id);
        if (err != null) c.getSource().sendFailure(Component.translatable(err));
        else if (!approve) c.getSource().sendSuccess(() -> Component.translatable("realmlord.council.dismissed"), false);
        return err == null ? 1 : 0;
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("realm")
                .executes(c -> info(c, c.getSource().getPlayerOrException()))
                .then(Commands.literal("info")
                        .executes(c -> info(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2))
                                .executes(c -> info(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("territories").executes(RealmCommand::territories))
                .then(Commands.literal("council")
                        .then(Commands.literal("approve").then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(c -> council(c, true))))
                        .then(Commands.literal("dismiss").then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(c -> council(c, false)))))
                .then(Commands.literal("capitals").executes(c -> {
                    for (int i = 0; i < com.realmlord.worldgen.ChinaMap.CAPITALS.length; i++) {
                        final int k = i;
                        double[] ll = com.realmlord.worldgen.ChinaMap.CAPITAL_LONLAT[k];
                        int[] pos = com.realmlord.worldgen.ChinaMap.blockOf(ll[0], ll[1]);
                        c.getSource().sendSuccess(() -> Component.translatable("realmlord.cmd.capital", com.realmlord.worldgen.ChinaMap.CAPITALS[k], pos[0], pos[1]), false);
                    }
                    c.getSource().sendSuccess(() -> Component.translatable("realmlord.cmd.capitals_note"), false);
                    return 1;
                }))
                .then(Commands.literal("spawn").requires(s -> s.hasPermission(2)).executes(c -> {
                    com.realmlord.territory.SpawnPicker.offer(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("speed").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("times", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 60)).executes(c -> {
                            int n = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "times");
                            com.realmlord.village.Village.speed = n;
                            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("realmlord.speed.set", n), true);
                            return n;
                        })))
                .then(Commands.literal("beastwave").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("start").executes(c -> {
                            com.realmlord.world.Beastwave.force(c.getSource().getServer(), true);
                            return 1;
                        }))
                        .then(Commands.literal("stop").executes(c -> {
                            com.realmlord.world.Beastwave.force(c.getSource().getServer(), false);
                            return 1;
                        }))
                        .executes(c -> {
                            long today = com.realmlord.village.GaiaCalendar.today(c.getSource().getServer());
                            int y = com.realmlord.world.Beastwave.nextWaveYear(today);
                            long left = com.realmlord.world.Beastwave.newYearDay(y) - today;
                            c.getSource().sendSuccess(() -> Component.translatable("realmlord.beastwave.next", y, left), false);
                            return 1;
                        }))
                .then(Commands.literal("den").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    var w = com.realmlord.world.WorldState.get(p.server);
                    var at = p.blockPosition().relative(p.getDirection(), 16);
                    int y = com.realmlord.village.Village.groundAt(p.serverLevel(), at.getX(), at.getZ());
                    w.dens.add(com.realmlord.world.Dens.create(p.serverLevel(), new net.minecraft.core.BlockPos(at.getX(), y, at.getZ())));
                    w.touch();
                    return 1;
                }))
                .then(Commands.literal("guide").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ItemStack g = new ItemStack(ModItems.VILLAGE_GUIDE.get());
                    if (!p.getInventory().add(g)) p.drop(g, false);
                    return 1;
                }))
                .then(Commands.literal("trust")
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> trust(c, EntityArgument.getPlayer(c, "player"), true))))
                .then(Commands.literal("untrust")
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> trust(c, EntityArgument.getPlayer(c, "player"), false))))
                .then(Commands.literal("trusted").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    com.realmlord.territory.Territory t = com.realmlord.territory.TerritoryData.get(p.server).ownedBy(p.getUUID());
                    if (t == null) {
                        c.getSource().sendFailure(Component.translatable("realmlord.protect.no_territory"));
                        return 0;
                    }
                    java.util.List<String> names = new java.util.ArrayList<>();
                    for (java.util.UUID u : t.trusted) {
                        var prof = p.server.getProfileCache() == null ? java.util.Optional.<com.mojang.authlib.GameProfile>empty() : p.server.getProfileCache().get(u);
                        names.add(prof.map(com.mojang.authlib.GameProfile::getName).orElse(u.toString()));
                    }
                    c.getSource().sendSuccess(() -> Component.translatable("realmlord.protect.list", names.isEmpty() ? "-" : String.join(", ", names)), false);
                    return 1;
                }))
                .then(Commands.literal("camp")
                        .then(Commands.literal("keep").executes(c -> QuestLogic.decideCamp(c.getSource().getPlayerOrException(), false) ? 1 : 0))
                        .then(Commands.literal("salvage").executes(c -> QuestLogic.decideCamp(c.getSource().getPlayerOrException(), true) ? 1 : 0))
                        .then(Commands.literal("travel").executes(c -> QuestLogic.travel(c.getSource().getPlayerOrException()) ? 1 : 0))
                        .then(Commands.literal("return").executes(c -> QuestLogic.returnHome(c.getSource().getPlayerOrException()) ? 1 : 0)))
                .then(Commands.literal("role").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ModNetwork.send(p, new OpenScreenPacket(Profiles.get(p).hasChosen() ? OpenScreenPacket.CHARACTER : OpenScreenPacket.ROLE));
                    return 1;
                }))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                        .executes(c -> reset(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> reset(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("level").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, Profile.MAX_LEVEL)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Profiles.get(p).setLevel(IntegerArgumentType.getInteger(c, "level"));
                            Profiles.applyAttributes(p);
                            return done(c, p, "realmlord.cmd.level", Profiles.get(p).getLevel());
                        })))
                .then(Commands.literal("worklevel").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 10)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Profiles.get(p).setWorkLevel(IntegerArgumentType.getInteger(c, "level"));
                            Profiles.applyAttributes(p);
                            return done(c, p, "realmlord.cmd.worklevel", Profiles.get(p).getWorkLevel());
                        })))
                .then(Commands.literal("points").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("points", IntegerArgumentType.integer(0, 10000)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Profiles.get(p).setPoints(IntegerArgumentType.getInteger(c, "points"));
                            return done(c, p, "realmlord.cmd.points", Profiles.get(p).getPoints());
                        })))
                .then(Commands.literal("merit").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("add").then(Commands.argument("amount", LongArgumentType.longArg(-10_000_000, 10_000_000)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Profiles.grantMerit(p, LongArgumentType.getLong(c, "amount"), Component.translatable("realmlord.reason.admin"));
                            return done(c, p, "realmlord.cmd.merit", Profiles.get(p).getMerit());
                        })))
                        .then(Commands.literal("set").then(Commands.argument("amount", LongArgumentType.longArg(0, 10_000_000)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Profile prof = Profiles.get(p);
                            prof.setMerit(0);
                            Profiles.grantMerit(p, LongArgumentType.getLong(c, "amount"), null);
                            Profiles.refreshName(p);
                            return done(c, p, "realmlord.cmd.merit", prof.getMerit());
                        }))))
                .then(Commands.literal("token").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("grade", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(TokenGrade.values()).map(g -> g.id), b))
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    String g = StringArgumentType.getString(c, "grade").toLowerCase(Locale.ROOT);
                                    TokenGrade grade = Arrays.stream(TokenGrade.values()).filter(t -> t.id.equals(g)).findFirst().orElse(TokenGrade.BRONZE);
                                    p.getInventory().add(new ItemStack(ModItems.token(grade)));
                                    return 1;
                                })))
                .then(Commands.literal("quest").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("start").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            QuestLogic.start(p);
                            return 1;
                        }))
                        .then(Commands.literal("supplies").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            VillageQuest q = Profiles.get(p).quest();
                            if (q.stage != VillageQuest.Stage.SUPPLIES) return 0;
                            q.logs = VillageQuest.LOGS_NEEDED;
                            q.stone = VillageQuest.STONE_NEEDED;
                            q.food = VillageQuest.FOOD_NEEDED;
                            QuestLogic.beginBandits(p);
                            return 1;
                        }))
                        .then(Commands.literal("clear").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            QuestLogic.clear(p);
                            return 1;
                        }))))
        ;
    }

    private static int done(CommandContext<CommandSourceStack> c, ServerPlayer p, String key, Object value) {
        ModNetwork.sync(p);
        c.getSource().sendSuccess(() -> Component.translatable(key, value), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        Profile prof = Profiles.get(p);
        TerritoryData data = TerritoryData.get(p.server);
        if (prof.getTerritoryId() >= 0) data.remove(prof.getTerritoryId());
        prof.reset();
        Profiles.applyAttributes(p);
        Profiles.refreshName(p);
        ModNetwork.sync(p);
        ModNetwork.send(p, new OpenScreenPacket(OpenScreenPacket.ROLE));
        c.getSource().sendSuccess(() -> Component.translatable("realmlord.cmd.reset", p.getDisplayName()), true);
        return 1;
    }

    private static int trust(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c, ServerPlayer other, boolean add)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        com.realmlord.territory.TerritoryData data = com.realmlord.territory.TerritoryData.get(p.server);
        com.realmlord.territory.Territory t = data.ownedBy(p.getUUID());
        if (t == null) {
            c.getSource().sendFailure(Component.translatable("realmlord.protect.no_territory"));
            return 0;
        }
        if (add) t.trusted.add(other.getUUID());
        else t.trusted.remove(other.getUUID());
        data.touch();
        c.getSource().sendSuccess(() -> Component.translatable(add ? "realmlord.protect.trusted" : "realmlord.protect.untrusted", other.getDisplayName(), t.name), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> c, ServerPlayer p) throws CommandSyntaxException {
        Profile prof = Profiles.get(p);
        CommandSourceStack s = c.getSource();
        if (!prof.hasChosen()) {
            s.sendSuccess(() -> Component.translatable("realmlord.cmd.info.none", p.getDisplayName()), false);
            return 1;
        }
        Component occ = Component.translatable(prof.getCombatClass() != null ? prof.getCombatClass().key() : prof.getWork().key());
        s.sendSuccess(() -> Component.translatable("realmlord.cmd.info.head", p.getDisplayName(),
                Component.translatable(prof.getMode().key), occ, prof.getLevel(), Component.translatable(prof.getTitle().key()), prof.getMerit()), false);
        s.sendSuccess(() -> Component.translatable("realmlord.cmd.info.stats", prof.stat(Stat.COMMAND), prof.stat(Stat.FORCE),
                prof.stat(Stat.INTELLIGENCE), prof.stat(Stat.POLITICS), prof.getPoints()), false);
        s.sendSuccess(() -> Component.translatable("realmlord.cmd.info.innate", prof.innate(com.realmlord.profile.Innate.BODY),
                prof.innate(com.realmlord.profile.Innate.COMPREHENSION), prof.innate(com.realmlord.profile.Innate.LUCK),
                prof.innate(com.realmlord.profile.Innate.CHARM), prof.getReputation()), false);
        Territory t = prof.getTerritoryId() >= 0 ? TerritoryData.get(p.server).byId(prof.getTerritoryId()) : null;
        if (t != null) {
            s.sendSuccess(() -> Component.translatable("realmlord.cmd.info.territory", t.name, Component.translatable(t.grade.key()),
                    Component.translatable(t.level.kindKey()), t.level.grade, t.population, t.center.getX(), t.center.getZ()), false);
        }
        return 1;
    }

    private static int territories(CommandContext<CommandSourceStack> c) {
        TerritoryData data = TerritoryData.get(c.getSource().getServer());
        if (data.all().isEmpty()) {
            c.getSource().sendSuccess(() -> Component.translatable("realmlord.cmd.territories.none"), false);
            return 1;
        }
        for (Territory t : data.all()) {
            c.getSource().sendSuccess(() -> Component.translatable("realmlord.cmd.territories.line", t.foundedOrder, t.name, t.ownerName,
                    Component.translatable(t.grade.key()), Component.translatable(t.level.kindKey()), t.level.grade, t.population), false);
        }
        return 1;
    }
}
