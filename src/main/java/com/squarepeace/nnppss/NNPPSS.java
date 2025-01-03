/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.squarepeace.nnppss;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.JOptionPane;

public class NNPPSS {
    public static List<String> DownloadList = new ArrayList<>();
    public static List<String> fileURLs = new ArrayList<>();
    public static List<String> localFilePaths = new ArrayList<>();
    public static List<String> fileNames = new ArrayList<>();
    public static List<String> zRIFs = new ArrayList<>();
    public static List<String> consoles = new ArrayList<>();
    private static Set<String> processedFiles = new HashSet<>(); // To track processed files

    public static void main(String[] args) throws IOException {
        Config configFrame = new Config();
        Utilities utilities = new Utilities();
        Frame frame = new Frame(utilities);

        utilities.createConfigFile();

        try (FileReader reader = new FileReader("config.properties")) {
            utilities.getUrlsFromPage();
            Properties p = new Properties();
            p.load(reader);
            String psvitaUrl = p.getProperty("psvita.url");
            String pspUrl = p.getProperty("psp.url");
            String psxUrl = p.getProperty("psx.url");

            // si las urls estan vacias, se abre la ventana de configuracion
            if (psvitaUrl.isEmpty() && pspUrl.isEmpty() && psxUrl.isEmpty()) {
                configFrame.pack();
                configFrame.setLocationRelativeTo(null);
                configFrame.setResizable(false);
                configFrame.setVisible(true);
                configFrame.loadValues();
                return;
            }

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);

            // Descargar en segundo plano
            processDownload(psvitaUrl, utilities.TSV_VITA, frame);
            processDownload(pspUrl, utilities.TSV_PSP, frame);
            processDownload(psxUrl, utilities.TSV_PSX, frame);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processDownload(String url, String tsv, Frame frame) {
        if (!url.isEmpty()) {
            File tsvFile = new File(tsv);
            if (!tsvFile.exists()) {
                try {
                    Utilities utilities = new Utilities();
                    utilities.downloadFile(url, tsv);
                    System.out.println(tsv + " downloaded");
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Failed to download " + tsv);
                    return;
                }
            }

            DownloadList.add(url);
            DownloadList.add(tsv);
            DownloadList.add(tsv.substring(3));
            DownloadList.add("dfadsfsdsf");
            DownloadList.add("fsdfdfdfd");

            for (int i = 0; i < DownloadList.size(); i += 5) {
                String fileUrl = DownloadList.get(i);
                if (processedFiles.contains(fileUrl)) {
                    continue; // Skip already processed files
                }
                processedFiles.add(fileUrl);

                fileURLs.add(fileUrl);
                localFilePaths.add(DownloadList.get(i + 1));
                fileNames.add(DownloadList.get(i + 2));
                zRIFs.add(DownloadList.get(i + 3));
                consoles.add(DownloadList.get(i + 4));

                DownloadThread downloadThread = new DownloadThread(frame, fileURLs, localFilePaths, fileNames, zRIFs, consoles);
                DownloadList.clear();
                downloadThread.start();

                try {
                    downloadThread.join();
                    if (downloadThread.isDownloadSuccessful()) {
                        // Llenar la tabla y el combobox después de la descarga
                        // frame.fillTableAndComboBox();
                    } else {
                        JOptionPane.showMessageDialog(frame, "Download failed. Unable to populate table and combobox.");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Clase interna para la descarga en segundo plano
    static class DownloadThread extends Thread {
        private final Frame frame;
        private final List<String> url;
        private final List<String> fileName;
        private final List<String> localFilePaths;
        private final List<String> zRIFs;
        private final List<String> consoles;
        private boolean downloadSuccessful;

        public DownloadThread(Frame frame, List<String> url, List<String> localFilePaths, List<String> fileName, List<String> zRIFs, List<String> consoles) {
            this.frame = frame;
            this.url = url;
            this.localFilePaths = localFilePaths;
            this.zRIFs = zRIFs;
            this.consoles = consoles;
            this.fileName = fileName;
            this.downloadSuccessful = false;
        }

        @Override
        public void run() {
            frame.downloadFilesInBackground(url, localFilePaths, fileName, zRIFs, consoles, null);
            this.downloadSuccessful = true;
        }

        public boolean isDownloadSuccessful() {
            return downloadSuccessful;
        }
    }
}