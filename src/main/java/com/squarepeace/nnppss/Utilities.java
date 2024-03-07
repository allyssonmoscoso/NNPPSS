package com.squarepeace.nnppss;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.table.DefaultTableModel;

public class Utilities {

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

    // Método para obtener el tamaño del archivo remoto
    public long getFileSize(String fileURL) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        return conn.getContentLengthLong();
    }

    // Método para convertir el tamaño del archivo de bytes a MiB
    public String convertFileSize(Object fileSizeValue) {
        if (fileSizeValue != null) {
            long fileSize = Long.parseLong(fileSizeValue.toString());
            double fileSizeMiB = fileSize / (1024 * 1024.0); // Convertir bytes a MiB
            return String.format("%.1f MiB", fileSizeMiB);
        } else {
            return "unknown"; // Si el valor del tamaño del archivo es nulo
        }
    }

    // Método para mover un archivo a una ubicación específica
    public void moveFile(String sourceFilePath, String destinationFilePath) {
        File sourceFile = new File(sourceFilePath);
        File destinationFile = new File(destinationFilePath);
        try {
            Files.move(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Manejar cualquier excepción que pueda ocurrir al mover el archivo
            e.printStackTrace();
        }
    }

    public static String buildCommand(String PKGname, String zRifKey) {
        // Obtener el separador de archivos del sistema
        String fileSeparator = System.getProperty("file.separator");
        // Construir la ruta del comando dependiendo del sistema operativo
        String command = "";
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            command = "lib" + fileSeparator + "pkg2zip.exe -x games" + fileSeparator + PKGname + " " + zRifKey;
        } else if (System.getProperty("os.name").toLowerCase().indexOf("nix") >= 0
                || System.getProperty("os.name").toLowerCase().indexOf("nux") >= 0
                || System.getProperty("os.name").toLowerCase().indexOf("aix") > 0
                || System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {

            // Verificar si el comando está instalado
            if (isCommandInstalled("pkg2zip")) {
                command = "pkg2zip -x games" + fileSeparator + PKGname + " " + zRifKey;
            } else {
                JOptionPane.showMessageDialog(null, "pkg2zip is not installed");
            }

        } else {
            JOptionPane.showMessageDialog(null, "Operating system not supported");
        }
        return command;
    }

    // Método para verificar si un comando está instalado  
    public static boolean isCommandInstalled(String command) {
        boolean installed = false;
        try {
            // Ejecutar el comando 'which' para verificar si está instalado
            Process process = Runtime.getRuntime().exec("which " + command);
            // Leer la salida del proceso
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            // Leer todas las líneas de salida
            while ((line = reader.readLine()) != null) {
                // Si la línea no está vacía, el comando está instalado
                if (!line.isEmpty()) {
                    installed = true;
                    break;
                }
            }
            // Esperar a que el proceso termine
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            // Capturar cualquier excepción que pueda ocurrir durante la verificación
            e.printStackTrace();
        }
        return installed;
    }

    public static void runCommandWithLoadingMessage(String command) {
        try {
            // Verificar si la carpeta "lib" existe
            File libFolder = new File("lib");
            if (!libFolder.exists()) {
                // Si no existe, crearla
                libFolder.mkdir();

                if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
                    // Mostrar mensaje solicitando al usuario que coloque pkg2zip en la carpeta lib
                    JOptionPane.showMessageDialog(null, "Please place pkg2zip.exe in the folder 'lib'.");
                }

            }

            // Crear y mostrar el diálogo de preparación con barra de progreso
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            JDialog dialog = new JDialog();
            dialog.setTitle("Preparing package");
            dialog.add(progressBar);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.setVisible(true);

            // Ejecutar el comando en un hilo separado
            Thread thread = new Thread(() -> {
                try {
                    // Ejecutar el comando
                    Process process = Runtime.getRuntime().exec(command);

                    // Esperar a que el proceso termine
                    int exitCode = process.waitFor();

                    // Verificar si el comando se ejecutó correctamente
                    if (exitCode == 0) {

                        // Cerrar el diálogo de progreso cuando el proceso haya terminado
                        dialog.setVisible(false);
                        // Mostrar mensaje de éxito
                        JOptionPane.showMessageDialog(null, "Ready to install!");
                    } else {
                        // Mostrar mensaje de error si el comando falla
                        JOptionPane.showMessageDialog(null, "Error running pkg2zip");
                    }
                } catch (Exception e) {
                    // Capturar cualquier excepción que pueda ocurrir durante la ejecución del comando
                    JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
                }
            });
            thread.start();
        } catch (Exception e) {
            // Capturar cualquier excepción que pueda ocurrir durante la ejecución del comando
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }
    
     public static String getUrlForConsole(String console) {
        try {
            File file = new File("url.txt");
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                int index = line.indexOf(":");
                if (index != -1) {
                    String consoleFromFile = line.substring(0, index).trim(); // Obtener la parte antes del ":"
                    // Transformar "TSV_VITA" a "VITA" y "TSV_PSP" a "PSP"
                    consoleFromFile = consoleFromFile.substring(4).trim(); // Eliminar los primeros 4 caracteres
                    if (consoleFromFile.equals(console)) { // Verificar si coincide con la consola solicitada
                        return line.substring(index + 1).trim(); // Obtener la parte después del ":"
                    }
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    
}
