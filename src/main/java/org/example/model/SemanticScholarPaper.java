package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)

public class SemanticScholarPaper {
    public String paperId;
    public String title;
    public int citationCount; //引用数
    public List<AuthorReference> authors;
    public boolean hasValidatedArtifact = false;
    public long adScore = 0;
}
