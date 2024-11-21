package org.example.app.components;

import org.example.app.services.imageProc;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

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
    private imageProc process;       // image processing not really working right now.

    private PDDocument newDoc;

    private Tesseract inst;

    float fontSize;

    public pdfHandler() {
        inst = new Tesseract();
        inst.setDatapath("C:\\tessdata");
        //inst.setDatapath("./tessdata");      //comment out the line above (and unocomment the current line) or ensure that you have C:\tessdata if you want to build an executable.
        inst.setTessVariable("tessedit_create_hocr", "1");
        //inst.setTessVariable("tessedit_write_images", "1");
    }

    public void extractText(String path, String destPath, char useMultithreading) throws Exception {
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Home: " + System.getProperty("java.home"));
        long startTime = System.currentTimeMillis();
        // PDDocument newDoc = null;
        final PDDocument doc;
        try {
            doc = Loader.loadPDF(new File(path));
        } catch (IOException e) {
            System.err.println("Failed to load the PDF file: " + e.getMessage());
            return;
        }

        newDoc = new PDDocument();
        ExecutorService scheduler = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Void>> futures = new ArrayList<>();
        try {
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

            if (useMultithreading == 'm') {
                System.out.println("Using multithreading to process the PDF. ");
                for (int i = 0; i < doc.getNumberOfPages(); i++) {
                    final int pageIndex = i;
                    pdfHandler handler = new pdfHandler();
                    Future<Void> future = scheduler.submit(() -> {
                        try {
                            final PDPage page = doc.getPage(pageIndex);
                            final PDPage newPage = newDoc.getPage(pageIndex);
                            final BufferedImage image = renderPageToImage(doc, pageIndex);
                            // final BufferedImage processedImage = process.processImage(image);
                            final String hocrData = performOCR(handler.inst, image);
                            addPageWithText(newPage, image, hocrData, pageIndex);
                        } catch (IOException | TesseractException e) {
                            e.printStackTrace();
                        }
                        return null;
                    });
                    futures.add(future);
                }
                /*for (int i = 0; i < numPages; i++) {
                    PDPage page = doc.getPage(i);
                    PDPage newPage = newDoc.getPage(i);
                    BufferedImage image = renderPageToImage(doc, i);
                    BufferedImage processedImage = process.processImage(image);
                    String hocrData = performOCR(image);
                    addPageWithText(newDoc, newPage, image, hocrData, i);
                }*/

                for (Future<Void> future : futures) {
                    future.get();
                }
            } else {
                for (int i = 0; i < numPages; i++) {
                    PDPage page = doc.getPage(i);
                    PDPage newPage = newDoc.getPage(i);
                    BufferedImage image = renderPageToImage(doc, i);
                    // BufferedImage processedImage = process.processImage(image);
                    String hocrData = performOCR(inst, image);
                    addPageWithText(newPage, image, hocrData, i);
                }
            }

            System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) + "ms");
            saveDocument(newDoc, path, destPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to process the PDF: " + e.getMessage());
        } finally {
            scheduler.shutdown();
            closeDocument(doc);
            closeDocument(newDoc);
        }
    }

    private BufferedImage renderPageToImage(PDDocument doc, int pageIndex) throws IOException {
        PDFRenderer renderer = new PDFRenderer(doc);
        return renderer.renderImageWithDPI(pageIndex, 300);
    }

    private String performOCR(Tesseract inst, BufferedImage image) throws TesseractException {
        return inst.doOCR(image);
    }

    private void addPageWithText(PDPage newPage, BufferedImage image, String hocrData, int i) throws IOException {
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

        //File imageFile = new File("temp_image_" + i + ".png");
        // ImageIO.write(image, "png", imageFile);
        //ImageIO.write(image, "png", imageFile);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        synchronized (newDoc) {
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(newDoc, imageBytes, "temp_image_" + i);
            PDPageContentStream contentStream = new PDPageContentStream(newDoc, newPage);
            contentStream.drawImage(pdImage, 0, 0, newPage.getMediaBox().getWidth(), newPage.getMediaBox().getHeight());
            contentStream.beginText();
            contentStream.appendRawCommands("3 Tr ");
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            //contentStream.setNonStrokingColor(new Color(0, 0, 0));

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

                fontSize = (float) ((Float.parseFloat(xSize) * scaleY) * 0.92);

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
    }

    private void saveDocument(PDDocument doc, String originalPath, String destPath) throws IOException {
        String outputName = "modified_" + new File(originalPath).getName();
        File outputPdfFile = new File(destPath, outputName);
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
