package com.sumit.seleniumapp.Util;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URI;

@Component
public class BrowserLauncher {

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        try {
            String url = "http://localhost:8080";

            if (Desktop.isDesktopSupported() && !GraphicsEnvironment.isHeadless()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // macOS fallback (BEST)
                Runtime.getRuntime().exec(new String[]{"open", url});
            }

        } catch (Exception e) {
            System.out.println("⚠️ Unable to auto-open browser. Please open http://localhost:8080 manually.");
        }
    }
}