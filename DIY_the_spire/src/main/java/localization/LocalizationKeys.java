package localization;

import com.megacrit.cardcrawl.core.Settings;
import java.util.HashMap;
import java.util.Map;

public class LocalizationKeys {
    private static final Map<String, String> ZHS = new HashMap<>();
    private static final Map<String, String> ENG = new HashMap<>();
    
    static {
        initZHS();
        initENG();
    }
    
    private static void initZHS() {
        ZHS.put("panel_title", "DIY the Spire");
        ZHS.put("pack_select", "图包选择");
        ZHS.put("pack_empty", "无图包");
        ZHS.put("pack_create", "新建图包");
        ZHS.put("pack_rename", "重命名图包");
        ZHS.put("pack_copy", "复制图包");
        ZHS.put("pack_delete", "删除图包");
        ZHS.put("pack_delete_confirm", "确定删除图包?");
        ZHS.put("open_folder", "打开卡包文件夹");
        ZHS.put("make_normal", "制作普通版");
        ZHS.put("make_upgrade", "制作升级版");
        ZHS.put("select_pack_first", "请先选择图包");
        ZHS.put("select_image", "选择原图");
        ZHS.put("image_too_small", "图片尺寸不足，最小要求250×190");
        ZHS.put("crop_confirm", "确认裁剪");
        ZHS.put("crop_cancel", "取消");
        ZHS.put("crop_size_warning", "尺寸不足500×380");
        ZHS.put("original_size", "原始尺寸");
        ZHS.put("output_size", "输出尺寸");
        ZHS.put("pack_name_prompt", "输入图包名称");
        ZHS.put("pack_new_name_prompt", "输入新名称");
        ZHS.put("pack_copy_name_prompt", "输入复制后的名称");
        ZHS.put("success", "成功");
        ZHS.put("failed", "失败");
        ZHS.put("current_card", "当前卡牌");
        ZHS.put("card_id", "卡牌ID");
        ZHS.put("card_type", "卡牌类型");
        ZHS.put("card_color", "卡牌颜色");
    }
    
    private static void initENG() {
        ENG.put("panel_title", "DIY the Spire");
        ENG.put("pack_select", "Pack Select");
        ENG.put("pack_empty", "No Pack");
        ENG.put("pack_create", "Create Pack");
        ENG.put("pack_rename", "Rename Pack");
        ENG.put("pack_copy", "Copy Pack");
        ENG.put("pack_delete", "Delete Pack");
        ENG.put("pack_delete_confirm", "Confirm delete pack?");
        ENG.put("open_folder", "Open Pack Folder");
        ENG.put("make_normal", "Make Normal");
        ENG.put("make_upgrade", "Make Upgrade");
        ENG.put("select_pack_first", "Please select a pack first");
        ENG.put("select_image", "Select Image");
        ENG.put("image_too_small", "Image too small, minimum 250x190");
        ENG.put("crop_confirm", "Confirm Crop");
        ENG.put("crop_cancel", "Cancel");
        ENG.put("crop_size_warning", "Size less than 500x380");
        ENG.put("original_size", "Original Size");
        ENG.put("output_size", "Output Size");
        ENG.put("pack_name_prompt", "Enter pack name");
        ENG.put("pack_new_name_prompt", "Enter new name");
        ENG.put("pack_copy_name_prompt", "Enter copy name");
        ENG.put("success", "Success");
        ENG.put("failed", "Failed");
        ENG.put("current_card", "Current Card");
        ENG.put("card_id", "Card ID");
        ENG.put("card_type", "Card Type");
        ENG.put("card_color", "Card Color");
    }
    
    public static String get(String key) {
        if (Settings.language == Settings.GameLanguage.ZHS) {
            return ZHS.getOrDefault(key, ENG.getOrDefault(key, key));
        }
        return ENG.getOrDefault(key, key);
    }
    
    public static String getCardTypeName(com.megacrit.cardcrawl.cards.AbstractCard.CardType type) {
        if (type == com.megacrit.cardcrawl.cards.AbstractCard.CardType.ATTACK)
            return Settings.language == Settings.GameLanguage.ZHS ? "攻击" : "Attack";
        if (type == com.megacrit.cardcrawl.cards.AbstractCard.CardType.SKILL)
            return Settings.language == Settings.GameLanguage.ZHS ? "技能" : "Skill";
        if (type == com.megacrit.cardcrawl.cards.AbstractCard.CardType.POWER)
            return Settings.language == Settings.GameLanguage.ZHS ? "能力" : "Power";
        if (type == com.megacrit.cardcrawl.cards.AbstractCard.CardType.STATUS)
            return Settings.language == Settings.GameLanguage.ZHS ? "状态" : "Status";
        if (type == com.megacrit.cardcrawl.cards.AbstractCard.CardType.CURSE)
            return Settings.language == Settings.GameLanguage.ZHS ? "诅咒" : "Curse";
        return type.name();
    }
    
    public static String getCardColorName(com.megacrit.cardcrawl.cards.AbstractCard.CardColor color) {
        if (color == com.megacrit.cardcrawl.cards.AbstractCard.CardColor.RED)
            return Settings.language == Settings.GameLanguage.ZHS ? "红色" : "Red";
        if (color == com.megacrit.cardcrawl.cards.AbstractCard.CardColor.GREEN)
            return Settings.language == Settings.GameLanguage.ZHS ? "绿色" : "Green";
        if (color == com.megacrit.cardcrawl.cards.AbstractCard.CardColor.BLUE)
            return Settings.language == Settings.GameLanguage.ZHS ? "蓝色" : "Blue";
        if (color == com.megacrit.cardcrawl.cards.AbstractCard.CardColor.PURPLE)
            return Settings.language == Settings.GameLanguage.ZHS ? "紫色" : "Purple";
        if (color == com.megacrit.cardcrawl.cards.AbstractCard.CardColor.COLORLESS)
            return Settings.language == Settings.GameLanguage.ZHS ? "无色" : "Colorless";
        if (color == com.megacrit.cardcrawl.cards.AbstractCard.CardColor.CURSE)
            return Settings.language == Settings.GameLanguage.ZHS ? "诅咒" : "Curse";
        return color.name();
    }
}