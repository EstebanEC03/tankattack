/* ============================================================
 * AssetManager.ts
 * Sprites vaporwave generados proceduralmente con Canvas.
 * No depende de archivos externos: cada entidad se dibuja
 * en el momento a partir de formas geometricas simples.
 * ============================================================ */

import type { Direction, EnemyType, ObjectiveType } from "./types";

export const VAPORWAVE = {
  bgDark: "#120027",
  bgDeep: "#06001a",
  neonPurple: "#7b2cff",
  neonPink: "#ff4fd8",
  neonCyan: "#00e5ff",
  neonYellow: "#ffe066",
  white: "#f8f8ff",
  gray: "rgba(248, 248, 255, 0.4)",
} as const;

/** Dibuja el fondo del tablero: gradiente + reticula neon. */
export function drawBoardBackground(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  tileSize: number,
  cols: number,
  rows: number
): void {
  // Gradiente principal
  const g = ctx.createLinearGradient(0, 0, 0, height);
  g.addColorStop(0, "#1a0033");
  g.addColorStop(0.5, "#120027");
  g.addColorStop(1, "#06001a");
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, width, height);

  // Glow central suave
  const rg = ctx.createRadialGradient(
    width / 2, height / 2, Math.min(width, height) * 0.1,
    width / 2, height / 2, Math.max(width, height) * 0.6
  );
  rg.addColorStop(0, "rgba(123, 44, 255, 0.20)");
  rg.addColorStop(1, "rgba(123, 44, 255, 0)");
  ctx.fillStyle = rg;
  ctx.fillRect(0, 0, width, height);

  // Reticula
  ctx.strokeStyle = "rgba(0, 229, 255, 0.07)";
  ctx.lineWidth = 1;
  ctx.beginPath();
  for (let x = 0; x <= cols; x++) {
    const px = x * tileSize + 0.5;
    ctx.moveTo(px, 0);
    ctx.lineTo(px, rows * tileSize);
  }
  for (let y = 0; y <= rows; y++) {
    const py = y * tileSize + 0.5;
    ctx.moveTo(0, py);
    ctx.lineTo(cols * tileSize, py);
  }
  ctx.stroke();
}

/** Tanque del jugador: cuerpo celeste, bordes rosados, canon blanco. */
export function drawPlayerTank(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  size: number,
  direction: Direction
): void {
  drawGenericTank(ctx, x, y, size, direction, {
    body: VAPORWAVE.neonCyan,
    bodyDark: "#00a0b3",
    edge: VAPORWAVE.neonPink,
    cannon: VAPORWAVE.white,
    glow: VAPORWAVE.neonCyan,
  });
}

export function drawEnemyTank(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  size: number,
  direction: Direction,
  type: EnemyType
): void {
  if (type === "RAPIDO") {
    drawGenericTank(ctx, x, y, size, direction, {
      body: VAPORWAVE.neonPink,
      bodyDark: "#a3219b",
      edge: VAPORWAVE.neonYellow,
      cannon: VAPORWAVE.white,
      glow: VAPORWAVE.neonPink,
      stripe: true,
    });
  } else if (type === "PESADO") {
    drawHeavyTank(ctx, x, y, size, direction);
  } else {
    drawTacticalTank(ctx, x, y, size, direction);
  }
}

interface TankStyle {
  body: string;
  bodyDark: string;
  edge: string;
  cannon: string;
  glow: string;
  stripe?: boolean;
}

function drawGenericTank(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  size: number,
  direction: Direction,
  s: TankStyle
): void {
  const cx = x + size / 2;
  const cy = y + size / 2;

  ctx.save();
  ctx.shadowBlur = 14;
  ctx.shadowColor = s.glow;

  // Cuerpo
  ctx.fillStyle = s.body;
  ctx.fillRect(x + 5, y + 11, size - 10, size - 22);

  // Laterales oscuros
  ctx.fillStyle = s.bodyDark;
  ctx.fillRect(x + 5, y + 11, 3, size - 22);
  ctx.fillRect(x + size - 8, y + 11, 3, size - 22);

  // Borde
  ctx.shadowBlur = 0;
  ctx.strokeStyle = s.edge;
  ctx.lineWidth = 1.5;
  ctx.strokeRect(x + 5, y + 11, size - 10, size - 22);

  // Banda de velocidad opcional (tanque rapido)
  if (s.stripe) {
    ctx.strokeStyle = s.edge;
    ctx.lineWidth = 1;
    ctx.beginPath();
    for (let i = 0; i < 3; i++) {
      const ly = y + 14 + i * 4;
      ctx.moveTo(x + 8, ly);
      ctx.lineTo(x + 18, ly);
    }
    ctx.stroke();
  }

  // Torreta
  ctx.fillStyle = s.body;
  ctx.fillRect(cx - 5, cy - 5, 10, 10);
  ctx.strokeStyle = s.edge;
  ctx.strokeRect(cx - 5, cy - 5, 10, 10);

  // Canon segun direccion
  drawCannon(ctx, cx, cy, size, direction, s.cannon, s.edge);
  ctx.restore();
}

