package com.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GoogleTest {

    private WebDriver driver;

    @BeforeEach
    public void setUp() {
        // ChromeDriver must be installed and available in PATH
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.manage().window().maximize();
    }

    @Test
    public void testOpenExampleDotCom() {
        driver.get("https://example.com");
        String title = driver.getTitle();
        System.out.println("Page title: " + title);
        assertTrue(title.contains("Example"), "Title should contain 'Example'");
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}

