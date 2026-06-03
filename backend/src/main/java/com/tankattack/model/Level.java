package com.tankattack.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuracion completa de un nivel. Se carga desde el
 * {@link com.tankattack.level.LevelLoader} y se usa para
 * inicializar el {@link com.tankattack.engine.GameEngine}.
 */
public final class Level {

    private final String id;
    private final int width;
    private final int height;
    private final int tileSize;
    private final List<Wall> walls;
    private final List<Objective> objectives;
    private final List<EnemySpawn> enemies;
    private final Position playerStart;
    private final Direction playerStartDirection;
    private final int playerLives;

    public Level(String id, int width, int height, int tileSize,
                 List<Wall> walls, List<Objective> objectives,
                 List<EnemySpawn> enemies, Position playerStart,
                 Direction playerStartDirection, int playerLives) {
        this.id = Objects.requireNonNull(id, "id");
        this.width = width;
        this.height = height;
        this.tileSize = tileSize <= 0 ? 32 : tileSize;
        this.walls = walls == null ? List.of() : List.copyOf(walls);
        this.objectives = objectives == null ? List.of() : List.copyOf(objectives);
        this.enemies = enemies == null ? List.of() : List.copyOf(enemies);
        this.playerStart = Objects.requireNonNull(playerStart, "playerStart");
        this.playerStartDirection = playerStartDirection == null
                ? Direction.RIGHT : playerStartDirection;
        this.playerLives = playerLives <= 0 ? 3 : playerLives;
    }

    public String getId() { return id; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTileSize() { return tileSize; }
    public List<Wall> getWalls() { return walls; }
    public List<Objective> getObjectives() { return objectives; }
    public List<EnemySpawn> getEnemies() { return enemies; }
    public Position getPlayerStart() { return playerStart; }
    public Direction getPlayerStartDirection() { return playerStartDirection; }
    public int getPlayerLives() { return playerLives; }

    public List<Position> wallPositions() {
        List<Position> out = new ArrayList<>(walls.size());
        for (Wall w : walls) out.add(w.getPosition());
        return out;
    }

    public record EnemySpawn(String id, EnemyType type, Position position,
                             Direction direction, int speed,
                             String defendedObjectiveId) { }
}
