
class build 
{
    public static void main(String[] args) 
    {
        var target = "build.java";
        System.out.println("build target: " + target);

        var processBuilder = new ProessBuilder("javac", target);
        processBuilder.inheritIO();
        Process process;

        try 
        {
            process = processBuilder.start();
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
