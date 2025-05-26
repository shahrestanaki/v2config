package com.msh.api;

import com.msh.dto.InputDto;
import com.msh.service.IReadConfigSrv;
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

    @GetMapping(value = "test")
    public @ResponseBody
    Object test(Locale locale) {
        try {
            log.info("intro to methode.");
        } catch (Exception e) {
            log.error("error : ", e);
        }
        return null;
    }


    @PostMapping(value = "readFile")
    public @ResponseBody Object refreshToken(@RequestBody InputDto input, HttpServletRequest request, Locale locale) {
        try{
            configSrv.readFile(input);
        }catch (Exception e){
            log.error("error : ", e);
        }
        return null;
    }
}
