package ui;

import javax.swing.*;
import java.awt.*;

public class UIUtils {
    
    private static Frame gameFrame;
    
    public static void init() {
        try {
            Canvas c = (Canvas) org.lwjgl.opengl.Display.getParent();
            if (c != null) {
                Container parent = c.getParent();
                while (parent != null) {
                    if (parent instanceof Frame) {
                        gameFrame = (Frame) parent;
                        return;
                    }
                    parent = parent.getParent();
                }
            }
        } catch (Exception ignored) {}
        
        for (Frame f : Frame.getFrames()) {
            if (f.isVisible() && f.getWidth() > 800 && hasCanvasChild(f)) {
                gameFrame = f;
                return;
            }
        }
        for (Frame f : Frame.getFrames()) {
            if (f.isVisible() && f.getWidth() > 800) {
                gameFrame = f;
                return;
            }
        }
    }
    
    private static boolean hasCanvasChild(Container c) {
        for (java.awt.Component child : c.getComponents()) {
            if (child instanceof Canvas) return true;
            if (child instanceof Container && hasCanvasChild((Container) child)) return true;
        }
        return false;
    }
    
    public static int showConfirmDialog(String message, String title, int optionType) {
        if (gameFrame != null) gameFrame.setAlwaysOnTop(false);
        int result = JOptionPane.showConfirmDialog(gameFrame, message, title, optionType);
        if (gameFrame != null) gameFrame.setAlwaysOnTop(true);
        return result;
    }
    
    public static void showMessageDialog(String message) {
        if (gameFrame != null) gameFrame.setAlwaysOnTop(false);
        JOptionPane.showMessageDialog(gameFrame, message);
        if (gameFrame != null) gameFrame.setAlwaysOnTop(true);
    }
    
    public static String showInputDialog(String message) {
        if (gameFrame != null) gameFrame.setAlwaysOnTop(false);
        String result = JOptionPane.showInputDialog(gameFrame, message);
        if (gameFrame != null) gameFrame.setAlwaysOnTop(true);
        return result;
    }
    
    public static int showFileOpenDialog(JFileChooser chooser) {
        if (gameFrame != null) gameFrame.setAlwaysOnTop(false);
        int result = chooser.showOpenDialog(gameFrame);
        if (gameFrame != null) gameFrame.setAlwaysOnTop(true);
        return result;
    }
}
