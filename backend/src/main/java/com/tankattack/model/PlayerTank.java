package com.tankattack.model;

import java.util.Objects;

/**
 * Tanque manejado por el usuario. Se mueve segun las
 * entradas que el frontend envia al backend.
 */
public final class PlayerTank extends Tank {

    public static final String DEFAULT_SKIN = "player_tank_vaporwave";

    private final String skin;

    public PlayerTank(String id, Position position, Direction direction, int lives) {
        this(id, position, direction, lives, 4, DEFAULT_SKIN);
    }

    public PlayerTank(String id, Position position, Direction direction,
                      int lives, int speed, String skin) {
        super(id, position, direction, lives, speed);
        this.skin = Objects.requireNonNull(skin, "skin");
    }

    public String getSkin() { return skin; }
}
