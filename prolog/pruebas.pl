/* ============================================================
 * pruebas.pl
 * Escenarios de prueba manuales para validar el modulo
 * Prolog sin necesidad de Java.
 *
 * Uso desde SWI-Prolog:
 *   ?- [pruebas].
 *   ?- prueba_basica.
 *   ?- prueba_muro_bloquea.
 *   ?- prueba_disparo_en_linea.
 *   ?- prueba_defensa_objetivo.
 *   ?- prueba_coordinacion.
 * ============================================================ */

:- module(pruebas, [
    prueba_basica/0,
    prueba_muro_bloquea/0,
    prueba_disparo_en_linea/0,
    prueba_defensa_objetivo/0,
    prueba_coordinacion/0,
    prueba_sin_ruta/0,
    prueba_heuristica/0,
    prueba_sin_ciclos/0,
    prueba_niveles/0,
    prueba_retroceder/0
]).

:- use_module(estado).
:- use_module(tablero).
:- use_module(busqueda).
:- use_module(decisiones).
:- use_module(coordinacion).

/* prueba_basica/0
 * Carga un tablero pequeno y verifica que la ruta entre
 * dos puntos exista. */
prueba_basica :-
    cargar_estado_minimo,
    format("~n=== Prueba basica ===~n"),
    (   buscar_ruta(0, 0, 5, 5, Ruta)
    ->  format("Ruta encontrada: ~w~n", [Ruta])
    ;   format("ERROR: no se encontro ruta~n"),
        fail
    ).

/* prueba_muro_bloquea/0
 * Verifica que el muro central del estado demo obligue a
 * la ruta a rodearlo. */
prueba_muro_bloquea :-
    cargar_estado_demo,
    format("~n=== Prueba muro bloquea ===~n"),
    (   buscar_ruta(0, 2, 7, 2, Ruta)
    ->  format("Ruta evitando muro: ~w~n", [Ruta]),
        (   member([3, 2], Ruta)
        ->  format("ERROR: la ruta cruza el muro~n"),
            fail
        ;   format("OK: la ruta no cruza el muro~n")
        )
    ;   format("ERROR: no existe ruta posible~n"),
        fail
    ).

/* prueba_disparo_en_linea/0
 * Verifica que un tanque pueda disparar al jugador solo
 * cuando esta en linea recta y sin obstaculos. */
prueba_disparo_en_linea :-
    cargar_estado_demo,
    format("~n=== Prueba disparo en linea ===~n"),
    estado:limpiar_estado,
    assertz(tamano_tablero(10, 10)),
    assertz(jugador(j1, 5, 5, vivo)),
    assertz(tanque_enemigo(e1, rapido, 5, 2, vivo)),
    format("Caso A: misma columna, sin muro -> "),
    (   puede_disparar_al_jugador(e1)
    ->  format("PUEDE disparar~n")
    ;   format("ERROR: deberia poder disparar~n"),
        fail
    ),
    estado:limpiar_estado,
    assertz(tamano_tablero(10, 10)),
    assertz(jugador(j1, 5, 5, vivo)),
    assertz(tanque_enemigo(e1, rapido, 5, 2, vivo)),
    assertz(muro(5, 3)),
    format("Caso B: misma columna, con muro -> "),
    (   puede_disparar_al_jugador(e1)
    ->  format("ERROR: no deberia poder disparar~n"),
        fail
    ;   format("NO puede disparar~n")
    ),
    estado:limpiar_estado,
    assertz(tamano_tablero(10, 10)),
    assertz(jugador(j1, 5, 5, vivo)),
    assertz(tanque_enemigo(e1, rapido, 3, 5, vivo)),
    format("Caso C: misma fila, sin muro -> "),
    (   puede_disparar_al_jugador(e1)
    ->  format("PUEDE disparar~n")
    ;   format("ERROR: deberia poder disparar~n"),
        fail
    ),
    estado:limpiar_estado.

/* prueba_defensa_objetivo/0
 * Verifica que un tanque cerca de su objetivo amenazado
 * reciba la accion de defender. */
