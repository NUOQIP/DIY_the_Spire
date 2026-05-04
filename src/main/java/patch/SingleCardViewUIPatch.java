package patch;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import modcore.ModInitialize;
import ui.PackEditorUI;

public class SingleCardViewUIPatch {
    
    private static SingleCardViewPopup currentPopup = null;
    
    @SpirePatch(
            clz = SingleCardViewPopup.class,
            method = "open",
            paramtypez = {AbstractCard.class}
    )
    public static class OpenPatch {
        @SpirePostfixPatch
        public static void Postfix(SingleCardViewPopup __instance, AbstractCard card) {
            currentPopup = __instance;
            PackEditorUI.getInstance().setPopup(__instance);
            ModInitialize.getInstance().setCurrentPopup(__instance);
        }
    }
    
    @SpirePatch(
            clz = SingleCardViewPopup.class,
            method = "close"
    )
    public static class ClosePatch {
        @SpirePrefixPatch
        public static void Prefix(SingleCardViewPopup __instance) {
            ReflectionHacks.setPrivate(__instance, SingleCardViewPopup.class, "portraitImg", (Object) null);
            currentPopup = null;
            ModInitialize.getInstance().setCurrentPopup(null);
        }
    }
    
    @SpirePatch(
            clz = SingleCardViewPopup.class,
            method = "render"
    )
    public static class RenderPatch {
        @SpirePostfixPatch
        public static void Postfix(SingleCardViewPopup __instance, SpriteBatch sb) {
            if (__instance.isOpen) {
                PackEditorUI.getInstance().render(sb);
            }
        }
    }
    
    @SpirePatch(
            clz = SingleCardViewPopup.class,
            method = "update"
    )
    public static class UpdatePatch {
        @SpirePrefixPatch
        public static void Prefix(SingleCardViewPopup __instance) {
            if (__instance.isOpen) {
                PackEditorUI.getInstance().setPopup(__instance);
                PackEditorUI.getInstance().blockClicks();
            }
        }
        
        @SpirePostfixPatch
        public static void Postfix(SingleCardViewPopup __instance) {
            if (__instance.isOpen) {
                PackEditorUI.getInstance().update();
            }
        }
    }
    
    public static SingleCardViewPopup getCurrentPopup() {
        return currentPopup;
    }
}