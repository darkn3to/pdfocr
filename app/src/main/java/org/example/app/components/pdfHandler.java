package org.example.app.components;

import org.example.app.services.imageProc;

import java.io.File;
import java.util.List;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class pdfHandler {

    @Autowired
    private imageProc process;

    private Tesseract inst;

    float fontSize;

    public pdfHandler() {
        inst = new Tesseract();
        inst.setDatapath("C:\\tessdata");
        inst.setLanguage("eng");
        inst.setTessVariable("tessedit_create_hocr", "1");
    }

    public void extractText(String path) throws Exception {
        long startTime = System.currentTimeMillis();
        PDDocument doc = null, newDoc = null;
        
        ExecutorService threads = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        
        try {
            doc = Loader.loadPDF(new File(path));
            newDoc = new PDDocument();
            int numPages = doc.getNumberOfPages();

            for (int i = 0; i < numPages; i++) {
                PDPage page = doc.getPage(i);
                PDRectangle mb = page.getMediaBox();
                boolean isLandscape = mb.getWidth() > mb.getHeight();

                PDPage newPage = new PDPage();
                if (isLandscape) {
                    newPage.setMediaBox(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                } else {
                    newPage.setMediaBox(PDRectangle.A4);
                }
                newDoc.addPage(newPage);
            }

            for (int i = 0; i < numPages; i++) {
                PDPage page = doc.getPage(i);
                PDPage newPage = newDoc.getPage(i);
                BufferedImage image = renderPageToImage(doc, i);
                BufferedImage processedImage = process.processImage(image);
                String hocrData = performOCR(image);
                addPageWithText(newDoc, newPage, image, hocrData, i);
            }

            System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) + "ms");
            saveDocument(newDoc, path);
        } catch (IOException | TesseractException e) {
            e.printStackTrace();
            System.err.println("Failed to process the PDF: " + e.getMessage());
        } finally {
            closeDocument(doc);
            closeDocument(newDoc);
        }
    }

    private BufferedImage renderPageToImage(PDDocument doc, int pageIndex) throws IOException {
        PDFRenderer renderer = new PDFRenderer(doc);
        return renderer.renderImageWithDPI(pageIndex, 300);
    }

    private String performOCR(BufferedImage image) throws TesseractException {
        return inst.doOCR(image);
    }

    private void addPageWithText(PDDocument newDoc, PDPage newPage, BufferedImage image, String hocrData, int i) throws IOException {
        /*PDRectangle mb = page.getMediaBox();
        boolean isLandscape = mb.getWidth() > mb.getHeight();

        PDPage newPage = new PDPage();
        if (isLandscape) {
            newPage.setMediaBox(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
        } else {
            newPage.setMediaBox(PDRectangle.A4);
        }
        newDoc.addPage(newPage);*/

        float pageWidth = newPage.getMediaBox().getWidth();
        float pageHeight = newPage.getMediaBox().getHeight();
        float scaleX = pageWidth / image.getWidth();
        float scaleY = pageHeight / image.getHeight();

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

        Document docHocr = Jsoup.parse(hocrData);
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

            fontSize = (float) ((Float.parseFloat(xSize) * scaleY) * 0.9);

            Elements words = line.select(".ocrx_word");
            for (Element word : words) {
                String text = word.text();
                String title = word.attr("title");
                String[] wordToken = title.split(";");
                String[] bbox = wordToken[0].replace("bbox ", "").split(" ");
                String conf = wordToken[1].replace("x_wconf ", "");
                if (Float.parseFloat(conf) > 5.0) {
                    int left, bottom;

                    left = Integer.parseInt(bbox[0]);
                    bottom = Integer.parseInt(bbox[3]);

                    float x = left * scaleX;
                    float y = (image.getHeight() - bottom) * scaleY;

                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);
                    contentStream.newLineAtOffset(x, y);
                    contentStream.showText(text);
                    contentStream.newLineAtOffset(-x, -y);
                }
            }
        }

        contentStream.endText();
        contentStream.close();
    }

    private void saveDocument(PDDocument doc, String originalPath) throws IOException {
        File outputPdfFile = new File("modified_" + new File(originalPath).getName());
        doc.save(outputPdfFile);
        System.out.println("Modified PDF saved successfully at: " + outputPdfFile.getAbsolutePath());
    }

    private void closeDocument(PDDocument doc) {
        if (doc != null) {
            try {
                doc.close();
            } catch (IOException e) {
                System.err.println("Failed to close the PDF: " + e.getMessage());
            }
        }
    }
}
