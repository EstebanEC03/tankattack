package com.tankattack.model;

import java.util.Objects;

/**
 * Clase abstracta base para tanques. Maneja direccion, vidas,
 * velocidad y cooldown de disparo.
 */
public abstract class Tank extends GameObject {

    public static final int DEFAULT_PLAYER_LIVES = 3;

    protected Direction direction;
    protected int lives;
    protected int speed;
    protected long lastShotNanos;

    public Tank(String id, Position position, Direction direction,
                int lives, int speed) {
        super(id, position, true);
        this.direction = Objects.requireNonNull(direction, "direction");
        this.lives = lives;
        this.speed = Math.max(1, speed);
    }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }
    public int getLives() { return lives; }
    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = Math.max(1, speed); }
    public long getLastShotNanos() { return lastShotNanos; }

    /**
     * Reduce una vida. Devuelve true si el tanque sigue vivo.
     */
    public boolean takeDamage() {
        if (lives > 0) lives--;
        if (lives <= 0) {
            this.active = false;
            return false;
        }
        return true;
    }

    public boolean isAlive() {
        return active && lives > 0;
    }

    /**
     * Cooldown entre disparos en nanosegundos. Por defecto
     * 1.2s; las subclases pueden ajustar.
     */
    public long shotCooldownNanos() {
        return 1_200_000_000L;
    }

    public boolean canShoot(long nowNanos) {
        return isAlive() && (nowNanos - lastShotNanos) >= shotCooldownNanos();
    }

    public void markShot(long nowNanos) {
        this.lastShotNanos = nowNanos;
    }
}
