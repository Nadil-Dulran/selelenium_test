# Running Selenium + TestNG tests & CI locally

This project runs Selenium + TestNG tests under Maven. The repository includes a GitHub Actions workflow (`.github/workflows/ci.yml`) that runs tests on `ubuntu-latest`, wraps the test run with Xvfb so Chrome can run in CI, and uploads debug artifacts produced by tests (HTML DOM dumps + screenshots) from `target/surefire-reports/test-debug`.

Below are short, copy-pasteable notes for running the same steps locally.

## 1) Run tests under Xvfb (Linux / CI-like)

If you're on a Linux machine (or running in a Linux container) you can run the same command the workflow uses:

```bash
# start an X virtual framebuffer and run mvn test
xvfb-run -s "-screen 0 1920x1080x24" mvn -B -DtrimStackTrace=false test
```

This will produce debug artifacts in `target/test-debug` and (because the tests also copy them) in `target/surefire-reports/test-debug`. The CI workflow uploads the zip/folder from `target/surefire-reports/test-debug`.

## 2) Run tests on macOS (local)

On macOS you generally don't have `xvfb`. You can run tests either with a visible browser window (non-headless) or in headless mode:

- Non-headless (opens a Chrome window):

```bash
mvn test
```

- Headless (recommended for CI-like stability):

```bash
mvn -Dheadless=true test
```

or with a few CI-style flags:

```bash
mvn -B -Dheadless=true -DtrimStackTrace=false test
```

- Alternative: run the tests on a Linux VM or container (Docker) and use the `xvfb-run` command above to mimic CI.

You can also point the test to a custom Chrome/Chromium binary by exporting `CHROME_BIN` (useful on Linux CI that installs `chromium-browser`). On macOS, do NOT use the Linux path `/usr/bin/chromium-browser`. Use one of these:

```bash
## Google Chrome (macOS)
export CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
mvn -Dheadless=true test

## Chromium (macOS, if installed)
export CHROME_BIN="/Applications/Chromium.app/Contents/MacOS/Chromium"
mvn -Dheadless=true test
```

Note: This project compiles with Java 21 (Temurin) as configured in CI. Ensure you have JDK 21 locally, or update the `<release>` value in `pom.xml` to `17` if you prefer Java 17.

## 3) Manually create the zip (if you want to reproduce what CI uploads)

If you prefer to create the artifact yourself (for uploading to CI or for inspection):

```bash
# create the zip from the debug folder
zip -r target/test-debug.zip target/test-debug

# copy into the surefire-reports area so CI or other tools can find it
mkdir -p target/surefire-reports/test-debug
cp target/test-debug.zip target/surefire-reports/test-debug/
```

## 4) Run the GitHub Actions workflow locally with `act` (optional)

You can run the workflow locally using `act` (https://github.com/nektos/act). `act` runs Actions inside Docker, so Docker must be installed. Example invocation:

```bash
# run the `build-and-test` job locally using act (docker required)
act -j build-and-test
```

Notes: the workflow installs `chromium-browser` and `xvfb` inside the runner; when running with `act` those packages will be installed during the workflow run in the container. `act` may behave slightly differently from GitHub-hosted runners depending on your Docker setup.

## Where to look for artifacts

- Primary (single file): `target/surefire-reports/test-debug/test-debug.zip`
- Raw files: `target/surefire-reports/test-debug/` (contains `.html` DOM dumps and `.png` screenshots)

If you want, I can add a simple Maven profile or a `-D` property to toggle headless mode from the command line rather than modifying test source.

## Test reports (HTML)

After running tests, you have several report options:

- Surefire XML/text: found in `target/surefire-reports/` (default output).
- TestNG emailable HTML report: `target/surefire-reports/emailable-report.html` (enabled via TestNG listener).
- Maven Surefire HTML report (aggregated):

	```bash
	# generate an aggregated HTML report at target/site/surefire-report.html
	mvn -DskipTests surefire-report:report
	open target/site/surefire-report.html  # macOS
	```

In CI, you can upload the `target/site` folder or the `emailable-report.html` as an artifact for download.

### CI: site build and downloadable artifacts

The CI workflow now runs a full Maven site build:

```bash
mvn -B -DskipTests site
```

This produces `target/site/` which includes:

- `surefire-report.html` — aggregated TestNG/Surefire HTML test report
- `xref/` and `xref-test/` — source cross-references for main and test code (clickable links from reports)
- other standard Maven site pages (dependencies, plugins, project info)

Artifacts uploaded on each run:

- `site-report` — the entire `target/site` folder (contains surefire-report.html, xref, xref-test, and site assets)
- `testng-emailable-report` — `target/surefire-reports/emailable-report.html`
- `extent-report` — `target/extent-reports` (open `spark.html`)
- `test-debug-zip` and `test-debug-folder` — failure screenshots and DOM dumps

How to download:
1. Go to the repository’s Actions tab.
2. Open the run you care about (push/PR).
3. Scroll to “Artifacts” and download `site-report` (and/or other reports).
4. On macOS, open `target/site/surefire-report.html` to view test results, and browse `target/site/xref-test/` for source links.

Tip: Prefer `site-report` for a complete, navigable website (with XRef). For a simple single-file report, use `emailable-report.html`. For a modern interactive HTML, open `extent-reports/spark.html`.

## ExtentReports (Spark)

This project integrates ExtentReports for a modern, interactive HTML report.

Extent Reports are a popular open-source library used for generating detailed and customizable test reports in automation testing. The "Spark" in this context refers to the ExtentSparkReporter, which is a specific type of reporter available within the Extent Reports framework.

- Generated automatically by TestNG listener into:
	- `target/extent-reports/spark.html`

- Run tests (examples):
	```bash
	mvn test
	# or headless
	mvn -Dheadless=true test
	```

- Open locally on macOS:
	```bash
	open target/extent-reports/spark.html
	```

- In GitHub Actions, an `extent-report` artifact is uploaded (the whole `target/extent-reports` folder). Download it and open `spark.html`.

Notes:
- Screenshots/DOM captured on failures are in `target/surefire-reports/test-debug` and are referenced in the TestNG logs. The Extent TestNG adapter also shows `Reporter.log(...)` entries for quick links/context.
- Extent is configured via `src/test/resources/extent.properties`. You can adjust output paths or add a custom Spark config if desired.

## Project structure

```
pom.xml
README.md
testng.xml
.github/
	workflows/
		ci.yml
src/
	main/
		java/
			com/
				example/
					App.java
	test/
		java/
			base/
				BaseTest.java
				TestBase.java
			com/
				example/
					AppTest.java
					ContactFormTest.java
			listeners/
				ExtentTestListener.java
			testcases/
				PortfolioTests.java
		resources/
			extent.properties
target/
	site/                 # Maven Site (surefire-report.html, xref/, xref-test/, etc.)
	extent-reports/       # ExtentReports output (spark.html)
	surefire-reports/     # TestNG/Surefire raw outputs + test-debug artifacts
```
