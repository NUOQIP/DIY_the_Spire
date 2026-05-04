package patch;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import core.PackManager;
import core.TextureManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CardPortraitPatch {
    private static final Logger logger = LogManager.getLogger(CardPortraitPatch.class.getName());
    
    private static Set<String> notFoundCache = new HashSet<>();
    private static Set<String> replacedCards = new HashSet<>();
    private static Map<String, TextureAtlas.AtlasRegion> originalPortraits = new HashMap<>();
    private static boolean originalsSaved = false;
    
    public static void saveAllOriginalPortraits() {
        if (originalsSaved) return;
        originalsSaved = true;
        
        for (AbstractCard card : CardLibrary.getAllCards()) {
            if (card.portrait != null) {
                originalPortraits.put(card.cardID + "_false", card.portrait);
                originalPortraits.put(card.cardID + "_true", card.portrait);
            }
        }
    }
    
    @SpirePatch(
            clz = AbstractCard.class,
            method = "renderPortrait"
    )
    public static class RenderPortraitPatch {
        @SpirePrefixPatch
        public static void Prefix(AbstractCard __instance) {
            if (PackManager.getInstance().isEmpty()) {
                return;
            }
            
            String key = __instance.cardID + "_" + __instance.upgraded;
            if (notFoundCache.contains(key)) {
                return;
            }
            
            TextureAtlas.AtlasRegion region = TextureManager.getInstance().getSmallAtlasRegion(__instance.cardID, __instance.upgraded);
            if (region != null) {
                __instance.portrait = region;
                replacedCards.add(key);
            } else {
                notFoundCache.add(key);
                if (replacedCards.remove(key)) {
                    TextureAtlas.AtlasRegion original = originalPortraits.get(key);
                    if (original != null) {
                        __instance.portrait = original;
                        logger.debug("RenderPortrait: Restored original for {}", key);
                    }
                }
            }
        }
    }
    
    public static void clearCache() {
        notFoundCache.clear();
        logger.debug("CardPortraitPatch: Cache cleared");
    }
    
    public static void clearCardCache(String cardId) {
        notFoundCache.remove(cardId + "_false");
        notFoundCache.remove(cardId + "_true");
        replacedCards.remove(cardId + "_false");
        replacedCards.remove(cardId + "_true");
        logger.debug("CardPortraitPatch: Cleared cache for {}", cardId);
    }
    
    public static void invalidateNotFound(String cardId) {
        notFoundCache.remove(cardId + "_false");
        notFoundCache.remove(cardId + "_true");
    }
}