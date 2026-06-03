# Tank-Attack

Juego tipo Battle City / Tank-Attack implementado para el
curso de Lenguajes de Programacion. Combina tres paradigmas
distintos, cada uno ejecutando en su propio proceso:

- **Logica** (SWI-Prolog 9.x) — busqueda DFS con heuristica,
  decisiones individuales de tanques, coordinacion logica
  (puntos extra).
- **Orientado a objetos** (Java 21 + Javalin 5) — motor del
  juego, deteccion de colisiones, API REST + WebSocket, puente
  JPL hacia Prolog.
- **Visual / funcional** (TypeScript + Vite 5 + HTML5 Canvas) —
  renderizado vaporwave, captura de teclado, editor de niveles,
  HUD con indicadores de dano.

```
Tarea2.txt                          enunciado del proyecto
plan_prolog.md                      plan del modulo Prolog
plan_java_backend.md                plan del backend Java
plan_frontend_vite_typescript_canvas.md  plan del frontend
prolog/   <modulos .pl>
backend/  <codigo Java + jar auto-contenido>
frontend/ <codigo TypeScript>
```

---

## Arquitectura del sistema

El sistema es una aplicacion cliente-servidor de tres capas
comunicadas por HTTP/JSON y WebSocket. La capa logica (Prolog)
es un subservicio embebido dentro del backend Java mediante
JPL: el proceso Java lanza el interprete SWI-Prolog en el
mismo espacio de direcciones y se comunica con el por
llamadas nativas. Las tres capas nunca se mezclan en un mismo
lenguaje.

```
                        +---------------------+
                        |    NAVEGADOR WEB    |
                        |  (Vite + TS + DOM)  |
                        +---------------------+
                                  |
                                  |  HTTP (REST)
                                  |  WebSocket /ws/game
                                  v
+----------------------------------------------------------+
|                BACKEND JAVA 21  (Javalin 5)             |
|  +-----------------+   +----------------+   +---------+ |
|  |   ApiServer     |   |  GameEngine    |   |Editor  | |
|  | (REST + WS)     |<->|  (tick manual) |<->|Reposit.| |
|  +-----------------+   +-------+--------+   +---------+ |
|         ^                      |                        |
|         | JPL (nativo)         |  llama                 |
|         v                      v                        |
|  +-------------+        +---------------+               |
|  | PrologSrv   |        | CollisionMgr  |               |
|  | (puente)    |        +---------------+               |
|  +------+------+                                       |
+---------|-----------------------------------------------+
          | JPL native call (mismo proceso)
          v
+----------------------------------------------------------+
|                  SWI-Prolog 9.x                         |
|  +----------+ +-----------+ +-----------+ +----------+ |
|  | estado   | | tablero   | | busqueda  | |decisiones| |
|  +----------+ +-----------+ +-----------+ +----------+ |
|  +---------------+                                       |
|  | coordinacion  |  (puntos extra)                     |
|  +---------------+                                       |
+----------------------------------------------------------+
```

### Responsabilidades por capa

| Capa | Tecnologia | Hace | NO hace |
|------|-----------|------|---------|
| **Presentacion** | TypeScript + Vite + Canvas | Dibujar el tablero, capturar teclado, enviar inputs, editor visual | Calcular rutas, colisiones, decisiones |
| **Motor / API** | Java 21 + Javalin 5 | Mantener el estado del juego, ejecutar ticks, validar colisiones, exponer REST+WS | Buscar rutas por su cuenta; delega a Prolog |
| **Logica** | SWI-Prolog 9 (via JPL) | Calcular rutas (DFS+heuristica), decidir acciones, coordinar tanques | Mover entidades, dibujar, mantener estado mutable |

### Por que esta division

- **El enunciado lo exige**: aplicar conceptos de programacion
  **Logica** (Prolog) y **Orientada a Objetos** (Java) en el
  mismo proyecto, con la busqueda de soluciones residiendo
  en Prolog y siendo invocada desde el lenguaje OO.
- **Rendimiento**: el motor de juego necesita colision,
  movimiento y rendering a tiempo real; Prolog no es apto
  para esto. Prolog es lento por naturaleza pero excelente
  para busqueda declarativa.
- **Aislamiento**: si Prolog no encuentra ruta, el motor
  Java degrada a un fallback determinista sin abortar el
  juego. Ningun fallo de Prolog rompe la partida.

### Flujo de un input del jugador (caso tipico)

