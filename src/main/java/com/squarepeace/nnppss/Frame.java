/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.squarepeace.nnppss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.model.Game;
import com.squarepeace.nnppss.model.DownloadState;
import com.squarepeace.nnppss.service.ConfigManager;
import com.squarepeace.nnppss.service.DownloadService;
import com.squarepeace.nnppss.service.DownloadStateManager;
import com.squarepeace.nnppss.service.GameRepository;
import com.squarepeace.nnppss.service.PackageService;
import com.squarepeace.nnppss.service.SystemValidator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.LineBorder;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class Frame extends javax.swing.JFrame implements ActionListener {
    private static final Logger log = LoggerFactory.getLogger(Frame.class);

    private DefaultTableModel originalModel;
    private TableRowSorter<DefaultTableModel> rowSorter;

    private final ConfigManager configManager;
    private final GameRepository gameRepository;
    private final DownloadService downloadService;
    private final PackageService packageService;
    private final DownloadStateManager downloadStateManager;
    private final SystemValidator systemValidator;

    private boolean downloadPaused = false; // deprecated global; kept for compatibility, not used
    private boolean downloading = false;
    private long lastStateSaveMs = 0;
    private static final long STATE_SAVE_INTERVAL_MS = 10_000; // 10 seconds

    private List<Game> downloadList = new ArrayList<>();
    private final Set<String> selectedUrls = new LinkedHashSet<>();
    private final Map<String, Color> originalBarColorByUrl = new LinkedHashMap<>();
    private List<String> downloadOrderUrls = new ArrayList<>();
    private int lastAnchorIndex = -1;
    private String lastAnchorUrl = null;

    private final Map<String, javax.swing.JProgressBar> progressBarsByUrl = new LinkedHashMap<>();
    private JPanel downloadsPanel;
    private JScrollPane downloadsScroll;
    private int activeDownloadCount = 0;
    // Tracking for speed calculation per download
    private final Map<String, Long> lastBytesByUrl = new LinkedHashMap<>();
    private final Map<String, Long> lastTimeByUrl = new LinkedHashMap<>();
    private final Map<String, Long> downloadStartTimeByUrl = new LinkedHashMap<>();
    // Smoothing + throttle maps
    private final Map<String, Double> smoothedSpeedMibByUrl = new LinkedHashMap<>();
    private final Map<String, Long> lastUiUpdateMsByUrl = new LinkedHashMap<>();
    private final Map<String, Integer> lastProgressPercentByUrl = new LinkedHashMap<>();

    public Frame(ConfigManager configManager, GameRepository gameRepository, DownloadService downloadService,
                 PackageService packageService, DownloadStateManager downloadStateManager) {
        this.configManager = configManager;
        this.gameRepository = gameRepository;
        this.downloadService = downloadService;
        this.packageService = packageService;
        this.downloadStateManager = downloadStateManager;
        this.systemValidator = new SystemValidator(configManager);
        
        initComponents();
        jbResumeAndPause.setEnabled(false);
        
        // Add shutdown hook to save state on exit
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveDownloadState();
            }
        });

        // Debounced search filtering using DocumentListener + Swing Timer
        final Timer debounceTimer = new Timer(250, e -> {
            filtrarTablaPorTextoYRegion(jtfSearch.getText(), (String) jcbRegion.getSelectedItem());
        });
        debounceTimer.setRepeats(false);
        jtfSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                debounceTimer.restart();
            }
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgConsoles = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jbsearch = new javax.swing.JLabel();
        jbRefresh = new javax.swing.JButton();
        jtfSearch = new javax.swing.JTextField();
        jcbRegion = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtData = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jbSetting = new javax.swing.JButton();
        jrbPsvita = new javax.swing.JRadioButton();
        jrbPsp = new javax.swing.JRadioButton();
        jbDownloadList = new javax.swing.JButton();
        jrbPsx = new javax.swing.JRadioButton();
        jbResumeAndPause = new javax.swing.JButton();
        downloadsPanel = new JPanel();
        downloadsPanel.setLayout(new javax.swing.BoxLayout(downloadsPanel, javax.swing.BoxLayout.Y_AXIS));
        downloadsScroll = new JScrollPane(downloadsPanel);
        downloadsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

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

        // KeyListener removed; using debounced DocumentListener above for filtering.

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

        jLabel2.setText("Console:");

        jbSetting.setText("Setting");
        jbSetting.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSettingActionPerformed(evt);
            }
        });

        bgConsoles.add(jrbPsvita);
        jrbPsvita.setText("Psvita");
        jrbPsvita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jrbPsvitaActionPerformed(evt);
            }
        });

        bgConsoles.add(jrbPsp);
        jrbPsp.setText("Psp");
        jrbPsp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jrbPspActionPerformed(evt);
            }
        });

        jbDownloadList.setText("Download list");
        jbDownloadList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbDownloadListActionPerformed(evt);
            }
        });

        bgConsoles.add(jrbPsx);
        jrbPsx.setText("Psx");
        jrbPsx.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jrbPsxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jrbPsvita)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jrbPsp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jrbPsx)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jbDownloadList, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(jcbRegion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jbsearch)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jtfSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(31, 31, 31)
                .addComponent(jbRefresh)
                .addGap(18, 18, 18)
                .addComponent(jbSetting)
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1179, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jbSetting)
                    .addComponent(jrbPsvita)
                    .addComponent(jrbPsp)
                    .addComponent(jbDownloadList)
                    .addComponent(jrbPsx))
                .addGap(26, 26, 26)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 552, Short.MAX_VALUE)
                .addContainerGap())
        );

        jbResumeAndPause.setText("Pause");
        jbResumeAndPause.setActionCommand(":p");
        jbResumeAndPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbResumeAndPauseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(downloadsScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 1000, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jbResumeAndPause)
                .addContainerGap(40, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(downloadsScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jbResumeAndPause)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

        private void jtDataMousePressed(java.awt.event.MouseEvent evt) {
        int selectedRow = jtData.getSelectedRow();
        if (selectedRow != -1) {
            int modelRowIndex = jtData.convertRowIndexToModel(selectedRow);
            DefaultTableModel model = (DefaultTableModel) jtData.getModel();

            String pkgUrl = (String) model.getValueAt(modelRowIndex, getColumnIndexByName("PKG direct link"));
            
            if ("MISSING".equals(pkgUrl)) {
                JOptionPane.showMessageDialog(this, "You cannot download this game because the download link is not registered.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if ("CART ONLY".equals(pkgUrl)) {
                JOptionPane.showMessageDialog(this, "This game is available in cart only.", "Information", JOptionPane.INFORMATION_MESSAGE);
                return;
            } else if ("NOT REQUIRED".equals(pkgUrl)) {
                JOptionPane.showMessageDialog(this, "No download required", "Information", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String name = (String) model.getValueAt(modelRowIndex, getColumnIndexByName("Name"));
            String fileSize = (String) model.getValueAt(modelRowIndex, getColumnIndexByName("File Size"));
            String zRif = (String) model.getValueAt(modelRowIndex, getColumnIndexByName("zRIF"));
            String region = (String) model.getValueAt(modelRowIndex, getColumnIndexByName("Region"));

            int option = JOptionPane.showConfirmDialog(this,
                    "Do you want to add " + name + " (" + fileSize + ") to download list?",
                    "Download List",
                    JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION) {
                if (downloadList.stream().anyMatch(g -> g.getPkgUrl().equals(pkgUrl))) {
                    JOptionPane.showMessageDialog(this, "The game is already in the download list.");
                    return;
                }

                Game game = new Game();
                game.setTitle(name);
                game.setPkgUrl(pkgUrl);
                game.setzRif(zRif);
                game.setRegion(region);
                
                if (jrbPsp.isSelected()) game.setConsole(Console.PSP);
                else if (jrbPsvita.isSelected()) game.setConsole(Console.PSVITA);
                else if (jrbPsx.isSelected()) game.setConsole(Console.PSX);

                downloadList.add(game);
                System.out.println("Added to list: " + game);
            }
        }
    }

    private void jbDownloadListActionPerformed(java.awt.event.ActionEvent evt) {
        JTable table = new JTable();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addColumn("Name");
        model.addColumn("Region");
        model.addColumn("Console");
        model.addColumn("URL");

        for (Game game : downloadList) {
            model.addRow(new Object[]{game.getTitle(), game.getRegion(), game.getConsole(), game.getPkgUrl()});
        }

        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int modelRow = table.convertRowIndexToModel(selectedRows[i]);
                String url = (String) model.getValueAt(modelRow, 3);
                downloadList.removeIf(g -> g.getPkgUrl().equals(url));
                model.removeRow(modelRow);
            }
        });

        JButton clearButton = new JButton("Clear List");
        clearButton.addActionListener(e -> {
            downloadList.clear();
            model.setRowCount(0);
        });

        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(this);

        JOptionPane.showMessageDialog(null, new Object[]{new JScrollPane(table), downloadButton, removeButton, clearButton}, "Download List", JOptionPane.PLAIN_MESSAGE);
    }// GEN-LAST:event_jtDataMousePressed

    private void jbResumeAndPauseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jbResumeAndPauseActionPerformed
        Set<String> targets = selectedUrls.isEmpty() ? new LinkedHashSet<>(progressBarsByUrl.keySet()) : new LinkedHashSet<>(selectedUrls);
        if (targets.isEmpty()) return;
        boolean anyPaused = false;
        for (String url : targets) { if (downloadService.isPaused(url)) { anyPaused = true; break; } }
        if (anyPaused) {
            for (String url : targets) { if (downloadService.isPaused(url)) downloadService.resumeUrl(url); }
        } else {
            for (String url : targets) { downloadService.pauseUrl(url); }
        }
        refreshPauseVisualsPerBar();
        updatePauseButtonText();
        saveDownloadState(); // Save state after pause/resume
    }// GEN-LAST:event_jbResumeAndPauseActionPerformed

    private void jbSettingActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jbSettingActionPerformed
        Config config = new Config(configManager);
        config.setLocationRelativeTo(null);
        config.setResizable(false);
        config.setVisible(true);

        // metodo para cargar los valores de las url en los campos de texto desde el
        // archivo config.properties
        config.loadValues();

    }// GEN-LAST:event_jbSettingActionPerformed

    private void jrbPspActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jrbPspActionPerformed
        fillTable();
    }// GEN-LAST:event_jrbPspActionPerformed

    // si el usuario selecciona la consola psp, se cargan los datos de la consola
    // psp, si selecciona psvita, se cargan los datos de psvita por medio de los
    // metodos fillTableAndComboBoxPsp y fillTableAndComboBoxVita usando
    // JradioButton para seleccionar la consola "jrbPsp" y "jrbPsvita

    public void fillTable() {
        if (jrbPsp.isSelected()) {
            fillTableAndComboBox("Psp");
        } else if (jrbPsvita.isSelected()) {
            fillTableAndComboBox("Psvita");
        } else if (jrbPsx.isSelected()) {
            fillTableAndComboBox("Psx");
        }
    }

    public void fillTableAndComboBox(String consoleName) {
        Console console;
        try {
            console = Console.fromDisplayName(consoleName);
        } catch (IllegalArgumentException e) {
            log.error("Invalid console name: {}", consoleName, e);
            return;
        }

        List<Game> games;
        try {
            games = gameRepository.loadGames(console);
        } catch (Exception e) {
            log.error("Error loading games for console: {}", consoleName, e);
            return;
        }

        // Define columns expected by the UI
        String[] columnNames = {"Name", "Region", "PKG direct link", "zRIF", "File Size"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        for (Game game : games) {
            model.addRow(new Object[]{
                    game.getTitle(),
                    game.getRegion(),
                    game.getPkgUrl(),
                    game.getzRif(),
                    convertFileSize(game.getFileSize())
            });
        }

        originalModel = model;
        jtData.setModel(model);

        // Create the TableRowSorter
        rowSorter = new TableRowSorter<>(model);
        jtData.setRowSorter(rowSorter);

        // Update Region ComboBox
        int regionColumnIndex = 1; // "Region" is at index 1
        java.util.Set<String> regionSet = new java.util.TreeSet<>();
        for (int row = 0; row < model.getRowCount(); row++) {
            String region = (String) model.getValueAt(row, regionColumnIndex);
            if (region != null) {
                regionSet.add(region);
            }
        }

        jcbRegion.removeAllItems();
        jcbRegion.addItem("All regions");
        for (String region : regionSet) {
            jcbRegion.addItem(region);
        }
        jcbRegion.setSelectedIndex(0);
    }

    private String convertFileSize(long fileSize) {
        double fileSizeMiB = fileSize / (1024 * 1024.0);
        return String.format("%.1f MiB", fileSizeMiB);
    }

    private void filtrarTablaPorTextoYRegion(String searchText, String region) {
        System.out.println("Search Text: " + searchText);
        System.out.println("Region: " + region);
        
        if (region == null) {
            return;
        }

        // Crear un RowFilter para filtrar por el texto ingresado y la región
        // seleccionada
        RowFilter<DefaultTableModel, Integer> rowFilterByText = null;
        RowFilter<DefaultTableModel, Integer> rowFilterByRegion = null;
        try {
            // Filtrar por texto ingresado
            rowFilterByText = RowFilter.regexFilter("(?i)" + searchText); // Ignore case
            System.out.println("Row Filter By Text: " + rowFilterByText);
            // Filtrar por región seleccionada si no se selecciona "Todas las regiones"
            if (!region.equals("All regions")) {
                rowFilterByRegion = RowFilter.regexFilter("(?i)" + region, getColumnIndexByName("Region"));
                System.out.println("Row Filter By Region: " + rowFilterByRegion);
            }

            // Combinar los filtros
            List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();
            if (rowFilterByText != null)
                filters.add(rowFilterByText);
            if (rowFilterByRegion != null)
                filters.add(rowFilterByRegion);

            RowFilter<DefaultTableModel, Integer> combinedRowFilter = RowFilter.andFilter(filters);

            // Establecer el RowFilter en el TableRowSorter
            rowSorter.setRowFilter(combinedRowFilter);
        } catch (java.util.regex.PatternSyntaxException e) {
            // Si hay un error en la expresión regular, simplemente no aplicamos ningún filtro
            log.debug("Invalid regex pattern in filter", e);
            rowSorter.setRowFilter(null);
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Download")) {
            startDownloads();
        }
    }

    private void startDownloads() {
        if (downloadList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The download list is empty.");
            return;
        }

        if (downloading) {
            JOptionPane.showMessageDialog(this, "Downloads in progress.");
            return;
        }

        // Validate disk space before starting downloads
        long totalSize = downloadList.stream().mapToLong(Game::getFileSize).sum();
        SystemValidator.ValidationResult validationResult = systemValidator.validateTotalDiskSpace(totalSize, "games");
        if (!validationResult.isValid()) {
            JOptionPane.showMessageDialog(this, validationResult.getMessage(), 
                "Insufficient Disk Space", JOptionPane.WARNING_MESSAGE);
            return;
        }

        downloading = true;
        jbResumeAndPause.setEnabled(true);

        resetDownloadUI(downloadList);

        final int concurrency = Math.max(1, configManager.getSimultaneousDownloads());
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);

        for (Game game : new ArrayList<>(downloadList)) {
            executor.submit(() -> {
                String fileName = game.getFileName();
                String destPath = "games/" + fileName;
                downloadService.downloadFile(game.getPkgUrl(), destPath, new DownloadService.DownloadListener() {
                    @Override
                    public void onProgress(long bytesDownloaded, long totalBytes) {
                        SwingUtilities.invokeLater(() -> updateDownloadProgress(game, bytesDownloaded, totalBytes));
                    }
                    @Override
                    public void onComplete(File file) {
                        SwingUtilities.invokeLater(() -> {
                            markDownloadCompleted(game);
                            // PSVita needs zRif for extraction, PSP/PSX don't
                            if (game.getConsole() == Console.PSVITA) {
                                if (game.getzRif() != null && !game.getzRif().isEmpty()) {
                                    packageService.extractPackage(fileName, game.getzRif(), game.getConsole());
                                }
                            } else {
                                // PSP and PSX always extract (no zRif needed)
                                packageService.extractPackage(fileName, null, game.getConsole());
                            }
                        });
                    }
                    @Override
                    public void onCancelled() {
                        SwingUtilities.invokeLater(() -> markDownloadCancelled(game));
                    }
                    @Override
                    public void onError(Exception e) {
                        log.error("Download error for game: {}", game.getTitle(), e);
                        SwingUtilities.invokeLater(() -> markDownloadError(game));
                    }
                });
            });
        }

        new Thread(() -> {
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                log.warn("Download executor termination interrupted", e);
                Thread.currentThread().interrupt();
            }
            // Final UI cleanup happens as each bar completes.
        }).start();
    }

    private void resetDownloadUI(List<Game> games) {
        SwingUtilities.invokeLater(() -> {
            downloadsPanel.removeAll();
            progressBarsByUrl.clear();
            lastBytesByUrl.clear();
            lastTimeByUrl.clear();
            smoothedSpeedMibByUrl.clear();
            lastUiUpdateMsByUrl.clear();
            lastProgressPercentByUrl.clear();
            originalBarColorByUrl.clear();
            downloadOrderUrls = new ArrayList<>(games.size());
            activeDownloadCount = games.size();
            java.util.HashSet<String> newUrls = new java.util.HashSet<>();
            for (Game g : games) {
                javax.swing.JProgressBar bar = new javax.swing.JProgressBar();
                bar.setStringPainted(true);
                bar.setIndeterminate(true);
                bar.setString(g.getTitle() + " – Starting… – " + g.getConsole());
                progressBarsByUrl.put(g.getPkgUrl(), bar);
                lastBytesByUrl.put(g.getPkgUrl(), 0L);
                lastTimeByUrl.put(g.getPkgUrl(), System.currentTimeMillis());
                lastUiUpdateMsByUrl.put(g.getPkgUrl(), 0L);
                lastProgressPercentByUrl.put(g.getPkgUrl(), -1);
                originalBarColorByUrl.put(g.getPkgUrl(), bar.getForeground());
                bar.putClientProperty("url", g.getPkgUrl());
                newUrls.add(g.getPkgUrl());
                downloadOrderUrls.add(g.getPkgUrl());
                bar.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        javax.swing.JProgressBar source = (javax.swing.JProgressBar) e.getSource();
                        Object prop = source.getClientProperty("url");
                        if (!(prop instanceof String)) return;
                        String u = (String) prop;

                        if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                            showContextMenu(source, e.getX(), e.getY());
                            return;
                        }
                        if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;

                        int idx = downloadOrderUrls.indexOf(u);
                        boolean ctrl = e.isControlDown() || e.isMetaDown();
                        boolean shift = e.isShiftDown();

                        if (shift && lastAnchorIndex >= 0 && idx >= 0) {
                            int start = Math.min(lastAnchorIndex, idx);
                            int end = Math.max(lastAnchorIndex, idx);
                            if (!ctrl) selectedUrls.clear();
                            for (int i = start; i <= end; i++) selectedUrls.add(downloadOrderUrls.get(i));
                        } else if (ctrl) {
                            if (selectedUrls.contains(u)) selectedUrls.remove(u); else selectedUrls.add(u);
                            lastAnchorIndex = idx;
                            lastAnchorUrl = u;
                        } else {
                            selectedUrls.clear();
                            selectedUrls.add(u);
                            lastAnchorIndex = idx;
                            lastAnchorUrl = u;
                        }
                        refreshSelectionVisuals();
                        updatePauseButtonText();
                    }
                });
                downloadsPanel.add(bar);
            }
            // Persist selection to only URLs that still exist
            selectedUrls.retainAll(newUrls);
            // Recompute anchor index based on last anchor URL
            if (lastAnchorUrl != null) {
                lastAnchorIndex = downloadOrderUrls.indexOf(lastAnchorUrl);
            } else {
                lastAnchorIndex = -1;
            }
            downloadsPanel.revalidate();
            downloadsPanel.repaint();
            jbResumeAndPause.setEnabled(true);
            refreshPauseVisualsPerBar();
            refreshSelectionVisuals();
            updatePauseButtonText();
        });
    }

    private void showContextMenu(java.awt.Component invoker, int x, int y) {
        boolean hasSelection = !selectedUrls.isEmpty();
        java.util.Set<String> targets = hasSelection ? new LinkedHashSet<>(selectedUrls) : new LinkedHashSet<>(progressBarsByUrl.keySet());
        String invokerUrl = null;
        if (invoker instanceof javax.swing.JProgressBar) {
            Object p = ((javax.swing.JProgressBar)invoker).getClientProperty("url");
            if (p instanceof String) invokerUrl = (String) p;
        }
        final String fInvokerUrl = invokerUrl;

        JPopupMenu menu = new JPopupMenu();
        JMenuItem pauseItem = new JMenuItem(hasSelection ? "Pause Selected" : "Pause All");
        JMenuItem continueItem = new JMenuItem(hasSelection ? "Continue Selected" : "Continue All");
        JMenuItem clearSel = new JMenuItem("Clear Selection");
        JMenuItem removeFromSel = new JMenuItem("Remove from Selection");
        JMenuItem cancelItem = new JMenuItem(hasSelection ? "Cancel Selected" : "Cancel All");
        JMenuItem cancelRemoveItem = new JMenuItem(hasSelection ? "Cancel && Remove Selected" : "Cancel && Remove All");

        pauseItem.addActionListener(ae -> {
            for (String url : targets) downloadService.pauseUrl(url);
            refreshPauseVisualsPerBar();
            updatePauseButtonText();
        });
        continueItem.addActionListener(ae -> {
            for (String url : targets) if (downloadService.isPaused(url)) downloadService.resumeUrl(url);
            refreshPauseVisualsPerBar();
            updatePauseButtonText();
        });
        clearSel.addActionListener(ae -> {
            selectedUrls.clear();
            refreshSelectionVisuals();
            updatePauseButtonText();
        });
        removeFromSel.addActionListener(ae -> {
            if (fInvokerUrl != null) {
                selectedUrls.remove(fInvokerUrl);
                refreshSelectionVisuals();
                updatePauseButtonText();
            }
        });
        cancelItem.addActionListener(ae -> cancelDownloads(targets, false, true));
        cancelRemoveItem.addActionListener(ae -> cancelDownloads(targets, true, true));

        menu.add(pauseItem);
        menu.add(continueItem);
        menu.addSeparator();
        menu.add(cancelItem);
        menu.add(cancelRemoveItem);
        menu.addSeparator();
        if (fInvokerUrl != null) menu.add(removeFromSel);
        menu.add(clearSel);
        menu.show(invoker, x, y);
    }

    private void cancelDownloads(java.util.Set<String> targets, boolean removeBars, boolean confirmAlways) {
        if (targets == null || targets.isEmpty()) return;
        if (confirmAlways || targets.size() > 1) {
            String action = removeBars ? "cancel and remove" : "cancel";
            int r = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + action + " " + targets.size() + " download(s)?",
                "Confirm Cancel",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }
        for (String url : new java.util.ArrayList<>(targets)) {
            downloadService.cancelUrl(url);
            if (removeBars) {
                javax.swing.JProgressBar bar = progressBarsByUrl.remove(url);
                if (bar != null) {
                    downloadsPanel.remove(bar);
                }
                originalBarColorByUrl.remove(url);
                lastBytesByUrl.remove(url);
                lastTimeByUrl.remove(url);
                smoothedSpeedMibByUrl.remove(url);
                lastUiUpdateMsByUrl.remove(url);
                lastProgressPercentByUrl.remove(url);
                selectedUrls.remove(url);
                downloadOrderUrls.remove(url);
            }
        }
        downloadsPanel.revalidate();
        downloadsPanel.repaint();
        refreshSelectionVisuals();
        refreshPauseVisualsPerBar();
        updatePauseButtonText();
    }

    private void updateDownloadProgress(Game game, long bytesDownloaded, long totalBytes) {
        javax.swing.JProgressBar bar = progressBarsByUrl.get(game.getPkgUrl());
        if (bar == null) return;
        long now = System.currentTimeMillis();
        
        // Initialize start time if first call
        downloadStartTimeByUrl.putIfAbsent(game.getPkgUrl(), now);
        
        long lastTime = lastTimeByUrl.getOrDefault(game.getPkgUrl(), now);
        long lastBytes = lastBytesByUrl.getOrDefault(game.getPkgUrl(), 0L);
        long deltaTime = now - lastTime; // ms
        long deltaBytes = bytesDownloaded - lastBytes;
        lastTimeByUrl.put(game.getPkgUrl(), now);
        lastBytesByUrl.put(game.getPkgUrl(), bytesDownloaded);

        // Instant speed (MiB/s)
        Double smoothed = smoothedSpeedMibByUrl.get(game.getPkgUrl());
        double instantMib = -1.0;
        if (deltaTime > 150 && deltaBytes > 0) { // require minimum interval
            double bytesPerSec = (deltaBytes / (deltaTime / 1000.0));
            instantMib = bytesPerSec / (1024 * 1024.0);
            if (smoothed == null) {
                smoothed = instantMib; // first measurement
            } else {
                double alpha = 0.25; // smoothing factor
                smoothed = smoothed + alpha * (instantMib - smoothed);
            }
            smoothedSpeedMibByUrl.put(game.getPkgUrl(), smoothed);
        }

        // Determine progress percent (if known)
        int progressPercent = -1;
        if (totalBytes > 0) {
            progressPercent = (int) ((bytesDownloaded * 100) / totalBytes);
            bar.setIndeterminate(false);
            bar.setValue(progressPercent);
        } else {
            bar.setIndeterminate(true);
        }

        // Calculate ETA
        String etaStr = "—";
        if (totalBytes > 0 && smoothed != null && smoothed > 0) {
            long remainingBytes = totalBytes - bytesDownloaded;
            double remainingSeconds = remainingBytes / (smoothed * 1024 * 1024); // MiB/s to bytes/s
            etaStr = formatETA((long) remainingSeconds);
        }

        // Throttle UI updates: update text only if progress changed or 1s elapsed
        long lastUi = lastUiUpdateMsByUrl.getOrDefault(game.getPkgUrl(), 0L);
        int lastProgressStored = lastProgressPercentByUrl.getOrDefault(game.getPkgUrl(), -999);
        boolean progressChanged = progressPercent != lastProgressStored && progressPercent >= 0;
        boolean timeElapsed = (now - lastUi) >= 1000; // 1s throttle
        boolean first = lastUi == 0L;
        if (progressChanged || timeElapsed || first || bar.isIndeterminate()) {
            lastUiUpdateMsByUrl.put(game.getPkgUrl(), now);
            lastProgressPercentByUrl.put(game.getPkgUrl(), progressPercent);
            String speedStr;
            Double current = smoothedSpeedMibByUrl.get(game.getPkgUrl());
            if (current == null || current <= 0) {
                speedStr = "—"; // em dash for unknown
            } else {
                speedStr = String.format("%.2f MiB/s", current);
            }
            String base;
            if (bar.isIndeterminate()) {
                base = String.format("%s – %d bytes – %s – %s", game.getTitle(), bytesDownloaded, game.getConsole(), speedStr);
            } else {
                base = String.format("%s – %d%% – %s – ETA: %s – %s", 
                    game.getTitle(), progressPercent, game.getConsole(), etaStr, speedStr);
            }
            if (downloadService.isPaused(game.getPkgUrl())) base += " (Paused)";
            bar.setString(base);
        }
        
        // Throttled state save (every 10 seconds)
        if (now - lastStateSaveMs >= STATE_SAVE_INTERVAL_MS) {
            saveDownloadState();
            lastStateSaveMs = now;
        }
    }

    private void markDownloadCompleted(Game game) {
        javax.swing.JProgressBar bar = progressBarsByUrl.get(game.getPkgUrl());
        if (bar != null) {
            bar.setIndeterminate(false);
            bar.setValue(100);
            bar.setString(game.getTitle() + " – Completed – " + game.getConsole());
        }
        activeDownloadCount--;
        checkAllDownloadsFinished();
        saveDownloadState(); // Save state after completion
    }

    private void markDownloadError(Game game) {
        javax.swing.JProgressBar bar = progressBarsByUrl.get(game.getPkgUrl());
        if (bar != null) {
            bar.setIndeterminate(false);
            bar.setString(game.getTitle() + " – Error – " + game.getConsole());
        }
        activeDownloadCount--;
        checkAllDownloadsFinished();
        saveDownloadState(); // Save state after error
    }

    private void markDownloadCancelled(Game game) {
        javax.swing.JProgressBar bar = progressBarsByUrl.get(game.getPkgUrl());
        if (bar != null) {
            bar.setIndeterminate(false);
            bar.setString(game.getTitle() + " – Cancelled – " + game.getConsole());
            bar.setForeground(Color.GRAY);
        }
        selectedUrls.remove(game.getPkgUrl());
        refreshSelectionVisuals();
        updatePauseButtonText();
        activeDownloadCount--;
        checkAllDownloadsFinished();
        saveDownloadState(); // Save state after cancellation
    }

    private void checkAllDownloadsFinished() {
        if (activeDownloadCount <= 0) {
            downloading = false;
            jbResumeAndPause.setEnabled(false);
            downloadList.clear();
        }
    }

    private void refreshPauseVisualsPerBar() {
        for (Map.Entry<String, javax.swing.JProgressBar> e : progressBarsByUrl.entrySet()) {
            String url = e.getKey();
            javax.swing.JProgressBar bar = e.getValue();
            String s = bar.getString();
            if (s == null) continue;
            boolean paused = downloadService.isPaused(url);
            if (paused) {
                if (!s.endsWith("(Paused)")) bar.setString(s + " (Paused)");
                bar.setForeground(Color.GRAY);
            } else {
                if (s.endsWith("(Paused)")) bar.setString(s.substring(0, s.length() - 9));
                Color orig = originalBarColorByUrl.get(url);
                if (orig != null) bar.setForeground(orig);
            }
        }
    }

    private void refreshSelectionVisuals() {
        for (Map.Entry<String, javax.swing.JProgressBar> e : progressBarsByUrl.entrySet()) {
            String url = e.getKey();
            javax.swing.JProgressBar bar = e.getValue();
            if (selectedUrls.contains(url)) {
                bar.setBorder(new LineBorder(new Color(0x3B,0x82,0xF6), 2));
            } else {
                bar.setBorder(null);
            }
        }
    }

    private void updatePauseButtonText() {
        Set<String> targets = selectedUrls.isEmpty() ? progressBarsByUrl.keySet() : selectedUrls;
        boolean anyPaused = false;
        for (String url : targets) { if (downloadService.isPaused(url)) { anyPaused = true; break; } }
        jbResumeAndPause.setText(anyPaused ? "Continue" : "Pause");
    }

    /**
     * Format seconds into human-readable ETA
     */
    private String formatETA(long seconds) {
        if (seconds < 0) return "—";
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long mins = seconds / 60;
            long secs = seconds % 60;
            return String.format("%dm %ds", mins, secs);
        } else {
            long hours = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            return String.format("%dh %dm", hours, mins);
        }
    }

    /**
     * Collect current download states for persistence
     */
    private List<DownloadState> getCurrentDownloadStates() {
        List<DownloadState> states = new ArrayList<>();
        
        for (Game game : downloadList) {
            String url = game.getPkgUrl();
            javax.swing.JProgressBar bar = progressBarsByUrl.get(url);
            
            // Skip completed, cancelled, or errored downloads
            if (bar != null) {
                String barText = bar.getString();
                if (barText != null && (barText.contains("Completed") || 
                    barText.contains("Cancelled") || 
                    barText.contains("Error"))) {
                    continue;
                }
            }
            
            DownloadState state = new DownloadState();
            state.setTitle(game.getTitle());
            state.setRegion(game.getRegion());
            state.setConsole(game.getConsole().getDisplayName());
            state.setPkgUrl(url);
            state.setzRif(game.getzRif());
            
            // Progress tracking
            Long bytes = lastBytesByUrl.get(url);
            state.setBytesDownloaded(bytes != null ? bytes : 0L);
            state.setTotalBytes(game.getFileSize());
            
            // File path
            state.setFilePath("games/" + game.getFileName());
            
            // State flags
            boolean paused = downloadService.isPaused(url);
            state.setPaused(paused);
            
            // Determine status
            if (paused) {
                state.setStatus("paused");
            } else if (downloading) {
                state.setStatus("downloading");
            } else {
                state.setStatus("pending");
            }
            
            state.setLastUpdateTimestamp(System.currentTimeMillis());
            
            states.add(state);
        }
        
        return states;
    }

    /**
     * Save current download state to disk
     */
    private void saveDownloadState() {
        try {
            List<DownloadState> states = getCurrentDownloadStates();
            downloadStateManager.saveStates(states);
        } catch (Exception e) {
            log.error("Error saving download state", e);
        }
    }

    /**
     * Restore downloads from saved state
     * Call this after Frame is initialized
     */
    public void restoreDownloads(List<DownloadState> savedStates) {
        if (savedStates == null || savedStates.isEmpty()) {
            return;
        }
        
        // Convert DownloadState → Game objects
        downloadList.clear();
        
        // Store pause states to restore after starting downloads
        Map<String, Boolean> pauseStatesByUrl = new LinkedHashMap<>();
        
        for (DownloadState state : savedStates) {
            Game game = new Game();
            game.setTitle(state.getTitle());
            game.setRegion(state.getRegion());
            game.setConsole(Console.fromDisplayName(state.getConsole()));
            game.setPkgUrl(state.getPkgUrl());
            game.setzRif(state.getzRif());
            game.setFileSize(state.getTotalBytes());
            
            downloadList.add(game);
            pauseStatesByUrl.put(state.getPkgUrl(), state.isPaused());
        }
        
        // Show confirmation dialog to user
        int count = downloadList.size();
        if (count > 0) {
            String message = String.format("Se encontraron %d descarga%s pendiente%s de la sesión anterior.\n¿Deseas reanudarlas ahora?",
                    count, count == 1 ? "" : "s", count == 1 ? "" : "s");
            int option = JOptionPane.showConfirmDialog(this, message, "Restaurar Descargas", 
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            
            if (option == JOptionPane.YES_OPTION) {
                // Start downloads immediately
                startDownloads();
                
                // Restore pause states after a short delay to allow downloads to initialize
                SwingUtilities.invokeLater(() -> {
                    try {
                        Thread.sleep(500); // Give downloads time to start
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Apply paused states
                    for (Map.Entry<String, Boolean> entry : pauseStatesByUrl.entrySet()) {
                        if (entry.getValue()) {
                            downloadService.pauseUrl(entry.getKey());
                        }
                    }
                    refreshPauseVisualsPerBar();
                    updatePauseButtonText();
                });
            } else {
                // User declined, clear the download list
                downloadList.clear();
                // Also clear the saved state file
                downloadStateManager.clearStates();
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgConsoles;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton jbDownloadList;
    private javax.swing.JButton jbRefresh;
    private javax.swing.JButton jbResumeAndPause;
    private javax.swing.JButton jbSetting;
    private javax.swing.JLabel jbsearch;
    private javax.swing.JComboBox<String> jcbRegion;
    // Removed single progress bar; using downloadsPanel instead.
    private javax.swing.JRadioButton jrbPsp;
    private javax.swing.JRadioButton jrbPsvita;
    private javax.swing.JRadioButton jrbPsx;
    private javax.swing.JTable jtData;
    private javax.swing.JTextField jtfSearch;
    // End of variables declaration//GEN-END:variables

    private void jbRefreshActionPerformed(java.awt.event.ActionEvent evt) {
        fillTable();
    }

    private void jcbRegionItemStateChanged(java.awt.event.ItemEvent evt) {
        filtrarTablaPorTextoYRegion(jtfSearch.getText(), (String) jcbRegion.getSelectedItem());
    }

    private void jrbPsvitaActionPerformed(java.awt.event.ActionEvent evt) {
        fillTable();
    }

    private void jrbPsxActionPerformed(java.awt.event.ActionEvent evt) {
        fillTable();
    }
}
