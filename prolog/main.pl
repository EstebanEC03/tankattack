/* ============================================================
 * main.pl
 * Punto de entrada del modulo Prolog del juego Tank-Attack.
 *
 * Carga todos los modulos. Al consultarse desde Java no se
 * ejecuta ningun predicado automaticamente; Java es
 * responsable de limpiar el estado y luego cargar el
 * estado real antes de cada consulta.
 * ============================================================ */

:- module(tank_attack_prolog, []).

:- use_module(estado).
:- use_module(tablero).
:- use_module(busqueda).
:- use_module(decisiones).
:- use_module(coordinacion).
