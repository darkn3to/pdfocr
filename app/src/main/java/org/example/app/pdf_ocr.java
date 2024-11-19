package org.example.app;

import org.example.app.components.pdfHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;

@SpringBootApplication
public class pdf_ocr {
    @Autowired
    public pdfHandler demo;
    
    public static void main(String[] args) {
        SpringApplication.run(pdf_ocr.class, args);
    }

    @Bean
    CommandLineRunner run() {
        return args -> {
            if (args.length >= 2) {
                try {
                    String sourcePath = args[0];
                    String destPath = args[1];
                    char useMultithreading = 'n'; 
                    if (args.length == 3) {
                        useMultithreading = args[2].charAt(0);
                        if (Character.toLowerCase(useMultithreading) != 'm' && Character.toLowerCase(useMultithreading) != 'n') {
                            System.out.println("Invalid flag. Please use 'm' for multithreading or 'n' for no multithreading.");
                            return;
                        }
                    }
                    demo.extractText(sourcePath, destPath, useMultithreading);
                }
                catch (Exception e) {
                    System.out.println("Failed to extract text from the PDF: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            else {
                System.out.println("Please provide a destination path. ");
            }
        };
    }
}
