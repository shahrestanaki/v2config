package com.msh.service;

import com.msh.dto.InputDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

/**
 * @author m.Shahrestanaki @createDate 5/26/2025
 */
@Slf4j
@Service
public class ReadConfigSrv implements IReadConfigSrv{

    @Override
    public void readFile(InputDto input) {
        try {
            String url = input.getUrl();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("HTTP Status: 200\nFiltered VLESS links:");
                Arrays.stream(response.body().split("\n"))
                        .map(String::trim)
                        .filter(line -> line.startsWith("vless://") || line.startsWith("vlessl://"))
                        .forEach(System.out::println);
            } else {
                System.out.println("Failed to fetch content. Status: " + response.statusCode());
            }
        }catch (Exception e){
            log.error("error : ",e);
        }

    }
}
