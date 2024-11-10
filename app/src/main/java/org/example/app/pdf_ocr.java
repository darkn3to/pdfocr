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
            if (args.length >= 1) {
                try {
                    String filepath = args[0];
                    demo.extractText(filepath);
                }
                catch (Exception e) {
                    System.out.println("Failed to extract text from the PDF. ");
                }
            }
            else {
                System.out.println("Please provide the path to the PDF. ");
            }
        };
    }
}
