package core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patch.CardPortraitPatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PackManager {
    private static final Logger logger = LogManager.getLogger(PackManager.class.getName());
    
    public static final String PACK_ROOT_DIR = "mods/diy_the_spire/";
    
    private static PackManager instance;
    
    private List<String> packNames = new ArrayList<>();
    private String currentPack = "";
    
    public static PackManager getInstance() {
        if (instance == null) {
            instance = new PackManager();
        }
        return instance;
    }
    
    private PackManager() {
    }
    
    public void scanPacks() {
        logger.info("PackManager: Scanning packs in " + PACK_ROOT_DIR);
        packNames.clear();
        
        FileHandle rootDir = Gdx.files.local(PACK_ROOT_DIR);
        if (!rootDir.exists()) {
            logger.info("PackManager: Root directory does not exist, creating it");
            rootDir.mkdirs();
            return;
        }
        
        for (FileHandle child : rootDir.list()) {
            if (child.isDirectory()) {
                String name = child.name();
                if (!name.equals("masks") && !name.equals("images")) {
                    packNames.add(name);
                    logger.info("PackManager: Found pack: " + name);
                }
            }
        }
        
        Collections.sort(packNames);
        logger.info("PackManager: Found " + packNames.size() + " packs");
        
        String savedPack = PackConfig.loadCurrentPack();
        if (!savedPack.isEmpty() && packNames.contains(savedPack)) {
            currentPack = savedPack;
        } else if (!packNames.isEmpty()) {
            currentPack = packNames.get(0);
        } else {
            currentPack = "";
        }
        logger.info("PackManager: Current pack set to: " + currentPack);
    }
    
    public List<String> getPackNames() {
        return new ArrayList<>(packNames);
    }
    
    public String getCurrentPack() {
        return currentPack;
    }
    
    public boolean isEmpty() {
        return currentPack == null || currentPack.isEmpty();
    }
    
    public void setCurrentPack(String packName) {
        if (packNames.contains(packName) || packName.isEmpty()) {
            currentPack = packName;
            PackConfig.saveCurrentPack(packName);
            TextureManager.getInstance().clearCache();
            CardPortraitPatch.clearCache();
            logger.info("PackManager: Switched to pack: " + packName);
        } else {
            logger.warn("PackManager: Pack not found: " + packName);
        }
    }
    
    public String createPack(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        name = name.trim();
        String sanitized = sanitizePackName(name);
        
        FileHandle packDir = Gdx.files.local(PACK_ROOT_DIR + sanitized);
        if (packDir.exists()) {
            logger.warn("PackManager: Pack already exists: " + sanitized);
            return null;
        }
        
        packDir.mkdirs();
        createPackStructure(packDir);
        
        packNames.add(sanitized);
        Collections.sort(packNames);
        
        logger.info("PackManager: Created pack: " + sanitized);
        return sanitized;
    }
    
    private void createPackStructure(FileHandle packDir) {
        String[] colors = {"red", "green", "blue", "purple", "colorless", "curse", "status"};
        String[] types = {"attack", "skill", "power"};
        
        for (String color : colors) {
            for (String type : types) {
                packDir.child(color).child(type).mkdirs();
                packDir.child(color).child(type).child("small").mkdirs();
            }
        }
    }
    
    public boolean renamePack(String oldName, String newName) {
        if (!packNames.contains(oldName)) {
            return false;
        }
        
        newName = sanitizePackName(newName);
        if (newName.isEmpty() || packNames.contains(newName)) {
            return false;
        }
        
        FileHandle oldDir = Gdx.files.local(PACK_ROOT_DIR + oldName);
        FileHandle newDir = Gdx.files.local(PACK_ROOT_DIR + newName);
        
        if (!oldDir.exists() || newDir.exists()) {
            return false;
        }
        
        try {
            copyDirectory(oldDir, newDir);
            oldDir.deleteDirectory();
            
            packNames.remove(oldName);
            packNames.add(newName);
            Collections.sort(packNames);
            
            if (currentPack.equals(oldName)) {
                currentPack = newName;
                PackConfig.saveCurrentPack(newName);
            }
            
            logger.info("PackManager: Renamed pack " + oldName + " to " + newName);
            return true;
        } catch (Exception e) {
            logger.error("PackManager: Failed to rename pack", e);
            return false;
        }
    }
    
    public boolean copyPack(String sourceName, String targetName) {
        if (!packNames.contains(sourceName)) {
            return false;
        }
        
        targetName = sanitizePackName(targetName);
        if (targetName.isEmpty() || packNames.contains(targetName)) {
            return false;
        }
        
        FileHandle sourceDir = Gdx.files.local(PACK_ROOT_DIR + sourceName);
        FileHandle targetDir = Gdx.files.local(PACK_ROOT_DIR + targetName);
        
        if (!sourceDir.exists() || targetDir.exists()) {
            return false;
        }
        
        try {
            copyDirectory(sourceDir, targetDir);
            
            packNames.add(targetName);
            Collections.sort(packNames);
            
            logger.info("PackManager: Copied pack " + sourceName + " to " + targetName);
            return true;
        } catch (Exception e) {
            logger.error("PackManager: Failed to copy pack", e);
            return false;
        }
    }
    
    public boolean deletePack(String name) {
        if (!packNames.contains(name)) {
            return false;
        }
        
        FileHandle packDir = Gdx.files.local(PACK_ROOT_DIR + name);
        if (!packDir.exists()) {
            return false;
        }
        
        try {
            packDir.deleteDirectory();
            
            packNames.remove(name);
            
            if (currentPack.equals(name)) {
                if (!packNames.isEmpty()) {
                    currentPack = packNames.get(0);
                } else {
                    currentPack = "";
                }
                PackConfig.saveCurrentPack(currentPack);
            }
            
            logger.info("PackManager: Deleted pack: " + name);
            return true;
        } catch (Exception e) {
            logger.error("PackManager: Failed to delete pack", e);
            return false;
        }
    }
    
    private String sanitizePackName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
    
    private void copyDirectory(FileHandle source, FileHandle target) {
        target.mkdirs();
        for (FileHandle child : source.list()) {
            if (child.isDirectory()) {
                copyDirectory(child, target.child(child.name()));
            } else {
                child.copyTo(target.child(child.name()));
            }
        }
    }
}