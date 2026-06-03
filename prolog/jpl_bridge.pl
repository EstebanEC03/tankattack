/* ============================================================
 * jpl_bridge.pl
 * Modulo puente para que JPL (Java) pueda invocar las
 * predicados de los modulos del proyecto usando nombres
 * planos (sin prefijo de modulo).
 *
 * Carga main.pl y luego importa las predicados publicas al
 * modulo 'user' (el modulo por defecto en el que JPL
 * ejecuta las consultas). Esto evita que JPL tenga que
 * usar la sintaxis modulo:predicado, que su parser no
 * maneja bien con variables.
 * ============================================================ */

:- consult('main.pl').

:- use_module(decisiones, [
        decidir_accion/3,
        puede_disparar_al_jugador/1,
        jugador_cerca/1,
        jugador_lejos/1,
        objetivo_en_peligro/2,
        ruta_hacia_jugador/2,
        ruta_hacia_objetivo/3,
        ruta_patrullaje/2,
        ruta_maniobra/2,
        velocidad_movimiento/2
]).

:- use_module(estado, [
        limpiar_estado/0,
        cargar_estado/1,
        jugador/4,
        tanque_enemigo/5,
        objetivo/5,
        defiende/2,
        muro/2,
        bala/5,
        tamano_tablero/2,
        vidas_jugador/1
]).

:- use_module(busqueda, [
        buscar_ruta/5,
        distancia_manhattan/5
]).

:- use_module(coordinacion, [
        plan_coordinado/1,
        coordinar_tanques/2
]).

:- use_module(tablero, [
        posicion_valida/2,
        mismo_punto/4
]).
