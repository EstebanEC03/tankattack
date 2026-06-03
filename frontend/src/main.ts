/* ============================================================
 * main.ts
 * Punto de entrada del frontend. Conecta el cliente HTTP+WS
 * con el motor de renderizado, captura entradas, mantiene el
 * HUD sincronizado y orquesta el editor de niveles.
 *
 * Modo de juego: tick manual por accion.
 *   - El backend solo avanza la simulacion cuando el jugador
 *     envia un MOVE o SHOOT.
 *   - Pause siempre esta disponible (incluso pausado).
 *   - Editor se muestra como vista completa al pulsar
 *     "Editar nivel".
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
let editorOpen = false;

/* ---------- View switcher (Play <-> Editor) ---------- */

const playView = document.getElementById("play-view")!;
const editorView = document.getElementById("editor-view")!;
const editLevelBtn = document.getElementById("btn-edit-level")!;
const editorCloseBtn = document.getElementById("editor-close")!;

function showPlayView(): void {
  editorOpen = false;
  playView.classList.add("active");
  playView.classList.remove("hidden");
  editorView.classList.remove("active");
  editorView.classList.add("hidden");
  hud.setEditLevelActive(false);
  requestAnimationFrame(() => {
    if (lastState) {
      renderer.fit(canvas.parentElement?.clientWidth ?? 1280,
                   canvas.parentElement?.clientHeight ?? 800,
                   lastState);
    }
  });
}

function showEditorView(): void {
  editorOpen = true;
  playView.classList.remove("active");
  playView.classList.add("hidden");
  editorView.classList.add("active");
  editorView.classList.remove("hidden");
  hud.setEditLevelActive(true);
  editor.fit();
  editor.draw();
  // Escape cierra el editor
  setTimeout(() => editorCanvas.focus(), 50);
}

editLevelBtn.addEventListener("click", () => {
  if (editorOpen) showPlayView();
  else showEditorView();
});

editorCloseBtn.addEventListener("click", () => showPlayView());

document.addEventListener("keydown", (ev) => {
  if (ev.key === "Escape" && editorOpen) {
    ev.preventDefault();
    showPlayView();
  }
});

/* ---------- Renderer loop (60 FPS, solo re-dibuja ultimo estado) ---------- */

function tick(now: number): void {
  if (!editorOpen && lastState) {
    renderer.fit(
      canvas.parentElement?.clientWidth ?? 1280,
      canvas.parentElement?.clientHeight ?? 800,
      lastState
    );
    renderer.render(lastState, now);
  } else if (!lastState) {
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
  client.loadLevel(levelId).then(() => showPlayView());
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
  flashEditorStatus(ok ? "Nivel guardado" : "Error al guardar");
});

document.getElementById("editor-load")?.addEventListener("click", async () => {
  const idInput = document.getElementById("editor-load-id") as HTMLInputElement;
  const id = idInput.value.trim();
  if (!id) {
    flashEditorStatus("Especifica un ID");
    return;
  }
  const ok = await editor.loadById(id);
  flashEditorStatus(ok ? `Cargado: ${id}` : `No se encontro ${id}`);
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
  if (lastState && !editorOpen) {
    renderer.fit(canvas.parentElement?.clientWidth ?? 1280,
                 canvas.parentElement?.clientHeight ?? 800,
                 lastState);
  }
  if (editorOpen) {
    editor.fit();
    editor.draw();
  }
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
      renderer.fit(canvas.parentElement?.clientWidth ?? 1280,
                   canvas.parentElement?.clientHeight ?? 800,
                   state);
      if (!renderLoopRunning) {
        renderLoopRunning = true;
        requestAnimationFrame(tick);
      }
    }
  });
  editor.draw();
  showPlayView();
});