```
1. Jugador presiona 'D' en el navegador
2. InputController.ts captura keydown, throttle a 90 ms
3. GameClient.ts envia WS message:
     { "type": "PLAYER_MOVE", "direction": "RIGHT" }
4. ApiServer.java recibe el mensaje, llama engine.handlePlayerInput()
5. GameEngine.handlePlayerInput:
     - aplica el MOVE (mueve al tanque si la celda es valida)
     - ejecuta tick() (1 sola vez)
       a) collisions.updateBullets(state)       // mueve balas
       b) prolog.loadGameState(state)            // assertz hechos
       c) prolog.decideEnemyAction(e1..e3)        // predicados
       d) prolog.getCoordinatedPlan()            // 3 estrategias
       e) advanceEnemies()                       // sigue la ruta
       f) maybeEnemiesShoot()                    // si DISPARAR
       g) notifyListeners()                       // broadcast
6. WsBroadcaster envia { type:"GAME_STATE", payload: ... }
   a TODOS los clientes WS conectados
7. Frontend (GameClient.onState) recibe el snapshot
8. GameRenderer.render() redibuja el canvas
9. Hud.update() actualiza vidas, IA de enemigos, dano
```

### Carga dinamica del estado a Prolog

El enunciado senala: *"siendo la pantalla la que delimita
la cantidad de datos (hechos) que habra en Prolog, su
ingreso a la memoria del interprete debera hacerse de
manera dinamica (assert)."*

`PrologService.loadGameState(GameState state)` se ejecuta
antes de cada consulta relevante y sigue este patron:

```
limpiar_estado.                                  % retractall
assertz(tamano_tablero(W, H)).
assertz(vidas_jugador(N)).
assertz(jugador(j1, X, Y, vivo)).                % jugador
assertz(tanque_enemigo(e1, rapido, X, Y, vivo)).% por cada enemigo
assertz(objetivo(o1, base, X, Y, activo)).        % por cada objetivo
assertz(defiende(e1, o1)).                       % relaciones
assertz(muro(X, Y)).                              % por cada muro
assertz(bala(b1, jugador, X, Y, arriba)).          % por cada bala
```

Esto permite que cada nivel (con diferente distribucion de
muros, enemigos y objetivos) se traduzca en una base de
hechos Prolog fresca sin reiniciar el motor.

### Puente JPL: como se llama Prolog desde Java

`backend/src/main/java/com/tankattack/prolog/PrologService.java`
es el unico punto de contacto con Prolog. Usa JPL
(`org.jpl7`), la API nativa de SWI-Prolog para Java.

**Problema conocido y su workaround**: el parser de JPL
cuenta mal los argumentos cuando la meta contiene variables
y prefijo de modulo (`modulo:predicado(...)` lanza
`more actual params than formal`). Solucion: se consulta
`prolog/jpl_bridge.pl` al arrancar, que reexporta las
predicados al modulo `user`. Las consultas Java usan nombres
planos: `decidir_accion(...)`, `plan_coordinado(...)`,
`buscar_ruta(...)`, `velocidad_movimiento(...)`.

**Construccion del term**: en lugar de construir la consulta
como `Query(String, Term[])`, se construye un `Compound` con
`Atom` y `Variable` y se pasa como `Query(Term)`. Esto evita
el bug de JPL y da acceso explicito a las variables para
leer las respuestas via `q.next().get("Nombre")`.

**Parseo de la respuesta**: las rutas devueltas por Prolog
son listas de coordenadas representadas como `[X, Y]` (listas
de 2 elementos), no tuplas. En JPL se ven como
`isListPair()` cuyo `head` es un entero y `tail` es otro
`isListPair()` cuyo `head` es el segundo entero.

### API REST y WebSocket (resumen)

REST (`Content-Type: application/json` cuando hay body):

| Metodo | Ruta                  | Descripcion                        |
| ------ | --------------------- | ---------------------------------- |
| GET    | `/health`             | healthcheck                        |
| POST   | `/game/start`         | inicia el nivel actual             |
| POST   | `/game/restart`       | reinicia                           |
| POST   | `/game/pause`         | alterna pausa                      |
| POST   | `/game/level/{id}`    | carga y arranca `nivel1..nivel3`   |
| POST   | `/game/next`          | siguiente nivel                    |
| GET    | `/game/state`         | snapshot en JSON                   |
| GET    | `/levels`             | lista resumida                     |
| GET    | `/levels/{id}`        | nivel completo en JSON             |
| POST   | `/levels`             | crea nivel (body = LevelDefinition)|
| PUT    | `/levels/{id}`        | reemplaza                          |
| DELETE | `/levels/{id}`        | elimina                            |

