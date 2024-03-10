/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.squarepeace.nnppss;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;

public class NNPPSS {

    public static void main(String[] args) throws IOException {

        /*
         * Utilities utilities = new Utilities();
         * 
         * 
         * // url database
         * String url = utilities.getVitaGamesURL();
         * 
         * Frame frame = new Frame(utilities);
         * frame.pack();
         * frame.setLocationRelativeTo(null);
         * frame.setResizable(false);
         * frame.setVisible(true);
         */
        Config configFrame = new Config();
        Utilities utilities = new Utilities();
        Frame frame = new Frame(utilities);

        utilities.createConfigFile();

        FileReader reader;
        try {
            reader = new FileReader("config.properties");
            Properties p = new Properties();
            p.load(reader);
            String psvitaUrl = p.getProperty("psvita.url");
            String pspUrl = p.getProperty("psp.url");

            // si las urls estan vacias, se abre la ventana de configuracion
            if (psvitaUrl.isEmpty() && pspUrl.isEmpty()) {
                configFrame.pack();
                configFrame.setLocationRelativeTo(null);
                configFrame.setResizable(false);
                configFrame.setVisible(true);
                configFrame.loadValues();
            } else {
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setResizable(false);
                frame.setVisible(true);

                // Descargar en segundo plano
                if (!psvitaUrl.isEmpty()) {
                    DownloadThread downloadThread = new DownloadThread(frame, psvitaUrl, utilities.TSV_VITA);
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

                if (!pspUrl.isEmpty()) {
                    DownloadThread downloadThread = new DownloadThread(frame, pspUrl, utilities.TSV_PSP);
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
        private final String url;
        private final String fileName;
        private boolean downloadSuccessful;

        public DownloadThread(Frame frame, String url, String fileName) {
            this.frame = frame;
            this.url = url;
            this.fileName = fileName;
            this.downloadSuccessful = false;
        }

        @Override
        public void run() {
            // Descargar el archivo en segundo plano
            frame.downloadFileInBackground(url, fileName, null, null, null);
            // Marcar la descarga como exitosa si no se lanzan excepciones
            this.downloadSuccessful = true;
        }

        public boolean isDownloadSuccessful() {
            return downloadSuccessful;
        }
    }

}