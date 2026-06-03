# Plan de Java Backend para Tank-Attack

## Stack definido

El backend del juego se implementará con:

- **Java** como lenguaje principal orientado a objetos.
- **Javalin** como servidor liviano para exponer API REST y WebSocket.
- **SWI-Prolog** como motor lógico externo.
- **JPL** o ejecución controlada de SWI-Prolog desde Java para consultar reglas, decisiones y rutas.
- **JSON** como formato de intercambio entre backend y frontend.
- **Frontend separado con Vite + TypeScript + Canvas**, conectado al backend por REST y WebSocket.

Este plan es compatible con el plan de frontend porque Java será el dueño del estado real del juego, mientras que el frontend solo enviará entradas del usuario y dibujará el estado recibido.

---

## Objetivo del backend

El backend será el motor principal del juego. Debe encargarse de:

- Mantener el estado real de la partida.
- Administrar niveles, tanques, objetivos, muros, balas y vidas.
- Aplicar la lógica orientada a objetos.
- Comunicarse con Prolog para obtener decisiones, rutas y coordinación entre tanques enemigos.
- Enviar el estado actualizado al frontend.
- Recibir acciones del jugador desde el frontend.
- Garantizar que el frontend no tenga lógica crítica del juego.

---

## Relación general entre Java, Prolog y Frontend

La arquitectura recomendada será:

```text
Frontend Vite + TypeScript + Canvas
        |
        | WebSocket: movimiento, disparos, estado actualizado
        | REST: iniciar juego, cargar nivel, reiniciar, editar mapas
        v
Backend Java + Javalin
        |
        | Consultas a SWI-Prolog
        v
Motor lógico Prolog
```

Java no debe duplicar la lógica de búsqueda o decisión que ya existe en Prolog. Java debe solicitar a Prolog acciones como:

- Decidir qué debe hacer un tanque enemigo.
- Calcular una ruta con DFS y heurística.
- Obtener un plan coordinado entre varios tanques.
- Validar rutas y comportamientos estratégicos.

---

## Responsabilidades principales de Java

Java será responsable de implementar toda la parte orientada a objetos del juego:

1. **Motor del juego**
   - Ciclo principal del juego.
   - Actualización de entidades.
   - Control de tiempo.
   - Movimiento de tanques.
   - Movimiento de balas.
   - Detección de colisiones.
   - Validación de fin de nivel.
   - Validación de victoria o derrota.

2. **Modelo de objetos**
   - Clases para tanques, balas, objetivos, muros, tablero, niveles y controlador del juego.
   - Encapsulamiento de atributos.
   - Uso de herencia, interfaces o composición según convenga.
   - Métodos públicos claros para interactuar con cada objeto.
   - Atributos privados para proteger el estado interno.

3. **Integración con Prolog**
   - Cargar los archivos `.pl`.
   - Enviar el estado actual del nivel a Prolog.
   - Consultar decisiones individuales de tanques.
   - Consultar planes coordinados.
   - Recibir rutas y convertirlas a objetos Java.
   - Manejar errores si Prolog no devuelve ruta.

4. **Comunicación con frontend**
   - Exponer rutas REST para acciones generales.
   - Exponer WebSocket para comunicación en tiempo real.
   - Enviar snapshots del estado del juego en JSON.
   - Recibir entradas del jugador desde el navegador.

---

## Clases principales recomendadas

### GameEngine

Clase principal del motor.

Responsabilidades:

- Iniciar la partida.
- Detener la partida.
- Actualizar el juego por ticks.
- Coordinar llamadas a Prolog.
- Actualizar enemigos, balas, objetivos y jugador.
- Notificar al frontend con el estado actualizado.

Métodos sugeridos:

```java
startGame()
stopGame()
loadLevel(int levelNumber)
update()
handlePlayerInput(PlayerInput input)
getGameState()
```

---

### GameState

Representa el estado completo de la partida.

Atributos sugeridos:

```java
Level currentLevel;
PlayerTank player;
List<EnemyTank> enemies;
List<Bullet> bullets;
List<Objective> objectives;
List<Wall> walls;
int currentLevelNumber;
boolean running;
boolean gameOver;
boolean levelCompleted;
```

Debe ser convertible a JSON para enviarlo al frontend.

---

### Level

Representa un nivel del juego.

Responsabilidades:

- Guardar tamaño del tablero.
- Guardar muros.
- Guardar posiciones iniciales.
- Guardar objetivos.
- Definir configuración inicial de enemigos.

