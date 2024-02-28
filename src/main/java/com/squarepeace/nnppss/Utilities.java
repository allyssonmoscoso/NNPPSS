package com.squarepeace.nnppss;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author allysson
 */
public class Utilities{
    
    public String URL_vita_games = "https://nopaystation.com/tsv/PSV_GAMES.tsv";
    public String localFilePath = "PSV_GAMES.tsv";
    
    
    public void DownloadTSv() {

        // Verificar si el archivo ya existe
        File file = new File(localFilePath);
        if (file.exists()) {
            System.out.println("El archivo ya existe. No es necesario descargarlo nuevamente.");
            return; // Salir del método si el archivo ya existe
        }

        try (BufferedInputStream in = new BufferedInputStream(new URL(URL_vita_games).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(localFilePath)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            // Manejar la excepción
            e.printStackTrace(); // Imprimir la traza de la excepción para depuración
        }
    }
    
    public DefaultTableModel readTSV() throws FileNotFoundException, IOException {
        DefaultTableModel model = new DefaultTableModel();

        // Leer el archivo TSV
        BufferedReader TSVFile = new BufferedReader(new FileReader(localFilePath));
        String headers = TSVFile.readLine(); // Leer la primera línea como encabezados

        // Dividir los encabezados y añadirlos al modelo de la tabla
        String[] columnNames = headers.split("\t");
        for (String columnName : columnNames) {
            model.addColumn(columnName);
        }

        // Leer las filas de datos
        String dataRow = TSVFile.readLine();
        while (dataRow != null) {
            String[] data = dataRow.split("\t");
            model.addRow(data); // Añadir la fila al modelo de la tabla
            dataRow = TSVFile.readLine(); // Leer la siguiente fila
        }

        // Cerrar el archivo
        TSVFile.close();

        return model;
    } 
}
