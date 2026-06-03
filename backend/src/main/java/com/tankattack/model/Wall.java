package com.tankattack.model;

import java.util.Objects;

/**
 * Muro estatico. No se destruye y bloquea tanques y balas.
 */
public final class Wall extends GameObject {

    public Wall(int x, int y) {
        this(x + "_" + y, new Position(x, y));
    }

    public Wall(String id, Position position) {
        super(id, position, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wall w)) return false;
        return Objects.equals(id, w.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
