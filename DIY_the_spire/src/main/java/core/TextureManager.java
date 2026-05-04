package core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TextureManager {
    private static final Logger logger = LogManager.getLogger(TextureManager.class.getName());
    
    public static final int BIG_WIDTH = 500;
    public static final int BIG_HEIGHT = 380;
    public static final int SMALL_WIDTH = 250;
    public static final int SMALL_HEIGHT = 190;
    
    private static TextureManager instance;
    
    private Map<String, Texture> bigTextureCache = new HashMap<>();
    private Map<String, TextureRegion> smallTextureCache = new HashMap<>();
    private Map<String, TextureAtlas.AtlasRegion> smallRegionCache = new HashMap<>();
    private Set<String> notFoundCache = new HashSet<>();
    
    public static TextureManager getInstance() {
        if (instance == null) {
            instance = new TextureManager();
        }
        return instance;
    }
    
    private TextureManager() {
    }
    
    public Texture getCardImage(String cardId, boolean upgraded, boolean big) {
        if (PackManager.getInstance().isEmpty()) {
            return null;
        }
        
        String key = cardId + "_" + upgraded;
        if (notFoundCache.contains(key)) {
            return null;
        }
        
        CardRegistry.CardInfo info = CardRegistry.getCardInfo(cardId);
        if (info == null) {
            notFoundCache.add(key);
            return null;
        }
        
        String suffix = upgraded ? "_p" : "";
        String fileName = info.getFileName() + suffix + ".png";
        
        String basePath = PackManager.PACK_ROOT_DIR + PackManager.getInstance().getCurrentPack() + "/" +
                          info.getColorFolderName() + "/" + info.getTypeFolderName() + "/";
        
        if (big) {
            String fullPath = basePath + fileName;
            Texture cached = bigTextureCache.get(key);
            if (cached != null) {
                return cached;
            }
            
            FileHandle file = Gdx.files.local(fullPath);
            if (file.exists()) {
                try {
                    Texture tex = new Texture(file);
                    bigTextureCache.put(key, tex);
                    logger.debug("TextureManager: Loaded big texture for " + cardId);
                    return tex;
                } catch (Exception e) {
                    logger.error("TextureManager: Failed to load texture " + fullPath, e);
                    notFoundCache.add(key);
                    return null;
                }
            }
            
            if (upgraded) {
                Texture normalTex = getCardImage(cardId, false, true);
                if (normalTex != null) {
                    bigTextureCache.put(key, normalTex);
                    return normalTex;
                }
            }
            
            notFoundCache.add(key);
            return null;
        } else {
            String smallPath = basePath + "small/" + fileName;
            TextureRegion cached = smallTextureCache.get(key);
            if (cached != null) {
                return cached.getTexture();
            }
            
            FileHandle smallFile = Gdx.files.local(smallPath);
            if (smallFile.exists()) {
                try {
                    Texture tex = new Texture(smallFile);
                    TextureRegion region = new TextureRegion(tex);
                    smallTextureCache.put(key, region);
                    logger.debug("TextureManager: Loaded small texture for " + cardId);
                    return tex;
                } catch (Exception e) {
                    logger.error("TextureManager: Failed to load small texture " + smallPath, e);
                }
            }
            
            Texture bigTex = getCardImage(cardId, upgraded, true);
            if (bigTex != null) {
                TextureRegion smallRegion = scaleTextureRegion(bigTex, SMALL_WIDTH, SMALL_HEIGHT);
                smallTextureCache.put(key, smallRegion);
                return smallRegion.getTexture();
            }
            
            notFoundCache.add(key);
            return null;
        }
    }
    
    public TextureRegion getSmallTextureRegion(String cardId, boolean upgraded) {
        String key = cardId + "_" + upgraded;
        
        TextureRegion cached = smallTextureCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        Texture tex = getCardImage(cardId, upgraded, false);
        if (tex != null) {
            TextureRegion region = new TextureRegion(tex);
            smallTextureCache.put(key, region);
            return region;
        }
        
        return null;
    }
    
    public TextureAtlas.AtlasRegion getSmallAtlasRegion(String cardId, boolean upgraded) {
        String key = cardId + "_" + upgraded;
        
        TextureAtlas.AtlasRegion cached = smallRegionCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        Texture tex = getCardImage(cardId, upgraded, false);
        if (tex != null) {
            TextureAtlas.AtlasRegion region = new TextureAtlas.AtlasRegion(tex, 0, 0, tex.getWidth(), tex.getHeight());
            region.packedWidth = tex.getWidth();
            region.packedHeight = tex.getHeight();
            region.originalWidth = tex.getWidth();
            region.originalHeight = tex.getHeight();
            smallRegionCache.put(key, region);
            return region;
        }
        
        return null;
    }
    
    public void clearCache() {
        logger.info("TextureManager: Clearing all caches");
        
        for (Texture tex : bigTextureCache.values()) {
            if (tex != null) {
                tex.dispose();
            }
        }
        
        bigTextureCache.clear();
        smallTextureCache.clear();
        smallRegionCache.clear();
        notFoundCache.clear();
        
        logger.info("TextureManager: Cache cleared");
    }
    
    public void clearCardCache(String cardId) {
        String keyNormal = cardId + "_false";
        String keyUpgraded = cardId + "_true";
        
        Texture tex1 = bigTextureCache.remove(keyNormal);
        Texture tex2 = bigTextureCache.remove(keyUpgraded);
        TextureRegion region1 = smallTextureCache.remove(keyNormal);
        TextureRegion region2 = smallTextureCache.remove(keyUpgraded);
        
        if (tex1 != null) tex1.dispose();
        if (tex2 != null) tex2.dispose();
        if (region1 != null && region1.getTexture() != null) region1.getTexture().dispose();
        if (region2 != null && region2.getTexture() != null) region2.getTexture().dispose();
        
        smallRegionCache.remove(keyNormal);
        smallRegionCache.remove(keyUpgraded);
        
        notFoundCache.remove(keyNormal);
        notFoundCache.remove(keyUpgraded);
        
        logger.debug("TextureManager: Cleared cache for card " + cardId);
    }
    
    public void invalidateNotFound(String cardId) {
        notFoundCache.remove(cardId + "_false");
        notFoundCache.remove(cardId + "_true");
    }
    
    public static TextureRegion scaleTextureRegion(Texture source, int targetWidth, int targetHeight) {
        if (source.getWidth() == targetWidth && source.getHeight() == targetHeight) {
            return new TextureRegion(source);
        }
        
        Pixmap sourcePixmap = textureToPixmap(source);
        Pixmap scaledPixmap = scalePixmap(sourcePixmap, targetWidth, targetHeight);
        Texture scaledTexture = new Texture(scaledPixmap);
        
        sourcePixmap.dispose();
        scaledPixmap.dispose();
        
        return new TextureRegion(scaledTexture);
    }
    
    public static Texture scaleTexture(Texture source, int targetWidth, int targetHeight) {
        if (source.getWidth() == targetWidth && source.getHeight() == targetHeight) {
            return source;
        }
        
        Pixmap sourcePixmap = textureToPixmap(source);
        Pixmap scaledPixmap = scalePixmap(sourcePixmap, targetWidth, targetHeight);
        Texture scaledTexture = new Texture(scaledPixmap);
        
        sourcePixmap.dispose();
        scaledPixmap.dispose();
        
        return scaledTexture;
    }
    
    private static Pixmap textureToPixmap(Texture texture) {
        texture.getTextureData().prepare();
        return texture.getTextureData().consumePixmap();
    }
    
    public static Pixmap scalePixmap(Pixmap source, int targetWidth, int targetHeight) {
        Pixmap target = new Pixmap(targetWidth, targetHeight, source.getFormat());
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        
        for (int y = 0; y < targetHeight; y++) {
            int srcY = y * srcH / targetHeight;
            for (int x = 0; x < targetWidth; x++) {
                int srcX = x * srcW / targetWidth;
                target.drawPixel(x, y, source.getPixel(srcX, srcY));
            }
        }
        return target;
    }
    
    public static BufferedImage pixmapToBufferedImage(Pixmap pixmap) {
        int w = pixmap.getWidth();
        int h = pixmap.getHeight();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = pixmap.getPixel(x, y);
                int r = (pixel >> 24) & 0xFF;
                int g = (pixel >> 16) & 0xFF;
                int b = (pixel >> 8) & 0xFF;
                int a = pixel & 0xFF;
                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return image;
    }
}