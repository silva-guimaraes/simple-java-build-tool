
import java.io.FileOutputStream;
import java.io.File;
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
        var workingDir = target.getParent() + File.separator;

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

        try 
        {
            final var manifest = 
                "Manifest-Version: 1.0\nClass-Path: .\nMain-Class: Test\n";
            FileOutputStream outputStream = 
                new FileOutputStream(
                        workingDir + "MANIFEST.MF"
                        );
            outputStream.write(manifest.getBytes());
            outputStream.close();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("creating jar...");
        String[] jarArgs = {
            "jar", "cvfm", 
            workingDir + "out.jar", 
            workingDir + "MANIFEST.MF", 
            workingDir 
        };

        runCommand(jarArgs);
        System.out.println("success!");
    }
}
