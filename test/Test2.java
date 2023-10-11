
// SJBT: @dependency net.lingala.zip4j:zip4j:2.11.5

import net.lingala.zip4j.ZipFile;

class Test2 {
    public static void main(String[] args) {
        System.out.println("foo bar foo bar");
        try {
            new ZipFile("filename.zip").addFile("Test.java");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
