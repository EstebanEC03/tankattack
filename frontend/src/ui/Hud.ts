/* ============================================================
 * Hud.ts
 * HUD vaporwave: vidas, nivel, objetivos, estado, IA de
 * enemigos. Actualiza el DOM en cada snapshot recibido del
 * backend.
 * ============================================================ */

import type { GameState, EnemyTank } from "../game/types";

const ACTION_LABELS: Record<string, string> = {
  DISPARAR: "Disparar",
  DEFENDER_OBJETIVO: "Defender",
  RETROCEDER: "Retroceder",
  PERSEGUIR_JUGADOR: "Perseguir",
  EMBOSCAR: "Emboscar",
  PATRULLAR: "Patrullar",
  ESPERAR: "Esperar",
  COORDINAR: "Coordinar",
};

const TYPE_LABELS: Record<string, string> = {
  RAPIDO: "Rápido",
  PESADO: "Pesado",
  TACTICO: "Táctico",
};

export class Hud {
  private readonly livesEl: HTMLElement;
  private readonly levelEl: HTMLElement;
  private readonly objectivesEl: HTMLElement;
  private readonly enemiesEl: HTMLElement;
  private readonly statusEl: HTMLElement;
  private readonly enemyListEl: HTMLElement;
  private readonly overlayEl: HTMLElement;
  private readonly overlayTitleEl: HTMLElement;
  private readonly overlayMessageEl: HTMLElement;
  private readonly overlayStartBtn: HTMLButtonElement;
  private readonly overlayRestartBtn: HTMLButtonElement;
  private readonly overlayNextBtn: HTMLButtonElement;

  private lastEnemySig = "";
  private lastStatus = "";
  private lastLevel = 0;

  constructor() {
    this.livesEl = this.require("#hud-lives");
    this.levelEl = this.require("#hud-level");
    this.objectivesEl = this.require("#hud-objectives");
    this.enemiesEl = this.require("#hud-enemies");
    this.statusEl = this.require("#hud-status");
    this.enemyListEl = this.require("#enemy-list");
    this.overlayEl = this.require("#overlay");
    this.overlayTitleEl = this.require("#overlay-title");
    this.overlayMessageEl = this.require("#overlay-message");
    this.overlayStartBtn = this.require<HTMLButtonElement>("#overlay-start");
    this.overlayRestartBtn = this.require<HTMLButtonElement>("#overlay-restart");
    this.overlayNextBtn = this.require<HTMLButtonElement>("#overlay-next");
  }

  /** Llama cuando llega un nuevo estado del backend. */
  update(state: GameState): void {
    this.livesEl.textContent = String(state.player.lives);
    if (this.lastLevel !== state.level) {
      this.levelEl.textContent = String(state.level);
      this.lastLevel = state.level;
    }
    this.objectivesEl.textContent = String(state.status.activeObjectives);
    this.enemiesEl.textContent = String(state.status.aliveEnemies);

    const statusText = this.computeStatusText(state);
    if (statusText !== this.lastStatus) {
      this.statusEl.textContent = statusText;
      this.lastStatus = statusText;
    }

    const sig = state.enemies
      .map((e) => `${e.id}:${e.action ?? ""}:${e.role ?? ""}:${e.status}`)
      .join("|");
    if (sig !== this.lastEnemySig) {
      this.renderEnemyList(state.enemies);
      this.lastEnemySig = sig;
    }

    this.updateOverlay(state);
  }

  hideOverlay(): void {
    this.overlayEl.classList.add("hidden");
  }

  setOverlayStartHandler(fn: () => void): void {
    this.overlayStartBtn.onclick = () => fn();
  }
  setOverlayRestartHandler(fn: () => void): void {
    this.overlayRestartBtn.onclick = () => fn();
  }
  setOverlayNextHandler(fn: () => void): void {
    this.overlayNextBtn.onclick = () => fn();
  }

  setConnectionStatus(status: "connecting" | "connected" | "disconnected"): void {
    const el = document.getElementById("ws-status");
    if (!el) return;
    el.textContent = `WS: ${status}`;
    el.classList.remove("connected", "disconnected", "connecting");
    el.classList.add(status);
  }

  private computeStatusText(state: GameState): string {
    if (state.status.gameOver) return "Game Over";
    if (state.status.levelCompleted) return "Nivel completo";
    if (state.status.paused) return "Pausa";
    if (!state.status.running) return "Detenido";
    return "En curso";
  }

  private renderEnemyList(enemies: EnemyTank[]): void {
    this.enemyListEl.innerHTML = "";
    if (enemies.length === 0) {
      const li = document.createElement("li");
      li.textContent = "— sin enemigos —";
      this.enemyListEl.appendChild(li);
      return;
    }
    for (const e of enemies) {
      const li = document.createElement("li");
      const id = document.createElement("span");
      id.className = "enemy-id";
      id.textContent = `${e.id} · ${TYPE_LABELS[e.type] ?? e.type}`;
      const action = document.createElement("span");
      action.className = "enemy-action";
      action.textContent = `acción: ${ACTION_LABELS[String(e.action ?? "")] ?? (e.action ?? "—")}`;
      const role = document.createElement("span");
      role.className = "enemy-role";
      role.textContent = `rol: ${e.role ?? "—"}`;
      li.appendChild(id);
      li.appendChild(action);
      li.appendChild(role);
      this.enemyListEl.appendChild(li);
    }
  }

  private updateOverlay(state: GameState): void {
    if (state.status.gameOver) {
      this.overlayEl.classList.remove("hidden");
      this.overlayTitleEl.textContent = "GAME OVER";
      this.overlayMessageEl.textContent = "Te han destruido. Intentalo de nuevo.";
      this.overlayStartBtn.classList.add("hidden");
      this.overlayRestartBtn.classList.remove("hidden");
      this.overlayNextBtn.classList.add("hidden");
      return;
    }
    if (state.status.levelCompleted) {
      this.overlayEl.classList.remove("hidden");
      this.overlayTitleEl.textContent = "NIVEL COMPLETO";
      this.overlayMessageEl.textContent = "Has destruido todos los objetivos.";
      this.overlayStartBtn.classList.add("hidden");
      this.overlayRestartBtn.classList.remove("hidden");
      this.overlayNextBtn.classList.remove("hidden");
      return;
    }
    if (!state.status.running) {
      this.overlayEl.classList.remove("hidden");
      this.overlayTitleEl.textContent = "TANK ATTACK";
      this.overlayMessageEl.textContent = "Presiona Iniciar para jugar";
      this.overlayStartBtn.classList.remove("hidden");
      this.overlayRestartBtn.classList.add("hidden");
      this.overlayNextBtn.classList.add("hidden");
      return;
    }
    this.overlayEl.classList.add("hidden");
  }

  private require<T extends HTMLElement = HTMLElement>(selector: string): T {
    const el = document.querySelector(selector) as T | null;
    if (!el) throw new Error(`No se encontro ${selector} en el DOM`);
    return el;
  }
}
