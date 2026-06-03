package com.tankattack.model;

import java.util.Map;
import java.util.Optional;

/**
 * Tipos de tanques enemigos. Coinciden con los atomos de Prolog
 * ({@code rapido}, {@code pesado}, {@code tactico}) usados en el
 * predicado {@code tanque_enemigo/5}.
 */
public enum EnemyType {
    RAPIDO, PESADO, TACTICO;

    private static final Map<EnemyType, String> TO_PROLOG = Map.of(
            RAPIDO, "rapido",
            PESADO, "pesado",
            TACTICO, "tactico"
    );

    private static final Map<String, EnemyType> FROM_PROLOG = Map.of(
            "rapido", RAPIDO,
            "pesado", PESADO,
            "tactico", TACTICO
    );

    public String toPrologAtom() {
        return TO_PROLOG.get(this);
    }

    public static Optional<EnemyType> fromPrologAtom(String atom) {
        if (atom == null) return Optional.empty();
        return Optional.ofNullable(FROM_PROLOG.get(atom));
    }

    public static Optional<EnemyType> fromJsonName(String name) {
        if (name == null) return Optional.empty();
        try {
            return Optional.of(EnemyType.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Skin vaporwave sugerida para el tipo de tanque. El frontend
     * la usa como pista visual; puede ignorarla.
     */
    public String defaultSkin() {
        return switch (this) {
            case RAPIDO  -> "vaporwave_fast_tank";
            case PESADO  -> "vaporwave_heavy_tank";
            case TACTICO -> "vaporwave_tactical_tank";
        };
    }
}
