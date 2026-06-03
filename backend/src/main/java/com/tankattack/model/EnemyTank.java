package com.tankattack.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tanque enemigo. Su comportamiento es decidido por Prolog.
 * Mantiene la accion y la ruta actuales que el motor debe
 * ejecutar.
 */
public final class EnemyTank extends Tank {

    private final EnemyType type;
    private final String skin;
    private final String defendedObjectiveId;
    private final List<Position> currentRoute = new ArrayList<>();
    private EnemyAction currentAction = EnemyAction.ESPERAR;
    private String currentRole;
    private int routeIndex = 0;
    private int movesThisTick = 0;

    public EnemyTank(String id, EnemyType type, Position position,
                     Direction direction, int speed) {
        this(id, type, position, direction, speed, null, type.defaultSkin());
    }

    public EnemyTank(String id, EnemyType type, Position position,
                     Direction direction, int speed,
                     String defendedObjectiveId, String skin) {
        super(id, position, direction, 1, speed);
        this.type = Objects.requireNonNull(type, "type");
        this.defendedObjectiveId = defendedObjectiveId;
        this.skin = skin == null ? type.defaultSkin() : skin;
    }

    public EnemyType getType() { return type; }
    public String getSkin() { return skin; }
    public String getDefendedObjectiveId() { return defendedObjectiveId; }
    public EnemyAction getCurrentAction() { return currentAction; }
    public String getCurrentRole() { return currentRole; }
    public List<Position> getCurrentRoute() { return List.copyOf(currentRoute); }

    public void setCurrentAction(EnemyAction currentAction) {
        this.currentAction = currentAction;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    /**
     * Sustituye la ruta actual por una nueva. Si la ruta no
     * incluye la posicion actual, el motor aniadira la celda
     * actual como punto de partida.
     */
    public void setRoute(List<Position> route) {
        this.currentRoute.clear();
        if (route != null) this.currentRoute.addAll(route);
        this.routeIndex = 0;
    }

    public boolean hasMoreSteps() {
        return routeIndex < currentRoute.size();
    }

    public Position nextStep() {
        if (!hasMoreSteps()) return null;
        Position step = currentRoute.get(routeIndex);
        routeIndex++;
        return step;
    }

    /**
     * Velocidad maxima de pasos que puede dar en un mismo
     * tick. Se usa para que un tanque rapido pueda moverse
     * mas veces que uno pesado.
     */
    public int maxStepsPerTick() {
        return switch (type) {
            case RAPIDO  -> 2;
            case TACTICO -> 1;
            case PESADO  -> 1;
        };
    }

    public void beginTickMovement() {
        movesThisTick = 0;
    }

    public boolean canMoveMoreThisTick() {
        return movesThisTick < maxStepsPerTick();
    }

    public void recordStep() {
        movesThisTick++;
    }

    public boolean shouldActThisTick(long tickCount) {
        return switch (type) {
            case RAPIDO  -> true;
            case TACTICO -> tickCount % 2 == 0;
            case PESADO  -> tickCount % 3 == 0;
        };
    }
}
