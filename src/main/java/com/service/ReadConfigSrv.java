package com.service;

import com.dto.InputDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author m.Shahrestanaki @createDate 5/26/2025
 */
@Slf4j
@Service
public class ReadConfigSrv implements IReadConfigSrv {

    private final FilesSrv filesSrv;
    private final GitHubsSrv gitHubsSrv;
    private final V2GenerateSrv v2GenerateSrv;

    @Value("${config.file.subscribe}")
    private String subscribe;
    @Value("${config.supported-protocols}")
    private String supportedProtocols;
    @Value("${config.v2ray.main}")
    private String v2rayMainLocation;
    @Value("${config.v2ray.configFile}")
    private String v2rayConfigFile;
    @Value("${config.file.filesLocation}")
    private String filesLocation;
    @Value("${config.file.none}")
    private String noneFile;
    @Value("${config.v2ray.maxPing}")
    private Double v2rayMaxPing;

    public List<String> getSupportedProtocols() {
        return Arrays.asList(supportedProtocols.split(","));
    }

    public ReadConfigSrv(FilesSrv filesSrv, GitHubsSrv gitHubsSrv, V2GenerateSrv v2GenerateSrv) {
        this.filesSrv = filesSrv;
        this.gitHubsSrv = gitHubsSrv;
        this.v2GenerateSrv = v2GenerateSrv;
    }

    @Override
    public boolean gatheringConfigs(InputDto input) {
        boolean result = false;
        try {
            List<String> sub = filesSrv.readFile(Paths.get(filesLocation).resolve(subscribe));
            if (!sub.isEmpty()) {
                Set<String> configs = new HashSet<>();
                sub.forEach(url -> configs.addAll(readUrl(url)));
                if (!configs.isEmpty()) {
                    result = filesSrv.saveToFile(new ArrayList<>(configs), Paths.get(filesLocation).resolve(noneFile));
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
            List<String> configs = filesSrv.readFile(Paths.get(filesLocation).resolve(noneFile));
            if (!configs.isEmpty()) {
                List<String> confirmConfigs = checkConfig(new ArrayList<>(configs), input);
                if (!confirmConfigs.isEmpty()) {
                    String joinedConfigs = String.join("\n", configs);
                    String base64Encoded = Base64.getEncoder().encodeToString(joinedConfigs.getBytes(StandardCharsets.UTF_8));
                    result = filesSrv.saveToJsonOrBase64File(base64Encoded, Paths.get(filesLocation).resolve(input.getOperatorFile()));
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
            Path path = Paths.get(filesLocation).resolve(input.getOperatorFile());
            if (Files.exists(path)) {
                gitHubsSrv.githubUploader("shahrestanaki/v2config", input.getOperatorFile(),
                        "master", path, "created or update file : " + input.getOperatorFile());
            }
        } catch (Exception e) {
            log.error("error in pushToGitHub: ", e);
        }
        return result;
    }


    private List<String> checkConfig(List<String> configs, InputDto input) {
        List<String> data = new ArrayList<>();
        Map<String, Double> result = new HashMap<>();
        try {
            AtomicInteger count = new AtomicInteger();
            configs.forEach(item -> {
                boolean status = v2GenerateSrv.vless(item, Paths.get(v2rayMainLocation + v2rayConfigFile));
                log.info("success save json file for record {}: is: {} in operator: {}", 0, status, input.getOperator());
                if (status) {
                    Double time = checkCurlCall(v2GenerateSrv.testConnection());
                    log.info("timing for record {}: is: {} ", count.get(), time);
                    if (time != null && time < v2rayMaxPing) {
                        result.put(configs.get(0), time);
                    }
                }
                count.getAndIncrement();
            });
            if (!result.isEmpty()) {
                data = result.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .toList();
            }
            if (!data.isEmpty()) {
                log.info("total : {} record success for operator: {} ", data.size(), input.getOperator());
                filesSrv.saveToFile(data, Paths.get(filesLocation).resolve(input.getOperatorFile()));
            }
        } catch (Exception e) {
            log.error("error in checkConfig: ", e);
        }
        log.info("result for checkConfig is {} records", data.size());
        return data;
    }

    private Double checkCurlCall(String response) {
        Double result = null;
        try {
            if (response.toLowerCase().contains("time:")) {
                String[] parts = response.split(":");
                String numberStr = parts[1].trim();
                result = Double.parseDouble(numberStr);
            }
        } catch (Exception e) {
            log.error("error in gatheringConfigs: ", e);
        }
        return result;
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
