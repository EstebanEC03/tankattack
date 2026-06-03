package com.tankattack.model;

import java.util.Objects;

/**
 * Bala que se mueve en una de las cuatro direcciones. Puede
 * pertenecer al jugador o a un enemigo. Coincide con el
 * predicado Prolog {@code bala/5}.
 */
public final class Bullet extends GameObject {

    public static final int DEFAULT_SPEED_TILES_PER_SEC = 8;

    private final String ownerId;
    private final boolean ownerIsPlayer;
    private final Direction direction;
    private final int speed;

    public Bullet(String id, String ownerId, boolean ownerIsPlayer,
                  Position position, Direction direction, int speed) {
        super(id, position, true);
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.ownerIsPlayer = ownerIsPlayer;
        this.direction = Objects.requireNonNull(direction, "direction");
        this.speed = Math.max(1, speed);
    }

    public String getOwnerId() { return ownerId; }
    public boolean isOwnerPlayer() { return ownerIsPlayer; }
    public Direction getDirection() { return direction; }
    public int getSpeed() { return speed; }

    /**
     * Avanza la bala una celda en su direccion. El caller
     * debe validar colisiones despues.
     */
    public void advanceOneTile() {
        int dx = 0, dy = 0;
        switch (direction) {
            case UP    -> dy = -1;
            case DOWN  -> dy = 1;
            case LEFT  -> dx = -1;
            case RIGHT -> dx = 1;
        }
        this.position = this.position.translate(dx, dy);
    }
}
