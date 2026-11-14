package com.sumit.seleniumapp.Service;

import com.sumit.seleniumapp.Controller.AutomationStatusStream;
import com.sumit.seleniumapp.Factory.DriverFactory;
import com.sumit.seleniumapp.Util.ExcelUtil;
import com.sumit.seleniumapp.Util.ExcelUtilLatLong;
import com.sumit.seleniumapp.Util.LoginDetails;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SeleniumAutomationService {

    @Autowired
    private DriverFactory driverFactory;

    @Autowired
    private ExcelUtil excelUtil;

    @Autowired
    private ExcelUtilLatLong excelUtilLatLong;

    @Autowired
    private LoginDetails loginCredentials;

    @Autowired
    private AutomationStatusStream automationStatusStream;

    private WebDriver driver;

    private static final Logger log =
            LoggerFactory.getLogger(SeleniumAutomationService.class);

    private static final int CAPTCHA_LENGTH = 5;

    private volatile boolean stopRequested = false;

    public void stopAutomation() {
        try {
            log.warn("Stop requested by user.");
            stopRequested = true;

            if (driver != null) {
                driver.quit();
            }

        } catch (Exception e) {
            log.error("Error while stopping automation", e);
        }
    }

    public void runAutomation(String userId, String password, String filePath) throws Exception {

        stopRequested = false;
        driver = driverFactory.createDriver();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            login(userId, password, wait, loginWait);

            int totalRows = excelUtil.getRowCount(filePath);
            log.info("Total farmers found in Excel: {}", totalRows);

            for (int row = 1; row <= totalRows; row++) {

                if (stopRequested) {
                    log.warn("Automation stopped by user at row {}", row);
                    break;
                }

                log.info("===== Processing Farmer Row {} =====", row);

                try {
                    Map<String, String> farmer = excelUtil.getRowData(filePath, row);

                    processFarmer(farmer, driver, wait, longWait);

                    log.info("✅ Farmer row {} completed successfully", row);

                } catch (Exception e) {


                    if (stopRequested) {
                        log.warn("Automation stop detected. Exiting loop.");
                        break;
                    }

                    log.error("❌ Farmer row {} failed: {}", row, e.getMessage(), e);

                    recoverToSafeState();
                }
            }

        } finally {
            automationStatusStream.notifyStatus("STOPPED");
            log.info("EXITING Automation");
        }
    }

    public void runAutomationLatLong(String userId, String password, String filePath) throws Exception {

        stopRequested = false;
        driver = driverFactory.createDriver();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            login(userId, password, wait, loginWait);

            int totalRows = excelUtilLatLong.getRowCount(filePath);
            log.info("LatLong Total farmers found in Excel: {}", totalRows);

            for (int row = 1; row <= totalRows; row++) {

                if (stopRequested) {
                    log.warn("LatLong Automation stopped by user at row {}", row);
                    break;
                }

                log.info("===== LatLong Processing Farmer Row {} =====", row);

                try {
                    Map<String, String> farmer = excelUtilLatLong.getRowData(filePath, row);

                    processFarmerLatLong(farmer, driver, wait, longWait);

                    log.info("✅ LatLong Farmer row {} completed successfully", row);


                } catch (Exception e) {


                    if (stopRequested) {
                        log.warn("LatLong Automation stop detected. Exiting loop.");
                        break;
                    }

                    log.error("❌ Farmer row {} failed: {}", row, e.getMessage(), e);

                    recoverToSafeState();
                }
            }

        } finally {
            automationStatusStream.notifyStatus("STOPPED");
            log.info("EXITING Automation");
        }
    }

    private void login(String userId, String password,
                       WebDriverWait wait,
                       WebDriverWait loginWait) {

        driver.get(loginCredentials.getUrl());

        new Select(wait.until(ExpectedConditions.elementToBeClickable(
                By.id("userType")))).selectByValue("lcGroup");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("localGroupUserId"))).sendKeys(userId);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("localGroupPasswordRC"))).sendKeys(password);

        loginWait.until(d ->
                d.findElement(By.id("verify"))
                        .getAttribute("value")
                        .length() >= CAPTCHA_LENGTH
        );

        driver.findElement(By.id("loginBtn")).click();

        wait.until(ExpectedConditions.urlToBe(
                "https://pgsindia-ncof.gov.in/NF/farmer/listOfGroupMembers"
        ));
    }

    private void processFarmer(Map<String, String> farmer,
                               WebDriver driver,
                               WebDriverWait wait,
                               WebDriverWait longWait) {

        log.info("Farmer Data Loaded :: {}", farmer);

        String farmerName = farmer.get("Farmer Name");

        String fatherName = Optional.ofNullable(farmer.get("Fathers Name"))
                .or(() -> Optional.ofNullable(farmer.get("Fathers Name/ Husband Name")))
                .orElse("");

        String mobileNumber = farmer.get("Mobile Number");
        String village = farmer.get("Village");
        String pinCode = farmer.get("Pincode");
        String gender = farmer.get("Gender");
        String categoryId = farmer.get("Category");

        String age = Optional.ofNullable(farmer.get("Age"))
                .or(() -> Optional.ofNullable(farmer.get("Farmer Age")))
                .orElse("");

        String totalArea = farmer.get("Total Area");

        String plotNo = Optional.ofNullable(farmer.get("Plot No"))
                .or(() -> Optional.ofNullable(farmer.get("Plot No.")))
                .orElse("");

        String plotArea = farmer.get("Organic Area");

        String khasraNo = Optional.ofNullable(farmer.get("Khata No."))
                .or(() -> Optional.ofNullable(farmer.get("Khasra No.")))
                .orElse("");

        driver.navigate().to("https://pgsindia-ncof.gov.in/NF/farmer/addFarmer");

        safeFill(driver, wait, "farmerName", farmerName);
        safeFill(driver, wait, "fatherOrHusband", fatherName);

        longWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//select[@id='village_id']/option"), 1));

        retry(3,
                () -> selectDropdownByText(driver, "village_id", village),
                "Village Selection");

        clickSafe(driver, wait, By.id("apiCallerBtn"));

        acceptAlertIfPresent(driver, 2);

        safeFill(driver, wait, "pincode", pinCode);

        if (mobileNumber != null && mobileNumber.length() >= 10) {
            safeFill(driver, wait, "mobile", mobileNumber);
        }

        retry(3,
                () -> selectGenderRobust(driver, wait, gender),
                "Gender Selection");

        retry(3,
                () -> selectDropdownByText(driver, "category", categoryId),
                "Category Selection");

        safeFill(driver, wait, "groupMemberFarmer", age);

        clickSafe(driver, longWait, By.id("submitBtnNext1"));

        waitForNextSection(driver, longWait);

        safeFill(driver, wait, "totalArea", totalArea);
        safeFill(driver, wait, "plot_area", plotArea);
        safeFill(driver, wait, "plot", plotNo);
        safeFill(driver, wait, "khasraNo", khasraNo);

        clickSafe(driver, wait, By.cssSelector(".addbtnicon-one"));
        acceptAlertIfPresent(driver, 2);

        clickSafe(driver, longWait, By.id("submitBtn"));

        acceptAlertIfPresent(driver, 2);
        acceptAlertIfPresent(driver, 2);

        log.info("🎉 Farmer registration completed successfully");
    }

    private void processFarmerLatLong(
            Map<String, String> farmer,
            WebDriver driver,
            WebDriverWait wait,
            WebDriverWait longWait) {

        log.info("Farmer LatLong Data Loaded :: {}", farmer);

        String memberId = farmer.get("Member ID");
        String latitude = farmer.get("Latitude");
        String longitude = farmer.get("Longitude");

        // 1. Open farmer list
        driver.navigate().to(
                "https://pgsindia-ncof.gov.in/NF/farmer/listOfGroupMembers?pageId=0&items=200"
        );

        // 2. Execute JavaScript function
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                "updateFarmerDetails(arguments[0]);",
                memberId
        );

        // 3. Wait for editDetails page
        longWait.until(ExpectedConditions.urlContains(
                "/NF/farmer/editDetails"
        ));

        log.info("Redirected to farmer edit page. Member ID :: {}", memberId);

        // 4. Click "Farmer Land and Other Information" tab
        WebElement landTab = longWait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                "//a[@href='#tab3' and " +
                                        "contains(normalize-space(.), 'Farmer Land and Other Information')]"
                        )
                )
        );

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                landTab
        );

        js.executeScript(
                "arguments[0].click();",
                landTab
        );

        // 5. Wait until tab3 becomes active
        longWait.until(ExpectedConditions.attributeContains(
                By.cssSelector("a[href='#tab3']"),
                "class",
                "active"
        ));

        log.info("Farmer Land and Other Information tab opened.");

        // ------------------------------------------------------------
        // 6. Wait for existing land table to load
        // ------------------------------------------------------------

        WebElement landRow = longWait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("#addRow tr")
                )
        );

        List<WebElement> cells = landRow.findElements(By.tagName("td"));
        String rowId = landRow.getAttribute("id");   // MI4025357

        if (cells.size() < 8) {
            throw new IllegalStateException(
                    "Invalid land table row. Expected at least 8 cells, found: "
                            + cells.size()
            );
        }

        // ------------------------------------------------------------
        // 7. Read existing land data from table
        // ------------------------------------------------------------

        // HTML:
        // td[0] = S.NO
        // td[1] = Plot No.
        // td[2] = Area for Natural
        // td[3] = Khasra No.
        // td[4] = Land Ownership
        // td[5] = Latitude
        // td[6] = Longitude
        // td[7] = Action

        String plotNo = cells.get(1).getText().trim();
        String plotArea = cells.get(2).getText().trim();
        String khasraNo = cells.get(3).getText().trim();

        log.info(
                "Existing land data :: Plot No={}, Plot Area={}, Khasra No={}",
                plotNo,
                plotArea,
                khasraNo
        );

        // ------------------------------------------------------------
        // 8. Fill existing land values
        // ------------------------------------------------------------

        safeFill(driver, wait, "plot_area", plotArea);
        safeFill(driver, wait, "plot", plotNo);
        safeFill(driver, wait, "khasraNo", khasraNo);

        // ------------------------------------------------------------
        // 9. Fill latitude / longitude
        // ------------------------------------------------------------

        safeFill(driver, wait, "latitude", latitude);
        safeFill(driver, wait, "longitude", longitude);

        // ------------------------------------------------------------
        // 10. Delete the existing land row
        // ------------------------------------------------------------

        if (rowId == null || rowId.isBlank()) {
            throw new IllegalStateException(
                    "Cannot delete land row: row ID is missing"
            );
        }

        if (!rowId.matches("^MI\\d+$")) {
            throw new IllegalStateException(
                    "Cannot delete land row: unexpected row ID format :: " + rowId
            );
        }

        String landId = rowId.substring(2);

        if (landId.isBlank()) {
            throw new IllegalStateException(
                    "Cannot delete land row: extracted land ID is empty. Row ID :: " + rowId
            );
        }

        log.info(
                "Deleting existing land. Row ID :: {}, Land ID :: {}",
                rowId,
                landId
        );

        js.executeScript(
                "deleteLandData(Number(arguments[0]));",
                landId
        );

        acceptAlertIfPresent(driver, 2);
        acceptAlertIfPresent(driver, 2);

        clickSafe(driver, wait, By.cssSelector(".addbtnicon-one"));

        acceptAlertIfPresent(driver, 2);

        log.info(
                "Farmer land details with LatLong updated successfully. Member ID :: {}",
                memberId
        );
    }

    private void waitForNextSection(WebDriver driver,
                                    WebDriverWait longWait) {

        longWait.until(d -> {
            try {
                acceptAlertIfPresent(driver, 2);

                if (isLoaderVisible(driver)) {
                    waitForLoaderToDisappear(driver);
                }

                try {
                    WebElement totalArea = d.findElement(By.id("totalArea"));
                    if (totalArea.isDisplayed()) {
                        return true;
                    }
                } catch (NoSuchElementException ignored) {
                }

                return false;

            } catch (UnhandledAlertException e) {
                acceptAlertIfPresent(driver, 2);
                return false;
            }
        });
    }

