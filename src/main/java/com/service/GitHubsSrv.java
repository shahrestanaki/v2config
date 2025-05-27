package com.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * @author m.Shahrestanaki @createDate 5/26/2025
 */
@Slf4j
@Service
public class GitHubsSrv {
    @Value("${config.github.token}")
    private String token;
    public void githubUploader(String repo, String path, String branch, Path localFilePath, String commitMessage) {
        try {
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
                log.info("File uploaded successfully: {}", path);
            } else {
                log.error("Failed to upload file: " + path + " Status: " + response.statusCode());
                log.error("Response: " + response.body());
            }
        } catch (Exception e) {
            log.error("error : ", e);
        }
    }

}
