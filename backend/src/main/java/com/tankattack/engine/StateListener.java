package com.tankattack.engine;

import com.tankattack.model.GameState;

/**
 * Callback que el motor invoca cada vez que el estado
 * cambia de forma significativa (movimiento, decision de
 * enemigo, fin de nivel, etc.). El servidor WebSocket lo
 * usa para enviar snapshots al frontend.
 */
@FunctionalInterface
public interface StateListener {
    void onStateChange(GameState state);
}
