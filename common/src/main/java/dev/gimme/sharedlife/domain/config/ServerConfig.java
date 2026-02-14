package dev.gimme.sharedlife.domain.config;

public abstract class ServerConfig {

    public abstract boolean shareHealth();
    public abstract boolean shareDeath();
    public abstract boolean shareHunger();
    public abstract boolean shareExperience();
    public abstract boolean announceDamage();
    public abstract boolean includeDamageSource();
}