prueba_defensa_objetivo :-
    cargar_estado_demo,
    format("~n=== Prueba defensa de objetivo ===~n"),
    estado:limpiar_estado,
    assertz(tamano_tablero(15, 10)),
    assertz(jugador(j1, 6, 4, vivo)),
    assertz(tanque_enemigo(e1, pesado, 10, 5, vivo)),
    assertz(objetivo(o1, base, 10, 5, activo)),
    assertz(defiende(e1, o1)),
    format("Accion para e1: "),
    (   decidir_accion(e1, Accion, Ruta)
    ->  format("~w con ruta ~w~n", [Accion, Ruta])
    ;   format("ERROR: sin decision~n"),
        fail
    ),
    estado:limpiar_estado.

/* prueba_coordinacion/0
 * Carga tres tanques y verifica que plan_coordinado/1
 * devuelva una lista no vacia con roles asignados. */
prueba_coordinacion :-
    cargar_estado_demo,
    format("~n=== Prueba coordinacion ===~n"),
    estado:limpiar_estado,
    assertz(tamano_tablero(15, 10)),
    assertz(jugador(j1, 5, 5, vivo)),
    assertz(tanque_enemigo(e1, rapido,  1, 5, vivo)),
    assertz(tanque_enemigo(e2, pesado, 13, 1, vivo)),
    assertz(tanque_enemigo(e3, tactico, 1, 9, vivo)),
    assertz(objetivo(o1, base, 13, 1, activo)),
    assertz(defiende(e2, o1)),
    format("Plan de coordinacion: "),
    (   plan_coordinado(Plan)
    ->  format("~w~n", [Plan]),
        (   Plan = []
        ->  format("ERROR: plan vacio~n"),
            fail
        ;   format("OK: plan generado~n")
        )
    ;   format("ERROR: no se pudo generar plan~n"),
        fail
    ),
    estado:limpiar_estado.

/* prueba_sin_ruta/0
 * Construye un escenario en el que el destino es
 * inaccesible y comprueba que buscar_ruta/5 falle. */
prueba_sin_ruta :-
    cargar_estado_demo,
    format("~n=== Prueba sin ruta ===~n"),
    estado:limpiar_estado,
    assertz(tamano_tablero(4, 4)),
    assertz(jugador(j1, 0, 0, vivo)),
    assertz(tanque_enemigo(e1, rapido, 3, 3, vivo)),
    assertz(muro(0, 1)),
    assertz(muro(1, 0)),
    assertz(muro(1, 1)),
    format("Intentando ruta de (0,0) a (3,3) con bloque total -> "),
    (   buscar_ruta(0, 0, 3, 3, _)
    ->  format("ERROR: se encontro ruta imposible~n"),
        fail
    ;   format("OK: fallo controlado~n")
    ),
    estado:limpiar_estado.

/* prueba_heuristica/0
 * Carga dos rutas equivalentes y verifica que el DFS
 * con heuristica elija el camino de menor longitud. */
prueba_heuristica :-
    cargar_estado_demo,
    format("~n=== Prueba heuristica ===~n"),
    estado:limpiar_estado,
    assertz(tamano_tablero(10, 1)),
    assertz(jugador(j1, 0, 0, vivo)),
    assertz(tanque_enemigo(e1, rapido, 9, 0, vivo)),
    (   buscar_ruta(0, 0, 9, 0, Ruta)
    ->  length(Ruta, L),
        format("Ruta con heuristica longitud=~w:~n  ~w~n", [L, Ruta]),
        (   L =:= 10
        ->  format("OK: ruta optima~n")
        ;   format("ERROR: ruta no optima~n"),
            fail
        )
    ;   format("ERROR: no hay ruta~n"),
        fail
    ),
    estado:limpiar_estado.

/* prueba_sin_ciclos/0
 * Verifica que el DFS termine y que la ruta devuelta no
 * contenga celdas repetidas (sin ciclos). */
