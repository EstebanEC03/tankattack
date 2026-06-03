/* ============================================================
 * decisiones.pl
 * Logica individual de cada tanque enemigo.
 *
 * Decide que accion tomar y devuelve una ruta hacia el
 * objetivo correspondiente. Las acciones se prueban en
 * orden de prioridad: si el tanque puede disparar lo hace
 * de inmediato, luego defiende, persigue, embosca,
 * retrocede o patrulla.
 * ============================================================ */

:- module(decisiones, [
    decidir_accion/3,
    puede_disparar_al_jugador/1,
    jugador_cerca/1,
    jugador_lejos/1,
    jugador_en_linea_disparo/1,
    objetivo_en_peligro/2,
    ruta_hacia_jugador/2,
    ruta_hacia_objetivo/3,
    ruta_patrullaje/2,
    ruta_maniobra/2,
    tipo_tanque/1,
    rango_disparo/2,
    velocidad_movimiento/2,
    prioridad/2
]).

:- use_module(estado).
:- use_module(tablero).
:- use_module(busqueda).

/* ----- Tipos de tanques y sus caracteristicas ------------- */

tipo_tanque(rapido).
tipo_tanque(pesado).
tipo_tanque(tactico).

rango_disparo(rapido, 4).
rango_disparo(pesado, 6).
rango_disparo(tactico, 5).

/* velocidad_movimiento(Tipo, TilesPorSegundo)
 * Java puede consultar este predicado para ajustar el
 * delay entre movimientos segun el tipo. Por ahora
 * devuelve un valor nominal. */
velocidad_movimiento(rapido, 4).
velocidad_movimiento(tactico, 3).
velocidad_movimiento(pesado, 2).

/* prioridad(Tipo, AccionPreferida)
 * Accion que el tanque prefiere cuando ninguna condicion
 * urgente se cumple. Java puede usarla para informar al
 * usuario o para tomar decisiones de coordinacion. */
prioridad(rapido, perseguir_jugador).
prioridad(pesado, defender_objetivo).
prioridad(tactico, coordinar_ataque).

/* ----- Definiciones de cercania --------------------------- */

/* jugador_cerca(+TanqueID)
 * Verdadero si el jugador esta dentro del rango de
 * disparo del tanque. */
jugador_cerca(TanqueID) :-
    tanque_enemigo(TanqueID, Tipo, X, Y, vivo),
    jugador(_, JX, JY, vivo),
    rango_disparo(Tipo, Rango),
    distancia_manhattan(X, Y, JX, JY, Distancia),
    Distancia =< Rango.

/* jugador_lejos(+TanqueID)
 * Verdadero si el jugador esta claramente fuera de
 * cualquier rango de disparo. Se usa para decidir si
 * conviene perseguir o patrullar. */
jugador_lejos(TanqueID) :-
    tanque_enemigo(TanqueID, Tipo, X, Y, vivo),
    jugador(_, JX, JY, vivo),
    rango_disparo(Tipo, Rango),
    distancia_manhattan(X, Y, JX, JY, Distancia),
    Distancia > Rango + 2.

/* jugador_en_linea_disparo(+TanqueID)
 * Verdadero si el tanque y el jugador comparten fila o
 * columna y no hay muros entre ellos. */
jugador_en_linea_disparo(TanqueID) :-
    tanque_enemigo(TanqueID, _, X, Y, vivo),
    jugador(_, JX, JY, vivo),
    (   X =:= JX, camino_vertical_libre(X, Y, JY)
    ;   Y =:= JY, camino_horizontal_libre(Y, X, JX)
    ).

/* puede_disparar_al_jugador(+TanqueID)
 * El tanque puede disparar si esta dentro de su rango y
 * ademas el jugador esta en linea recta sin obstaculos. */
puede_disparar_al_jugador(TanqueID) :-
    jugador_cerca(TanqueID),
    jugador_en_linea_disparo(TanqueID).

/* objetivo_en_peligro(+TanqueID, -ObjetivoID)
 * Verdadero si TanqueID defiende un objetivo y el jugador
 * esta a una distancia amenazante de dicho objetivo. */
