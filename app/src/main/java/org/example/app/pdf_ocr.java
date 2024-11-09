package org.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.pdfbox.*;
import net.sourceforge.tess4j.Tesseract;
import org.opencv.core.*;

@SpringBootApplication
public class pdf_ocr {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("Welcome to OpenCV " + Core.VERSION);
        SpringApplication.run(pdf_ocr.class, args);
    }
}
