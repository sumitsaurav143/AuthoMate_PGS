package com.sumit.seleniumapp.Dto;

import java.util.List;

public class ExcelValidationResult {

    private boolean valid;
    private List<RowValidationResult> rowResults;
    private List<String> fileErrors;

    // ✅ REQUIRED no-arg constructor (for JSON serialization)
    public ExcelValidationResult() {
    }

    // ✅ REQUIRED constructor used in your code
    public ExcelValidationResult(
            boolean valid,
            List<RowValidationResult> rowResults,
            List<String> fileErrors) {

        this.valid = valid;
        this.rowResults = rowResults;
        this.fileErrors = fileErrors;
    }

    // ✅ Getters
    public boolean isValid() {
        return valid;
    }

    public List<RowValidationResult> getRowResults() {
        return rowResults;
    }

    public List<String> getFileErrors() {
        return fileErrors;
    }
}