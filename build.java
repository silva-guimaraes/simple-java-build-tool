
import java.io.FileOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.nio.file.Paths;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;


class Dependency {
    public String url;
    public String filename;
    public String path;

    Dependency(String url) {
        this.url = url;
        this.filename = new File(url).getName();
    }

    public String toString() {
        return url;
    }
}

class build 
{
    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + 
                    zipEntry.getName());
        }

        return destFile;
    }
    // https://www.baeldung.com/java-compress-and-uncompress
    public static void unzipFile(String zipPath, String buildDir) throws IOException {
        // String zipPath = "src/main/resources/unzipTest/compressed.zip";
        File destDir = new File(buildDir);

        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) 
        {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    static void runCommand(String[] args, boolean inheritIO) 
    {
        var processBuilder = new ProcessBuilder(args);
        if (inheritIO)
            processBuilder.inheritIO();

        try 
        {
            var process = processBuilder.start();
            process.waitFor();

            int exit = process.exitValue();

            if (exit != 0) 
                System.exit(exit);
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public static void main(String[] args) 
    {
        if (args.length == 0) {
            System.err.println("missing build target!");
            System.exit(1);
        }

        var target = new File(args[0]);
        var parent = target.getParent() == null ? "." : target.getParent();
        var workingDir = parent + File.separator;
        // System.out.println(parent);

        if (!target.exists()) {
            System.err.println(target + " file does not exist.");
            System.exit(1);
        }

        var dependencies = new ArrayList<Dependency>();
        try {
            var filereader = new FileReader(target);
            var br = new BufferedReader(filereader);
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("// SJBT: @dependency ")) {
                    dependencies.add(new Dependency(line.substring(21)));
                }
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (dependencies.size() > 0) {
            var buildDir = new File(workingDir + "build");
            buildDir.mkdir();
            // buildDir.deleteOnExit();
            try {
                var client = HttpClient.newHttpClient();

                for (Dependency dependency : dependencies) {
                    var path = buildDir.toString() + File.separator + dependency.filename;

                    if (new File(path).exists())
                        continue;

                    var request = HttpRequest.newBuilder()
                        .uri(URI.create(dependency.url))
                        .build();
                    System.out.println("downloading dependency: " + dependency.url);

                    System.out.println("path: "  + path);
                    client.send(request, 
                            BodyHandlers.ofFile(Paths.get(path)));
                    dependency.path = path;

                    unzipFile(path, buildDir.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }


        // compilar codigo fonte para arquivos de classes
        // System.out.println("build target: " + target);
        String[] buildArgs = {
            "javac", target.toString()
        };

        runCommand(buildArgs, true);
        // System.out.println(".class created.");

        // transformar arquivos de classes em um .jar 
        ArrayList<String> jarFiles = new ArrayList<String>();

        var mainClass = target.getName().split("\\.")[0];

        try { // cria um MANIFEST temporário para ser adicionado ao .jar

            final var manifest = 
                "Manifest-Version: 1.0\nClass-Path: .\nMain-Class: " + mainClass + "\n";

            var manifestFilename = new File(workingDir + "___manifest___");
            manifestFilename.deleteOnExit();

            FileOutputStream outputStream = new FileOutputStream(manifestFilename);
            outputStream.write(manifest.getBytes());
            outputStream.close();

            jarFiles.add(manifestFilename);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        for (var i : new File(workingDir).list()) 
        {
            if (i.endsWith(".class")) 
            {
                jarFiles.add(workingDir + i);
            }
        }

        ArrayList<String> jarArgs = new ArrayList<String>();
        jarArgs.add("jar");
        jarArgs.add("cvfm");
        jarArgs.add(workingDir + mainClass + ".jar");
        jarArgs.addAll(jarFiles);

        runCommand(jarArgs.toArray(new String[0]), false);
    }
}
