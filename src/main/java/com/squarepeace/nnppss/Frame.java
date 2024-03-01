/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.squarepeace.nnppss;

import com.squarepeace.nnppss.Utilities;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
/**
 *
 * @author allysson
 */
public class Frame extends javax.swing.JFrame {

    /**
     * Creates new form Frame
     */
     private DefaultTableModel originalModel; // Variable para almacenar el modelo de datos original
     
    // Declaración del TableRowSorter
    private TableRowSorter<DefaultTableModel> rowSorter;
    
    private final Utilities utilities; // Utilizamos una variable final para guardar la instancia de Utilities

    public Frame(Utilities utilities) { // Modifica el constructor para aceptar una instancia de Utilities
        this.utilities = utilities; // Asigna la instancia recibida a la variable de clase
        initComponents(); // Asegúrate de llamar al constructor de la superclase si es necesario
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jbsearch = new javax.swing.JLabel();
        jbRefresh = new javax.swing.JButton();
        jtfSearch = new javax.swing.JTextField();
        jcbRegion = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtData = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jpbDownload = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("NNPPSS");

        jPanel1.setName("NNPPSS"); // NOI18N

        jbsearch.setText("Search:");

        jbRefresh.setText("Refresh");
        jbRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbRefreshActionPerformed(evt);
            }
        });

        jtfSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtfSearchKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jtfSearchKeyReleased(evt);
            }
        });

        jcbRegion.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jcbRegionItemStateChanged(evt);
            }
        });

        jtData.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jtDataMousePressed(evt);
            }
        });
        jScrollPane1.setViewportView(jtData);

        jLabel1.setText("Region: ");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(234, 234, 234)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jcbRegion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(62, 62, 62)
                .addComponent(jbsearch)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jtfSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(31, 31, 31)
                .addComponent(jbRefresh)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1231, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jcbRegion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbsearch)
                    .addComponent(jtfSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbRefresh)
                    .addComponent(jLabel1))
                .addGap(26, 26, 26)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 552, Short.MAX_VALUE)
                .addContainerGap())
        );

        jpbDownload.setStringPainted(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 3, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGap(280, 280, 280)
                .addComponent(jpbDownload, javax.swing.GroupLayout.PREFERRED_SIZE, 561, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpbDownload, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jbRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbRefreshActionPerformed
        
         // Descargar el archivo TSV si es necesario
        downloadFileInBackground(utilities.URL_vita_games, utilities.TSV);
    
    }//GEN-LAST:event_jbRefreshActionPerformed
     
    private void jtfSearchKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtfSearchKeyPressed
        String searchText = jtfSearch.getText().trim();
        filtrarTablaPorTextoYRegion(searchText, (String) jcbRegion.getSelectedItem()); 
    }//GEN-LAST:event_jtfSearchKeyPressed

    private void jcbRegionItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jcbRegionItemStateChanged
        
        String selectedRegion = (String) jcbRegion.getSelectedItem();
        System.out.println("Selected Region: " + selectedRegion); // Mensaje de depuración
        if (selectedRegion != null) {
            filtrarTablaPorTextoYRegion(jtfSearch.getText().trim(), selectedRegion);
        }
    }//GEN-LAST:event_jcbRegionItemStateChanged

    private void jtfSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtfSearchKeyReleased
        
      String searchText = jtfSearch.getText().trim();
      filtrarTablaPorTextoYRegion(searchText, (String) jcbRegion.getSelectedItem());
        
    }//GEN-LAST:event_jtfSearchKeyReleased

    private void jtDataMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jtDataMousePressed
        
        int selectedRow = jtData.getSelectedRow();
        if (selectedRow != -1) { // Verificar si se ha seleccionado una fila válida
            // Obtener el índice de la fila seleccionada en el modelo de la vista
            int modelRowIndex = jtData.convertRowIndexToModel(selectedRow);

            // Obtener el modelo de tabla filtrado a través del TableRowSorter
            DefaultTableModel filteredModel = (DefaultTableModel) jtData.getModel();

            // Obtener el valor de la columna "PKG direct link" en la fila seleccionada
            Object nameValue = filteredModel.getValueAt(modelRowIndex, getColumnIndexByName("Name"));
            // Obtener el valor de la columna "PKG direct link" en la fila seleccionada
            Object fileSizeValue = filteredModel.getValueAt(modelRowIndex, getColumnIndexByName("File Size"));
            // Obtener el valor de la columna "PKG direct link" en la fila seleccionada
            Object pkgDirectLinkValue = filteredModel.getValueAt(modelRowIndex, getColumnIndexByName("PKG direct link"));
            // Obtener el valor de la columna "zRIF" en la fila seleccionada
            Object zRIFValue = filteredModel.getValueAt(modelRowIndex, getColumnIndexByName("zRIF"));

            // Extraer el nombre del archivo de la URL del paquete directo
            String pkgDirectLink = pkgDirectLinkValue.toString();
            String fileName = pkgDirectLink.substring(pkgDirectLink.lastIndexOf("/") + 1);

            System.out.println("PKG direct link: " + pkgDirectLinkValue); // Imprimir el valor de la columna "PKG direct link"
            System.out.println("zRIF: " + zRIFValue); // Imprimir el valor de la columna "zRIF"
            System.out.println("Nombre del archivo: " + fileName);

            // Mostrar el cuadro de diálogo de confirmación para descargar el archivo
            int option = JOptionPane.showConfirmDialog(this,
                    "¿Desea descargar " + nameValue + ", el peso es de " + convertFileSize(fileSizeValue) + "?",
                    "Descargar Archivo",
                    JOptionPane.YES_NO_OPTION);

            // Verificar la opción seleccionada por el usuario
            if (option == JOptionPane.YES_OPTION) {
                // Lógica para descargar el archivo aquí
                downloadFileInBackground(pkgDirectLink, fileName);
            }
        
        }
        
    }//GEN-LAST:event_jtDataMousePressed
    
    public void fillTableAndComboBox(){
    
    // Crear un nuevo modelo de tabla usando los datos del archivo TSV
        try {
            originalModel = utilities.readTSV();
        } catch (IOException e) {
            // Manejar cualquier excepción que pueda ocurrir al leer el archivo TSV
            e.printStackTrace();
            // Si ocurre un error al leer el archivo, salir del método
            return;
        }
        //Carga el modelo a la Jtable
        jtData.setModel(originalModel);

        // Crear el TableRowSorter si no existe
        if (rowSorter == null) {
            rowSorter = new TableRowSorter<>(originalModel);
            jtData.setRowSorter(rowSorter);
        }

        // Obtener el número de columnas
        int regionColumnIndex = jtData.getColumn("Region").getModelIndex();

        // Verificar si la columna de la región existe
        if (regionColumnIndex != -1) {
            // Crear un conjunto para almacenar valores únicos de la columna de la región
            Set<String> regionSet = new HashSet<>();

            // Iterar sobre las filas para obtener los valores únicos de la región
            for (int row = 0; row < originalModel.getRowCount(); row++) {
                // Obtener el valor de la región en la fila actual
                String region = (String) originalModel.getValueAt(row, regionColumnIndex);

                // Agregar el valor al conjunto
                regionSet.add(region);
            }

            // Limpiar el JComboBox
            jcbRegion.removeAllItems();
            // Agregar la opción para mostrar todas las regiones
            jcbRegion.addItem("Todas las regiones");

            // Agregar los elementos únicos al JComboBox
            for (String region : regionSet) {
                jcbRegion.addItem(region);
            }
        } else {
            System.out.println("La columna de la región no existe en la tabla.");
        }
        
    }
    
    private void filtrarTablaPorTextoYRegion(String searchText, String region) {
    // Crear un RowFilter para filtrar por el texto ingresado y la región seleccionada
    RowFilter<DefaultTableModel, Integer> rowFilterByText = null;
    RowFilter<DefaultTableModel, Integer> rowFilterByRegion = null;
    try {
        // Filtrar por texto ingresado
        rowFilterByText = RowFilter.regexFilter("(?i)" + searchText); // Ignore case

        // Filtrar por región seleccionada si no se selecciona "Todas las regiones"
        if (!region.equals("Todas las regiones")) {
            rowFilterByRegion = RowFilter.regexFilter("(?i)" + region, getColumnIndexByName("Region"));
        }

        // Combinar los filtros
        List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();
        if (rowFilterByText != null) filters.add(rowFilterByText);
        if (rowFilterByRegion != null) filters.add(rowFilterByRegion);

        RowFilter<DefaultTableModel, Integer> combinedRowFilter = RowFilter.andFilter(filters);

        // Establecer el RowFilter en el TableRowSorter
        rowSorter.setRowFilter(combinedRowFilter);
    } catch (java.util.regex.PatternSyntaxException e) {
        // Si hay un error en la expresión regular, simplemente no aplicamos ningún filtro
        rowSorter.setRowFilter(null);
        return;
    }
}
    
    private int getColumnIndexByName(String columnName) {
    for (int i = 0; i < originalModel.getColumnCount(); i++) {
        if (originalModel.getColumnName(i).equals(columnName)) {
            return i;
        }
    }
    return -1; // Si no se encuentra la columna, retornar -1
}
    
    public void downloadFileInBackground(String fileURL, String localFilePath) {
        // Hilo de descarga para no bloquear la interfaz de usuario
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Verificar si el archivo ya existe
                File file = new File(localFilePath);
                if (file.exists()) {
                    JOptionPane.showMessageDialog(Frame.this, "El archivo ya existe. No es necesario descargarlo nuevamente.");                 
                    return null; // Salir si el archivo ya existe
                }

                try (BufferedInputStream in = new BufferedInputStream(new URL(fileURL).openStream());
                     FileOutputStream fileOutputStream = new FileOutputStream(localFilePath)) {
                    byte dataBuffer[] = new byte[1024];
                    int bytesRead;
                    long fileSize = getFileSize(fileURL);
                    long bytesDownloaded = 0;

                    // Leer y escribir datos del archivo
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                        bytesDownloaded += bytesRead;

                        // Calcular y publicar el progreso
                        int progress = (int) (bytesDownloaded * 100 / fileSize);
                        publish(progress);
                    }
                } catch (IOException e) {
                    // Manejar la excepción
                    e.printStackTrace(); // Imprimir la traza de la excepción para depuración
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                // Actualizar la barra de progreso con el último valor publicado
                int progress = chunks.get(chunks.size() - 1);
                jpbDownload.setValue(progress);
            }

            @Override
            protected void done() {
                if (localFilePath.equals("PSV_GAMES.tsv")) {
                    JOptionPane.showMessageDialog(Frame.this, "Base de datos cargada");
                    fillTableAndComboBox();
                }else{
                
                // Notificar al usuario que la descarga ha finalizado
                JOptionPane.showMessageDialog(Frame.this, "Descarga completada.");
                fillTableAndComboBox();
                }
            }
        };

        // Ejecutar el SwingWorker
        worker.execute();
    }

    // Método para obtener el tamaño del archivo remoto
    private long getFileSize(String fileURL) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        return conn.getContentLengthLong();
    }
    
    // Método para convertir el tamaño del archivo de bytes a MiB
private String convertFileSize(Object fileSizeValue) {
    if (fileSizeValue != null) {
        long fileSize = Long.parseLong(fileSizeValue.toString());
        double fileSizeMiB = fileSize / (1024 * 1024.0); // Convertir bytes a MiB
        return String.format("%.1f MiB", fileSizeMiB);
    } else {
        return "desconocido"; // Si el valor del tamaño del archivo es nulo
    }
}

    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton jbRefresh;
    private javax.swing.JLabel jbsearch;
    private javax.swing.JComboBox<String> jcbRegion;
    private javax.swing.JProgressBar jpbDownload;
    private javax.swing.JTable jtData;
    private javax.swing.JTextField jtfSearch;
    // End of variables declaration//GEN-END:variables
}
