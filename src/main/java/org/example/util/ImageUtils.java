package org.example.util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageUtils {
    private static final int IMG_WIDTH = 100;
    private static final int IMG_HEIGHT = 100;

    public static byte[] resizeAndConvert(File file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file);

        Image scaled = originalImage.getScaledInstance(IMG_WIDTH, IMG_HEIGHT, Image.SCALE_SMOOTH);
        BufferedImage bufferedScaled = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g2d = bufferedScaled.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        return ((java.awt.image.DataBufferByte) bufferedScaled.getRaster().getDataBuffer()).getData();
    }
    public static ImageIcon convertBytesToIcon(byte[] data) {
        if (data == null || data.length == 0) return null;

        BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);

        byte[] targetPixels = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(data, 0, targetPixels, 0, data.length);

        Image scaled = image.getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}