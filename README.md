# Tank-Attack

Juego tipo Battle City / Tank-Attack implementado para el
curso de Lenguajes de Programacion. Combina tres paradigmas:

- **Logica** (SWI-Prolog) — busqueda DFS con heuristica,
  decisiones individuales de tanques, coordinacion logica
  (puntos extra).
- **Orientado a objetos** (Java 21 + Javalin) — motor del juego,
  deteccion de colisiones, API REST + WebSocket.
- **Visual / funcional** (TypeScript + Vite + Canvas) —
  renderizado vaporwave, captura de teclado, editor de niveles.

```
Tarea2.txt                          enunciado del proyecto
plan_prolog.md                      plan del modulo Prolog
plan_java_backend.md                plan del backend Java
plan_frontend_vite_typescript_canvas.md  plan del frontend
prolog/   <modulos .pl ya implementados>
backend/  <codigo Java>
frontend/ <codigo TypeScript>
```

## Arquitectura

```
Frontend Vite + TypeScript + Canvas
        |   WebSocket /ws/game
        |   REST /game/*, /levels/*
        v
Backend Java + Javalin
        |   JPL
        v
SWI-Prolog (estado, tablero, busqueda, decisiones, coordinacion)
```

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
2. Mueve al jugador con `WASD` o flechas.
3. Dispara con `Espacio`.
4. Observa el panel **IA de enemigos** a la derecha — se
   actualiza con la accion y rol que Prolog decidio para cada
   tanque.
5. Pausa con `P`, reinicia con `R`, siguiente nivel con `N`.

### 5) Probar el editor de niveles

1. Cambia a la vista **Editor** (boton arriba a la derecha).
2. Selecciona herramienta (Muro, Jugador, Rapido, Pesado,
   Tactico, Base, Refineria, Borrar).
3. Click en las celdas para colocar elementos. Click derecho
   rota la direccion del jugador.
4. Click **Guardar nivel** para enviarlo al backend
   (`POST /levels`).
5. Click **Cargar nivel…** con un ID existente (p.ej.
   `nivel1`).
6. Click **Probar nivel** para cargarlo en la vista de juego.

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
│   └── pruebas.pl
├── backend/                        modulo Java
│   ├── pom.xml
│   ├── README.md
│   ├── src/main/java/com/tankattack/
│   │   ├── Main.java
│   │   ├── LevelFixtures.java
│   │   ├── engine/        GameEngine, PlayerInput, StateListener
│   │   ├── model/         Tank, EnemyTank, Bullet, Objective, ...
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

## API REST y WebSocket (resumen)

REST (todas con `Content-Type: application/json` cuando hay body):

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

**El juego va muy rapido o muy lento**
Ajusta `TICK_PERIOD_MS` en `backend/.../GameEngine.java` y
recompila. `50 ms` da ~20 ticks/segundo.

**Quiero regenerar el fat-jar**
```bash
cd backend && mvn clean package -DskipTests
```
El jar queda en `target/tank-attack-backend.jar`.
