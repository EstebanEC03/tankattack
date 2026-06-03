package com.tankattack.engine;

import com.tankattack.model.Direction;

/**
 * Comando que el frontend envia al backend para controlar
 * al jugador.
 */
public record PlayerInput(InputType type, Direction direction) {

    public enum InputType { MOVE, SHOOT, PAUSE, RESTART }

    public static PlayerInput move(Direction d) {
        return new PlayerInput(InputType.MOVE, d);
    }

    public static PlayerInput shoot() {
        return new PlayerInput(InputType.SHOOT, null);
    }

    public static PlayerInput pause() {
        return new PlayerInput(InputType.PAUSE, null);
    }

    public static PlayerInput restart() {
        return new PlayerInput(InputType.RESTART, null);
    }
}
