## Reproducibility Note

This project depends on external APIs for retrieving bibliographic and repository contribution data.  
To reproduce the experiments, users must provide their own API credentials in:

`src/main/java/org/example/io/NetworkDataFetcher.java`

Please replace the placeholder values in the following fields:

```java
private static final String GITHUB_TOKEN = "";
private static final String S2_API_KEY = "";