Atributos sugeridos:

```java
int width;
int height;
List<Wall> walls;
List<Objective> objectives;
List<EnemyTank> enemies;
Position playerStart;
```

---

### Position

Clase simple para representar coordenadas.

Atributos:

```java
int x;
int y;
```

Métodos sugeridos:

```java
boolean equals(Object other)
int distanceTo(Position other)
```

---

### Direction

Enum para representar las cuatro direcciones permitidas.

```java
UP, DOWN, LEFT, RIGHT
```

Debe coincidir con el movimiento permitido por el enunciado y con las rutas devueltas por Prolog.

---

### GameObject

Clase abstracta base para todos los objetos del juego.

Atributos sugeridos:

```java
String id;
Position position;
boolean active;
```

Métodos sugeridos:

```java
getId()
getPosition()
setPosition(Position position)
isActive()
```

---

### Tank

Clase abstracta para tanques.

Hereda de `GameObject`.

Atributos sugeridos:

```java
Direction direction;
int lives;
int speed;
long lastShotTime;
```

Métodos sugeridos:

```java
move(Direction direction)
shoot()
takeDamage()
canShoot()
```

---

### PlayerTank

Representa el tanque manejado por el usuario.

Responsabilidades:

- Moverse según entradas del jugador.
- Disparar cuando el usuario lo indique.
- Perder vidas cuando recibe impacto.

---

### EnemyTank

Representa un tanque enemigo.

Atributos sugeridos:

```java
EnemyType type;
String defendedObjectiveId;
List<Position> currentRoute;
EnemyAction currentAction;
```

Métodos sugeridos:

```java
updateBehavior(PrologService prologService)
followRoute()
setRoute(List<Position> route)
```

El tipo del tanque debe corresponder con los tipos definidos en Prolog:

- `rapido`
- `pesado`
- `tactico`

---

### EnemyType

Enum para representar los tipos de tanque enemigo.

```java
RAPIDO, PESADO, TACTICO
```

Debe tener métodos para convertir entre Java y Prolog:

```java
toPrologAtom()
fromPrologAtom(String atom)
```

---

### EnemyAction

Enum para representar las acciones que Prolog puede devolver.

Valores sugeridos:

```java
DISPARAR,
DEFENDER_OBJETIVO,
RETROCEDER,
PERSEGUIR_JUGADOR,
EMBOSCAR,
PATRULLAR,
ESPERAR,
COORDINAR
```

También debe tener conversión desde átomos de Prolog.

---

### Bullet

Representa una bala como objeto del juego.

Atributos sugeridos:

```java
String ownerId;
Position position;
Direction direction;
int speed;
boolean active;
```

Responsabilidades:

- Avanzar en línea recta.
- Detectar colisión con muro.
- Detectar colisión con tanque.
- Detectar colisión con objetivo.
- Desactivarse después del impacto.

---

### Objective

Representa un objetivo primario.

Atributos sugeridos:

```java
String id;
ObjectiveType type;
Position position;
boolean active;
```

Tipos sugeridos:

```java
BASE, REFINERIA
```

En la parte visual, estos objetivos tendrán estilo vaporwave.

---

### Wall

Representa un muro.

Atributos:

```java
Position position;
```

Los muros no se destruyen y deben bloquear tanques y balas.

---

### CollisionManager

Clase encargada de validar colisiones.

Responsabilidades:

- Verificar si una posición está bloqueada.
- Verificar impacto de balas contra muros.
- Verificar impacto de balas contra tanques.
- Verificar impacto de balas contra objetivos.
- Evitar que los tanques atraviesen muros u objetivos.

---

### LevelLoader

Clase encargada de cargar niveles.

Opciones:

- Cargar niveles desde archivos JSON.
- Generar niveles aleatorios desde Java.
- Cargar configuraciones base y luego enviarlas a Prolog.

Debe garantizar que la información del nivel sea compatible con los hechos dinámicos de Prolog.

---

### PrologService

Clase encargada de conectar Java con SWI-Prolog.

Responsabilidades:

- Inicializar Prolog.
- Consultar `main.pl`.
- Limpiar estado lógico.
- Cargar hechos dinámicos.
- Consultar rutas.
- Consultar decisiones.
- Consultar coordinación.
- Convertir respuestas Prolog a objetos Java.

Métodos sugeridos:

```java
initialize()
clearState()
loadGameState(GameState state)
EnemyDecision decideEnemyAction(String enemyId)
List<EnemyPlan> getCoordinatedPlan()
List<Position> findRoute(Position start, Position end)
```