function drawCannon(
  ctx: CanvasRenderingContext2D,
  cx: number,
  cy: number,
  size: number,
  direction: Direction,
  color: string,
  edge: string
): void {
  const len = size * 0.55;
  const w = 4;
  ctx.fillStyle = color;
  ctx.strokeStyle = edge;
  ctx.lineWidth = 1;
  ctx.beginPath();
  switch (direction) {
    case "UP":
      ctx.rect(cx - w / 2, cy - len, w, len);
      break;
    case "DOWN":
      ctx.rect(cx - w / 2, cy, w, len);
      break;
    case "LEFT":
      ctx.rect(cx - len, cy - w / 2, len, w);
      break;
    case "RIGHT":
      ctx.rect(cx, cy - w / 2, len, w);
      break;
  }
  ctx.fill();
  ctx.stroke();
}

function drawHeavyTank(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  size: number,
  direction: Direction
): void {
  const cx = x + size / 2;
  const cy = y + size / 2;
  ctx.save();
  ctx.shadowBlur = 18;
  ctx.shadowColor = VAPORWAVE.neonYellow;

  // Cuerpo mas grande
  ctx.fillStyle = "#3a1a6b";
  ctx.fillRect(x + 2, y + 8, size - 4, size - 16);
  ctx.fillStyle = "#5a2a9b";
  ctx.fillRect(x + 5, y + 10, size - 10, size - 20);

  // Borde grueso
  ctx.shadowBlur = 0;
  ctx.strokeStyle = VAPORWAVE.neonYellow;
  ctx.lineWidth = 2;
  ctx.strokeRect(x + 2, y + 8, size - 4, size - 16);

  // Detalle de blindaje
  ctx.strokeStyle = "rgba(255, 224, 102, 0.6)";
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(x + 5, y + 16);
  ctx.lineTo(x + size - 5, y + 16);
  ctx.moveTo(x + 5, y + size - 16);
  ctx.lineTo(x + size - 5, y + size - 16);
  ctx.stroke();

  // Torreta pesada
  ctx.fillStyle = "#3a1a6b";
  ctx.fillRect(cx - 7, cy - 7, 14, 14);
  ctx.strokeStyle = VAPORWAVE.neonYellow;
  ctx.lineWidth = 1.5;
  ctx.strokeRect(cx - 7, cy - 7, 14, 14);

  // Canon
  drawCannon(ctx, cx, cy, size, direction, VAPORWAVE.white, VAPORWAVE.neonYellow);
  ctx.restore();
}

function drawTacticalTank(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  size: number,
  direction: Direction
): void {
  const cx = x + size / 2;
  const cy = y + size / 2;
  ctx.save();
  ctx.shadowBlur = 14;
  ctx.shadowColor = VAPORWAVE.neonPurple;

  ctx.fillStyle = "#2a1760";
  ctx.fillRect(x + 5, y + 11, size - 10, size - 22);

  // Circuitos decorativos
  ctx.shadowBlur = 0;
  ctx.strokeStyle = "rgba(0, 229, 255, 0.6)";
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(x + 8, y + 14);
  ctx.lineTo(x + 14, y + 14);
  ctx.lineTo(x + 14, y + 18);
  ctx.lineTo(x + 20, y + 18);
  ctx.moveTo(x + size - 8, y + 14);
  ctx.lineTo(x + size - 14, y + 14);
  ctx.lineTo(x + size - 14, y + 18);
  ctx.lineTo(x + size - 20, y + 18);
  ctx.stroke();

  // Borde
  ctx.strokeStyle = VAPORWAVE.neonCyan;
  ctx.lineWidth = 1.5;
  ctx.strokeRect(x + 5, y + 11, size - 10, size - 22);

  // Torreta
  ctx.fillStyle = "#2a1760";
  ctx.fillRect(cx - 5, cy - 5, 10, 10);
  ctx.strokeStyle = VAPORWAVE.neonCyan;
  ctx.strokeRect(cx - 5, cy - 5, 10, 10);

  // Punto brillante en torreta
  ctx.fillStyle = VAPORWAVE.neonCyan;
  ctx.beginPath();
  ctx.arc(cx, cy, 1.5, 0, Math.PI * 2);
  ctx.fill();

  drawCannon(ctx, cx, cy, size, direction, VAPORWAVE.neonCyan, VAPORWAVE.white);
  ctx.restore();
}

export function drawObjective(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  size: number,
  type: ObjectiveType
): void {
  if (type === "BASE") drawBase(ctx, x, y, size);
  else drawRefinery(ctx, x, y, size);
}

