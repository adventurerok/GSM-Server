package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.server.MSMServerPlugin;
import com.ithinkrok.msm.server.command.CommandInfo;
import com.ithinkrok.msm.server.permission.PermissionInfo;
import com.ithinkrok.util.FIleUtil;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.InvalidConfigException;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.config.YamlConfigIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

/**
 * Created by paul on 02/02/16.
 */
public class MSMPluginLoader {

    private static final Logger log = LogManager.getLogger(MSMPluginLoader.class);
    private static final Map<Class<? extends MSMServerPlugin>, Config> configLookup = new HashMap<>();

    private static final Path pluginDirectory = Paths.get("plugins");

    public static void configurePlugin(MSMServerPlugin plugin) throws ReflectiveOperationException {
        Class<MSMServerPlugin> pluginClass = MSMServerPlugin.class;

        //Ensure that we haven't already been called on this plugin
        Field configuredField = pluginClass.getDeclaredField("configured");
        configuredField.setAccessible(true);
        if (configuredField.getBoolean(plugin)) throw new RuntimeException("Plugin is already configured");
        configuredField.setBoolean(plugin, true);

        //Set the plugin yml field
        Config pluginYml = configLookup.get(plugin.getClass());
        Field pluginYmlField = pluginClass.getDeclaredField("pluginYml");
        pluginYmlField.setAccessible(true);
        pluginYmlField.set(plugin, pluginYml);

        //Set the plugin name field
        Field nameField = pluginClass.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(plugin, pluginYml.getString("name"));

        Path pluginDirectory = plugin.getDataDirectory();
        if(!Files.exists(pluginDirectory)) {
            try {
                Files.createDirectory(pluginDirectory);
            } catch (IOException e) {
                log.warn("Error while creating plugin data directory: " + e);
            }
        }
    }

    /**
     * Loads all the valid plugins in the plugins directory, logging errors about any invalid plugins it finds.
     *
     * @return A list of valid plugins loaded from the plugins directory.
     */
    public List<MSMServerPlugin> loadPlugins() {
        List<MSMServerPlugin> plugins = new ArrayList<>();
        List<Path> jarPaths = findPluginJarPaths();

        if (jarPaths.isEmpty()) {
            log.warn("No plugin jars found in the plugin folder");
            return plugins;
        }

        List<URL> pluginLoaderUrls = new ArrayList<>();
        List<Config> pluginYmls = new ArrayList<>();

        //Load all the plugin ymls
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

        //Load the plugin main classes from the plugin ymls
        for (Config pluginYml : pluginYmls) {
            try {
                plugins.add(loadPluginObject(pluginLoader, pluginYml));
            } catch (Exception e) {
                log.warn("Error loading plugin: " + pluginYml.getString("name"), e);
            }
        }

        return plugins;
    }

