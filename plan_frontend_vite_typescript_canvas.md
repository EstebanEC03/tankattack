# Plan de Frontend para Tank-Attack

## Stack definido

El frontend del juego se implementará con:

- **Vite** como herramienta de desarrollo y construcción.
- **TypeScript** como lenguaje principal del frontend.
- **HTML5 Canvas** para dibujar el juego 2D.
- **CSS** para menús, HUD, botones y estilo visual.
- **WebSocket** para recibir el estado del juego en tiempo real desde Java.
- **Fetch / REST** para iniciar partida, reiniciar, cargar niveles y guardar mapas.

Este frontend será compatible con el backend Java porque toda la comunicación se hará mediante JSON usando REST y WebSocket.

---

## Objetivo del frontend

El frontend será la capa visual e interactiva del juego. Su objetivo no es manejar la lógica principal, sino:

- Dibujar el tablero.
- Dibujar tanques, objetivos, muros y balas.
- Mostrar vidas, nivel actual, estado de la partida y mensajes.
- Capturar teclas del jugador.
- Enviar acciones al backend Java.
- Recibir el estado actualizado del juego.
- Renderizar el estado recibido en Canvas.
- Implementar un editor visual o semivisual de niveles.
- Aplicar un estilo visual vaporwave a tanques, objetivos y elementos principales.

El frontend no debe calcular decisiones de enemigos, rutas DFS, coordinación lógica ni colisiones principales. Todo eso debe permanecer en Java y Prolog.

---

## Arquitectura compatible con Java

La arquitectura será:

```text
Vite + TypeScript + Canvas
        |
        | REST
        | WebSocket
        v
Java Backend + Javalin
        |
        v
SWI-Prolog
```

El frontend solo conoce el estado que Java le envía. Java será la fuente de verdad del juego.

---

## Estructura recomendada del proyecto frontend

```text
frontend/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── src/
│   ├── main.ts
│   ├── game/
│   │   ├── GameClient.ts
│   │   ├── GameRenderer.ts
│   │   ├── InputController.ts
│   │   ├── AssetManager.ts
│   │   ├── Camera.ts
│   │   └── types.ts
│   ├── editor/
│   │   ├── LevelEditor.ts
│   │   └── LevelSerializer.ts
│   ├── ui/
│   │   ├── Hud.ts
│   │   ├── Menu.ts
│   │   └── Screens.ts
│   ├── styles/
│   │   └── main.css
│   └── assets/
│       ├── tanks/
│       ├── objectives/
│       ├── walls/
│       └── effects/
```

---

## Responsabilidades principales

### GameClient

Clase encargada de comunicarse con Java.

Responsabilidades:

- Abrir conexión WebSocket.
- Recibir mensajes `GAME_STATE`.
- Enviar acciones del jugador.
- Llamar endpoints REST para iniciar, reiniciar o cargar niveles.
- Mantener el último estado recibido.

Métodos sugeridos:

```ts
connect(): void
startGame(): Promise<void>
restartGame(): Promise<void>
loadLevel(level: number): Promise<void>
sendPlayerMove(direction: Direction): void
sendPlayerShoot(): void
onGameState(callback: (state: GameState) => void): void
```

---

### GameRenderer

Clase encargada de dibujar el juego en Canvas.

Responsabilidades:

- Limpiar el canvas.
- Dibujar fondo vaporwave.
- Dibujar grilla del tablero.
- Dibujar muros.
- Dibujar objetivos.
- Dibujar jugador.
- Dibujar enemigos.
- Dibujar balas.
- Dibujar efectos visuales.

Métodos sugeridos:

```ts
render(state: GameState): void
drawBoard(state: GameState): void
drawPlayer(player: PlayerTank): void
drawEnemy(enemy: EnemyTank): void
drawObjective(objective: Objective): void
drawWall(wall: Wall): void
drawBullet(bullet: Bullet): void
```

---

### InputController

Clase encargada de capturar el teclado.

Controles sugeridos:

```text
W / ArrowUp: mover arriba
S / ArrowDown: mover abajo
A / ArrowLeft: mover izquierda
D / ArrowRight: mover derecha
Space: disparar
P: pausar
R: reiniciar
```

El frontend no mueve directamente al jugador. Solo envía la intención al backend:

```json
{
  "type": "PLAYER_MOVE",
  "direction": "UP"
}
```

---

### AssetManager

Clase encargada de cargar imágenes o dibujar sprites generados por Canvas.

Puede manejar:

- Sprites de tanques vaporwave.
- Objetivos vaporwave.
- Muros neón.
- Balas brillantes.
- Explosiones simples.
- Fondo con gradiente.

---

### Hud

