/* ============================================================
 * busqueda.pl
 * Busqueda de rutas en el tablero.
 *
 * Implementa una busqueda en profundidad (DFS) mejorada
 * con una heuristica: los vecinos de cada nodo se ordenan
 * por distancia Manhattan al destino antes de explorarlos.
 * Esto mantiene el espiritu del DFS visto en clase y, a la
 * vez, prioriza los caminos mas prometedores para evitar
 * explorar demasiadas ramas inutiles.
 * ============================================================ */

:- module(busqueda, [
    distancia_manhattan/5,
    vecinos_ordenados/5,
    convertir_vecinos/2,
    buscar_ruta/5,
    dfs/6,
    ruta_existe/4
]).

:- use_module(tablero).
:- use_module(estado).

/* distancia_manhattan(+X1, +Y1, +X2, +Y2, -D)
 * Distancia Manhattan entre dos puntos. Se usa como
 * heuristica para ordenar los vecinos. */
distancia_manhattan(X1, Y1, X2, Y2, D) :-
    DX is abs(X1 - X2),
    DY is abs(Y1 - Y2),
    D is DX + DY.

/* vecinos_ordenados(+X, +Y, +DestX, +DestY, -Lista)
 * Devuelve los vecinos validos de (X,Y) ordenados de menor
 * a mayor distancia Manhattan al destino. El formato
 * interno [D, NX, NY] se usa solo para ordenar; luego se
 * convierte a [NX, NY] con convertir_vecinos/2. */
vecinos_ordenados(X, Y, DestX, DestY, VecinosOrdenados) :-
    findall([D, NX, NY],
            ( vecino(X, Y, NX, NY),
              distancia_manhattan(NX, NY, DestX, DestY, D) ),
            Vecinos),
    sort(Vecinos, Ordenados),
    convertir_vecinos(Ordenados, VecinosOrdenados).

convertir_vecinos([], []).
convertir_vecinos([[_, X, Y] | Resto], [[X, Y] | Convertidos]) :-
    convertir_vecinos(Resto, Convertidos).

/* buscar_ruta(+InicioX, +InicioY, +DestX, +DestY, -Ruta)
 * Punto de entrada publico. Devuelve la ruta desde el
 * inicio hasta el destino como una lista de coordenadas
 * [X, Y] en el orden en que se deben recorrer. Si no
 * existe ruta, falla (y debe ser manejado por el
 * llamador). */
buscar_ruta(InicioX, InicioY, DestX, DestY, Ruta) :-
    dfs(InicioX, InicioY, DestX, DestY, [[InicioX, InicioY]], RutaInvertida),
    reverse(RutaInvertida, Ruta).

/* dfs/6
 * Caso base: estamos en el destino, devolvemos la lista
 * de visitados (ruta invertida).
 * Caso recursivo: obtenemos vecinos ordenados por
 * heuristica y probamos cada uno que no haya sido
 * visitado. */
dfs(X, Y, X, Y, Visitados, Visitados).
dfs(X, Y, DestX, DestY, Visitados, Ruta) :-
    vecinos_ordenados(X, Y, DestX, DestY, Vecinos),
    member([NX, NY], Vecinos),
    \+ member([NX, NY], Visitados),
    dfs(NX, NY, DestX, DestY, [[NX, NY] | Visitados], Ruta).

/* ruta_existe/4
 * Variante que no une la ruta, util para decidir si vale
 * la pena intentar la accion. */
ruta_existe(InicioX, InicioY, DestX, DestY) :-
    buscar_ruta(InicioX, InicioY, DestX, DestY, _).
