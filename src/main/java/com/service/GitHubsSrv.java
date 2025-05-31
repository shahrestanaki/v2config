package com.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
            // read file Base64
            log.info("start updating ..... repo: {},path: {},branch: {},message: {}", repo,path,branch,commitMessage);
            byte[] fileBytes = Files.readAllBytes(localFilePath);
            String contentBase64 = Base64.getEncoder().encodeToString(fileBytes);
            String url = "https://api.github.com/repos/" + repo + "/contents/" + path;

            HttpClient client = HttpClient.newHttpClient();

            // check and get sha from github
            String sha = null;
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();

            HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());

            if (getResponse.statusCode() == 200) {
                // if file exist get sha
                String responseBody = getResponse.body();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(responseBody);
                sha = root.get("sha").asText();
            }

            log.info("start commit to gitHub");
            //create with or withOut sha
            ObjectNode jsonNode = new ObjectMapper().createObjectNode();
            jsonNode.put("message", commitMessage);
            jsonNode.put("branch", branch);
            jsonNode.put("content", contentBase64);
            if (sha != null) {
                jsonNode.put("sha", sha);
            }

            String json = jsonNode.toString();

            HttpRequest putRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github.v3+json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> putResponse = client.send(putRequest, HttpResponse.BodyHandlers.ofString());

            if (putResponse.statusCode() == 201 || putResponse.statusCode() == 200) {
                log.info("File uploaded successfully: {}", path);
            } else {
                log.error("Failed to upload file: " + path + " Status: " + putResponse.statusCode());
                log.error("Response: " + putResponse.body());
            }
        } catch (Exception e) {
            log.error("Error in GitHub upload: ", e);
        }
    }


}