objetivo_en_peligro(TanqueID, ObjetivoID) :-
    defiende(TanqueID, ObjetivoID),
    objetivo(ObjetivoID, _, OX, OY, activo),
    jugador(_, JX, JY, vivo),
    distancia_manhattan(OX, OY, JX, JY, D),
    D =< 5.

/* ----- Rutas segun accion --------------------------------- */

/* ruta_hacia_jugador(+TanqueID, -Ruta)
 * Construye la ruta desde el tanque hasta la celda
 * adyacente al jugador en la direccion del tanque. Si la
 * ruta no existe, falla. */
ruta_hacia_jugador(TanqueID, Ruta) :-
    tanque_enemigo(TanqueID, _, X, Y, vivo),
    jugador(_, JX, JY, vivo),
    buscar_ruta(X, Y, JX, JY, Ruta).

/* ruta_hacia_objetivo(+TanqueID, +ObjetivoID, -Ruta)
 * Construye la ruta desde el tanque hasta la celda
 * adyacente al objetivo que protege. */
ruta_hacia_objetivo(TanqueID, ObjetivoID, Ruta) :-
    tanque_enemigo(TanqueID, _, X, Y, vivo),
    objetivo(ObjetivoID, _, OX, OY, activo),
    buscar_ruta(X, Y, OX, OY, Ruta).

/* ruta_patrullaje(+TanqueID, -Ruta)
 * Si el tanque protege un objetivo, calcula una ruta que
 * rodea el objetivo. Si no, devuelve una ruta hacia el
 * centro del tablero. Si ninguna ruta existe, falla. */
ruta_patrullaje(TanqueID, Ruta) :-
    tanque_enemigo(TanqueID, _, X, Y, vivo),
    (   defiende(TanqueID, ObjetivoID),
        objetivo(ObjetivoID, _, OX, OY, activo),
        cell_patrulla(OX, OY, PX, PY)
    ;   centro_tablero(PX, PY)
    ),
    buscar_ruta(X, Y, PX, PY, Ruta).

/* cell_patrulla(+OX, +OY, -PX, -PY)
 * Devuelve una celda adyacente valida al objetivo
 * (OX,OY) para usarla como punto de patrulla. */
cell_patrulla(OX, OY, PX, PY) :-
    (   PX is OX + 1, PY = OY, posicion_valida(PX, PY)
    ;   PX is OX - 1, PY = OY, posicion_valida(PX, PY)
    ;   PX = OX, PY is OY + 1, posicion_valida(PX, PY)
    ;   PX = OX, PY is OY - 1, posicion_valida(PX, PY)
    ;   PX = OX, PY = OY
    ).

/* centro_tablero(-X, -Y)
 * Calcula la celda central del tablero. Se usa como
 * destino por defecto para tanques sin objetivo. */
centro_tablero(CX, CY) :-
    tamano_tablero(Ancho, Alto),
    CX is Ancho // 2,
    CY is Alto // 2.

/* ruta_maniobra(+TanqueID, -Ruta)
 * Variante de ruta_patrullaje usada cuando el tanque
 * decide emboscar: se dirige a una celda lateral cercana
 * al jugador pero sin alcanzarlo. */
ruta_maniobra(TanqueID, Ruta) :-
    tanque_enemigo(TanqueID, _, X, Y, vivo),
    jugador(_, JX, JY, vivo),
    cell_lateral(JX, JY, TX, TY),
    buscar_ruta(X, Y, TX, TY, Ruta).

/* cell_lateral(+JX, +JY, -TX, -TY)
 * Devuelve una celda adyacente al jugador, priorizando
 * las diagonales horizontales. */
cell_lateral(JX, JY, TX, TY) :-
    (   TX is JX + 1, TY is JY, posicion_valida(TX, TY)
    ;   TX is JX - 1, TY is JY, posicion_valida(TX, TY)
    ;   TX is JX, TY is JY + 1, posicion_valida(TX, TY)
    ;   TX is JX, TY is JY - 1, posicion_valida(TX, TY)
    ;   TX = JX, TY = JY
    ).

