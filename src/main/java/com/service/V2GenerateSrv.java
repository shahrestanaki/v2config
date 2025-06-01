package com.service;

/**
 * @author m.Shahrestanaki @createDate 5/28/2025
 */

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class V2GenerateSrv {
    private final FilesSrv filesSrv;
    @Value("${config.v2ray.main}")
    private String v2rayMainLocation;
    @Value("${config.v2ray.configFile}")
    private String v2rayConfigFile;
    @Value("${config.v2ray.testPort}")
    private String v2rayTestPort;
    @Value("${config.v2ray.exe}")
    private String v2rayExeFile;
    @Value("${config.v2ray.timeoutSeconds}")
    private String v2rayTimeoutSeconds;
    @Value("${config.v2ray.urlTest}")
    private String v2rayUrlTest;

    public V2GenerateSrv(FilesSrv filesSrv) {
        this.filesSrv = filesSrv;
    }

    public boolean vless(String link, Path outputPath) {
        boolean result = false;
        try {
            if (!link.startsWith("vless://")) {
                return false;
            }
            String noPrefix = link.substring(8); // remove "vless://"
            String[] userInfoAndRest = noPrefix.split("@");
            String userId = userInfoAndRest[0];

            String[] hostAndParams = userInfoAndRest[1].split("\\?", 2);
            String[] hostParts = hostAndParams[0].split(":");

            String address = hostParts[0];
            int port = Integer.parseInt(hostParts[1]);

            Map<String, String> params = Arrays.stream(hostAndParams[1].split("&"))
                    .map(kv -> kv.split("=", 2))
                    .collect(Collectors.toMap(kv -> kv[0], kv -> kv.length > 1 ? kv[1] : ""));

            String path = URLDecoder.decode(params.getOrDefault("path", "/"), StandardCharsets.UTF_8);
            String security = params.getOrDefault("security", "none");
            String network = params.getOrDefault("type", "tcp");
            String host = params.getOrDefault("host", address);
            String encryption = params.getOrDefault("encryption", "none");

            String json = """
                    {
                      "inbounds": [
                        {
                          "port": """ + v2rayTestPort + "," + """
                          
                          "listen": "127.0.0.1",
                          "protocol": "socks",
                          "settings": {
                            "auth": "noauth",
                            "udp": true
                          }
                        }
                      ],
                      "outbounds": [
                        {
                          "protocol": "vless",
                          "settings": {
                            "vnext": [
                              {
                                "address": "%s",
                                "port": %d,
                                "users": [
                                  {
                                    "id": "%s",
                                    "encryption": "%s",
                                    "flow": ""
                                  }
                                ]
                              }
                            ]
                          },
                          "streamSettings": {
                            "network": "%s",
                            "security": "%s",
                            "wsSettings": {
                              "path": "%s",
                              "headers": {
                                "Host": "%s"
                              }
                            }
                          },
                          "mux": {
                            "enabled": false
                          }
                        }
                      ]
                    }
                    """.formatted(address, port, userId, encryption, network, security, path, host);
            result = filesSrv.saveToJsonOrBase64File(json, outputPath);
        } catch (Exception e) {
            log.error("error in vless: ", e);
        }
        return result;
    }


    public String testConnection() {
        String result = null;
        Process v2rayProcess = null;
        Process curlProcess = null;
        try {
            // run v2ray apps
            log.info("config file : {}",v2rayMainLocation+v2rayConfigFile);
            ProcessBuilder v2rayProcessBuilder = new ProcessBuilder(v2rayMainLocation + v2rayExeFile, "-config", v2rayMainLocation+v2rayConfigFile);
            v2rayProcess = v2rayProcessBuilder.start();

            //  sleep for upping V2Ray
            Thread.sleep(5000);

            ProcessBuilder curlProcessBuilder = new ProcessBuilder(
                    "curl",
                    "-x", "socks5h://127.0.0.1:" + v2rayTestPort,
                    "-s",
                    "-o", "nul",
                    "-w", "HTTP_CODE: %{http_code} TIME: %{time_total}\\n",
                    "--max-time", String.valueOf(v2rayTimeoutSeconds),
                    v2rayUrlTest
            );
            curlProcess = curlProcessBuilder.start();
        } catch (Exception e) {
            log.error("error in vless: ", e);
            Thread.currentThread().interrupt();
        }
        if (curlProcess != null) {
            // call curl
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(curlProcess.getInputStream()))) {
                String line = reader.readLine();
                //  wait for end curl
                curlProcess.waitFor();
                // stop v2ray
                v2rayProcess.destroy();
                result = line != null ? line : "No response from curl";
            } catch (Exception e) {
                v2rayProcess.destroy();
                log.error("Error during curl execution: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        return result;
    }

    public Process startV2ray() throws IOException {
        ProcessBuilder v2rayProcessBuilder = new ProcessBuilder(
                v2rayMainLocation + v2rayExeFile,
                "run",
                "--config", v2rayMainLocation + v2rayConfigFile
        );
        v2rayProcessBuilder.redirectErrorStream(true);
        Process process = v2rayProcessBuilder.start();
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[V2Ray] " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return process;
    }

    public void stopV2ray(Process process) {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

    public String testCurl() throws IOException, InterruptedException {
        ProcessBuilder curlProcessBuilder = new ProcessBuilder(
                "curl",
                "-x", "socks5h://127.0.0.1:" + v2rayTestPort,
                "-s",
                "-o", "nul",
                "-w", "HTTP_CODE: %{http_code} TIME: %{time_total}\\n",
                "--max-time", String.valueOf(v2rayTimeoutSeconds),
                v2rayUrlTest
        );
        Process curlProcess = curlProcessBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(curlProcess.getInputStream()))) {
            String line = reader.readLine();
            log.info("response from curl: {}",line);
            curlProcess.waitFor();
            return line != null ? line : "No response from curl";
        }
    }
}
