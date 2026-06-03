# Tank-Attack — Backend Java

Backend del proyecto Tank-Attack construido con Java 21, Javalin 5 y
SWI-Prolog a traves de JPL.

## Arquitectura

```
Frontend Vite + TypeScript + Canvas
        |   WebSocket /ws/game
        |   REST /game/*, /levels/*
        v
Backend Java + Javalin
   |  - com.tankattack.engine.GameEngine (loop a 20 ticks/seg)
   |  - com.tankattack.prolog.PrologService (JPL)
   |  - com.tankattack.editor.LevelRepository
   v
SWI-Prolog (modulos: estado, tablero, busqueda, decisiones, coordinacion)
```

## Requisitos

- JDK 21
- Maven 3.9+
- SWI-Prolog 9.x con JPL habilitado

## Compilar

```bash
cd backend
mvn package
```

El uber-jar queda en `target/tank-attack-backend.jar` (~8 MB). Incluye
JPL desempaquetado, Jetty, Jackson, Kotlin stdlib y los .pl de
`src/main/resources/prolog/`.

## Ejecutar

```bash
java -jar target/tank-attack-backend.jar
```

Propiedades del sistema (opcionales):
- `-Dtank.port=7070` — puerto HTTP (default `7070`).
- `-Dtank.level=nivel1` — nivel inicial (default `nivel1`).
- `-Djpl.library.path=/usr/lib/swi-prolog/lib/x86_64-linux` — ruta de
  la libreria nativa. Si se omite, `Main` intenta ubicarla en rutas
  comunes de Linux/macOS/Windows.

## Endpoints REST

| Metodo | Ruta                  | Descripcion                                |
| ------ | --------------------- | ------------------------------------------ |
| GET    | `/`                   | Info del backend                           |
| GET    | `/health`             | Healthcheck                                |
| POST   | `/game/start`         | Inicia la partida del nivel actual         |
| POST   | `/game/restart`       | Reinicia el nivel actual                   |
| POST   | `/game/pause`         | Alterna pausa                              |
| POST   | `/game/level/{id}`    | Carga y arranca un nivel (nivel1..3)       |
| POST   | `/game/next`          | Avanza al siguiente nivel                  |
| GET    | `/game/state`         | Snapshot del estado en JSON                |
| GET    | `/levels`             | Lista de niveles disponibles               |
| GET    | `/levels/{id}`        | JSON detallado de un nivel                 |
| POST   | `/levels`             | Crea un nivel (body = JSON del nivel)      |
| PUT    | `/levels/{id}`        | Sobrescribe un nivel                       |
| DELETE | `/levels/{id}`        | Elimina un nivel personalizado             |

## WebSocket `/ws/game`

Cliente -> servidor (JSON):

```json
{ "type": "PLAYER_MOVE", "direction": "UP" }
{ "type": "PLAYER_SHOOT" }
{ "type": "PAUSE_GAME" }
{ "type": "RESTART" }
{ "type": "LOAD_LEVEL", "levelId": "nivel2" }
{ "type": "NEXT_LEVEL" }
```

Servidor -> cliente (JSON):

```json
{
  "type": "GAME_STATE",
  "payload": {
    "level": 1,
    "board": { "width": 20, "height": 15, "tileSize": 32 },
    "player": { "id": "j1", "x": 1, "y": 1, "direction": "RIGHT", "lives": 3, ... },
    "enemies": [ ... ],
    "objectives": [ ... ],
    "walls": [ ... ],
    "bullets": [ ... ],
    "status": { "running": true, "paused": false, "tick": 42, ... }
  }
}
```

## Estructura de directorios

```
backend/
├── pom.xml
├── src/main/java/com/tankattack/
│   ├── Main.java                    # punto de entrada
│   ├── LevelFixtures.java           # niveles predefinidos
│   ├── engine/                      # GameEngine, PlayerInput, StateListener
│   ├── model/                       # Position, Direction, Tank, ...
│   ├── collision/CollisionManager.java
│   ├── prolog/PrologService.java    # puente JPL
│   ├── level/LevelLoader.java       # nivel <-> JSON
│   ├── editor/LevelRepository.java  # CRUD en memoria
│   └── api/                         # ApiServer, WsBroadcaster, DTOs
└── src/main/resources/prolog/       # .pl copiados al classpath
    ├── main.pl
    ├── estado.pl
    ├── tablero.pl
    ├── busqueda.pl
    ├── decisiones.pl
    ├── coordinacion.pl
    ├── pruebas.pl
    └── jpl_bridge.pl   # reexporta predicados al modulo 'user' para JPL
```

## Ciclo del juego

1. **Tick** (`TICK_PERIOD_MS = 50`, 20 Hz) ejecutado por
   `ScheduledExecutorService`.
2. Cada tick:
   - Avanza balas y resuelve colisiones.
   - Cada `ENEMY_DECISION_INTERVAL = 6` ticks: vuelca el estado a
     Prolog, consulta `decidir_accion/3` por cada enemigo, aplica
     accion y ruta.
   - Cada `COORDINATION_INTERVAL = 30` ticks: consulta
     `plan_coordinado/1` para asignar roles (punto extra).
   - Mueve los enemigos siguiendo la ruta actual.
   - Si la accion es `DISPARAR` y el enemigo esta alineado con el
     jugador, dispara una bala.
   - Verifica fin de nivel / game over.
   - Notifica a los listeners (WebSocket).

## Encapsulamiento

- Atributos de las clases `Tank`, `EnemyTank`, `PlayerTank`,
  `Objective`, `Bullet`, `Wall` son `protected`/`private` y se
  exponen por getters/setters publicos.
- Los enums son inmutables.
- `PrologService` mantiene un `ReentrantLock` que serializa todas
  las operaciones de JPL (no thread-safe).

## Dependencias de Prolog

- `jpl_bridge.pl` se consulta al iniciar y reexporta las predicados
  de los modulos del proyecto al modulo `user` (el modulo por
  defecto de JPL). Esto evita el problema del parser de JPL con
  goals con prefijo `modulo:predicado(...)`.
- El backend NO duplica la logica de busqueda ni de decision: toda
  decision de enemigo se delega a Prolog.
