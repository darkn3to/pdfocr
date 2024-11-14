package org.example.app.services;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.awt.image.ColorModel;
import javax.imageio.ImageIO;

import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;
import org.opencv.core.CvType;

import org.springframework.stereotype.Service;

@Service 
public class imageProc {
    static {
        try {
            System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV native library: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Mat buffImg2Mat(BufferedImage image) {
        Mat mat = null;
        try {
            if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
                mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1);
                byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
                mat.put(0, 0, data);
            } 
            else if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
                mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
                byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
                mat.put(0, 0, data);
            } 
            else {
                BufferedImage convertedImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                convertedImg.getGraphics().drawImage(image, 0, 0, null);
                mat = new Mat(convertedImg.getHeight(), convertedImg.getWidth(), CvType.CV_8UC3);
                byte[] data = ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
                mat.put(0, 0, data);
            }
        } catch (Exception e) {
            System.err.println("Error converting BufferedImage to Mat: " + e.getMessage());
            e.printStackTrace();
        }
        return mat;
    }

    public void processImage(BufferedImage image) {
        try {
            Mat mat = buffImg2Mat(image); // Convert BufferedImage to Mat
            if (mat.empty()) {
                throw new IllegalArgumentException("The provided image is empty or invalid.");
            }
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY); // Convert to grayscale
            Imgproc.threshold(mat, mat, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU); // Apply thresholding
        } catch (Exception e) {
            System.err.println("Error processing image: " + e.getMessage());
            e.printStackTrace();
        }
    }
}