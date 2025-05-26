package com.msh.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.List;

/**
 * @author m.Shahrestanaki @createDate 5/26/2025
 */
@Slf4j
@Service
public class SaveDataSrv {
    public boolean saveToFile(List<String> data, String fileName){
        boolean result = false;
        try {
            Path path = Paths.get(System.getProperty("user.dir")).resolve(fileName);
            Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            result = true;
        }catch (Exception e){
            log.error("error : ",e);
        }
        return result;
    }

    public void GithubUploader(String repo, String path, String branch, Path localFilePath, String commitMessage){
        try {
            String token = "ghp_Mc0DD2II4ZyimSLHOV9pqkbgGqDHSUB4O5NIy";

            byte[] fileBytes = Files.readAllBytes(localFilePath);
            String contentBase64 = Base64.getEncoder().encodeToString(fileBytes);
            String json = """
                        {
                          "message": "%s",
                          "branch": "%s",
                          "content": "%s"
                        }
                        """.formatted(commitMessage, branch, contentBase64);

            String url = "https://api.github.com/repos/" + repo + "/contents/" + path;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github.v3+json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                System.out.println("File uploaded successfully: " + path);
            } else {
                System.err.println("Failed to upload file: " + path + " Status: " + response.statusCode());
                System.err.println("Response: " + response.body());
            }
        }catch (Exception e){
            log.error("error : ",e);
        }
    }

}
