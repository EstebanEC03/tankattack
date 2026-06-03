package com.tankattack.model;

import java.util.Objects;

/**
 * Objetivo primario que el jugador debe destruir. Coincide
 * con el predicado Prolog {@code objetivo/5}.
 */
public final class Objective extends GameObject {

    private final ObjectiveType type;
    private final String skin;

    public Objective(String id, ObjectiveType type, Position position) {
        this(id, type, position, type.defaultSkin());
    }

    public Objective(String id, ObjectiveType type, Position position, String skin) {
        super(id, position, true);
        this.type = Objects.requireNonNull(type, "type");
        this.skin = skin == null ? type.defaultSkin() : skin;
    }

    public ObjectiveType getType() { return type; }
    public String getSkin() { return skin; }

    public void destroy() {
        this.active = false;
    }
}
