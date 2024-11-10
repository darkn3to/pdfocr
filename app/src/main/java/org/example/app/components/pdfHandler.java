package org.example.app.components;

import org.springframework.stereotype.Component;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import net.sourceforge.tess4j.Tesseract;

import java.io.File;

@Component
public class pdfHandler {
    public void extractText(String path) throws Exception {
        PDDocument doc = null;
        try {
            doc = PDDocument.load(new File(path));
            System.setProperty("TESSDATA_PREFIX", "C:\\tessdata");
            PDFRenderer renderer = new PDFRenderer(doc);
            Tesseract inst = new Tesseract();
            inst.setDatapath("C:\\tessdata");
            inst.setLanguage("eng");
            String text = inst.doOCR(renderer.renderImageWithDPI(0, 300));
            System.out.println("Extracted Text: " + text);
        }
        catch (Exception e) {
            System.out.println("Failed to load the PDF. ");
        }
        finally {
            if (doc != null) {
                try {
                    doc.close();
                }
                catch (Exception e) {
                    System.out.println("Failed to close the PDF. ");
                }
            }
        }
    }
}
