package com.sumit.seleniumapp.Util;

import com.sumit.seleniumapp.Dto.ExcelValidationResult;
import com.sumit.seleniumapp.Dto.RowValidationResult;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.util.*;

@Component
public class ExcelUtilLatLong {

    // Mandatory columns for LatLong Excel
    static final Set<String> REQUIRED_COLUMNS = Set.of(
            "FarmerName (Original)",
            "Village",
            "Longitude",
            "Latitude",
            "Member ID"
    );

    public ExcelValidationResult validateExcel(String filePath) throws Exception {

        List<RowValidationResult> rowResults = new ArrayList<>();
        List<String> fileErrors = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            if (workbook.getNumberOfSheets() == 0) {
                return new ExcelValidationResult(
                        false,
                        List.of(),
                        List.of("No sheet found in Excel file")
                );
            }

            Sheet sheet = workbook.getSheetAt(0);

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

                Cell cell = headerRow.getCell(
                        i,
                        Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
                );

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

            // Validate required columns
            for (String requiredColumn : REQUIRED_COLUMNS) {

                if (!headers.containsValue(requiredColumn)) {
                    fileErrors.add(
                            "Missing required column: " + requiredColumn
                    );
                }
            }

            // If required columns are missing, don't validate rows
            if (!fileErrors.isEmpty()) {
                return new ExcelValidationResult(
                        false,
                        List.of(),
                        fileErrors
                );
            }

            // Validate data rows
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {

                Row row = sheet.getRow(r);

                // Skip blank rows
                if (row == null || isRowEmpty(row, formatter)) {
                    continue;
                }

                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();

                // Validate each required column
                for (Map.Entry<Integer, String> entry : headers.entrySet()) {

                    int colIndex = entry.getKey();
                    String columnName = entry.getValue();

                    if (!REQUIRED_COLUMNS.contains(columnName)) {
                        continue;
                    }

                    Cell cell = row.getCell(
                            colIndex,
                            Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
                    );

                    String value = cell == null
                            ? ""
                            : formatter.formatCellValue(cell)
                            .replace('\u00A0', ' ')
                            .trim();

                    if (value.isEmpty()) {
                        errors.add(
                                "Row " + (r + 1) +
                                        ", Column '" + columnName +
                                        "' is mandatory but empty"
                        );
                    }
                }

                rowResults.add(
                        new RowValidationResult(
                                r + 1,
                                errors.isEmpty(),
                                errors,
                                warnings
                        )
                );
            }
        }

        boolean valid = fileErrors.isEmpty()
                && rowResults.stream()
                .allMatch(RowValidationResult::isValid);

        return new ExcelValidationResult(
                valid,
                rowResults,
                fileErrors
        );
    }

    private boolean isRowEmpty(
            Row row,
            DataFormatter formatter) {

        if (row == null) {
            return true;
        }

        for (
                int c = row.getFirstCellNum();
                c < row.getLastCellNum();
                c++
        ) {

            if (c < 0) {
                continue;
            }

            Cell cell = row.getCell(
                    c,
                    Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
            );

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
}