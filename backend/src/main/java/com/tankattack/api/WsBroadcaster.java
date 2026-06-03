package com.tankattack.api;

import com.tankattack.model.GameState;
import com.tankattack.api.JsonUtil;
import io.javalin.websocket.WsContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona las conexiones WebSocket activas y envia
 * snapshots del estado a todos los clientes conectados
 * cada vez que el {@link com.tankattack.engine.GameEngine}
 * lo notifica.
 */
public class WsBroadcaster implements com.tankattack.engine.StateListener {

    private final Set<WsContext> connections = ConcurrentHashMap.newKeySet();

    public void register(WsContext ctx) {
        connections.add(ctx);
    }

    public void unregister(WsContext ctx) {
        connections.remove(ctx);
    }

    public int connectionCount() {
        return connections.size();
    }

    @Override
    public void onStateChange(GameState state) {
        if (connections.isEmpty()) return;
        Map<String, Object> payload = GameStateDto.toMap(state);
        Map<String, Object> envelope = new java.util.LinkedHashMap<>();
        envelope.put("type", "GAME_STATE");
        envelope.put("payload", payload);
        String json;
        try {
            json = toJson(envelope);
        } catch (Exception ex) {
            return;
        }
        for (WsContext ctx : connections) {
            try {
                if (ctx.session.isOpen()) {
                    ctx.send(json);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void sendTo(WsContext ctx, Object payload) {
        try {
            if (ctx.session.isOpen()) {
                Map<String, Object> envelope = new java.util.LinkedHashMap<>();
                envelope.put("type", "CUSTOM");
                envelope.put("payload", payload);
                ctx.send(JsonUtil.MAPPER.writeValueAsString(envelope));
            }
        } catch (Exception ignored) {
        }
    }

    private String toJson(Object payload) throws Exception {
        return JsonUtil.MAPPER.writeValueAsString(payload);
    }
}
