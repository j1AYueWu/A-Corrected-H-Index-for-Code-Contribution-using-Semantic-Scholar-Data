package org.example.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorProfile {
    public String authorId;
    public String name;
    public List<SemanticScholarPaper> papers;
}
