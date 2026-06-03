/* ============================================================
 * InputController.ts
 * Captura el teclado y emite acciones hacia el backend.
 *
 * El frontend NO mueve al jugador por su cuenta: cada
 * pulsacion se traduce a un mensaje WebSocket y el backend
 * decide si es valido.
 * ============================================================ */

import type { Direction } from "./types";

export type InputEvent =
  | { type: "MOVE"; direction: Direction }
  | { type: "SHOOT" }
  | { type: "PAUSE" }
  | { type: "RESTART" }
  | { type: "NEXT_LEVEL" };

export type InputListener = (event: InputEvent) => void;

const DIRECTION_KEYS: Record<string, Direction> = {
  ArrowUp: "UP", KeyW: "UP",
  ArrowDown: "DOWN", KeyS: "DOWN",
  ArrowLeft: "LEFT", KeyA: "LEFT",
  ArrowRight: "RIGHT", KeyD: "RIGHT",
};

export class InputController {
  private readonly listeners = new Set<InputListener>();
  private active: Set<string> = new Set();
  private attached = false;
  private lastMoveSentAt = 0;
  private moveCooldownMs = 90;

  attach(): void {
    if (this.attached) return;
    this.attached = true;
    window.addEventListener("keydown", this.onKeyDown);
    window.addEventListener("keyup", this.onKeyUp);
    window.addEventListener("blur", this.onBlur);
  }

  detach(): void {
    if (!this.attached) return;
    this.attached = false;
    window.removeEventListener("keydown", this.onKeyDown);
    window.removeEventListener("keyup", this.onKeyUp);
    window.removeEventListener("blur", this.onBlur);
  }

  on(listener: InputListener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  setMoveCooldown(ms: number): void {
    this.moveCooldownMs = Math.max(0, ms);
  }

  private onKeyDown = (ev: KeyboardEvent): void => {
    if (ev.repeat) return;
    if (DIRECTION_KEYS[ev.code]) {
      const dir = DIRECTION_KEYS[ev.code];
      this.active.add(ev.code);
      this.emit({ type: "MOVE", direction: dir });
      ev.preventDefault();
      return;
    }
    if (ev.code === "Space") {
      this.emit({ type: "SHOOT" });
      ev.preventDefault();
      return;
    }
    if (ev.code === "KeyP") {
      this.emit({ type: "PAUSE" });
      ev.preventDefault();
      return;
    }
    if (ev.code === "KeyR") {
      this.emit({ type: "RESTART" });
      ev.preventDefault();
      return;
    }
    if (ev.code === "KeyN") {
      this.emit({ type: "NEXT_LEVEL" });
      ev.preventDefault();
    }
  };

  private onKeyUp = (ev: KeyboardEvent): void => {
    this.active.delete(ev.code);
  };

  private onBlur = (): void => {
    this.active.clear();
  };

  private emit(event: InputEvent): void {
    if (event.type === "MOVE") {
      const now = performance.now();
      if (now - this.lastMoveSentAt < this.moveCooldownMs) return;
      this.lastMoveSentAt = now;
    }
    for (const l of this.listeners) {
      try { l(event); } catch { /* listener error: ignored */ }
    }
  }
}
