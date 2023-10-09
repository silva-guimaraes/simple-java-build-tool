
// SJBT: @dependency https://jsoup.org/packages/jsoup-1.16.1.jar
// SJBT: @dependency https://repo1.maven.org/maven2/org/slog4j/slog4j/0.20.0/slog4j-0.20.0.jar
import org.jsoup.Jsoup;

class Test {
    public static void main(String[] args) {
        System.out.println("foo bar foo bar");
        try {
            var doc = Jsoup.connect("https://example.com").get();
            System.out.println(doc.title());
            var newsHeadlines = doc.select("#mp-itn b a");
            for (var headline : newsHeadlines) {
                System.out.printf("%s\n\t%s", 
                        headline.attr("title"), headline.absUrl("href"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
