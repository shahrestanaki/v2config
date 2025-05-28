package com.api;

import com.dto.InputDto;
import com.service.IReadConfigSrv;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

/**
 * @author m.Shahrestanaki @createDate 5/26/2025
 */
@Slf4j
@RestController
@RequestMapping("/api/main/")
public class MainController {
    private final IReadConfigSrv configSrv;

    public MainController(IReadConfigSrv configSrv) {
        this.configSrv = configSrv;
    }

    @GetMapping(value = "ping")
    public @ResponseBody
    Object ping() {
        try {
            log.info("OK. I am here.");
        } catch (Exception e) {
            log.error("error on ping: ", e);
        }
        return null;
    }

    @PostMapping(value = "test")
    public @ResponseBody
    Object test(@RequestBody InputDto input, HttpServletRequest request, Locale locale) {
        try {
            configSrv.test(input);
        } catch (Exception e) {
            log.error("error : ", e);
        }
        return null;
    }

    @PostMapping(value = "gatheringConfigs")
    public @ResponseBody
    Object gatheringConfigs(@RequestBody InputDto input, HttpServletRequest request) {
        boolean result = false;
        try {
            result = configSrv.gatheringConfigs(input);
        } catch (Exception e) {
            log.error("error : ", e);
        }
        return result;
    }

    @PostMapping(value = "checkConfig")
    public @ResponseBody
    Object checkConfig(@RequestBody InputDto input, HttpServletRequest request) {
        boolean result = false;
        try {
            result = configSrv.checkConfig(input);
        } catch (Exception e) {
            log.error("error : ", e);
        }
        return result;
    }

    @GetMapping(value = "pushToGitHub")
    public @ResponseBody
    Object pushToGitHub(@RequestBody InputDto input, HttpServletRequest request) {
        boolean result = false;
        try {
            result = configSrv.pushToGitHub(input);
        } catch (Exception e) {
            log.error("error : ", e);
        }
        return result;
    }
}
