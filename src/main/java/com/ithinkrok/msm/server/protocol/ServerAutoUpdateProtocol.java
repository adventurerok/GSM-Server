package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.util.FIleUtil;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.config.YamlConfigIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by paul on 05/02/16.
 */
public class ServerAutoUpdateProtocol extends ServerUpdateBaseProtocol {

    private final Logger log = LogManager.getLogger(ServerAutoUpdateProtocol.class);


    public ServerAutoUpdateProtocol(Path serverResourcePath) {
        super("MSMAutoUpdate", serverResourcePath);
    }

    @Override
    protected Config getDefaultResourceConfig(Path path) {
        if(!path.getFileName().toString().toLowerCase().endsWith(".jar")) return null;

        Config config = super.getDefaultResourceConfig(path);

        try (FileSystem jarFile = FIleUtil.createZipFileSystem(path)) {
            Path pluginYmlPath = jarFile.getPath("/plugin.yml");

            if (!Files.exists(pluginYmlPath)) return null;

            Config pluginYml = YamlConfigIO.loadToConfig(pluginYmlPath, new MemoryConfig('/'));

            String name = pluginYml.getString("name");
            if (name == null) return null;

            config.set("resource_name", name);
        } catch (IOException e) {
            log.warn("Failed to update version info for plugin: " + path, e);
            return null;
        }

        return config;
    }
}
