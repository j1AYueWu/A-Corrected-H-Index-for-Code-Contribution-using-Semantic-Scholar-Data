package org.example;

import java.util.List;
import org.example.io.*;
import org.example.model.*;
import java.util.*;
import java.util.stream.Collectors;

public class BaselineStudySystem {


    public void runBaseline() throws Exception {
        List<AuthorTask> tasks = AuthorTaskReader.loadTasks("authors_data.csv");
        List<BaselineResult> results = new ArrayList<>();

        for (AuthorTask task : tasks) {
            System.out.println("[Benchmark test] Analysing: " + task.name);

            // 1. Obtaining academic data (introducing mandatory downtime to circumvent this)
            AuthorProfile profile = NetworkDataFetcher.fetchS2Profile(task.s2Id);
            if (profile == null || profile.papers == null) {
                System.err.println("  [Skip] Unable to retrieve author data; this may have triggered an S2 429 limit.");
                Thread.sleep(5000);
                continue;
            }

            // 2. Retrieve GitHub statistics (includes a built-in retry mechanism with 20 attempts)
            // Concatenate the original URLs for Fetcher to clean up
            String rawRepoUrls = String.join(";", task.repoPaths);
            Map<String, Object> gitStats = NetworkDataFetcher.getGitHubStats(rawRepoUrls, task.githubHandle);

            long adScore = (long) gitStats.get("score");
            int gitRank = (int) gitStats.get("rank");

            // 3. Calculating rank and the h-index
            int academicRank = getAuthorRankInPaper(profile, task.targetPaperTitle, task.s2Id);
            int hOrig = calculateOriginalH(profile);
            int hA = HIndexCalculator.calculateWeightedH(profile, task.s2Id, "A");
            int hB = HIndexCalculator.calculateWeightedH(profile, task.s2Id, "B");

            results.add(new BaselineResult(task.name, task.targetPaperTitle, academicRank, hOrig, hA, hB, adScore, gitRank));

            // 4. Mandatory rest periods after each task to protect the API
            System.out.println("  [Complete] 5-second cooldown...");
            Thread.sleep(5000);
        }

        saveBaselineCsv(results);
        System.out.println("\n[Log] Experiment completed");
    }

    private int getAuthorRankInPaper(AuthorProfile profile, String title, String s2Id) {
        for (SemanticScholarPaper p : profile.papers) {
            if (isTitleMatch(p.title, title)) {
                for (int i = 0; i < p.authors.size(); i++) {
                    if (p.authors.get(i).authorId.equals(s2Id)) return i + 1;
                }
            }
        }
        return -1;
    }


    private void saveBaselineCsv(List<BaselineResult> list) throws Exception {
        java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("Baseline_Validation.csv"));
        pw.println("Author,Paper,Paper_Rank,h_Orig,h_ModelA,h_ModelB,GitHub_AD,GitHub_Rank");
        for (BaselineResult r : list) {
            pw.printf("%s,%s,%d,%d,%d,%d,%d,%d\n",
                    r.name, r.paper, r.paperRank, r.hOrig, r.hA, r.hB, r.ad, r.gitRank);
        }
        pw.close();
        System.out.println("[Completed] The benchmark verification document has been generated: Baseline_Validation.csv");
    }

    private static class BaselineResult {
        String name, paper;
        int paperRank, hOrig, hA, hB, gitRank;
        long ad;
        BaselineResult(String n, String p, int pr, int ho, int ha, int hb, long ad, int gr) {
            this.name = n; this.paper = p; this.paperRank = pr;
            this.hOrig = ho; this.hA = ha; this.hB = hb; this.ad = ad; this.gitRank = gr;
        }
    }
    private int calculateOriginalH(AuthorProfile profile) {
        List<Integer> citations = profile.papers.stream()
                .map(p -> p.citationCount)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        int h = 0;
        for (int i = 0; i < citations.size(); i++) {
            if (citations.get(i) >= (i + 1)) h = i + 1;
            else break;
        }
        return h;
    }

    private boolean isTitleMatch(String s2Title, String targetTitle) {
        if (s2Title == null || targetTitle == null) return false;
        String t1 = s2Title.toLowerCase().replaceAll("[^a-z0-9]", "");
        String t2 = targetTitle.toLowerCase().replaceAll("[^a-z0-9]", "");
        return t1.contains(t2) || t2.contains(t1);
    }
}