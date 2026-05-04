package ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import core.TextureManager;
import localization.LocalizationKeys;
import processing.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class ImageCropDialog extends JFrame {
    private static final float ASPECT_RATIO = 500f / 380f;
    
    private AbstractCard card;
    private boolean upgrade;
    private Pixmap originalPixmap;
    private String originalName;
    
    private int originalWidth;
    private int originalHeight;
    
    private int cropX = 0;
    private int cropY = 0;
    private int cropWidth = 250;
    private int cropHeight = 190;
    
    private int minCropSize = 30;
    
    private CropPanel cropPanel;
    private JLabel sizeLabel;
    private JLabel warningLabel;
    
    private boolean dragging = false;
    private boolean resizing = false;
    private int resizeCorner = -1;
    private int dragStartX, dragStartY;
    private int cropStartX, cropStartY, cropStartW, cropStartH;
    
    private BufferedImage displayImage;
    
    public ImageCropDialog(AbstractCard card, boolean upgrade, Pixmap originalPixmap, String originalName) {
        this.card = card;
        this.upgrade = upgrade;
        this.originalPixmap = originalPixmap;
        this.originalName = originalName;
        
        this.originalWidth = originalPixmap.getWidth();
        this.originalHeight = originalPixmap.getHeight();
        
        initCropSize();
        initUI();
    }
    
    private void initCropSize() {
        float imageAspect = (float) originalWidth / originalHeight;
        
        if (imageAspect > ASPECT_RATIO) {
            cropHeight = originalHeight;
            cropWidth = (int) (cropHeight * ASPECT_RATIO);
        } else {
            cropWidth = originalWidth;
            cropHeight = (int) (cropWidth / ASPECT_RATIO);
        }
        
        if (cropWidth < minCropSize) cropWidth = minCropSize;
        if (cropHeight < minCropSize) cropHeight = minCropSize;
        
        cropHeight = Math.min(cropHeight, originalHeight);
        cropWidth = Math.min(cropWidth, originalWidth);
        
        cropX = (originalWidth - cropWidth) / 2;
        cropY = (originalHeight - cropHeight) / 2;
    }
    
    private void initUI() {
        setTitle(LocalizationKeys.get("select_image") + " - " + originalName);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        displayImage = pixmapToBufferedImage(originalPixmap);
        
        int displayScale = calculateDisplayScale();
        int displayWidth = originalWidth / displayScale;
        int displayHeight = originalHeight / displayScale;
        
        cropPanel = new CropPanel(displayWidth, displayHeight, displayScale);
        cropPanel.setPreferredSize(new Dimension(displayWidth, displayHeight));
        
        sizeLabel = new JLabel();
        warningLabel = new JLabel();
        warningLabel.setForeground(Color.YELLOW);
        
        updateSizeLabel();
        
        JButton confirmButton = new JButton(LocalizationKeys.get("crop_confirm"));
        confirmButton.addActionListener(e -> confirmCrop());
        
        JButton cancelButton = new JButton(LocalizationKeys.get("crop_cancel"));
        cancelButton.addActionListener(e -> cancelCrop());
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(sizeLabel);
        infoPanel.add(warningLabel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(cropPanel, BorderLayout.CENTER);
        getContentPane().add(infoPanel, BorderLayout.NORTH);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelCrop();
                }
            }
        });
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        toFront();
        requestFocus();
    }
    
    private int calculateDisplayScale() {
        int maxDisplayWidth = 800;
        int maxDisplayHeight = 600;
        
        int scaleW = originalWidth / maxDisplayWidth + 1;
        int scaleH = originalHeight / maxDisplayHeight + 1;
        
        return Math.max(scaleW, scaleH);
    }
    
    private BufferedImage pixmapToBufferedImage(Pixmap pixmap) {
        return TextureManager.pixmapToBufferedImage(pixmap);
    }
    
    private void updateSizeLabel() {
        String text = LocalizationKeys.get("original_size") + ": " + originalWidth + "×" + originalHeight + 
                      " → " + LocalizationKeys.get("output_size") + ": " + cropWidth + "×" + cropHeight;
        sizeLabel.setText(text);
        
        if (cropWidth < TextureManager.BIG_WIDTH || cropHeight < TextureManager.BIG_HEIGHT) {
            warningLabel.setText(LocalizationKeys.get("crop_size_warning"));
        } else {
            warningLabel.setText("");
        }
    }
    
    private void confirmCrop() {
        CardCrawlGame.sound.play("UI_CLICK_1");
        
        Pixmap croppedPixmap = ImageProcessor.applyCrop(originalPixmap, cropX, cropY, cropWidth, cropHeight);
        Pixmap scaledPixmap = ImageProcessor.scalePixmap(croppedPixmap, TextureManager.BIG_WIDTH, TextureManager.BIG_HEIGHT);
        Pixmap maskedPixmap = ImageProcessor.applyMask(scaledPixmap, card.type);
        
        ImageProcessor.saveCardImage(card, maskedPixmap, upgrade);
        
        croppedPixmap.dispose();
        scaledPixmap.dispose();
        originalPixmap.dispose();
        
        dispose();
    }
    
    private void cancelCrop() {
        originalPixmap.dispose();
        dispose();
    }
    
    private class CropPanel extends JPanel {
        private int displayScale;
        
        public CropPanel(int width, int height, int scale) {
            this.displayScale = scale;
            setPreferredSize(new Dimension(width, height));
            
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int mx = e.getX() * displayScale;
                    int my = e.getY() * displayScale;
                    
                    int cornerSize = 15 * displayScale;
                    
                    if (mx >= cropX - cornerSize && mx <= cropX + cornerSize &&
                        my >= cropY - cornerSize && my <= cropY + cornerSize) {
                        resizing = true;
                        resizeCorner = 0;
                    } else if (mx >= cropX + cropWidth - cornerSize && mx <= cropX + cropWidth + cornerSize &&
                               my >= cropY - cornerSize && my <= cropY + cornerSize) {
                        resizing = true;
                        resizeCorner = 1;
                    } else if (mx >= cropX - cornerSize && mx <= cropX + cornerSize &&
                               my >= cropY + cropHeight - cornerSize && my <= cropY + cropHeight + cornerSize) {
                        resizing = true;
                        resizeCorner = 2;
                    } else if (mx >= cropX + cropWidth - cornerSize && mx <= cropX + cropWidth + cornerSize &&
                               my >= cropY + cropHeight - cornerSize && my <= cropY + cropHeight + cornerSize) {
                        resizing = true;
                        resizeCorner = 3;
                    } else if (mx >= cropX && mx <= cropX + cropWidth &&
                               my >= cropY && my <= cropY + cropHeight) {
                        dragging = true;
                        dragStartX = mx;
                        dragStartY = my;
                        cropStartX = cropX;
                        cropStartY = cropY;
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    dragging = false;
                    resizing = false;
                    resizeCorner = -1;
                }
                
                @Override
                public void mouseDragged(MouseEvent e) {
                    int mx = e.getX() * displayScale;
                    int my = e.getY() * displayScale;
                    
                    if (resizing) {
                        handleResize(mx, my);
                    } else if (dragging) {
                        handleDrag(mx, my);
                    }
                    
                    updateSizeLabel();
                    repaint();
                }
            };
            
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }
        
        private void handleResize(int mx, int my) {
            int newWidth, newHeight;
            
            switch (resizeCorner) {
                case 0:
                    newWidth = cropX + cropWidth - mx;
                    newHeight = (int) (newWidth / ASPECT_RATIO);
                    if (newWidth >= minCropSize && newHeight >= minCropSize &&
                        mx >= 0 && my >= 0 &&
                        mx + newWidth <= originalWidth && my + newHeight <= originalHeight) {
                        cropX = mx;
                        cropY = my;
                        cropWidth = newWidth;
                        cropHeight = newHeight;
                    }
                    break;
                case 1:
                    newWidth = mx - cropX;
                    newHeight = (int) (newWidth / ASPECT_RATIO);
                    if (newWidth >= minCropSize && newHeight >= minCropSize &&
                        cropY >= 0 && cropY + newHeight <= originalHeight &&
                        cropX + newWidth <= originalWidth) {
                        cropWidth = newWidth;
                        cropHeight = newHeight;
                    }
                    break;
                case 2:
                    newHeight = cropY + cropHeight - my;
                    newWidth = (int) (newHeight * ASPECT_RATIO);
                    if (newWidth >= minCropSize && newHeight >= minCropSize &&
                        cropX >= 0 && cropX + newWidth <= originalWidth &&
                        my >= 0 && my + newHeight <= originalHeight) {
                        cropX = mx;
                        cropY = my;
                        cropWidth = newWidth;
                        cropHeight = newHeight;
                    }
                    break;
                case 3:
                    newHeight = my - cropY;
                    newWidth = (int) (newHeight * ASPECT_RATIO);
                    if (newWidth >= minCropSize && newHeight >= minCropSize &&
                        cropX >= 0 && cropX + newWidth <= originalWidth &&
                        cropY + newHeight <= originalHeight) {
                        cropWidth = newWidth;
                        cropHeight = newHeight;
                    }
                    break;
            }
        }
        
        private void handleDrag(int mx, int my) {
            int dx = mx - dragStartX;
            int dy = my - dragStartY;
            
            int newX = cropStartX + dx;
            int newY = cropStartY + dy;
            
            if (newX >= 0 && newX + cropWidth <= originalWidth) {
                cropX = newX;
            }
            if (newY >= 0 && newY + cropHeight <= originalHeight) {
                cropY = newY;
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g;
            
            int dispX = cropX / displayScale;
            int dispY = cropY / displayScale;
            int dispW = cropWidth / displayScale;
            int dispH = cropHeight / displayScale;
            
            int imgX = 0;
            int imgY = 0;
            int imgW = displayImage.getWidth() / displayScale;
            int imgH = displayImage.getHeight() / displayScale;
            
            g2d.drawImage(displayImage, imgX, imgY, imgW, imgH, null);
            
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, dispX, getHeight());
            g2d.fillRect(dispX + dispW, 0, getWidth() - dispX - dispW, getHeight());
            g2d.fillRect(dispX, 0, dispW, dispY);
            g2d.fillRect(dispX, dispY + dispH, dispW, getHeight() - dispY - dispH);
            
            g2d.setColor(new Color(0, 255, 0, 100));
            g2d.fillRect(dispX, dispY, dispW, dispH);
            
            g2d.setColor(Color.WHITE);
            int cornerSize = 10;
            g2d.fillRect(dispX - cornerSize/2, dispY - cornerSize/2, cornerSize, cornerSize);
            g2d.fillRect(dispX + dispW - cornerSize/2, dispY - cornerSize/2, cornerSize, cornerSize);
            g2d.fillRect(dispX - cornerSize/2, dispY + dispH - cornerSize/2, cornerSize, cornerSize);
            g2d.fillRect(dispX + dispW - cornerSize/2, dispY + dispH - cornerSize/2, cornerSize, cornerSize);
            
            g2d.setColor(Color.GREEN);
            g2d.drawRect(dispX, dispY, dispW, dispH);
        }
    }
}