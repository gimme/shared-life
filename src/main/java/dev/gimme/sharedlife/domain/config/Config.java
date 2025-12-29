package dev.gimme.sharedlife.domain.config;

public interface Config {

    boolean syncHealth();
    boolean syncFood();
    boolean syncSaturation();
    boolean syncThirst();
    boolean syncQuenched();
}
