# Plan para desarrollar la parte lógica en Prolog

## 1. Objetivo del módulo Prolog

El módulo en Prolog será el encargado de resolver la parte inteligente del juego **Tank-Attack**. Su función principal será recibir desde Java el estado actual del nivel y devolver una decisión lógica para cada tanque enemigo junto con una ruta calculada mediante búsqueda en profundidad, usando heurísticas para evitar rutas innecesarias o poco útiles.

Prolog no se encargará de dibujar el juego ni de mover directamente los objetos en pantalla. Su responsabilidad será analizar el tablero, tomar decisiones y calcular caminos.

---

## 2. Información que debe recibir Prolog desde Java

Para que Prolog pueda decidir correctamente, Java deberá enviarle hechos dinámicos con la información actual del juego. Antes de cada consulta importante, Java debe limpiar o actualizar los hechos anteriores y cargar el nuevo estado del tablero.

La información mínima que debe recibir Prolog es:

- Tamaño del tablero.
- Posición del tanque del jugador.
- Posición de cada tanque enemigo.
- Tipo de cada tanque enemigo.
- Posición de los objetivos primarios.
- Tipo de cada objetivo primario.
- Relación entre objetivo y tanque defensor.
- Posición de los muros.
- Posición de las balas activas, si se desea considerar peligro inmediato.
- Vidas actuales del jugador.
- Estado de cada objetivo: activo o destruido.
- Estado de cada tanque enemigo: vivo, destruido o en espera.

Ejemplo de hechos posibles:

```prolog
tamano_tablero(20, 15).
jugador(j1, 5, 10, vivo).
tanque_enemigo(e1, rapido, 12, 4, vivo).
tanque_enemigo(e2, pesado, 3, 8, vivo).
objetivo(o1, base, 15, 3, activo).
defiende(e1, o1).
muro(0, 0).
muro(1, 0).
bala(b1, jugador, 8, 10, derecha).
```

---

## 3. Uso de hechos dinámicos

Como cada nivel puede tener una distribución diferente y los objetos cambian durante la partida, los hechos deben manejarse de forma dinámica usando `assert`, `retract` y `retractall`.

Se deben declarar como dinámicos los predicados que cambian durante la ejecución:

```prolog
:- dynamic jugador/4.
:- dynamic tanque_enemigo/5.
:- dynamic objetivo/5.
:- dynamic defiende/2.
:- dynamic muro/2.
:- dynamic bala/5.
:- dynamic tamano_tablero/2.
```

Antes de cargar un nuevo estado desde Java, se recomienda limpiar los hechos anteriores:

```prolog
limpiar_estado :-
    retractall(jugador(_, _, _, _)),
    retractall(tanque_enemigo(_, _, _, _, _)),
    retractall(objetivo(_, _, _, _, _)),
    retractall(defiende(_, _)),
    retractall(muro(_, _)),
    retractall(bala(_, _, _, _, _)),
    retractall(tamano_tablero(_, _)).
```

---

## 4. Representación del tablero

El tablero debe tratarse como una matriz de coordenadas `(X, Y)`. Cada celda puede estar libre u ocupada por algún elemento.

Se debe crear un predicado para validar si una posición es transitable:

```prolog
posicion_valida(X, Y) :-
    tamano_tablero(Ancho, Alto),
    X >= 0,
    Y >= 0,
    X < Ancho,
    Y < Alto,
    \+ muro(X, Y),
    \+ objetivo(_, _, X, Y, activo).
```

También se debe decidir si los tanques enemigos bloquean o no el paso entre ellos. Para una primera versión, es recomendable que los tanques vivos también bloqueen el paso:

```prolog
ocupado_por_tanque(X, Y) :-
    tanque_enemigo(_, _, X, Y, vivo).
```

---

## 5. Movimiento en cuatro direcciones

Todos los tanques y balas solamente se mueven en las cuatro direcciones básicas. Por eso, Prolog debe trabajar con movimientos simples:

```prolog
movimiento(X, Y, X1, Y) :- X1 is X + 1.
movimiento(X, Y, X1, Y) :- X1 is X - 1.
movimiento(X, Y, X, Y1) :- Y1 is Y + 1.
movimiento(X, Y, X, Y1) :- Y1 is Y - 1.
```

