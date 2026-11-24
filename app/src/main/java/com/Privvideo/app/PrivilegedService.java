package com.Privvideo.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Serviço executado pelo Shizuku com identidade shell/root autorizada pelo usuário. */
public final class PrivilegedService extends IPrivilegedService.Stub {
    public String execute(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "Comando vazio";
        }
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("exitCode=").append(exitCode);
            }
            return output.toString();
        } catch (Exception e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (process != null) process.destroy();
        }
    }

    public void destroy() {
        System.exit(0);
    }

    public void exit() {
        destroy();
    }
}