function drawBase(ctx: CanvasRenderingContext2D, x: number, y: number, size: number): void {
  const cx = x + size / 2;
  const cy = y + size / 2;
  ctx.save();
  ctx.shadowBlur = 16;
  ctx.shadowColor = VAPORWAVE.neonPink;

  // Anillo exterior
  ctx.strokeStyle = VAPORWAVE.neonPink;
  ctx.lineWidth = 1.5;
  ctx.beginPath();
  ctx.arc(cx, cy, size * 0.42, 0, Math.PI * 2);
  ctx.stroke();

  // Anillo interior
  ctx.shadowBlur = 0;
  ctx.strokeStyle = "rgba(255, 79, 216, 0.5)";
  ctx.beginPath();
  ctx.arc(cx, cy, size * 0.30, 0, Math.PI * 2);
  ctx.stroke();

  // Nucleo brillante
  const g = ctx.createRadialGradient(cx, cy, 1, cx, cy, size * 0.28);
  g.addColorStop(0, VAPORWAVE.white);
  g.addColorStop(0.4, VAPORWAVE.neonPink);
  g.addColorStop(1, "rgba(123, 44, 255, 0)");
  ctx.fillStyle = g;
  ctx.beginPath();
  ctx.arc(cx, cy, size * 0.28, 0, Math.PI * 2);
  ctx.fill();

  // Cruz pequena
  ctx.strokeStyle = VAPORWAVE.neonPurple;
  ctx.lineWidth = 1.2;
  ctx.beginPath();
  ctx.moveTo(cx - size * 0.12, cy);
  ctx.lineTo(cx + size * 0.12, cy);
  ctx.moveTo(cx, cy - size * 0.12);
  ctx.lineTo(cx, cy + size * 0.12);
  ctx.stroke();
  ctx.restore();
}

function drawRefinery(ctx: CanvasRenderingContext2D, x: number, y: number, size: number): void {
  ctx.save();
  ctx.shadowBlur = 14;
  ctx.shadowColor = VAPORWAVE.neonCyan;

  // Tanque principal (cilindro)
  ctx.fillStyle = "#0c3a55";
  ctx.fillRect(x + 5, y + 10, size - 10, size - 20);
  ctx.shadowBlur = 0;
  ctx.strokeStyle = VAPORWAVE.neonCyan;
  ctx.lineWidth = 1.2;
  ctx.strokeRect(x + 5, y + 10, size - 10, size - 20);

  // Brillo superior
  ctx.fillStyle = "rgba(0, 229, 255, 0.4)";
  ctx.fillRect(x + 5, y + 10, size - 10, 3);

  // Tanques secundarios
  ctx.fillStyle = "#5a1a4a";
  ctx.fillRect(x + 2, y + 18, 3, size - 30);
  ctx.fillRect(x + size - 5, y + 18, 3, size - 30);
  ctx.strokeStyle = VAPORWAVE.neonPink;
  ctx.strokeRect(x + 2, y + 18, 3, size - 30);
  ctx.strokeRect(x + size - 5, y + 18, 3, size - 30);

  // Valvula / detalle
  ctx.fillStyle = VAPORWAVE.neonYellow;
  ctx.beginPath();
  ctx.arc(x + size / 2, y + 14, 2, 0, Math.PI * 2);
  ctx.fill();

  // Etiqueta "REF"
  ctx.fillStyle = VAPORWAVE.white;
  ctx.font = `bold 6px ui-monospace, monospace`;
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText("REF", x + size / 2, y + size / 2 + 6);
  ctx.restore();
}

export function drawWall(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  size: number
): void {
  ctx.save();
  // Cuerpo
  const g = ctx.createLinearGradient(x, y, x, y + size);
  g.addColorStop(0, "#1f0a3d");
  g.addColorStop(1, "#0c001a");
  ctx.fillStyle = g;
  ctx.fillRect(x + 1, y + 1, size - 2, size - 2);

  // Borde neon
  ctx.strokeStyle = VAPORWAVE.neonPurple;
  ctx.lineWidth = 1.5;
  ctx.strokeRect(x + 1, y + 1, size - 2, size - 2);

  // Lineas internas (ladrillo neon)
  ctx.strokeStyle = "rgba(0, 229, 255, 0.35)";
  ctx.lineWidth = 0.5;
  ctx.beginPath();
  ctx.moveTo(x + 1, y + size / 2);
  ctx.lineTo(x + size - 1, y + size / 2);
  ctx.moveTo(x + size / 2, y + 1);
  ctx.lineTo(x + size / 2, y + size - 1);
  ctx.stroke();
  ctx.restore();
}

export function drawBullet(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  size: number,
  ownerIsPlayer: boolean
): void {
  const cx = x + size / 2;
  const cy = y + size / 2;
  const r = size * 0.28;
  const color = ownerIsPlayer ? VAPORWAVE.neonYellow : VAPORWAVE.neonPink;
  ctx.save();
  ctx.shadowBlur = 16;
  ctx.shadowColor = color;
  ctx.fillStyle = color;
  ctx.beginPath();
  ctx.arc(cx, cy, r, 0, Math.PI * 2);
  ctx.fill();
  // Nucleo blanco
  ctx.shadowBlur = 0;
  ctx.fillStyle = VAPORWAVE.white;
  ctx.beginPath();
  ctx.arc(cx, cy, r * 0.45, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
}
