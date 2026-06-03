package com.tankattack.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Estado completo de la partida en un instante dado. Es
 * mutable porque el {@link com.tankattack.engine.GameEngine}
 * lo actualiza en cada tick, pero todas las mutaciones
 * ocurren dentro del hilo del motor.
 *
 * La conversion a JSON (para enviar al frontend) se hace en
 * {@link com.tankattack.api.GameStateDto}.
 */
public final class GameState {

    private final AtomicLong bulletIdSeq = new AtomicLong(1);
    private final List<Bullet> bullets = new CopyOnWriteArrayList<>();

    private Level currentLevel;
    private PlayerTank player;
    private final List<EnemyTank> enemies = new ArrayList<>();
    private final List<Objective> objectives = new ArrayList<>();
    private final List<Wall> walls = new ArrayList<>();

    private int currentLevelNumber = 1;
    private boolean running;
    private boolean gameOver;
    private boolean levelCompleted;
    private boolean paused;
    private long tickCount;

    public void configure(Level level) {
        this.currentLevel = level;
        this.walls.clear();
        this.walls.addAll(level.getWalls());
        this.objectives.clear();
        this.objectives.addAll(level.getObjectives());
        this.enemies.clear();
        for (Level.EnemySpawn spawn : level.getEnemies()) {
            this.enemies.add(new EnemyTank(
                    spawn.id(), spawn.type(), spawn.position(),
                    spawn.direction(), spawn.speed(),
                    spawn.defendedObjectiveId(), null));
        }
        this.player = new PlayerTank(
                "j1", level.getPlayerStart(),
                level.getPlayerStartDirection(), level.getPlayerLives());
        this.bullets.clear();
        this.currentLevelNumber = parseLevelNumber(level.getId());
        this.gameOver = false;
        this.levelCompleted = false;
        this.running = true;
        this.paused = false;
        this.tickCount = 0;
    }

    public Level getCurrentLevel() { return currentLevel; }
    public PlayerTank getPlayer() { return player; }
    public List<EnemyTank> getEnemies() { return List.copyOf(enemies); }
    public List<Objective> getObjectives() { return List.copyOf(objectives); }
    public List<Wall> getWalls() { return List.copyOf(walls); }
    public List<Bullet> getBullets() { return bullets; }
    public int getCurrentLevelNumber() { return currentLevelNumber; }
    public boolean isRunning() { return running; }
    public boolean isGameOver() { return gameOver; }
    public boolean isLevelCompleted() { return levelCompleted; }
    public boolean isPaused() { return paused; }
    public long getTickCount() { return tickCount; }

    public void setRunning(boolean running) { this.running = running; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }
    public void setLevelCompleted(boolean levelCompleted) { this.levelCompleted = levelCompleted; }
    public void incrementTick() { this.tickCount++; }

    public String nextBulletId() {
        return "b" + bulletIdSeq.getAndIncrement();
    }

    public List<EnemyTank> mutableEnemies() { return enemies; }
    public List<Objective> mutableObjectives() { return objectives; }
    public List<Wall> mutableWalls() { return walls; }

    public int aliveEnemyCount() {
        int n = 0;
        for (EnemyTank e : enemies) if (e.isAlive()) n++;
        return n;
    }

    public int activeObjectiveCount() {
        int n = 0;
        for (Objective o : objectives) if (o.isActive()) n++;
        return n;
    }

    public void addBullet(Bullet b) { bullets.add(b); }

    public void removeInactiveBullets() {
        bullets.removeIf(b -> !b.isActive());
    }

    private static int parseLevelNumber(String id) {
        if (id == null) return 1;
        String digits = id.replaceAll("\\D+", "");
        if (digits.isBlank()) return 1;
        try { return Integer.parseInt(digits); }
        catch (NumberFormatException e) { return 1; }
    }
}
