<h1 align="center">pdfocr - A Spring Boot App</h1>

This application converts image-based PDFs into text-embedded PDFs using Tesseract OCR. It leverages Tesseract for text extraction and Apache PDFBox for PDF manipulation, providing an efficient solution for making scanned documents searchable and selectable. 

## Features

• Utilizes Tesseract OCR to accurately extract text from images within PDF files.

• Ensures the output PDF retains the original layout and formatting while embedding the extracted text.

• Multi-threading support for processing mutiple pages in parallel. 


## Requirements
• <b>JDK</b> 

## Installation
1. Open terminal.

2. Clone the repository by using the command:
   ```cmd
   git clone https://github.com/darkn3to/pdfocr.git
   ```
   or simply download the zip file from the code dropdown button above.


## Usage

1. Navigate to the cloned directory.

2. Run the command:
    ```cmd
    ./gradlew clean shadowJar
    ```

3. Run the jar file using:
    ```cmd
    java -jar app/build/libs/pdf_ocr-1.0-all.jar <source_file_path> <dest_file_path>
    ```


## Packaged Binaries
   You may download the application from the 'Releases' tab.
