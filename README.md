# Running tests & CI locally

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

On macOS you generally don't have `xvfb`. Two options:

- Quick: run Chrome in headless mode. Open `src/test/java/testcases/PortfolioTests.java` and uncomment the headless option:

```java
// options.addArguments("--headless=new");
```

Then run:

```bash
mvn -B -DtrimStackTrace=false test
```

- Alternative: run the tests on a Linux VM or container (Docker) and use the `xvfb-run` command above to mimic CI.

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
