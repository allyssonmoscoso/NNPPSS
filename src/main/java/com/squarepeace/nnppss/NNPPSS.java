/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.squarepeace.nnppss;

import java.io.IOException;
import javax.swing.JOptionPane;


public class NNPPSS {
    
    
    public static void main(String[] args) throws IOException {
        
        Utilities utilities = new Utilities();
        
        // url database
        String url = utilities.getVitaGamesURL();
        
        Frame frame = new Frame(utilities);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

         // Si la URL es null o vacía, pedir al usuario que la ingrese
        if (url == null || url.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Welcome to the app! You need to provide database URL.");
            url = JOptionPane.showInputDialog(null, "Enter the tsv file URL:");
            
            // Guardar la URL en el archivo externo
            if (url != null && !url.isEmpty()) {
                utilities.saveVitaGamesURL(url);
            } else {
                JOptionPane.showMessageDialog(null, "URL cannot be empty. Exiting...");
                System.exit(0);
            }
        }
        
        // Descargar en segundo plano
        DownloadThread downloadThread = new DownloadThread(frame, url, utilities.TSV);
        downloadThread.start();

        try {
        downloadThread.join();
        // Verificar si la descarga se completó con éxito antes de llenar la tabla y el combobox
        if (downloadThread.isDownloadSuccessful()) {
            // Llenar la tabla y el combobox después de la descarga
            //frame.fillTableAndComboBox();
        } else {
            // Imprimir un mensaje de error si la descarga falló
            JOptionPane.showMessageDialog(frame, "Download failed. Unable to populate table and combobox.");
        }
    } catch (InterruptedException e) {
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
        frame.downloadFileInBackground(url, fileName, null, null);
        // Marcar la descarga como exitosa si no se lanzan excepciones
        this.downloadSuccessful = true;
    }

    public boolean isDownloadSuccessful() {
        return downloadSuccessful;
    }
}
    
}