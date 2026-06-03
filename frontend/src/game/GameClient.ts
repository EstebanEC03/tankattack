/* ============================================================
 * GameClient.ts
 * Cliente HTTP + WebSocket para el backend Java.
 *
 *  - GET  /levels             -> LevelSummary[]
 *  - GET  /levels/{id}        -> LevelDefinition
 *  - POST /levels             -> crea (body = LevelDefinition)
 *  - PUT  /levels/{id}        -> reemplaza
 *  - DELETE /levels/{id}      -> elimina
 *  - POST /game/start         -> inicia partida
 *  - POST /game/restart       -> reinicia
 *  - POST /game/pause         -> alterna pausa
 *  - POST /game/level/{id}    -> carga nivel
 *  - POST /game/next          -> siguiente nivel
 *  - GET  /game/state         -> snapshot actual
 *  - WS   /ws/game            -> {"type":"GAME_STATE", payload}
 * ============================================================ */

import type {
  ClientMessage,
  GameState,
  LevelDefinition,
  LevelSummary,
  WsEnvelope,
} from "./types";

export type StateListener = (state: GameState) => void;
export type ConnectionListener = (status: ConnectionStatus) => void;
export type ConnectionStatus = "connecting" | "connected" | "disconnected";

export interface GameClientConfig {
  baseUrl?: string;
  wsUrl?: string;
  reconnectDelayMs?: number;
  maxReconnectDelayMs?: number;
}

const DEFAULT_CONFIG: Required<GameClientConfig> = {
  baseUrl: "http://localhost:7070",
  wsUrl: "ws://localhost:7070/ws/game",
  reconnectDelayMs: 800,
  maxReconnectDelayMs: 8000,
};

export class GameClient {
  private readonly config: Required<GameClientConfig>;
  private ws: WebSocket | null = null;
  private stateListeners: StateListener[] = [];
  private connectionListeners: ConnectionListener[] = [];
  private lastState: GameState | null = null;
  private connectionStatus: ConnectionStatus = "disconnected";
  private reconnectAttempts = 0;
  private reconnectTimer: number | null = null;
  private manualClose = false;

  constructor(config: GameClientConfig = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config };
  }

  /* ---------- WebSocket ---------- */

  connect(): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) return;
    this.manualClose = false;
    this.openSocket();
  }

  disconnect(): void {
    this.manualClose = true;
    if (this.reconnectTimer != null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.ws) {
      try { this.ws.close(); } catch { /* noop */ }
      this.ws = null;
    }
    this.setStatus("disconnected");
  }

  isConnected(): boolean {
    return this.ws != null && this.ws.readyState === WebSocket.OPEN;
  }

  onState(listener: StateListener): () => void {
    this.stateListeners.push(listener);
    if (this.lastState) listener(this.lastState);
    return () => {
      this.stateListeners = this.stateListeners.filter((l) => l !== listener);
    };
  }

  onConnection(listener: ConnectionListener): () => void {
    this.connectionListeners.push(listener);
    listener(this.connectionStatus);
    return () => {
      this.connectionListeners = this.connectionListeners.filter((l) => l !== listener);
    };
  }

  send(message: ClientMessage): void {
    if (!this.isConnected()) {
      // Cola simple: si el socket no esta abierto, ignora.
      // El backend rechaza la accion de todos modos si la
      // partida no esta corriendo, asi que perder un mensaje
      // es aceptable.
      return;
    }
    this.ws!.send(JSON.stringify(message));
  }

  /* ---------- REST helpers ---------- */

  async getState(): Promise<GameState | null> {
    return this.fetchJson<GameState>("/game/state");
  }

  async startGame(): Promise<void> {
    await this.postEmpty("/game/start");
  }

  async restartGame(): Promise<void> {
    await this.postEmpty("/game/restart");
  }

  async pauseGame(): Promise<void> {
    await this.postEmpty("/game/pause");
  }

  async loadLevel(levelId: string): Promise<void> {
    await this.postEmpty(`/game/level/${encodeURIComponent(levelId)}`);
  }

  async nextLevel(): Promise<boolean> {
    try {
      const res = await fetch(`${this.config.baseUrl}/game/next`, { method: "POST" });
      return res.ok;
    } catch {
      return false;
    }
  }

  async listLevels(): Promise<LevelSummary[]> {
    const res = await this.fetchJson<LevelSummary[]>("/levels");
    return res ?? [];
  }

  async getLevel(id: string): Promise<LevelDefinition | null> {
    return this.fetchJson<LevelDefinition>(`/levels/${encodeURIComponent(id)}`);
  }

  async saveLevel(level: LevelDefinition): Promise<LevelDefinition | null> {
    try {
      const res = await fetch(`${this.config.baseUrl}/levels`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(level),
      });
      if (!res.ok) return null;
      return (await res.json()) as LevelDefinition;
    } catch {
      return null;
    }
  }

  async deleteLevel(id: string): Promise<boolean> {
    try {
      const res = await fetch(
        `${this.config.baseUrl}/levels/${encodeURIComponent(id)}`,
        { method: "DELETE" }
      );
      return res.ok;
    } catch {
      return false;
    }
  }

  /* ---------- Internals ---------- */

  private openSocket(): void {
    this.setStatus("connecting");
    try {
      this.ws = new WebSocket(this.config.wsUrl);
    } catch (e) {
      this.scheduleReconnect();
      return;
    }

    this.ws.onopen = () => {
      this.reconnectAttempts = 0;
      this.setStatus("connected");
    };

    this.ws.onmessage = (ev) => {
      try {
        const env = JSON.parse(ev.data) as WsEnvelope;
        if (env.type === "GAME_STATE" && env.payload) {
          this.lastState = env.payload;
          for (const l of this.stateListeners) {
            try { l(env.payload); } catch (e) { /* listener error: ignored */ }
          }
        }
      } catch {
        // ignore malformed messages
      }
    };

    this.ws.onerror = () => {
      // onclose will follow; reconnect logic lives there
    };

    this.ws.onclose = () => {
      this.ws = null;
      this.setStatus("disconnected");
      if (!this.manualClose) this.scheduleReconnect();
    };
  }

  private scheduleReconnect(): void {
    if (this.manualClose) return;
    if (this.reconnectTimer != null) return;
    const delay = Math.min(
      this.config.maxReconnectDelayMs,
      this.config.reconnectDelayMs * Math.pow(1.5, this.reconnectAttempts)
    );
    this.reconnectAttempts++;
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      this.openSocket();
    }, delay);
  }

  private setStatus(status: ConnectionStatus): void {
    if (this.connectionStatus === status) return;
    this.connectionStatus = status;
    for (const l of this.connectionListeners) l(status);
  }

  private async postEmpty(path: string): Promise<void> {
    try {
      await fetch(`${this.config.baseUrl}${path}`, { method: "POST" });
    } catch {
      // network failure: el caller no necesita manejar
    }
  }

  private async fetchJson<T>(path: string): Promise<T | null> {
    try {
      const res = await fetch(`${this.config.baseUrl}${path}`);
      if (!res.ok) return null;
      return (await res.json()) as T;
    } catch {
      return null;
    }
  }
}
