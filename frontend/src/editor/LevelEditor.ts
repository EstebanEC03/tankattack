/* ============================================================
 * LevelEditor.ts
 * Editor visual de niveles. Dibuja un canvas con la grilla
 * y permite colocar muros, jugador, enemigos y objetivos.
 * ============================================================ */

import {
  drawBoardBackground,
  drawEnemyTank,
  drawObjective,
  drawPlayerTank,
  drawWall,
} from "../game/AssetManager";
import { Camera } from "../game/Camera";
import type { EditorTool, Direction } from "../game/types";
import {
  applyTool,
  createEmptyState,
  fromLevelDefinition,
  resizeState,
  toLevelDefinition,
  type EditorState,
} from "./LevelSerializer";
import type { GameClient } from "../game/GameClient";
import type { LevelDefinition } from "../game/types";

export class LevelEditor {
  private readonly canvas: HTMLCanvasElement;
  private readonly ctx: CanvasRenderingContext2D;
  private readonly camera = new Camera();
  private readonly client: GameClient;
  private readonly onPlay: (levelId: string) => void;

  private state: EditorState = createEmptyState("custom1", 20, 15);
  private tool: EditorTool = "ENEMY_RAPIDO";
  private dragging = false;

  constructor(
    canvas: HTMLCanvasElement,
    client: GameClient,
    onPlay: (levelId: string) => void
  ) {
    this.canvas = canvas;
    const ctx = canvas.getContext("2d");
    if (!ctx) throw new Error("Canvas 2D context no disponible");
    this.ctx = ctx;
    this.client = client;
    this.onPlay = onPlay;

    this.fit();
    this.canvas.addEventListener("mousedown", this.onMouseDown);
    this.canvas.addEventListener("mousemove", this.onMouseDrag);
    this.canvas.addEventListener("mouseup", this.onMouseUp);
    this.canvas.addEventListener("mouseleave", this.onMouseUp);
  }

  setTool(tool: EditorTool): void {
    this.tool = tool;
  }

  resize(width: number, height: number): void {
    this.state = resizeState(this.state, width, height);
    this.fit();
    this.draw();
  }

  setId(id: string): void {
    this.state.id = id;
  }

  toDefinition(): LevelDefinition {
    return toLevelDefinition(this.state);
  }

  async loadById(id: string): Promise<boolean> {
    const def = await this.client.getLevel(id);
    if (!def) return false;
    this.state = fromLevelDefinition(def);
    this.fit();
    this.draw();
    return true;
  }

  async save(): Promise<boolean> {
    const def = toLevelDefinition(this.state);
    const saved = await this.client.saveLevel(def);
    return saved != null;
  }

  playThis(): void {
    this.onPlay(this.state.id);
  }

  fit(): void {
    const parent = this.canvas.parentElement;
    const w = parent ? parent.clientWidth - 32 : 640;
    const h = 480;
    this.camera.configure(this.state.width, this.state.height, w, h);
    this.canvas.width = this.camera.getViewportWidth();
    this.canvas.height = this.camera.getViewportHeight();
  }

  draw(): void {
    const cam = this.camera;
    const tile = cam.getTileSize();
    drawBoardBackground(
      this.ctx,
      this.canvas.width,
      this.canvas.height,
      tile,
      this.state.width,
      this.state.height
    );

    for (let y = 0; y < this.state.height; y++) {
      for (let x = 0; x < this.state.width; x++) {
        const cell = this.state.cells.get(`${x},${y}`);
        if (!cell) continue;
        const { px, py } = cam.cellToPixel(x, y);
        if (cell.walls.has("wall")) {
          drawWall(this.ctx, px, py, tile);
        }
        for (const o of cell.objectives) {
          drawObjective(this.ctx, px, py, tile, o.type);
        }
        for (const e of cell.enemies) {
          drawEnemyTank(this.ctx, px, py, tile, e.direction, e.type);
        }
        if (cell.player) {
          drawPlayerTank(this.ctx, px, py, tile, this.state.playerDirection);
        }
      }
    }
  }

  private onMouseDown = (ev: MouseEvent): void => {
    this.dragging = true;
    this.paintAt(ev);
  };
  private onMouseDrag = (ev: MouseEvent): void => {
    if (!this.dragging) return;
    this.paintAt(ev);
  };
  private onMouseUp = (): void => {
    this.dragging = false;
  };

  private paintAt(ev: MouseEvent): void {
    const rect = this.canvas.getBoundingClientRect();
    const px = (ev.clientX - rect.left) * (this.canvas.width / rect.width);
    const py = (ev.clientY - rect.top) * (this.canvas.height / rect.height);
    const tile = this.camera.getTileSize();
    const x = Math.floor(px / tile);
    const y = Math.floor(py / tile);
    this.state = applyTool(this.state, x, y, this.tool);
    this.draw();
  }

  /** Cambia la direccion del jugador en la celda de inicio. */
  rotatePlayerDirection(): void {
    const order: Direction[] = ["UP", "RIGHT", "DOWN", "LEFT"];
    const i = order.indexOf(this.state.playerDirection);
    const dir = order[(i + 1) % order.length];
    this.state = { ...this.state, playerDirection: dir };
    const cell = this.state.cells.get(
      `${this.state.playerStart.x},${this.state.playerStart.y}`
    );
    if (cell) cell.player = { direction: dir };
    this.draw();
  }
}
