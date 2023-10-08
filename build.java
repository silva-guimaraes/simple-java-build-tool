
import java.io.FileOutputStream;
import java.io.File;
import java.util.ArrayList;

class build 
{
    static void runCommand(String[] args, boolean inheritIO) {
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
        try 
        {
            // cria um MANIFEST temporário para ser adicionado ao .jar
            final var manifest = 
                "Manifest-Version: 1.0\nClass-Path: .\nMain-Class: " + mainClass + "\n";
            var manifestFilename = workingDir + "___manifest___";
            FileOutputStream outputStream = new FileOutputStream(manifestFilename);
            outputStream.write(manifest.getBytes());
            outputStream.close();
            jarFiles.add(manifestFilename);
            new File(manifestFilename).deleteOnExit();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            System.exit(1);
        }

        for (var i : new File(workingDir).list()) {
            if (i.endsWith(".class")) {
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
