package com.msh.api;

import com.msh.service.IReadConfigSrv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping(value = "test")
    public @ResponseBody
    Object test(Locale locale) {
        try {
            log.info("intro to methode.");
            //configSrv.test
        } catch (Exception e) {
            log.error("error : ", e);
        }
        return null;
    }
}
