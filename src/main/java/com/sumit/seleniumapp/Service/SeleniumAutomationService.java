package com.sumit.seleniumapp.Service;

import com.sumit.seleniumapp.Controller.AutomationStatusStream;
import com.sumit.seleniumapp.Factory.DriverFactory;
import com.sumit.seleniumapp.Util.ExcelUtil;
import com.sumit.seleniumapp.Util.LoginDetails;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeleniumAutomationService {

    @Autowired
    private DriverFactory driverFactory;

    @Autowired
    private ExcelUtil excelUtil;

    @Autowired
    private LoginDetails loginCredentials;

    @Autowired
    AutomationStatusStream automationStatusStream;

    public boolean isAutomationEnabled = false;

    WebDriver driver;

    private static final Logger log =
            LoggerFactory.getLogger(SeleniumAutomationService.class);

    private static final int CAPTCHA_LENGTH = 5;

    public void stopAutomation() throws Exception {
        isAutomationEnabled = false;
        log.warn("Stopping Automation Safely.");
        driver.quit();
    }

    public void runAutomation(String userId, String password, String filePath) throws Exception {

        /* =========================
           STEP 2: INIT DRIVER
           ========================= */
        isAutomationEnabled = true;
        driver = driverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebDriverWait LoginWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(60));

        /* =========================
           STEP 3: LOGIN
           ========================= */
        driver.get(loginCredentials.getUrl());

        new Select(wait.until(ExpectedConditions.elementToBeClickable(
                By.id("userType")))).selectByValue("lcGroup");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("localGroupUserId"))).sendKeys(userId);//loginCredentials.getUsername()

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("localGroupPasswordRC"))).sendKeys(password);//loginCredentials.getPassword()

        LoginWait.until(d ->
                d.findElement(By.id("verify"))
                        .getAttribute("value").length() >= CAPTCHA_LENGTH
        );

        driver.findElement(By.id("loginBtn")).click();

        wait.until(ExpectedConditions.urlToBe(
                "https://pgsindia-ncof.gov.in/NF/farmer/listOfGroupMembers"
        ));

        int totalRows = excelUtil.getRowCount(filePath);
        log.info("Total farmers found in Excel: {}", totalRows);



        for (int row = 1; row <= totalRows; row++) {

            log.info(" Automation Status :: " + isAutomationEnabled);

            if(!isAutomationEnabled){
                break;
            }

            log.info("===== Processing Farmer Row {} =====", row);

            try {
                Map<String, String> farmer = excelUtil.getRowData(filePath,row);
                processFarmer(farmer, driver, wait, longWait);
                excelUtil.writeResult(
                        filePath,
                        row,
                        "SUCCESS",
                        "Farmer added successfully"
                );

                log.info("✅ Farmer row {} completed successfully", row);

            } catch (Exception e) {
                excelUtil.writeResult(
                        filePath,
                        row,
                        "FAILED",
                        e.getMessage()
                );
                log.error("❌ Farmer row {} failed: {}", row, e.getMessage(), e);
                //automationStatusStream.notifyStatus("FAILED");
                break;
            }
        }
        log.info("EXITING Automation!!");
        automationStatusStream.notifyStatus("STOPPED");
    }

    public void runAutomation2(String userId, String password, String year, String season, String month, String fromDate, String toDate, String filePath) throws Exception {

        /* =========================
           INIT DRIVER
           ========================= */
        isAutomationEnabled= true;

        driver = driverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebDriverWait LoginWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(60));

        /* =========================
           LOGIN
           ========================= */
        driver.get(loginCredentials.getUrl());

        new Select(wait.until(ExpectedConditions.elementToBeClickable(
                By.id("userType")))).selectByValue("lcGroup");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("localGroupUserId"))).sendKeys(userId);//loginCredentials.getUsername()

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("localGroupPasswordRC"))).sendKeys(password);//loginCredentials.getPassword()

        LoginWait.until(d ->
                d.findElement(By.id("verify"))
                        .getAttribute("value").length() >= CAPTCHA_LENGTH
        );

        driver.findElement(By.id("loginBtn")).click();

        wait.until(ExpectedConditions.urlToBe(
                "https://pgsindia-ncof.gov.in/NF/farmer/listOfGroupMembers"
        ));

        log.info("Redirecting to Peers Appraisals Add Page...");

         /* ==============================
           OPEN PEERS APPRAISAL PAGE
           ============================== */
        driver.navigate().to("https://pgsindia-ncof.gov.in/NF/farmer/peer/add?id=141095");

        wait.until(ExpectedConditions.urlToBe(
                "https://pgsindia-ncof.gov.in/NF/farmer/peer/add?id=141095"
        ));

       // https://pgsindia-ncof.gov.in/NF/farmer/peer/add?id=141036

        /* ============
           SELECT YEAR
           ============ */
        log.info("Selecting Year...");
        longWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//select[@id='financialYearMasterId']/option"), 1
        ));

        selectDropdownByText(driver, "financialYearMasterId", year);

         /* ============
           SELECT SEASON
           ============= */
        log.info("Selecting Season...");
        longWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//select[@id='season']/option"), 1
        ));

        selectDropdownByText(driver, "season", season);


        /* ============
           SELECT MONTH
           ============= */
        log.info("Selecting Month...");
        longWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//select[@id='month']/option"), 1
        ));

        selectDropdownByText(driver, "month", month);

        /* =====================
           SELECT RANDOM FARMERS
           ==================== */
        // wait till options are loaded
        log.info("Selecting Random 6 Farmers from List...");
        longWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//select[@id='multiselect']/option"), 6
        ));

        Select select = new Select(driver.findElement(By.id("multiselect")));

        if (!select.isMultiple()) {
            throw new RuntimeException("Dropdown does not support multiple selection");
        }

        List<WebElement> options = select.getOptions();

        // filter valid options (optional but recommended)
        List<WebElement> validOptions = options.stream()
                .filter(o -> o.isEnabled())
                .filter(o -> !o.getText().trim().isEmpty())
                .collect(Collectors.toList());

        if (validOptions.size() < 6) {
            throw new RuntimeException("Less than 6 options available");
        }

        // shuffle and pick first 6
        Collections.shuffle(validOptions);

        for (int i = 0; i < 6; i++) {
            select.selectByVisibleText(validOptions.get(i).getText());
        }

        driver.findElement(By.id("multiselect_rightSelected")).click();

        /* ==================
           SELECT DATE RANGE
           ================== */
        log.info("Selecting Dates of Peers Appraisals...");
        setDateRange(driver, fromDate, toDate);

        /* ==================
           SELECT FARMERS RANGE
           ================== */
        log.info("COUNTING FARMERS...");
        longWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//select[@id='fmrExpYields']/option"), 1
        ));

        WebElement farmerDropdown = driver.findElement(By.id("fmrExpYields"));
        Select sel = new Select(farmerDropdown);

        List<String> allFarmers = sel.getOptions().stream()
                .map(WebElement::getText)
                .filter(t -> !t.trim().isEmpty())
                .filter(t -> !t.equalsIgnoreCase("--Select--"))
                .toList();

        log.info("Total farmers to process: " + allFarmers.size());

        int totalExcelRows = excelUtil.getRowCount(filePath);
        log.info("Total farmers found in Excel: {}", totalExcelRows);

        Set<String> usedFarmers = new HashSet<>();

        boolean allSuccess = true;

        if (totalExcelRows == allFarmers.size()) {
            log.info("Proceeding farmers details from Excel");
//            for (int row = 1; row <= totalExcelRows; row++) {
//
//                log.info(" Automation Status :: " + isAutomationEnabled);
//
//                if(!isAutomationEnabled){
//                    allSuccess=false;
//                    break;
//                }
//
//
//                log.info("===== Processing Farmer Row {} =====", row);
//
//
//                try {
//                    Map<String, String> farmer = excelUtil.getRowData(filePath, row);
//                    addFarmerDetails(farmer, usedFarmers, driver, wait, longWait);
//
//                    log.info("✅ Farmer row {} insert done", row);
//                    try {
//                        Thread.sleep(1500);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                } catch (Exception e) {
//
//                    log.error("❌ Farmer row {} failed: {}", row, e.getMessage(), e);
//                    automationStatusStream.notifyStatus("FAILED");
//                    allSuccess=false;
//                    break;
//                }
//            }

            if(allSuccess) {
                log.info("All farmers added.. Saving data..");
                // now click
                WebDriverWait wait2 = new WebDriverWait(driver, Duration.ofSeconds(5));

                By submitBtn = By.id("submitBtnNext1");

                WebElement button = wait2.until(
                        ExpectedConditions.presenceOfElementLocated(submitBtn)
                );

                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", button);

                Thread.sleep(1000);

                wait2.until(ExpectedConditions.elementToBeClickable(button));

                try {
                    button.click();
                } catch (ElementClickInterceptedException e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
                }

                Thread.sleep(1000);

                /* ===================
                 STEP : SELECT CLAUSE
               ======================== */

                JavascriptExecutor js = (JavascriptExecutor) driver;

                js.executeScript(
                        "document.querySelectorAll(\"label.checkcontainer input[type='radio'][value='Y']\")" +
                                ".forEach(r => {" +
                                "   r.checked = true;" +
                                "   r.dispatchEvent(new Event('change'));" +
                                "});"
                );

                log.info("All clause radios with value 'Y' selected via JS");


                //SELECT MEMBERS STATUS - PGS NATURAL

                js.executeScript(
                        "document.querySelectorAll(\"select[name='complingFarmerStatus']\").forEach(s => {" +
                                "  const opt = [...s.options].find(o => o.text.trim() === 'PGS NATURAL');" +
                                "  if (opt) {" +
                                "    s.value = opt.value;" +
                                "    s.dispatchEvent(new Event('change', { bubbles: true }));" +
                                "  }" +
                                "});"
                );

                log.info("All members status selected to - PGS NATURAL ");

                // SCROLL TILL declaration

                By declarationCheckbox = By.id("declaration");

                WebElement buttonDec = wait2.until(
                        ExpectedConditions.presenceOfElementLocated(declarationCheckbox)
                );

                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].scrollIntoView({block:'center'});", buttonDec);

                wait2.until(ExpectedConditions.elementToBeClickable(buttonDec));

                try {
                    if (!buttonDec.isSelected()) {
                        buttonDec.click();
                    }
                } catch (ElementClickInterceptedException e) {
                    ((JavascriptExecutor) driver)
                            .executeScript("arguments[0].checked = true; arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                                    buttonDec);
                }

                By saveBtnBy = By.id("save");

                // 1️⃣ Wait until present in DOM
                WebElement saveBtn = wait.until(ExpectedConditions.presenceOfElementLocated(saveBtnBy));

                ((JavascriptExecutor) driver).executeScript(
                                        "arguments[0].scrollIntoView({block:'center'});",
                                        saveBtn
                                );

                Thread.sleep(500);

                try {
                    wait.until(ExpectedConditions.elementToBeClickable(saveBtn)).click();
                } catch (ElementClickInterceptedException | TimeoutException e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", saveBtn);
                }

                log.info("✅ Save button clicked successfully");

                log.info("Automation Done!!");

            }
        } else {
            log.info("Not Proceeding farmers from Excel :: LESS RECORDS PRESENT IN EXCEL SHEET!!");
            log.info("EXITING Automation!!");
            automationStatusStream.notifyStatus("FAILED");
        }

    }


    /* =========================
       HELPER METHODS
       ========================= */

    public void setDateRange(WebDriver driver, String fromDate, String toDate) {

        JavascriptExecutor js = (JavascriptExecutor) driver;

        WebElement from = driver.findElement(By.id("peerAppraisalFromDateStr"));
        WebElement to = driver.findElement(By.id("peerAppraisalToDateStr"));

        js.executeScript("arguments[0].value = arguments[1];", from, fromDate);
        js.executeScript("arguments[0].value = arguments[1];", to, toDate);

        // trigger change event if app listens to it
        js.executeScript("arguments[0].dispatchEvent(new Event('change'));", from);
        js.executeScript("arguments[0].dispatchEvent(new Event('change'));", to);
    }


    private void selectCropMatchingPeas(String cropName, WebDriver driver, WebDriverWait wait) {

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cropName")));

        Select cropSelect = new Select(
                wait.until(ExpectedConditions.elementToBeClickable(By.id("cropName")))
        );

        List<WebElement> freshOptions = cropSelect.getOptions();

        boolean selected = false;

        for (WebElement option : freshOptions) {
            String text = option.getText(); // SAFE now
            if (text != null && text.toUpperCase().contains(cropName)) {
                cropSelect.selectByVisibleText(text);
                selected = true;
                break;
            }
        }

        if (!selected) {
            throw new RuntimeException(cropName + " :: Crop not found");
        }
    }

    private void selectMatchingFarmer(
            String farmerName,
            Set<String> usedFarmers,
            WebDriver driver,
            WebDriverWait wait) {

        log.info("Searching Farmer :: " + farmerName);

        Select farmSelect = new Select(
                wait.until(ExpectedConditions.elementToBeClickable(By.id("fmrExpYields")))
        );

        String normalizedInput = normalizeName(farmerName);

        for (WebElement option : farmSelect.getOptions()) {

            String optionText = option.getText();
            if (optionText == null) continue;

            String normalizedOption = normalizeName(optionText);
            if (usedFarmers.contains(normalizedOption)) {
                continue;
            }

            // Remove S/O part
            String baseName = optionText.split("S/O")[0];

            if (normalizeName(baseName).contains(normalizedInput)) {

                farmSelect.selectByVisibleText(optionText);

                usedFarmers.add(normalizedOption);   // ✅ mark as used

                log.info("Selected Farmer :: " + optionText);
                return;
            }
        }

        throw new RuntimeException("No unused farmer matched: " + farmerName);
    }

    private void fillInput(WebDriverWait wait, String id, String value) {
        WebElement input = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id(id)));
        input.clear();
        log.info("FILLING FIELD [{}] WITH VALUE [{}]", id, value);
        input.sendKeys(value);
    }

    private void selectDropdownByText(WebDriver driver, String id, String text) {

        Select select = new Select(driver.findElement(By.id(id)));

        String normalizedInput = normalize(text);

        for (WebElement option : select.getOptions()) {
            String normalizedOption = normalize(option.getText());

            if (normalizedOption.equals(normalizedInput)) {
                option.click();
                return;
            }
        }

        throw new RuntimeException(
                "Option '" + text + "' not found for dropdown with id: " + id
        );
    }

    private String normalize(String value) {
        if (value == null) return "";

        value = value.trim().toLowerCase();

        // Handle GEN <-> GENERAL mapping
        if (value.equals("gen")) return "General";
        if (value.equals("general")) return "General";
        if (value.equals("m")) return "Male";
        if (value.equals("male")) return "Male";
        if (value.equals("f")) return "Female";
        if (value.equals("female")) return "Female";

        return value;
    }

    private String normalizeName(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z ]", "")   // remove special chars
                .replaceAll("\\s+", "")      // remove ALL spaces
                .trim();
    }


    private void acceptAlertIfPresent(WebDriver driver, int seconds) {
        try {
            WebDriverWait shortWait =
                    new WebDriverWait(driver, Duration.ofSeconds(seconds));

            Alert alert = shortWait.until(ExpectedConditions.alertIsPresent());
            log.info("Alert :: {}", alert.getText());
            alert.accept();

        } catch (TimeoutException e) {
            log.info("No alert present, continuing...");
        }
    }



    private void clickWithJs(WebDriver driver, WebDriverWait wait, By locator)
            throws InterruptedException {

        WebElement element = wait.until(
                ExpectedConditions.presenceOfElementLocated(locator));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element);

        Thread.sleep(400);

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].click();", element);
    }

    private void acceptAlert(WebDriverWait wait) {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        log.info("Alert :: {}", alert.getText());
        alert.accept();
    }

    private void processFarmer(Map<String, String> farmer,WebDriver driver, WebDriverWait wait, WebDriverWait longWait) throws Exception {

            /* =========================
           STEP 1: READ EXCEL DATA
           ========================= */
        //Map<String, String> farmer = excelUtil.getRowData(1);
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


        /* =========================
           STEP 2: ADD FARMER PAGE
           ========================= */
        driver.navigate().to("https://pgsindia-ncof.gov.in/NF/farmer/addFarmer");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("farmerName"))).sendKeys(farmerName);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("fatherOrHusband"))).sendKeys(fatherName);

        /* =========================
           STEP 3: SELECT VILLAGE
           ========================= */
        longWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//select[@id='village_id']/option"), 1
        ));

        selectDropdownByText(driver, "village_id", village);

        /* =========================
           STEP 4: VERIFY FARMER
           ========================= */
        clickWithJs(driver, wait, By.id("apiCallerBtn"));
        acceptAlert(wait);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("pincode"))).sendKeys(pinCode);

        if(mobileNumber.length()>=10) {
            log.info("Mobile Number present, adding...");
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id("mobile"))).sendKeys(mobileNumber);
        }

            /* =========================
           STEP : select gender
           ========================= */

        String xpath = "//label[@class='checkcontainer']//input[@type='radio' and @name='gn' and @value='"
                + normalize(gender) + "']";

        List<WebElement> radios = driver.findElements(By.xpath(xpath));

        if (!radios.isEmpty()) {
            WebElement radio = radios.get(0);
            radio.click();
            log.info("Gender radio FOUND and selected: {}", normalize(gender));
        } else {
            log.warn("Gender radio NOT FOUND for value: {}", normalize(gender));
        }


        /* =========================
           STEP 5: CATEGORY & AGE
           ========================= */
        selectDropdownByText(driver, "category", categoryId);

        WebElement ageInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("groupMemberFarmer")));
        ageInput.clear();
        ageInput.sendKeys(age);

        clickWithJs(driver, wait, By.id("submitBtnNext1"));
        acceptAlertIfPresent(driver, 3);

        /* =========================
           STEP 6: LAND DETAILS
           ========================= */
        fillInput(wait, "totalArea", totalArea);
        fillInput(wait, "plot_area", plotArea);
        fillInput(wait, "plot", plotNo);
        fillInput(wait, "khasraNo", khasraNo);

        wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(".addbtnicon-one"))).click();
        acceptAlertIfPresent(driver, 5);

        /* =========================
           STEP 7: FINAL SUBMIT
           ========================= */
        clickWithJs(driver, longWait, By.id("submitBtn"));
        acceptAlertIfPresent(driver, 3);
        acceptAlertIfPresent(driver, 3);

        log.info("🎉 Farmer registration completed successfully");

    }

    private void addFarmerDetails(Map<String, String> farmer, Set<String> usedFarmers, WebDriver driver, WebDriverWait wait, WebDriverWait longWait) {
        {
            log.info("Selected Farmer: " + farmer.toString());

            String farmerName = farmer.get("Farmer Name");
            String cgName = farmer.get("Crop Category").toUpperCase();
            String cropName = farmer.get("Crop*").toUpperCase();
            String areaShowInHa = farmer.get("Area Shown in (Ha)*");
            String expQuintals = farmer.get("Expected Yields in (Quintals)*");
            String selfQuintals = farmer.get("Self Consumption in (Quintals)*");

            // ===== Select Farmer =====
            selectMatchingFarmer(farmerName, usedFarmers, driver, wait);

            //  ===== Select CG =====
            WebElement cgDropdown = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("cgName"))
            );

            Select cgSelect = new Select(cgDropdown);

            boolean found = false;

            for (WebElement option : cgSelect.getOptions()) {
                if (option.getText().trim().equalsIgnoreCase(cgName.trim())) {
                    cgSelect.selectByVisibleText(option.getText()); // select original text
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new RuntimeException("CG name not found (ignore-case): " + cgName);
            }

            // ===== Wait crop reload =====
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                    By.xpath("//select[@id='cropName']/option"), 1
            ));

            // ✅ SAFE crop selection
            selectCropMatchingPeas(cropName, driver, wait);

            // ===== Inputs =====
            driver.findElement(By.id("areaShowInHa")).clear();
            driver.findElement(By.id("areaShowInHa")).sendKeys(areaShowInHa);

            driver.findElement(By.id("expQuintals")).clear();
            driver.findElement(By.id("expQuintals")).sendKeys(expQuintals);

            driver.findElement(By.id("selfQuintals")).clear();
            driver.findElement(By.id("selfQuintals")).sendKeys(selfQuintals);

            // ===== Add =====
            WebElement addButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector("button.addfarmerYieldsBtn")
                    )
            );
            addButton.click();

            log.info("Farmer details added: " + farmerName);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("fmrExpYields")));
        }

    }
}


