package dev.gimme.sharedlife.gametest;

import com.mojang.authlib.GameProfile;
import dev.gimme.sharedlife.Main;
import dev.gimme.sharedlife.domain.util.ExtractingValueOutput;
import dev.gimme.sharedlife.domain.util.FakePlayer;
import dev.gimme.sharedlife.infrastructure.ConfigTestSupport;
import dev.gimme.sharedlife.infrastructure.ConfigTestSupport.Scope;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Loader-agnostic, end-to-end game test bodies, wired into {@code FabricGameTests} and
 * {@code NeoForgeGameTests}. Every test drives the production {@link Main#INSTANCE} singleton through
 * real gameplay ({@link ServerPlayer#hurtServer}, loader hooks, real {@code FoodData} ticks), so the
 * pool and config are global state: tests reset/seed the pool and pin the config toggles they rely on
 * with {@link ConfigTestSupport#override}.
 *
 * <p>Cross-player tests use {@link #spawnRealPlayer} — registered in the player list the per-tick sync
 * iterates — and after {@link #resetPool} the spawns themselves seed the pool through the mod's real
 * join-sync; join-time tests only need detached {@link FakePlayer}s. Ticks are driven by hand so each
 * test runs atomically within one server tick, except {@link #serverTickHookIsWired}, which awaits a
 * real tick in its own sequential batch.
 */
public final class SharedLifeGameTests {

    private SharedLifeGameTests() {
    }

    // ---- health: damage, healing, death cascade ----

    /** Real damage on one player crosses the live shared pool to another on the next tick. */
    public static void damageSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 6f);

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 14f);
                assertApproxHealth(helper, b, 14f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * The pool drops by the armor-reduced damage, not the raw hit: vanilla applies armor before the
     * loader's damage hook, so full iron armor (15 points) reduces a 9.0 hit by 42% to 5.22.
     */
    public static void armorReducesSharedDamage(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                a.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                a.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                a.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                a.doTick(); // equipment attribute modifiers (the armor points) only apply on the entity's tick
                helper.assertTrue(a.getArmorValue() == 15, "full iron armor should give 15 armor points, but A had " + a.getArmorValue());

                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(b), 9f);

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 14.78f);
                assertApproxHealth(helper, b, 14.78f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /** Real healing on one player raises the shared pool and lifts another on the next tick. */
    public static void healingSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            a.setHealth(10);
            b.setHealth(10);
            try {
                seedPoolFrom(a);
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // sync B to the re-seeded pool

                a.heal(4f);

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 14f);
                assertApproxHealth(helper, b, 14f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /** One player's death ends the shared life, and the next tick kills everyone still alive. */
    public static void deathCascadesToAllPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000f);

                Main.INSTANCE.getServerHandler().onServerTick();

                helper.assertTrue(a.isDeadOrDying(),
                        "A should have died from the fatal blow, but had health " + a.getHealth());
                helper.assertTrue(b.isDeadOrDying(),
                        "B should have been killed by the death cascade, but had health " + b.getHealth());
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /** A totem of undying on the fatally hit player revives the shared pool to one health for everyone. */
    public static void totemRevivesSharedLife(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));

                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000f);

                helper.assertTrue(a.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                        "the totem should have been consumed");

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 1f);
                assertApproxHealth(helper, b, 1f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    // ---- hunger & starvation ----

    /** A change to one player's food crosses the shared pool to another on the next tick. */
    public static void hungerSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.getFoodData().setFoodLevel(14);
                a.getFoodData().setSaturation(0f);

                Main.INSTANCE.getServerHandler().onServerTick();

                helper.assertTrue(a.getFoodData().getFoodLevel() == 14,
                        "A should have kept food 14, but had " + a.getFoodData().getFoodLevel());
                helper.assertTrue(b.getFoodData().getFoodLevel() == 14,
                        "B should have synced to the shared food 14, but had " + b.getFoodData().getFoodLevel());
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * When the shared food empties, the shared heart starves and every player takes the damage — as
     * {@code shared_life} damage, which bypasses invulnerability, so both take it on the same tick.
     */
    public static void starvationHurtsAllPlayers(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Difficulty previousDifficulty = helper.getLevel().getDifficulty();
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            server.setDifficulty(Difficulty.HARD, true); // EASY/NORMAL won't starve a full-health heart

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.getFoodData().setFoodLevel(0);
                a.getFoodData().setSaturation(0f);
                seedPoolFrom(a);
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // sync B to the re-seeded pool

                // Vanilla starvation fires once every 80 ticks of empty food; tick until the heart starves.
                boolean starved = false;
                for (int i = 0; i < 90 && !starved; i++) {
                    Main.INSTANCE.getServerHandler().onServerTick();
                    starved = a.getHealth() < 20f;
                }

                helper.assertTrue(starved, "the shared heart should have starved within 90 ticks");
                assertApproxHealth(helper, a, 19f);
                assertApproxHealth(helper, b, 19f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        } finally {
            server.setDifficulty(previousDifficulty, true);
        }
        helper.succeed();
    }

    // ---- combined natural regeneration ----

    /**
     * With every player fed to vanilla's regeneration threshold, the group heals — one heal, not one
     * per fed player — and every player pays vanilla's exhaustion cost for it.
     */
    public static void combinedRegenHealsWhenAllFed(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.COMBINE_NATURAL_REGENERATION, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            a.setHealth(10);
            b.setHealth(10);
            try {
                seedPoolFrom(a);
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // sync B to the re-seeded pool

                setFood(a, 18, 0f); // fed enough for vanilla's slow regeneration, no saturation
                setFood(b, 18, 0f);

                for (int i = 0; i < 85; i++) {
                    Main.INSTANCE.getServerHandler().onServerTick();
                }

                assertApproxHealth(helper, a, 11f); // exactly one vanilla heal (tick 80), not one per player
                assertApproxHealth(helper, b, 11f);
                assertExhaustion(helper, a, 6f);    // both players paid vanilla's cost for that heal
                assertExhaustion(helper, b, 6f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /** One hungry player blocks the whole group's natural regeneration. */
    public static void combinedRegenBlockedWhileAnyoneHungry(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.COMBINE_NATURAL_REGENERATION, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            a.setHealth(10);
            b.setHealth(10);
            try {
                seedPoolFrom(a);
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // sync B to the re-seeded pool

                setFood(a, 20, 5f);
                setFood(b, 17, 0f); // below vanilla's regeneration threshold of 18

                for (int i = 0; i < 85; i++) {
                    Main.INSTANCE.getServerHandler().onServerTick();
                }

                assertApproxHealth(helper, a, 10f);
                assertApproxHealth(helper, b, 10f);
                assertExhaustion(helper, a, 0f);
                assertExhaustion(helper, b, 0f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * Vanilla's fast (saturated) regeneration applies only while <em>everyone</em> is at full food with
     * saturation, limited by the group's lowest saturation.
     */
    public static void combinedRegenFastOnlyWhenAllSaturated(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.COMBINE_NATURAL_REGENERATION, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            a.setHealth(10);
            b.setHealth(10);
            try {
                seedPoolFrom(a);
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // sync B to the re-seeded pool

                setFood(a, 20, 6f); // everyone at full food with saturation:
                setFood(b, 20, 8f); // vanilla's fast branch, limited by the lowest saturation (6)

                for (int i = 0; i < 12; i++) {
                    Main.INSTANCE.getServerHandler().onServerTick();
                }

                assertApproxHealth(helper, a, 11f); // one fast heal (tick 10) of min(6, 6) / 6 = 1.0
                assertApproxHealth(helper, b, 11f);

                setFood(b, 20, 0f); // B's saturation runs out: the group falls back to the slow branch

                for (int i = 0; i < 15; i++) {
                    Main.INSTANCE.getServerHandler().onServerTick();
                }

                assertApproxHealth(helper, a, 11f); // no fast heal without everyone saturated
                assertApproxHealth(helper, b, 11f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * A player's own vanilla regeneration is suppressed (via {@code MixinFoodData}) while combined
     * regeneration is active — and only then: with the option off, the same setup regenerates again.
     */
    public static void individualRegenSuppressedWhenCombined(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.COMBINE_NATURAL_REGENERATION, true)) {

            ServerPlayer player = spawnFake(helper, 10f);
            setFood(player, 20, 6f);

            for (int i = 0; i < 15; i++) {
                player.getFoodData().tick(player);
            }

            assertHealth(helper, player, 10f); // vanilla's fast regeneration never fired
            assertExhaustion(helper, player, 0f);
        }

        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.COMBINE_NATURAL_REGENERATION, false)) {

            ServerPlayer player = spawnFake(helper, 10f);
            setFood(player, 20, 6f);

            for (int i = 0; i < 15; i++) {
                player.getFoodData().tick(player);
            }

            helper.assertTrue(player.getHealth() > 10f,
                    "combining is off, so the player should have regenerated on their own, but had health "
                            + player.getHealth());
        }
        helper.succeed();
    }

    // ---- experience ----

    /**
     * With experience sharing on, gaining levels on one player raises everyone on the next tick.
     */
    public static void experienceSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, true)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.giveExperienceLevels(5);

                Main.INSTANCE.getServerHandler().onServerTick();

                helper.assertTrue(a.experienceLevel == 5,
                        "A should have kept 5 levels, but had " + a.experienceLevel);
                helper.assertTrue(b.experienceLevel == 5,
                        "B should have synced to the shared 5 levels, but had " + b.experienceLevel);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    // ---- loader wiring: the real per-tick hook ----

    /**
     * The one test that does <em>not</em> tick by hand: a real server tick must fire the loader's
     * registered per-tick hook, so this fails if that wiring is ever dropped while every hand-driven
     * test above still passes. It must run in its own test environment (see the loader wiring), being the
     * only test whose players stay in the player list across a tick boundary; the config {@link Scope}s
     * and players are released inside the deferred callback, since try-with-resources would restore them
     * before the awaited tick.
     */
    public static void serverTickHookIsWired(GameTestHelper helper) {
        Scope shareHealth = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
        Scope shareHunger = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
        Scope shareExperience = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false);

        resetPool(helper);
        ServerPlayer a = spawnRealPlayer(helper);
        ServerPlayer b = spawnRealPlayer(helper);

        a.invulnerableTime = 0;
        a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 6f); // pool 20 -> 14

        // Deliberately NO onServerTick(): the loader's registered END_SERVER_TICK hook must run the sync.
        succeedAfterTicks(helper, 2,
                () -> {
                    assertApproxHealth(helper, a, 14f);
                    assertApproxHealth(helper, b, 14f);
                },
                () -> removeRealPlayers(helper, a, b),
                shareHealth, shareHunger, shareExperience);
    }

    // ---- join-time gating: disabled toggles, ethereal players, re-seeding ----

    /** With health sharing off, a joining player keeps their own health. */
    public static void healthNotSharedWhenDisabled(GameTestHelper helper) {
        ServerPlayer seed = spawnFake(helper, 7f);
        seedPoolFrom(seed);

        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, false)) {
            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);
            assertHealth(helper, joiner, 20f);
        }
        helper.succeed();
    }

    /** With hunger sharing off, a joining player keeps their own food level. */
    public static void hungerNotSharedWhenDisabled(GameTestHelper helper) {
        ServerPlayer seed = spawnFake(helper, 20f);
        seed.getFoodData().setFoodLevel(7);
        seedPoolFrom(seed);

        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false)) {
            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);
            helper.assertTrue(joiner.getFoodData().getFoodLevel() == 20,
                    "hunger sharing is off, so the joining player should keep food level 20, but had "
                            + joiner.getFoodData().getFoodLevel());
        }
        helper.succeed();
    }

    /** Experience sharing ships disabled, so a joining player keeps their own levels. */
    public static void experienceNotSharedWhenDisabled(GameTestHelper helper) {
        ServerPlayer seed = spawnFake(helper, 20f);
        seed.giveExperienceLevels(5);
        seedPoolFrom(seed);

        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {
            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);
            helper.assertTrue(joiner.experienceLevel == 0,
                    "experience sharing is off, so the joining player should keep 0 levels, but had "
                            + joiner.experienceLevel);
        }
        helper.succeed();
    }

    /** Creative/spectator players are ethereal and never join or seed the shared pool. */
    public static void etherealPlayersExcluded(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            ServerPlayer first = spawnFake(helper, 7f);
            Main.INSTANCE.getPlayerHandler().onPlayerDeath(first); // ensure the pool starts dead

            ServerPlayer creative = spawnFake(helper, 20f);
            creative.setGameMode(GameType.CREATIVE);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(creative); // ignored: ethereal

            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(first); // pool still dead -> seeds at 7

            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);

            assertHealth(helper, joiner, 7f); // synced to the survival seed, not the ignored creative one
        }
        helper.succeed();
    }

    /** After a death, the next player to join re-seeds the pool from their own state. */
    public static void deathReseedsFromNextJoiner(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            ServerPlayer seed = spawnFake(helper, 20f);
            seedPoolFrom(seed);

            Main.INSTANCE.getPlayerHandler().onPlayerDeath(seed);

            ServerPlayer reseeder = spawnFake(helper, 6f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(reseeder);

            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);

            assertHealth(helper, joiner, 6f);
        }
        helper.succeed();
    }

    // ---- helpers ----

    /**
     * Lets {@code delayTicks} real server ticks elapse, then runs {@code assertion} once and passes the
     * test. The cleanup and config scopes are released even when the assertion throws, so a failing test
     * can't leak players or config overrides into the next one.
     */
    private static void succeedAfterTicks(GameTestHelper helper, int delayTicks, Runnable assertion,
                                          Runnable cleanup, Scope... scopes) {
        helper.runAfterDelay(delayTicks, () -> {
            try {
                assertion.run();
            } finally {
                cleanup.run();
                for (Scope scope : scopes) {
                    scope.close();
                }
            }
            helper.succeed();
        });
    }

    /**
     * Pins a deterministic starting state on the shared singleton: kills the pool, then re-seeds it from
     * {@code seed}'s current health/food/experience.
     */
    private static void seedPoolFrom(ServerPlayer seed) {
        killPool(seed);
        Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(seed); // pool dead -> re-seed from this player
    }

    /**
     * Forces the shared pool dead, with health sharing temporarily on so {@code killBy} applies
     * regardless of the test's toggles.
     */
    private static void killPool(ServerPlayer agent) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            Main.INSTANCE.getPlayerHandler().onPlayerDeath(agent);
        }
    }

    /**
     * Test-isolation step, run before any player joins: returns the server-lifetime singleton to a dead
     * pool so this test never inherits what an earlier test left behind — the first spawn then re-seeds
     * it fresh through the mod's real join logic.
     */
    private static void resetPool(GameTestHelper helper) {
        killPool(spawnFake(helper, 20f));
    }

    /**
     * A real, survival player registered in {@code server.getPlayerList()}, so the per-tick sync loop
     * sees it and death/tick processing runs for real. {@link GameTestHelper#makeMockServerPlayer(GameType)}
     * pins survival (never ethereal, survival abilities); registration mirrors the "in level" half of the
     * deprecated {@code makeMockServerPlayerInLevel()}.
     */
    private static ServerPlayer spawnRealPlayer(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList()
            .placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        // Mock players aren't wired to a connection, so the server never runs their per-tick update.
        helper.onEachTick(player::doTick);

        // A freshly placed player is invulnerable until its client reports the level loaded.
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        player.invulnerableTime = 0;
        return player;
    }

    /** Removes players registered via {@link #spawnRealPlayer} from the live player list. */
    private static void removeRealPlayers(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer player : players) {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    /** A detached survival {@link FakePlayer}, not registered in the player list — enough for join-time tests. */
    private static ServerPlayer spawnFake(GameTestHelper helper, float health) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "TestPlayer"));
        // The game-test server defaults new players to creative, which Players treats as ethereal
        // and excludes from the shared life. Force survival so the shared-life gating applies.
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(health);
        return player;
    }

    private static void setFood(ServerPlayer player, int foodLevel, float saturation) {
        player.getFoodData().setFoodLevel(foodLevel);
        player.getFoodData().setSaturation(saturation);
    }

    /** Reads the player's exhaustion through {@link ExtractingValueOutput}, since vanilla exposes no getter. */
    private static void assertExhaustion(GameTestHelper helper, ServerPlayer player, float expected) {
        var output = new ExtractingValueOutput();
        player.getFoodData().addAdditionalSaveData(output);
        helper.assertTrue(Math.abs(output.getExhaustion() - expected) < 0.01f,
                "expected exhaustion " + expected + " but player had " + output.getExhaustion());
    }

    private static void assertApproxHealth(GameTestHelper helper, ServerPlayer player, float expected) {
        helper.assertTrue(Math.abs(player.getHealth() - expected) < 0.01f,
                "expected shared health ~" + expected + " but player had " + player.getHealth());
    }

    private static void assertHealth(GameTestHelper helper, ServerPlayer player, float expected) {
        helper.assertTrue(player.getHealth() == expected,
                "expected shared health " + expected + " but player had " + player.getHealth());
    }
}
