package com.tankattack.api;

import com.tankattack.model.Bullet;
import com.tankattack.model.EnemyTank;
import com.tankattack.model.GameState;
import com.tankattack.model.Level;
import com.tankattack.model.Objective;
import com.tankattack.model.PlayerTank;
import com.tankattack.model.Position;
import com.tankattack.model.Wall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte el {@link GameState} interno a un mapa
 * compatible con el formato JSON esperado por el frontend.
 * El formato coincide con el que define el
 * {@code plan_frontend_vite_typescript_canvas.md}.
 */
public final class GameStateDto {

    private GameStateDto() {}

    public static Map<String, Object> toMap(GameState state) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (state == null) return root;
        Level level = state.getCurrentLevel();

        root.put("level", state.getCurrentLevelNumber());
        root.put("board", board(level));
        root.put("player", player(state.getPlayer()));
        root.put("enemies", enemies(state));
        root.put("objectives", objectives(state));
        root.put("walls", walls(state));
        root.put("bullets", bullets(state));
        root.put("status", status(state));
        return root;
    }

    private static Map<String, Object> board(Level level) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("width", level.getWidth());
        b.put("height", level.getHeight());
        b.put("tileSize", level.getTileSize());
        return b;
    }

    private static Map<String, Object> player(PlayerTank p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("x", p.getPosition().x());
        m.put("y", p.getPosition().y());
        m.put("direction", p.getDirection().name());
        m.put("lives", p.getLives());
        m.put("status", p.isAlive() ? "ALIVE" : "DESTROYED");
        m.put("skin", p.getSkin());
        return m;
    }

    private static List<Map<String, Object>> enemies(GameState state) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (EnemyTank e : state.mutableEnemies()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("type", e.getType().name());
            m.put("x", e.getPosition().x());
            m.put("y", e.getPosition().y());
            m.put("direction", e.getDirection().name());
            m.put("status", e.isAlive() ? "ALIVE" : "DESTROYED");
            m.put("action", e.getCurrentAction() == null ? null
                    : e.getCurrentAction().name());
            m.put("role", e.getCurrentRole());
            m.put("skin", e.getSkin());
            m.put("defends", e.getDefendedObjectiveId());
            out.add(m);
        }
        return out;
    }

    private static List<Map<String, Object>> objectives(GameState state) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Objective o : state.mutableObjectives()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("type", o.getType().name());
            m.put("x", o.getPosition().x());
            m.put("y", o.getPosition().y());
            m.put("status", o.isActive() ? "ACTIVE" : "DESTROYED");
            m.put("skin", o.getSkin());
            out.add(m);
        }
        return out;
    }

    private static List<Map<String, Object>> walls(GameState state) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Wall w : state.mutableWalls()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("x", w.getPosition().x());
            m.put("y", w.getPosition().y());
            m.put("skin", "vaporwave_wall");
            out.add(m);
        }
        return out;
    }

    private static List<Map<String, Object>> bullets(GameState state) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Bullet b : state.getBullets()) {
            if (!b.isActive()) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("ownerId", b.getOwnerId());
            m.put("ownerIsPlayer", b.isOwnerPlayer());
            m.put("x", b.getPosition().x());
            m.put("y", b.getPosition().y());
            m.put("direction", b.getDirection().name());
            m.put("active", true);
            m.put("skin", "vaporwave_bullet");
            out.add(m);
        }
        return out;
    }

    private static Map<String, Object> status(GameState state) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("running", state.isRunning());
        s.put("paused", state.isPaused());
        s.put("gameOver", state.isGameOver());
        s.put("levelCompleted", state.isLevelCompleted());
        s.put("tick", state.getTickCount());
        s.put("aliveEnemies", state.aliveEnemyCount());
        s.put("activeObjectives", state.activeObjectiveCount());
        return s;
    }

    public static Map<String, Object> positionOnly(Position p) {
        if (p == null) return null;
        return Map.of("x", p.x(), "y", p.y());
    }
}
