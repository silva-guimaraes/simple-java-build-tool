
import java.io.FileOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
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
        if (url.startsWith("http")) {
            this.url = url;
            this.filename = new File(url).getName();
            return;
        }
        var spec = url.split(":");
        var packagePath = spec[0].replace('.', '/');
        var formatted = String.format(
                "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
                packagePath, spec[1], spec[2], spec[1], spec[2]);

        this.url = formatted;
        this.filename = new File(formatted).getName();
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
    public static void cleanDependency(Path jar, String buildDir) {
        System.out.println("cleaning dependency: " + jar);
        try {
            File destDir = new File(buildDir);

            ZipInputStream zis = new ZipInputStream(
                    new FileInputStream(jar.toFile()));
            ZipEntry zipEntry;
            ArrayList<File> entries = new ArrayList<File>();

            while ((zipEntry = zis.getNextEntry()) != null) 
            {
                entries.add(newFile(destDir, zipEntry));
            }
            Collections.reverse(entries);
            entries.stream().forEach(File :: delete);

            jar.toFile().delete();

            zis.closeEntry();
            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    public static File newFile(File destinationDir, ZipEntry zipEntry) {
        try {
            File destFile = new File(destinationDir, zipEntry.getName());

            String destDirPath = destinationDir.getCanonicalPath();
            String destFilePath = destFile.getCanonicalPath();

            if (!destFilePath.startsWith(destDirPath + File.separator)) {
                throw new IOException("Entry is outside of the target dir: " + 
                        zipEntry.getName());
            }

            return destFile;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        // !!!!
        return new File("null");
    }
    // https://www.baeldung.com/java-compress-and-uncompress
    public static void unzipFile(String zipPath, String buildDir) throws IOException {
        // String zipPath = "src/main/resources/unzipTest/compressed.zip";
        File destDir = new File(buildDir);

        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath));
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) 
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

        if (!target.exists()) {
            System.err.println(target + " file does not exist.");
            System.exit(1);
        }

        // nessa ferramenta dependencias sao declaradas com comentarios no inicio
        // do arquivo principal e baixadas e instaladas automaticamente.
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
        // buildDir.toFile().deleteOnExit();
        if (dependencies.size() > 0) {
            try {
                var client = HttpClient.newHttpClient();

                for (Dependency dependency : dependencies) {
                    var path = Paths.get(buildDir.toString(), dependency.filename);
                    dependency.path = path;

                    // não baixar depencias já instaladas
                    if (path.toFile().exists())
                        continue;

                    // baixar dependencias
                    System.out.println("downloading dependency: " + dependency.url);
                    var request = HttpRequest.newBuilder()
                        .uri(URI.create(dependency.url))
                        .build();
                    client.send(request, BodyHandlers.ofFile(path));


                    // remover do zip para que arquivos .class possam ser 
                    // adicionados ao .jar final
                    unzipFile(path.toString(), buildDir.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            // adicionar bibliotecas ao classpath para a compilação
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

        // compilar javac
        runCommand(buildArgs.toArray(new String[0]), Paths.get("."), true);

        try {
            var dep = dependencies.stream()
                .map(Dependency :: getPath)
                .map(Path :: toString)
                .collect(Collectors.toList());
            Files.list(buildDir)
                .map(Path :: toString)
                .filter(x -> x.endsWith(".jar"))
                .filter(x -> !dep.contains(x))
                .map(Paths :: get)
                .forEach(x -> cleanDependency(x, buildDir.toString()));
            
        } catch (Exception e) {
            //TODO: handle exception
        }

        var mainClass = target.getName().split("\\.")[0];

        // cria um MANIFEST temporário para ser adicionado ao .jar
        final String manifestName =  "___manifest___";
        final var manifest = 
            "Manifest-Version: 1.0\nClass-Path: .\nMain-Class: " + mainClass + "\n";

        var manifestPath = Paths.get(buildDir.toString(), manifestName);
        // manifestPath.toFile().deleteOnExit();
        try { 
            FileOutputStream outputStream = 
                new FileOutputStream(manifestPath.toFile());
            outputStream.write(manifest.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // esse comando é executado dentro do diretório build
        ArrayList<String> jarArgs = new ArrayList<String>();
        jarArgs.add("jar");
        jarArgs.add("cvfm");
        // criar .jar fora diretório build
        jarArgs.add(Paths.get("..", mainClass + ".jar").toString());
        jarArgs.add(manifestName);
        jarArgs.add(".");

        // criar .jar
        runCommand(jarArgs.toArray(new String[0]), buildDir, false);
    }
}
