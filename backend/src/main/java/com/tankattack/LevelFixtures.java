package com.tankattack;

import com.tankattack.model.Direction;
import com.tankattack.model.EnemyType;
import com.tankattack.model.Level;
import com.tankattack.model.Objective;
import com.tankattack.model.ObjectiveType;
import com.tankattack.model.Position;
import com.tankattack.model.Wall;

import java.util.ArrayList;
import java.util.List;

/** Niveles predefinidos que coinciden con los de Prolog. */
public final class LevelFixtures {

    private LevelFixtures() {}

    public static Level level1() {
        int w = 20, h = 15;
        List<Wall> walls = borderWalls(w, h);
        List<Objective> objectives = List.of(
                new Objective("o1", ObjectiveType.BASE, new Position(15, 3)),
                new Objective("o2", ObjectiveType.REFINERIA, new Position(5, 12))
        );
        List<Level.EnemySpawn> enemies = List.of(
                new Level.EnemySpawn("e1", EnemyType.RAPIDO,  new Position(16, 3),  Direction.LEFT,  4, "o1"),
                new Level.EnemySpawn("e2", EnemyType.PESADO,  new Position(3, 12),  Direction.RIGHT, 2, "o2"),
                new Level.EnemySpawn("e3", EnemyType.TACTICO, new Position(18, 12), Direction.LEFT,  3, null)
        );
        return new Level("nivel1", w, h, 32, walls, objectives, enemies,
                new Position(1, 1), Direction.RIGHT, 3);
    }

    public static Level level2() {
        int w = 20, h = 15;
        List<Wall> walls = borderWalls(w, h);
        List<Objective> objectives = List.of(
                new Objective("o1", ObjectiveType.BASE, new Position(18, 13)),
                new Objective("o2", ObjectiveType.REFINERIA, new Position(9, 1))
        );
        List<Level.EnemySpawn> enemies = List.of(
                new Level.EnemySpawn("e1", EnemyType.RAPIDO,  new Position(17, 13), Direction.LEFT,  4, "o1"),
                new Level.EnemySpawn("e2", EnemyType.TACTICO, new Position(9, 2),   Direction.DOWN,  3, null),
                new Level.EnemySpawn("e3", EnemyType.PESADO,  new Position(17, 1),  Direction.LEFT,  2, "o2")
        );
        return new Level("nivel2", w, h, 32, walls, objectives, enemies,
                new Position(1, 7), Direction.RIGHT, 3);
    }

    public static Level level3() {
        int w = 20, h = 15;
        List<Wall> walls = borderWalls(w, h);
        List<Objective> objectives = List.of(
                new Objective("o1", ObjectiveType.BASE, new Position(2, 1)),
                new Objective("o2", ObjectiveType.REFINERIA, new Position(17, 1))
        );
        List<Level.EnemySpawn> enemies = List.of(
                new Level.EnemySpawn("e1", EnemyType.PESADO,  new Position(2, 2),   Direction.RIGHT, 2, "o1"),
                new Level.EnemySpawn("e2", EnemyType.RAPIDO,  new Position(17, 2),  Direction.LEFT,  4, "o2"),
                new Level.EnemySpawn("e3", EnemyType.TACTICO, new Position(10, 13), Direction.UP,    3, null)
        );
        return new Level("nivel3", w, h, 32, walls, objectives, enemies,
                new Position(10, 7), Direction.RIGHT, 3);
    }

    public static List<Level> all() {
        return List.of(level1(), level2(), level3());
    }

    public static Level byId(String id) {
        if (id == null) return level1();
        return switch (id.toLowerCase()) {
            case "nivel1", "level1", "1" -> level1();
            case "nivel2", "level2", "2" -> level2();
            case "nivel3", "level3", "3" -> level3();
            default -> level1();
        };
    }

    private static List<Wall> borderWalls(int w, int h) {
        List<Wall> out = new ArrayList<>();
        for (int x = 0; x < w; x++) {
            out.add(new Wall(x, 0));
            out.add(new Wall(x, h - 1));
        }
        for (int y = 1; y < h - 1; y++) {
            out.add(new Wall(0, y));
            out.add(new Wall(w - 1, y));
        }
        return out;
    }
}
