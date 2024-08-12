package com.work.demo.service.utils;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImageUtils {
    public static BufferedImage convertMultipartFileToBufferedImage(MultipartFile imageFile) throws IOException {
        byte[] bytes = imageFile.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        return ImageIO.read(inputStream);
    }

    // Método para convertir BufferedImage a un array de flotantes
    public static float[] convertImageToFloatArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Array para almacenar los valores de píxeles normalizados
        float[] floatArray = new float[width * height * 3]; // 3 canales de color: R, G, B

        // Iterar sobre los píxeles de la imagen
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Obtener el valor del píxel en RGB
                int rgb = image.getRGB(x, y);

                // Extraer los componentes de color R, G, B
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Normalizar los valores de píxeles al rango [0, 1] y almacenarlos en el array de flotantes
                floatArray[idx++] = (float) red / 255.0f;
                floatArray[idx++] = (float) green / 255.0f;
                floatArray[idx++] = (float) blue / 255.0f;
            }
        }

        return floatArray;
    }

    public static float[][][] convertImageTo3DFloatArray (BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        float[][][] result = new float[3][height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));

                // Normalizar los valores de los colores a [0, 1]
                result[0][y][x] = color.getRed() / 255f;
                result[1][y][x] = color.getGreen() / 255f;
                result[2][y][x] = color.getBlue() / 255f;
            }
        }

        return result;
    }
    public static float[][][][] convertImageTo4DFloatArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Crear un array 4D con tamaño de lote 1
        float[][][][] result = new float[1][3][height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));

                // Normalizar los valores de los colores a [0, 1]
                result[0][0][y][x] = color.getRed() / 255f;
                result[0][1][y][x] = color.getGreen() / 255f;
                result[0][2][y][x] = color.getBlue() / 255f;
            }
        }

        return result;
    }
}
