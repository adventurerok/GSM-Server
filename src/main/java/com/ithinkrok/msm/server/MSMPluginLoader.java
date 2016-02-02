package com.ithinkrok.msm.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by paul on 02/02/16.
 */
public class MSMPluginLoader {

    private static final Logger log = LogManager.getLogger(MSMPluginLoader.class);
    private static final Map<Class<? extends MSMServerPlugin>, ConfigurationSection> configLookup = new HashMap<>();

    private final Path pluginDirectory = Paths.get("plugins");

    private List<MSMServerPlugin> plugins = new ArrayList<>();

    public static void configurePlugin(MSMServerPlugin plugin) throws ReflectiveOperationException {
        Class<MSMServerPlugin> pluginClass = MSMServerPlugin.class;

        //Ensure that we haven't already been called on this plugin
        Field configuredField = pluginClass.getDeclaredField("configured");
        configuredField.setAccessible(true);
        if (configuredField.getBoolean(plugin)) throw new RuntimeException("Plugin is already configured");
        configuredField.setBoolean(plugin, true);

        //Set the plugin yml field
        ConfigurationSection pluginYml = configLookup.get(plugin.getClass());
        Field pluginYmlField = pluginClass.getDeclaredField("pluginYml");
        pluginYmlField.setAccessible(true);
        pluginYmlField.set(plugin, pluginYml);

        //Set the plugin name field
        Field nameField = pluginClass.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(plugin, pluginYml.getString("name"));

    }

    public void loadPlugins() {
        List<Path> jarPaths = findPluginJarPaths();

        if (jarPaths.isEmpty()) {
            log.warn("No plugin jars found in the plugin folder");
            return;
        }

        List<URL> pluginLoaderUrls = new ArrayList<>();
        List<FileConfiguration> pluginYmls = new ArrayList<>();

        for (Path path : jarPaths) {
            try {
                //If the plugin is invalid (throws exception) it will be skipped
                pluginYmls.add(loadPluginYml(path));

                pluginLoaderUrls.add(new URL("jar:file:" + path.toUri().getPath() + "!/"));
            } catch (IOException e) {
                log.warn("Error while loading plugin jar: " + path, e);
            }
        }

        URL[] pluginLoaderUrlsArray = new URL[pluginLoaderUrls.size()];
        pluginLoaderUrls.toArray(pluginLoaderUrlsArray);

        URLClassLoader pluginLoader = new URLClassLoader(pluginLoaderUrlsArray);

        for (FileConfiguration pluginYml : pluginYmls) {
            try {
                loadPluginObject(pluginLoader, pluginYml);
            } catch (Exception e) {
                log.warn("Error loading plugin: " + pluginYml.getString("name"), e);
            }
        }
    }

    private List<Path> findPluginJarPaths() {
        List<Path> jarPaths = new ArrayList<>();

        if (!Files.isDirectory(pluginDirectory)) {
            log.warn("The plugin directory does not exist or is not a directory: " + pluginDirectory.toAbsolutePath());
            return jarPaths;
        }

        try (DirectoryStream<Path> paths = Files.newDirectoryStream(pluginDirectory)) {
            for (Path path : paths) {
                if (Files.isDirectory(path) || !path.endsWith(".jar")) continue;
                jarPaths.add(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jarPaths;
    }

    private FileConfiguration loadPluginYml(Path path) throws IOException {
        //Open the jar as a file system
        try (FileSystem jar = createZipFileSystem(path)) {

            //The plugin yml path is msm_plugin.yml
            Path pluginYml = jar.getPath("/msm_plugin.yml");

            if (!Files.exists(pluginYml)) throw new IOException("Plugin jar does not contain plugin.yml");

            YamlConfiguration config = new YamlConfiguration();

            try (Reader reader = Files.newBufferedReader(pluginYml)) {
                config.load(reader);

                return config;
            } catch (InvalidConfigurationException e) {
                throw new IOException("plugin.yml is invalid", e);
            }
        }

    }

    private void loadPluginObject(ClassLoader pluginLoader, FileConfiguration pluginYml)
            throws ReflectiveOperationException {
        String mainClassName = pluginYml.getString("main");

        Class<?> anyClass = pluginLoader.loadClass(mainClassName);
        Class<? extends MSMServerPlugin> pluginClass = anyClass.asSubclass(MSMServerPlugin.class);

        configLookup.put(pluginClass, pluginYml);

        String pluginName = pluginYml.getString("name");
        String pluginVersion = pluginYml.getString("version");
        log.info("Loading plugin " + pluginName + " version " + pluginVersion);

        MSMServerPlugin plugin = pluginClass.newInstance();

        plugins.add(plugin);
    }

    private FileSystem createZipFileSystem(Path zipFile) throws IOException {
        //Absolute URI
        final URI uri = URI.create("jar:file:" + zipFile.toUri().getPath());

        return FileSystems.newFileSystem(uri, new HashMap<String, Object>());
    }


}
