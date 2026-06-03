package com.tankattack.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tankattack.editor.LevelRepository;
import com.tankattack.engine.GameEngine;
import com.tankattack.engine.PlayerInput;
import com.tankattack.model.Direction;
import com.tankattack.model.GameState;
import com.tankattack.model.Level;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor HTTP + WebSocket construido sobre Javalin.
 * Expone:
 *
 *   REST:
 *     GET  /                      -> healthcheck/info
 *     POST /game/start            -> inicia/reanuda la partida
 *     POST /game/restart          -> reinicia el nivel actual
 *     POST /game/pause            -> alterna pausa
 *     POST /game/level/{id}       -> carga y arranca un nivel
 *     POST /game/next             -> avanza al siguiente nivel
 *     GET  /game/state            -> snapshot del estado
 *     GET  /levels                -> lista de niveles disponibles
 *     GET  /levels/{id}           -> JSON de un nivel
 *     POST /levels                -> crea/sobrescribe un nivel
 *     PUT  /levels/{id}           -> actualiza un nivel
 *     DELETE /levels/{id}         -> elimina un nivel
 *
 *   WebSocket /ws/game:
 *     Cliente -> servidor: PlayerInputMessage JSON
 *     Servidor -> cliente: { "type": "GAME_STATE", "payload": ... }
 */
public final class ApiServer {

    private static final Logger log = LoggerFactory.getLogger(ApiServer.class);

    private final GameEngine engine;
    private final LevelRepository levels;
    private final WsBroadcaster broadcaster;
    private Javalin app;

    public ApiServer(GameEngine engine, LevelRepository levels) {
        this.engine = engine;
        this.levels = levels;
        this.broadcaster = new WsBroadcaster();
    }

    public WsBroadcaster broadcaster() { return broadcaster; }

    public int start(int port) {
        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(JsonUtil.MAPPER));
            config.showJavalinBanner = false;
            config.plugins.enableCors(cors -> cors.add(it -> {
                it.reflectClientOrigin = true;
                it.allowCredentials = false;
            }));
        });

        registerRoutes();
        engine.addStateListener(broadcaster);
        app.start(port);
        log.info("Servidor Javalin escuchando en puerto {}", port);
        return port;
    }

    public void stop() {
        if (app != null) app.stop();
    }

    private void registerRoutes() {
        app.get("/", ctx -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", "tank-attack-backend");
            info.put("version", "1.0.0");
            info.put("running", engine.getState() != null && engine.getState().isRunning());
            info.put("wsClients", broadcaster.connectionCount());
            ctx.json(info);
        });

        app.post("/game/start", ctx -> {
            engine.startGame();
            ctx.json(status());
        });

        app.post("/game/restart", ctx -> {
            engine.restart();
            ctx.json(status());
        });

        app.post("/game/pause", ctx -> {
            engine.handlePlayerInput(PlayerInput.pause());
            ctx.json(status());
        });

        app.post("/game/level/{id}", ctx -> {
            String id = ctx.pathParam("id");
            engine.loadLevel(id);
            engine.startGame();
            ctx.json(status());
        });

        app.post("/game/next", ctx -> {
            boolean ok = engine.advanceToNextLevel();
            ctx.status(ok ? HttpStatus.OK : HttpStatus.NOT_FOUND);
            ctx.json(status());
        });

        app.get("/game/state", ctx -> ctx.json(GameStateDto.toMap(engine.getState())));

        app.get("/levels", ctx -> {
            List<Map<String, Object>> list = levels.list().stream()
                    .map(ApiServer::levelSummary)
                    .toList();
            ctx.json(list);
        });

        app.get("/levels/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Level level = levels.get(id).orElse(null);
            if (level == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(Map.of("error", "Nivel no encontrado", "id", id));
                return;
            }
            try {
                ctx.json(JsonUtil.MAPPER.readTree(levels.toJson(level)));
            } catch (Exception e) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(Map.of("error", e.getMessage()));
            }
        });

        app.post("/levels", ctx -> saveLevel(ctx, null));
        app.put("/levels/{id}", ctx -> saveLevel(ctx, ctx.pathParam("id")));

        app.delete("/levels/{id}", ctx -> {
            String id = ctx.pathParam("id");
            boolean removed = levels.delete(id);
            ctx.status(removed ? HttpStatus.OK : HttpStatus.NOT_FOUND);
            ctx.json(Map.of("removed", removed, "id", id));
        });

        app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

        // WebSocket
        app.ws("/ws/game", ws -> {
            ws.onConnect(broadcaster::register);
            ws.onClose(broadcaster::unregister);
            ws.onMessage(ctx -> {
                try {
                    PlayerInputMessage msg = JsonUtil.MAPPER.readValue(
                            ctx.message(), PlayerInputMessage.class);
                    if (msg == null || msg.type == null) return;
                    handleWsInput(msg);
                } catch (Exception ex) {
                    log.warn("WS mensaje invalido: {}", ex.getMessage());
                }
            });
        });
    }

    private void handleWsInput(PlayerInputMessage msg) {
        switch (msg.type) {
            case "PLAYER_MOVE" -> {
                Direction d = Direction.fromJsonName(msg.direction).orElse(null);
                if (d != null) engine.handlePlayerInput(PlayerInput.move(d));
            }
            case "PLAYER_SHOOT" -> engine.handlePlayerInput(PlayerInput.shoot());
            case "PAUSE_GAME"   -> engine.handlePlayerInput(PlayerInput.pause());
            case "RESTART"      -> engine.handlePlayerInput(PlayerInput.restart());
            case "LOAD_LEVEL"   -> {
                if (msg.levelId != null) {
                    engine.loadLevel(msg.levelId);
                    engine.startGame();
                }
            }
            case "NEXT_LEVEL"   -> engine.advanceToNextLevel();
            default -> log.debug("WS tipo desconocido: {}", msg.type);
        }
    }

    private void saveLevel(Context ctx, String forcedId) {
        try {
            JsonNode body = JsonUtil.MAPPER.readTree(ctx.body());
            if (forcedId != null) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) body).put("id", forcedId);
            }
            Level level = engine.levelLoader().fromJson(body);
            levels.save(level);
            ctx.status(HttpStatus.CREATED);
            ctx.json(JsonUtil.MAPPER.readTree(levels.toJson(level)));
        } catch (Exception e) {
            log.warn("Error al guardar nivel: {}", e.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", e.getMessage()));
        }
    }

    private static Map<String, Object> levelSummary(Level level) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", level.getId());
        m.put("width", level.getWidth());
        m.put("height", level.getHeight());
        m.put("objectives", level.getObjectives().size());
        m.put("enemies", level.getEnemies().size());
        return m;
    }

    private Map<String, Object> status() {
        Map<String, Object> s = new LinkedHashMap<>();
        GameState st = engine.getState();
        s.put("ok", true);
        s.put("running", st != null && st.isRunning());
        s.put("paused", st != null && st.isPaused());
        s.put("gameOver", st != null && st.isGameOver());
        s.put("levelCompleted", st != null && st.isLevelCompleted());
        s.put("level", st == null ? 0 : st.getCurrentLevelNumber());
        return s;
    }
}
