package org.opendaylight.controller.distribution;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;

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

        String os = System.getProperty("os.name").toLowerCase();
        List<String> script = new ArrayList<String>();

        if(os.contains("windows")){
            script.add("cmd.exe");
            script.add("/c");
            script.add("runsanity.bat");
        } else {
            script.add("./runsanity.sh");
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.inheritIO().command(script);
        Process p = processBuilder.start();

        copy(p.getInputStream(), System.out);

        p.waitFor();

        System.out.println("Test exited with exitValue = " + p.exitValue());

        System.exit(p.exitValue());
    }
}
