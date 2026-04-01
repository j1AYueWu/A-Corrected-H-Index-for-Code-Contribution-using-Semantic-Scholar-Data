package org.example.io;

import java.util.List;

public class AuthorTask {
    public String name, s2Id, githubHandle, targetPaperTitle;
    public List<String> repoPaths;

    public AuthorTask(String n, String s, String g, String p, String r) {
        this.name = n; this.s2Id = s; this.githubHandle = g;
        this.targetPaperTitle = p;
        // Handling multiple repositories separated by semicolons
        this.repoPaths = java.util.Arrays.stream(r.split(";"))
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }
}