---

## Integración con Prolog

Tu proyecto Prolog ya tiene módulos separados para:

- Estado dinámico.
- Tablero.
- Búsqueda DFS con heurística.
- Decisiones individuales.
- Coordinación lógica.
- Pruebas manuales.

Por eso Java debe actuar como cliente del motor lógico.

### Flujo recomendado de integración

1. Java actualiza el estado del juego.
2. Java limpia el estado anterior en Prolog.
3. Java inserta hechos dinámicos actualizados.
4. Java consulta a Prolog.
5. Prolog devuelve acción y ruta.
6. Java convierte la ruta a objetos `Position`.
7. Java mueve el tanque enemigo siguiendo esa ruta.
8. Java envía el nuevo estado al frontend.

---

## Predicados que Java debe consultar

### Para decisiones individuales

```prolog
decidir_accion(TanqueID, Accion, Ruta)
```

Uso desde Java:

- Enviar el ID de un enemigo.
- Recibir acción.
- Recibir ruta.
- Aplicar esa ruta al tanque correspondiente.

---

### Para coordinación lógica

```prolog
plan_coordinado(Plan)
```

Uso desde Java:

- Consultar un plan global para todos los tanques vivos.
- Recibir una lista de elementos tipo `plan(TanqueID, Rol, Ruta)`.
- Convertir cada elemento en un `EnemyPlan`.
- Aplicar roles coordinados en el motor Java.

---

### Para búsqueda directa de rutas

```prolog
buscar_ruta(InicioX, InicioY, DestX, DestY, Ruta)
```

Uso desde Java:

- Calcular rutas específicas cuando el backend lo necesite.
- Validar caminos para enemigos.
- Reutilizar la búsqueda DFS con heurística de Prolog.

---

### Para velocidad de tanques

```prolog
velocidad_movimiento(Tipo, Velocidad)
```

Uso desde Java:

- Consultar o sincronizar la velocidad según el tipo de tanque.
- Ajustar cada cuántos ticks se mueve un enemigo.

---

## Carga dinámica de estado hacia Prolog

Java debe convertir su `GameState` en hechos de Prolog.

Ejemplo conceptual:

```prolog
jugador(j1, X, Y, vivo).
tanque_enemigo(e1, rapido, X, Y, vivo).
objetivo(o1, base, X, Y, activo).
defiende(e1, o1).
muro(X, Y).
vidas_jugador(3).
tamano_tablero(20, 15).
```

Antes de cargar cada estado, Java debe llamar:

```prolog
limpiar_estado.
```

Esto evita residuos de estados anteriores.

---

## Comunicación con el frontend

El backend Java se comunicará con el frontend Vite + TypeScript + Canvas mediante:

### REST

Para acciones que no requieren tiempo real:

```text
POST /game/start
POST /game/restart
POST /game/level/{id}
GET  /game/state
POST /editor/load
POST /editor/save
```

### WebSocket

Para acciones en tiempo real:

```text
/ws/game
```

Mensajes que el frontend puede enviar:

```json
{
  "type": "PLAYER_MOVE",
  "direction": "UP"
}
```

```json
{
  "type": "PLAYER_SHOOT"
}
```

```json
{
  "type": "PAUSE_GAME"
}
```

Mensajes que Java puede enviar:

```json
{
  "type": "GAME_STATE",
  "payload": {
    "level": 1,
    "player": {},
    "enemies": [],
    "bullets": [],
    "walls": [],
    "objectives": []
  }
}
```

---

## Formato JSON compatible con el frontend

El frontend necesita recibir un estado simple y fácil de dibujar.

Ejemplo recomendado:

```json
{
  "level": 1,
  "board": {
    "width": 20,
    "height": 15,
    "tileSize": 32
  },
  "player": {
    "id": "j1",
    "x": 1,
    "y": 1,
    "direction": "RIGHT",
    "lives": 3,
    "status": "ALIVE"
  },
  "enemies": [
    {
      "id": "e1",
      "type": "RAPIDO",
      "x": 16,
      "y": 3,
      "direction": "LEFT",
      "status": "ALIVE",
      "action": "PERSEGUIR_JUGADOR",
      "role": "PERSEGUIDOR"
    }
  ],
  "objectives": [
    {
      "id": "o1",
      "type": "BASE",
      "x": 15,
      "y": 3,
      "status": "ACTIVE"
    }
  ],
  "walls": [
    { "x": 0, "y": 0 }
  ],
  "bullets": [
    {
      "id": "b1",
      "ownerId": "j1",
      "x": 2,
      "y": 1,
      "direction": "RIGHT",
      "active": true
    }
  ],
  "status": {
    "running": true,
    "gameOver": false,
    "levelCompleted": false
  }
}
```

