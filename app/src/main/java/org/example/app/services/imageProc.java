package org.example.app.services;

import java.io.ByteArrayInputStream;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.ColorModel;
import javax.imageio.ImageIO;

import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.CvType;
import org.opencv.core.Core;
import org.opencv.imgcodecs.Imgcodecs;

import org.springframework.stereotype.Service;

@Service 
public class imageProc {
    static {
        try {
            System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
        } 
        catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV native library: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // remember images are represented as a mat object.
    // A 2-d mat represents a grayscale image, while a 3-d mat represents a color image.
    // CV_8UC1 is grayscale, CV_8UC3 is RGB and CV_8UC4 is CMYK.
    public Mat buffImg2Mat(BufferedImage image) {
        Mat mat = null;
        try {
            if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) { // for grayscale images
                mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1);
                byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData(); // convert the image to byte array
                mat.put(0, 0, data);  
            } 
            else if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) { // for color images
                mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
                byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
                mat.put(0, 0, data);
            } 
            else {  // for other types of images
                BufferedImage convertedImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                convertedImg.getGraphics().drawImage(image, 0, 0, null);
                mat = new Mat(convertedImg.getHeight(), convertedImg.getWidth(), CvType.CV_8UC3);
                byte[] data = ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData(); //convert from DataBufferInt to DataBufferByte
                mat.put(0, 0, data);
            }
        } 
        catch (Exception e) {
            System.err.println("Error converting BufferedImage to Mat: " + e.getMessage());
            e.printStackTrace();
        }
        return mat;
    }

    public BufferedImage Mat2buffImg(Mat mat) {
        BufferedImage image = null;
        try {
            MatOfByte mob = new MatOfByte(); 
            Imgcodecs.imencode(".png", mat, mob);  // encode the matrix into a byte array
            byte[] byteArray = mob.toArray();
            image = ImageIO.read(new ByteArrayInputStream(byteArray));
        } 
        catch (Exception e) {
            System.err.println("Error converting Mat to BufferedImage: " + e.getMessage());
            e.printStackTrace();
        }
        return image;
    }

    public BufferedImage processImage(BufferedImage image) {
        try {
            Mat mat = buffImg2Mat(image);   // convert the image to a matrix as adaptiveThreshold() requires a mat object
            if (mat.empty()) {
                throw new IllegalArgumentException("The provided image is empty or invalid.");
            }
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);  // convert the image to grayscale
            
            //thresholding for binarizing the image according to the local pixel intensity
            Imgproc.adaptiveThreshold(mat, mat, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2.0);
            //distance transform finds out the distance to the nearest zero pixel 
            //in the image
            //Imgproc.distanceTransform(mat, mat, Imgproc.DIST_L2, 5);  
            //Core.normalize(mat, mat, 0.0, 1.0, org.opencv.core.Core.NORM_MINMAX);  // normalize the image
            
            image = Mat2buffImg(mat); // convert mat back to BufferedImage
        } 
        catch (Exception e) {
            System.err.println("Error processing image: " + e.getMessage());
            e.printStackTrace();
        }
        return image;
    }
}
