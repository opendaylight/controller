package org.opendaylight.controller.distribution;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Sanity {

    static void copy(InputStream in, OutputStream out) throws IOException {
      while (true) {
        int c = in.read();
        if (c == -1) break;
        out.write((char)c);
      }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String cwd = System.getProperty("user.dir");

        System.out.println("Current working directory = " + cwd);

        // We assume that the program is being run from the sanitytest directory
        // We need to specify the opendaylight directory as the working directory for the shell/batch scripts
        File processWorkingDir = new File(cwd, "../opendaylight");

        String os = System.getProperty("os.name").toLowerCase();
        String script = "./run.sh";

        if(os.contains("windows")){
            script = "run.bat";
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(processWorkingDir.getCanonicalFile());
        processBuilder.command(script);
        Process p = processBuilder.start();

        copy(p.getInputStream(), System.out);

        p.waitFor();

        System.out.println("Test exited with exitValue = " + p.exitValue());

        System.exit(p.exitValue());
    }
}