WebSocket `ws://localhost:7070/ws/game`:

- cliente -> servidor: `{type:"PLAYER_MOVE", direction:"UP"}`,
  `{type:"PLAYER_SHOOT"}`, `{type:"PAUSE_GAME"}`,
  `{type:"RESTART"}`, `{type:"LOAD_LEVEL", levelId:"nivel2"}`,
  `{type:"NEXT_LEVEL"}`.
- servidor -> cliente: `{type:"GAME_STATE", payload:{...}}`.

Al conectarse un nuevo cliente WS, `WsBroadcaster` le envia
inmediatamente el ultimo snapshot cacheado para que no
quede esperando al siguiente tick.

---

## Requisitos del sistema

| Herramienta   | Version verificada | Como verificar           |
| ------------- | ------------------ | ------------------------ |
| SWI-Prolog    | 9.2.9 con JPL      | `swipl --version`        |
| JDK           | 21                 | `java -version`          |
| Maven         | 3.9+               | `mvn --version`          |
| Node.js       | 20+ (probado 25.8) | `node --version`         |
| npm           | 11+                | `npm --version`          |

### Instalar dependencias en Ubuntu/Debian

```bash
sudo apt update
sudo apt install -y swi-prolog swi-prolog-jpl openjdk-21-jdk maven nodejs npm
```

`swi-prolog-jpl` aporta `jpl.jar` y `libjpl.so` que el backend
Java necesita. En otras distribuciones:

- Fedora: `sudo dnf install swi-prolog-java java-21-openjdk-devel maven nodejs npm`
- macOS: `brew install swi-prolog openjdk@21 maven node`
- Arch: `sudo pacman -S swi-prolog jdk-openjdk maven nodejs npm`

Si `mvn` no esta en el PATH despues de instalar, cualquier
Maven 3.9+ funciona. En este proyecto se usa uno empaquetado
en `~/.m2/wrapper/`.

## Orden de inicializacion

Hay que arrancar **primero el backend** y luego el frontend
(que espera al backend en `localhost:7070`).

### 1) Probar Prolog (opcional pero recomendado)

```bash
cd prolog
swipl -q -t halt -g "
  consult('main.pl'),
  cargar_estado(nivel1),
  (decidir_accion(e1, A, R) -> format('e1: ~w ruta_len=~w~n', [A, length(R, _)])).
"
```

Deberia imprimir algo como `e1: emboscar ruta_len=17`.

### 2) Backend Java (terminal A)

```bash
cd backend
mvn package
java -jar target/tank-attack-backend.jar
```

Salida esperada:

```
JPL native library configurada en /usr/lib/swi-prolog/lib/x86_64-linux
Iniciando Tank-Attack backend en puerto 7070
JPL native library dir/path = /usr/lib/swi-prolog/lib/x86_64-linux
Prolog inicializado. Modulos cargados desde /tmp/...
Javalin started in 100ms \o/
Servidor Javalin escuchando en puerto 7070
Backend listo. Nivel inicial: nivel1
```

Pruebas rapidas en otra terminal:

```bash
curl http://localhost:7070/health
curl http://localhost:7070/game/state | head -c 200
curl -X POST http://localhost:7070/game/start
```

Si JPL no encuentra `libjpl.so`, arranca con
`-Djpl.library.path=/usr/lib/swi-prolog/lib/x86_64-linux`
(la ruta puede variar segun el SO; ver `find / -name libjpl.so`).

Para reiniciar con otro nivel:

```bash
curl -X POST http://localhost:7070/game/level/nivel2
```

### 3) Frontend Vite (terminal B)

```bash
cd frontend
npm install
npm run dev
```

Salida esperada:

```
VITE v5.4.21  ready in 83 ms
Local:   http://127.0.0.1:5173/
```

Abre `http://127.0.0.1:5173/` en el navegador. La HUD
aparece vacia hasta que el WebSocket se conecta al backend;
la esquina inferior derecha muestra `WS: connected` cuando
lo logra.

### 4) Probar el juego

1. Click en **Iniciar partida** (overlay) o envia `R` desde
   el backend: `curl -X POST http://localhost:7070/game/restart`.
2. Mueve al jugador con `WASD` o flechas. **Cada movimiento
   tuyo dispara un tick**: los tanques enemigos solo se
   mueven cuando vos te moves (tick manual por accion).
3. Dispara con `Espacio`.
4. Observa el panel **IA de enemigos** a la derecha — se
   actualiza con la accion y rol que Prolog decidio para cada
   tanque.
