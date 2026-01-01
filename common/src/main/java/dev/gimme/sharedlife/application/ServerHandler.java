package dev.gimme.sharedlife.application;

import dev.gimme.sharedlife.domain.SharedLife;

public class ServerHandler {

    private final SharedLife sharedLife;

    public ServerHandler(SharedLife sharedLife) {
        this.sharedLife = sharedLife;
    }

    public void onServerTick() {
        sharedLife.tick();
    }
}
