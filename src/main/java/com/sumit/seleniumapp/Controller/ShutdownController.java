package com.sumit.seleniumapp.Controller;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
public class ShutdownController {

    private final ConfigurableApplicationContext context;

    public ShutdownController(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @PostMapping("/shutdown")
    public void shutdown() {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // allow response to return
            } catch (InterruptedException ignored) {}
            context.close();      // graceful shutdown
            System.exit(0);       // ensure JVM exits
        }).start();
    }
}