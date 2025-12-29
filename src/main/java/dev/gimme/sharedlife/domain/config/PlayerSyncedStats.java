package dev.gimme.sharedlife.domain.config;

/**
 * Player stats that are synced.
 */
public record PlayerSyncedStats(
        boolean health,
        boolean absorption,
        boolean food,
        boolean saturation,
        boolean thirst,
        boolean quenched,
        boolean experience
) {
}
