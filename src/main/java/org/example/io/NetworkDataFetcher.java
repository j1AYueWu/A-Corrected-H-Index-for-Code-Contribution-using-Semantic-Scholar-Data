package org.example.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.model.AuthorProfile;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class NetworkDataFetcher {
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    // --- Configuration area: you need to set your own api key ---
    private static final String GITHUB_TOKEN = "";
    private static final String S2_API_KEY = "";
    // ----------------

    /**
     * Request the author’s full academic profile on Semantic Scholar
     */
    public static AuthorProfile fetchS2Profile(String s2Id) throws IOException {

        String url = "https://api.semanticscholar.org/graph/v1/author/" + s2Id
                + "?fields=name,papers.title,papers.citationCount,papers.authors";

        int maxRetries = 8;

        for (int attempt = 0; attempt < maxRetries; attempt++) {

            Request request = new Request.Builder()
                    .url(url)
                    .header("x-api-key", S2_API_KEY)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.code() == 200) {
                    return mapper.readValue(response.body().string(), AuthorProfile.class);
                }

                if (response.code() == 429) {
                    int sleep = (int) Math.pow(2, attempt) * 2000;
                    System.out.println("  [S2限流] 冷却 " + sleep + " ms (retry " + (attempt + 1) + "/" + maxRetries + ")");
                    Thread.sleep(sleep);
                    continue;
                }

                if (response.code() >= 500) {
                    Thread.sleep(2000);
                    continue;
                }

                return null;

            } catch (Exception e) {

                int sleep = (int) Math.pow(2, attempt) * 2000;
                System.out.println("  [S2异常] retry " + (attempt + 1));
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        System.out.println("  [错误] S2 获取失败：" + s2Id);
        return null;
    }

    /**
     * Retrieve the author’s Additions-Deletions (A-D) score
     * and ranking in the repository
     * (supports multiple repository links for a single article)
     */
    public static Map<String, Object> getGitHubStats(String rawUrl, String handle) throws IOException {
        Map<String, Object> bestResult = new HashMap<>();
        bestResult.put("score", 0L);
        bestResult.put("rank", -1);

        List<String> repoPaths = cleanRepoPaths(rawUrl);
        long maxAd = -1;
        int bestRank = -1;

        for (String path : repoPaths) {
            String apiUrl = "https://api.github.com/repos/" + path + "/stats/contributors";
            JsonNode root = fetchWithRetry(apiUrl);

            if (root != null && root.isArray()) {
                List<Long> scores = new ArrayList<>();
                long currentAuthorAd = -1;
                for (JsonNode contributor : root) {
                    long ad = calculateAD(contributor);
                    scores.add(ad);
                    if (contributor.get("author").get("login").asText().equalsIgnoreCase(handle)) {
                        currentAuthorAd = ad;
                    }
                }
                if (currentAuthorAd != -1) {
                    scores.sort(Collections.reverseOrder());
                    int currentRank = scores.indexOf(currentAuthorAd) + 1;
                    if (currentAuthorAd > maxAd) {
                        maxAd = currentAuthorAd;
                        bestRank = currentRank;
                    }
                }
            }
        }
        if (maxAd != -1) {
            bestResult.put("score", maxAd);
            bestResult.put("rank", bestRank);
        }
        return bestResult;
    }


    /**
     * A general request retry tool (20 GitHub polls)
     */
    private static JsonNode fetchWithRetry(String url) throws IOException {
        for (int i = 0; i < 20; i++) {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "token " + GITHUB_TOKEN)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 200) return mapper.readTree(response.body().string());
                if (response.code() == 202) {
                    System.out.println("  [GitHub] Data aggregation: retry " + (i + 1) + "/20...");
                    Thread.sleep(3000);
                } else {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    private static List<String> cleanRepoPaths(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return Collections.emptyList();
        return Arrays.stream(rawUrl.split(";"))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .map(url -> url.replace("https://github.com/", "")
                        .replace("http://github.com/", "")
                        .replaceAll("/$", ""))
                .collect(Collectors.toList());
    }

    private static long calculateAD(JsonNode contributor) {
        long a = 0, d = 0;
        for (JsonNode week : contributor.get("weeks")) {
            a += week.get("a").asLong();
            d += week.get("d").asLong();
        }
        return a - d;
    }
}