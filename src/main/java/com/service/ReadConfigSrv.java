package com.service;

import com.dto.InputDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author m.Shahrestanaki @createDate 5/26/2025
 */
@Slf4j
@Service
public class ReadConfigSrv implements IReadConfigSrv {

    private final FilesSrv filesSrv;
    private final GitHubsSrv gitHubsSrv;

    @Value("${config.file.subscribe}")
    private String subscribe;
    @Value("${config.file.supported-protocols}")
    private String supportedProtocols;
    @Value("${config.file.v2rayLocation}")
    private String v2rayLocation;
    @Value("${config.file.config.none-test}")
    private String noneTest;
    @Value("${config.file.tci}")
    private String tci;
    @Value("${config.file.irancell}")
    private String irancell;
    @Value("${config.file.rightel}")
    private String rightel;

    public List<String> getSupportedProtocols() {
        return Arrays.asList(supportedProtocols.split(","));
    }

    public ReadConfigSrv(FilesSrv filesSrv, GitHubsSrv gitHubsSrv) {
        this.filesSrv = filesSrv;
        this.gitHubsSrv = gitHubsSrv;
    }

    @Override
    public boolean gatheringConfigs(InputDto input) {
        boolean result = false;
        try {
            List<String> sub = filesSrv.readFile(subscribe);
            if (!sub.isEmpty()) {
                Set<String> configs = new HashSet<>();
                sub.forEach(url -> configs.addAll(readUrl(url)));
                if (!configs.isEmpty()) {
                    result = filesSrv.saveToFile(new ArrayList<>(configs), Paths.get(noneTest).resolve(input.getOperator()));
                }
            }
        } catch (Exception e) {
            log.error("error in gatheringConfigs: ", e);
        }
        return result;
    }

    @Override
    public boolean checkConfig(InputDto input) {
        boolean result = false;
        try {
            List<String> configs = filesSrv.readFile(noneTest);
            if (!configs.isEmpty()) {
                List<String> confirmConfigs = checkConfig(new ArrayList<>(configs), input);
                if (!confirmConfigs.isEmpty()) {
                    result = filesSrv.saveToFile(confirmConfigs, Paths.get(v2rayLocation).resolve(input.getOperator()));
                }
            }
        } catch (Exception e) {
            log.error("error in checkConfig: ", e);
        }
        return result;
    }

    @Override
    public boolean pushToGitHub(InputDto input) {
        boolean result = false;
        try {
            String file = null;
            switch (input.getOperator()) {
                case "tci" -> file = tci;
                case "irancell" -> file = irancell;
                case "rightel" -> file = rightel;
            }
            if (file != null) {
                List<String> configs = filesSrv.readFile(file);
                if (!configs.isEmpty()) {
                    Path path = Paths.get(v2rayLocation).resolve(input.getOperator());
                    gitHubsSrv.githubUploader("shahrestanaki/v2config", input.getOperator(), "master", path.getFileName(), "created");
                }
            }
        } catch (Exception e) {
            log.error("error in checkConfig: ", e);
        }
        return result;
    }


    private List<String> checkConfig(List<String> configs, InputDto input) {
        List<String> data = new ArrayList<>();
        try {
            Path path = Paths.get(v2rayLocation).resolve(input.getOperator());
            filesSrv.saveToFile(configs, path);
            //check config
            //sort by response and save in : data
        } catch (Exception e) {
            log.error("error in gatheringConfigs: ", e);
        }
        log.info("result for checkConfig is {} records", data.size());
        return data;
    }

    private List<String> readUrl(String url) {
        List<String> data = new ArrayList<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.info("OK url: {}", url);
                Arrays.stream(response.body().split("\n"))
                        .map(String::trim)
                        .filter(line -> getSupportedProtocols().stream().anyMatch(line::startsWith))
                        .forEach(data::add);
            } else {
                log.error("Failed to fetch content. Status: {} in url: {}", response.statusCode(), url);
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.error("error readUrl: ", e);
        }
        log.info("result for readUrl is {} records", data.size());
        return data;
    }

    @Override
    public void test(InputDto input) {
        List<String> data = new ArrayList<>();
        try {
            String url = input.getUrl();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.info("HTTP Status: 200\nFiltered VLESS links:");
                Arrays.stream(response.body().split("\n"))
                        .map(String::trim)
                        .filter(line -> line.startsWith("vless://") || line.startsWith("vlessl://"))
                        .forEach(data::add);
            } else {
                log.error("Failed to fetch content. Status: " + response.statusCode());
            }
            if (!data.isEmpty()) {
                Path path = Paths.get(System.getProperty("user.dir")).resolve("132456.txt");
                boolean result = filesSrv.saveToFile(data, path);
                if (result) {
                    gitHubsSrv.githubUploader("shahrestanaki/v2config", input.getOperator(), "master", path.getFileName(), "created");
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.error("error : ", e);
        }
    }


}