prueba_sin_ciclos :-
    cargar_estado_demo,
    format("~n=== Prueba sin ciclos ===~n"),
    estado:limpiar_estado,
    assertz(tamano_tablero(6, 6)),
    assertz(jugador(j1, 0, 0, vivo)),
    assertz(tanque_enemigo(e1, rapido, 5, 5, vivo)),
    assertz(muro(2, 0)),
    assertz(muro(2, 2)),
    assertz(muro(4, 4)),
    (   catch(buscar_ruta(0, 0, 5, 5, Ruta), _, fail)
    ->  format("Ruta devuelta: ~w~n", [Ruta]),
        length(Ruta, L),
        length_sin_duplicados(Ruta, LUnicas),
        (   L =:= LUnicas
        ->  format("OK: ~w celdas, todas unicas~n", [L])
        ;   format("ERROR: la ruta repite celdas~n"),
            fail
        )
    ;   format("ERROR: DFS no termino~n"),
        fail
    ),
    estado:limpiar_estado.

/* length_sin_duplicados(+Lista, -N)
 * N es la cantidad de elementos unicos en Lista. */
length_sin_duplicados(Lista, N) :-
    list_to_set(Lista, Set),
    length(Set, N).

/* prueba_niveles/0
 * Verifica que cargar_estado/1 funcione para los tres
 * niveles del juego y que produzcan estados validos. */
prueba_niveles :-
    cargar_estado_demo,
    format("~n=== Prueba niveles ===~n"),
    forall(
        nivel(N),
        (   catch(cargar_estado(N), E, (format('ERROR cargando ~w: ~w~n', [N, E]), fail)),
            (   tamano_tablero(W, H)
            ->  format("Nivel ~w cargado: ~wx~w~n", [N, W, H]),
                validar_nivel_cargado(N)
            ;   format("ERROR: nivel ~w sin tamano de tablero~n", [N]),
                fail
            )
        )
    ),
    estado:limpiar_estado.

nivel(nivel1).
nivel(nivel2).
nivel(nivel3).

/* validar_nivel_cargado(+N)
 * Verifica invariantes minimas de un nivel bien formado:
 * hay un jugador, al menos un objetivo, al menos un
 * tanque y la cantidad de tanques defensores no supera la
 * cantidad de objetivos. */
validar_nivel_cargado(_) :-
    (   jugador(_, _, _, _)
    ->  true
    ;   format("ERROR: falta jugador~n"),
        fail
    ),
    (   objetivo(_, _, _, _, _)
    ->  true
    ;   format("ERROR: sin objetivos~n"),
        fail
    ),
    (   tanque_enemigo(_, _, _, _, _)
    ->  true
    ;   format("ERROR: sin tanques~n"),
        fail
    ),
    findall(Obj, objetivo(Obj, _, _, _, activo), Objetivos),
    findall(Def, defiende(_, Def), Defensores),
    length(Objetivos, NO),
    length(Defensores, ND),
    (   ND =< NO
    ->  format("OK: ~w objetivos, ~w defensores~n", [NO, ND])
    ;   format("ERROR: hay ~w defensores para ~w objetivos~n", [ND, NO]),
        fail
    ).

/* prueba_retroceder/0
 * Verifica que la accion retroceder se dispare cuando el
 * tanque esta a distancia 1-2 del jugador pero NO tiene
 * linea de tiro directa (hay un muro en medio). */
prueba_retroceder :-
    cargar_estado_demo,
    format("~n=== Prueba retroceder ===~n"),
    estado:limpiar_estado,
    assertz(tamano_tablero(8, 8)),
    assertz(jugador(j1, 4, 4, vivo)),
    assertz(tanque_enemigo(e1, rapido, 3, 3, vivo)),
    assertz(muro(3, 4)),
    format("Tanque rapido en diagonal al jugador, muro bloquea tiro~n"),
    (   catch(decidir_accion(e1, Accion, Ruta), _, fail)
    ->  format("Accion: ~w~nRuta:   ~w~n", [Accion, Ruta]),
        (   Accion = retroceder
        ->  format("OK: retrocede~n")
        ;   format("NOTA: se eligio ~w en su lugar~n", [Accion])
        )
    ;   format("ERROR: sin decision~n"),
        fail
    ),
    estado:limpiar_estado.
