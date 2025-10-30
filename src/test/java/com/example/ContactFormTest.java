package com.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class ContactFormTest {

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless=new"); // enable for CI
        options.addArguments("--start-maximized");
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            driver.get("https://nadildulran.netlify.app/");

            // ---- robust navigation to Contact ----
            goToContactSection(driver, wait);

            // ---- form fields (generic, resilient locators) ----
            By nameBy    = By.xpath("//input[contains(@placeholder,'Your Name') or @name='name' or @id='name']");
            By emailBy   = By.xpath("//input[@type='email' or contains(@placeholder,'Email') or @name='email']");
            By messageBy = By.xpath("//textarea[contains(@placeholder,'Message') or @name='message' or @id='message']");
            By sendBy    = By.xpath("//button[normalize-space()='SEND' or contains(.,'Send')]");

            WebElement name    = wait.until(ExpectedConditions.visibilityOfElementLocated(nameBy));
            WebElement email   = wait.until(ExpectedConditions.visibilityOfElementLocated(emailBy));
            WebElement message = wait.until(ExpectedConditions.visibilityOfElementLocated(messageBy));

            name.clear();
            name.sendKeys("Test User");
            email.clear();
            email.sendKeys("test.user@example.com");

            String msg = """
                Hello Nadil,

                This is a sample message sent via an automated Selenium test.
                Please ignore.

                — QA Bot
                """;
            message.clear();
            message.sendKeys(msg);

            WebElement sendBtn = wait.until(ExpectedConditions.elementToBeClickable(sendBy));
            scrollIntoView(driver, sendBtn);
            jsClick(driver, sendBtn);

            // exact success text (handle curly/straight apostrophes)
            By successCurly   = By.xpath("//*[contains(normalize-space(.), \"Thanks! I’ll get back to you soon.\")]");
            By successStraight= By.xpath("//*[contains(normalize-space(.), \"Thanks! I'll get back to you soon.\")]");
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(successCurly),
                    ExpectedConditions.visibilityOfElementLocated(successStraight)
            ));

            System.out.println("✅ Contact form submitted: success text detected.");

        } catch (Exception e) {
            System.out.println("❌ Test failed: " + e.getMessage());
        } finally {
            driver.quit();
        }
    }

    // ---- helpers ----
    private static void goToContactSection(WebDriver driver, WebDriverWait wait) {
        // 1) Try to find a nav link and JS-click it
        By navContact = By.xpath("(//a[normalize-space()='Contact' or contains(@href,'#contact') or contains(@href,'/contact')])[1]");
        try {
            WebElement link = wait.until(ExpectedConditions.presenceOfElementLocated(navContact));
            scrollIntoView(driver, link);
            jsClick(driver, link);
            return;
        } catch (TimeoutException ignored) {}

        // 2) Force the hash (works for SPA anchors)
        try {
            ((JavascriptExecutor) driver).executeScript("location.hash='#contact';");
            return;
        } catch (JavascriptException ignored) {}

        // 3) Scroll to the section heading as last resort
        By contactHeader = By.xpath("//*[self::h1 or self::h2 or self::h3][normalize-space()='Contact']");
        try {
            WebElement hdr = wait.until(ExpectedConditions.presenceOfElementLocated(contactHeader));
            scrollIntoView(driver, hdr);
        } catch (TimeoutException te) {
            // If the site lazy-loads, just do a big scroll
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
        }
    }

    private static void scrollIntoView(WebDriver driver, WebElement el) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});", el);
    }

    private static void jsClick(WebDriver driver, WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }
}
