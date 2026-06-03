/* ============================================================
 * coordinacion.pl
 * Coordinacion logica entre dos o mas tanques enemigos.
 *
 * Implementa las tres estrategias descritas en el plan
 * para optar al punto extra:
 *   1) Ataque por roles (perseguidor, defensor, bloqueador).
 *   2) Encierro del jugador (calcular celdas adyacentes).
 *   3) Defensa conjunta (apoyo entre tanques defensores).
 *
 * El predicado principal es coordinar_tanques/2, que
 * devuelve un Plan con la lista de acciones/rutas para
 * todos los tanques solicitados.
 * ============================================================ */

:- module(coordinacion, [
    rol_coordinado/2,
    posicion_ataque/4,
    posiciones_disponibles_ataque/1,
    tanques_vivos/1,
    coordinar_tanques/2,
    asignar_roles/3,
    plan_coordinado/1
]).

:- use_module(estado).
:- use_module(tablero).
:- use_module(busqueda).
:- use_module(decisiones).

/* ----- Roles segun tipo de tanque ------------------------- */

rol_coordinado(TanqueID, perseguidor) :-
    tanque_enemigo(TanqueID, rapido, _, _, vivo).

rol_coordinado(TanqueID, defensor) :-
    tanque_enemigo(TanqueID, pesado, _, _, vivo).

rol_coordinado(TanqueID, bloqueador) :-
    tanque_enemigo(TanqueID, tactico, _, _, vivo).

/* ----- Asignacion de roles -------------------------------- */

/* asignar_roles(+Tanques, +Posiciones, -Plan)
 * Variante "pura": recibe tanques y celdas destino
 * (no necesariamente adyacentes al jugador) y calcula
 * las rutas correspondientes. Util cuando Java ya tiene
 * una distribucion precalculada de coordenadas y solo
 * quiere delegar el calculo de rutas. */
asignar_roles([], _, []).
asignar_roles([TanqueID | Ts], [[X, Y] | Pys], [plan(TanqueID, Rol, Ruta) | Ps]) :-
    (   ruta_hacia_posicion(TanqueID, X, Y, Ruta)
    ->  Rol = asignado
    ;   Ruta = [],
        Rol = sin_ruta
    ),
    asignar_roles(Ts, Pys, Ps).
asignar_roles(_, [], []).

/* ----- Celdas adyacentes al jugador ----------------------- */

posicion_ataque(jugador, arriba,    X, Y1) :- jugador(_, X, Y, vivo), Y1 is Y - 1.
posicion_ataque(jugador, abajo,     X, Y1) :- jugador(_, X, Y, vivo), Y1 is Y + 1.
posicion_ataque(jugador, izquierda, X1, Y) :- jugador(_, X, Y, vivo), X1 is X - 1.
posicion_ataque(jugador, derecha,   X1, Y) :- jugador(_, X, Y, vivo), X1 is X + 1.

/* posiciones_disponibles_ataque(-Posiciones)
 * Lista de direcciones en las que hay una celda adyacente
 * valida al jugador. Se usa para asignar tanques a los
 * flancos. */
posiciones_disponibles_ataque(Posiciones) :-
    findall(Dir,
            ( posicion_ataque(jugador, Dir, X, Y),
              posicion_valida(X, Y) ),
            Posiciones).

/* tanques_vivos(-Tanques)
 * Lista con los IDs de todos los tanques enemigos vivos.
 * Util como punto de partida para la coordinacion. */
tanques_vivos(Tanques) :-
    findall(ID, tanque_enemigo(ID, _, _, _, vivo), Tanques).

/* ----- Predicado principal -------------------------------- */

/* plan_coordinado(-Plan)
 * Construye el plan de coordinacion para todos los tanques
 * vivos del estado actual. Cada elemento del plan tiene la
 * forma: plan(ID, Rol, Ruta). */
plan_coordinado(Plan) :-
    tanques_vivos(Tanques),
    (   Tanques = []
    ->  Plan = []
    ;   seleccionar_estrategia(Estrategia),
        ejecutar_estrategia(Estrategia, Tanques, Plan)
    ).

coordinar_tanques(Tanques, Plan) :-
    (   Tanques = []
    ->  Plan = []
    ;   seleccionar_estrategia(Estrategia),
        ejecutar_estrategia(Estrategia, Tanques, Plan)
    ).

/* seleccionar_estrategia(-Estrategia)
 * Elige la estrategia segun la situacion del juego. Las
 * reglas se prueban en orden; la primera que aplique
 * sera la usada. */
