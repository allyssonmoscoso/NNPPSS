package com.squarepeace.nnppss.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squarepeace.nnppss.model.Console;

public class PackageService {
    private static final Logger log = LoggerFactory.getLogger(PackageService.class);

    public void extractPackage(String pkgName, String zRifKey, Console console) {
        log.info("Starting package extraction: {} (console: {})", pkgName, console);
        String command = buildCommand(pkgName, zRifKey, console);
        if (command == null || command.isEmpty()) {
            log.warn("No command generated for package extraction: {}", pkgName);
            return;
        }
        runCommandWithLoadingMessage(command);
    }

    private String buildCommand(String pkgName, String zRifKey, Console console) {
        String fileSeparator = System.getProperty("file.separator");
        String command = "";
        String os = System.getProperty("os.name").toLowerCase();
        log.debug("Building extraction command for OS: {}", os);

        if (os.contains("win")) {
            String exePath = "lib" + fileSeparator + "pkg2zip.exe";
            String pkgPath = "games" + fileSeparator + pkgName;
            
            if (console == Console.PSVITA) {
                command = exePath + " -x " + pkgPath + " " + zRifKey;
            } else {
                command = exePath + " " + pkgPath;
            }
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            if (isCommandInstalled("pkg2zip")) {
                String pkgPath = "games" + fileSeparator + pkgName;
                if (console == Console.PSVITA) {
                    command = "pkg2zip -x " + pkgPath + " " + zRifKey;
                } else {
                    command = "pkg2zip " + pkgPath;
                }
            } else {
                log.error("pkg2zip is not installed on system");
                JOptionPane.showMessageDialog(null, "pkg2zip is not installed");
            }
        } else {
            log.error("Unsupported operating system: {}", os);
            JOptionPane.showMessageDialog(null, "Operating system not supported");
        }
        log.debug("Generated command: {}", command);
        return command;
    }

    private boolean isCommandInstalled(String command) {
        String checkCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "where " + command : "which " + command;
        try {
            Process process = Runtime.getRuntime().exec(checkCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    process.waitFor();
                    return true;
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.warn("Error checking if command is installed: {}", command, e);
        }
        return false;
    }

    private void runCommandWithLoadingMessage(String command) {
        // This UI logic should ideally be separated, but for now we keep it here or use a callback.
        // To keep it clean, we will run it and block, letting the caller handle UI?
        // The original code showed a JDialog.
        
        SwingUtilities.invokeLater(() -> {
             // ... UI code for dialog ...
             // For this refactoring, I'll simplify and just run it in a thread, 
             // but since this method was void and showed UI, I'll replicate the behavior but cleaner.
             
             new Thread(() -> {
                 try {
                     // Use ProcessBuilder with shell splitting when needed
                     java.util.List<String> cmd;
                     if (System.getProperty("os.name").toLowerCase().contains("win")) {
                         cmd = java.util.Arrays.asList("cmd", "/c", command);
                     } else {
                         cmd = java.util.Arrays.asList("sh", "-lc", command);
                     }
                     ProcessBuilder pb = new ProcessBuilder(cmd);
                     pb.redirectErrorStream(true);
                     Process process = pb.start();

                     // Drain output
                     try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                         String line;
                         while ((line = r.readLine()) != null) {
                             log.debug("pkg2zip output: {}", line);
                         }
                     }

                     int exitCode = process.waitFor();
                     if (exitCode != 0) {
                         log.error("pkg2zip failed with exit code: {}", exitCode);
                         SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "pkg2zip failed (exit " + exitCode + ")"));
                     } else {
                         log.info("Package extraction completed successfully");
                     }
                 } catch (Exception e) {
                     log.error("Error running pkg2zip command", e);
                     SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Error running pkg2zip: " + e.getMessage()));
                 }
             }).start();
        });
    }
}
