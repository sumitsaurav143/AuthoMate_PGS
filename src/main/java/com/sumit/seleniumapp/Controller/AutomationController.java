package com.sumit.seleniumapp.Controller;

import com.sumit.seleniumapp.Dto.ExcelValidationResult;
import com.sumit.seleniumapp.Service.SeleniumAutomationService;
import com.sumit.seleniumapp.Util.ExcelUtil;
import com.sumit.seleniumapp.Util.ExcelUtil2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class AutomationController {

    private static final Logger log = LogManager.getLogger(AutomationController.class);
    @Autowired
    private SeleniumAutomationService service;

    @Autowired
    ExcelUtil excelUtil;

    @Autowired
    ExcelUtil2 excelUtil2;

    @Value("${logging.file.name}")
    private String logFilePath;

    @Value("${app.upload.dir}")
    private String uploadDirPath;

    @PostMapping("/validate")
    public ResponseEntity<ExcelValidationResult> validateExcel(
            @RequestParam("file") MultipartFile file) {

        Path tempFile = null;

        try {

            if (file.isEmpty()) {
                throw new IllegalArgumentException("Uploaded file is empty");
            }

            if (!file.getOriginalFilename().endsWith(".xlsx")) {
                throw new IllegalArgumentException("Only .xlsx files are supported");
            }

            tempFile = Files.createTempFile(
                    "authomate_validate_",
                    "_" + file.getOriginalFilename()
            );

            file.transferTo(tempFile.toFile());

            ExcelValidationResult result =
                    excelUtil.validateExcel(tempFile.toString());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Excel validation failed", e);

            return ResponseEntity.badRequest().body(
                    new ExcelValidationResult(
                            false,
                            List.of(),
                            List.of(e.getMessage())
                    )
            );

        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temp file {}", tempFile);
                }
            }
        }
    }

    @PostMapping("/validate2")
    public ResponseEntity<ExcelValidationResult> validateExcel2(
            @RequestParam("file") MultipartFile file) {

        Path tempFile = null;

        try {

            if (file.isEmpty()) {
                throw new IllegalArgumentException("Uploaded file is empty");
            }

            if (!file.getOriginalFilename().endsWith(".xlsx")) {
                throw new IllegalArgumentException("Only .xlsx files are supported");
            }

            tempFile = Files.createTempFile(
                    "peersmate_validate_",
                    "_" + file.getOriginalFilename()
            );

            file.transferTo(tempFile.toFile());

            ExcelValidationResult result =
                    excelUtil2.validateExcel(tempFile.toString());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Excel validation failed", e);

            return ResponseEntity.badRequest().body(
                    new ExcelValidationResult(
                            false,
                            List.of(),
                            List.of(e.getMessage())
                    )
            );

        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temp file {}", tempFile);
                }
            }
        }
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startAutomation(
            @RequestParam("userId") String userId,
            @RequestParam("password") String password,
            @RequestParam("file") MultipartFile file) {

        String runId = UUID.randomUUID().toString();

        new Thread(() -> {
            try {
                log.info("Automation started for user :: {}", userId);

                // ✅ Safe writable directory
                Path uploadDir = Paths.get(uploadDirPath);

                Files.createDirectories(uploadDir);

                // ✅ Single, consistent file path
                Path filePath = uploadDir.resolve(
                        runId + "_" + file.getOriginalFilename()
                );

                file.transferTo(filePath.toFile());

                log.info("File saved to path :: {}", filePath);

                service.runAutomation(userId, password, filePath.toString());

            } catch (Exception e) {
                log.error("Automation failed", e);
            }
        }).start();

        Map<String, String> response = new HashMap<>();
        response.put("status", "STARTED");
        response.put("runId", runId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/start2")
    public ResponseEntity<Map<String, String>> startAutomation(
            @RequestParam("userId") String userId,
            @RequestParam("password") String password,
            @RequestParam("year") String year,
            @RequestParam("season") String season,
            @RequestParam("month") String month,
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam("file") MultipartFile file) {

        String runId = UUID.randomUUID().toString();

        new Thread(() -> {
            try {
                log.info("Automation started for user :: {}", userId);

                // ✅ Safe writable directory
                Path uploadDir = Paths.get(uploadDirPath);

                Files.createDirectories(uploadDir);

                // ✅ Single, consistent file path
                Path filePath = uploadDir.resolve(
                        runId + "_" + file.getOriginalFilename()
                );

                file.transferTo(filePath.toFile());

                log.info("File saved to path :: {}", filePath);

                service.runAutomation2(userId, password, year, season, month, fromDate, toDate, filePath.toString());

            } catch (Exception e) {
                log.error("Automation failed", e);
            }
        }).start();

        Map<String, String> response = new HashMap<>();
        response.put("status", "STARTED");
        response.put("runId", runId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/stop")
    public String stopAutomation() {

        new Thread(() -> {
            try {
                service.stopAutomation();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return "Automation stopped";
    }


    @GetMapping(value = "/logs", produces = "text/plain")
    public String getLogs() {

        Path logPath = Paths.get(logFilePath);

        try {
            if (!Files.exists(logPath)) {
                return "Logs not available yet...";
            }

            return Files.readString(logPath);

        } catch (Exception e) {
            return "❌ Unable to read logs: " + e.getMessage();
        }
    }
}