package com.tankattack.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mensajes que el frontend puede enviar al backend.
 * La estructura es flexible: segun el {@code type} se
 * esperan campos diferentes.
 *
 * Ejemplos:
 * <pre>
 *   { "type": "PLAYER_MOVE", "direction": "UP" }
 *   { "type": "PLAYER_SHOOT" }
 *   { "type": "PAUSE_GAME" }
 *   { "type": "RESTART" }
 *   { "type": "LOAD_LEVEL", "levelId": "nivel2" }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerInputMessage {

    public String type;
    public String direction;
    public String levelId;
}
