import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

/** Optional read-only Jira Cloud import. Credentials stay in environment variables. */
class JiraTicket {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].matches("[A-Z][A-Z0-9_]*-[0-9]+")) throw new IllegalArgumentException("Provide the actual Jira key, such as OG-1.");
        String key = args[0];
        String site = required("JIRA_SITE").replaceAll("/$", "");
        URI origin = URI.create(site);
        if (!origin.getScheme().equals("https") || origin.getHost() == null || !origin.getHost().endsWith(".atlassian.net")
            || origin.getRawUserInfo() != null || origin.getPort() != -1 || !origin.getPath().isEmpty() || origin.getQuery() != null || origin.getFragment() != null) {
            throw new IllegalArgumentException("JIRA_SITE must be your Jira Cloud origin: https://your-team.atlassian.net");
        }
        String credentials = required("JIRA_EMAIL") + ":" + required("JIRA_API_TOKEN");
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
        String apiBase = site;
        String cloudId = System.getenv("JIRA_CLOUD_ID");
        if (cloudId != null && !cloudId.isBlank()) {
            if (!cloudId.matches("[A-Za-z0-9-]+")) throw new IllegalArgumentException("Invalid JIRA_CLOUD_ID.");
            apiBase = "https://api.atlassian.com/ex/jira/" + cloudId;
        }
        var request = HttpRequest.newBuilder(URI.create(apiBase + "/rest/api/3/issue/" + key + "?fields=summary,description,status,issuetype"))
            .timeout(Duration.ofSeconds(30)).header("Accept", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8))).GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("Jira import returned HTTP " + response.statusCode() + ". Check site, credentials, token type, and issue permissions. No file written.");
        Path output = Path.of("work", key, "jira.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, response.body());
        System.out.println("Imported " + key + " into " + output + ". This script never changes Jira status.");
    }
    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Set " + name + " first. See docs/JIRA_GITHUB.md.");
        return value;
    }
}
