package com.sumit.seleniumapp.Util;


import com.sumit.seleniumapp.Dto.ExcelValidationResult;
import com.sumit.seleniumapp.Dto.RowValidationResult;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.apache.poi.ss.usermodel.DataFormatter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

@Component
public class ExcelUtil {

    // Mandatory single columns
    static final Set<String> REQUIRED_COLUMNS = Set.of(
            "Farmer Name",
            "Village",
            "Pincode",
            "Gender",
            "Category",
            "Total Area",
            "Organic Area"
    );

    // Either–Or (at least one must exist and have value)
    static final List<Set<String>> REQUIRED_ALIAS_GROUPS = List.of(
            Set.of("Fathers Name", "Fathers Name/ Husband Name"),
            Set.of("Age", "Farmer Age"),
            Set.of("Khata No.", "Khasra No."),
            Set.of("Plot No.", "Plot No")
    );


    public int getRowCount(String filePath) throws Exception {
        int count = 0;

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row,new DataFormatter())) continue;
                count++;
            }
        }
        return count;
    }

    public Map<String, String> getRowData(String filePath, int rowIndex) throws Exception {
        Map<String, String> data = new HashMap<>();
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            Row dataRow = sheet.getRow(rowIndex);

            if (dataRow == null) {
                throw new RuntimeException("Row " + rowIndex + " is empty");
            }

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String key = formatter.formatCellValue(headerRow.getCell(i)).trim();
                if (key.isEmpty()) {
                    continue;
                }
                Cell cell = dataRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String value = formatter.formatCellValue(cell).trim();
                data.put(key, value);
            }
        }
        return data;
    }

    public void writeResult(
            String filePath,
            int rowIndex,
            String status,
            String message) throws Exception {

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            Row dataRow = sheet.getRow(rowIndex);

            int statusCol = -1;
            int messageCol = -1;
            int lastCol = headerRow.getLastCellNum();

            for (int i = 0; i < lastCol; i++) {
                String header = headerRow.getCell(i).getStringCellValue().trim();
                if ("STATUS".equalsIgnoreCase(header)) statusCol = i;
                if ("MESSAGE".equalsIgnoreCase(header)) messageCol = i;
            }

            if (statusCol == -1) {
                statusCol = lastCol;
                headerRow.createCell(statusCol).setCellValue("STATUS");
            }

            if (messageCol == -1) {
                messageCol = lastCol + 1;
                headerRow.createCell(messageCol).setCellValue("MESSAGE");
            }

            dataRow.getCell(statusCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                    .setCellValue(status);

            dataRow.getCell(messageCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                    .setCellValue(message);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }

        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            if (c < 0) continue;

            Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

            if (cell != null) {
                String value = formatter.formatCellValue(cell)
                        .replace('\u00A0', ' ')
                        .trim();

                if (!value.isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    public ExcelValidationResult validateExcel(String filePath) throws Exception {

        List<RowValidationResult> rowResults = new ArrayList<>();
        List<String> fileErrors = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                return new ExcelValidationResult(
                        false,
                        List.of(),
                        List.of("No sheet found in Excel file")
                );
            }

            Row headerRow = sheet.getRow(0);

            if (headerRow == null || isRowEmpty(headerRow, formatter)) {
                return new ExcelValidationResult(
                        false,
                        List.of(),
                        List.of("Header row missing or empty")
                );
            }

            Map<Integer, String> headers = new LinkedHashMap<>();

            // Read headers safely
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                if (cell == null) {
                    continue;
                }

                String header = formatter.formatCellValue(cell)
                        .replace('\u00A0', ' ')
                        .trim();

                if (!header.isEmpty()) {
                    headers.put(i, header);
                }
            }

            // Validate alias groups
            for (Set<String> aliasGroup : REQUIRED_ALIAS_GROUPS) {
                boolean groupPresent = aliasGroup.stream()
                        .anyMatch(alias -> headers.containsValue(alias));

                if (!groupPresent) {
                    fileErrors.add(
                            "Missing required column (any one required): " +
                                    String.join(" / ", aliasGroup)
                    );
                }
            }

            // Validate exact required columns
            for (String required : REQUIRED_COLUMNS) {
                if (!headers.containsValue(required)) {
                    fileErrors.add("Missing required column: " + required);
                }
            }

            if (!fileErrors.isEmpty()) {
                return new ExcelValidationResult(false, List.of(), fileErrors);
            }

            // Validate data rows
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {

                Row row = sheet.getRow(r);

                // Skip null/blank formatting-only rows
                if (row == null || isRowEmpty(row, formatter)) {
                    continue;
                }

                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();

                for (Map.Entry<Integer, String> entry : headers.entrySet()) {

                    int colIndex = entry.getKey();
                    String columnName = entry.getValue();

                    Cell cell = row.getCell(
                            colIndex,
                            Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
                    );

                    String value = cell == null
                            ? ""
                            : formatter.formatCellValue(cell)
                            .replace('\u00A0', ' ')
                            .trim();

                    if (REQUIRED_COLUMNS.contains(columnName) && value.isEmpty()) {
                        errors.add(
                                "Row " + (r + 1) +
                                        ", Column '" + columnName +
                                        "' is mandatory but empty"
                        );
                    }
                }

                rowResults.add(new RowValidationResult(
                        r + 1,   // Excel row number
                        errors.isEmpty(),
                        errors,
                        warnings
                ));
            }
        }

        boolean valid = fileErrors.isEmpty() &&
                rowResults.stream().allMatch(RowValidationResult::isValid);

        return new ExcelValidationResult(valid, rowResults, fileErrors);
    }
}


