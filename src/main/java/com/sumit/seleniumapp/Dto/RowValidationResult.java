package com.sumit.seleniumapp.Dto;

import java.util.List;

public class RowValidationResult {

    private int rowNumber;
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;

    // ✅ No-arg constructor
    public RowValidationResult() {
    }

    // ✅ Constructor used in ExcelUtil
    public RowValidationResult(
            int rowNumber,
            boolean valid,
            List<String> errors,
            List<String> warnings) {

        this.rowNumber = rowNumber;
        this.valid = valid;
        this.errors = errors;
        this.warnings = warnings;
    }

    // ✅ Getter REQUIRED for method reference
    public boolean isValid() {
        return valid;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}