/* ============================================================
 * types.ts
 * Tipos compartidos del frontend. Deben coincidir con los DTO
 * de Java en com.tankattack.api.GameStateDto.
 * ============================================================ */

export type Direction = "UP" | "DOWN" | "LEFT" | "RIGHT";
export type EnemyType = "RAPIDO" | "PESADO" | "TACTICO";
export type ObjectiveType = "BASE" | "REFINERIA";
export type TankStatus = "ALIVE" | "DESTROYED";
export type ObjectiveStatus = "ACTIVE" | "DESTROYED";

export type EnemyAction =
  | "DISPARAR"
  | "DEFENDER_OBJETIVO"
  | "RETROCEDER"
  | "PERSEGUIR_JUGADOR"
  | "EMBOSCAR"
  | "PATRULLAR"
  | "ESPERAR"
  | "COORDINAR";

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
  status: TankStatus;
  skin?: string;
}

export interface EnemyTank {
  id: string;
  type: EnemyType;
  x: number;
  y: number;
  direction: Direction;
  status: TankStatus;
  action?: EnemyAction | null;
  role?: string | null;
  skin?: string;
  defends?: string | null;
}

export interface Objective {
  id: string;
  type: ObjectiveType;
  x: number;
  y: number;
  status: ObjectiveStatus;
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
  ownerIsPlayer?: boolean;
  x: number;
  y: number;
  direction: Direction;
  active: boolean;
  skin?: string;
}

export interface GameStatus {
  running: boolean;
  paused: boolean;
  gameOver: boolean;
  levelCompleted: boolean;
  tick: number;
  aliveEnemies: number;
  activeObjectives: number;
}

export interface GameState {
  level: number;
  board: Board;
  player: PlayerTank;
  enemies: EnemyTank[];
  objectives: Objective[];
  walls: Wall[];
  bullets: Bullet[];
  status: GameStatus;
}

export interface WsEnvelope {
  type: string;
  payload: GameState;
}

export interface ClientMessage {
  type:
    | "PLAYER_MOVE"
    | "PLAYER_SHOOT"
    | "PAUSE_GAME"
    | "RESTART"
    | "LOAD_LEVEL"
    | "NEXT_LEVEL";
  direction?: Direction;
  levelId?: string;
}

export interface LevelSummary {
  id: string;
  width: number;
  height: number;
  objectives: number;
  enemies: number;
}

export interface LevelDefinition {
  id: string;
  width: number;
  height: number;
  tileSize: number;
  playerLives: number;
  playerStart: { x: number; y: number; direction: Direction };
  walls: Position[];
  objectives: Array<{ id: string; type: ObjectiveType; x: number; y: number }>;
  enemies: Array<{
    id: string;
    type: EnemyType;
    x: number;
    y: number;
    direction: Direction;
    speed: number;
    defends?: string | null;
  }>;
}

export type EditorTool =
  | "WALL"
  | "PLAYER"
  | "ENEMY_RAPIDO"
  | "ENEMY_PESADO"
  | "ENEMY_TACTICO"
  | "OBJECTIVE_BASE"
  | "OBJECTIVE_REFINERIA"
  | "ERASE";
