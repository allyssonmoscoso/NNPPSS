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
import com.squarepeace.nnppss.model.DownloadHistory;
import com.squarepeace.nnppss.service.ConfigManager;
import com.squarepeace.nnppss.service.DownloadService;
import com.squarepeace.nnppss.service.DownloadStateManager;
import com.squarepeace.nnppss.service.DownloadHistoryManager;
import com.squarepeace.nnppss.service.GameRepository;
import com.squarepeace.nnppss.service.PackageService;
import com.squarepeace.nnppss.service.SystemValidator;
import com.squarepeace.nnppss.ui.NotificationPanel;
import com.squarepeace.nnppss.ui.GlobalProgressPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.LineBorder;
import javax.swing.GroupLayout;
import javax.swing.JLayeredPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.SwingWorker;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import java.awt.event.KeyEvent;
import java.awt.Cursor;

public class Frame extends javax.swing.JFrame implements ActionListener, com.squarepeace.nnppss.service.ConfigListener {
    private static final Logger log = LoggerFactory.getLogger(Frame.class);

    // Color constants for download status
    private static final Color COLOR_COMPLETED = new Color(34, 139, 34);  // Green
    private static final Color COLOR_PAUSED = new Color(255, 165, 0);     // Orange
    private static final Color COLOR_FAILED = new Color(220, 20, 60);     // Red
    private static final Color COLOR_DEFAULT = Color.BLACK;                // Black
    private static final long DOWNLOAD_START_DELAY_MS = 500; // Delay before starting downloads

    private DefaultTableModel originalModel;
    private TableRowSorter<DefaultTableModel> rowSorter;

    private final ConfigManager configManager;
    private final GameRepository gameRepository;
    private final DownloadService downloadService;
    private final PackageService packageService;
    private final DownloadStateManager downloadStateManager;
    private final DownloadHistoryManager historyManager;
    private final SystemValidator systemValidator;
    private final com.squarepeace.nnppss.service.DatabaseManager databaseManager;
    private final com.squarepeace.nnppss.service.DownloadQueueManager queueManager;
    private javax.swing.SwingWorker<java.util.List<Game>, Void> currentLoadWorker;
    private ExecutorService downloadExecutor;

    private boolean downloading = false;
    private long lastStateSaveMs = 0;
    private static final long STATE_SAVE_INTERVAL_MS = 10_000; // 10 seconds

    private final List<Game> downloadList = new CopyOnWriteArrayList<>();
    private final Set<String> selectedUrls = new LinkedHashSet<>();
    private final Map<String, Color> originalBarColorByUrl = new LinkedHashMap<>();
    private List<String> downloadOrderUrls = new ArrayList<>();
    private int lastAnchorIndex = -1;
    private String lastAnchorUrl = null;

