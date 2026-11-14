package com.sumit.seleniumapp.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class AutomationStatusStream {

    private final SseEmitter emitter = new SseEmitter(0L);

    @GetMapping("/status/stream")
    public SseEmitter stream() {
        return emitter;
    }

    public void notifyStatus(String status) {
        try {
            emitter.send(status);
        } catch (Exception e) {
            emitter.complete();
        }
    }
}