5. Pausa con `P` (siempre funciona, incluso pausado),
   reinicia con `R`, siguiente nivel con `N`.
6. Cuando un enemigo te impacta: flash rojo en el canvas,
   card "DAÑO RECIBIDO" en el HUD y toast rojo.

### 5) Probar el editor de niveles

1. Click en **Editar nivel** (boton arriba a la derecha).
   El editor aparece como vista a pantalla completa.
2. Selecciona herramienta (Muro, Jugador, Rapido, Pesado,
   Tactico, Base, Refineria, Borrar).
3. Click en las celdas para colocar elementos. Click derecho
   rota la direccion del jugador.
4. Click **Guardar nivel** para enviarlo al backend
   (`POST /levels`).
5. Click **Cargar nivel…** con un ID existente (p.ej.
   `nivel1`).
6. Click **Probar nivel** para cargarlo en la vista de juego.
7. Pulsa **Escape** o **Cerrar** para volver al juego.

## Build de produccion

```bash
cd backend  && mvn package -DskipTests && ls target/tank-attack-backend.jar
cd frontend && npm run build        && ls dist/
```

El frontend empaquetado (carpeta `dist/`) se puede servir con
cualquier servidor estatico. Para un build de un solo jar
que tambien sirva el frontend, hacer un `cat dist/* > ...` no
vale porque Java no es un servidor estatico. Recomendado:
ejecutar backend y frontend por separado, o montar un reverse
proxy simple.

## Estructura completa

```
tankattack/
├── README.md                       <-- este archivo
├── Tarea2.txt                      enunciado
├── plan_*.md                       planes de los 3 modulos
├── prolog/                         modulo Prolog (listo)
│   ├── main.pl
│   ├── estado.pl
│   ├── tablero.pl
│   ├── busqueda.pl
│   ├── decisiones.pl
│   ├── coordinacion.pl
│   ├── pruebas.pl
│   └── jpl_bridge.pl               reexporta a modulo 'user' para JPL
├── backend/                        modulo Java
│   ├── pom.xml
│   ├── README.md
│   ├── src/main/java/com/tankattack/
│   │   ├── Main.java
│   │   ├── LevelFixtures.java
│   │   ├── engine/        GameEngine, PlayerInput, StateListener
│   │   ├── model/         Position, Tank, EnemyTank, Bullet, Objective, ...
│   │   ├── collision/     CollisionManager
│   │   ├── prolog/        PrologService (puente JPL)
│   │   ├── level/         LevelLoader (JSON)
│   │   ├── editor/        LevelRepository (en memoria)
│   │   └── api/           ApiServer, WsBroadcaster, DTOs
│   └── src/main/resources/prolog/  copia local de los .pl
└── frontend/                      modulo TypeScript
    ├── package.json
    ├── tsconfig.json
    ├── vite.config.ts
    ├── index.html
    ├── README.md
    └── src/
        ├── main.ts
        ├── styles/main.css
        ├── game/  types, GameClient, Renderer, AssetManager, Camera, Input
        ├── editor/  LevelEditor, LevelSerializer
        └── ui/   Hud
```

## Solucion de problemas

**`UnsatisfiedLinkError: no jpl in java.library.path`**
JPL no encuentra `libjpl.so`. Instala `swi-prolog-jpl` y/o
exporta `LD_LIBRARY_PATH=/usr/lib/swi-prolog/lib/x86_64-linux`
antes de `java -jar`.

**`Error: spawn EACCES` o falta Maven**
Instala Maven o usa uno portable:
`./mvnw package` (no se incluye Maven Wrapper en este repo).

**`npm install` falla por red**
Configura el proxy o el registro segun tu entorno.

**Frontend no se conecta al backend**
- Verifica que el backend esta corriendo: `curl http://localhost:7070/health`.
- Mira la esquina inferior derecha del HUD: dice `WS: connecting`,
  `connected` o `disconnected`.
- Si el backend esta en otro host, define `VITE_BACKEND_URL` y
  `VITE_WS_URL` al arrancar Vite, o modifica el constructor de
  `GameClient` en `frontend/src/game/GameClient.ts`.

**Quiero regenerar el fat-jar**
```bash
cd backend && mvn clean package -DskipTests
```
El jar queda en `target/tank-attack-backend.jar`.

**Ver y matar el proceso del backend**
```bash
ps -ef | grep "tank-attack-backend" | grep -v grep
kill <PID>                  # SIGTERM
# o
pkill -f tank-attack-backend.jar
```
