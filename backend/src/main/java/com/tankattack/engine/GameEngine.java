package com.tankattack.engine;

import com.tankattack.collision.CollisionManager;
import com.tankattack.level.LevelLoader;
import com.tankattack.model.Bullet;
import com.tankattack.model.Direction;
import com.tankattack.model.EnemyAction;
import com.tankattack.model.EnemyPlan;
import com.tankattack.model.EnemyTank;
import com.tankattack.model.GameState;
import com.tankattack.model.Level;
import com.tankattack.model.Objective;
import com.tankattack.model.PlayerTank;
import com.tankattack.model.Position;
import com.tankattack.prolog.PrologService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Motor principal del juego.
 *
 * <p><b>Tick manual basado en acciones del jugador.</b>
 * A diferencia de un tick autonomo, aqui la simulacion solo
 * avanza cuando el jugador envia una accion (mover, disparar,
 * etc). Esto permite que los tanques enemigos se muevan
 * unicamente cuando el jugador lo hace, manteniendo una
 * dinamica por turnos visible.</p>
 *
 * <p>Cada accion del jugador (cuando el juego no esta en pausa,
 * game over o nivel completo) ejecuta un tick que:</p>
 * <ol>
 *   <li>Aplica el input.</li>
 *   <li>Avanza las balas.</li>
 *   <li>Recalcula decisiones y planes coordinados de los
 *       enemigos cada cierto numero de ticks.</li>
 *   <li>Avanza los enemigos siguiendo sus rutas.</li>
 *   <li>Los enemigos disparan si su accion es DISPARAR.</li>
 *   <li>Detecta fin de nivel / game over.</li>
 *   <li>Notifica a los listeners (WebSocket).</li>
 * </ol>
 */
public final class GameEngine {

    private static final Logger log = LoggerFactory.getLogger(GameEngine.class);

    /** Cada cuantos ticks se recalculan rutas de enemigos. */
    public static final int ENEMY_DECISION_INTERVAL = 1;
    /** Cada cuantos ticks se recalcula el plan coordinado. */
    public static final int COORDINATION_INTERVAL = 6;
    /** Cooldown entre disparos de cada enemigo (ticks). */
    public static final int ENEMY_SHOT_COOLDOWN_TICKS = 4;

    private final PrologService prolog;
    private final CollisionManager collisions;
    private final LevelLoader levelLoader;
    private final List<StateListener> listeners = new CopyOnWriteArrayList<>();

    private GameState state;
    private long nowNanos;
    private int enemyShotCooldown = 0;

    public GameEngine(PrologService prolog) {
        this.prolog = prolog;
        this.collisions = new CollisionManager();
        this.levelLoader = new LevelLoader();
    }

    public GameState getState() { return state; }

    public void addStateListener(StateListener l) { listeners.add(l); }
    public void removeStateListener(StateListener l) { listeners.remove(l); }

    /* ============================================================
     * Control de partida
     * ============================================================ */

    public synchronized void startGame() {
        if (state == null) loadLevel("nivel1");
        state.setRunning(true);
        state.setPaused(false);
        state.setGameOver(false);
        state.setLevelCompleted(false);
        enemyShotCooldown = 0;
        notifyListeners();
    }

    public synchronized void restart() {
        if (state == null || state.getCurrentLevel() == null) {
            loadLevel("nivel1");
        } else {
            loadLevel(state.getCurrentLevel().getId());
        }
        startGame();
    }

    public synchronized void stopGame() {
        if (state != null) state.setRunning(false);
    }

    public void shutdown() {
        stopGame();
    }

    public synchronized void loadLevel(String levelId) {
        Level level = levelLoader.predefined(levelId);
        if (level == null) throw new IllegalArgumentException("Nivel no encontrado: " + levelId);
        if (state == null) state = new GameState();
        state.configure(level);
        enemyShotCooldown = 0;
        notifyListeners();
    }

    public synchronized void loadLevel(Level level) {
        if (level == null) throw new IllegalArgumentException("level null");
        if (state == null) state = new GameState();
        state.configure(level);
        enemyShotCooldown = 0;
        notifyListeners();
    }

