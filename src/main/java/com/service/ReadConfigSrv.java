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
    @Value("${config.github.gitHubRepo}")
    private String gitHubRepo;
    @Value("${config.github.branch}")
    private String branch;
    @Value("${config.v2ray.maxConfig}")
    private Integer maxConfig;
    private boolean stopOperation = false;

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
                sub.stream()
                        .filter(url-> !url.startsWith("#"))
                        .forEach(url -> configs.addAll(readUrl(url)));
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
                    //String joinedConfigs = String.join("\n", configs);
                    //String base64Encoded = Base64.getEncoder().encodeToString(joinedConfigs.getBytes(StandardCharsets.UTF_8));
                    //result = filesSrv.saveToJsonOrBase64File(base64Encoded, Paths.get(filesLocation).resolve(input.getOperatorFile()));
                    result = filesSrv.saveToFile(confirmConfigs, Paths.get(filesLocation).resolve(input.getOperatorFile()));
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
                gitHubsSrv.githubUploader("shahrestanaki/"+ gitHubRepo, input.getOperatorFile(),
                        branch, path, "created or update file : " + input.getOperatorFile() + " in : " + new Date());
            }
        } catch (Exception e) {
            log.error("error in pushToGitHub: ", e);
        }
        return result;
    }


    private List<String> checkConfig(List<String> configs, InputDto input) {
        List<String> data = new ArrayList<>();
        Map<String, Double> result = new HashMap<>();
        stopOperation = false;
        Process v2rayProcess = null;
        try {
            int count = 0;
            v2rayProcess = v2GenerateSrv.startV2ray();
            Thread.sleep(5000);
            log.info("Running v2rayProcess? {}", v2rayProcess.isAlive());
            for(String item : configs){
                if(stopOperation || (maxConfig != null && result.size() >= maxConfig)){
                    break;
                }
                boolean status = v2GenerateSrv.vless(item, Paths.get(v2rayMainLocation + v2rayConfigFile));
                log.debug("success save json file for record {}: is: {} in operator: {}", count, status, input.getOperator());
                if (status) {
                    String curlOutput = v2GenerateSrv.testCurl();
                    Double time = checkCurlCall(curlOutput);
                    log.info("timing for record {}: is: {} ", count, time);
                    if (time != null && time < v2rayMaxPing) {
                        log.info("accept config: {} ", count);
                        result.put(item, time);
                    }
                }
                count++;
            }
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
        } finally {
            v2GenerateSrv.stopV2ray(v2rayProcess); // Always stop
        }
        log.info("result for checkConfig is {} records", data.size());
        return data;
    }

    private Double checkCurlCall(String response) {
        Double result = null;
        try {
            if(response.contains("HTTP_CODE: 200")) {
                if (response.toLowerCase().contains("time:")) {
                    String[] parts = response.split(":");
                    String numberStr = parts[1].trim();
                    result = Double.parseDouble(numberStr);
                }
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
                        .map(line -> line.replaceAll("[\"'`\\s]+$", ""))
                        .filter(line -> getSupportedProtocols().stream().anyMatch(line::startsWith))
                        .filter(line -> !isSuspiciousV2rayLink(line))
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

    @Override
    public void stopAndSaveConfig(InputDto input) {
        stopOperation = true;
    }

    public static boolean isSuspiciousV2rayLink(String link) {
        String lower = link.toLowerCase();
        if (lower.contains("security=none") && lower.contains("encryption=none")) return true;
        if (lower.matches(".*:@[^:]+:(80|8880).*")) return true;
        String[] badHosts = {
                "speedtest.net", "foffmelo.com", "wlftest.xyz",
                "hiddendom.shop", "filegear-sg.me", "zulai.ir",
                "pronetwork.com"
        };
        for (String badHost : badHosts) {
            if (lower.contains(badHost)) return true;
        }

        // telegram or ....
        //if (lower.contains("t.me") || lower.contains("telegram") || lower.contains("shadowproxy")) return true;

        // TLS or GRPC security
        if (lower.contains("type=grpc") && lower.contains("security=none")) return true;

        return false;
    }
}
