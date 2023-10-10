
import java.io.FileOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;
import java.util.stream.Collectors;
import java.nio.file.StandardCopyOption;


class Dependency {
    public String url;
    public String filename;
    public Path path;

    Dependency(String url) {
        this.url = url;
        this.filename = new File(url).getName();
    }

    public String toString() {
        return url;
    }

    public Path getPath() {
        return this.path;
    }
}

class build 
{
    Path pwd;

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

    static void runCommand(String[] args, Path pwd, boolean inheritIO) 
    {
        var processBuilder = new ProcessBuilder(args);
        if (inheritIO)
            processBuilder.inheritIO();

        processBuilder.directory(pwd.toFile());

        try {
            var process = processBuilder.start();
            process.waitFor();

            int exit = process.exitValue();

            if (exit != 0) 
                System.exit(exit);
        } catch (Exception e) {
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
        // var parent = target.getParent() == null ? "." : target.getParent();
        // var workingDir = Paths.get(parent);

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

        ArrayList<String> buildArgs = new ArrayList<String>();
        buildArgs.add("javac");
        buildArgs.add(target.toString());

        var buildDir = Paths.get("build");
        buildDir.toFile().mkdir();
        buildDir.toFile().deleteOnExit();
        if (dependencies.size() > 0) {
            try {
                var client = HttpClient.newHttpClient();

                for (Dependency dependency : dependencies) {
                    var path = Paths.get(buildDir.toString(), dependency.filename);

                    // if (path.toFile().exists())
                    //     continue;

                    var request = HttpRequest.newBuilder()
                        .uri(URI.create(dependency.url))
                        .build();
                    System.out.println("downloading dependency: " + dependency.url);

                    client.send(request, BodyHandlers.ofFile(path));

                    dependency.path = path;

                    unzipFile(path.toString(), buildDir.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            buildArgs.add("-cp");
            buildArgs.add(
                    dependencies.stream()
                    .map(Dependency :: getPath)
                    .map(Path :: toString)
                    .collect(Collectors.joining(":"))
                    );
        }

        buildArgs.add("-d");
        buildArgs.add(buildDir.toString());

        runCommand(buildArgs.toArray(new String[0]), Paths.get("."), true);

        var mainClass = target.getName().split("\\.")[0];

        // cria um MANIFEST temporário para ser adicionado ao .jar
        final var manifest = 
            "Manifest-Version: 1.0\nClass-Path: .\nMain-Class: " + mainClass + "\n";

        var manifestPath = Paths.get(buildDir.toString(), "___manifest___");
        manifestPath.toFile().deleteOnExit();
        try { 
            FileOutputStream outputStream = 
                new FileOutputStream(manifestPath.toFile());
            outputStream.write(manifest.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // try {
        //     if (dependencies.size() > 0) {
        //         // mover todos os arquivos .class para a pasta build para que
        //         // possam ser incluidos dentro do jar junto com todas as dependencias
        //         Files.list(workingDir)
        //             .filter(x -> x.toString().endsWith(".class"))
        //             .forEach(x -> moveFile(x, buildDir));
        //         // incluir todos os arquivos dentro da pasta build 
        //         // (nosso programa e dependencias)
        //         jarFiles.add(buildDir.toString());
        //     } else {
        //         // adicionar apenas arquivos .class ao jar
        //         jarFiles.addAll(Files.list(workingDir)
        //                 .map(Path :: toString)
        //                 .filter(x -> x.endsWith(".class"))
        //                 .collect(Collectors.toList())
        //                 );
        //     }
        // } catch (Exception e) {
        //     e.printStackTrace();
        //     System.exit(1);
        // }

        ArrayList<String> jarArgs = new ArrayList<String>();
        jarArgs.add("jar");
        jarArgs.add("cvfm");
        jarArgs.add(Paths.get("..", mainClass + ".jar").toString());
        jarArgs.add("___manifest___");
        jarArgs.add(".");

        runCommand(jarArgs.toArray(new String[0]), buildDir, false);
    }
}
