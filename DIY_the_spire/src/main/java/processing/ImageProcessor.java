package processing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import core.CardRegistry;
import core.PackManager;
import core.TextureManager;
import localization.LocalizationKeys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patch.CardPortraitPatch;
import ui.ImageCropDialog;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.EventQueue;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.zip.Deflater;

public class ImageProcessor {
    private static final Logger logger = LogManager.getLogger(ImageProcessor.class.getName());
    
    private static final int MIN_WIDTH = 250;
    private static final int MIN_HEIGHT = 190;
    
    public static void selectAndProcessImage(AbstractCard card, boolean upgrade) {
        EventQueue.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(LocalizationKeys.get("select_image"));
            chooser.setFileFilter(new FileNameExtensionFilter("PNG/JPG/JPEG", "png", "jpg", "jpeg"));
            
            int result = ui.UIUtils.showFileOpenDialog(chooser);
            
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                processSelectedFile(card, upgrade, selectedFile);
            }
        });
    }
    
    public static void processSelectedFile(AbstractCard card, boolean upgrade, File selectedFile) {
        try {
            FileHandle fileHandle = Gdx.files.absolute(selectedFile.getAbsolutePath());
            Pixmap originalPixmap = new Pixmap(fileHandle);
            
            if (originalPixmap.getWidth() < MIN_WIDTH || originalPixmap.getHeight() < MIN_HEIGHT) {
                originalPixmap.dispose();
                showError(LocalizationKeys.get("image_too_small"));
                return;
            }
            
            showCropDialog(card, upgrade, originalPixmap, selectedFile.getName());
            
        } catch (Exception e) {
            logger.error("ImageProcessor: Failed to load image", e);
            showError("Failed to load image: " + e.getMessage());
        }
    }
    
    private static void showCropDialog(AbstractCard card, boolean upgrade, Pixmap originalPixmap, String originalName) {
        EventQueue.invokeLater(() -> {
            ImageCropDialog dialog = new ImageCropDialog(card, upgrade, originalPixmap, originalName);
            dialog.setVisible(true);
        });
    }
    
    public static Pixmap applyCrop(Pixmap original, int cropX, int cropY, int cropWidth, int cropHeight) {
        Pixmap cropped = new Pixmap(cropWidth, cropHeight, original.getFormat());
        
        for (int y = 0; y < cropHeight; y++) {
            for (int x = 0; x < cropWidth; x++) {
                int srcX = cropX + x;
                int srcY = cropY + y;
                
                if (srcX >= 0 && srcX < original.getWidth() && srcY >= 0 && srcY < original.getHeight()) {
                    int pixel = original.getPixel(srcX, srcY);
                    cropped.drawPixel(x, y, pixel);
                }
            }
        }
        
        return cropped;
    }
    
    public static Pixmap applyMask(Pixmap pixmap, AbstractCard.CardType type) {
        Pixmap mask = MaskManager.getMask(type);
        if (mask == null) {
            return pixmap;
        }
        
        Pixmap result = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGBA8888);
        
        int width = Math.min(pixmap.getWidth(), mask.getWidth());
        int height = Math.min(pixmap.getHeight(), mask.getHeight());
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int maskPixel = mask.getPixel(x, y);
                int maskR = (maskPixel & 0xFF000000) >>> 24;
                
                if (maskR == 0) {
                    result.drawPixel(x, y, 0);
                } else {
                    result.drawPixel(x, y, pixmap.getPixel(x, y));
                }
            }
        }
        
        return result;
    }
    
    public static Pixmap scalePixmap(Pixmap source, int targetWidth, int targetHeight) {
        return TextureManager.scalePixmap(source, targetWidth, targetHeight);
    }
    
    public static void saveCardImage(AbstractCard card, Pixmap processedPixmap, boolean upgrade) {
        CardRegistry.CardInfo info = CardRegistry.getCardInfo(card.cardID);
        if (info == null) {
            logger.error("ImageProcessor: Card not registered: " + card.cardID);
            processedPixmap.dispose();
            return;
        }
        
        String packName = PackManager.getInstance().getCurrentPack();
        if (packName.isEmpty()) {
            logger.error("ImageProcessor: No pack selected");
            processedPixmap.dispose();
            return;
        }
        
        String suffix = upgrade ? "_p" : "";
        String fileName = info.getFileName() + suffix + ".png";
        
        String basePath = PackManager.PACK_ROOT_DIR + packName + "/" +
                          info.getColorFolderName() + "/" + info.getTypeFolderName() + "/";
        
        Pixmap bigPixmap = null;
        Pixmap smallPixmap = null;
        
        try {
            bigPixmap = scalePixmap(processedPixmap, TextureManager.BIG_WIDTH, TextureManager.BIG_HEIGHT);
            smallPixmap = scalePixmap(processedPixmap, TextureManager.SMALL_WIDTH, TextureManager.SMALL_HEIGHT);
            
            FileHandle bigFile = Gdx.files.local(basePath + fileName);
            FileHandle smallFile = Gdx.files.local(basePath + "small/" + fileName);
            
            bigFile.parent().mkdirs();
            smallFile.parent().mkdirs();
            
            savePixmapAsPNG(bigPixmap, bigFile);
            savePixmapAsPNG(smallPixmap, smallFile);
            
            CardCrawlGame.sound.play("UI_CLICK_1");
            
            logger.info("ImageProcessor: Saved card image for " + card.cardID + " (upgrade=" + upgrade + ")");
            
        } catch (Exception e) {
            logger.error("ImageProcessor: Failed to save image", e);
        } finally {
            if (bigPixmap != null) bigPixmap.dispose();
            if (smallPixmap != null) smallPixmap.dispose();
            processedPixmap.dispose();
        }
        
        final String savedCardId = card.cardID;
        Gdx.app.postRunnable(() -> {
            ui.PackEditorUI.getInstance().reloadCardViewImage();
            TextureManager.getInstance().clearCardCache(savedCardId);
            CardPortraitPatch.clearCardCache(savedCardId);
        });
    }
    
    private static void savePixmapAsPNG(Pixmap pixmap, FileHandle file) {
        int width = pixmap.getWidth();
        int height = pixmap.getHeight();
        
        try {
            byte[] header = writeIHDR(width, height);
            byte[] idat = writeIDAT(pixmap);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            dos.write(137); dos.write(80); dos.write(78); dos.write(71);
            dos.write(13); dos.write(10); dos.write(26); dos.write(10);
            
            writeChunk(dos, "IHDR", header);
            writeChunk(dos, "IDAT", idat);
            writeChunk(dos, "IEND", new byte[0]);
            
            dos.flush();
            file.writeBytes(baos.toByteArray(), false);
        } catch (Exception e) {
            logger.error("ImageProcessor: Failed to save PNG", e);
        }
    }
    
    private static byte[] writeIHDR(int width, int height) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(width);
        dos.writeInt(height);
        dos.writeByte(8);
        dos.writeByte(6);
        dos.writeByte(0);
        dos.writeByte(0);
        dos.writeByte(0);
        dos.flush();
        return baos.toByteArray();
    }
    
    private static byte[] writeIDAT(Pixmap pixmap) {
        int width = pixmap.getWidth();
        int height = pixmap.getHeight();
        byte[] raw = new byte[height * (1 + width * 4)];
        int idx = 0;
        
        for (int y = 0; y < height; y++) {
            raw[idx++] = 0;
            for (int x = 0; x < width; x++) {
                int pixel = pixmap.getPixel(x, y);
                raw[idx++] = (byte) ((pixel >> 24) & 0xFF);
                raw[idx++] = (byte) ((pixel >> 16) & 0xFF);
                raw[idx++] = (byte) ((pixel >> 8) & 0xFF);
                raw[idx++] = (byte) (pixel & 0xFF);
            }
        }
        
        Deflater deflater = new Deflater();
        deflater.setInput(raw);
        deflater.finish();
        byte[] buf = new byte[raw.length + 64];
        int len = deflater.deflate(buf);
        deflater.end();
        
        byte[] result = new byte[len];
        System.arraycopy(buf, 0, result, 0, len);
        return result;
    }
    
    private static void writeChunk(DataOutputStream dos, String type, byte[] data) throws Exception {
        dos.writeInt(data.length);
        
        byte[] typeBytes = type.getBytes("US-ASCII");
        byte[] crcData = new byte[4 + data.length];
        System.arraycopy(typeBytes, 0, crcData, 0, 4);
        System.arraycopy(data, 0, crcData, 4, data.length);
        
        dos.write(typeBytes);
        dos.write(data);
        dos.writeInt(crc32(crcData));
    }
    
    private static int crc32(byte[] data) {
        int crc = -1;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) != 0) crc = (crc >>> 1) ^ 0xEDB88320;
                else crc >>>= 1;
            }
        }
        return crc ^ -1;
    }
    
    private static void showError(String message) {
        EventQueue.invokeLater(() -> {
            ui.UIUtils.showMessageDialog(message);
        });
    }
}