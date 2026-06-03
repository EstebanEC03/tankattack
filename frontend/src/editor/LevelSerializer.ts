/* ============================================================
 * LevelSerializer.ts
 * Convierte entre el modelo interno del editor y el
 * LevelDefinition JSON que espera el backend Java.
 * ============================================================ */

import type {
  Direction,
  EditorTool,
  EnemyType,
  LevelDefinition,
  ObjectiveType,
  Position,
} from "../game/types";

export interface EditorCell {
  walls: Set<string>;
  player?: { direction: Direction };
  enemies: Array<{
    id: string;
    type: EnemyType;
    direction: Direction;
    speed: number;
    defends?: string | null;
  }>;
  objectives: Array<{ id: string; type: ObjectiveType }>;
}

export interface EditorState {
  id: string;
  width: number;
  height: number;
  tileSize: number;
  playerLives: number;
  cells: Map<string, EditorCell>;
  playerStart: Position;
  playerDirection: Direction;
}

function key(x: number, y: number): string {
  return `${x},${y}`;
}

export function createEmptyState(
  id: string,
  width: number,
  height: number
): EditorState {
  const cells = new Map<string, EditorCell>();
  for (let x = 0; x < width; x++) {
    for (let y = 0; y < height; y++) {
      cells.set(key(x, y), {
        walls: new Set(),
        enemies: [],
        objectives: [],
      });
    }
  }
  return {
    id,
    width,
    height,
    tileSize: 32,
    playerLives: 3,
    cells,
    playerStart: { x: 1, y: 1 },
    playerDirection: "RIGHT",
  };
}

export function resizeState(
  state: EditorState,
  width: number,
  height: number
): EditorState {
  const cells = new Map<string, EditorCell>();
  for (let x = 0; x < width; x++) {
    for (let y = 0; y < height; y++) {
      const k = key(x, y);
      cells.set(k, state.cells.get(k) ?? {
        walls: new Set(),
        enemies: [],
        objectives: [],
      });
    }
  }
  // Si el jugador queda fuera, lo movemos al centro.
  let playerStart = state.playerStart;
  if (playerStart.x >= width || playerStart.y >= height) {
    playerStart = { x: 1, y: 1 };
  }
  return { ...state, width, height, cells, playerStart };
}

export function applyTool(
  state: EditorState,
  x: number,
  y: number,
  tool: EditorTool
): EditorState {
  if (x < 0 || y < 0 || x >= state.width || y >= state.height) return state;
  const k = key(x, y);
  const cell = state.cells.get(k);
  if (!cell) return state;
  const next: EditorState = {
    ...state,
    cells: new Map(state.cells),
  };
  const newCell: EditorCell = {
    walls: new Set(cell.walls),
    enemies: [...cell.enemies],
    objectives: [...cell.objectives],
  };
  next.cells.set(k, newCell);

  switch (tool) {
    case "WALL":
      if (newCell.walls.has("wall")) {
        newCell.walls.delete("wall");
      } else {
        newCell.walls.add("wall");
      }
      break;
    case "ERASE":
      newCell.walls.clear();
      newCell.enemies = [];
      newCell.objectives = [];
      break;
    case "PLAYER":
      next.playerStart = { x, y };
      next.playerDirection = newCell.player?.direction ?? "RIGHT";
      newCell.player = { direction: next.playerDirection };
      break;
    case "ENEMY_RAPIDO":
    case "ENEMY_PESADO":
    case "ENEMY_TACTICO": {
      const type: EnemyType = tool === "ENEMY_RAPIDO" ? "RAPIDO"
        : tool === "ENEMY_PESADO" ? "PESADO" : "TACTICO";
      const speed = type === "RAPIDO" ? 4 : type === "PESADO" ? 2 : 3;
      newCell.enemies.push({
        id: `e${newCell.enemies.length + 1}-${x}-${y}`,
        type,
        direction: "LEFT",
        speed,
      });
      break;
    }
    case "OBJECTIVE_BASE":
    case "OBJECTIVE_REFINERIA": {
      const type: ObjectiveType = tool === "OBJECTIVE_BASE" ? "BASE" : "REFINERIA";
      newCell.objectives.push({
        id: `o${newCell.objectives.length + 1}-${x}-${y}`,
        type,
      });
      break;
    }
  }
  return next;
}

export function toLevelDefinition(state: EditorState): LevelDefinition {
  const walls: Position[] = [];
  const objectives: LevelDefinition["objectives"] = [];
  const enemies: LevelDefinition["enemies"] = [];

  let enemyCounter = 0;
  let objectiveCounter = 0;

  for (let y = 0; y < state.height; y++) {
    for (let x = 0; x < state.width; x++) {
      const cell = state.cells.get(key(x, y));
      if (!cell) continue;
      if (cell.walls.has("wall")) walls.push({ x, y });
      for (const o of cell.objectives) {
        objectiveCounter++;
        objectives.push({
          id: o.id || `o${objectiveCounter}`,
          type: o.type,
          x,
          y,
        });
      }
      for (const e of cell.enemies) {
        enemyCounter++;
        enemies.push({
          id: e.id || `e${enemyCounter}`,
          type: e.type,
          x,
          y,
          direction: e.direction,
          speed: e.speed,
          defends: e.defends ?? null,
        });
      }
    }
  }

  return {
    id: state.id,
    width: state.width,
    height: state.height,
    tileSize: state.tileSize,
    playerLives: state.playerLives,
    playerStart: {
      x: state.playerStart.x,
      y: state.playerStart.y,
      direction: state.playerDirection,
    },
    walls,
    objectives,
    enemies,
  };
}

export function fromLevelDefinition(def: LevelDefinition): EditorState {
  const state = createEmptyState(def.id, def.width, def.height);
  state.playerLives = def.playerLives;
  state.playerStart = { x: def.playerStart.x, y: def.playerStart.y };
  state.playerDirection = def.playerStart.direction;
  state.tileSize = def.tileSize;

  for (const w of def.walls) {
    const cell = state.cells.get(key(w.x, w.y));
    if (cell) cell.walls.add("wall");
  }
  for (const o of def.objectives) {
    const cell = state.cells.get(key(o.x, o.y));
    if (cell) {
      cell.objectives.push({ id: o.id, type: o.type });
    }
  }
  for (const e of def.enemies) {
    const cell = state.cells.get(key(e.x, e.y));
    if (cell) {
      cell.enemies.push({
        id: e.id,
        type: e.type,
        direction: e.direction,
        speed: e.speed,
        defends: e.defends ?? null,
      });
    }
  }
  const start = state.cells.get(key(def.playerStart.x, def.playerStart.y));
  if (start) start.player = { direction: def.playerStart.direction };
  return state;
}
