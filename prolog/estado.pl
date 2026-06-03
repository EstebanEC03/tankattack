/* ============================================================
 * estado.pl
 * Manejo de hechos dinamicos y limpieza del estado del juego.
 *
 * Cada predicado que representa el estado actual del nivel se
 * declara como dinamico para poder ser actualizado con assert/
 * retract entre consultas desde Java.
 * ============================================================ */

:- module(estado, [
    limpiar_estado/0,
    cargar_estado/1,
    cargar_estado_minimo/0,
    cargar_estado_demo/0,
    jugador/4,
    tanque_enemigo/5,
    objetivo/5,
    defiende/2,
    muro/2,
    bala/5,
    tamano_tablero/2,
    vidas_jugador/1
]).

:- dynamic jugador/4.
:- dynamic tanque_enemigo/5.
:- dynamic objetivo/5.
:- dynamic defiende/2.
:- dynamic muro/2.
:- dynamic bala/5.
:- dynamic tamano_tablero/2.
:- dynamic vidas_jugador/1.

/* limpiar_estado/0
 * Retracta todos los hechos dinamicos. Debe invocarse desde Java
 * antes de cargar el estado de un nuevo nivel o de un nuevo
 * turno para evitar residuos de estados anteriores. */
limpiar_estado :-
    retractall(jugador(_, _, _, _)),
    retractall(tanque_enemigo(_, _, _, _, _)),
    retractall(objetivo(_, _, _, _, _)),
    retractall(defiende(_, _)),
    retractall(muro(_, _)),
    retractall(bala(_, _, _, _, _)),
    retractall(tamano_tablero(_, _)),
    retractall(vidas_jugador(_)).

/* cargar_estado(+Nivel)
 * Punto de entrada principal para cargar un nivel.
 * Nivel es un atomo que identifica el escenario:
 *   - minimo: tablero 10x8 vacio
 *   - demo:   escenario de pruebas con muro central
 *   - nivel1, nivel2, nivel3: niveles del juego (a
 *     personalizar; en esta version cargan escenarios
 *     predefinidos representativos).
 * Java llama cargar_estado(nivel1) antes de cada partida. */
cargar_estado(minimo) :- cargar_estado_minimo.
cargar_estado(demo)   :- cargar_estado_demo.
cargar_estado(nivel1) :- cargar_nivel(1).
cargar_estado(nivel2) :- cargar_nivel(2).
cargar_estado(nivel3) :- cargar_nivel(3).

/* cargar_nivel(+N)
 * Niveles del juego. Cada nivel tiene 2 objetivos y 3
 * tanques enemigos con distribuciones distintas. Los
 * valores concretos pueden ajustarse luego; esta version
 * es funcional y demostrable. */
cargar_nivel(1) :-
    limpiar_estado,
    assertz(tamano_tablero(20, 15)),
    assertz(vidas_jugador(3)),
    assertz(jugador(j1, 1, 1, vivo)),
    assertz(tanque_enemigo(e1, rapido,  16,  3, vivo)),
    assertz(tanque_enemigo(e2, pesado,   3, 12, vivo)),
    assertz(tanque_enemigo(e3, tactico, 18, 12, vivo)),
    assertz(objetivo(o1, base,  15,  3, activo)),
    assertz(objetivo(o2, refineria,  5, 12, activo)),
    assertz(defiende(e1, o1)),
    assertz(defiende(e2, o2)),
    assert_bordes_muro(20, 15).

cargar_nivel(2) :-
    limpiar_estado,
    assertz(tamano_tablero(20, 15)),
    assertz(vidas_jugador(3)),
    assertz(jugador(j1, 1, 7, vivo)),
    assertz(tanque_enemigo(e1, rapido,  17, 13, vivo)),
    assertz(tanque_enemigo(e2, tactico,  9,  2, vivo)),
    assertz(tanque_enemigo(e3, pesado,  17,  1, vivo)),
    assertz(objetivo(o1, base, 18, 13, activo)),
    assertz(objetivo(o2, refineria, 9,  1, activo)),
    assertz(defiende(e1, o1)),
    assertz(defiende(e3, o2)),
    assert_bordes_muro(20, 15).

cargar_nivel(3) :-
    limpiar_estado,
    assertz(tamano_tablero(20, 15)),
    assertz(vidas_jugador(3)),
    assertz(jugador(j1, 10, 7, vivo)),
    assertz(tanque_enemigo(e1, pesado,   2,  2, vivo)),
    assertz(tanque_enemigo(e2, rapido,  17,  2, vivo)),
    assertz(tanque_enemigo(e3, tactico, 10, 13, vivo)),
    assertz(objetivo(o1, base,  2,  1, activo)),
    assertz(objetivo(o2, refineria, 17,  1, activo)),
    assertz(defiende(e1, o1)),
    assertz(defiende(e2, o2)),
    assert_bordes_muro(20, 15).

/* assert_bordes_muro(+W, +H)
 * Crea un muro en cada celda del borde. Util para
 * delimitar el area jugable de cada nivel. */
assert_bordes_muro(W, H) :-
    between(0, W, X),
    assertz_muro_si_valido(X, 0),
    assertz_muro_si_valido(X, H),
    fail.
assert_bordes_muro(W, H) :-
    between(0, H, Y),
    assertz_muro_si_valido(0, Y),
    assertz_muro_si_valido(W, Y),
    fail.
assert_bordes_muro(_, _).

assertz_muro_si_valido(X, Y) :-
    X >= 0, Y >= 0,
    \+ muro(X, Y),
    assertz(muro(X, Y)).
assertz_muro_si_valido(_, _).

/* cargar_estado_minimo/0
 * Tablero 10x8 sin muros y sin enemigos, util para pruebas
 * aisladas de los algoritmos. */
cargar_estado_minimo :-
    limpiar_estado,
    assertz(tamano_tablero(10, 8)),
    assertz(jugador(j1, 0, 0, vivo)),
    assertz(vidas_jugador(3)).

/* cargar_estado_demo/0
 * Escenario pequeno con un muro central, un tanque enemigo
 * rapido y un objetivo defendido, pensado para verificar
 * manualmente que DFS rodee el muro. */
cargar_estado_demo :-
    limpiar_estado,
    assertz(tamano_tablero(8, 6)),
    assertz(jugador(j1, 0, 2, vivo)),
    assertz(vidas_jugador(3)),
    assertz(tanque_enemigo(e1, rapido, 7, 2, vivo)),
    assertz(objetivo(o1, base, 7, 5, activo)),
    assertz(defiende(e1, o1)),
    assertz(muro(3, 2)),
    assertz(muro(4, 2)),
    assertz(muro(3, 3)),
    assertz(muro(4, 3)).
