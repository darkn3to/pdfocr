package org.example.app.components;

import java.io.File;
import java.util.List;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.OSDResult;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

@Component
public class pdfHandler {
    public void extractText(String path) throws Exception {
        PDDocument doc = null, newDoc = null;
        try {
            doc = PDDocument.load(new File(path));
            System.setProperty("TESSDATA_PREFIX", "C:\\tessdata");
            PDFRenderer renderer = new PDFRenderer(doc);
            Tesseract inst = new Tesseract();
            inst.setDatapath("C:\\tessdata");
            inst.setLanguage("eng");

            newDoc = new PDDocument();

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                String text = inst.doOCR(image);
                List<Word> words = inst.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);
                Graphics2D g2d = image.createGraphics();
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(3));
                for (Word word : words) {

                    if (word.getConfidence() > 75.0) {
                        System.out.println("Word: " + word.getText() + ", Bounding Box: " + word.getBoundingBox() + ", Confidence: " + word.getConfidence());
                        g2d.draw(word.getBoundingBox());
                    }
                    g2d.drawString(String.format("%.2f", word.getConfidence()), (float) word.getBoundingBox().getX(), (float) (word.getBoundingBox().getY() + word.getBoundingBox().getHeight() + 12));
                }
                g2d.dispose();

                File outputImageFile = new File("output_image_with_rectangles_page_" + i + ".png");
                ImageIO.write(image, "png", outputImageFile);
                System.out.println("Modified image saved successfully at: " + outputImageFile.getAbsolutePath());

                PDPage newPage = new PDPage();
                newDoc.addPage(newPage);

                PDImageXObject pdImage = PDImageXObject.createFromFileByContent(outputImageFile, newDoc);
                PDPageContentStream contentStream = new PDPageContentStream(newDoc, newPage);
                contentStream.drawImage(pdImage, 0, 0, newPage.getMediaBox().getWidth(), newPage.getMediaBox().getHeight());
                contentStream.close();
            }

            File outputPdfFile = new File("modified_" + new File(path).getName());
            newDoc.save(outputPdfFile);
            System.out.println("Modified PDF saved successfully at: " + outputPdfFile.getAbsolutePath());
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
