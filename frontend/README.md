# Tank-Attack — Frontend Vite + TypeScript + Canvas

Frontend del proyecto Tank-Attack. Capa visual e interactiva
que se comunica con el backend Java por REST y WebSocket.

## Stack

- **Vite 5** — build/dev server
- **TypeScript 5** — tipado estricto
- **HTML5 Canvas 2D** — render del juego (sin imagenes externas)
- **CSS** — tema vaporwave (paleta neon, glow, gradientes)

## Estructura

```
frontend/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── src/
    ├── main.ts                  # punto de entrada
    ├── styles/main.css          # tema vaporwave
    ├── game/
    │   ├── types.ts             # tipos que coinciden con los DTO Java
    │   ├── GameClient.ts        # REST + WebSocket
    │   ├── GameRenderer.ts      # dibujado en Canvas
    │   ├── AssetManager.ts      # sprites procedurales
    │   ├── Camera.ts            # escalado y viewport
    │   └── InputController.ts   # captura de teclado
    ├── editor/
    │   ├── LevelEditor.ts       # edicion visual de niveles
    │   └── LevelSerializer.ts   # EditorState <-> LevelDefinition
    └── ui/
        └── Hud.ts               # HUD vaporwave (DOM)
```

## Requisitos

- Node.js 20+ (probado con 25.x)
- npm 11+
- Backend Java corriendo en `http://localhost:7070`

## Instalar y arrancar

```bash
cd frontend
npm install
npm run dev
```

Vite sirve la app en `http://127.0.0.1:5173/`.

## Build de produccion

```bash
npm run build       # genera dist/
npm run preview     # sirve dist/ en :5173
```

## Comunicacion con el backend

- **REST** (via fetch): `POST /game/start`, `POST /game/restart`,
  `POST /game/pause`, `POST /game/level/{id}`, `GET /game/state`,
  `GET|POST|PUT|DELETE /levels[/...]`.
- **WebSocket** `ws://localhost:7070/ws/game`:
  - Cliente -> servidor: `{type: "PLAYER_MOVE", direction: "UP"}`,
    `{type: "PLAYER_SHOOT"}`, `{type: "PAUSE_GAME"}`,
    `{type: "RESTART"}`, `{type: "LOAD_LEVEL", levelId: "nivel2"}`,
    `{type: "NEXT_LEVEL"}`.
  - Servidor -> cliente: `{type: "GAME_STATE", payload: GameState}`.
- `GameClient` reconecta automaticamente con backoff exponencial
  si el WebSocket se cae.

## Controles

| Tecla              | Accion            |
| ------------------ | ----------------- |
| `W` / `↑`          | mover arriba      |
| `S` / `↓`          | mover abajo       |
| `A` / `←`          | mover izquierda   |
| `D` / `→`          | mover derecha     |
| `Espacio`          | disparar          |
| `P`                | pausa             |
| `R`                | reiniciar nivel   |
| `N`                | siguiente nivel   |

## Caracteristicas

- **Render Canvas**: cada celda del tablero se dibuja segun
  `tileSize` calculado por `Camera` para ajustarse al viewport.
- **Sprites procedurales**: en `AssetManager.ts` se generan todos
  los elementos (tanques, muros, objetivos, balas) con primitivas
  de Canvas. No hay imagenes externas.
- **Estilo vaporwave**: gradiente morado-azul, reticula neon,
  glow alrededor de los tanques, paleta morado/rosa/celeste/
  amarillo. Definido en `styles/main.css`.
- **HUD**: muestra vidas, nivel, objetivos restantes, enemigos
  vivos, estado de partida y un panel lateral con la accion y
  rol actual de cada enemigo (demuestra la coordinacion logica
  del backend).
- **Editor de niveles**: permite colocar muros, jugador, tanques
  (rapido/pesado/tactico), objetivos (base/refineria) y borrar.
  El clic derecho rota la direccion del jugador. Los niveles se
  guardan/cargan en el backend via `POST /levels` y
  `GET /levels/{id}`.

## Compatibilidad con el backend

- El frontend **NO** calcula decisiones de enemigos, rutas DFS,
  coordinacion logica ni colisiones. Solo dibuja lo que el
  backend envia y envia las pulsaciones del teclado como
  intenciones.
- Los tipos TypeScript (`types.ts`) coinciden con los DTO Java
  (`com.tankattack.api.GameStateDto`). Si el backend anade un
  campo nuevo, el tipo TypeScript lo reflejara en el siguiente
  build (con `?` opcional o nuevo campo).
- Las posiciones se envian como enteros `(x, y)` y se dibujan
  con `x * tileSize`, `y * tileSize`.

## Variables de entorno

- `VITE_BACKEND_URL` — base URL del backend (default
  `http://localhost:7070`).
- `VITE_WS_URL` — URL del WebSocket (default
  `ws://localhost:7070/ws/game`).

## Scripts npm

- `npm run dev` — servidor de desarrollo con HMR.
- `npm run build` — typecheck + build de produccion.
- `npm run preview` — sirve el build de produccion.
- `npm run typecheck` — solo typecheck.
