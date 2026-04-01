package org.example;

import org.example.model.AuthorProfile;
import org.example.model.SemanticScholarPaper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class HIndexCalculator {

    // === Exponential decay-based authorship weighting model ===
    // Model A
    public static double calculateWeightA(int rank, int n) {
        return Math.pow(2, n - rank) / (Math.pow(2, n) - 1);
    }
    // === Harmonic authorship weighting ===
    // Model B
    public static double calculateWeightB(int rank, int n) {
        double harmonicSum = 0;
        for (int i = 1; i <= n; i++) harmonicSum += 1.0 / i;
        return (1.0 / rank) / harmonicSum;
    }

    // === Read GitHub_Rank from Baseline_Validation.csv ===
    // Baseline CSV ：Author,Paper,Paper_Rank,h_Orig,h_ModelA,h_ModelB,GitHub_AD,GitHub_Rank
    // key = AuthorName||PaperTitle
    public static Map<String, Integer> loadGithubRanksFromBaseline(String csvPath) throws IOException {
        Map<String, Integer> map = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 8) continue;

                String author = parts[0] == null ? "" : parts[0].trim();
                String paperTitle = parts[1] == null ? "" : parts[1].trim();

                int gitRank;
                try {
                    gitRank = Integer.parseInt(parts[7].trim());
                } catch (Exception e) {
                    gitRank = -1;
                }

                map.put(makeKey(author, paperTitle), gitRank);
            }
        }
        return map;
    }

    public static String normalizeTitle(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    public static String makeKey(String authorName, String paperTitle) {
        String a = (authorName == null ? "" : authorName.trim());
        String t = normalizeTitle(paperTitle);
        return a + "||" + t;
    }


    /**
     * Weighted h-index
     */
    public static int calculateWeightedH(AuthorProfile profile, String targetAuthorId, String modelType) {
        return computeH(profile, targetAuthorId, modelType, null, Collections.emptyMap(), 0,1.0);
    }

    /**
     * Github-aware h-index
     *
     */
    public static int calculateInnovativeH(
            AuthorProfile profile,
            String targetAuthorId,
            String baseModel,
            String baselineAuthorName,
            Map<String, Integer> githubRankMap,
            int k,double bonusMultiplier
    ) {
        return computeH(profile, targetAuthorId, baseModel, baselineAuthorName, githubRankMap, k,bonusMultiplier);
    }

    /**
     * For internal use only: First, calculate the credit for each paper (double, no rounding),
     *      * then sort them by credit in descending order and calculate the h-index.
     *      *
     *      * Base credit = citationCount * weight
     *      * When GitHub_Rank satisfies 1 ≤ rank ≤ k, apply the bonusMultiplier to that paper.
     */
    private static int computeH(
            AuthorProfile profile,
            String targetAuthorId,
            String modelType,
            String baselineAuthorName,
            Map<String, Integer> githubRankMap,
            int k,double bonusMultiplier
    ) {
        List<Double> credits = profile.papers.stream()
                .map(paper -> computeCreditForPaper(paper, targetAuthorId, modelType, baselineAuthorName, githubRankMap, k,bonusMultiplier))
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());

        int h = 0;
        for (int i = 0; i < credits.size(); i++) {
            if (credits.get(i) >= (i + 1)) h = i + 1;
            else break;
        }
        return h;
    }

    public static double computeCreditForPaper(
            SemanticScholarPaper paper,
            String targetAuthorId,
            String modelType,
            String baselineAuthorName,
            Map<String, Integer> githubRankMap,
            int k,double bonusMultiplier
    ) {
        int n = paper.authors == null ? 0 : paper.authors.size();
        if (n <= 0) return 0.0;

        // Find the author’s rank in the list of authors (starting from 1)
        int paperRank = 1;
        for (int i = 0; i < n; i++) {
            if (paper.authors.get(i) != null && targetAuthorId.equals(paper.authors.get(i).authorId)) {
                paperRank = i + 1;
                break;
            }
        }

        double weight = modelType.equalsIgnoreCase("A")
                ? calculateWeightA(paperRank, n)
                : calculateWeightB(paperRank, n);

        // Using the GitHub_Rank from the baseline CSV
        // key = baselineAuthorName||paper.title
        int gitRank = -1;
        if (githubRankMap != null && k > 0) {
            String key = makeKey(baselineAuthorName, paper.title);
            gitRank = githubRankMap.getOrDefault(key, -1);
        }

        boolean hasBonus = (k > 0 && gitRank >= 1 && gitRank <= k);
        double credit = paper.citationCount * weight;

        if (hasBonus) {
            credit *= bonusMultiplier;
        }

        return credit;
    }

    public static int countBonusPapers(AuthorProfile profile, String baselineAuthorName,
                                       Map<String,Integer> githubRankMap, int k) {
        int cnt = 0;
        for (var p : profile.papers) {
            String key = makeKey(baselineAuthorName, p.title);
            int gr = githubRankMap.getOrDefault(key, -1);
            if (gr >= 1 && gr <= k) cnt++;
        }
        return cnt;
    }
}