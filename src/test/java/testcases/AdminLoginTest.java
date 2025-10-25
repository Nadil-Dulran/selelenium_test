package testcases;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;

public class AdminLoginTest {

    public static void main(String[] args) {
        // Setup ChromeDriver
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();

        try {
            // Open the site
            driver.get("https://aquachamps-b3ajaeb3hzcqd8a4.eastasia-01.azurewebsites.net");
            driver.manage().window().maximize();

            // Wait until the Admin Login button is clickable
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement adminLoginBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("a.admin-login-btn"))
            );
            adminLoginBtn.click();

            // Wait until the login form loads
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@type='email']")));

            // Fill in email and password
            WebElement emailField = driver.findElement(By.xpath("//input[@type='email']"));
            WebElement passwordField = driver.findElement(By.xpath("//input[@type='password']"));
            emailField.sendKeys("admin@stms.com");
            passwordField.sendKeys("Admin#123"); 

            // Click the Sign In button
            WebElement signInButton = driver.findElement(By.xpath("//button[contains(.,'Sign In')]"));
            signInButton.click();

            // Optional: wait for navigation or success indicator
            wait.until(ExpectedConditions.urlContains("/dashboard"));
            System.out.println("✅ Admin logged in successfully!");

        } catch (Exception e) {
            // e.printStackTrace();
        } finally {
            // Close the browser
            driver.quit();
        }
    }
}
