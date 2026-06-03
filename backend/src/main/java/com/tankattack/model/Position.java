package com.tankattack.model;

import java.util.Objects;

/**
 * Coordenada discreta en el tablero. {@code (x, y)} con
 * origen en la esquina superior izquierda.
 */
public final class Position {
    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() { return x; }
    public int y() { return y; }

    public Position translate(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    /**
     * Distancia Manhattan entre dos posiciones. Es la
     * heuristica que usa Prolog en {@code distancia_manhattan/5}.
     */
    public int manhattanTo(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position other)) return false;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