Clase o módulo encargado de mostrar información encima o al lado del canvas.

Debe mostrar:

- Vidas del jugador.
- Nivel actual.
- Objetivos restantes.
- Estado del juego.
- Acción actual de los enemigos si se quiere demostrar la IA.
- Roles coordinados de los tanques para evidenciar el punto extra.

Ejemplo visual:

```text
VIDAS: 3
NIVEL: 1
OBJETIVOS: 2
ENEMIGO e1: PERSEGUIDOR
ENEMIGO e2: DEFENSOR
ENEMIGO e3: BLOQUEADOR
```

---

## Tipos TypeScript compatibles con Java

El frontend debe definir tipos que coincidan con el JSON enviado por Java.

Ejemplo:

```ts
export type Direction = 'UP' | 'DOWN' | 'LEFT' | 'RIGHT';
export type EnemyType = 'RAPIDO' | 'PESADO' | 'TACTICO';
export type EnemyAction =
  | 'DISPARAR'
  | 'DEFENDER_OBJETIVO'
  | 'RETROCEDER'
  | 'PERSEGUIR_JUGADOR'
  | 'EMBOSCAR'
  | 'PATRULLAR'
  | 'ESPERAR';

export interface Position {
  x: number;
  y: number;
}

export interface Board {
  width: number;
  height: number;
  tileSize: number;
}

export interface PlayerTank {
  id: string;
  x: number;
  y: number;
  direction: Direction;
  lives: number;
  status: 'ALIVE' | 'DESTROYED';
  skin?: string;
}

export interface EnemyTank {
  id: string;
  type: EnemyType;
  x: number;
  y: number;
  direction: Direction;
  status: 'ALIVE' | 'DESTROYED';
  action?: EnemyAction;
  role?: string;
  skin?: string;
}

export interface Objective {
  id: string;
  type: 'BASE' | 'REFINERIA';
  x: number;
  y: number;
  status: 'ACTIVE' | 'DESTROYED';
  skin?: string;
}

export interface Wall {
  x: number;
  y: number;
  skin?: string;
}

export interface Bullet {
  id: string;
  ownerId: string;
  x: number;
  y: number;
  direction: Direction;
  active: boolean;
  skin?: string;
}

export interface GameState {
  level: number;
  board: Board;
  player: PlayerTank;
  enemies: EnemyTank[];
  objectives: Objective[];
  walls: Wall[];
  bullets: Bullet[];
  status: {
    running: boolean;
    gameOver: boolean;
    levelCompleted: boolean;
  };
}
```

---

## Comunicación REST con Java

El frontend debe usar `fetch` para acciones generales.

Endpoints esperados:

```text
POST /game/start
POST /game/restart
POST /game/level/{id}
GET  /game/state
GET  /levels
GET  /levels/{id}
POST /levels
PUT  /levels/{id}
DELETE /levels/{id}
```

Ejemplo en TypeScript:

```ts
await fetch('http://localhost:7070/game/start', {
  method: 'POST'
});
```

---

## Comunicación WebSocket con Java

El WebSocket se usará para comunicación en tiempo real.

URL recomendada:

```text
ws://localhost:7070/ws/game
```

Mensajes enviados por el frontend:

```json
{
  "type": "PLAYER_MOVE",
  "direction": "LEFT"
}
```

```json
{
  "type": "PLAYER_SHOOT"
}
```

Mensajes recibidos desde Java:

```json
{
  "type": "GAME_STATE",
  "payload": {
    "level": 1,
    "board": {
      "width": 20,
      "height": 15,
      "tileSize": 32
    },
    "player": {},
    "enemies": [],
    "objectives": [],
    "walls": [],
    "bullets": []
  }
}
```

---

## Renderizado con Canvas

Canvas debe dibujar el estado recibido desde Java.

Reglas:

- Cada celda del tablero se dibuja según `tileSize`.
- Una posición `(x, y)` se convierte a píxeles así:

```ts
const px = x * tileSize;
const py = y * tileSize;
```

- El frontend no debe inventar posiciones.
- Si Java envía una entidad en `(5, 3)`, Canvas la dibuja en esa celda.

---

## Estilo visual vaporwave

El juego debe tener una estética vaporwave, especialmente en:

- Tanques.
- Objetivos.
- Balas.
- Fondo.
- HUD.
- Menús.

### Paleta sugerida

```text
Fondo oscuro: #120027
Morado neón: #7b2cff
Rosado neón: #ff4fd8
Celeste neón: #00e5ff
Amarillo neón: #ffe066
Blanco brillante: #f8f8ff
```

### Fondo

El fondo puede tener:

