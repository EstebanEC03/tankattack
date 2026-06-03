package com.tankattack.model;

import java.util.Map;
import java.util.Optional;

/**
 * Acciones que un tanque enemigo puede ejecutar. Coinciden con
 * los atomos devueltos por el predicado {@code decidir_accion/3}
 * de Prolog.
 */
public enum EnemyAction {
    DISPARAR,
    DEFENDER_OBJETIVO,
    RETROCEDER,
    PERSEGUIR_JUGADOR,
    EMBOSCAR,
    PATRULLAR,
    ESPERAR,
    COORDINAR;

    private static final Map<EnemyAction, String> TO_PROLOG = Map.ofEntries(
            Map.entry(DISPARAR, "disparar"),
            Map.entry(DEFENDER_OBJETIVO, "defender_objetivo"),
            Map.entry(RETROCEDER, "retroceder"),
            Map.entry(PERSEGUIR_JUGADOR, "perseguir_jugador"),
            Map.entry(EMBOSCAR, "emboscar"),
            Map.entry(PATRULLAR, "patrullar"),
            Map.entry(ESPERAR, "esperar"),
            Map.entry(COORDINAR, "coordinar_ataque")
    );

    private static final Map<String, EnemyAction> FROM_PROLOG = Map.ofEntries(
            Map.entry("disparar", DISPARAR),
            Map.entry("defender_objetivo", DEFENDER_OBJETIVO),
            Map.entry("retroceder", RETROCEDER),
            Map.entry("perseguir_jugador", PERSEGUIR_JUGADOR),
            Map.entry("emboscar", EMBOSCAR),
            Map.entry("patrullar", PATRULLAR),
            Map.entry("esperar", ESPERAR),
            Map.entry("coordinar_ataque", COORDINAR)
    );

    public String toPrologAtom() {
        return TO_PROLOG.get(this);
    }

    public static Optional<EnemyAction> fromPrologAtom(String atom) {
        if (atom == null) return Optional.empty();
        return Optional.ofNullable(FROM_PROLOG.get(atom));
    }
}
