package com.tankattack.model;

import java.util.List;
import java.util.Objects;

/**
 * Decision tomada por Prolog para un tanque enemigo. Es el
 * resultado de {@code decidir_accion(ID, Accion, Ruta)} o
 * de un elemento de {@code plan_coordinado/1}.
 */
public final class EnemyPlan {

    private final String enemyId;
    private final EnemyAction action;
    private final String role;
    private final List<Position> route;

    public EnemyPlan(String enemyId, EnemyAction action, String role, List<Position> route) {
        this.enemyId = Objects.requireNonNull(enemyId, "enemyId");
        this.action = action == null ? EnemyAction.ESPERAR : action;
        this.role = role;
        this.route = route == null ? List.of() : List.copyOf(route);
    }

    public String getEnemyId() { return enemyId; }
    public EnemyAction getAction() { return action; }
    public String getRole() { return role; }
    public List<Position> getRoute() { return route; }
}
