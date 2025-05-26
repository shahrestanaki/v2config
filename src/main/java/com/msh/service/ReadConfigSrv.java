package com.msh.service;

import com.msh.dto.InputDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author m.Shahrestanaki @createDate 5/26/2025
 */
@Slf4j
@Service
public class ReadConfigSrv implements IReadConfigSrv{

    private final SaveDataSrv saveDataSrv;

    public ReadConfigSrv(SaveDataSrv saveDataSrv) {
        this.saveDataSrv = saveDataSrv;
    }

    @Override
    public void readFile(InputDto input) {
        List<String> data = new ArrayList<>() ;
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
                        .forEach(data::add);
            } else {
                System.out.println("Failed to fetch content. Status: " + response.statusCode());
            }
            if(!data.isEmpty()){
                boolean result = saveDataSrv.saveToFile(data,"132456.txt");
                if(result){
                    Path path = Paths.get(System.getProperty("user.dir")).resolve("132456.txt");
                    //String repo, String path, String branch, Path localFilePath, String commitMessage
                    saveDataSrv.GithubUploader("shahrestanaki/v2config","132456.txt","master",path.getFileName(),"created");
                }
            }
        }catch (Exception e){
            log.error("error : ",e);
        }

    }
}
