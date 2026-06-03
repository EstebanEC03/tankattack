package com.tankattack.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tankattack.level.LevelLoader;
import com.tankattack.model.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio en memoria de niveles personalizados. Permite
 * que el editor del frontend guarde y recupere niveles sin
 * persistencia en disco (suficiente para la entrega).
 */
public final class LevelRepository {

    private static final Logger log = LoggerFactory.getLogger(LevelRepository.class);

    private final Map<String, Level> levels = new ConcurrentHashMap<>();
    private final LevelLoader loader = new LevelLoader();
    private final ObjectMapper mapper = new ObjectMapper();

    public LevelRepository() {
        // Sembrar con los niveles predefinidos.
        for (Level l : loader.predefinedAll()) {
            levels.put(l.getId(), l);
        }
    }

    public List<Level> list() {
        return new ArrayList<>(levels.values());
    }

    public Optional<Level> get(String id) {
        return Optional.ofNullable(levels.get(id));
    }

    public Level save(Level level) {
        if (level == null) throw new IllegalArgumentException("level null");
        levels.put(level.getId(), level);
        log.info("Nivel guardado: {}", level.getId());
        return level;
    }

    public Level saveFromJson(String json) throws Exception {
        Level level = loader.fromJson(json);
        return save(level);
    }

    public boolean delete(String id) {
        return levels.remove(id) != null;
    }

    public String toJson(Level level) throws Exception {
        return loader.toJson(level);
    }

    public ObjectMapper mapper() {
        return mapper;
    }
}
