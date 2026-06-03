package com.tankattack.model;

import java.util.Map;
import java.util.Optional;

/**
 * Direcciones cardinales permitidas. Coinciden con los atomos
 * de Prolog usados en {@code bala/5} y en las predicados de
 * disparo en linea recta.
 */
public enum Direction {
    UP, DOWN, LEFT, RIGHT;

    private static final Map<Direction, String> TO_PROLOG = Map.of(
            UP, "arriba",
            DOWN, "abajo",
            LEFT, "izquierda",
            RIGHT, "derecha"
    );

    private static final Map<String, Direction> FROM_PROLOG = Map.of(
            "arriba", UP,
            "abajo", DOWN,
            "izquierda", LEFT,
            "derecha", RIGHT
    );

    public String toPrologAtom() {
        return TO_PROLOG.get(this);
    }

    public static Optional<Direction> fromPrologAtom(String atom) {
        if (atom == null) return Optional.empty();
        return Optional.ofNullable(FROM_PROLOG.get(atom));
    }

    public static Optional<Direction> fromJsonName(String name) {
        if (name == null) return Optional.empty();
        try {
            return Optional.of(Direction.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