---

## Lógica de tiempo y actualización

Java debe usar un ciclo de actualización controlado.

Opciones:

- `ScheduledExecutorService`
- Timer propio dentro de `GameEngine`
- Loop controlado con ticks por segundo

Recomendación:

```java
ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
executor.scheduleAtFixedRate(gameEngine::update, 0, 50, TimeUnit.MILLISECONDS);
```

Esto daría aproximadamente 20 actualizaciones por segundo.

El frontend puede dibujar a 60 FPS con Canvas, pero solo renderizando el último estado recibido desde Java.

---

## Coordinación lógica para punto extra

El backend debe usar la coordinación de Prolog de forma explícita.

Flujo recomendado:

1. Cada cierto número de ticks, Java llama a `plan_coordinado(Plan)`.
2. Prolog devuelve roles y rutas.
3. Java asigna a cada enemigo su rol actual.
4. Java mueve cada tanque según la ruta recibida.
5. Java incluye el rol en el JSON enviado al frontend.
6. El frontend puede mostrar visualmente el rol o acción actual.

Ejemplo de datos para frontend:

```json
{
  "id": "e3",
  "type": "TACTICO",
  "x": 10,
  "y": 8,
  "action": "EMBOSCAR",
  "role": "BLOQUEADOR"
}
```

Esto ayuda a demostrar el punto extra porque la coordinación no queda escondida, sino que se puede ver durante la ejecución.

---

## Reglas de compatibilidad con el frontend

Para que Java sea compatible con Vite + TypeScript + Canvas:

- Todo estado enviado al frontend debe ir en JSON.
- Las posiciones deben ir como `x` y `y` enteros.
- Los tipos deben enviarse como strings claros.
- Las acciones deben enviarse como strings claros.
- El frontend no debe consultar Prolog.
- El frontend no debe decidir rutas de enemigos.
- El frontend no debe validar colisiones críticas.
- El backend siempre debe ser la fuente de verdad.

---

## Estilo visual vaporwave desde el backend

El backend no dibuja, pero sí debe enviar suficiente información para que el frontend pueda aplicar el estilo vaporwave.

Cada objeto puede incluir campos como:

```json
{
  "id": "e1",
  "type": "RAPIDO",
  "skin": "vaporwave_fast_tank"
}
```

Skins sugeridas:

```text
player_tank_vaporwave
vaporwave_fast_tank
vaporwave_heavy_tank
vaporwave_tactical_tank
vaporwave_base
vaporwave_refinery
vaporwave_wall
vaporwave_bullet
```

Java puede asignar el `skin` según el tipo de objeto, pero el frontend decide cómo se dibuja.

---

## Editor de niveles

El enunciado pide un módulo simple para editar pantallas. Como se usará frontend web, el editor puede implementarse en el frontend y guardar la información en Java.

Java debe exponer endpoints para:

```text
GET  /levels
GET  /levels/{id}
POST /levels
PUT  /levels/{id}
DELETE /levels/{id}
```

El formato del nivel debe incluir:

- Tamaño del tablero.
- Posición inicial del jugador.
- Posiciones de muros.
- Posiciones de objetivos.
- Posiciones de enemigos.
- Tipo de cada enemigo.
- Objetivo que defiende cada enemigo.

---

## Documentación que debe generarse desde esta parte

La parte Java debe documentar:

1. Arquitectura Java-Prolog-Frontend.
2. Diagrama de clases.
3. Descripción de clases principales.
4. Encapsulamiento aplicado.
5. Flujo de comunicación con Prolog.
6. Flujo de comunicación con frontend.
7. Formato JSON usado.
8. Manejo del ciclo del juego.
9. Manejo de colisiones.
10. Manejo de coordinación lógica entre tanques.

---

## Resultado esperado del plan Java

Al terminar esta parte, el proyecto debe tener:

- Backend Java funcional.
- Modelo OO completo.
- Motor de juego controlado por ticks.
- Conexión con SWI-Prolog.
- Consultas a decisiones individuales.
- Consultas a coordinación lógica.
- API REST para acciones generales.
- WebSocket para tiempo real.
- JSON compatible con Vite + TypeScript + Canvas.
- Soporte para estilos visuales vaporwave mediante campos `skin` o `type`.
