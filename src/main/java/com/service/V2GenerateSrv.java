package com.service;

/**
 * @author m.Shahrestanaki @createDate 5/28/2025
 */

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class V2GenerateSrv {
    private final FilesSrv filesSrv;

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
                          "port": 1080,
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
            result = filesSrv.saveToJsonFile(json,outputPath);
        } catch (Exception e) {
            log.error("error in vless: ", e);
        }
        return result;
    }

}
