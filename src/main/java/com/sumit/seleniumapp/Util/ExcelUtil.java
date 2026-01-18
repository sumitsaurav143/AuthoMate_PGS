package com.sumit.seleniumapp.Util;


import com.sumit.seleniumapp.Dto.ExcelValidationResult;
import com.sumit.seleniumapp.Dto.RowValidationResult;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

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
                if (row == null || isRowEmpty(row)) continue;
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

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    public ExcelValidationResult validateExcel(String filePath) throws Exception {

        List<RowValidationResult> rowResults = new ArrayList<>();
        List<String> fileErrors = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                return new ExcelValidationResult(false, List.of(),
                        List.of("Header row missing"));
            }

            Map<Integer, String> headers = new HashMap<>();

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String header = headerRow.getCell(i).getStringCellValue().trim();

                if (header.isEmpty()) {
                    //fileErrors.add("Empty column header at index " + i);
                } else {
                    headers.put(i, header);
                }
            }

            for (Set<String> aliasGroup : REQUIRED_ALIAS_GROUPS) {

                boolean groupPresent = aliasGroup.stream()
                        .anyMatch(headers::containsValue);

                if (!groupPresent) {
                    fileErrors.add(
                            "Missing required column (any one required): " +
                                    String.join(" / ", aliasGroup)
                    );
                }
            }

            for (String required : REQUIRED_COLUMNS) {
                if (!headers.containsValue(required)) {
                    fileErrors.add("Missing required column: " + required);
                }
            }

            if (!fileErrors.isEmpty()) {
                return new ExcelValidationResult(false, List.of(), fileErrors);
            }

            // Validate rows
            for (int r = 1; r <= getRowCount(filePath); r++) {


                Row row = sheet.getRow(r);
                if (row == null) continue;

                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();

                for (Map.Entry<Integer, String> entry : headers.entrySet()) {
                    Cell cell = row.getCell(entry.getKey(),
                            Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                    String value = (cell == null) ? null :
                            new DataFormatter().formatCellValue(cell).trim();

                    if (REQUIRED_COLUMNS.contains(entry.getValue())
                            && (value == null || value.isEmpty())) {
                        errors.add(entry.getValue() + " is mandatory");
                    }
                }

                rowResults.add(new RowValidationResult(
                        r,
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
