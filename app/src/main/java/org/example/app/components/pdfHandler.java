package org.example.app.components;

import org.example.app.services.imageProc;

import java.io.File;
import java.util.List;
import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class pdfHandler {

    @Autowired
    private imageProc process;

    public void extractText(String path) throws Exception {
        PDDocument doc = null, newDoc = null;
        int orientation;
        float fontSize;
        Document docHocr = null;
        try {
            doc = Loader.loadPDF(new File(path));
            System.setProperty("TESSDATA_PREFIX", "C:\\tessdata");
            PDFRenderer renderer = new PDFRenderer(doc);
            Tesseract inst = new Tesseract();
            inst.setDatapath("C:\\tessdata");
            inst.setLanguage("eng");

            inst.setTessVariable("tessedit_create_hocr", "1");
            inst.setTessVariable("tessedit_write_images", "1");

            newDoc = new PDDocument();

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                PDPage page = doc.getPage(i);
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();

                BufferedImage imgCopy = process.processImage(image);

                String hocrData = inst.doOCR(imgCopy);
                File hOCRdoc = new File("hocr_" + i + ".html");
                //org.apache.commons.io.FileUtils.writeStringToFile(hOCRdoc, hocrData, "UTF-8");
                //System.out.println("HOCR file created: " + hOCRdoc.getAbsolutePath());
                //String text = inst.doOCR(image);
                /*List<Word> words = null;
                try {
                    words = inst.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);
                } catch (Exception e) {
                    System.err.println("Error performing OCR on page " + i + ": " + e.getMessage());
                    e.printStackTrace();
                    continue; 
                }
                */
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

                //File imageFile = new File("temp_image_" + i + ".png");
                //d.d2();
                /*BufferedImage imgCopy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = imgCopy.createGraphics();
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();
                process.processImage(imgCopy);*/

                File imageFile = new File("temp_image_" + i + ".png");
                //ImageIO.write(image, "png", imageFile);
                ImageIO.write(image, "png", imageFile);
                PDImageXObject pdImage = PDImageXObject.createFromFileByContent(imageFile, newDoc);
                PDPageContentStream contentStream = new PDPageContentStream(newDoc, newPage);
                contentStream.drawImage(pdImage, 0, 0, newPage.getMediaBox().getWidth(), newPage.getMediaBox().getHeight());
                contentStream.beginText();
                contentStream.appendRawCommands("3 Tr ");
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.setNonStrokingColor(new Color(0, 0, 0)); 
                
                docHocr = Jsoup.parse(hocrData);
                Elements lines = docHocr.select(".ocr_line");
                for (Element line : lines) {
                    String lineTitle = line.attr("title");
                    String[] token = lineTitle.split("; ");
                    String xSize = null;
                    for (String part : token) {
                        if (part.startsWith("x_size")) {
                            xSize = part.split(" ")[1];
                            break;
                        }
                    }
                    //String text = line.text();
                    //System.out.println("Line text: " + line.text());
                    fontSize = (Float.parseFloat(xSize));
                    
                    Elements words = line.select(".ocrx_word");
                    fontSize = (float) ((fontSize * scaleY) * 0.9);
                    for (Element word : words) {
                        String text = word.text();
                        String title = word.attr("title");
                        String[] wordToken = title.split(";");
                        String[] bbox = wordToken[0].replace("bbox ", "").split(" ");
                        String conf = wordToken[1].replace("x_wconf ", "");
                        if (Float.parseFloat(conf) > 60.0) {
                            int left, bottom;
                            left = Integer.parseInt(bbox[0]);
                            bottom = Integer.parseInt(bbox[3]);
                            //System.out.println("Confidence: " + conf);
                            //System.out.println("Text: " + text + ", Left: " + left + ", Top: " + top + ", Right: " + right + ", Bottom: " + bottom);
                            //System.out.println("Font size: " + fontSize);

                            float x = left * scaleX;
                            float y = (imageHeight - bottom) * scaleY;
                        
                            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);
                            //System.out.println("X: " + x + ", Y: " + y + ", Text: " + text);
                            contentStream.newLineAtOffset(x, y);
                            contentStream.showText(text);
                            contentStream.newLineAtOffset(-x, -y);
                        }
                    }
                }
                /*for (Word word : words) {
                    if (word.getConfidence() > 35.0) {  // TODO: pre-process images and increase confidence threshold
                        float originalX = (float) word.getBoundingBox().getX();
                        float originalY = (float) word.getBoundingBox().getY();
                        float originalHeight = (float) word.getBoundingBox().getHeight();
                        float x = originalX * scaleX;
                        float y = (imageHeight - (originalY + originalHeight)) * scaleY;
                        fontSize = (int) (originalHeight * scaleY);
                        System.out.println("FOnt size: " + fontSize);
                        contentStream.setFont(PDType1Font.HELVETICA, fontSize);
                        contentStream.newLineAtOffset(x, y);
                        System.out.println("X: " + x + ", Y: " + y + ", Text: " + word.getText());
                        System.out.println("");
                        contentStream.showText(word.getText());
                        contentStream.newLineAtOffset(-x, -y);
                    }
                }*/
                
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
                } catch (IOException e) {
                    System.err.println("Failed to close the original PDF: " + e.getMessage());
                }
            }
            if (newDoc != null) {
                try {
                    newDoc.close();
                }
                catch (Exception e) {
                    System.out.println("Failed to close the PDF. ");
                }
            }
        }
    }
}
