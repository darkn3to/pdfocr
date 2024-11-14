package org.example.app.components;

import org.example.app.services.imageProc;

import java.io.File;
import java.util.List;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class pdfHandler {

    @Autowired
    private imageProc process;

    public void extractText(String path) throws Exception {
        PDDocument doc = null, newDoc = null;
        int orientation, fontSize;
        try {
            doc = PDDocument.load(new File(path));
            System.setProperty("TESSDATA_PREFIX", "C:\\tessdata");
            PDFRenderer renderer = new PDFRenderer(doc);
            Tesseract inst = new Tesseract();
            inst.setDatapath("C:\\tessdata");
            inst.setLanguage("eng");

            newDoc = new PDDocument();

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                PDPage page = doc.getPage(i);
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();

                BufferedImage imgCopy = process.processImage(image);

                //String text = inst.doOCR(image);
                List<Word> words = inst.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);

                PDRectangle mb = page.getMediaBox();
                boolean isLandscape = mb.getWidth() > mb.getHeight();
                
                PDPage newPage = new PDPage();
                if (isLandscape) {
                    newPage.setMediaBox(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                } 
                else {
                    newPage.setMediaBox(PDRectangle.A4);
                }
                newDoc.addPage(newPage);

                float pageWidth = newPage.getMediaBox().getWidth();
                float pageHeight = newPage.getMediaBox().getHeight();

                float scaleX = pageWidth / imageWidth;
                float scaleY = pageHeight / imageHeight;

                File imageFile = new File("temp_image_" + i + ".png");
                //d.d2();
                /*BufferedImage imgCopy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = imgCopy.createGraphics();
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();
                process.processImage(imgCopy);*/

                //ImageIO.write(image, "png", imageFile);
                ImageIO.write(imgCopy, "png", imageFile);
                PDImageXObject pdImage = PDImageXObject.createFromFileByContent(imageFile, newDoc);
                PDPageContentStream contentStream = new PDPageContentStream(newDoc, newPage);
                contentStream.drawImage(pdImage, 0, 0, newPage.getMediaBox().getWidth(), newPage.getMediaBox().getHeight());
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.setNonStrokingColor(255, 192, 203); 
                for (Word word : words) {
                    if (word.getConfidence() > 35.0) {  // TODO: pre-process images and increase confidence threshold
                        float originalX = (float) word.getBoundingBox().getX();
                        float originalY = (float) word.getBoundingBox().getY();
                        float originalHeight = (float) word.getBoundingBox().getHeight();
                        float x = originalX * scaleX;
                        float y = (imageHeight - (originalY + originalHeight)) * scaleY;
                        fontSize = (int) (originalHeight * scaleY);

                        contentStream.setFont(PDType1Font.HELVETICA, fontSize);
                        contentStream.newLineAtOffset(x, y);
                        //System.out.println("X: " + x + ", Y: " + y + ", Text: " + word.getText());
                        contentStream.showText(word.getText());
                        contentStream.newLineAtOffset(-x, -y);
                    }
                }
                contentStream.endText();
                contentStream.close();
            }

            File outputPdfFile = new File("modified_" + new File(path).getName());
            newDoc.save(outputPdfFile);
            System.out.println("Modified PDF saved successfully at: " + outputPdfFile.getAbsolutePath());
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load or save the PDF: " + e.getMessage());
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
