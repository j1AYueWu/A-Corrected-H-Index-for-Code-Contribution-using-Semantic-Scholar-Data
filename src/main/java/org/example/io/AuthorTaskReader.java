package org.example.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuthorTaskReader {
    /**
     * Load author data from authors_data.csv
     */
    public static List<AuthorTask> loadTasks(String filePath) throws IOException {
        List<AuthorTask> tasks = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length >= 5) {
                    tasks.add(new AuthorTask(
                            cols[0].trim(), // Name
                            cols[1].trim(), // S2_Author_ID (确保为 String)
                            cols[2].trim(), // GitHub_Handle
                            cols[3].trim(), // Paper_Title
                            cols[4].trim()  // Repo_Path
                    ));
                }
            }
        }
        return tasks;
    }
}