- Gradiente oscuro morado/azul.
- Líneas de grilla neón.
- Brillo suave detrás del tablero.
- Estética retro-futurista.

### Tanque del jugador

Estilo sugerido:

- Cuerpo principal celeste neón.
- Bordes rosados.
- Cañón blanco o celeste brillante.
- Sombra/glow alrededor.

Skin sugerida:

```text
player_tank_vaporwave
```

### Tanque rápido

Estilo sugerido:

- Forma más pequeña o alargada.
- Color rosado neón.
- Líneas de velocidad.
- Brillo intenso.

Skin:

```text
vaporwave_fast_tank
```

### Tanque pesado

Estilo sugerido:

- Forma más grande.
- Color morado oscuro con bordes amarillos.
- Sensación de blindaje fuerte.
- Glow más grueso.

Skin:

```text
vaporwave_heavy_tank
```

### Tanque táctico

Estilo sugerido:

- Color azul/morado.
- Detalles geométricos.
- Pequeñas líneas tipo circuito.
- Apariencia más tecnológica.

Skin:

```text
vaporwave_tactical_tank
```

### Objetivo tipo base

Estilo sugerido:

- Estructura geométrica neón.
- Forma de torre o núcleo.
- Centro brillante.
- Color rosado/morado.

Skin:

```text
vaporwave_base
```

### Objetivo tipo refinería

Estilo sugerido:

- Depósitos o cilindros simples.
- Líneas celestes.
- Brillo amarillo/rosado.
- Apariencia industrial retro-futurista.

Skin:

```text
vaporwave_refinery
```

### Muros

Estilo sugerido:

- Bloques oscuros.
- Bordes morados o celestes.
- Textura tipo ladrillo neón.

Skin:

```text
vaporwave_wall
```

### Balas

Estilo sugerido:

- Bolitas o rayos pequeños.
- Color amarillo, rosado o celeste.
- Glow fuerte.
- Estela corta.

Skin:

```text
vaporwave_bullet
```

---

## Diseño con Canvas sin imágenes externas

Para simplificar la entrega, los tanques y objetivos pueden dibujarse directamente con Canvas en vez de usar imágenes.

Ventajas:

- No se depende de archivos externos.
- Se puede explicar mejor en la documentación.
- Se mantiene todo dentro del código.
- Es más fácil cambiar colores y estilos.

Ejemplo conceptual:

```ts
function drawVaporwaveTank(ctx: CanvasRenderingContext2D, x: number, y: number, size: number) {
  ctx.shadowBlur = 15;
  ctx.shadowColor = '#ff4fd8';
  ctx.fillStyle = '#00e5ff';
  ctx.fillRect(x + 6, y + 10, size - 12, size - 20);

  ctx.fillStyle = '#ff4fd8';
  ctx.fillRect(x + size / 2 - 3, y + 2, 6, 18);

  ctx.shadowBlur = 0;
}
```

---

## Editor de niveles en frontend

El editor puede ser simple y compatible con el enunciado.

Opciones de edición:

- Seleccionar herramienta:
  - Muro
  - Jugador
  - Tanque rápido
  - Tanque pesado
  - Tanque táctico
  - Objetivo base
  - Objetivo refinería
  - Borrar
- Hacer clic en una celda del tablero.
- Guardar el nivel.
- Cargar el nivel.

El editor enviará a Java un JSON con el mapa. Java validará y guardará el nivel.

---

## HUD recomendado

El HUD debe seguir el estilo vaporwave.

Debe incluir:

- Vidas del jugador.
- Nivel actual.
- Objetivos restantes.
- Estado de partida.
- Acción de enemigos.
- Roles coordinados.

Esto ayuda a demostrar que el backend está usando Prolog para la toma de decisiones y coordinación.

---

## Compatibilidad obligatoria con el backend

Para mantener ambos planes compatibles:

- El frontend solo envía acciones.
- Java decide si las acciones son válidas.
- Java actualiza el estado.
- Java consulta Prolog.
- Java devuelve el estado final al frontend.
- El frontend dibuja exactamente lo que recibe.
- Los tipos TypeScript deben coincidir con los DTO de Java.
- Las rutas y decisiones de enemigos nunca se calculan en TypeScript.

---

## Resultado esperado del plan frontend

Al terminar esta parte, el proyecto debe tener:

- Frontend creado con Vite.
- Código en TypeScript.
- Canvas funcional.
- Comunicación REST con Java.
- Comunicación WebSocket con Java.
- Renderizado del tablero.
- Renderizado de tanques, objetivos, muros y balas.
- Estilo vaporwave aplicado.
- HUD funcional.
- Editor simple de niveles.
- Compatibilidad total con el JSON enviado por Java.