seleccionar_estrategia(defensa_conjunta) :-
    jugador_amenaza_objetivo, !.
seleccionar_estrategia(encierro) :-
    length_tanques_vivos(N), N >= 2, !.
seleccionar_estrategia(roles).
seleccionar_estrategia(defensa_conjunta).

/* jugador_amenaza_objetivo
 * Verdadero si hay al menos un objetivo activo dentro de
 * un radio amenazante del jugador. */
jugador_amenaza_objetivo :-
    objetivo(_, _, OX, OY, activo),
    jugador(_, JX, JY, vivo),
    distancia_manhattan(OX, OY, JX, JY, D),
    D =< 5.

/* length_tanques_vivos(-N)
 * Cuenta los tanques vivos. */
length_tanques_vivos(N) :-
    findall(ID, tanque_enemigo(ID, _, _, _, vivo), Tanques),
    length(Tanques, N).

/* ejecutar_estrategia(+Estrategia, +Tanques, -Plan)
 * Despacha a la implementacion correspondiente. */
ejecutar_estrategia(roles, Tanques, Plan)            :- estrategia_roles(Tanques, Plan).
ejecutar_estrategia(encierro, Tanques, Plan)         :- estrategia_encierro(Tanques, Plan).
ejecutar_estrategia(defensa_conjunta, Tanques, Plan) :- estrategia_defensa_conjunta(Tanques, Plan).

/* ----- Estrategia 1: Roles -------------------------------- */

/* estrategia_roles(+Tanques, -Plan)
 * Asigna a cada tanque un rol segun su tipo y calcula su
 * ruta individual. Si el tanque no tiene una ruta util,
 * se omite del plan. */
estrategia_roles([], []).
estrategia_roles([TanqueID | Resto], Plan) :-
    (   rol_coordinado(TanqueID, Rol),
        ruta_para_rol(TanqueID, Rol, Ruta),
        Plan = [plan(TanqueID, Rol, Ruta) | PlanResto]
    ;   Plan = PlanResto
    ),
    estrategia_roles(Resto, PlanResto).

/* ruta_para_rol(+TanqueID, +Rol, -Ruta)
 * Calcula la ruta adecuada al rol. */
ruta_para_rol(TanqueID, perseguidor, Ruta) :-
    !,
    ruta_hacia_jugador(TanqueID, Ruta).
ruta_para_rol(TanqueID, defensor, Ruta) :-
    !,
    (   defiende(TanqueID, ObjetivoID),
        ruta_hacia_objetivo(TanqueID, ObjetivoID, Ruta)
    ;   ruta_patrullaje(TanqueID, Ruta)
    ).
ruta_para_rol(TanqueID, bloqueador, Ruta) :-
    !,
    ruta_maniobra(TanqueID, Ruta).
ruta_para_rol(TanqueID, _, Ruta) :-
    ruta_patrullaje(TanqueID, Ruta).

/* ----- Estrategia 2: Encierro ----------------------------- */

/* estrategia_encierro(+Tanques, -Plan)
 * Distribuye los tanques en las celdas adyacentes al
 * jugador. Cada tanque recibe la ruta hacia una celda
 * distinta. Si no hay suficientes celdas validas, los
 * tanques sobrantes caen a un rol de bloqueo. */
estrategia_encierro(Tanques, Plan) :-
    posiciones_disponibles_ataque(PosicionesLibres),
    length(PosicionesLibres, NumLibres),
    length(Tanques, NumTanques),
    Min is min(NumLibres, NumTanques),
    take_first(Min, Tanques, TanquesAsignados),
    take_first(Min, PosicionesLibres, PosicionesAsignadas),
    pairs_to_plans(TanquesAsignados, PosicionesAsignadas, PlansDirectos),
    drop_first(Min, Tanques, TanquesExtra),
    list_to_plans_bloqueo(TanquesExtra, PlansExtra),
    append(PlansDirectos, PlansExtra, Plan).

take_first(0, _, []) :- !.
take_first(N, [X | Xs], [X | Ys]) :-
    N > 0,
    N1 is N - 1,
    take_first(N1, Xs, Ys).

drop_first(0, L, L) :- !.
drop_first(_, [], []) :- !.
drop_first(N, [_ | Xs], Resto) :-
    N > 0,
    N1 is N - 1,
    drop_first(N1, Xs, Resto).

/* pairs_to_plans(+Tanques, +Posiciones, -Plans)
 * Empareja tanques con celdas adyacentes al jugador y
 * calcula la ruta correspondiente. */
