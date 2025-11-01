package listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

public class ExtentTestListener implements ITestListener {
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> test = new ThreadLocal<>();

    private static ExtentReports createInstance() {
        String reportPath = "target/extent-reports/spark.html";
        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
        ExtentReports ext = new ExtentReports();
        ext.attachReporter(spark);
        ext.setSystemInfo("Framework", "TestNG");
        ext.setSystemInfo("Selenium", "4.x");
        return ext;
    }

    @Override
    public void onStart(ITestContext context) {
        extent = createInstance();
    }

    @Override
    public void onFinish(ITestContext context) {
        if (extent != null) extent.flush();
    }

    @Override
    public void onTestStart(ITestResult result) {
        String name = result.getMethod().getMethodName();
        test.set(extent.createTest(name));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        get().pass("Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        get().fail(result.getThrowable());
        attachLatestArtifacts(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
    get().log(Status.SKIP, "Test skipped");
    }

    private ExtentTest get() {
        return test.get();
    }

    private void attachLatestArtifacts(ITestResult result) {
        try {
            String method = result.getMethod().getMethodName();
            Path debugDir = Path.of("target", "surefire-reports", "test-debug");
            if (!Files.exists(debugDir)) return;

            // Attach latest screenshot for this method, if any
            Optional<Path> latestPng = Files.list(debugDir)
                    .filter(p -> p.getFileName().toString().startsWith(method + "-") && p.toString().endsWith(".png"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
            latestPng.ifPresent(p -> {
                try {
                    get().info("Screenshot:", MediaEntityBuilder.createScreenCaptureFromPath(p.toString()).build());
                } catch (RuntimeException ignored) { }
            });

            // Add a link to the latest DOM dump
            Optional<Path> latestHtml = Files.list(debugDir)
                    .filter(p -> p.getFileName().toString().startsWith(method + "-") && p.toString().endsWith(".html"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
            latestHtml.ifPresent(p -> get().info("DOM: " + p.toString()));
        } catch (java.io.IOException ignored) { }
    }
}
