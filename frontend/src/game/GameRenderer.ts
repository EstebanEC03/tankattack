/* ============================================================
 * GameRenderer.ts
 * Dibuja el estado del juego recibido desde el backend.
 *
 * - Limpia el canvas.
 * - Pinta fondo + grilla vaporwave.
 * - Pinta muros, objetivos, balas, jugador y enemigos.
 * - NO calcula decisiones, rutas ni colisiones.
 * ============================================================ */

import {
  drawBoardBackground,
  drawBullet as drawBulletSprite,
  drawEnemyTank,
  drawObjective,
  drawPlayerTank,
  drawWall,
} from "./AssetManager";
import { Camera } from "./Camera";
import type { GameState } from "./types";

export class GameRenderer {
  private readonly ctx: CanvasRenderingContext2D;
  private readonly canvas: HTMLCanvasElement;
  private readonly camera: Camera;
  private lastTime = 0;
  private animationFrame = 0;

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    const ctx = canvas.getContext("2d");
    if (!ctx) throw new Error("Canvas 2D context no disponible");
    this.ctx = ctx;
    this.camera = new Camera();
  }

  getCamera(): Camera { return this.camera; }
  getCanvas(): HTMLCanvasElement { return this.canvas; }
  getContext(): CanvasRenderingContext2D { return this.ctx; }

  /** Ajusta el tamano del canvas al viewport disponible. */
  fit(maxWidth: number, maxHeight: number, state: GameState | null): void {
    if (!state) return;
    this.camera.configure(
      state.board.width,
      state.board.height,
      maxWidth,
      maxHeight
    );
    this.canvas.width = this.camera.getViewportWidth();
    this.canvas.height = this.camera.getViewportHeight();
  }

  render(state: GameState, now: number): void {
    const dt = this.lastTime === 0 ? 16 : (now - this.lastTime);
    this.lastTime = now;
    this.animationFrame = (this.animationFrame + dt / 16) % 360;

    const ctx = this.ctx;
    const cam = this.camera;
    const tile = cam.getTileSize();
    const cols = state.board.width;
    const rows = state.board.height;

    drawBoardBackground(
      ctx,
      this.canvas.width,
      this.canvas.height,
      tile,
      cols,
      rows
    );

    // Muros
    for (const w of state.walls) {
      const { px, py } = cam.cellToPixel(w.x, w.y);
      drawWall(ctx, px, py, tile);
    }

    // Objetivos (debajo de tanques)
    for (const o of state.objectives) {
      if (o.status === "DESTROYED") continue;
      const { px, py } = cam.cellToPixel(o.x, o.y);
      drawObjective(ctx, px, py, tile, o.type);
    }

    // Balas
    for (const b of state.bullets) {
      if (!b.active) continue;
      const { px, py } = cam.cellToPixel(b.x, b.y);
      drawBulletSprite(ctx, px, py, tile, b.ownerIsPlayer ?? true);
    }

    // Jugador
    if (state.player.status === "ALIVE") {
      const { px, py } = cam.cellToPixel(state.player.x, state.player.y);
      drawPlayerTank(ctx, px, py, tile, state.player.direction);
    }

    // Enemigos
    for (const e of state.enemies) {
      if (e.status === "DESTROYED") continue;
      const { px, py } = cam.cellToPixel(e.x, e.y);
      drawEnemyTank(ctx, px, py, tile, e.direction, e.type);
    }

    // Indicador de pausa
    if (state.status.paused) {
      ctx.save();
      ctx.fillStyle = "rgba(10, 0, 26, 0.55)";
      ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
      ctx.fillStyle = "#ff4fd8";
      ctx.font = `bold ${Math.max(18, tile)}px ui-monospace, monospace`;
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.shadowBlur = 12;
      ctx.shadowColor = "#ff4fd8";
      ctx.fillText("PAUSA", this.canvas.width / 2, this.canvas.height / 2);
      ctx.restore();
    }
  }

  /** Limpia el canvas (util para el estado "sin conexion"). */
  clear(): void {
    this.ctx.fillStyle = "#06001a";
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
  }
}
