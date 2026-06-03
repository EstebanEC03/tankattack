package com.tankattack.collision;

import com.tankattack.model.Bullet;
import com.tankattack.model.EnemyTank;
import com.tankattack.model.GameState;
import com.tankattack.model.Objective;
import com.tankattack.model.PlayerTank;
import com.tankattack.model.Position;
import com.tankattack.model.Wall;

import java.util.List;
import java.util.Optional;

/**
 * Encargado de validar posiciones y colisiones. Mantiene el
 * principio de que las balas y los tanques respetan los
 * muros y no se atraviesan entre si (los tanques enemigos
 * pueden ocupar la misma celda brevemente durante la
 * actualizacion, pero se resuelve antes del siguiente
 * tick).
 */
public final class CollisionManager {

    /**
     * Determina si una posicion esta libre para que un
     * tanque se mueva a ella. Considera muros, objetivos
     * activos y a otros tanques vivos.
     */
    public boolean isWalkable(GameState state, Position pos, Object mover) {
        if (!isInBounds(state, pos)) return false;
        for (Wall w : state.mutableWalls()) {
            if (w.getPosition().equals(pos)) return false;
        }
        for (Objective o : state.mutableObjectives()) {
            if (o.isActive() && o.getPosition().equals(pos)) return false;
        }
        if (mover instanceof PlayerTank) {
            for (EnemyTank e : state.mutableEnemies()) {
                if (e.isAlive() && e.getPosition().equals(pos)) return false;
            }
        } else if (mover instanceof EnemyTank moverEnemy) {
            for (EnemyTank e : state.mutableEnemies()) {
                if (e.isAlive() && !e.getId().equals(moverEnemy.getId())
                        && e.getPosition().equals(pos)) {
                    return false;
                }
            }
            if (state.getPlayer().isAlive()
                    && state.getPlayer().getPosition().equals(pos)) {
                return false;
            }
        }
        return true;
    }

    public boolean isInBounds(GameState state, Position pos) {
        return pos.x() >= 0 && pos.y() >= 0
                && pos.x() < state.getCurrentLevel().getWidth()
                && pos.y() < state.getCurrentLevel().getHeight();
    }

    /**
     * Devuelve el muro en la posicion indicada, si existe.
     */
    public Optional<Wall> wallAt(GameState state, Position pos) {
        for (Wall w : state.mutableWalls()) {
            if (w.getPosition().equals(pos)) return Optional.of(w);
        }
        return Optional.empty();
    }

    /**
     * Devuelve el tanque enemigo en la posicion, si existe.
     */
    public Optional<EnemyTank> enemyAt(GameState state, Position pos) {
        for (EnemyTank e : state.mutableEnemies()) {
            if (e.isAlive() && e.getPosition().equals(pos)) return Optional.of(e);
        }
        return Optional.empty();
    }

    public Optional<Objective> objectiveAt(GameState state, Position pos) {
        for (Objective o : state.mutableObjectives()) {
            if (o.isActive() && o.getPosition().equals(pos)) return Optional.of(o);
        }
        return Optional.empty();
    }

    /**
     * Aplica el movimiento del jugador si la celda destino
     * es valida. Devuelve true si se movio.
     */
    public boolean tryMovePlayer(GameState state, com.tankattack.model.Direction dir) {
        PlayerTank p = state.getPlayer();
        if (!p.isAlive()) return false;
        Position next = nextPos(p.getPosition(), dir);
        if (!isWalkable(state, next, p)) return false;
        p.setPosition(next);
        p.setDirection(dir);
        return true;
    }

    /**
     * Mueve un tanque enemigo a la posicion indicada solo
     * si la celda es transitable. Devuelve true si se
     * concreto el movimiento.
     */
    public boolean tryMoveEnemy(GameState state, EnemyTank e, Position next) {
        if (!e.isAlive()) return false;
        if (!isWalkable(state, next, e)) return false;
        e.setPosition(next);
        return true;
    }

    /**
     * Procesa una lista de balas: avanza cada una y resuelve
     * colisiones con muros, tanques y objetivos. Las balas
     * que impactan se desactivan.
     */
    public void updateBullets(GameState state) {
        List<Bullet> active = List.copyOf(state.getBullets());
        for (Bullet b : active) {
            if (!b.isActive()) continue;
            b.advanceOneTile();
            if (!isInBounds(state, b.getPosition())) {
                b.setActive(false);
                continue;
            }
            if (wallAt(state, b.getPosition()).isPresent()) {
                b.setActive(false);
                continue;
            }
            if (b.isOwnerPlayer()) {
                var enemyHit = enemyAt(state, b.getPosition());
                if (enemyHit.isPresent()) {
                    enemyHit.get().takeDamage();
                    b.setActive(false);
                    continue;
                }
            } else {
                if (state.getPlayer().isAlive()
                        && state.getPlayer().getPosition().equals(b.getPosition())) {
                    state.getPlayer().takeDamage();
                    b.setActive(false);
                    continue;
                }
            }
            if (objectiveAt(state, b.getPosition()).isPresent()) {
                Objective o = objectiveAt(state, b.getPosition()).get();
                o.destroy();
                b.setActive(false);
            }
        }
        state.removeInactiveBullets();
    }

    public static Position nextPos(Position p, com.tankattack.model.Direction dir) {
        return switch (dir) {
            case UP    -> p.translate(0, -1);
            case DOWN  -> p.translate(0,  1);
            case LEFT  -> p.translate(-1, 0);
            case RIGHT -> p.translate( 1, 0);
        };
    }
}
