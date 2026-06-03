package com.tankattack.model;

import java.util.Map;
import java.util.Optional;

/**
 * Tipos de objetivos primarios. Coinciden con los atomos de
 * Prolog usados en el predicado {@code objetivo/5}.
 */
public enum ObjectiveType {
    BASE, REFINERIA;

    private static final Map<ObjectiveType, String> TO_PROLOG = Map.of(
            BASE, "base",
            REFINERIA, "refineria"
    );

    private static final Map<String, ObjectiveType> FROM_PROLOG = Map.of(
            "base", BASE,
            "refineria", REFINERIA
    );

    public String toPrologAtom() {
        return TO_PROLOG.get(this);
    }

    public static Optional<ObjectiveType> fromPrologAtom(String atom) {
        if (atom == null) return Optional.empty();
        return Optional.ofNullable(FROM_PROLOG.get(atom));
    }

    public static Optional<ObjectiveType> fromJsonName(String name) {
        if (name == null) return Optional.empty();
        try {
            return Optional.of(ObjectiveType.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public String defaultSkin() {
        return switch (this) {
            case BASE      -> "vaporwave_base";
            case REFINERIA -> "vaporwave_refinery";
        };
    }
}
