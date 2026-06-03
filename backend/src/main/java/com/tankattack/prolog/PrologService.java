package com.tankattack.prolog;

import com.tankattack.model.EnemyAction;
import com.tankattack.model.EnemyType;
import com.tankattack.model.EnemyPlan;
import com.tankattack.model.GameState;
import com.tankattack.model.Position;
import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.JPL;
import org.jpl7.Query;
import org.jpl7.Term;
import org.jpl7.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Servicio que conecta el backend Java con el modulo Prolog.
 *
 * Responsabilidades:
 *   1. Cargar los archivos .pl desde el classpath a un
 *      directorio temporal y consultarlos.
 *   2. Inicializar el motor Prolog a traves de jpl_bridge.pl,
 *      que reexporta las predicados al modulo 'user' (el
 *      modulo por defecto de JPL).
 *   3. Convertir el {@link GameState} Java en hechos
 *      dinamicos Prolog.
 *   4. Consultar decisiones individuales y planes
 *      coordinados.
 *   5. Convertir las respuestas de Prolog a objetos Java.
 *
 * JPL no es thread-safe para escrituras en la base de
 * datos dinamica, por lo que todas las llamadas se
 * serializan con un {@link ReentrantLock}. El motor del
 * juego es single threaded, asi que en la practica el lock
 * no es un cuello de botella.
 *
 * Todas las consultas se construyen como Term (no como
 * String) porque la forma String+Term[] de JPL cuenta mal
 * los parametros cuando la meta contiene variables.
 */
public final class PrologService {

    private static final Logger log = LoggerFactory.getLogger(PrologService.class);

    private final ReentrantLock lock = new ReentrantLock();
    private Path prologDir;
    private boolean initialized;

    public void initialize() {
        lock.lock();
        try {
            if (initialized) return;

            ensureNativeLibraryLoaded();

            prologDir = extractPrologFiles();
            JPL.setTraditional();

            String consultCommand = String.format(
                    "consult('%s/jpl_bridge.pl')",
                    prologDir.toString().replace("'", "''"));
            if (!Query.hasSolution(consultCommand)) {
                throw new IllegalStateException(
                        "No se pudo cargar el modulo Prolog desde " + prologDir);
            }

            initialized = true;
            log.info("Prolog inicializado. Modulos cargados desde {}", prologDir);
        } finally {
            lock.unlock();
        }
    }

    private void ensureNativeLibraryLoaded() {
        // JPL consulta varias propiedades en este orden:
        //   1. jpl.library.path (directorio)
        //   2. jpl.lib.path (alias)
        //   3. java.library.path
        // Las dos primeras se reflejan en JPL.nativeLibraryPath.
        String libPath = System.getProperty("jpl.library.path");
        if (libPath == null) libPath = System.getProperty("jpl.lib.path");
        if (libPath == null) {
            String[] candidates = {
                    "/usr/lib/swi-prolog/lib/x86_64-linux",
                    "/usr/lib/swi-prolog/lib",
                    "/opt/homebrew/lib/swi-prolog/lib",
                    "/usr/local/lib/swi-prolog/lib"
            };
            for (String c : candidates) {
                java.io.File f = new java.io.File(c);
                if (f.isDirectory()
                        && f.listFiles((d, n) -> n.startsWith("libjpl")) != null) {
                    libPath = c;
                    break;
                }
            }
        }
        if (libPath != null) {
            // JPL distingue entre path (archivo) y dir (directorio).
            // Si es un directorio, usamos setNativeLibraryDir; si
            // es un archivo .so, usamos setNativeLibraryPath.
            java.io.File f = new java.io.File(libPath);
            if (f.isDirectory()) {
                JPL.setNativeLibraryDir(libPath);
            } else {
                JPL.setNativeLibraryPath(libPath);
            }
            log.info("JPL native library dir/path = {}", libPath);
        }
    }

