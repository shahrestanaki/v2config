package com.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author m.Shahrestanaki @createDate 5/26/2025
 */
@Slf4j
@Service
public class FilesSrv {
    public boolean saveToFile(List<String> data, Path path) {
        boolean result = false;
        try {
            Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            result = true;
        } catch (Exception e) {
            log.error("error : ", e);
        }
        log.info("result for saveToFile is {} records in file : {}", data.size(),path.getFileName());
        return result;
    }
    public boolean saveToJsonFile(String json, Path path) {
        boolean result = false;
        try {
            Files.writeString(path, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            result = true;
        } catch (Exception e) {
            log.error("error : ", e);
        }
        log.info("Config written to: " + path.toAbsolutePath());
        return result;
    }

    public List<String> readFile(Path path) {
        List<String> result = new ArrayList<>();
        try {
            //Path path = Paths.get(System.getProperty("user.dir")).resolve(fileName);
            result = Files.lines(path).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("error : ", e);
        }
        log.info("result for readFile is {} records", result.size());
        return result;
    }
}