pairs_to_plans([], [], []).
pairs_to_plans([TanqueID | Ts], [Dir | Ds], [plan(TanqueID, Rol, Ruta) | Ps]) :-
    (   ruta_a_posicion_ataque(TanqueID, Dir, Ruta)
    ->  Rol = flanco(Dir)
    ;   Ruta = [],
        Rol = sin_ruta
    ),
    pairs_to_plans(Ts, Ds, Ps).

/* ruta_a_posicion_ataque(+TanqueID, +Dir, -Ruta)
 * Construye la ruta del tanque hasta la celda adyacente al
 * jugador en la direccion Dir. */
ruta_a_posicion_ataque(TanqueID, Dir, Ruta) :-
    tanque_enemigo(TanqueID, _, X, Y, vivo),
    posicion_ataque(jugador, Dir, PX, PY),
    buscar_ruta(X, Y, PX, PY, Ruta).

/* list_to_plans_bloqueo(+Tanques, -Plans)
 * Para los tanques que no cupieron en el encierro, genera
 * planes de bloqueo hacia la celda lateral del jugador. */
list_to_plans_bloqueo([], []).
list_to_plans_bloqueo([TanqueID | Ts], [plan(TanqueID, bloqueador, Ruta) | Ps]) :-
    (   ruta_maniobra(TanqueID, Ruta)
    ->  true
    ;   Ruta = []
    ),
    list_to_plans_bloqueo(Ts, Ps).

/* ----- Estrategia 3: Defensa conjunta --------------------- */

/* estrategia_defensa_conjunta(+Tanques, -Plan)
 * El primer tanque va directo al objetivo amenazado. Los
 * restantes se ubican en posiciones de apoyo alrededor del
 * objetivo. */
estrategia_defensa_conjunta(Tanques, Plan) :-
    objetivo_mas_amenazado(_ObjetivoID, OX, OY),
    (   Tanques = []
    ->  Plan = []
    ;   Tanques = [Defensor | Apoyo],
        (   ruta_hacia_posicion(Defensor, OX, OY, RutaDefensor)
        ->  PlanDefensor = [plan(Defensor, defensor, RutaDefensor)]
        ;   PlanDefensor = []
        ),
        posiciones_apoyo(OX, OY, Posiciones),
        pairs_to_apoyo(Apoyo, Posiciones, PlanApoyo),
        append(PlanDefensor, PlanApoyo, Plan)
    ).

/* objetivo_mas_amenazado(-ID, -X, -Y)
 * Devuelve el objetivo activo mas cercano al jugador. */
objetivo_mas_amenazado(ID, X, Y) :-
    findall(D-Obj, objetivo_amenazado(D, Obj), Pares),
    keysort(Pares, Sorted),
    Sorted = [_-ID | _],
    objetivo(ID, _, X, Y, activo).

objetivo_amenazado(Distancia, ObjetivoID) :-
    objetivo(ObjetivoID, _, OX, OY, activo),
    jugador(_, JX, JY, vivo),
    distancia_manhattan(OX, OY, JX, JY, Distancia).

/* ruta_hacia_posicion(+TanqueID, +X, +Y, -Ruta)
 * Wrapper de buscar_ruta para mantener consistencia con
 * otros modulos. */
ruta_hacia_posicion(TanqueID, X, Y, Ruta) :-
    tanque_enemigo(TanqueID, _, SX, SY, vivo),
    buscar_ruta(SX, SY, X, Y, Ruta).

/* posiciones_apoyo(+OX, +OY, -Posiciones)
 * Calcula celdas alrededor del objetivo, una por tanque de
 * apoyo disponible (hasta 4). */
posiciones_apoyo(OX, OY, Posiciones) :-
    findall([X, Y],
            (   X is OX + 1, Y = OY, posicion_valida(X, Y)
            ;   X is OX - 1, Y = OY, posicion_valida(X, Y)
            ;   X = OX, Y is OY + 1, posicion_valida(X, Y)
            ;   X = OX, Y is OY - 1, posicion_valida(X, Y)
            ),
            Posiciones).

pairs_to_apoyo([], _, []).
pairs_to_apoyo(_, [], []).
pairs_to_apoyo([TanqueID | Ts], [[X, Y] | Pys], [plan(TanqueID, apoyo, Ruta) | Ps]) :-
    (   ruta_hacia_posicion(TanqueID, X, Y, Ruta)
    ->  true
    ;   Ruta = []
    ),
    pairs_to_apoyo(Ts, Pys, Ps).