Luego se debe validar que el movimiento lleve a una posición válida:

```prolog
vecino(X, Y, X1, Y1) :-
    movimiento(X, Y, X1, Y1),
    posicion_valida(X1, Y1).
```

---

## 6. Búsqueda en profundidad con heurística

El enunciado solicita una búsqueda en profundidad, pero mejorada con heurísticas para obtener rutas rápidas y útiles. La idea es usar DFS, pero ordenar los vecinos antes de explorarlos, dando prioridad a las posiciones más cercanas al destino.

La heurística recomendada es la distancia Manhattan:

```prolog
distancia_manhattan(X1, Y1, X2, Y2, D) :-
    DX is abs(X1 - X2),
    DY is abs(Y1 - Y2),
    D is DX + DY.
```

Para mejorar el DFS, antes de seguir buscando se ordenan los vecinos según su cercanía al destino:

```prolog
vecinos_ordenados(X, Y, DestX, DestY, VecinosOrdenados) :-
    findall([D, NX, NY],
        (
            vecino(X, Y, NX, NY),
            distancia_manhattan(NX, NY, DestX, DestY, D)
        ),
        Vecinos),
    sort(Vecinos, Ordenados),
    convertir_vecinos(Ordenados, VecinosOrdenados).

convertir_vecinos([], []).
convertir_vecinos([[_, X, Y] | Resto], [[X, Y] | Convertidos]) :-
    convertir_vecinos(Resto, Convertidos).
```

La búsqueda debe evitar ciclos usando una lista de visitados:

```prolog
buscar_ruta(InicioX, InicioY, DestX, DestY, Ruta) :-
    dfs(InicioX, InicioY, DestX, DestY, [[InicioX, InicioY]], RutaInvertida),
    reverse(RutaInvertida, Ruta).

dfs(X, Y, X, Y, Visitados, Visitados).
dfs(X, Y, DestX, DestY, Visitados, Ruta) :-
    vecinos_ordenados(X, Y, DestX, DestY, Vecinos),
    member([NX, NY], Vecinos),
    \+ member([NX, NY], Visitados),
    dfs(NX, NY, DestX, DestY, [[NX, NY] | Visitados], Ruta).
```

---

## 7. Decisiones de los tanques enemigos

Cada tanque enemigo debe consultar a Prolog para decidir qué acción realizar. Las decisiones mínimas recomendadas son:

- `atacar_jugador`: si el jugador está cerca o en línea de ataque.
- `defender_objetivo`: si el objetivo que protege está en peligro.
- `perseguir_jugador`: si el jugador está dentro de un rango medio.
- `patrullar`: si no hay peligro inmediato.
- `retroceder`: si el tanque está en desventaja o muy cerca del jugador.
- `emboscar`: si puede buscar una posición estratégica cercana al jugador.
- `coordinar_ataque`: si se implementa el punto extra con varios tanques.

Ejemplo de predicado principal:

```prolog
decidir_accion(TanqueID, Accion, Ruta) :-
    puede_disparar_al_jugador(TanqueID),
    Accion = disparar,
    Ruta = [].

decidir_accion(TanqueID, Accion, Ruta) :-
    objetivo_en_peligro(TanqueID, ObjetivoID),
    Accion = defender_objetivo,
    ruta_hacia_objetivo(TanqueID, ObjetivoID, Ruta).

decidir_accion(TanqueID, Accion, Ruta) :-
    jugador_cerca(TanqueID),
    Accion = perseguir_jugador,
    ruta_hacia_jugador(TanqueID, Ruta).

decidir_accion(TanqueID, Accion, Ruta) :-
    Accion = patrullar,
    ruta_patrullaje(TanqueID, Ruta).
```

---

## 8. Definición de cercanía

El enunciado deja a criterio del programador la definición de “cerca”. Se recomienda usar distancia Manhattan y definir rangos según el tipo de tanque.

Ejemplo:

```prolog
rango_disparo(rapido, 4).
rango_disparo(normal, 5).
rango_disparo(pesado, 6).

jugador_cerca(TanqueID) :-
    tanque_enemigo(TanqueID, Tipo, X, Y, vivo),
    jugador(_, JX, JY, vivo),
    rango_disparo(Tipo, Rango),
    distancia_manhattan(X, Y, JX, JY, Distancia),
    Distancia =< Rango.
```

