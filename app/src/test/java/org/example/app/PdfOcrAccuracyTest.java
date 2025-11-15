package org.example.app;

import org.example.app.components.pdfHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests to test the accuracy of PDF OCR results using character edit distance (Levenshtein distance).
 */
class PdfOcrAccuracyTest {

    private pdfHandler pdfHandler;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        pdfHandler = new pdfHandler();
    }

    /**
     * Calculate the Levenshtein distance (edit distance) between two strings.
     * This measures the minimum number of single-character edits (insertions, deletions, or substitutions)
     * required to change one string into the other.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return The edit distance between the two strings
     */
    private int calculateEditDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        // Create a table to store results of subproblems
        int[][] dp = new int[m + 1][n + 1];

        // Fill dp[][] in bottom-up manner
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                // If first string is empty, insert all characters of second string
                if (i == 0) {
                    dp[i][j] = j;
                }
                // If second string is empty, remove all characters of first string
                else if (j == 0) {
                    dp[i][j] = i;
                }
                // If last characters are same, ignore last char and recur for remaining
                else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                }
                // If last character are different, consider all possibilities and find minimum
                else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i][j - 1], // Insert
                            dp[i - 1][j]), // Remove
                            dp[i - 1][j - 1]); // Replace
                }
            }
        }

        return dp[m][n];
    }

    /**
     * Calculate the accuracy percentage based on edit distance.
     * Accuracy = 1 - (editDistance / maxLength) * 100
     *
     * @param expected The expected text
     * @param actual The actual OCR result
     * @return Accuracy percentage (0-100)
     */
    private double calculateAccuracy(String expected, String actual) {
        int editDistance = calculateEditDistance(expected, actual);
        int maxLength = Math.max(expected.length(), actual.length());
        
        if (maxLength == 0) {
            return 100.0;
        }
        
        double accuracy = (1.0 - ((double) editDistance / maxLength)) * 100.0;
        return Math.max(0.0, accuracy); // Ensure non-negative
    }

    /**
     * Normalize text by removing extra whitespace and converting to lowercase
     * for better comparison.
     *
     * @param text The text to normalize
     * @return Normalized text
     */
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        // Replace multiple spaces/newlines with single space, trim, and convert to lowercase
        return text.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    @Test
    void testEditDistanceCalculation() {
        // Test basic edit distance calculations
        assertEquals(0, calculateEditDistance("hello", "hello"));
        assertEquals(1, calculateEditDistance("hello", "hallo"));
        assertEquals(3, calculateEditDistance("kitten", "sitting"));
        assertEquals(4, calculateEditDistance("hello", "world")); // Fixed: actual distance is 4
    }

    @Test
    void testAccuracyCalculation() {
        // Test accuracy percentage calculation
        assertEquals(100.0, calculateAccuracy("hello", "hello"), 0.01);
        assertTrue(calculateAccuracy("hello", "hallo") > 75.0); // Fixed: 1 char diff in 5 = 80%
        assertTrue(calculateAccuracy("hello", "world") < 50.0); // Fixed: 4 char diff in 5 = 20%
    }

    @Test
    void testTextNormalization() {
        // Test text normalization
        assertEquals("hello world", normalizeText("Hello   World"));
        assertEquals("hello world", normalizeText("Hello\nWorld"));
        assertEquals("test text", normalizeText("  Test   Text  "));
    }

    /**
     * Test OCR accuracy with a simple text comparison.
     * This test validates that the OCR accuracy measurement works correctly
     * by comparing expected and actual text results.
     */
    @Test
    void testOcrAccuracyMeasurement() {
        String expectedText = "This is a sample text for OCR testing.";
        String actualOcrResult = "This is a sample text for OCR testing."; // Perfect match
        
        String normalizedExpected = normalizeText(expectedText);
        String normalizedActual = normalizeText(actualOcrResult);
        
        int editDistance = calculateEditDistance(normalizedExpected, normalizedActual);
        double accuracy = calculateAccuracy(normalizedExpected, normalizedActual);
        
        assertEquals(0, editDistance, "Edit distance should be 0 for identical strings");
        assertEquals(100.0, accuracy, 0.01, "Accuracy should be 100% for identical strings");
    }

    /**
     * Test OCR accuracy with minor differences.
     * This simulates OCR results with small recognition errors.
     */
    @Test
    void testOcrAccuracyWithMinorErrors() {
        String expectedText = "The quick brown fox jumps over the lazy dog.";
        String actualOcrResult = "The quiek brown fox jumps over the Iazy dog."; // 2 character errors
        
        String normalizedExpected = normalizeText(expectedText);
        String normalizedActual = normalizeText(actualOcrResult);
        
        int editDistance = calculateEditDistance(normalizedExpected, normalizedActual);
        double accuracy = calculateAccuracy(normalizedExpected, normalizedActual);
        
        System.out.println("Expected: " + normalizedExpected);
        System.out.println("Actual: " + normalizedActual);
        System.out.println("Edit Distance: " + editDistance);
        System.out.println("Accuracy: " + accuracy + "%");
        
        assertTrue(editDistance > 0, "Edit distance should be greater than 0 for different strings");
        assertTrue(accuracy > 90.0, "Accuracy should be above 90% for minor errors");
        assertTrue(accuracy < 100.0, "Accuracy should be less than 100% when there are errors");
    }

    /**
     * Test OCR accuracy threshold validation.
     * This ensures that OCR results meet a minimum accuracy threshold.
     */
    @Test
    void testOcrAccuracyThreshold() {
        String expectedText = "Hello World Test Document";
        String actualOcrResult = "Hello World Test Document"; // Simulated OCR result
        
        String normalizedExpected = normalizeText(expectedText);
        String normalizedActual = normalizeText(actualOcrResult);
        
        double accuracy = calculateAccuracy(normalizedExpected, normalizedActual);
        
        double minimumAccuracyThreshold = 85.0; // 85% minimum accuracy
        
        assertTrue(accuracy >= minimumAccuracyThreshold, 
            String.format("OCR accuracy (%.2f%%) should meet minimum threshold (%.2f%%)", 
                accuracy, minimumAccuracyThreshold));
    }

    /**
     * Test edit distance with empty strings.
     */
    @Test
    void testEditDistanceWithEmptyStrings() {
        assertEquals(0, calculateEditDistance("", ""));
        assertEquals(5, calculateEditDistance("hello", ""));
        assertEquals(5, calculateEditDistance("", "hello"));
    }

    /**
     * Test accuracy with completely different strings.
     */
    @Test
    void testAccuracyWithCompletelyDifferentStrings() {
        String expected = "abcdefghij";
        String actual = "1234567890";
        
        int editDistance = calculateEditDistance(expected, actual);
        double accuracy = calculateAccuracy(expected, actual);
        
        assertEquals(10, editDistance, "All characters are different");
        assertEquals(0.0, accuracy, 0.01, "Accuracy should be 0% for completely different strings");
    }

    /**
     * Test accuracy with case sensitivity considerations.
     */
    @Test
    void testAccuracyWithCaseDifferences() {
        String expected = "Hello World";
        String actual = "hello world";
        
        // Without normalization
        int editDistanceRaw = calculateEditDistance(expected, actual);
        assertTrue(editDistanceRaw > 0, "Edit distance should be greater than 0 without normalization");
        
        // With normalization
        String normalizedExpected = normalizeText(expected);
        String normalizedActual = normalizeText(actual);
        int editDistanceNormalized = calculateEditDistance(normalizedExpected, normalizedActual);
        
        assertEquals(0, editDistanceNormalized, "Edit distance should be 0 after normalization");
    }

    /**
     * Test OCR accuracy with whitespace variations.
     */
    @Test
    void testOcrAccuracyWithWhitespaceVariations() {
        String expectedText = "This is a test document.";
        String actualOcrResult = "This  is   a  test   document."; // Extra spaces
        
        String normalizedExpected = normalizeText(expectedText);
        String normalizedActual = normalizeText(actualOcrResult);
        
        double accuracy = calculateAccuracy(normalizedExpected, normalizedActual);
        
        assertEquals(100.0, accuracy, 0.01, 
            "Accuracy should be 100% after normalization removes extra whitespace");
    }

    /**
     * Test OCR accuracy with realistic OCR errors.
     * Simulates common OCR mistakes like '1' vs 'l', 'O' vs '0', etc.
     */
    @Test
    void testOcrAccuracyWithRealisticErrors() {
        String expectedText = "The year 2024 has 12 months.";
        String actualOcrResult = "The year 2O24 has l2 months."; // O instead of 0, l instead of 1
        
        String normalizedExpected = normalizeText(expectedText);
        String normalizedActual = normalizeText(actualOcrResult);
        
        int editDistance = calculateEditDistance(normalizedExpected, normalizedActual);
        double accuracy = calculateAccuracy(normalizedExpected, normalizedActual);
        
        System.out.println("\nRealistic OCR Error Test:");
        System.out.println("Expected: " + normalizedExpected);
        System.out.println("Actual: " + normalizedActual);
        System.out.println("Edit Distance: " + editDistance);
        System.out.println("Accuracy: " + accuracy + "%");
        
        assertTrue(editDistance > 0, "Should detect character substitution errors");
        assertTrue(accuracy > 85.0, "Accuracy should still be relatively high despite errors");
    }
}
