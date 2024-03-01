package com.squarepeace.nnppss;
import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author allysson
 */
public class Utilities{
    
    public String URL_vita_games = "https://nopaystation.com/tsv/PSV_GAMES.tsv";
    public String TSV = "PSV_GAMES.tsv";
    
    
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
}