    /* ============================================================
     * Entradas del jugador
     *
     * La simulacion avanza exactamente un tick por cada input
     * que implique accion (MOVE o SHOOT). Pause y Restart
     * siempre estan permitidos, incluso con el juego pausado,
     * para evitar que el jugador quede bloqueado.
     * ============================================================ */

    public synchronized void handlePlayerInput(PlayerInput input) {
        if (state == null) return;

        switch (input.type()) {
            case PAUSE -> {
                if (state.isGameOver() || state.isLevelCompleted()) return;
                state.setPaused(!state.isPaused());
                notifyListeners();
                return;
            }
            case RESTART -> {
                restart();
                return;
            }
            case MOVE, SHOOT -> {
                if (!state.isRunning()) return;
                if (state.isPaused() || state.isGameOver() || state.isLevelCompleted()) {
                    return;
                }
                if (input.type() == PlayerInput.InputType.MOVE) {
                    handleMove(input.direction());
                } else {
                    handleShoot();
                }
                // Cada accion del jugador dispara exactamente
                // un tick de la simulacion: asi los enemigos
                // solo se mueven cuando el jugador se mueve.
                tick();
            }
        }
    }

    private void handleMove(Direction dir) {
        if (dir == null) return;
        PlayerTank p = state.getPlayer();
        if (!p.isAlive()) return;
        collisions.tryMovePlayer(state, dir);
    }

    private void handleShoot() {
        PlayerTank p = state.getPlayer();
        if (!p.isAlive()) return;
        if (!p.canShoot(nowNanos)) return;
        Direction dir = p.getDirection();
        Position bulletPos = bulletSpawn(p.getPosition(), dir);
        Bullet b = new Bullet(state.nextBulletId(), p.getId(), true,
                bulletPos, dir, Bullet.DEFAULT_SPEED_TILES_PER_SEC);
        state.addBullet(b);
        p.markShot(nowNanos);
    }

    private static Position bulletSpawn(Position origin, Direction dir) {
        return switch (dir) {
            case UP    -> origin.translate(0, -1);
            case DOWN  -> origin.translate(0,  1);
            case LEFT  -> origin.translate(-1, 0);
            case RIGHT -> origin.translate( 1, 0);
        };
    }

    /* ============================================================
     * Tick
     * Avanza la simulacion exactamente una unidad.
     * Llamado por handlePlayerInput cuando hay accion.
     * ============================================================ */

    void tick() {
        try {
            if (state == null) return;
            if (state.isPaused() || state.isGameOver() || state.isLevelCompleted()) {
                return;
            }
            nowNanos = System.nanoTime();
            state.incrementTick();

            collisions.updateBullets(state);

            if (state.getTickCount() % ENEMY_DECISION_INTERVAL == 0) {
                prolog.loadGameState(state);
                decideEnemies();
            }

            if (state.getTickCount() % COORDINATION_INTERVAL == 0) {
                applyCoordination();
            }

            advanceEnemies();
            maybeEnemiesShoot();
            enemyShotCooldown++;

            checkEndConditions();
            notifyListeners();
        } catch (Exception ex) {
            log.error("Error en tick del motor", ex);
        }
    }

    private void decideEnemies() {
        for (EnemyTank e : state.mutableEnemies()) {
            if (!e.isAlive()) continue;
            EnemyPlan plan = prolog.decideEnemyAction(e.getId());
            if (plan == null) continue;
            e.setCurrentAction(plan.getAction());
            e.setCurrentRole(plan.getRole());
            applyPlanToEnemy(e, plan);
        }
    }

    private void applyPlanToEnemy(EnemyTank e, EnemyPlan plan) {
        if (plan.getRoute() == null || plan.getRoute().isEmpty()) {
            e.setRoute(List.of());
            return;
        }
        List<Position> route = new ArrayList<>(plan.getRoute());
        // La ruta de Prolog siempre empieza en la posicion
        // actual del tanque. Removemos esa primera celda
        // para que los siguientes nextStep() den vecinos.
        if (!route.isEmpty() && route.get(0).equals(e.getPosition())) {
            route = route.subList(1, route.size());
        }
        e.setRoute(route);
    }

