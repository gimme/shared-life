package dev.gimme.sharedlife.neoforge.gametest;

import java.util.List;
import java.util.function.Consumer;

import dev.gimme.sharedlife.domain.util.Constants;
import dev.gimme.sharedlife.gametest.SharedLifeGameTests;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge test wiring. Registers each shared test as a {@code TEST_FUNCTION} plus a matching
 * {@link FunctionGameTestInstance}. Auto-discovered via {@link EventBusSubscriber} so {@code main}
 * never references this dev-only source set. To add a test, add one entry to {@link #TESTS}.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class NeoForgeGameTests {

    private record Test(String name, int maxTicks, boolean isolated, boolean required, Consumer<GameTestHelper> body) {
        // Required by default; pass required=false for known-red TDD specs that shouldn't fail the suite.
        // Not isolated by default; an isolated test gets its own environment (= its own sequential batch).
        Test(String name, int maxTicks, Consumer<GameTestHelper> body) {
            this(name, maxTicks, false, true, body);
        }

        Test(String name, int maxTicks, boolean isolated, Consumer<GameTestHelper> body) {
            this(name, maxTicks, isolated, true, body);
        }
    }

    // Test names become resource-location paths, so they must be snake_case ([a-z0-9/._-] only).
    private static final List<Test> TESTS = List.of(
            new Test("damage_syncs_across_real_players", 100, SharedLifeGameTests::damageSyncsAcrossRealPlayers),
            new Test("armor_reduces_shared_damage", 100, SharedLifeGameTests::armorReducesSharedDamage),
            new Test("healing_syncs_across_real_players", 100, SharedLifeGameTests::healingSyncsAcrossRealPlayers),
            new Test("death_cascades_to_all_players", 100, SharedLifeGameTests::deathCascadesToAllPlayers),
            new Test("totem_revives_shared_life", 100, SharedLifeGameTests::totemRevivesSharedLife),
            new Test("hunger_syncs_across_real_players", 100, SharedLifeGameTests::hungerSyncsAcrossRealPlayers),
            new Test("starvation_hurts_all_players", 100, SharedLifeGameTests::starvationHurtsAllPlayers),
            new Test("combined_regen_heals_when_all_fed", 100, SharedLifeGameTests::combinedRegenHealsWhenAllFed),
            new Test("combined_regen_blocked_while_anyone_hungry", 100, SharedLifeGameTests::combinedRegenBlockedWhileAnyoneHungry),
            new Test("combined_regen_fast_only_when_all_saturated", 100, SharedLifeGameTests::combinedRegenFastOnlyWhenAllSaturated),
            new Test("individual_regen_suppressed_when_combined", 100, SharedLifeGameTests::individualRegenSuppressedWhenCombined),
            new Test("experience_syncs_across_real_players", 100, SharedLifeGameTests::experienceSyncsAcrossRealPlayers),
            // The one real-tick test: isolated so it gets its own environment (= own sequential batch) and
            // never shares a batch with a test that re-seeds the global pool while its players sit in the
            // shared list across a tick boundary.
            new Test("server_tick_hook_is_wired", 100, true, SharedLifeGameTests::serverTickHookIsWired),
            new Test("health_not_shared_when_disabled", 100, SharedLifeGameTests::healthNotSharedWhenDisabled),
            new Test("hunger_not_shared_when_disabled", 100, SharedLifeGameTests::hungerNotSharedWhenDisabled),
            new Test("experience_not_shared_when_disabled", 100, SharedLifeGameTests::experienceNotSharedWhenDisabled),
            new Test("ethereal_players_excluded", 100, SharedLifeGameTests::etherealPlayersExcluded),
            new Test("death_reseeds_from_next_joiner", 100, SharedLifeGameTests::deathReseedsFromNextJoiner));

    private NeoForgeGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry ->
                TESTS.forEach(test -> registry.register(id(test.name()), test.body())));
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        // Hand-driven tests assert synchronously within a single tick, so they share one environment (one
        // batch) and may run concurrently. An isolated test runs on real ticks, with its players living in
        // the shared player list across a tick boundary, so it gets its own environment — the framework runs
        // each environment's batch strictly sequentially — to keep it away from any test re-seeding the
        // global pool mid-flight.
        Holder<TestEnvironmentDefinition<?>> shared = event.registerEnvironment(id("default"));
        TESTS.forEach(test -> {
            Holder<TestEnvironmentDefinition<?>> environment =
                    test.isolated() ? event.registerEnvironment(id("env/" + test.name())) : shared;
            ResourceKey<Consumer<GameTestHelper>> function =
                    ResourceKey.create(Registries.TEST_FUNCTION, id(test.name()));
            TestData<Holder<TestEnvironmentDefinition<?>>> data =
                    new TestData<>(environment, id("empty"), test.maxTicks(), 0, test.required());
            event.registerTest(id(test.name()), new FunctionGameTestInstance(function, data));
        });
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
