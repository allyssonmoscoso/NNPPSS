package com.squarepeace.nnppss;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Utilities {

    //use donwloadPath from config.properties
    public String downloadPath = getProperty("downloadPath");

    public String TSV_VITA = downloadPath + "/PSV_GAMES.tsv";
    public String TSV_PSP =downloadPath + "/PSP_GAMES.tsv";
    public String TSV_PSX =downloadPath + "/PSX_GAMES.tsv";

    public DefaultTableModel readTSV(String TSV) throws FileNotFoundException, IOException {
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
        if (!sourceFile.exists()) {
            System.err.println("Source file does not exist: " + sourceFilePath);
            return;
        }
        try {
            Files.move(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Manejar cualquier excepción que pueda ocurrir al mover el archivo
            e.printStackTrace();
        }
    }

    static String DownloadPath = getProperty("downloadPath");

    // Método para construir el comando para extraer un archivo PKG
    public static String buildCommand(String PKGname, String zRifKey, String Console) {
        // Obtener el separador de archivos del sistema
        String fileSeparator = System.getProperty("file.separator");
        // Construir la ruta del comando dependiendo del sistema operativo
        String command = "";
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {

            if (Console.equals("Psvita")) {
                command =  getPkgDecToolPath() + " -x " + "Temp/" + fileSeparator  + PKGname + " " + zRifKey;
                System.out.println("Comando desde Utilities: "+command);
            }else if (Console.equals("Psp")) {
                command = getPkgDecToolPath() + ""  + PKGname;
            }else if (Console.equals("Psx")) {
                command = getPkgDecToolPath() + "" + PKGname;
            }

            
        } else if (System.getProperty("os.name").toLowerCase().indexOf("nix") >= 0
                || System.getProperty("os.name").toLowerCase().indexOf("nux") >= 0
                || System.getProperty("os.name").toLowerCase().indexOf("aix") > 0
                || System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {

            // Verificar si el comando está instalado
            if (isCommandInstalled("pkg2zip")) {

                if (Console.equals("Psvita")) {
                    command = "pkg2zip -x Temp" + fileSeparator + PKGname + " " + zRifKey;
                }else if (Console.equals("Psp")) {
                    command = "pkg2zip Temp" + fileSeparator + PKGname;
                }else if (Console.equals("Psx")) {
                    command = "pkg2zip Temp" + fileSeparator + PKGname;
                }

                
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
        
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            try {
                // Ejecutar el comando 'where' para verificar si está instalado en Windows
                Process process = Runtime.getRuntime().exec("where " + command);
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
        } else if (System.getProperty("os.name").toLowerCase().indexOf("nix") >= 0
                || System.getProperty("os.name").toLowerCase().indexOf("nux") >= 0
                || System.getProperty("os.name").toLowerCase().indexOf("aix") > 0
                || System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {
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
        }
        return installed;
    }

    public static void runCommandWithLoadingMessage(String command) {
        
        if(getPkgDecToolPath().equals("")){
            JOptionPane.showMessageDialog(null, "Please set the path to pkg2zip in the config file.");
        
        }else{

            try {
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

                        ProcessBuilder pb = new ProcessBuilder(command);
                        pb.directory(new File(DownloadPath));
                        // Ejecutar el comando
                        Process process = pb.start();
    
                        // Esperar a que el proceso termine
                        int exitCode = process.waitFor();
    
                        // Verificar si el comando se ejecutó correctamente
                        if (exitCode == 0) {
    
                            // Cerrar el diálogo de progreso cuando el proceso haya terminado
                            dialog.setVisible(false);
                            // Mostrar mensaje de éxito si el comando se ejecuta correctamente
                            System.out.println("Ready to install!");
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
    }
    
    
        // ...

        

        public String getPSPUrl() {
            return getProperty("psp.url");
        }

        public String getPSVitaUrl() {
            return getProperty("psvita.url");
        }

        public String getPsxUrl() {
            return getProperty("psx.url");
        }

        public String getDownloadPath() {
            return getProperty("downloadPath");
        }

        static public String getPkgDecToolPath() {
            return getProperty("pkgDecToolPath");
        }

        public String getDeletePkgFile() {
            return getProperty("deletePkgFile");
        }

        //Metodo para obtener las propiedades del archivo config.properties
        static public String getProperty(String property) {
            try {
                Properties p = new Properties();
                p.load(new FileInputStream("config.properties"));
                return p.getProperty(property);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        
            
            //Metodo para crear el config.properties si no existe y agregarle las propiedades
            public void createConfigFile() {
                try {
                    File file = new File("config.properties");
            if (!file.exists() || file.length() == 0) {
                file.createNewFile();
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("psp.url=\n");
                    writer.write("psvita.url=\n");
                    writer.write("psx.url=\n");
                    writer.write("simultaneousDownloads=1\n");
                    writer.write("downloadPath=\n");
                    writer.write("pkgDecToolPath=\n");
                    writer.write("deletePkgFile=\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    

    void getUrlsFromPage(){

    // Obtener los urls de la página web https://nopaystation.com usando soup  
    try {
        // Conectar a la página web
        Connection connection = Jsoup.connect("https://nopaystation.com");
        Document document = connection.get();

        // Obtener todos los elementos 'a' de la página
        Elements elements = document.select("a");

        // Crear una lista para almacenar los urls
        ArrayList<String> urls = new ArrayList<>();
        // Iterar sobre los elementos 'a'
        for (Element element : elements) {
            // Obtener el atributo 'href' de cada elemento 'a'
            String url = element.attr("href");
            // Verificar si el url contiene 'http' y no está en la lista
            if (url.contains("tsv") && !urls.contains(url)) {
                // Añadir el url a la lista
                urls.add(url);
            }
        }

        // Imprimir los urls
        // for (String url : urls) {
        //    System.out.println(url);
        // }
        
         //remove urls containing "pending" the list
        urls.removeIf(url -> url.contains("pending"));
        //add the urls to the config file adding https://nopaystation.com/ to the beginning of the url  
        try {
            File file = new File("config.properties");
            Properties p = new Properties();
            p.load(new FileInputStream(file));
            for (String url : urls) {
            if (url.contains("PSP_GAMES")) {
                p.setProperty("psp.url", "https://nopaystation.com" + url);
            } else if (url.contains("PSV_GAMES")) {
                p.setProperty("psvita.url", "https://nopaystation.com" + url);
            } else if (url.contains("PSX_GAMES")) {
                p.setProperty("psx.url", "https://nopaystation.com" + url);
            }
            }
            p.store(new FileOutputStream(file), null);
        } catch (IOException e) {
            e.printStackTrace();
        }

    } catch (IOException e) {
        e.printStackTrace();

    }
}

public static void setFrameIcon(JFrame frame, String iconPath) {
        URL iconURL = frame.getClass().getResource(iconPath);
        if (iconURL != null) {
            frame.setIconImage(new ImageIcon(iconURL).getImage());
        } else {
            System.err.println("Icon resource not found: " + iconPath);
        }
    }

    public void downloadFile(String fileURL, String destinationFilePath) throws IOException {
        File destinationFile = new File(destinationFilePath);
        File parentDir = destinationFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs(); // Crear el directorio si no existe
        }
        URL url = new URL(fileURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(destinationFilePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

}