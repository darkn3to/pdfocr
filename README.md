<h1 align="center">pdfocr - A Spring Boot App</h1>

This application converts image-based PDFs into text-embedded PDFs using Tesseract OCR. It leverages Tesseract for text extraction and Apache PDFBox for PDF manipulation, providing an efficient solution for making scanned documents searchable and selectable. 

## Features

• Utilizes Tesseract OCR to accurately extract text from images within PDF files.

• Ensures the output PDF retains the original layout and formatting while embedding the extracted text.

• Multi-threading support for processing mutiple pages in parallel. 


## Requirements / Dependencies
• <b>JDK</b> - Java Development Kit.

• <b>Tess4j</b> library for performing ocr.

• <b>Pdfbox</b> library for pdf manipulation.

• <b>Gradle</b> - a build automation tool.


## Usage
1. Clone the repository by using the command:
   ```cmd
   git clone https://github.com/darkn3to/pdfocr.git
   ```
   or simply download the zip file from the code dropdown button above.
   
2. Navigate to the cloned directory.

3. Run the command:
    ```cmd
    ./gradlew clean shadowJar
    ```

4. Run the jar file using:
    ```cmd
    java -jar app/build/libs/pdf_ocr-1.0-all.jar <source_file_path> <dest_file_path>
    ```
    
5. (Optional) One may also provide the 'm' flag as a third parameter to use the multi-threading funtionality.
    ```cmd
    java -jar app/build/libs/pdf_ocr-1.0-all.jar <source_file_path> <dest_file_path> m
    ```

## Packaged Binaries
   You may download the application from the 'Releases' tab. The pdfocr.exe is a CLI-based application that can be executed by navigating to the directory having the .exe file and running:
    ```
    pdfocr <source_file_path> <dest_file_path>
    ```
    or 
    ```
    pdfocr <source_file_path> <dest_file_path> m
    ``` .

### NOTE: 
Please ensure that you have tessdata installed on C: drive or put it in the build/libs folder if you want to use your own tessdata with other languages included. Also ensure that you have opencv wrapper for java installed if you want to use image processing using opencv.
