package com.tankattack.level;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tankattack.LevelFixtures;
import com.tankattack.model.Direction;
import com.tankattack.model.EnemyType;
import com.tankattack.model.Level;
import com.tankattack.model.Objective;
import com.tankattack.model.ObjectiveType;
import com.tankattack.model.Position;
import com.tankattack.model.Wall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Carga niveles del juego. Soporta dos fuentes:
 *
 *   1. Niveles predefinidos en codigo (coinciden con los
 *      definidos en Prolog).
 *   2. Niveles almacenados en formato JSON, utiles para
 *      el editor de pantallas del frontend.
 *
 * El JSON esperado tiene la siguiente estructura:
 * <pre>
 * {
 *   "id": "custom1",
 *   "width": 20,
 *   "height": 15,
 *   "tileSize": 32,
 *   "playerStart": { "x": 1, "y": 1, "direction": "RIGHT" },
 *   "playerLives": 3,
 *   "walls": [ { "x": 0, "y": 0 }, ... ],
 *   "objectives": [
 *     { "id": "o1", "type": "BASE", "x": 15, "y": 3 }, ...
 *   ],
 *   "enemies": [
 *     { "id": "e1", "type": "RAPIDO", "x": 16, "y": 3, "direction": "LEFT",
 *       "speed": 4, "defends": "o1" }, ...
 *   ]
 * }
 * </pre>
 */
public final class LevelLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public Level predefined(String id) {
        return LevelFixtures.byId(id);
    }

    public List<Level> predefinedAll() {
        return LevelFixtures.all();
    }

    public Level fromJson(String json) throws IOException {
        JsonNode root = mapper.readTree(json);
        return parseLevel(root);
    }

    public Level fromJson(JsonNode root) {
        return parseLevel(root);
    }

    public String toJson(Level level) throws IOException {
        return mapper.writeValueAsString(levelToNode(level));
    }

    public JsonNode levelToNode(Level level) {
        var node = mapper.createObjectNode();
        node.put("id", level.getId());
        node.put("width", level.getWidth());
        node.put("height", level.getHeight());
        node.put("tileSize", level.getTileSize());
        node.put("playerLives", level.getPlayerLives());

        var player = mapper.createObjectNode();
        player.put("x", level.getPlayerStart().x());
        player.put("y", level.getPlayerStart().y());
        player.put("direction", level.getPlayerStartDirection().name());
        node.set("playerStart", player);

        var walls = mapper.createArrayNode();
        for (Wall w : level.getWalls()) {
            var wn = mapper.createObjectNode();
            wn.put("x", w.getPosition().x());
            wn.put("y", w.getPosition().y());
            walls.add(wn);
        }
        node.set("walls", walls);

        var objectives = mapper.createArrayNode();
        for (Objective o : level.getObjectives()) {
            var on = mapper.createObjectNode();
            on.put("id", o.getId());
            on.put("type", o.getType().name());
            on.put("x", o.getPosition().x());
            on.put("y", o.getPosition().y());
            objectives.add(on);
        }
        node.set("objectives", objectives);

        var enemies = mapper.createArrayNode();
        for (var e : level.getEnemies()) {
            var en = mapper.createObjectNode();
            en.put("id", e.id());
            en.put("type", e.type().name());
            en.put("x", e.position().x());
            en.put("y", e.position().y());
            en.put("direction", e.direction().name());
            en.put("speed", e.speed());
            if (e.defendedObjectiveId() != null) {
                en.put("defends", e.defendedObjectiveId());
            }
            enemies.add(en);
        }
        node.set("enemies", enemies);

        return node;
    }

    private Level parseLevel(JsonNode root) {
        String id = textOrDefault(root, "id", "custom");
        int width = intOrDefault(root, "width", 20);
        int height = intOrDefault(root, "height", 15);
        int tileSize = intOrDefault(root, "tileSize", 32);
        int lives = intOrDefault(root, "playerLives", 3);

        JsonNode playerNode = root.path("playerStart");
        Position playerPos = new Position(
                intOrDefault(playerNode, "x", 1),
                intOrDefault(playerNode, "y", 1));
        Direction playerDir = Direction.fromJsonName(
                textOrDefault(playerNode, "direction", "RIGHT"))
                .orElse(Direction.RIGHT);

        List<Wall> walls = new ArrayList<>();
        for (JsonNode w : iterable(root.path("walls"))) {
            walls.add(new Wall(intOr(w, "x"), intOr(w, "y")));
        }

        List<Objective> objectives = new ArrayList<>();
        for (JsonNode o : iterable(root.path("objectives"))) {
            String oid = textOrDefault(o, "id", "o" + objectives.size());
            ObjectiveType type = ObjectiveType.fromJsonName(
                    textOrDefault(o, "type", "BASE")).orElse(ObjectiveType.BASE);
            objectives.add(new Objective(oid, type,
                    new Position(intOr(o, "x"), intOr(o, "y"))));
        }

        List<Level.EnemySpawn> enemies = new ArrayList<>();
        for (JsonNode e : iterable(root.path("enemies"))) {
            String eid = textOrDefault(e, "id", "e" + enemies.size());
            EnemyType type = EnemyType.fromJsonName(
                    textOrDefault(e, "type", "RAPIDO")).orElse(EnemyType.RAPIDO);
            Direction dir = Direction.fromJsonName(
                    textOrDefault(e, "direction", "LEFT")).orElse(Direction.LEFT);
            int speed = intOrDefault(e, "speed", 3);
            String defends = e.has("defends") && !e.path("defends").isNull()
                    ? e.path("defends").asText() : null;
            enemies.add(new Level.EnemySpawn(eid, type,
                    new Position(intOr(e, "x"), intOr(e, "y")),
                    dir, speed, defends));
        }

        return new Level(id, width, height, tileSize, walls, objectives, enemies,
                playerPos, playerDir, lives);
    }

    private static Iterable<JsonNode> iterable(JsonNode array) {
        return () -> {
            Iterator<JsonNode> it = array.elements();
            return it;
        };
    }

    private static int intOr(JsonNode n, String field) {
        return n.path(field).asInt();
    }

    private static int intOrDefault(JsonNode n, String field, int def) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() ? def : v.asInt();
    }

    private static String textOrDefault(JsonNode n, String field, String def) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() ? def : v.asText();
    }
}
