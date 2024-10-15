/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.squarepeace.nnppss;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;

public class NNPPSS {
    public static List<String> DownloadList = new ArrayList<>();
    public static List<String> fileURLs = new ArrayList<>();
    public static List<String> localFilePaths = new ArrayList<>();
    public static List<String> fileNames = new ArrayList<>();
    public static List<String> zRIFs = new ArrayList<>();
    public static List<String> consoles = new ArrayList<>();
    public static void main(String[] args) throws IOException {

        Config configFrame = new Config();
        Utilities utilities = new Utilities();
        Frame frame = new Frame(utilities);

        utilities.createConfigFile();

        FileReader reader;
        try {
            utilities.getUrlsFromPage();
            reader = new FileReader("config.properties");
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
                if (!psvitaUrl.isEmpty()) {
                    
                DownloadList.add(psvitaUrl);
                DownloadList.add(utilities.TSV_VITA);
                DownloadList.add(utilities.TSV_VITA.substring(3));
                DownloadList.add("dfadsfsdsf");
                DownloadList.add("fsdfdfdfd");
                // download the list of games in the download list
                for (int i = 0; i < DownloadList.size(); i += 5) {
                    

                    fileURLs.add(DownloadList.get(i));
                    localFilePaths.add(DownloadList.get(i + 1));
                    fileNames.add(DownloadList.get(i + 2));
                    zRIFs.add(DownloadList.get(i + 3));
                    consoles.add(DownloadList.get(i + 4));

                    //frame.downloadFilesInBackground(fileURLs, localFilePaths, n , null, null);
                    
                    DownloadThread downloadThread = new DownloadThread(frame, fileURLs, localFilePaths, fileNames, zRIFs, consoles);
                    DownloadList.clear();
                    downloadThread.start();
                    try {
                        downloadThread.join();
                        // Verificar si la descarga se completó con éxito antes de llenar la tabla y el combobox
                        if (downloadThread.isDownloadSuccessful()) {
                            // Llenar la tabla y el combobox después de la descarga
                            // frame.fillTableAndComboBox();
                        } else {
                            // Imprimir un mensaje de error si la descarga falló
                            JOptionPane.showMessageDialog(frame, "Download failed. Unable to populate table and combobox.");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                

                    
                    
                    
                }

                if (!pspUrl.isEmpty()) {
                    

                    DownloadList.add(pspUrl);
                DownloadList.add(utilities.TSV_PSP);
                DownloadList.add(utilities.TSV_PSP.substring(3));
                DownloadList.add("dfadsfsdsf");
                DownloadList.add("fsdfdfdfd");
                // download the list of games in the download list
                for (int i = 0; i < DownloadList.size(); i += 5) {
                    

                    fileURLs.add(DownloadList.get(i));
                    localFilePaths.add(DownloadList.get(i + 1));
                    fileNames.add(DownloadList.get(i + 2));
                    zRIFs.add(DownloadList.get(i + 3));
                    consoles.add(DownloadList.get(i + 4));

                    //frame.downloadFilesInBackground(fileURLs, localFilePaths, n , null, null);
                    
                    DownloadThread downloadThread = new DownloadThread(frame, fileURLs, localFilePaths, fileNames, zRIFs, consoles);
                    DownloadList.clear();
                    downloadThread.start();
                    
                    
                    try {
                        downloadThread.join();
                        // Verificar si la descarga se completó con éxito antes de llenar la tabla y el combobox
                        if (downloadThread.isDownloadSuccessful()) {
                            // Llenar la tabla y el combobox después de la descarga
                            // frame.fillTableAndComboBox();
                        } else {
                            // Imprimir un mensaje de error si la descarga falló
                            JOptionPane.showMessageDialog(frame, "Download failed. Unable to populate table and combobox.");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                }

                if (!psxUrl.isEmpty()) {
                    

                    DownloadList.add(psxUrl);
                DownloadList.add(utilities.TSV_PSX);
                DownloadList.add(utilities.TSV_PSX.substring(3));
                DownloadList.add("dfadsfsdsf");
                DownloadList.add("fsdfdfdfd");
                // download the list of games in the download list
                for (int i = 0; i < DownloadList.size(); i += 5) {
                    

                    fileURLs.add(DownloadList.get(i));
                    localFilePaths.add(DownloadList.get(i + 1));
                    fileNames.add(DownloadList.get(i + 2));
                    zRIFs.add(DownloadList.get(i + 3));
                    consoles.add(DownloadList.get(i + 4));

                    //frame.downloadFilesInBackground(fileURLs, localFilePaths, n , null, null);
                    
                    DownloadThread downloadThread = new DownloadThread(frame, fileURLs, localFilePaths, fileNames, zRIFs, consoles);
                    DownloadList.clear();
                    downloadThread.start();
                    
                    try {
                        downloadThread.join();
                        // Verificar si la descarga se completó con éxito antes de llenar la tabla y el combobox
                        if (downloadThread.isDownloadSuccessful()) {
                            // Llenar la tabla y el combobox después de la descarga
                            // frame.fillTableAndComboBox();
                        } else {
                            // Imprimir un mensaje de error si la descarga falló
                            JOptionPane.showMessageDialog(frame, "Download failed. Unable to populate table and combobox.");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                }

            
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    // Clase interna para la descarga en segundo plano
    static class DownloadThread extends Thread {
        private final Frame frame;
        //private final String url;
        private  List<String> url = new ArrayList<>();
        private  List<String> fileName = new ArrayList<>();
        private  List<String> localFilePaths = new ArrayList<>();
        private  List<String> zRIFs = new ArrayList<>();
        private  List<String> consoles = new ArrayList<>();
        private  List<String> DownloadList = new ArrayList<>();
        //private final String fileName;
        private boolean downloadSuccessful;

        public DownloadThread(Frame frame, List<String> url , List<String> localFilePaths, List<String> fileName, List<String> zRIFs, List<String> consoles) {
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

            //DownloadList.add(url);
            //DownloadList.add(localFilePaths);
            //DownloadList.add(fileName);


            // Descargar el archivo en segundo plano
            frame.downloadFilesInBackground(url, localFilePaths, fileName , zRIFs, consoles);
            // Marcar la descarga como exitosa si no se lanzan excepciones
            this.downloadSuccessful = true;
        }

        public boolean isDownloadSuccessful() {
            return downloadSuccessful;
        }
    }

}