//    private void safeFill(WebDriver driver,
//                          WebDriverWait wait,
//                          String id,
//                          String value) {
//
//        acceptAlertIfPresent(driver, 1);
//        waitForLoaderToDisappear(driver);
//
//        WebElement input = wait.until(
//                ExpectedConditions.presenceOfElementLocated(By.id(id)));
//
//        if (!input.isEnabled()) {
//            throw new RuntimeException(id + " is disabled");
//        }
//
//        if (input.getAttribute("readonly") != null) {
//            ((JavascriptExecutor) driver).executeScript(
//                    "arguments[0].removeAttribute('readonly');",
//                    input
//            );
//        }
//
//        ((JavascriptExecutor) driver).executeScript(
//                "arguments[0].scrollIntoView({block:'center'});",
//                input
//        );
//
//        ((JavascriptExecutor) driver).executeScript(
//                "arguments[0].value='';",
//                input
//        );
//
//        input.sendKeys(value);
//
//        log.info("Filled [{}] with [{}]", id, value);
//    }

    private void safeFill(WebDriver driver,
                          WebDriverWait wait,
                          String id,
                          String value) {

        WebElement input = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(id)));

        if (!input.isEnabled()) {
            throw new RuntimeException(id + " is disabled");
        }

        if (input.getAttribute("readonly") != null) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].removeAttribute('readonly');",
                    input
            );
        }

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];",
                input,
                value
        );

        log.info("Filled [{}] with [{}]", id, value);
    }

    private void clickSafe(WebDriver driver,
                           WebDriverWait wait,
                           By locator) {

        acceptAlertIfPresent(driver, 1);

        WebElement element = wait.until(
                ExpectedConditions.presenceOfElementLocated(locator));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                element
        );

        try {
            wait.until(ExpectedConditions.elementToBeClickable(element));
            element.click();

        } catch (StaleElementReferenceException e) {

            element = wait.until(
                    ExpectedConditions.elementToBeClickable(locator));

            element.click();

        } catch (ElementClickInterceptedException e) {

            acceptAlertIfPresent(driver, 2);

            if (isLoaderVisible(driver)) {
                waitForLoaderToDisappear(driver);
            }

            element = wait.until(
                    ExpectedConditions.elementToBeClickable(locator));

            element.click();

        } catch (Exception e) {

            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].click();",
                    element
            );
        }

        acceptAlertIfPresent(driver, 1);

        if (isLoaderVisible(driver)) {
            waitForLoaderToDisappear(driver);
        }

        acceptAlertIfPresent(driver, 1);
    }

    private boolean isLoaderVisible(WebDriver driver) {
        try {
            WebElement loader = driver.findElement(By.id("loadingDiv"));
            return loader.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private void waitForLoaderToDisappear(WebDriver driver) {
        try {
            WebDriverWait loaderWait =
                    new WebDriverWait(driver, Duration.ofSeconds(3));

            loaderWait.until(d -> {
                try {
                    acceptAlertIfPresent(driver, 1);

                    WebElement loader = d.findElement(By.id("loadingDiv"));

                    return !loader.isDisplayed();

                } catch (NoSuchElementException e) {
                    return true;

                } catch (StaleElementReferenceException e) {
                    return true;

                } catch (UnhandledAlertException e) {
                    acceptAlertIfPresent(driver, 2);
                    return true;
                }
            });

        } catch (TimeoutException e) {
            log.warn("Loader timeout. Continuing...");
        }
    }

    private void selectGenderRobust(WebDriver driver,
                                    WebDriverWait wait,
                                    String gender) {

        String normalizedGender = normalize(gender);

        By locator = By.xpath(
                "//input[@type='radio' and @name='gn' and " +
                        "translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='" +
                        normalizedGender.toLowerCase() + "']"
        );

        WebElement radio = wait.until(
                ExpectedConditions.presenceOfElementLocated(locator));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                radio);

        try {
            wait.until(ExpectedConditions.elementToBeClickable(radio)).click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].click();",
                    radio);
        }

        if (!radio.isSelected()) {
            throw new RuntimeException(
                    "Gender radio not selected for value: " + gender);
        }

        log.info("Gender selected successfully: {}", normalizedGender);
    }

    private void selectDropdownByText(WebDriver driver,
                                      String id,
                                      String text) {

        Select select = new Select(driver.findElement(By.id(id)));

        String normalizedInput = normalize(text);

        for (WebElement option : select.getOptions()) {

            String normalizedOption = normalize(option.getText());

            if (normalizedOption.equalsIgnoreCase(normalizedInput)) {
                option.click();
                return;
            }
        }

        throw new RuntimeException(
                "Option '" + text + "' not found for dropdown id: " + id);
    }

    private String normalize(String value) {

        if (value == null) return "";

        value = value.trim().toLowerCase();

        switch (value) {
            case "gen":
            case "general":
                return "General";

            case "m":
            case "male":
                return "Male";

            case "f":
            case "female":
                return "Female";

            default:
                return value;
        }
    }

    private void acceptAlertIfPresent(WebDriver driver, int seconds) {

        try {
            WebDriverWait shortWait =
                    new WebDriverWait(driver, Duration.ofSeconds(seconds));

            Alert alert = shortWait.until(
                    ExpectedConditions.alertIsPresent());

            log.info("Alert :: {}", alert.getText());

            alert.accept();

        } catch (TimeoutException ignored) {
            // No alert found
        }
    }

    private void retry(int attempts,
                       Runnable action,
                       String stepName) {

        RuntimeException last = null;

        for (int i = 1; i <= attempts; i++) {
            try {
                action.run();
                return;

            } catch (RuntimeException e) {
                last = e;

                log.warn("{} failed. Retry {}/{}",
                        stepName, i, attempts);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }

        throw new RuntimeException(
                stepName + " failed after retries", last);
    }

    private void recoverToSafeState() {

        try {
            acceptAlertIfPresent(driver, 2);

            driver.navigate().to(
                    "https://pgsindia-ncof.gov.in/NF/farmer/listOfGroupMembers");

        } catch (Exception e) {
            log.error("Recovery failed, refreshing browser", e);

            try {
                driver.navigate().refresh();
            } catch (Exception ignored) {
            }
        }
    }
}