package com.uietpapers.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.serviceKey}")
    private String serviceKey;

    @Value("${supabase.bucket}")
    private String bucket;

    public String getBucket() {
        return bucket;
    }

    public String upload(String path, byte[] bytes, String contentType) throws Exception {
        String endpoint = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, path);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + serviceKey)
                .header("apikey", serviceKey)
                .header(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucket, path);
        } else {
            throw new RuntimeException("Supabase upload failed: " + response.statusCode() + " " + response.body());
        }
    }

    public static String safeFilename(String original) {
        String base = original == null ? "paper.pdf" : original.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (base.length() > 100) base = base.substring(0, 100);
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            return base.substring(0, dot) + "_" + uuid + base.substring(dot);
        }
        return base + "_" + uuid;
    }
    public void delete(String filePath) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String endpoint = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, filePath);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("apikey", serviceKey)
                    .header("Authorization", "Bearer " + serviceKey)
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                // File not found: probably already deleted, log and continue
                System.out.println("Supabase file already deleted: " + filePath);
                return;
            }

            if (response.statusCode() >= 300) {
                throw new RuntimeException("Supabase delete failed: " + response.statusCode() + " " + response.body());
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Supabase delete error", e);
        }
    }



}