    private void maybeEnemiesShoot() {
        if (enemyShotCooldown < ENEMY_SHOT_COOLDOWN_TICKS) return;
        if (state.getTickCount() % ENEMY_DECISION_INTERVAL != 0) return;
        for (EnemyTank e : state.mutableEnemies()) {
            if (!e.isAlive()) continue;
            if (e.getCurrentAction() == EnemyAction.DISPARAR) {
                spawnEnemyBullet(e);
            }
        }
        enemyShotCooldown = 0;
    }

    private void spawnEnemyBullet(EnemyTank e) {
        Direction dir = directionTowards(e.getPosition(), state.getPlayer().getPosition());
        Position bulletPos = bulletSpawn(e.getPosition(), dir);
        if (!collisions.isInBounds(state, bulletPos)) return;
        if (collisions.wallAt(state, bulletPos).isPresent()) return;
        Bullet b = new Bullet(state.nextBulletId(), e.getId(), false,
                bulletPos, dir, Bullet.DEFAULT_SPEED_TILES_PER_SEC);
        state.addBullet(b);
    }

    private static Direction directionTowards(Position from, Position to) {
        if (to.x() < from.x()) return Direction.LEFT;
        if (to.x() > from.x()) return Direction.RIGHT;
        if (to.y() < from.y()) return Direction.UP;
        return Direction.DOWN;
    }

    private void applyCoordination() {
        prolog.loadGameState(state);
        List<EnemyPlan> plans = prolog.getCoordinatedPlan();
        Map<String, EnemyPlan> byId = new HashMap<>();
        for (EnemyPlan p : plans) byId.put(p.getEnemyId(), p);

        for (EnemyTank e : state.mutableEnemies()) {
            if (!e.isAlive()) continue;
            EnemyPlan plan = byId.get(e.getId());
            if (plan == null) continue;
            e.setCurrentRole(plan.getRole());
            if (plan.getAction() != null) {
                e.setCurrentAction(plan.getAction());
            }
            if (plan.getRoute() != null && !plan.getRoute().isEmpty()) {
                applyPlanToEnemy(e, plan);
            }
        }
    }

    private void advanceEnemies() {
        for (EnemyTank e : state.mutableEnemies()) {
            if (!e.isAlive()) continue;
            e.beginTickMovement();
            while (e.hasMoreSteps() && e.canMoveMoreThisTick()) {
                Position step = e.nextStep();
                if (step == null) break;
                Position from = e.getPosition();
                if (collisions.tryMoveEnemy(state, e, step)) {
                    Direction dir = directionTowards(from, step);
                    e.setDirection(dir);
                    e.recordStep();
                } else {
                    // La ruta quedo invalidada (otro tanque
                    // o una bala); recalcular proximo tick.
                    e.setRoute(List.of());
                    break;
                }
            }
        }
    }

    private void checkEndConditions() {
        if (!state.getPlayer().isAlive()) {
            state.setGameOver(true);
            state.setRunning(false);
        } else if (state.activeObjectiveCount() == 0) {
            state.setLevelCompleted(true);
            state.setRunning(false);
        }
    }

    private void notifyListeners() {
        GameState snapshot = state;
        for (StateListener l : listeners) {
            try { l.onStateChange(snapshot); }
            catch (Exception ex) { log.warn("Listener fallo: {}", ex.getMessage()); }
        }
    }

    /* ============================================================
     * Metodos de soporte
     * ============================================================ */

    public PrologService prolog() { return prolog; }
    public CollisionManager collisions() { return collisions; }
    public LevelLoader levelLoader() { return levelLoader; }

    public int getCurrentLevelNumber() {
        return state == null ? 0 : state.getCurrentLevelNumber();
    }

    public List<Level> availableLevels() {
        return levelLoader.predefinedAll();
    }

    /** Fuerza la siguiente nivel cuando se completa una. */
    public synchronized boolean advanceToNextLevel() {
        if (state == null) return false;
        int next = state.getCurrentLevelNumber() + 1;
        String id = "nivel" + next;
        if (levelLoader.predefined(id) == null) return false;
        loadLevel(id);
        startGame();
        return true;
    }

    public List<Objective> getObjectives() {
        return state == null ? List.of() : state.getObjectives();
    }
}
