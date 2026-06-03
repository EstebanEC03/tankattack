package com.tankattack;

import com.tankattack.api.ApiServer;
import com.tankattack.editor.LevelRepository;
import com.tankattack.engine.GameEngine;
import com.tankattack.prolog.PrologService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada del backend. Inicializa Prolog, el
 * motor del juego, el repositorio de niveles y el servidor
 * HTTP/WebSocket.
 *
 * Propiedades del sistema (opcionales):
 *   -Dtank.port=7070   puerto HTTP (default 7070)
 *   -Dtank.level=nivel1   nivel inicial (default nivel1)
 */
public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        configureJplNativeLibrary();
        int port = Integer.parseInt(System.getProperty("tank.port",
                System.getenv().getOrDefault("TANK_PORT", "7070")));
        String initialLevel = System.getProperty("tank.level",
                System.getenv().getOrDefault("TANK_LEVEL", "nivel1"));

        log.info("Iniciando Tank-Attack backend en puerto {}", port);

        PrologService prolog = new PrologService();
        prolog.initialize();

        GameEngine engine = new GameEngine(prolog);

        LevelRepository levels = new LevelRepository();

        // Crear el ApiServer (registra el broadcaster como
        // listener del engine) ANTES de cargar el nivel, para
        // que el estado inicial quede cacheado en el broadcaster
        // y los clientes WebSocket lo reciban al conectarse.
        ApiServer server = new ApiServer(engine, levels);
        engine.loadLevel(initialLevel);

        server.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Apagando backend...");
            engine.shutdown();
            server.stop();
            prolog.shutdown();
        }, "tank-shutdown"));

        log.info("Backend listo. Nivel inicial: {}", initialLevel);
    }

    /**
     * Asegura que la libreria nativa de JPL (libjpl.so) sea
     * localizable. Si no se ha pasado -Djpl.library.path o
     * -Djava.library.path, intenta ubicarla en rutas
     * conocidas de SWI-Prolog en Linux/macOS/Windows.
     */
    private static void configureJplNativeLibrary() {
        if (System.getProperty("jpl.library.path") != null
                || System.getProperty("java.library.path") != null
                && System.getProperty("java.library.path").contains("swi-prolog")) {
            return;
        }
        String[] candidates = {
                "/usr/lib/swi-prolog/lib/x86_64-linux",
                "/usr/lib/swi-prolog/lib",
                "/opt/homebrew/lib/swi-prolog/lib",
                "/usr/local/lib/swi-prolog/lib",
        };
        for (String path : candidates) {
            java.io.File f = new java.io.File(path);
            if (f.isDirectory() && f.listFiles((d, n) -> n.startsWith("libjpl")) != null) {
                System.setProperty("jpl.library.path", path);
                log.info("JPL native library configurada en {}", path);
                return;
            }
        }
        log.warn("No se encontro libjpl.so. Use -Djpl.library.path=<ruta> al ejecutar");
    }
}
