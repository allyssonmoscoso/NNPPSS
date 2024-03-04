package com.squarepeace.nnppss;
import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

public class Utilities{
    
    private final String URL_FILE = "url.txt"; // Archivo para almacenar la URL
    public String TSV = "db/PSV_GAMES.tsv";
    
    
    public DefaultTableModel readTSV() throws FileNotFoundException, IOException {
        DefaultTableModel model = new DefaultTableModel();

        // Leer el archivo TSV
        BufferedReader TSVFile = new BufferedReader(new FileReader(TSV));
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
    
    
    public String getVitaGamesURL() throws IOException {
        File file = new File(URL_FILE);
        
        // Verificar si el archivo existe
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                return reader.readLine(); // Devolver la URL almacenada en el archivo
            }
        }
        
        return null; // Si el archivo no existe o está vacío
    }

    public void saveVitaGamesURL(String url) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(URL_FILE))) {
            writer.write(url); // Escribir la URL en el archivo
        }
    }
}