    private final Map<String, javax.swing.JProgressBar> progressBarsByUrl = new LinkedHashMap<>();
    private JPanel downloadsPanel;
    private JScrollPane downloadsScroll;
    private NotificationPanel notificationPanel;
    private GlobalProgressPanel globalProgressPanel;
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
                 PackageService packageService, DownloadStateManager downloadStateManager, 
                 com.squarepeace.nnppss.service.DatabaseManager databaseManager) {
        this.configManager = configManager;
        this.gameRepository = gameRepository;
        this.downloadService = downloadService;
        this.packageService = packageService;
        this.downloadStateManager = downloadStateManager;
        this.historyManager = new DownloadHistoryManager();
        this.systemValidator = new SystemValidator(configManager);
        this.databaseManager = databaseManager;
        this.queueManager = new com.squarepeace.nnppss.service.DownloadQueueManager();
        
        initComponents();
        setupDatabaseManager();
        setupNotificationAndProgressPanels();
        jbResumeAndPause.setEnabled(false);
        
        // Update Retry Failed button state based on history
        updateRetryFailedButton();
        
        // Restore download queue from previous session
        restoreDownloadQueue();
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (downloading) {
                    // Save in-progress downloads state
                    saveDownloadState();
                } else {
                    // Save pending queue only if not downloading
                    saveDownloadQueue();
                    // Clear any old download state
                    downloadStateManager.clearStates();
                }
                // Shutdown executor if running
                shutdownDownloadExecutor();
                System.exit(0);
            }
        });
        
        // Setup keyboard shortcuts
        setupKeyboardShortcuts();

        // Debounced search filtering using DocumentListener + Swing Timer
        final Timer debounceTimer = new Timer(250, e -> {
            applyFilters();
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
        jcbFileSize = new javax.swing.JComboBox<>();
        jlFileSize = new javax.swing.JLabel();
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
        jbRetryFailed = new javax.swing.JButton();
        downloadsPanel = new JPanel();
        downloadsPanel.setLayout(new javax.swing.BoxLayout(downloadsPanel, javax.swing.BoxLayout.Y_AXIS));
        downloadsScroll = new JScrollPane(downloadsPanel);
        downloadsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
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

        jlFileSize.setText("File Size:");
        
        jcbFileSize.addItem("All sizes");
        jcbFileSize.addItem("< 1GB");
        jcbFileSize.addItem("1-5GB");
        jcbFileSize.addItem("> 5GB");
        jcbFileSize.setSelectedIndex(0);
        jcbFileSize.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jcbFileSizeItemStateChanged(evt);
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
                .addComponent(jlFileSize)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jcbFileSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(jlFileSize)
                    .addComponent(jcbFileSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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

        jbRetryFailed.setText("Retry Failed");
        jbRetryFailed.setToolTipText("Retry downloads that previously failed");
        jbRetryFailed.setEnabled(false);
        jbRetryFailed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbRetryFailedActionPerformed(evt);
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jbResumeAndPause, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jbRetryFailed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jbRetryFailed)
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
                showNotification("Download link not registered for this game", NotificationPanel.NotificationType.ERROR);
                return;
            } else if ("CART ONLY".equals(pkgUrl)) {
                showNotification("This game is cart only", NotificationPanel.NotificationType.INFO);
                return;
            } else if ("NOT REQUIRED".equals(pkgUrl)) {
                showNotification("No download required", NotificationPanel.NotificationType.INFO);
                return;
            }

            String name = (String) model.getValueAt(modelRowIndex, getColumnIndexByName("Name"));
            String fileSize = (String) model.getValueAt(modelRowIndex, getColumnIndexByName("File Size"));
            String zRif = (String) model.getValueAt(modelRowIndex, getColumnIndexByName("zRIF"));
            String region = (String) model.getValueAt(modelRowIndex, getColumnIndexByName("Region"));
            String contentId = (String) model.getValueAt(modelRowIndex, getColumnIndexByName("Content ID"));

            // Check if already downloaded
            if (historyManager.isDownloaded(pkgUrl)) {
                int redownloadOption = JOptionPane.showConfirmDialog(this,
                        name + " has already been downloaded before.\n\nDo you want to download it again?",
                        "Already Downloaded",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                
                if (redownloadOption != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            int option = JOptionPane.showConfirmDialog(this,
                    "Do you want to add " + name + " (" + fileSize + ") to download list?",
                    "Download List",
                    JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION) {
                if (downloadList.stream().anyMatch(g -> g.getPkgUrl().equals(pkgUrl))) {
                    showNotification("Game already in download list", NotificationPanel.NotificationType.WARNING);
                    return;
                }

                Game game = new Game();
                game.setTitle(name);
                game.setPkgUrl(pkgUrl);
                game.setzRif(zRif);
                game.setRegion(region);
                game.setContentId(contentId);
                
                // Parse file size from string "X.X MiB" to long bytes
                if (fileSize != null && !fileSize.isEmpty()) {
                    try {
                        String[] parts = fileSize.split(" ");
                        if (parts.length >= 2) {
                            double sizeInMiB = Double.parseDouble(parts[0]);
                            long sizeInBytes = (long) (sizeInMiB * 1024 * 1024);
                            game.setFileSize(sizeInBytes);
                        }
                    } catch (NumberFormatException e) {
                        log.debug("Could not parse file size: {}", fileSize, e);
                    }
                }
                
                if (jrbPsp.isSelected()) game.setConsole(Console.PSP);
                else if (jrbPsvita.isSelected()) game.setConsole(Console.PSVITA);
                else if (jrbPsx.isSelected()) game.setConsole(Console.PSX);

                downloadList.add(game);
                System.out.println("Added to list: " + game);
            }
        }
    }

    private void jbDownloadListActionPerformed(java.awt.event.ActionEvent evt) {
        showDownloadListDialog();
    }// GEN-LAST:event_jtDataMousePressed
    
    /**
     * Shows an improved download list dialog with better UI and functionality
     */
    private void showDownloadListDialog() {
        // Create dialog
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Download Queue", true);
        dialog.setSize(900, 500);
        dialog.setLocationRelativeTo(this);
        
        // Create table with non-editable model
        String[] columnNames = {"#", "Name", "Region", "Console", "Size", "Content ID"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        JTable table = new JTable(model);
        table.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);  // #
        table.getColumnModel().getColumn(1).setPreferredWidth(300); // Name
        table.getColumnModel().getColumn(2).setPreferredWidth(80);  // Region
        table.getColumnModel().getColumn(3).setPreferredWidth(70);  // Console
        table.getColumnModel().getColumn(4).setPreferredWidth(100); // Size
        table.getColumnModel().getColumn(5).setPreferredWidth(150); // Content ID
        
        // Populate table
        updateDownloadListTable(model);
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        // Info panel at bottom
        JPanel infoPanel = new JPanel(new java.awt.BorderLayout());
        infoPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        javax.swing.JLabel infoLabel = new javax.swing.JLabel();
        updateDownloadListInfo(infoLabel);
        infoPanel.add(infoLabel, java.awt.BorderLayout.WEST);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 10));
        
        // Move Up button
        JButton moveUpButton = new JButton("▲ Move Up");
        moveUpButton.setToolTipText("Move selected items up in the queue");
        moveUpButton.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0 || selectedRows[0] == 0) return;
            
            for (int row : selectedRows) {
                if (row > 0) {
                    Game temp = downloadList.get(row);
                    downloadList.set(row, downloadList.get(row - 1));
                    downloadList.set(row - 1, temp);
                }
            }
            updateDownloadListTable(model);
            // Restore selection (shifted up)
            for (int i = 0; i < selectedRows.length; i++) {
                if (selectedRows[i] > 0) {
                    table.addRowSelectionInterval(selectedRows[i] - 1, selectedRows[i] - 1);
                }
            }
        });
        
        // Move Down button
        JButton moveDownButton = new JButton("▼ Move Down");
        moveDownButton.setToolTipText("Move selected items down in the queue");
        moveDownButton.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0 || selectedRows[selectedRows.length - 1] == downloadList.size() - 1) return;
            
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int row = selectedRows[i];
                if (row < downloadList.size() - 1) {
                    Game temp = downloadList.get(row);
                    downloadList.set(row, downloadList.get(row + 1));
                    downloadList.set(row + 1, temp);
                }
            }
            updateDownloadListTable(model);
            // Restore selection (shifted down)
            for (int i = 0; i < selectedRows.length; i++) {
                if (selectedRows[i] < downloadList.size() - 1) {
                    table.addRowSelectionInterval(selectedRows[i] + 1, selectedRows[i] + 1);
                }
            }
        });
        
        // Remove button
        JButton removeButton = new JButton("✖ Remove Selected");
        removeButton.setForeground(new Color(200, 50, 50));
        removeButton.setToolTipText("Remove selected games from queue");
        removeButton.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                showNotification("Please select items to remove", NotificationPanel.NotificationType.WARNING);
                return;
            }
            
            int confirm = JOptionPane.showConfirmDialog(dialog, 
                "Remove " + selectedRows.length + " item(s) from queue?",
                "Confirm Removal", 
                JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                // Remove in reverse order to maintain indices
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    downloadList.remove(selectedRows[i]);
                }
                updateDownloadListTable(model);
                updateDownloadListInfo(infoLabel);
            }
        });
        
        // Clear button
        JButton clearButton = new JButton("Clear All");
        clearButton.setToolTipText("Remove all games from queue");
        clearButton.addActionListener(e -> {
            if (downloadList.isEmpty()) return;
            
            int confirm = JOptionPane.showConfirmDialog(dialog,
                "Clear all " + downloadList.size() + " item(s) from queue?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (confirm == JOptionPane.YES_OPTION) {
                downloadList.clear();
                updateDownloadListTable(model);
                updateDownloadListInfo(infoLabel);
            }
        });
        
        // Start Download button
        JButton downloadButton = new JButton("▶ Start Downloads");
        downloadButton.setForeground(new Color(34, 139, 34));
        downloadButton.setFont(downloadButton.getFont().deriveFont(java.awt.Font.BOLD));
        downloadButton.setToolTipText("Start downloading all queued games");
        downloadButton.addActionListener(e -> {
            if (downloadList.isEmpty()) {
                showNotification("The download queue is empty", NotificationPanel.NotificationType.INFO);
                return;
            }
            dialog.dispose();
            startDownloads();
        });
        
        // Close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        
        // Add buttons to panel
        buttonPanel.add(moveUpButton);
        buttonPanel.add(moveDownButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL));
        buttonPanel.add(downloadButton);
        buttonPanel.add(closeButton);
        
        // Main panel layout
        JPanel mainPanel = new JPanel(new java.awt.BorderLayout(5, 5));
        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(scrollPane, java.awt.BorderLayout.CENTER);
        mainPanel.add(infoPanel, java.awt.BorderLayout.SOUTH);
        mainPanel.add(buttonPanel, java.awt.BorderLayout.PAGE_END);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    /**
     * Update the download list table with current queue
     */
    private void updateDownloadListTable(DefaultTableModel model) {
        model.setRowCount(0);
        int index = 1;
        for (Game game : downloadList) {
            model.addRow(new Object[]{
                index++,
                game.getTitle(),
                game.getRegion(),
                game.getConsole().toString(),
                convertFileSize(game.getFileSize()),
                game.getContentId() != null ? game.getContentId() : "N/A"
            });
        }
    }
    
    /**
     * Update the info label with queue statistics
     */
    private void updateDownloadListInfo(javax.swing.JLabel label) {
        if (downloadList.isEmpty()) {
            label.setText("Queue is empty. Add games from the table above.");
            return;
        }
        
        long totalSize = downloadList.stream().mapToLong(Game::getFileSize).sum();
        double totalGB = totalSize / (1024.0 * 1024.0 * 1024.0);
        
        String info = String.format("<html><b>Queue:</b> %d game(s) | <b>Total size:</b> %.2f GB", 
            downloadList.size(), totalGB);
        
        // Check available disk space
        File gamesDir = new File("games");
        if (gamesDir.exists()) {
            long freeSpace = gamesDir.getFreeSpace();
            double freeGB = freeSpace / (1024.0 * 1024.0 * 1024.0);
            
            if (totalSize > freeSpace) {
                info += String.format(" | <font color='red'><b>⚠ Free space: %.2f GB (Insufficient!)</b></font>", freeGB);
            } else {
                info += String.format(" | <b>Free space:</b> %.2f GB", freeGB);
            }
        }
        
        info += "</html>";
        label.setText(info);
    }

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
        Config config = new Config(configManager, this);
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
        // Cancel previous load if still running
        if (currentLoadWorker != null && !currentLoadWorker.isDone()) {
            currentLoadWorker.cancel(true);
        }

        Console console;
        try {
            console = Console.fromDisplayName(consoleName);
        } catch (IllegalArgumentException e) {
            log.error("Invalid console name: {}", consoleName, e);
            return;
        }

        // Show wait cursor
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        // Disable console buttons during loading
        jrbPsp.setEnabled(false);
        jrbPsvita.setEnabled(false);
        jrbPsx.setEnabled(false);

        // Create SwingWorker for async loading
        currentLoadWorker = new SwingWorker<List<Game>, Void>() {
            @Override
            protected List<Game> doInBackground() throws Exception {
                return gameRepository.loadGames(console);
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) {
                        log.debug("Game loading was cancelled for console: {}", consoleName);
                        return;
                    }

                    List<Game> games = get();
                    fillTableWithGames(games);
                    
                } catch (InterruptedException e) {
                    log.debug("Game loading interrupted for console: {}", consoleName, e);
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    log.error("Error loading games for console: {}", consoleName, e.getCause());
                    showNotification("Error loading games: " + e.getCause().getMessage(), 
                        NotificationPanel.NotificationType.ERROR);
                } finally {
                    // Restore cursor and enable buttons
                    setCursor(Cursor.getDefaultCursor());
                    jrbPsp.setEnabled(true);
                    jrbPsvita.setEnabled(true);
                    jrbPsx.setEnabled(true);
                }
            }
        };

        currentLoadWorker.execute();
    }

    /**
     * Populate the table with the loaded games (called on EDT after async load)
     */
    private void fillTableWithGames(List<Game> games) {
        // Define columns expected by the UI
        String[] columnNames = {"Name", "Region", "Content ID", "PKG direct link", "zRIF", "File Size"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        for (Game game : games) {
            model.addRow(new Object[]{
                    game.getTitle(),
                    game.getRegion(),
                    game.getContentId(),
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
        
        // Apply colors based on download history
        applyRowColors();
    }

    private String convertFileSize(long fileSize) {
        double fileSizeMiB = fileSize / (1024 * 1024.0);
        return String.format("%.1f MiB", fileSizeMiB);
    }

    /**
     * Apply colors to table rows based on download history status
     */
    private void applyRowColors() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    // Get the PKG URL from column 3 (after adding Content ID column)
                    int modelRow = table.convertRowIndexToModel(row);
                    String pkgUrl = (String) table.getModel().getValueAt(modelRow, 3);
                    
                    if (pkgUrl != null && !pkgUrl.isEmpty()) {
                        // Check download history status
                        if (historyManager.isDownloaded(pkgUrl)) {
                            c.setForeground(COLOR_COMPLETED);
                        } else if (historyManager.isPaused(pkgUrl)) {
                            c.setForeground(COLOR_PAUSED);
                        } else if (historyManager.isFailed(pkgUrl)) {
                            c.setForeground(COLOR_FAILED);
                        } else {
                            c.setForeground(COLOR_DEFAULT);
                        }
                    } else {
                        c.setForeground(COLOR_DEFAULT);
                    }
                } else {
                    // Keep default selection colors
                    c.setForeground(table.getSelectionForeground());
                }
                
                return c;
            }
        };
        
        // Apply renderer to all columns
        for (int i = 0; i < jtData.getColumnCount(); i++) {
            jtData.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private void filtrarTablaPorTextoRegionYTamano(String searchText, String region, String fileSize) {
        if (region == null || fileSize == null) {
            return;
        }

        try {
            List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();
            
            // Filtrar por texto ingresado (busca en Name, Region, Content ID y PKG direct link)
            if (searchText != null && !searchText.isEmpty()) {
                // Create filter for multiple columns: Name (0), Region (1), Content ID (2), PKG direct link (3)
                List<RowFilter<DefaultTableModel, Integer>> textFilters = new ArrayList<>();
                textFilters.add(RowFilter.regexFilter("(?i)" + searchText, 0)); // Name
                textFilters.add(RowFilter.regexFilter("(?i)" + searchText, 1)); // Region
                textFilters.add(RowFilter.regexFilter("(?i)" + searchText, 2)); // Content ID
                textFilters.add(RowFilter.regexFilter("(?i)" + searchText, 3)); // PKG direct link
                filters.add(RowFilter.orFilter(textFilters));
            }
            
            // Filtrar por región seleccionada
            if (!region.equals("All regions")) {
                filters.add(RowFilter.regexFilter("(?i)" + region, getColumnIndexByName("Region")));
            }
            
            // Filtrar por tamaño de archivo
            if (!fileSize.equals("All sizes")) {
                filters.add(createFileSizeFilter(fileSize));
            }

            // Combinar los filtros
            RowFilter<DefaultTableModel, Integer> combinedRowFilter = RowFilter.andFilter(filters);
            rowSorter.setRowFilter(combinedRowFilter);
        } catch (java.util.regex.PatternSyntaxException e) {
            log.debug("Invalid regex pattern in filter", e);
            rowSorter.setRowFilter(null);
        }
    }
    
    private RowFilter<DefaultTableModel, Integer> createFileSizeFilter(String sizeRange) {
        return new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                int fileSizeColumnIndex = getColumnIndexByName("File Size");
                if (fileSizeColumnIndex == -1) {
                    return true;
                }
                
                String fileSizeStr = (String) entry.getValue(fileSizeColumnIndex);
                if (fileSizeStr == null || fileSizeStr.isEmpty()) {
                    return true;
                }
                
                // Parse "X.X MiB" to get size in GB
                try {
                    String[] parts = fileSizeStr.split(" ");
                    if (parts.length < 2) {
                        return true;
                    }
                    
                    double sizeInMiB = Double.parseDouble(parts[0]);
                    double sizeInGB = sizeInMiB / 1024.0;
                    
                    switch (sizeRange) {
                        case "< 1GB":
                            return sizeInGB < 1.0;
                        case "1-5GB":
                            return sizeInGB >= 1.0 && sizeInGB <= 5.0;
                        case "> 5GB":
                            return sizeInGB > 5.0;
                        default:
                            return true;
                    }
                } catch (NumberFormatException e) {
                    log.debug("Error parsing file size: {}", fileSizeStr, e);
                    return true;
                }
            }
        };
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
        startDownloads(new ArrayList<>(downloadList));
    }

    /**
     * Start downloads for specific games
     * @param gamesToDownload List of games to download
     */
    private void startDownloads(List<Game> gamesToDownload) {
        if (gamesToDownload.isEmpty()) {
            showNotification("The download list is empty", NotificationPanel.NotificationType.INFO);
            return;
        }

        if (downloading) {
            showNotification("Downloads already in progress", NotificationPanel.NotificationType.WARNING);
            return;
        }

        // Validate disk space before starting downloads
        long totalSize = gamesToDownload.stream().mapToLong(Game::getFileSize).sum();
        SystemValidator.ValidationResult validationResult = systemValidator.validateTotalDiskSpace(totalSize, "games");
        if (!validationResult.isValid()) {
            showNotification(validationResult.getMessage(), NotificationPanel.NotificationType.WARNING);
            return;
        }

        downloading = true;
        jbResumeAndPause.setEnabled(true);
        
        // Clear queue since downloads are starting
        queueManager.clearQueue();

        resetDownloadUI(gamesToDownload);
        
        // Start global progress tracking
        globalProgressPanel.startDownloads(gamesToDownload.size(), totalSize);

        final int concurrency = Math.max(1, configManager.getSimultaneousDownloads());
        downloadExecutor = Executors.newFixedThreadPool(concurrency);

        for (Game game : gamesToDownload) {
            downloadExecutor.submit(() -> {
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
                                    packageService.extractPackage(fileName, game.getzRif(), game.getConsole(), success -> {
                                        if (success) {
                                            log.info("Extraction completed, updating table colors");
                                            applyRowColors();
                                        }
                                    });
                                }
                            } else {
                                // PSP and PSX always extract (no zRif needed)
                                packageService.extractPackage(fileName, null, game.getConsole(), success -> {
                                    if (success) {
                                        log.info("Extraction completed, updating table colors");
                                        applyRowColors();
                                    }
                                });
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
            downloadExecutor.shutdown();
            try {
                downloadExecutor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
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
        
        // Update global progress with current total downloaded bytes
        long totalDownloaded = progressBarsByUrl.values().stream()
            .mapToLong(b -> (long)((b.getValue() / 100.0) * b.getMaximum()))
            .sum();
        globalProgressPanel.updateProgress(totalDownloaded);
        
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
        
        // Save to download history
        DownloadHistory entry = new DownloadHistory();
        entry.setPkgUrl(game.getPkgUrl());
        entry.setTitle(game.getTitle());
        entry.setConsole(game.getConsole().toString());
        entry.setStatus("completed");
        entry.setDownloadDate(System.currentTimeMillis());
        entry.setFilePath("games/" + game.getConsole().toString());
        historyManager.addEntry(entry);
        log.info("Download completed and saved to history: {}", game.getTitle());
        
        // Update global progress
        globalProgressPanel.gameCompleted();
        showNotification(game.getTitle() + " downloaded successfully", NotificationPanel.NotificationType.SUCCESS);
        
        activeDownloadCount--;
        checkAllDownloadsFinished();
        saveDownloadState(); // Save state after completion
        updateRetryFailedButton();
    }

    private void markDownloadError(Game game) {
        javax.swing.JProgressBar bar = progressBarsByUrl.get(game.getPkgUrl());
        if (bar != null) {
            bar.setIndeterminate(false);
            bar.setString(game.getTitle() + " – Error – " + game.getConsole());
        }
        
        // Save to download history
        DownloadHistory entry = new DownloadHistory();
        entry.setPkgUrl(game.getPkgUrl());
        entry.setTitle(game.getTitle());
        entry.setConsole(game.getConsole().toString());
        entry.setStatus("failed");
        entry.setDownloadDate(System.currentTimeMillis());
        entry.setFilePath("");
        historyManager.addEntry(entry);
        log.warn("Download failed and saved to history: {}", game.getTitle());
        
        // Show error notification
        showNotification(game.getTitle() + " download failed", NotificationPanel.NotificationType.ERROR);
        
        activeDownloadCount--;
        checkAllDownloadsFinished();
        saveDownloadState(); // Save state after error
        updateRetryFailedButton();
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
            
            // Reset global progress panel
            globalProgressPanel.reset();
            showNotification("All downloads completed", NotificationPanel.NotificationType.SUCCESS);
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
     * Save the download queue to disk
     */
    private void saveDownloadQueue() {
        if (!downloadList.isEmpty() && !downloading) {
            queueManager.saveQueue(new ArrayList<>(downloadList));
            log.info("Saved {} games to download queue", downloadList.size());
        } else if (downloadList.isEmpty()) {
            queueManager.clearQueue();
        }
    }

    /**
     * Restore the download queue from previous session
     * Restores silently if there are in-progress downloads (will be added to the list)
     * Shows notification if only pending queue exists
     */
    private void restoreDownloadQueue() {
        List<Game> savedQueue = queueManager.loadQueue();
        if (savedQueue.isEmpty()) {
            return;
        }
        
        boolean hasInProgressDownloads = downloadStateManager.hasStates();
        
        if (hasInProgressDownloads) {
            // Silently restore queue - it will be added to the list after in-progress downloads
            downloadList.addAll(savedQueue);
            log.info("Silently restored {} pending games to queue (in-progress downloads will be handled separately)", savedQueue.size());
        } else {
            // No in-progress downloads, show notification about pending queue
            downloadList.addAll(savedQueue);
            log.info("Restored {} games to download queue", savedQueue.size());
            
            // Show notification to user
            SwingUtilities.invokeLater(() -> {
                int response = JOptionPane.showConfirmDialog(this,
                    "Found " + savedQueue.size() + " game(s) in download queue from previous session.\n\n" +
                    "Do you want to view the queue?",
                    "Queue Restored",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);
                    
                if (response == JOptionPane.YES_OPTION) {
                    showDownloadListDialog();
                }
            });
        }
    }

    /**
     * Restore downloads from saved state (IN-PROGRESS downloads, not pending queue)
     * Call this after Frame is initialized
     */
    public void restoreDownloads(List<DownloadState> savedStates) {
        if (savedStates == null || savedStates.isEmpty()) {
            return;
        }
        
        // These are downloads that were IN PROGRESS when app closed
        // They should be resumed automatically, not just added to queue
        List<Game> inProgressGames = new ArrayList<>();
        Map<String, Boolean> pauseStatesByUrl = new LinkedHashMap<>();
        
        for (DownloadState state : savedStates) {
            Game game = new Game();
            game.setTitle(state.getTitle());
            game.setRegion(state.getRegion());
            game.setConsole(Console.fromDisplayName(state.getConsole()));
            game.setPkgUrl(state.getPkgUrl());
            game.setzRif(state.getzRif());
            game.setFileSize(state.getTotalBytes());
            
            inProgressGames.add(game);
            pauseStatesByUrl.put(state.getPkgUrl(), state.isPaused());
        }
        
        // Check if there are pending games in the queue
        int pendingCount = downloadList.size();
        boolean hasPendingGames = pendingCount > 0;
        
        // Show confirmation dialog to user
        int count = inProgressGames.size();
        if (count > 0) {
            String message = String.format("Se encontraron %d descarga%s EN PROGRESO de la sesión anterior.\n¿Deseas reanudarlas ahora?",
                    count, count == 1 ? "" : "s", count == 1 ? "" : "s");
            
            if (hasPendingGames) {
                message += String.format("\n\nNota: También tienes %d juego%s pendiente%s en la cola que no se iniciarán automáticamente.",
                        pendingCount, pendingCount == 1 ? "" : "s", pendingCount == 1 ? "" : "s");
            }
            
            int option = JOptionPane.showConfirmDialog(this, message, "Restaurar Descargas en Progreso", 
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            
            if (option == JOptionPane.YES_OPTION) {
                // Start ONLY the in-progress downloads
                // Pending games stay in downloadList but are NOT started
                startDownloads(new ArrayList<>(inProgressGames));
                
                // Remove in-progress games from downloadList since they're now downloading
                for (Game inProgressGame : inProgressGames) {
                    downloadList.removeIf(g -> g.getPkgUrl().equals(inProgressGame.getPkgUrl()));
                }
                
                // Save the remaining pending queue if any
                if (!downloadList.isEmpty()) {
                    saveDownloadQueue();
                }
                
                // Restore pause states after a short delay to allow downloads to initialize
                SwingUtilities.invokeLater(() -> {
                    try {
                        Thread.sleep(DOWNLOAD_START_DELAY_MS); // Give downloads time to start
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
                // User declined, just clear the saved state file
                downloadStateManager.clearStates();
                // Keep pending queue intact (already in downloadList)
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
    private javax.swing.JButton jbRetryFailed;
    private javax.swing.JButton jbSetting;
    private javax.swing.JLabel jbsearch;
    private javax.swing.JComboBox<String> jcbRegion;
    private javax.swing.JComboBox<String> jcbFileSize;
    private javax.swing.JLabel jlFileSize;
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
        applyFilters();
    }
    
    private void jcbFileSizeItemStateChanged(java.awt.event.ItemEvent evt) {
        applyFilters();
    }
    
    private void applyFilters() {
        filtrarTablaPorTextoRegionYTamano(jtfSearch.getText(), 
            (String) jcbRegion.getSelectedItem(),
            (String) jcbFileSize.getSelectedItem());
    }

    private void jrbPsvitaActionPerformed(java.awt.event.ActionEvent evt) {
        fillTable();
    }

    private void jrbPsxActionPerformed(java.awt.event.ActionEvent evt) {
        fillTable();
    }

    private void jbRetryFailedActionPerformed(java.awt.event.ActionEvent evt) {
        retryFailedDownloads();
    }

    /**
     * Retry all failed downloads from history
     */
    private void retryFailedDownloads() {
        List<DownloadHistory> history = historyManager.loadHistory();
        List<DownloadHistory> failedDownloads = history.stream()
                .filter(h -> "failed".equals(h.getStatus()))
                .toList();

        if (failedDownloads.isEmpty()) {
            showNotification("No failed downloads found in history", 
                NotificationPanel.NotificationType.INFO);
            return;
        }

        // Confirm retry
        int result = JOptionPane.showConfirmDialog(this,
                "Found " + failedDownloads.size() + " failed download(s).\nDo you want to retry all of them?",
                "Retry Failed Downloads",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        // Convert failed history entries to Game objects and add to download queue
        int addedCount = 0;
        for (DownloadHistory failed : failedDownloads) {
            Game game = findGameByUrl(failed.getPkgUrl(), failed.getConsole());
            if (game != null) {
                // Check if not already in queue
                boolean alreadyInQueue = downloadList.stream()
                        .anyMatch(g -> g.getPkgUrl().equals(game.getPkgUrl()));
                
                if (!alreadyInQueue) {
                    downloadList.add(game);
                    addedCount++;
                    log.info("Added failed download to retry queue: {}", game.getTitle());
                }
            } else {
                log.warn("Could not find game info for failed download: {} ({})", failed.getTitle(), failed.getPkgUrl());
            }
        }

        // Remove failed entries from history (they'll be re-added on completion/failure)
        history.removeIf(h -> "failed".equals(h.getStatus()));
        historyManager.saveHistory(history);

        // Update button state
        updateRetryFailedButton();

        if (addedCount > 0) {
            showNotification(addedCount + " failed download(s) added to queue", 
                NotificationPanel.NotificationType.SUCCESS);
        } else {
            showNotification("No failed downloads could be added (games may no longer be available)", 
                NotificationPanel.NotificationType.WARNING);
        }
    }

    /**
     * Find a Game object by its PKG URL from the current table
     */
    private Game findGameByUrl(String pkgUrl, String consoleStr) {
        DefaultTableModel model = (DefaultTableModel) jtData.getModel();
        
        // First try to find in current table
        for (int i = 0; i < model.getRowCount(); i++) {
            String tablePkgUrl = (String) model.getValueAt(i, getColumnIndexByName("PKG direct link"));
            if (pkgUrl.equals(tablePkgUrl)) {
                // Found in table, create Game object
                String name = (String) model.getValueAt(i, getColumnIndexByName("Name"));
                String region = (String) model.getValueAt(i, getColumnIndexByName("Region"));
                String type = (String) model.getValueAt(i, getColumnIndexByName("Type"));
                String console = (String) model.getValueAt(i, getColumnIndexByName("Console"));
                String contentId = (String) model.getValueAt(i, getColumnIndexByName("Content ID"));
                String fileSizeStr = (String) model.getValueAt(i, getColumnIndexByName("File Size"));
                
                // Parse file size
                long fileSize = 0;
                if (fileSizeStr != null && !fileSizeStr.isEmpty()) {
                    try {
                        String numericPart = fileSizeStr.replaceAll("[^0-9.]", "").trim();
                        if (!numericPart.isEmpty()) {
                            double sizeMB = Double.parseDouble(numericPart);
                            fileSize = (long) (sizeMB * 1024 * 1024); // Convert MiB to bytes
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse file size: {}", fileSizeStr);
                    }
                }
                
                // Create Game using constructor: title, region, console, pkgUrl, zRif, fileSize
                Game game = new Game(name, region, Console.fromDisplayName(console), pkgUrl, type, fileSize);
                game.setContentId(contentId);
                return game;
            }
        }
        
        // If not in current table, try loading from the appropriate database
        try {
            Console console = Console.fromDisplayName(consoleStr);
            List<Game> games = gameRepository.loadGames(console);
            return games.stream()
                    .filter(g -> pkgUrl.equals(g.getPkgUrl()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error loading game from database for retry: {}", pkgUrl, e);
            return null;
        }
    }

    /**
     * Update Retry Failed button state based on failed downloads in history
     */
    private void updateRetryFailedButton() {
        List<DownloadHistory> history = historyManager.loadHistory();
        boolean hasFailedDownloads = history.stream()
                .anyMatch(h -> "failed".equals(h.getStatus()));
        jbRetryFailed.setEnabled(hasFailedDownloads);
    }

    /**
     * Shutdown download executor gracefully
     */
    private void shutdownDownloadExecutor() {
        if (downloadExecutor != null && !downloadExecutor.isShutdown()) {
            log.info("Shutting down download executor");
            downloadExecutor.shutdownNow();
            try {
                if (!downloadExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warn("Download executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                log.warn("Executor shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Setup keyboard shortcuts for quick actions
     */
    private void setupKeyboardShortcuts() {
        // Ctrl+F: Focus search field
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK), "focusSearch");
        getRootPane().getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jtfSearch.requestFocusInWindow();
                jtfSearch.selectAll();
            }
        });
        
        // Ctrl+D: Add selected game to download queue (simulates double-click)
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_DOWN_MASK), "addToQueue");
        getRootPane().getActionMap().put("addToQueue", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = jtData.getSelectedRow();
                if (selectedRow != -1) {
                    // Trigger the same logic as double-clicking the row
                    jtDataMousePressed(null);
                }
            }
        });
        
        // Ctrl+L: View download list
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK), "viewDownloadList");
        getRootPane().getActionMap().put("viewDownloadList", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jbDownloadListActionPerformed(null);
            }
        });
        
        // Space: Pause/Resume downloads (only when downloads are active)
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "togglePauseResume");
        getRootPane().getActionMap().put("togglePauseResume", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Only trigger if not focused on a text component or table
                Component focused = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (!(focused instanceof javax.swing.text.JTextComponent) && 
                    !(focused instanceof javax.swing.JTable) &&
                    jbResumeAndPause.isEnabled()) {
                    jbResumeAndPauseActionPerformed(null);
                }
            }
        });
        
        // Delete: Cancel selected download (when download list is visible)
        jtData.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "cancelDownload");
        jtData.getActionMap().put("cancelDownload", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = jtData.getSelectedRow();
                if (selectedRow != -1 && downloading) {
                    int modelRow = jtData.convertRowIndexToModel(selectedRow);
                    DefaultTableModel model = (DefaultTableModel) jtData.getModel();
                    String pkgUrl = (String) model.getValueAt(modelRow, getColumnIndexByName("PKG direct link"));
                    String name = (String) model.getValueAt(modelRow, getColumnIndexByName("Name"));
                    
                    // Find the game in download list
                    Game gameToCancel = downloadList.stream()
                        .filter(g -> g.getPkgUrl().equals(pkgUrl))
                        .findFirst()
                        .orElse(null);
                    
                    if (gameToCancel != null) {
                        int confirm = JOptionPane.showConfirmDialog(
                            Frame.this,
                            "Cancel download for: " + name + "?",
                            "Cancel Download",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                        );
                        if (confirm == JOptionPane.YES_OPTION) {
                            downloadList.remove(gameToCancel);
                            markDownloadCancelled(gameToCancel);
                            showNotification("Download cancelled: " + name, NotificationPanel.NotificationType.WARNING);
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Setup notification and progress panels
     */
    private void setupNotificationAndProgressPanels() {
        // Initialize notification panel
        notificationPanel = new NotificationPanel();
        notificationPanel.setBounds(10, 10, 420, 200);
        
        // Initialize global progress panel
        globalProgressPanel = new GlobalProgressPanel();
        
        // Add as layered components
        JLayeredPane layeredPane = getRootPane().getLayeredPane();
        layeredPane.add(notificationPanel, JLayeredPane.PALETTE_LAYER);
        
        // Add global progress to main content
        Container contentPane = getContentPane();
        if (contentPane.getLayout() instanceof GroupLayout) {
            // Will be added to layout in initComponents
        } else {
            contentPane.add(globalProgressPanel, BorderLayout.SOUTH);
        }
    }
    
    /**
     * Show a notification message
     */
    private void showNotification(String message, NotificationPanel.NotificationType type) {
        if (notificationPanel != null) {
            notificationPanel.showNotification(message, type);
        }
    }
    
    /**
     * Setup DatabaseManager listeners and check initial availability
     */
    private void setupDatabaseManager() {
        // Subscribe to database events
        databaseManager.addListener(new com.squarepeace.nnppss.service.DatabaseManager.DatabaseListener() {
            @Override
            public void onAvailabilityChanged(Console console, boolean available) {
                SwingUtilities.invokeLater(() -> updateConsoleButton(console, available));
            }
            
            @Override
            public void onDownloadComplete(Console console, boolean success) {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        log.info("Database downloaded successfully for {}", console);
                        updateConsoleButton(console, true);
                        
                        // Auto-select and load if no console is currently selected
                        if (bgConsoles.getSelection() == null) {
                            selectConsole(console);
                        }
                    }
                });
            }
            
            @Override
            public void onDownloadProgress(Console console, String message) {
                log.debug("Download progress: {}", message);
            }
        });
        
        // Check availability at startup and download missing databases
        databaseManager.checkAllConsolesAvailability().thenRun(() -> {
            log.info("Console availability check completed");
            
            // Auto-select first available console
            SwingUtilities.invokeLater(() -> {
                if (bgConsoles.getSelection() == null) {
                    for (Console console : Console.values()) {
                        if (databaseManager.isConsoleAvailable(console)) {
                            selectConsole(console);
                            break;
                        }
                    }
                }
                
                // Download missing databases
                databaseManager.downloadAllDatabases();
            });
        });
    }
    
    private void updateConsoleButton(Console console, boolean available) {
        javax.swing.JRadioButton button = getConsoleButton(console);
        if (button != null) {
            button.setEnabled(available);
            
            // If current selection becomes unavailable, clear it
            if (!available && button.isSelected()) {
                bgConsoles.clearSelection();
                jtData.setModel(new DefaultTableModel());
            }
        }
    }
    
    private javax.swing.JRadioButton getConsoleButton(Console console) {
        switch (console) {
            case PSP: return jrbPsp;
            case PSVITA: return jrbPsvita;
            case PSX: return jrbPsx;
            default: return null;
        }
    }
    
    private void selectConsole(Console console) {
        javax.swing.JRadioButton button = getConsoleButton(console);
        if (button != null && button.isEnabled()) {
            button.setSelected(true);
            fillTable();
        }
    }
    
    @Override
    public void onConfigSaved() {
        // Recheck database availability after config changes
        databaseManager.checkAllConsolesAvailability().thenRun(() -> {
            databaseManager.downloadAllDatabases();
        });
    }
}
