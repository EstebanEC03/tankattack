package com.tankattack.api;

import com.tankattack.model.GameState;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gestiona las conexiones WebSocket activas y envia
 * snapshots del estado a todos los clientes conectados
 * cada vez que el {@link com.tankattack.engine.GameEngine}
 * lo notifica.
 *
 * <p>Cuando un cliente se conecta, se le envia
 * inmediatamente el ultimo estado conocido (si existe)
 * para que reciba la situacion actual sin esperar al
 * siguiente tick.</p>
 */
public class WsBroadcaster implements com.tankattack.engine.StateListener {

    private static final Logger log = LoggerFactory.getLogger(WsBroadcaster.class);

    private final Set<WsContext> connections = ConcurrentHashMap.newKeySet();
    private final AtomicReference<GameState> lastState = new AtomicReference<>();

    public void register(WsContext ctx) {
        connections.add(ctx);
        GameState snapshot = lastState.get();
        if (snapshot != null) {
            sendTo(ctx, snapshot);
        }
    }

    public void unregister(WsContext ctx) {
        connections.remove(ctx);
    }

    public int connectionCount() {
        return connections.size();
    }

    @Override
    public void onStateChange(GameState state) {
        lastState.set(state);
        if (connections.isEmpty()) return;
        for (WsContext ctx : connections) {
            sendTo(ctx, state);
        }
    }

    private void sendTo(WsContext ctx, GameState state) {
        try {
            if (!ctx.session.isOpen()) {
                connections.remove(ctx);
                return;
            }
            Map<String, Object> payload = GameStateDto.toMap(state);
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("type", "GAME_STATE");
            envelope.put("payload", payload);
            ctx.send(JsonUtil.MAPPER.writeValueAsString(envelope));
        } catch (Exception ex) {
            log.debug("Fallo envio WS: {}", ex.getMessage());
        }
    }
}