---

## 9. Disparo en línea recta

Como las balas solo se mueven en cuatro direcciones, un tanque debería disparar solamente si el jugador está en la misma fila o columna y no hay muros entre ambos.

Se recomienda implementar:

- Mismo eje horizontal.
- Mismo eje vertical.
- Verificación de obstáculos entre tanque y jugador.
- Confirmación de que la distancia está dentro del rango permitido.

Predicados sugeridos:

```prolog
misma_fila(Y, Y).
misma_columna(X, X).

puede_disparar_al_jugador(TanqueID) :-
    tanque_enemigo(TanqueID, Tipo, X, Y, vivo),
    jugador(_, JX, JY, vivo),
    rango_disparo(Tipo, Rango),
    distancia_manhattan(X, Y, JX, JY, Distancia),
    Distancia =< Rango,
    (
        X =:= JX,
        camino_vertical_libre(X, Y, JY)
    ;
        Y =:= JY,
        camino_horizontal_libre(Y, X, JX)
    ).
```

---

## 10. Tipos de tanques enemigos

Se deben definir tres tipos de tanques enemigos con capacidades diferentes. Una propuesta simple es:

### Tanque rápido

- Se mueve más rápido en JavaFX.
- Tiene menor rango de disparo.
- Busca acercarse al jugador.
- Puede ser usado para emboscadas.

### Tanque pesado

- Se mueve más lento.
- Tiene mayor rango de disparo.
- Prioriza defender objetivos.
- Puede tener más resistencia si se decide implementar vida para enemigos.

### Tanque táctico

- Balanceado.
- Puede coordinar ataques.
- Prioriza posiciones estratégicas.
- Puede bloquear rutas del jugador.

En Prolog se pueden representar así:

```prolog
tipo_tanque(rapido).
tipo_tanque(pesado).
tipo_tanque(tactico).

rango_disparo(rapido, 4).
rango_disparo(pesado, 6).
rango_disparo(tactico, 5).

prioridad(rapido, perseguir_jugador).
prioridad(pesado, defender_objetivo).
prioridad(tactico, coordinar_ataque).
```

---

## 11. Defensa de objetivos

Cada objetivo debe tener un tanque defensor. Prolog debe conocer esa relación con el predicado `defiende/2`.

La defensa puede funcionar así:

- Si el jugador se acerca al objetivo, el tanque vuelve a defenderlo.
- Si el jugador está lejos del objetivo, el tanque puede patrullar o buscar una mejor posición.
- Si el objetivo ya fue destruido, el tanque puede pasar a perseguir al jugador o apoyar a otro tanque.

Predicados sugeridos:

```prolog
objetivo_en_peligro(TanqueID, ObjetivoID) :-
    defiende(TanqueID, ObjetivoID),
    objetivo(ObjetivoID, _, OX, OY, activo),
    jugador(_, JX, JY, vivo),
    distancia_manhattan(OX, OY, JX, JY, D),
    D =< 5.
```

---

## 12. Coordinación lógica entre tanques

Para obtener los puntos extra, se debe demostrar que dos o más tanques pueden tomar una decisión conjunta. La coordinación debe hacerse en Prolog, no solo en Java.

Estrategias recomendadas:

### Estrategia 1: Ataque por roles

Cuando varios tanques están cerca del jugador:

- Un tanque persigue directamente al jugador.
- Otro tanque intenta llegar a una posición lateral.
- Otro tanque protege el objetivo más cercano.

Ejemplo de roles:

```prolog
rol_coordinado(TanqueID, perseguidor) :-
    tanque_enemigo(TanqueID, rapido, _, _, vivo).

rol_coordinado(TanqueID, defensor) :-
    tanque_enemigo(TanqueID, pesado, _, _, vivo).

rol_coordinado(TanqueID, bloqueador) :-
    tanque_enemigo(TanqueID, tactico, _, _, vivo).
```

### Estrategia 2: Encierro del jugador

Prolog puede calcular posiciones alrededor del jugador y asignarlas a distintos tanques:

