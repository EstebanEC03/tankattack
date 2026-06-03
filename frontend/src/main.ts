/* ============================================================
 * main.ts
 * Punto de entrada del frontend. Conecta el cliente HTTP+WS
 * con el motor de renderizado, captura entradas, mantiene el
 * HUD sincronizado y orquesta el editor de niveles.
 * ============================================================ */

/// <reference types="vite/client" />

import { GameClient } from "./game/GameClient";
import { GameRenderer } from "./game/GameRenderer";
import { InputController } from "./game/InputController";
import { Hud } from "./ui/Hud";
import { LevelEditor } from "./editor/LevelEditor";
import type { Direction, EditorTool, GameState } from "./game/types";

/* ------------------------------------------------------------ */

const client = new GameClient({
  baseUrl: import.meta.env.VITE_BACKEND_URL ?? "http://localhost:7070",
  wsUrl: import.meta.env.VITE_WS_URL ?? "ws://localhost:7070/ws/game",
});

const canvas = document.getElementById("game-canvas") as HTMLCanvasElement;
const renderer = new GameRenderer(canvas);
const hud = new Hud();
const input = new InputController();

let lastState: GameState | null = null;
let renderLoopRunning = false;

/* ---------- View switcher ---------- */

const playView = document.getElementById("play-view")!;
const editorView = document.getElementById("editor-view")!;
const viewSwitcher = document.getElementById("view-switcher")!;

viewSwitcher.addEventListener("click", (ev) => {
  const target = ev.target as HTMLElement;
  if (!target.matches("button")) return;
  const view = target.dataset.view;
  if (view === "play") {
    playView.classList.add("active");
    editorView.classList.remove("active");
    viewSwitcher.querySelectorAll("button").forEach((b) =>
      b.classList.toggle("active", b === target)
    );
  } else if (view === "editor") {
    playView.classList.remove("active");
    editorView.classList.add("active");
    viewSwitcher.querySelectorAll("button").forEach((b) =>
      b.classList.toggle("active", b === target)
    );
  }
});

/* ---------- Renderer loop (60 FPS) ---------- */

function tick(now: number): void {
  if (lastState) {
    renderer.fit(canvas.parentElement?.clientWidth ?? 640,
                 canvas.parentElement?.clientHeight ?? 480,
                 lastState);
    renderer.render(lastState, now);
  } else {
    renderer.clear();
  }
  if (renderLoopRunning) requestAnimationFrame(tick);
}

/* ---------- React to backend state ---------- */

client.onState((state) => {
  lastState = state;
  hud.update(state);
  if (!renderLoopRunning) {
    renderLoopRunning = true;
    requestAnimationFrame(tick);
  }
});

client.onConnection((status) => {
  hud.setConnectionStatus(status);
});

/* ---------- Input -> backend ---------- */

input.on((event) => {
  switch (event.type) {
    case "MOVE":
      client.send({ type: "PLAYER_MOVE", direction: event.direction });
      break;
    case "SHOOT":
      client.send({ type: "PLAYER_SHOOT" });
      break;
    case "PAUSE":
      client.send({ type: "PAUSE_GAME" });
      break;
    case "RESTART":
      client.send({ type: "RESTART" });
      break;
    case "NEXT_LEVEL":
      client.nextLevel();
      break;
  }
});

/* ---------- Overlay actions ---------- */

hud.setOverlayStartHandler(() => {
  client.startGame();
});
hud.setOverlayRestartHandler(() => {
  client.restartGame();
});
hud.setOverlayNextHandler(() => {
  client.nextLevel();
});

/* ---------- Editor ---------- */

const editorCanvas = document.getElementById("editor-canvas") as HTMLCanvasElement;
const editor = new LevelEditor(editorCanvas, client, (levelId) => {
  client.loadLevel(levelId).then(() => {
    playView.classList.add("active");
    editorView.classList.remove("active");
    viewSwitcher.querySelectorAll("button").forEach((b) =>
      b.classList.toggle("active", b.dataset.view === "play")
    );
  });
});

const editorTools = document.querySelectorAll<HTMLButtonElement>(
  "#editor-toolbar .editor-tools button"
);
editorTools.forEach((btn) => {
  btn.addEventListener("click", () => {
    const tool = btn.dataset.tool as EditorTool;
    editor.setTool(tool);
    editorTools.forEach((b) => b.classList.toggle("active", b === btn));
  });
});

const editorWidthInput = document.getElementById("editor-width") as HTMLInputElement;
const editorHeightInput = document.getElementById("editor-height") as HTMLInputElement;
document.getElementById("editor-resize")?.addEventListener("click", () => {
  const w = Math.max(5, Math.min(40, parseInt(editorWidthInput.value, 10) || 20));
  const h = Math.max(5, Math.min(40, parseInt(editorHeightInput.value, 10) || 15));
  editorWidthInput.value = String(w);
  editorHeightInput.value = String(h);
  editor.resize(w, h);
});

document.getElementById("editor-save")?.addEventListener("click", async () => {
  const def = editor.toDefinition();
  const idInput = document.getElementById("editor-load-id") as HTMLInputElement | null;
  if (idInput && idInput.value.trim()) {
    def.id = idInput.value.trim();
    editor.setId(def.id);
  }
  const ok = await editor.save();
  if (ok) {
    flashEditorStatus("Nivel guardado");
  } else {
    flashEditorStatus("Error al guardar");
  }
});

document.getElementById("editor-load")?.addEventListener("click", async () => {
  const idInput = document.getElementById("editor-load-id") as HTMLInputElement;
  const id = idInput.value.trim();
  if (!id) {
    flashEditorStatus("Especifica un ID");
    return;
  }
  const ok = await editor.loadById(id);
  if (ok) {
    flashEditorStatus(`Cargado: ${id}`);
  } else {
    flashEditorStatus(`No se encontro ${id}`);
  }
});

document.getElementById("editor-play")?.addEventListener("click", () => {
  editor.playThis();
});

function flashEditorStatus(msg: string): void {
  const el = document.getElementById("editor-toolbar");
  if (!el) return;
  const div = document.createElement("div");
  div.textContent = msg;
  div.style.cssText = "color:var(--neon-cyan);font-size:11px;margin-top:4px;";
  el.appendChild(div);
  setTimeout(() => div.remove(), 1500);
}

/* ---------- Boot ---------- */

client.connect();
input.attach();

window.addEventListener("resize", () => {
  if (lastState) {
    renderer.fit(canvas.parentElement?.clientWidth ?? 640,
                 canvas.parentElement?.clientHeight ?? 480,
                 lastState);
  }
  editor.fit();
  editor.draw();
});

// Atajo: clic derecho en el editor rota la direccion del jugador.
editorCanvas.addEventListener("contextmenu", (ev) => {
  ev.preventDefault();
  editor.rotatePlayerDirection();
});

/* Cuando el DOM este listo, intentar obtener un snapshot inicial. */
window.addEventListener("DOMContentLoaded", () => {
  client.getState().then((state) => {
    if (state) {
      lastState = state;
      hud.update(state);
      renderer.fit(canvas.parentElement?.clientWidth ?? 640,
                   canvas.parentElement?.clientHeight ?? 480,
                   state);
      if (!renderLoopRunning) {
        renderLoopRunning = true;
        requestAnimationFrame(tick);
      }
    }
  });
  editor.draw();
});
