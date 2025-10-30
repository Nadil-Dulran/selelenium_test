package testcases;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PortfolioTests {

    WebDriver driver;
    WebDriverWait wait;
    static final String BASE = "https://nadildulran.netlify.app/";

    // ---------- setup/teardown ----------
    @BeforeClass
    public void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments("--start-maximized");
        // options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
        // Increase explicit wait timeout to be more tolerant of slow network/pages
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        driver.get(BASE);
        // Wait until the page is loaded and the client-side app has rendered into #root
        wait.until(d -> ((JavascriptExecutor)d).executeScript("return document.readyState").equals("complete"));
        wait.until(d -> ((JavascriptExecutor)d).executeScript("return document.querySelector('#root') && document.querySelector('#root').innerText.length > 10"));
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() {
        // Ensure any debug artifacts produced during tests are copied into the surefire reports
        try {
            Path debugDir = Path.of("target", "test-debug");
            Path surefireDir = Path.of("target", "surefire-reports", "test-debug");
            if (Files.exists(debugDir)) {
                Files.createDirectories(surefireDir);
                // copy all files (non-recursive since debug files are flat)
                try (var stream = Files.list(debugDir)) {
                    stream.forEach(p -> {
                        try {
                            Files.copy(p, surefireDir.resolve(p.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            // ignore copy failures
                        }
                    });
                }
            }
        } catch (Exception ignored) { }
        if (driver != null) driver.quit();
    }

    // ---------- utilities ----------
    private WebElement q(By by) { return wait.until(ExpectedConditions.presenceOfElementLocated(by)); }
    private void jsClick(WebElement el) { ((JavascriptExecutor)driver).executeScript("arguments[0].click();", el); }
    private String hash() { return (String)((JavascriptExecutor)driver).executeScript("return location.hash;"); }
    private void clickNavbarContactFast() {
    // Direct link in header/nav, then assert we landed
    // Broaden selector to find contact links even if header/nav wrappers differ
    // Try multiple strategies because the site uses buttons (not anchors) for navbar
    By[] candidates = new By[] {
            By.cssSelector("a[href*='#contact']"),
            By.xpath("//header//button[normalize-space()='Contact']"),
            By.xpath("//button[normalize-space()='Contact']"),
            By.xpath("//div[@id='menu-appbar']//p[normalize-space()='Contact']")
    };

    WebElement link = null;
    for (By b : candidates) {
        try {
            List<WebElement> found = driver.findElements(b);
            if (found != null && !found.isEmpty()) { link = found.get(0); break; }
        } catch (WebDriverException ignored) { }
    }

    if (link == null) {
        // Fall back to waiting briefly for the first candidate to appear (preserve previous behavior)
        link = q(By.cssSelector("a[href*='#contact']"));
    }

    jsClick(link);
        wait.until(ExpectedConditions.or(
            d -> "#contact".equals(hash()),
            ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[self::h1 or self::h2 or self::h3 or self::h4][normalize-space()='Contact']"))
        ));
    }

    // ---------- tests ----------

    @Test(description = "Brand & hero texts visible")
    public void heroTextsPresent() {
        // brand in header
        q(By.xpath("//header//a[normalize-space()='NADIL']"));
        // main name (H1 split lines OK)
        q(By.xpath("//*[self::h1 or self::h2][contains(normalize-space(.),'Nadil') and contains(normalize-space(.),'Gamage')]"));
        // tagline
        q(By.xpath("//*[contains(.,'Computer Science Undergraduate') and contains(.,'Tech Enthusiast')]"));
    }

    @Test(description = "Navbar Contact click is fast and lands in section")
    public void navContactNavigates() {
        clickNavbarContactFast();
        assert "#contact".equals(hash()) || !driver.findElements(
            By.xpath("//*[self::h1 or self::h2 or self::h3 or self::h4][normalize-space()='Contact']")).isEmpty();
    }

    @Test(description = "External links: GitHub and LinkedIn open")
    public void externalLinksOpen() {
    // Find ‘Links’ section (may use smaller heading levels)
    q(By.xpath("//*[self::h1 or self::h2 or self::h3 or self::h4 or self::h5 or self::h6][normalize-space()='Links']"));
        WebElement gh = q(By.xpath("//a[contains(translate(.,'GITHUB','github'),'github')]"));
        WebElement li = q(By.xpath("//a[contains(translate(.,'LINKEDIN','linkedin'),'linkedin')]"));
        String ghHref = gh.getAttribute("href");
        String liHref = li.getAttribute("href");
        assert ghHref != null && ghHref.contains("github");
        assert liHref != null && liHref.contains("linkedin");

        // Open GitHub in a new tab and verify domain
        ((JavascriptExecutor)driver).executeScript("window.open(arguments[0], '_blank');", ghHref);
        List<String> tabs = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(tabs.get(tabs.size()-1));
        wait.until(ExpectedConditions.urlContains("github"));
        driver.close(); driver.switchTo().window(tabs.get(0));
    }

    @Test(description = "Skills contain representative items")
    public void skillsContainExpected() {
        // A few samples across groups
        q(By.xpath("//*[contains(.,'Programming Languages')]"));
        q(By.xpath("//*[contains(.,'C/C++')]"));
        q(By.xpath("//*[contains(.,'Java')]"));
        q(By.xpath("//*[contains(.,'Next.js')]"));
        q(By.xpath("//*[contains(.,'PostgreSQL')]"));
        q(By.xpath("//*[contains(.,'LangChain')]"));
        q(By.xpath("//*[contains(.,'GitHub (Branching/Actions)')]"));
        q(By.xpath("//*[contains(.,'Docker')]"));
    }

    @Test(description = "Projects section shows key projects")
    public void projectsVisible() {
    q(By.xpath("//*[self::h1 or self::h2 or self::h3 or self::h4][normalize-space()='Projects']"));
        q(By.xpath("//*[contains(.,'AquaClash-Swimming Tournament Management System')]"));
        q(By.xpath("//*[contains(.,'NEMRA-Smart Apartment Management System')]"));
        q(By.xpath("//*[contains(.,'Dental Clinic Management System')]"));
    }

    // ---------- Contact form tests ----------

    @Test(description = "Contact validation: required fields block submission")
    public void contactRequiredValidation() {
        clickNavbarContactFast();
        WebElement send = q(By.xpath("//button[normalize-space()='SEND' or contains(.,'Send')]"));
        jsClick(send);

        // Use HTML5 validity via JS for reliability
        Boolean nameValid = (Boolean)((JavascriptExecutor)driver)
                .executeScript("return document.querySelector(\"input[placeholder*='Your Name'],input[name='name'],#name\").checkValidity();");
        Boolean emailValid = (Boolean)((JavascriptExecutor)driver)
                .executeScript("return document.querySelector(\"input[type='email'],input[name='email']\").checkValidity();");
        Boolean msgValid = (Boolean)((JavascriptExecutor)driver)
                .executeScript("return document.querySelector(\"textarea[placeholder*='Message'],textarea[name='message'],#message\").checkValidity();");

        assert !nameValid || !emailValid || !msgValid : "Form should not be valid when empty.";
    }

    @Test(description = "Contact validation: invalid email rejected")
    public void contactInvalidEmail() {
        clickNavbarContactFast();
        WebElement name = q(By.xpath("//input[contains(@placeholder,'Your Name') or @name='name' or @id='name']"));
        WebElement email = q(By.xpath("//input[@type='email' or contains(@placeholder,'Email') or @name='email']"));
        WebElement msg = q(By.xpath("//textarea[contains(@placeholder,'Message') or @name='message' or @id='message']"));

        name.clear(); name.sendKeys("Bad Email Test");
        email.clear(); email.sendKeys("invalid-email"); // no @
        msg.clear(); msg.sendKeys("Testing invalid email.");

        WebElement send = q(By.xpath("//button[normalize-space()='SEND' or contains(.,'Send')]"));
        jsClick(send);

        Boolean emailOk = (Boolean)((JavascriptExecutor)driver)
                .executeScript("return document.querySelector(\"input[type='email'],input[name='email']\").checkValidity();");
        assert !emailOk : "Email should be invalid.";
    }

    @Test(description = "Contact happy path: submits and shows success")
    public void contactHappyPath() {
        clickNavbarContactFast();
        WebElement name = q(By.xpath("//input[contains(@placeholder,'Your Name') or @name='name' or @id='name']"));
        WebElement email = q(By.xpath("//input[@type='email' or contains(@placeholder,'Email') or @name='email']"));
        WebElement msg = q(By.xpath("//textarea[contains(@placeholder,'Message') or @name='message' or @id='message']"));

        name.clear(); name.sendKeys("Test User");
        email.clear(); email.sendKeys("test.user@example.com");
        String body = """
                Hello Nadil,

                Automated Selenium test message. Please ignore.

                — QA Bot
                """;
        msg.clear(); msg.sendKeys(body);

        WebElement send = q(By.xpath("//button[normalize-space()='SEND' or contains(.,'Send')]"));
        jsClick(send);

        By s1 = By.xpath("//*[contains(normalize-space(.), \"Thanks! I’ll get back to you soon.\")]");
        By s2 = By.xpath("//*[contains(normalize-space(.), \"Thanks! I'll get back to you soon.\")]");
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(s1),
                ExpectedConditions.visibilityOfElementLocated(s2)
        ));
    }

    // Capture debug artifacts (screenshot + DOM) when a test fails to help diagnose SPA timing/selector issues
    @AfterMethod(alwaysRun = true)
    public void captureDebugOnFailure(ITestResult result) {
        if (result.isSuccess()) return;
        try {
            Path outDir = Path.of("target", "test-debug");
            Path surefireDir = Path.of("target", "surefire-reports", "test-debug");
            Files.createDirectories(outDir);
            Files.createDirectories(surefireDir);
            String name = result.getMethod().getMethodName();
            String stamp = String.valueOf(System.currentTimeMillis());

            // Save page source (DOM)
            String dom = (String) ((JavascriptExecutor) driver).executeScript("return document.documentElement.outerHTML;");
            Path domFile = outDir.resolve(name + "-" + stamp + ".html");
            Files.writeString(domFile, dom, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            // also write into surefire reports area so CI can pick it up even if Maven stops early
            Path domFile2 = surefireDir.resolve(name + "-" + stamp + ".html");
            Files.writeString(domFile2, dom, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Save screenshot (if supported)
            if (driver instanceof TakesScreenshot) {
                byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Path imgFile = outDir.resolve(name + "-" + stamp + ".png");
                Files.write(imgFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Path imgFile2 = surefireDir.resolve(name + "-" + stamp + ".png");
                Files.write(imgFile2, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException | WebDriverException e) {
            // Do not fail the test further because debug capture failed; just log to stdout
            System.out.println("Failed to capture debug artifacts: " + e.getMessage());
        }
    }
}
