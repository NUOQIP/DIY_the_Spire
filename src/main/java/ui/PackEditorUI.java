package ui;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import core.CardRegistry;
import core.PackManager;
import core.TextureManager;
import localization.LocalizationKeys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patch.CardPortraitPatch;
import processing.ImageProcessor;

import javax.swing.*;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PackEditorUI {
    private static final Logger logger = LogManager.getLogger(PackEditorUI.class.getName());
    
    private static PackEditorUI instance;
    public static PackEditorUI getInstance() {
        if (instance == null) instance = new PackEditorUI();
        return instance;
    }
    
    private SingleCardViewPopup currentPopup;
    private AbstractCard currentCard;
    
    private float panelX, panelY, panelWidth;
    private float rowHeight, smallRowHeight, padding;
    
    private Hitbox panelHb;
    private Hitbox packDropdownHb;
    private Hitbox createPackHb;
    private Hitbox renamePackHb;
    private Hitbox copyPackHb;
    private Hitbox deletePackHb;
    private Hitbox openFolderHb;
    private Hitbox makeNormalHb;
    private Hitbox makeUpgradeHb;
    
    private int selectedPackIndex = 0;
    private boolean dropdownOpen = false;
    private List<Hitbox> dropdownItemHbs = new ArrayList<>();
    
    private boolean imageProcessingActive = false;
    private boolean makingUpgradeVersion = false;
    private boolean initialized = false;
    private boolean savedClick = false;
    
    private static final com.badlogic.gdx.graphics.Color
            BG_COLOR = new com.badlogic.gdx.graphics.Color(0.08f, 0.08f, 0.10f, 0.92f),
            SECTION_BG = new com.badlogic.gdx.graphics.Color(0.12f, 0.12f, 0.15f, 1f),
            ACCENT = new com.badlogic.gdx.graphics.Color(0.45f, 0.55f, 0.95f, 1f),
            ACCENT_DIM = new com.badlogic.gdx.graphics.Color(0.30f, 0.38f, 0.65f, 1f),
            TEXT_PRIMARY = new com.badlogic.gdx.graphics.Color(0.90f, 0.90f, 0.92f, 1f),
            TEXT_SECONDARY = new com.badlogic.gdx.graphics.Color(0.55f, 0.55f, 0.60f, 1f),
            TEXT_MUTED = new com.badlogic.gdx.graphics.Color(0.35f, 0.35f, 0.40f, 1f),
            HOVER_BRIGHT = new com.badlogic.gdx.graphics.Color(0.20f, 0.20f, 0.24f, 1f),
            DANGER = new com.badlogic.gdx.graphics.Color(0.85f, 0.30f, 0.30f, 1f),
            DANGER_HOVER = new com.badlogic.gdx.graphics.Color(0.65f, 0.22f, 0.22f, 1f);
    
    private PackEditorUI() {}
    
    private void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        
        panelWidth = 320f * Settings.scale;
        panelX = 40f * Settings.scale;
        panelY = 500f * Settings.scale;
        rowHeight = 40f * Settings.scale;
        smallRowHeight = 32f * Settings.scale;
        padding = 14f * Settings.scale;
        
        float smallButtonWidth = (panelWidth - padding * 3) / 2;
        float buttonWidth = panelWidth - padding * 2;
        float buttonY = panelY - padding * 2.5f;
        
        panelHb = new Hitbox(panelX, panelY - 340f * Settings.scale, panelWidth, 340f * Settings.scale);
        
        packDropdownHb = new Hitbox(panelX + padding, buttonY - rowHeight, buttonWidth, rowHeight);
        buttonY -= rowHeight + padding;
        
        createPackHb = new Hitbox(panelX + padding, buttonY - smallRowHeight, smallButtonWidth, smallRowHeight);
        renamePackHb = new Hitbox(panelX + padding + smallButtonWidth + padding, buttonY - smallRowHeight, smallButtonWidth, smallRowHeight);
        buttonY -= smallRowHeight + padding;
        
        copyPackHb = new Hitbox(panelX + padding, buttonY - smallRowHeight, smallButtonWidth, smallRowHeight);
        deletePackHb = new Hitbox(panelX + padding + smallButtonWidth + padding, buttonY - smallRowHeight, smallButtonWidth, smallRowHeight);
        buttonY -= smallRowHeight + padding;
        
        openFolderHb = new Hitbox(panelX + padding, buttonY - smallRowHeight, buttonWidth, smallRowHeight);
        buttonY -= smallRowHeight + padding;
        
        makeNormalHb = new Hitbox(panelX + padding, buttonY - rowHeight, buttonWidth, rowHeight);
        buttonY -= rowHeight + padding;
        
        makeUpgradeHb = new Hitbox(panelX + padding, buttonY - rowHeight, buttonWidth, rowHeight);
    }
    
    public void setPopup(SingleCardViewPopup popup) {
        ensureInitialized();
        this.currentPopup = popup;
        this.currentCard = ReflectionHacks.getPrivate(popup, SingleCardViewPopup.class, "card");
        syncSelectedPackIndex();
    }
    
    private void syncSelectedPackIndex() {
        String current = PackManager.getInstance().getCurrentPack();
        if (!current.isEmpty()) {
            List<String> packs = PackManager.getInstance().getPackNames();
            int idx = packs.indexOf(current);
            if (idx >= 0) selectedPackIndex = idx;
        }
    }
    
    public void blockClicks() {
        ensureInitialized();
        if (currentPopup == null || !currentPopup.isOpen) {
            savedClick = false;
            return;
        }
        savedClick = InputHelper.justClickedLeft;
        if (isMouseOverUI()) InputHelper.justClickedLeft = false;
    }
    
    public void update() {
        ensureInitialized();
        if (currentPopup == null || !currentPopup.isOpen) {
            dropdownOpen = false;
            savedClick = false;
            return;
        }
        if (imageProcessingActive) return;
        
        panelHb.update();
        packDropdownHb.update();
        createPackHb.update();
        renamePackHb.update();
        copyPackHb.update();
        deletePackHb.update();
        openFolderHb.update();
        makeNormalHb.update();
        makeUpgradeHb.update();
        
        if (dropdownOpen) {
            for (Hitbox hb : dropdownItemHbs) hb.update();
        }
        
        handleUIClicks();
    }
    
    private void handleUIClicks() {
        if (!savedClick) return;
        savedClick = false;
        
        if (packDropdownHb.hovered) { toggleDropdown(); return; }
        
        if (checkDropdownClicks()) return;
        
        if (createPackHb.hovered) { createPack(); return; }
        if (renamePackHb.hovered) { renamePack(); return; }
        if (copyPackHb.hovered) { copyPack(); return; }
        if (deletePackHb.hovered) { deletePack(); return; }
        if (openFolderHb.hovered) { openPackFolder(); return; }
        if (makeNormalHb.hovered) { startMakeImage(false); return; }
        if (makeUpgradeHb.hovered) { startMakeImage(true); return; }
    }
    
    private void toggleDropdown() {
        CardCrawlGame.sound.play("UI_CLICK_1");
        dropdownOpen = !dropdownOpen;
        if (dropdownOpen) buildDropdownItems();
    }
    
    private void syncAfterPackChange() {
        syncSelectedPackIndex();
        if (dropdownOpen) buildDropdownItems();
    }
    
    private boolean checkDropdownClicks() {
        if (!dropdownOpen) return false;
        List<String> packs = PackManager.getInstance().getPackNames();
        for (int i = 0; i < dropdownItemHbs.size(); i++) {
            if (i < packs.size() && dropdownItemHbs.get(i).hovered) {
                selectedPackIndex = i;
                PackManager.getInstance().setCurrentPack(packs.get(i));
                reloadCardViewImage();
                dropdownOpen = false;
                return true;
            }
        }
        return false;
    }
    
    private void buildDropdownItems() {
        dropdownItemHbs.clear();
        List<String> packs = PackManager.getInstance().getPackNames();
        float startY = packDropdownHb.y - padding;
        for (int i = 0; i < packs.size(); i++) {
            dropdownItemHbs.add(new Hitbox(panelX + padding, startY - i * rowHeight, panelWidth - padding * 2, rowHeight));
        }
    }
    
    private boolean isMouseOverUI() {
        float mx = InputHelper.mX;
        float my = InputHelper.mY;
        if (mx >= panelHb.x && mx <= panelHb.x + panelHb.width && my >= panelHb.y && my <= panelHb.y + panelHb.height) return true;
        if (dropdownOpen) {
            float dt = packDropdownHb.y, db = packDropdownHb.y - dropdownItemHbs.size() * rowHeight;
            if (mx >= panelX && mx <= panelX + panelWidth && my <= dt && my >= db) return true;
        }
        return false;
    }
    
    private void createPack() {
        CardCrawlGame.sound.play("UI_CLICK_1");
        EventQueue.invokeLater(() -> {
            String name = UIUtils.showInputDialog(LocalizationKeys.get("pack_name_prompt"));
            if (name != null && !name.trim().isEmpty()) {
                PackManager.getInstance().createPack(name);
                Gdx.app.postRunnable(this::syncAfterPackChange);
            }
        });
    }
    
    private void renamePack() {
        CardCrawlGame.sound.play("UI_CLICK_1");
        String cur = PackManager.getInstance().getCurrentPack();
        if (cur.isEmpty()) return;
        EventQueue.invokeLater(() -> {
            String name = UIUtils.showInputDialog(LocalizationKeys.get("pack_new_name_prompt"));
            if (name != null && !name.trim().isEmpty()) {
                PackManager.getInstance().renamePack(cur, name);
                Gdx.app.postRunnable(this::syncAfterPackChange);
            }
        });
    }
    
    private void copyPack() {
        CardCrawlGame.sound.play("UI_CLICK_1");
        String cur = PackManager.getInstance().getCurrentPack();
        if (cur.isEmpty()) return;
        EventQueue.invokeLater(() -> {
            String name = UIUtils.showInputDialog(LocalizationKeys.get("pack_copy_name_prompt"));
            if (name != null && !name.trim().isEmpty()) {
                PackManager.getInstance().copyPack(cur, name);
                Gdx.app.postRunnable(this::syncAfterPackChange);
            }
        });
    }
    
    private void deletePack() {
        CardCrawlGame.sound.play("UI_CLICK_1");
        String cur = PackManager.getInstance().getCurrentPack();
        if (cur.isEmpty()) return;
        EventQueue.invokeLater(() -> {
            int r = UIUtils.showConfirmDialog(LocalizationKeys.get("pack_delete_confirm"), LocalizationKeys.get("pack_delete"), JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                PackManager.getInstance().deletePack(cur);
                Gdx.app.postRunnable(this::syncAfterPackChange);
            }
        });
    }
    
    private void startMakeImage(boolean upgrade) {
        CardCrawlGame.sound.play("UI_CLICK_1");
        if (PackManager.getInstance().isEmpty()) {
            EventQueue.invokeLater(() -> UIUtils.showMessageDialog(LocalizationKeys.get("select_pack_first")));
            return;
        }
        if (currentCard == null) return;
        this.makingUpgradeVersion = upgrade;
        this.imageProcessingActive = true;
        EventQueue.invokeLater(() -> {
            ImageProcessor.selectAndProcessImage(currentCard, upgrade);
            imageProcessingActive = false;
        });
    }
    
    public void render(SpriteBatch sb) {
        ensureInitialized();
        if (currentPopup == null || !currentPopup.isOpen) return;
        renderPanel(sb);
        renderDropdown(sb);
        renderButtons(sb);
        renderDropdownItems(sb);
    }
    
    private void renderPanel(SpriteBatch sb) {
        sb.setColor(BG_COLOR);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, panelX, panelY - 340f * Settings.scale, panelWidth, 340f * Settings.scale);
        float titleY = panelY - padding * 0.7f;
        FontHelper.renderFontCentered(sb, FontHelper.cardEnergyFont_L, LocalizationKeys.get("panel_title"), panelX + panelWidth / 2, titleY, ACCENT);
        sb.setColor(TEXT_MUTED);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, panelX + padding, titleY - FontHelper.cardEnergyFont_L.getLineHeight() - 4f * Settings.scale, panelWidth - padding * 2, 1f * Settings.scale);
    }
    
    private void renderDropdown(SpriteBatch sb) {
        String cur = PackManager.getInstance().getCurrentPack();
        if (cur.isEmpty()) cur = LocalizationKeys.get("pack_empty");
        renderFlatButton(sb, packDropdownHb, LocalizationKeys.get("pack_select") + ": " + cur, packDropdownHb.hovered ? HOVER_BRIGHT : SECTION_BG, ACCENT);
    }
    
    private void renderDropdownItems(SpriteBatch sb) {
        if (!dropdownOpen) return;
        List<String> packs = PackManager.getInstance().getPackNames();
        for (int i = 0; i < dropdownItemHbs.size() && i < packs.size(); i++) {
            boolean sel = i == selectedPackIndex;
            boolean hov = dropdownItemHbs.get(i).hovered;
            com.badlogic.gdx.graphics.Color bg = sel ? ACCENT : (hov ? HOVER_BRIGHT : SECTION_BG);
            com.badlogic.gdx.graphics.Color txt = sel ? com.badlogic.gdx.graphics.Color.WHITE : (hov ? TEXT_PRIMARY : TEXT_SECONDARY);
            renderFlatButton(sb, dropdownItemHbs.get(i), packs.get(i), bg, txt);
        }
    }
    
    private void renderButtons(SpriteBatch sb) {
        boolean pe = PackManager.getInstance().isEmpty();
        renderSmallFlatButton(sb, createPackHb, LocalizationKeys.get("pack_create"));
        renderSmallFlatButton(sb, renamePackHb, LocalizationKeys.get("pack_rename"));
        renderSmallFlatButton(sb, copyPackHb, LocalizationKeys.get("pack_copy"));
        renderSmallFlatButton(sb, deletePackHb, LocalizationKeys.get("pack_delete"), true);
        renderSmallFlatButton(sb, openFolderHb, LocalizationKeys.get("open_folder"));
        
        sb.setColor(TEXT_MUTED);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, panelX + padding, openFolderHb.y - 6f * Settings.scale, panelWidth - padding * 2, 1f * Settings.scale);
        
        com.badlogic.gdx.graphics.Color mc = pe ? TEXT_MUTED : ACCENT;
        renderFlatButton(sb, makeNormalHb, LocalizationKeys.get("make_normal"), makeNormalHb.hovered ? HOVER_BRIGHT : SECTION_BG, mc);
        renderFlatButton(sb, makeUpgradeHb, LocalizationKeys.get("make_upgrade"), makeUpgradeHb.hovered ? HOVER_BRIGHT : SECTION_BG, mc);
    }
    
    private void renderFlatButton(SpriteBatch sb, Hitbox hb, String text, com.badlogic.gdx.graphics.Color bg, com.badlogic.gdx.graphics.Color tc) {
        sb.setColor(bg);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, hb.x, hb.y, hb.width, hb.height);
        FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, text, hb.x + hb.width / 2, hb.y + hb.height * 0.55f, tc);
    }
    
    private void renderSmallFlatButton(SpriteBatch sb, Hitbox hb, String text, boolean danger) {
        com.badlogic.gdx.graphics.Color bg = hb.hovered ? (danger ? DANGER_HOVER : HOVER_BRIGHT) : SECTION_BG;
        com.badlogic.gdx.graphics.Color tc = hb.hovered ? (danger ? com.badlogic.gdx.graphics.Color.WHITE : TEXT_PRIMARY) : (danger ? DANGER : TEXT_SECONDARY);
        renderFlatButton(sb, hb, text, bg, tc);
    }
    
    private void renderSmallFlatButton(SpriteBatch sb, Hitbox hb, String text) { renderSmallFlatButton(sb, hb, text, false); }
    
    private void openPackFolder() {
        String pn = PackManager.getInstance().getCurrentPack();
        if (pn.isEmpty()) return;
        try { Desktop.getDesktop().open(new File(PackManager.PACK_ROOT_DIR + pn)); }
        catch (IOException e) { logger.error("Failed to open pack folder", e); }
    }
    
    public void reloadCardViewImage() {
        if (currentCard == null || currentPopup == null) return;
        
        CardRegistry.CardInfo info = CardRegistry.getCardInfo(currentCard.cardID);
        if (info == null) return;
        
        String suffix = SingleCardViewPopup.isViewingUpgrade ? "_p" : "";
        String packName = PackManager.getInstance().getCurrentPack();
        if (packName.isEmpty()) return;
        
        String path = PackManager.PACK_ROOT_DIR + packName + "/" + info.getColorFolderName() + "/" + info.getTypeFolderName() + "/" + info.getFileName() + suffix + ".png";
        FileHandle file = Gdx.files.local(path);
        
        if (file.exists()) {
            try {
                Texture tex = new Texture(file);
                basemod.ReflectionHacks.setPrivate(currentPopup, SingleCardViewPopup.class, "portraitImg", tex);
            } catch (Exception e) {
                logger.warn("Failed to reload portrait for {}", currentCard.cardID, e);
            }
        }
    }
}