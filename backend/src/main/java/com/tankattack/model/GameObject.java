package com.tankattack.model;

/**
 * Clase base abstracta para todos los objetos del juego.
 * Encapsula identidad, posicion y estado activo.
 */
public abstract class GameObject {
    protected final String id;
    protected Position position;
    protected boolean active;

    protected GameObject(String id, Position position, boolean active) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id no puede ser nulo o vacio");
        }
        this.id = id;
        this.position = position == null ? new Position(0, 0) : position;
        this.active = active;
    }

    public String getId() { return id; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
