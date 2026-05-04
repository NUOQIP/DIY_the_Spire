package modcore;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import core.CardRegistry;
import core.PackConfig;
import core.PackManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patch.CardPortraitPatch;
import processing.MaskManager;
import ui.UIUtils;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

@SpireInitializer
public class ModInitialize implements PostInitializeSubscriber {
    public static final Logger logger = LogManager.getLogger(ModInitialize.class.getName());
    
    public static final String MOD_ID = "diy_the_spire";
    public static final String MOD_NAME = "DIY the Spire";
    public static final String AUTHOR = "DIY";
    public static final String DESCRIPTION = "自定义卡图编辑器MOD";
    public static final String VERSION = "1.0.0";
    
    private static ModInitialize instance;
    private SingleCardViewPopup currentPopup;
    
    public static void initialize() {
        logger.info("========================= DIY_THE_SPIRE INIT =========================");
        instance = new ModInitialize();
        BaseMod.subscribe(instance);
        logger.info("================================================================");
    }
    
    public ModInitialize() {
    }
    
    @Override
    public void receivePostInitialize() {
        logger.info("DIY_the_spire: PostInitialize started");
        
        Pixmap pixmap = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        Texture badgeTexture = new Texture(pixmap);
        pixmap.dispose();
        
        BaseMod.registerModBadge(badgeTexture, MOD_NAME, AUTHOR, DESCRIPTION, null);
        
        try {
            UIUtils.init();
            PackConfig.loadCurrentPack();
            MaskManager.loadMasks();
            CardRegistry.registerAllCards();
            CardPortraitPatch.saveAllOriginalPortraits();
            PackManager.getInstance().scanPacks();
        } catch (Exception e) {
            logger.error("DIY_the_spire: Initialization failed", e);
        }
        
        logger.info("DIY_the_spire: PostInitialize completed");
    }
    
    public static ModInitialize getInstance() {
        return instance;
    }
    
    public void setCurrentPopup(SingleCardViewPopup popup) {
        this.currentPopup = popup;
    }
    
    public SingleCardViewPopup getCurrentPopup() {
        return currentPopup;
    }
}