package patch;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import core.CardRegistry;
import core.PackManager;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SingleCardViewPatch {
    private static final Logger logger = LogManager.getLogger(SingleCardViewPatch.class.getName());
    
    private static Texture loadBigImageDirect(String cardId, boolean upgraded) {
        if (PackManager.getInstance().isEmpty()) return null;
        
        CardRegistry.CardInfo info = CardRegistry.getCardInfo(cardId);
        if (info == null) return null;
        
        String suffix = upgraded ? "_p" : "";
        String packName = PackManager.getInstance().getCurrentPack();
        String path = PackManager.PACK_ROOT_DIR + packName + "/" +
                      info.getColorFolderName() + "/" + info.getTypeFolderName() + "/" +
                      info.getFileName() + suffix + ".png";
        
        FileHandle file = Gdx.files.local(path);
        if (file.exists()) {
            try {
                return new Texture(file);
            } catch (Exception e) {
                logger.warn("Failed to load big image for {} upgrade={}", cardId, upgraded, e);
            }
        }
        
        if (upgraded) {
            return loadBigImageDirect(cardId, false);
        }
        
        return null;
    }
    
    @SpirePatch(
            clz = SingleCardViewPopup.class,
            method = "loadPortraitImg"
    )
    public static class LoadPortraitPatch {
        @SpirePostfixPatch
        public static void Postfix(SingleCardViewPopup __instance) {
            if (PackManager.getInstance().isEmpty()) return;
            
            AbstractCard card = ReflectionHacks.getPrivate(__instance, SingleCardViewPopup.class, "card");
            if (card == null) return;
            
            Texture customTexture = loadBigImageDirect(card.cardID, SingleCardViewPopup.isViewingUpgrade);
            
            if (customTexture != null) {
                Texture currentTexture = ReflectionHacks.getPrivate(__instance, SingleCardViewPopup.class, "portraitImg");
                if (currentTexture != null && currentTexture != customTexture) {
                    currentTexture.dispose();
                }
                ReflectionHacks.setPrivate(__instance, SingleCardViewPopup.class, "portraitImg", customTexture);
            }
        }
    }
    
    @SpirePatch(
            clz = SingleCardViewPopup.class,
            method = "updateUpgradePreview"
    )
    public static class UpdateUpgradePatch {
        @SpireInsertPatch(
                locator = Locator.class,
                localvars = {"card", "portraitImg"}
        )
        public static void Insert(SingleCardViewPopup __instance, AbstractCard card, @ByRef Texture[] portraitImg) {
            if (PackManager.getInstance().isEmpty()) return;
            
            boolean viewingUpgrade = !SingleCardViewPopup.isViewingUpgrade;
            Texture customTexture = loadBigImageDirect(card.cardID, viewingUpgrade);
            
            if (customTexture != null) {
                portraitImg[0] = customTexture;
            }
        }
        
        private static class Locator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
                Matcher matcher = new Matcher.FieldAccessMatcher(SingleCardViewPopup.class, "isViewingUpgrade");
                return LineFinder.findInOrder(ctMethodToPatch, matcher);
            }
        }
    }
    
    @SpirePatch(
            clz = SingleCardViewPopup.class,
            method = "renderPortrait"
    )
    public static class RenderPortraitOverridePatch {
        private static Texture savedImg;
        
        @SpirePrefixPatch
        public static void Prefix(SingleCardViewPopup __instance) {
            if (PackManager.getInstance().isEmpty()) return;
            
            AbstractCard card = ReflectionHacks.getPrivate(__instance, SingleCardViewPopup.class, "card");
            if (card == null) return;
            
            Texture custom = loadBigImageDirect(card.cardID, SingleCardViewPopup.isViewingUpgrade);
            if (custom != null) {
                savedImg = ReflectionHacks.getPrivate(__instance, SingleCardViewPopup.class, "portraitImg");
                ReflectionHacks.setPrivate(__instance, SingleCardViewPopup.class, "portraitImg", custom);
            }
        }
        
        @SpirePostfixPatch
        public static void Postfix(SingleCardViewPopup __instance) {
            if (savedImg != null) {
                ReflectionHacks.setPrivate(__instance, SingleCardViewPopup.class, "portraitImg", savedImg);
                savedImg = null;
            }
        }
    }
}