/* cell_retroceso(+JX, +JY, +TX, +TY, -RX, -RY)
 * Devuelve una celda adyacente al tanque (TX,TY) que
 * esta en direccion opuesta al jugador (JX,JY). Sirve
 * para que el tanque se aleje del jugador. */
cell_retroceso(JX, JY, TX, TY, RX, RY) :-
    (   TX > JX, RX is TX + 1, RY = TY, posicion_valida(RX, RY)
    ;   TX < JX, RX is TX - 1, RY = TY, posicion_valida(RX, RY)
    ;   TY > JY, RX = TX, RY is TY + 1, posicion_valida(RX, RY)
    ;   TY < JY, RX = TX, RY is TY - 1, posicion_valida(RX, RY)
    ;   RX is TX + 1, RY = TY, posicion_valida(RX, RY)
    ;   RX is TX - 1, RY = TY, posicion_valida(RX, RY)
    ;   RX = TX, RY is TY + 1, posicion_valida(RX, RY)
    ;   RX = TX, RY is TY - 1, posicion_valida(RX, RY)
    ;   RX = TX, RY = TY
    ).

/* ----- Predicado principal decidir_accion ----------------- */

/* decidir_accion(+TanqueID, -Accion, -Ruta)
 * Devuelve la accion recomendada para TanqueID junto con
 * la ruta asociada. Las clausulas se prueban en orden, por
 * lo que el orden refleja la prioridad. Si la primera
 * accion posible no tiene ruta, se intenta la siguiente. */
decidir_accion(TanqueID, Accion, Ruta) :-
    puede_disparar_al_jugador(TanqueID),
    Accion = disparar,
    Ruta = [].

decidir_accion(TanqueID, Accion, Ruta) :-
    objetivo_en_peligro(TanqueID, ObjetivoID),
    ruta_hacia_objetivo(TanqueID, ObjetivoID, Ruta),
    Accion = defender_objetivo.

/* Si el tanque esta muy cerca del jugador (D=1 o 2)
 * pero no tiene linea de tiro, retrocede para obtener
 * una mejor posicion. Se prioriza sobre perseguir para
 * evitar que el tanque se "pegue" al jugador. */
decidir_accion(TanqueID, Accion, Ruta) :-
    jugador_cerca(TanqueID),
    tanque_enemigo(TanqueID, _, X, Y, vivo),
    jugador(_, JX, JY, vivo),
    distancia_manhattan(X, Y, JX, JY, D),
    D =< 2,
    cell_retroceso(JX, JY, X, Y, RX, RY),
    buscar_ruta(X, Y, RX, RY, Ruta),
    Accion = retroceder.

decidir_accion(TanqueID, Accion, Ruta) :-
    jugador_cerca(TanqueID),
    ruta_hacia_jugador(TanqueID, Ruta),
    Accion = perseguir_jugador.

decidir_accion(TanqueID, Accion, Ruta) :-
    jugador_lejos(TanqueID),
    ruta_maniobra(TanqueID, Ruta),
    Accion = emboscar.

decidir_accion(TanqueID, Accion, Ruta) :-
    ruta_patrullaje(TanqueID, Ruta),
    Accion = patrullar.

/* Fallback: si no se puede calcular ninguna ruta valida,
 * el tanque espera. Devolver una accion deterministica
 * permite que Java no quede colgado esperando una
 * decision. */
decidir_accion(_, esperar, []) :-
    \+ (tanque_enemigo(TanqueID, _, _, _, vivo),
        (   puede_disparar_al_jugador(TanqueID)
        ;   objetivo_en_peligro(TanqueID, _),
            ruta_hacia_objetivo(TanqueID, _, _)
        ;   jugador_cerca(TanqueID),
            (   ruta_hacia_jugador(TanqueID, _)
            ;   (   tanque_enemigo(TanqueID, _, X, Y, vivo),
                    jugador(_, JX, JY, vivo),
                    distancia_manhattan(X, Y, JX, JY, D),
                    D =< 2,
                    cell_retroceso(JX, JY, X, Y, _, _),
                    buscar_ruta(X, Y, _, _, _)
                )
            )
        ;   jugador_lejos(TanqueID),
            ruta_maniobra(TanqueID, _)
        ;   ruta_patrullaje(TanqueID, _)
        )).
