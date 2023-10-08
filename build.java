
import java.io.FileOutputStream;

class build 
{
    public static void main(String[] args) 
    {
        if (args.length == 0) {
            System.out.println("missing build target!");
            System.exit(1);
        }

        var target = args[0];
        System.out.println("build target: " + target);
        String[] buildArgs = {
            "javac", target
        };

        var processBuilder = new ProcessBuilder(buildArgs);
        processBuilder.inheritIO();
        Process process;

        try 
        {
            process = processBuilder.start();
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
        System.out.println(".class created");

        try 
        {
            final var manifest = 
                "Manifest-Version: 1.0\nClass-Path: .\nMain-Class: Test\n";
            FileOutputStream outputStream = new FileOutputStream("test/MANIFEST.MF");
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
            "jar", "cvfm", "test/out.jar", "test/MANIFEST.MF", "test/."
        };

        var pb = new ProcessBuilder(jarArgs);
        pb.inheritIO();

        try 
        {
            process = pb.start();
            process.waitFor();

            int exit = process.exitValue();

            if (exit != 0) 
                System.exit(exit);

            System.out.println("success!");
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
