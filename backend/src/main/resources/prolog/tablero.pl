/* ============================================================
 * tablero.pl
 * Representacion del tablero y validacion de posiciones.
 *
 * El tablero es una matriz discreta de coordenadas (X, Y)
 * donde X representa la columna e Y la fila. El origen (0,0)
 * es la esquina superior izquierda.
 * ============================================================ */

:- module(tablero, [
    posicion_valida/2,
    movimiento/4,
    vecino/4,
    ocupado_por_tanque/2,
    ocupado_por_otro_tanque/3,
    mismo_punto/4,
    misma_fila/2,
    misma_columna/2,
    camino_vertical_libre/3,
    camino_horizontal_libre/3
]).

:- use_module(estado).

/* posicion_valida(+X, +Y)
 * Una posicion es valida si esta dentro del tablero, no
 * contiene un muro y no contiene un objetivo activo.
 *
 * NOTA sobre bloqueo entre tanques: el plan_prolog.md
 * (§4) sugiere que los tanques vivos bloqueen el paso.
 * En esta version NO se bloquean entre si, para
 * simplificar la primera entrega. Esta decision
 * permite que un tanque pueda planificar rutas que
 * pasan por la celda de otro tanque, lo cual se
 * resuelve en Java con la logica de colisiones. Si en
 * el futuro se desea que los tanques se bloqueen, se
 * debe aniadir la clausula:
 *   \+ ocupado_por_tanque(X, Y)
 * y manejar la exclusion del propio tanque con la
 * variante ocupado_por_otro_tanque/3. */
posicion_valida(X, Y) :-
    tamano_tablero(Ancho, Alto),
    X >= 0,
    Y >= 0,
    X < Ancho,
    Y < Alto,
    \+ muro(X, Y),
    \+ objetivo(_, _, X, Y, activo).

/* movimiento(+X, +Y, -X1, -Y1)
 * Genera los cuatro movimientos basicos permitidos en el
 * juego. La seleccion de cual se prueba primero depende
 * del orden de las clausulas y del ordenamiento externo. */
movimiento(X, Y, X1, Y) :- X1 is X + 1.
movimiento(X, Y, X1, Y) :- X1 is X - 1.
movimiento(X, Y, X, Y1) :- Y1 is Y + 1.
movimiento(X, Y, X, Y1) :- Y1 is Y - 1.

/* vecino(+X, +Y, -NX, -NY)
 * Movimiento basico que ademas cae en una posicion valida. */
vecino(X, Y, NX, NY) :-
    movimiento(X, Y, NX, NY),
    posicion_valida(NX, NY).

/* ocupado_por_tanque(+X, +Y)
 * Verdadero si hay un tanque enemigo vivo en (X,Y). Util
 * para reglas de colision en Java y para restricciones
 * adicionales en busqueda. */
ocupado_por_tanque(X, Y) :-
    tanque_enemigo(_, _, X, Y, vivo).

/* ocupado_por_otro_tanque(+X, +Y, +TanqueID)
 * Verdadero si hay un tanque enemigo vivo distinto a
 * TanqueID en (X,Y). Permite que un tanque "vea" a otros
 * como obstaculos sin bloquearse a si mismo. */
ocupado_por_otro_tanque(X, Y, TanqueID) :-
    tanque_enemigo(OtroID, _, X, Y, vivo),
    OtroID \= TanqueID.

/* mismo_punto/4 y mismas fila/columna auxiliares. */
mismo_punto(X, Y, X, Y).
misma_fila(Y, Y).
misma_columna(X, X).

/* camino_vertical_libre(+X, +Y1, +Y2)
 * Verdadero si no hay muros en la columna X entre las filas
 * Y1 y Y2 (excluyendo los extremos). Y1 puede ser mayor o
 * menor que Y2. */
camino_vertical_libre(_, Y, Y).
camino_vertical_libre(X, Y1, Y2) :-
    Y1 < Y2,
    YIntermedio is Y1 + 1,
    \+ muro(X, YIntermedio),
    camino_vertical_libre(X, YIntermedio, Y2).
camino_vertical_libre(X, Y1, Y2) :-
    Y1 > Y2,
    YIntermedio is Y1 - 1,
    \+ muro(X, YIntermedio),
    camino_vertical_libre(X, YIntermedio, Y2).

/* camino_horizontal_libre(+Y, +X1, +X2)
 * Verdadero si no hay muros en la fila Y entre las columnas
 * X1 y X2 (excluyendo los extremos). */
camino_horizontal_libre(_, X, X).
camino_horizontal_libre(Y, X1, X2) :-
    X1 < X2,
    XIntermedio is X1 + 1,
    \+ muro(XIntermedio, Y),
    camino_horizontal_libre(Y, XIntermedio, X2).
camino_horizontal_libre(Y, X1, X2) :-
    X1 > X2,
    XIntermedio is X1 - 1,
    \+ muro(XIntermedio, Y),
    camino_horizontal_libre(Y, XIntermedio, X2).
