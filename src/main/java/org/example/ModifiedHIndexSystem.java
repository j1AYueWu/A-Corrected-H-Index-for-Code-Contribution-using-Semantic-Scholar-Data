package org.example;

import org.example.io.AuthorTask;
import org.example.io.AuthorTaskReader;
import org.example.io.NetworkDataFetcher;
import org.example.io.ResultExporter;
import org.example.model.AuthorProfile;
import org.example.model.AuthorResult;
import org.example.model.SemanticScholarPaper;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ModifiedHIndexSystem
 * execute the GitHub-aware weighted h-index experiment
 *
 * Output：
 * Experimental_Results.csv
 *    - Original h-index
 *    - Model A weighted h-index
 *    - Model B weighted h-index
 *    - GitHub bonus h-index (rk1 / rk2 / rk3)
 */
public class ModifiedHIndexSystem {

    double bonusMultiplier = 1.75;

    public void process() throws Exception {
        // Step 1: Read all author tasks from the CSV file
        List<AuthorTask> allTasks = AuthorTaskReader.loadTasks("authors_data.csv");
        // Step 2: // Step 2: Group by author ID
        Map<String, List<AuthorTask>> tasksByAuthor =
                allTasks.stream().collect(Collectors.groupingBy(t -> t.s2Id));

        List<AuthorResult> results = new ArrayList<>();
        // Step 3: Retrieve GitHub contribution rankings from the baseline results
        // (to avoid making duplicate API calls)
        Map<String, Integer> rankMap =
                HIndexCalculator.loadGithubRanksFromBaseline("Baseline_Validation.csv");

        // Step 4: Iterate through each author and calculate their h-index for each model
        for (Map.Entry<String, List<AuthorTask>> entry : tasksByAuthor.entrySet()) {

            String s2Id = entry.getKey();
            List<AuthorTask> authorTasks = entry.getValue();

            String authorName = authorTasks.get(0).name;
            System.out.println("[log] analysing author: " + authorName);
            // Step 4.1: Retrieve complete author paper data from Semantic Scholar
            AuthorProfile profile = NetworkDataFetcher.fetchS2Profile(s2Id);

            if (profile == null || profile.papers == null) continue;
            // Step 4.2: Calculate the raw h-index
            int hOrig = calculateOriginalH(profile);

            // Step 4.3: Calculating two weighted h-indices
            int hA = HIndexCalculator.calculateWeightedH(profile, s2Id, "A");
            int hB = HIndexCalculator.calculateWeightedH(profile, s2Id, "B");

            // Step 4.4: Calculating the GitHub-aware model (with different k values for sensitivity analysis)
            //            // k represents the threshold for core contributors (top k)
            //rk1
            int hBonus1 = HIndexCalculator.calculateInnovativeH(
                    profile, s2Id, "A", authorName, rankMap, 1, bonusMultiplier);
            //rk2
            int hBonus2 = HIndexCalculator.calculateInnovativeH(
                    profile, s2Id, "A", authorName, rankMap, 2, bonusMultiplier);
            //rk3
            int hBonus3 = HIndexCalculator.calculateInnovativeH(
                    profile, s2Id, "A", authorName, rankMap, 3, bonusMultiplier);

            results.add(new AuthorResult(
                    authorName,
                    hOrig,
                    hA,
                    hB,
                    hBonus1,
                    hBonus2,
                    hBonus3
            ));


        }

        ResultExporter.saveResults("Experimental_Results.csv", results);

        System.out.println("[Complete] Experimental results exported.");
    }

    private int calculateOriginalH(AuthorProfile profile) {

        List<Integer> citations = profile.papers.stream()
                .map(p -> p.citationCount)
                .sorted(Comparator.reverseOrder())
                .toList();

        int h = 0;

        for (int i = 0; i < citations.size(); i++) {
            if (citations.get(i) >= i + 1) h = i + 1;
            else break;
        }
        return h;
    }

}