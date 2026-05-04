package core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PackConfig {
    private static final Logger logger = LogManager.getLogger(PackConfig.class.getName());
    private static final String CONFIG_FILE = "mods/diy_the_spire/config.json";
    private static final Json json = new Json();

    public static String loadCurrentPack() {
        FileHandle file = Gdx.files.local(CONFIG_FILE);
        if (file.exists()) {
            try {
                ConfigData data = json.fromJson(ConfigData.class, file);
                if (data != null && data.currentPack != null) {
                    return data.currentPack;
                }
            } catch (Exception e) {
                logger.error("PackConfig: Failed to load config", e);
            }
        }
        return "";
    }
    
    public static void saveCurrentPack(String packName) {
        try {
            ConfigData data = new ConfigData();
            data.currentPack = packName;
            FileHandle file = Gdx.files.local(CONFIG_FILE);
            file.writeString(json.toJson(data), false);
        } catch (Exception e) {
            logger.error("PackConfig: Failed to save config", e);
        }
    }
    
    private static class ConfigData {
        public String currentPack;
    }
}