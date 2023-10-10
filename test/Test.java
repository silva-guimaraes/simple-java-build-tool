
// SJBT: @dependency org.jsoup:jsoup:1.16.1
// // SJBT: @dependency net.lingala.zip4j:zip4j:2.11.5

import org.jsoup.Jsoup;
// import net.lingala.zip4j.ZipFile;

class Test {
    public static void main(String[] args) {
        System.out.println("foo bar foo bar");
        try {
            // new ZipFile("filename.zip").addFile("Test.java");

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
