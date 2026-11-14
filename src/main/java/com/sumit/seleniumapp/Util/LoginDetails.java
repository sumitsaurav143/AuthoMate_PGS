package com.sumit.seleniumapp.Util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginDetails{

    @Value("${app.url}")
    private String url;

    public String getUrl() {
        return url;
    }
}