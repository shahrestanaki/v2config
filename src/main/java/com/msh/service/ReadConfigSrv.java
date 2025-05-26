package com.msh.service;

import com.msh.dto.InputDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
            System.out.println("HTTP Status: " + response.statusCode());
            System.out.println("Content:\n" + response.body());
        }catch (Exception e){
            log.error("error : ",e);
        }

    }
}