    private List<Path> findPluginJarPaths() {
        List<Path> jarPaths = new ArrayList<>();

        if (!Files.isDirectory(pluginDirectory)) {
            log.warn("The plugin directory does not exist or is not a directory: " + pluginDirectory.toAbsolutePath());
            return jarPaths;
        }

        //** is required to match across directory boundaries
        PathMatcher jarMatcher = FileSystems.getDefault().getPathMatcher("glob:**.jar");

        try (DirectoryStream<Path> paths = Files.newDirectoryStream(pluginDirectory)) {
            for (Path path : paths) {
                if (Files.isDirectory(path) || !jarMatcher.matches(path)) continue;
                jarPaths.add(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jarPaths;
    }

    private Config loadPluginYml(Path path) throws IOException {
        //Open the jar as a file system
        try (FileSystem jar = FIleUtil.createZipFileSystem(path)) {

            //The plugin yml path is msm_plugin.yml
            Path pluginYmlPath = jar.getPath("/msm_plugin.yml");

            if (!Files.exists(pluginYmlPath)) throw new IOException("Plugin jar does not contain msm_plugin.yml");


            try (Reader reader = Files.newBufferedReader(pluginYmlPath)) {
                Config pluginYml = YamlConfigIO.loadToConfig(reader, new MemoryConfig('/'));

                //Loop over the keys required in the msm_plugin.yml and check they are there
                for (String required : new String[]{"name", "main", "version"}) {
                    if (pluginYml.contains(required)) continue;

                    throw new InvalidConfigException("msm_plugin.yml missing required key: " + required);
                }

                return pluginYml;
            } catch (InvalidConfigException e) {
                throw new IOException("plugin.yml is invalid", e);
            }
        }

    }

    private MSMServerPlugin loadPluginObject(ClassLoader pluginLoader, Config pluginYml)
            throws ReflectiveOperationException {
        String mainClassName = pluginYml.getString("main");

        //Get the main class specified in the plugin yml
        Class<?> anyClass = pluginLoader.loadClass(mainClassName);

        //Make it a subclass of MSMServerPlugin. Throws an exception if it doesn't work
        Class<? extends MSMServerPlugin> pluginClass = anyClass.asSubclass(MSMServerPlugin.class);

        configLookup.put(pluginClass, pluginYml);

        return pluginClass.newInstance();
    }

    public List<MSMServerPlugin> enablePlugins(List<MSMServerPlugin> plugins) {
        List<MSMServerPlugin> enabledPlugins = new ArrayList<>();

        Map<String, MSMServerPlugin> pluginsByName = new HashMap<>();

        for (MSMServerPlugin plugin : plugins) {
            pluginsByName.put(plugin.getName(), plugin);
        }

        for (MSMServerPlugin plugin : plugins) {
            Set<MSMServerPlugin> loading = new HashSet<>();


            enablePlugin(plugin, loading, pluginsByName, enabledPlugins);
        }

        return enabledPlugins;
    }

    private boolean enablePlugin(MSMServerPlugin plugin, Set<MSMServerPlugin> loading,
                                 Map<String, MSMServerPlugin> pluginsByName, List<MSMServerPlugin> enabledPlugins) {

        //Make sure the plugin is not already enabled
        if (plugin.isEnabled()) return true;

        //Make sure we are not already enabling the plugin (cyclic dependency)
        if (!loading.add(plugin)) {
            log.warn("Plugin " + plugin.getName() + " has cyclic dependencies");
            return false;
        }

        try {

            //Enable dependencies first
            for (String dependencyName : plugin.getDependencies()) {
                MSMServerPlugin dependency = pluginsByName.get(dependencyName);

                if (dependency == null) {
                    log.warn("Plugin " + plugin.getName() + " is missing dependency: " + dependencyName);
                    return false;
                }

                if (!enablePlugin(dependency, loading, pluginsByName, enabledPlugins)) return false;
            }

            for(String dependencyName : plugin.getSoftDependencies()) {
                MSMServerPlugin dependency = pluginsByName.get(dependencyName);

                if(dependency == null) continue;

                enablePlugin(dependency, loading, pluginsByName, enabledPlugins);
            }

            log.info("Enabling plugin " + plugin.getName() + " v" + plugin.getVersion());

            setupPluginCommandsAndPermissions(plugin);

            plugin.onEnable();

            Field enabledField = MSMServerPlugin.class.getDeclaredField("enabled");
            enabledField.setAccessible(true);
            enabledField.setBoolean(plugin, true);

        } catch (Exception e) {
            log.warn("Failed to enable plugin " + plugin.getName(), e);
            return false;
        } finally {
            loading.remove(plugin);
        }

        enabledPlugins.add(plugin);

        return true;
    }

    private void setupPluginCommandsAndPermissions(MSMServerPlugin plugin) {
        for(CommandInfo commandInfo : plugin.getCommands().values()) {
            plugin.getServer().registerCommand(commandInfo);
        }

        for(PermissionInfo permissionInfo : plugin.getPermissions()) {
            plugin.getServer().registerPermission(permissionInfo);
        }
    }


}
