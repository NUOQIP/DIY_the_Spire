package processing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.megacrit.cardcrawl.cards.AbstractCard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class MaskManager {
    private static final Logger logger = LogManager.getLogger(MaskManager.class.getName());
    
    private static Map<AbstractCard.CardType, Pixmap> masks = new HashMap<>();
    
    private static final String ATTACK_MASK = "masks/AttackMask.png";
    private static final String SKILL_MASK = "masks/SkillMask.png";
    private static final String POWER_MASK = "masks/PowerMask.png";
    
    public static void loadMasks() {
        logger.info("MaskManager: Loading masks");
        
        loadMask(AbstractCard.CardType.ATTACK, ATTACK_MASK);
        loadMask(AbstractCard.CardType.SKILL, SKILL_MASK);
        loadMask(AbstractCard.CardType.POWER, POWER_MASK);
        
        logger.info("MaskManager: Loaded " + masks.size() + " masks");
    }
    
    private static void loadMask(AbstractCard.CardType type, String path) {
        FileHandle file = Gdx.files.local(path);
        if (!file.exists()) {
            file = Gdx.files.internal(path);
        }
        
        if (file.exists()) {
            try {
                Pixmap mask = new Pixmap(file);
                masks.put(type, mask);
                logger.info("MaskManager: Loaded mask for " + type + " from " + path);
            } catch (Exception e) {
                logger.error("MaskManager: Failed to load mask from " + path, e);
            }
        } else {
            logger.warn("MaskManager: Mask file not found: " + path);
        }
    }
    
    public static Pixmap getMask(AbstractCard.CardType type) {
        Pixmap mask = masks.get(type);
        
        if (mask == null && type != AbstractCard.CardType.STATUS && type != AbstractCard.CardType.CURSE) {
            logger.warn("MaskManager: Mask not found for type " + type + ", using SKILL mask as fallback");
            mask = masks.get(AbstractCard.CardType.SKILL);
        }
        
        return mask;
    }
    
    public static boolean hasMask(AbstractCard.CardType type) {
        return masks.containsKey(type);
    }
    
    public static void dispose() {
        for (Pixmap mask : masks.values()) {
            if (mask != null) {
                mask.dispose();
            }
        }
        masks.clear();
        logger.info("MaskManager: Disposed all masks");
    }
}