package com.sumit.seleniumapp.Controller;

import com.sumit.seleniumapp.Dto.ExcelValidationResult;
import com.sumit.seleniumapp.Service.SeleniumAutomationService;
import com.sumit.seleniumapp.Util.ExcelUtil;
import com.sumit.seleniumapp.Util.ExcelUtilLatLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
    ExcelUtilLatLong excelUtilLatLong;

    @PostMapping("/validate")
    public ResponseEntity<ExcelValidationResult> validateExcel(
            @RequestParam("file") MultipartFile file) {

        Path tempFile = null;

        try {
            // ✅ Basic validation
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Uploaded file is empty");
            }

            if (!file.getOriginalFilename().trim().endsWith(".xlsx")) {
                throw new IllegalArgumentException("Only .xlsx files are supported");
            }

            // ✅ Safe temp file creation
            tempFile = Files.createTempFile(
                    "authomate_validate_",
                    "_" + file.getOriginalFilename()
            );

            file.transferTo(tempFile.toFile());

            // ✅ Validate
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
            // ✅ CRITICAL: cleanup temp file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temp file {}", tempFile);
                }
            }
        }
    }

    @PostMapping("/validateLatLong")
    public ResponseEntity<ExcelValidationResult> validateLatLongExcel(
            @RequestParam("file") MultipartFile file) {

        Path tempFile = null;

        try {
            // ✅ Basic validation
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Uploaded file is empty");
            }

            if (!file.getOriginalFilename().trim().endsWith(".xlsx")) {
                throw new IllegalArgumentException("Only .xlsx files are supported");
            }

            // ✅ Safe temp file creation
            tempFile = Files.createTempFile(
                    "authomate_validate_",
                    "_" + file.getOriginalFilename()
            );

            file.transferTo(tempFile.toFile());

            // ✅ Validate
            ExcelValidationResult result =
                    excelUtilLatLong.validateExcel(tempFile.toString());

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
            // ✅ CRITICAL: cleanup temp file
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
                Path uploadDir = Paths.get(
                        System.getProperty("user.home"),
                        "AuthoMate",
                        "uploads"
                );

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
    public ResponseEntity<Map<String, String>> startAutomation2(
            @RequestParam("userId") String userId,
            @RequestParam("password") String password,
            @RequestParam("file") MultipartFile file) {

        String runId = UUID.randomUUID().toString();

        new Thread(() -> {
            try {
                log.info("LatLong Automation started for user :: {}", userId);

                // ✅ Safe writable directory
                Path uploadDir = Paths.get(
                        System.getProperty("user.home"),
                        "AuthoMate",
                        "uploads"
                );

                Files.createDirectories(uploadDir);

                // ✅ Single, consistent file path
                Path filePath = uploadDir.resolve(
                        runId + "_" + file.getOriginalFilename()
                );

                file.transferTo(filePath.toFile());

                log.info("LatLong File saved to path :: {}", filePath);

                service.runAutomationLatLong(userId, password, filePath.toString());

            } catch (Exception e) {
                log.error("LatLong Automation failed", e);
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

        Path logPath = Paths.get(
                System.getProperty("user.home"),
                "AuthoMate",
                "logs",
                "authomate.log"
        );

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