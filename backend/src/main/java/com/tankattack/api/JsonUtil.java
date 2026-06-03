package com.tankattack.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * ObjectMapper compartido por todo el backend para
 * serializar JSON. Configurado para no generar campos
 * nulos y usar orden de insercion.
 */
public final class JsonUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private JsonUtil() {}
}
