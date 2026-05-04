package core;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CardRegistry {
    private static final Logger logger = LogManager.getLogger(CardRegistry.class.getName());
    
    private static Map<String, CardInfo> cardInfoMap = new HashMap<>();
    private static Map<AbstractCard.CardColor, Set<String>> colorToCardIds = new HashMap<>();
    private static Map<AbstractCard.CardType, Set<String>> typeToCardIds = new HashMap<>();
    
    public static class CardInfo {
        private String cardId;
        private AbstractCard.CardColor color;
        private AbstractCard.CardType type;
        private String assetUrl;
        
        public CardInfo(String cardId, AbstractCard.CardColor color, AbstractCard.CardType type, String assetUrl) {
            this.cardId = cardId;
            this.color = color;
            this.type = type;
            this.assetUrl = assetUrl;
        }
        
        public String getCardId() { return cardId; }
        public AbstractCard.CardColor getColor() { return color; }
        public AbstractCard.CardType getType() { return type; }
        public String getAssetUrl() { return assetUrl; }
        
        public String getColorFolderName() {
            if (color == AbstractCard.CardColor.RED) return "red";
            if (color == AbstractCard.CardColor.GREEN) return "green";
            if (color == AbstractCard.CardColor.BLUE) return "blue";
            if (color == AbstractCard.CardColor.PURPLE) return "purple";
            if (color == AbstractCard.CardColor.COLORLESS) return "colorless";
            if (color == AbstractCard.CardColor.CURSE) return "curse";
            return color.name().toLowerCase();
        }
        
        public String getTypeFolderName() {
            if (type == AbstractCard.CardType.ATTACK) return "attack";
            if (type == AbstractCard.CardType.SKILL) return "skill";
            if (type == AbstractCard.CardType.POWER) return "power";
            if (type == AbstractCard.CardType.STATUS) return "status";
            if (type == AbstractCard.CardType.CURSE) return "curse";
            return type.name().toLowerCase();
        }
        
        public String getFileName() {
            return cardId.toLowerCase().replace(" ", "_").replace(":", "_");
        }
    }
    
    public static void registerAllCards() {
        logger.info("CardRegistry: Registering all cards from CardLibrary");
        cardInfoMap.clear();
        colorToCardIds.clear();
        typeToCardIds.clear();
        
        for (AbstractCard card : CardLibrary.getAllCards()) {
            register(card);
        }
        
        logger.info("CardRegistry: Registered " + cardInfoMap.size() + " cards");
    }
    
    public static void register(AbstractCard card) {
        String cardId = card.cardID;
        AbstractCard.CardColor color = card.color;
        AbstractCard.CardType type = card.type;
        
        String assetUrl = null;
        try {
            assetUrl = (String) basemod.ReflectionHacks.getPrivate(card, AbstractCard.class, "assetUrl");
        } catch (Exception e) {
            assetUrl = "";
        }
        
        CardInfo info = new CardInfo(cardId, color, type, assetUrl);
        cardInfoMap.put(cardId, info);
        
        colorToCardIds.computeIfAbsent(color, k -> new HashSet<>()).add(cardId);
        typeToCardIds.computeIfAbsent(type, k -> new HashSet<>()).add(cardId);
        
        logger.debug("CardRegistry: Registered card " + cardId + " (color=" + color + ", type=" + type + ")");
    }
    
    public static CardInfo getCardInfo(String cardId) {
        return cardInfoMap.get(cardId);
    }
    
    public static Set<String> getCardIdsByColor(AbstractCard.CardColor color) {
        return colorToCardIds.getOrDefault(color, new HashSet<>());
    }
    
    public static Set<String> getCardIdsByType(AbstractCard.CardType type) {
        return typeToCardIds.getOrDefault(type, new HashSet<>());
    }
    
    public static Set<AbstractCard.CardColor> getAllColors() {
        return colorToCardIds.keySet();
    }
    
    public static boolean hasCard(String cardId) {
        return cardInfoMap.containsKey(cardId);
    }
    
    public static int getTotalCardCount() {
        return cardInfoMap.size();
    }
}