    public void loadGameState(GameState state) {
        ensureInitialized();
        lock.lock();
        try {
            if (!Query.hasSolution("limpiar_estado")) {
                log.warn("limpiar_estado no se ejecuto como esperaba");
            }
            assertFact("tamano_tablero", state.getCurrentLevel().getWidth(),
                    state.getCurrentLevel().getHeight());
            assertFact("vidas_jugador", state.getPlayer().getLives());

            var p = state.getPlayer();
            assertFact("jugador", p.getId(), p.getPosition().x(), p.getPosition().y(),
                    p.isAlive() ? "vivo" : "muerto");

            for (var enemy : state.mutableEnemies()) {
                assertFact("tanque_enemigo", enemy.getId(),
                        enemy.getType().toPrologAtom(),
                        enemy.getPosition().x(), enemy.getPosition().y(),
                        enemy.isAlive() ? "vivo" : "muerto");
            }

            for (var obj : state.mutableObjectives()) {
                assertFact("objetivo", obj.getId(), obj.getType().toPrologAtom(),
                        obj.getPosition().x(), obj.getPosition().y(),
                        obj.isActive() ? "activo" : "destruido");
            }

            for (var enemy : state.mutableEnemies()) {
                if (enemy.getDefendedObjectiveId() != null) {
                    assertFact("defiende", enemy.getId(),
                            enemy.getDefendedObjectiveId());
                }
            }

            for (var wall : state.mutableWalls()) {
                assertFact("muro", wall.getPosition().x(), wall.getPosition().y());
            }

            for (var bullet : state.getBullets()) {
                if (bullet.isActive()) {
                    assertFact("bala", bullet.getId(),
                            bullet.isOwnerPlayer() ? "jugador" : "enemigo",
                            bullet.getPosition().x(), bullet.getPosition().y(),
                            bullet.getDirection().toPrologAtom());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public EnemyPlan decideEnemyAction(String enemyId) {
        ensureInitialized();
        lock.lock();
        try {
            Variable accion = new Variable("Accion");
            Variable ruta = new Variable("Ruta");
            Term goal = new Compound("decidir_accion", new Term[] {
                    new Atom(enemyId), accion, ruta
            });
            Query q = new Query(goal);
            try {
                if (!q.hasNext()) return null;
                Map<String, Term> sol = q.next();
                EnemyAction action = EnemyAction.fromPrologAtom(
                        termToAtomName(sol.get("Accion"))).orElse(EnemyAction.ESPERAR);
                List<Position> route = termToPositions(sol.get("Ruta"));
                return new EnemyPlan(enemyId, action, null, route);
            } finally {
                q.close();
            }
        } catch (Exception ex) {
            log.warn("Fallo decidir_accion para {}: {}", enemyId, ex.getMessage());
            return null;
        } finally {
            lock.unlock();
        }
    }

    public List<EnemyPlan> getCoordinatedPlan() {
        ensureInitialized();
        lock.lock();
        try {
            Variable planVar = new Variable("Plan");
            Term goal = new Compound("plan_coordinado", new Term[] { planVar });
            Query q = new Query(goal);
            try {
                if (!q.hasNext()) return List.of();
                Map<String, Term> sol = q.next();
                return parsePlan(sol.get("Plan"));
            } finally {
                q.close();
            }
        } catch (Exception ex) {
            log.warn("Fallo plan_coordinado: {}", ex.getMessage());
            return List.of();
        } finally {
            lock.unlock();
        }
    }

    public List<Position> findRoute(Position start, Position end) {
        ensureInitialized();
        lock.lock();
        try {
            Variable ruta = new Variable("Ruta");
            Term goal = new Compound("buscar_ruta", new Term[] {
                    new org.jpl7.Integer(start.x()),
                    new org.jpl7.Integer(start.y()),
                    new org.jpl7.Integer(end.x()),
                    new org.jpl7.Integer(end.y()),
                    ruta
            });
            Query q = new Query(goal);
            try {
                if (!q.hasNext()) return List.of();
                return termToPositions(q.next().get("Ruta"));
            } finally {
                q.close();
            }
        } catch (Exception ex) {
            log.warn("Fallo buscar_ruta: {}", ex.getMessage());
            return List.of();
        } finally {
            lock.unlock();
        }
    }

    public int getMovementSpeed(EnemyType type) {
        ensureInitialized();
        lock.lock();
        try {
            Variable v = new Variable("V");
            Term goal = new Compound("velocidad_movimiento", new Term[] {
                    new Atom(type.toPrologAtom()), v
            });
            Query q = new Query(goal);
            try {
                if (!q.hasNext()) return 1;
                Term t = q.next().get("V");
                return (t != null && t.isInteger()) ? t.intValue() : 1;
            } finally {
                q.close();
            }
        } catch (Exception ex) {
            return 1;
        } finally {
            lock.unlock();
        }
    }

    public void clearState() {
        ensureInitialized();
        lock.lock();
        try {
            Query.hasSolution("limpiar_estado");
        } finally {
            lock.unlock();
        }
    }

    /* ============================================================
     * Helpers
     * ============================================================ */

    private void assertFact(String functor, Object... args) {
        StringBuilder sb = new StringBuilder("assertz(").append(functor).append("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatArg(args[i]));
        }
        sb.append("))");
        try {
            Query q = new Query(sb.toString());
            try { q.hasNext(); } finally { q.close(); }
        } catch (Exception ex) {
            log.warn("assertz fallo: {} - {}", sb, ex.getMessage());
        }
    }

    private String formatArg(Object arg) {
        if (arg instanceof Integer i) return i.toString();
        if (arg instanceof Long l) return l.toString();
        if (arg instanceof String s) return "'" + escapeAtom(s) + "'";
        return "'" + arg + "'";
    }

    private String escapeAtom(String s) {
        return s.replace("\\", "\\\\").replace("'", "''");
    }

    private String termToAtomName(Term t) {
        if (t == null) return null;
        if (t.isAtom()) return ((Atom) t).name();
        return t.toString();
    }

    private List<Position> termToPositions(Term term) {
        List<Position> out = new ArrayList<>();
        if (term == null) return out;
        if (term.isListNil()) return out;

        Term[] elements = listToArray(term);
        if (elements == null) return out;

        for (Term el : elements) {
            Position p = termToPosition(el);
            if (p != null) out.add(p);
        }
        return out;
    }

    private Position termToPosition(Term t) {
        if (t == null) return null;

        // Caso 1: termino compuesto de 2 argumentos enteros
        // (por si Prolog devuelve una tupla explicita).
        if (t.isCompound() && t.arity() == 2
                && t.arg(1).isInteger() && t.arg(2).isInteger()) {
            return new Position(t.arg(1).intValue(), t.arg(2).intValue());
        }

        // Caso 2: lista de 2 elementos [X, Y] = .(X, .(Y, [])).
        // En JPL esto se ve como un list-pair cuyo head es un
        // entero y cuyo tail es otro list-pair cuyo head es
        // un entero (y tail es []).
        if (t.isListPair()) {
            Term head = t.arg(1);
            Term tail = t.arg(2);
            if (head != null && head.isInteger()
                    && tail != null && tail.isListPair()
                    && tail.arg(1).isInteger()
                    && tail.arg(2).isListNil()) {
                return new Position(head.intValue(), tail.arg(1).intValue());
            }
        }

        return null;
    }

    private Term[] listToArray(Term list) {
        if (list == null) return new Term[0];
        if (list.isListNil()) return new Term[0];
        try {
            return org.jpl7.Util.listToTermArray(list);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<EnemyPlan> parsePlan(Term planTerm) {
        List<EnemyPlan> result = new ArrayList<>();
        if (planTerm == null || planTerm.isListNil()) return result;
        Term[] elements = listToArray(planTerm);
        if (elements == null) return result;
        for (Term el : elements) {
            if (!el.isCompound() || !"plan".equals(el.name()) || el.arity() != 3) continue;
            String id = el.arg(1).isAtom() ? ((Atom) el.arg(1)).name() : el.arg(1).toString();
            String role = el.arg(2).isAtom() ? ((Atom) el.arg(2)).name() : el.arg(2).toString();
            List<Position> route = termToPositions(el.arg(3));

            EnemyAction action = inferActionFromRole(role);
            result.add(new EnemyPlan(id, action, role, route));
        }
        return result;
    }

    private EnemyAction inferActionFromRole(String role) {
        if (role == null) return EnemyAction.ESPERAR;
        String lower = role.toLowerCase();
        if (lower.startsWith("perseguidor")) return EnemyAction.PERSEGUIR_JUGADOR;
        if (lower.startsWith("flanco"))       return EnemyAction.PERSEGUIR_JUGADOR;
        if (lower.startsWith("defensor"))     return EnemyAction.DEFENDER_OBJETIVO;
        if (lower.startsWith("apoyo"))        return EnemyAction.DEFENDER_OBJETIVO;
        if (lower.startsWith("bloqueador"))   return EnemyAction.EMBOSCAR;
        if (lower.startsWith("asignado"))     return EnemyAction.COORDINAR;
        if (lower.startsWith("sin_ruta"))     return EnemyAction.ESPERAR;
        return EnemyAction.COORDINAR;
    }

    private Path extractPrologFiles() {
        try {
            Path tmp = Files.createTempDirectory("tankattack-prolog-");
            String[] files = {"main.pl", "estado.pl", "tablero.pl",
                    "busqueda.pl", "decisiones.pl", "coordinacion.pl", "pruebas.pl",
                    "jpl_bridge.pl"};
            for (String f : files) {
                URL resource = getClass().getClassLoader().getResource("prolog/" + f);
                if (resource == null) {
                    throw new IllegalStateException("No se encontro prolog/" + f + " en el classpath");
                }
                try (InputStream in = resource.openStream()) {
                    Path target = tmp.resolve(f);
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return tmp;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudieron extraer los archivos Prolog", ex);
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("PrologService no fue inicializado");
        }
    }

    public void shutdown() {
        lock.lock();
        try {
            initialized = false;
        } finally {
            lock.unlock();
        }
    }
}
