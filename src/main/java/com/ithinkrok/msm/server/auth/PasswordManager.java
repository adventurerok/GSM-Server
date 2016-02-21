package com.ithinkrok.msm.server.auth;

import com.ithinkrok.util.config.BinaryConfigIO;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Created by paul on 21/02/16.
 */
public class PasswordManager {

    private final Logger log = LogManager.getLogger(PasswordManager.class);

    private final Config passwordsConfig;
    private final Path passwordsPath;

    private final Object lock = new Object();

    public PasswordManager(Path passwordsPath) {
        this.passwordsPath = passwordsPath;

        Config tempConfig;
        try {
            tempConfig = BinaryConfigIO.loadConfig(this.passwordsPath);
        } catch (IOException e) {
            log.warn("Failed to load passwords config. Creating new one", e);
            tempConfig = new MemoryConfig();

            savePasswords();
        }

        this.passwordsConfig = tempConfig;
    }

    private void savePasswords() {
        synchronized (lock) {
            try {
                BinaryConfigIO.saveConfig(passwordsPath, passwordsConfig);
            } catch (IOException e) {
                log.error("Failed to save passwords config", e);
            }
        }
    }


    public boolean loginServer(String serverName, byte[] password) {
        if(!passwordsConfig.contains(serverName)) return false;

        Config serverConfig = passwordsConfig.getConfigOrNull(serverName);

        byte[] salt = serverConfig.getByteArray("salt");
        int n = serverConfig.getInt("n");
        int r = serverConfig.getInt("r");
        int p = serverConfig.getInt("p");

        byte[] expected = serverConfig.getByteArray("password");

        byte[] actual = PasswordHasher.hash(password, salt, n, r, p);

        return Arrays.equals(expected, actual);
    }

    public void registerServer(String serverName, byte[] password) {
        PasswordHasher.ScryptParameters parameters = new PasswordHasher.ScryptParameters();

        parameters.generateParameters();

        byte[] hash = PasswordHasher.hash(password, parameters);

        Config serverConfig = new MemoryConfig();

        serverConfig.set("salt", parameters.salt);
        serverConfig.set("n", parameters.n);
        serverConfig.set("r", parameters.r);
        serverConfig.set("p", parameters.p);
        serverConfig.set("password", hash);

        passwordsConfig.set(serverName, serverConfig);

        savePasswords();
    }
}