```prolog
posicion_ataque(jugador, arriba, X, Y1) :- jugador(_, X, Y, vivo), Y1 is Y - 1.
posicion_ataque(jugador, abajo, X, Y1) :- jugador(_, X, Y, vivo), Y1 is Y + 1.
posicion_ataque(jugador, izquierda, X1, Y) :- jugador(_, X, Y, vivo), X1 is X - 1.
posicion_ataque(jugador, derecha, X1, Y) :- jugador(_, X, Y, vivo), X1 is X + 1.
```

Después se asigna cada tanque a una posición distinta:

```prolog
coordinar_tanques(Tanques, Plan) :-
    posiciones_disponibles_ataque(Posiciones),
    asignar_roles(Tanques, Posiciones, Plan).
```

### Estrategia 3: Defensa conjunta

Si el jugador se acerca a un objetivo importante, el tanque asignado defiende directamente y otro tanque cercano se mueve a una posición de apoyo.

Este punto debe documentarse claramente, explicando:

- Cuándo se activa la coordinación.
- Qué tanques participan.
- Qué rol toma cada tanque.
- Qué ruta calcula cada tanque.
- Cómo Java ejecuta esas rutas.

---

## 13. Predicados principales que debe tener el módulo Prolog

Se recomienda organizar el archivo Prolog con estos predicados principales:

```prolog
limpiar_estado/0.
cargar_estado/1.
posicion_valida/2.
vecino/4.
distancia_manhattan/5.
buscar_ruta/5.
decidir_accion/3.
puede_disparar_al_jugador/1.
jugador_cerca/1.
objetivo_en_peligro/2.
ruta_hacia_jugador/2.
ruta_hacia_objetivo/3.
ruta_patrullaje/2.
coordinar_tanques/2.
asignar_roles/3.
```

---

## 14. Respuesta esperada hacia Java

Prolog debe devolver una respuesta simple de interpretar desde Java. Se recomienda devolver:

- ID del tanque.
- Acción decidida.
- Ruta como lista de coordenadas.

Ejemplo:

```prolog
decidir_accion(e1, perseguir_jugador, [[12,4], [11,4], [10,4], [9,4]]).
```

Java debe interpretar esa respuesta y mover el objeto `EnemyTank` siguiendo la ruta devuelta.

---

## 15. Pruebas necesarias en Prolog

Antes de conectar con Java, se deben hacer pruebas directas en SWI-Prolog.

Pruebas mínimas:

- Cargar un tablero simple.
- Verificar que los muros bloqueen rutas.
- Verificar que DFS encuentre una ruta válida.
- Verificar que la heurística priorice posiciones cercanas al destino.
- Verificar que un tanque dispare solo si está cerca y en línea recta.
- Verificar que un tanque defienda su objetivo.
- Verificar que varios tanques coordinen una estrategia.
- Verificar que no se generen ciclos infinitos.
- Verificar que si no existe ruta, Prolog devuelva una respuesta controlada.

---

## 16. Archivos Prolog recomendados

Para mantener ordenado el proyecto, se recomienda dividir la lógica en varios archivos:

```text
prolog/
├── main.pl
├── estado.pl
├── tablero.pl
├── busqueda.pl
├── decisiones.pl
├── coordinacion.pl
└── pruebas.pl
```

Descripción:

- `main.pl`: carga todos los módulos.
- `estado.pl`: hechos dinámicos y limpieza de estado.
- `tablero.pl`: validación de posiciones, muros y movimientos.
- `busqueda.pl`: DFS con heurística.
- `decisiones.pl`: lógica individual de cada tanque.
- `coordinacion.pl`: estrategias grupales para puntos extra.
- `pruebas.pl`: escenarios de prueba manuales.

---

## 17. Resultado final esperado de la parte Prolog

Al terminar esta parte, el proyecto debe tener un módulo Prolog capaz de:

- Recibir dinámicamente el estado del juego desde Java.
- Representar tablero, muros, jugador, enemigos, objetivos y balas.
- Calcular rutas usando DFS con heurística.
- Tomar decisiones individuales para cada tanque enemigo.
- Determinar cuándo un tanque puede disparar.
- Defender objetivos primarios.
- Coordinar varios tanques para atacar, bloquear o defender.
- Devolver a Java una acción y una ruta clara para cada tanque enemigo.
