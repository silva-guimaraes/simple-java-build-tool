
import java.io.FileOutputStream;
import java.io.File;
import java.util.ArrayList;
// import java.nio.*;

class build 
{
    static void runCommand(String[] args) {
        var processBuilder = new ProcessBuilder(args);
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

        System.out.println("build target: " + target);
        String[] buildArgs = {
            "javac", target.toString()
        };

        runCommand(buildArgs);
        System.out.println(".class created.");

        ArrayList<String> jarFiles = new ArrayList<String>();

        try 
        {
            final var manifest = 
                "Manifest-Version: 1.0\nClass-Path: .\nMain-Class: Test\n";
            var manifestFilename = workingDir + "MANIFEST.MF";
            FileOutputStream outputStream = new FileOutputStream(manifestFilename);
            outputStream.write(manifest.getBytes());
            outputStream.close();
            jarFiles.add(manifestFilename);
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
        jarArgs.add(workingDir + "out.jar");
        jarArgs.addAll(jarFiles);

        System.out.println("creating jar...");
        runCommand(jarArgs.toArray(new String[0]));
        System.out.println("success!");
    }
}
