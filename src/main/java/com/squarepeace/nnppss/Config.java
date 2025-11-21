/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.squarepeace.nnppss;

import com.squarepeace.nnppss.service.ConfigManager;
import com.squarepeace.nnppss.service.ConfigListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class Config extends javax.swing.JFrame {

    private final ConfigListener configListener;
    private final ConfigManager configManager;

    public Config(ConfigManager configManager, ConfigListener listener) {
        this.configManager = configManager;
        this.configListener = listener;
        initComponents();
    }
    
    public Config(ConfigManager configManager) {
        this(configManager, (ConfigListener) null);
    }
    
    public Config() {
        this(new ConfigManager(), (ConfigListener) null);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jtfUrlPsvita = new javax.swing.JTextField();
        jtfUrlPsp = new javax.swing.JTextField();
        jbSave = new javax.swing.JButton();
        jbClear = new javax.swing.JButton();
        jbClose = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jtfUrlPsx = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jSpinner_simultaneous_downloads = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jSpinner_download_speed = new javax.swing.JSpinner();
        jSpinner_download_speed.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 50));
        ((javax.swing.JSpinner.DefaultEditor)jSpinner_download_speed.getEditor()).getTextField().setColumns(8);
        jCheckBoxAutoCleanup = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Configuration");
        setUndecorated(true);

        jLabel1.setText("PSVITA URL:");

        jLabel2.setText("PSP URL:");

        jbSave.setText("Save");
        jbSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSaveActionPerformed(evt);
            }
        });

        jbClear.setText("Clear");
        jbClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbClearActionPerformed(evt);
            }
        });

        jbClose.setText("Close");
        jbClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbCloseActionPerformed(evt);
            }
        });

        jLabel3.setText("PSX URL:");

        jLabel4.setText("simultaneous downloads:");

        jLabel5.setText("Download Speed Limit (KB/s):");
        jSpinner_download_speed.setToolTipText("0 for unlimited");

        jCheckBoxAutoCleanup.setText("Auto-delete .pkg files after extraction");
        jCheckBoxAutoCleanup.setToolTipText("Automatically delete package files after successful extraction to save disk space");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(74, 74, 74)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jbClear)
                    .addComponent(jLabel1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2))
                        .addGap(8, 8, 8)))
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(44, 44, 44)
                        .addComponent(jbClose)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 110, Short.MAX_VALUE)
                        .addComponent(jbSave)
                        .addGap(20, 20, 20))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jtfUrlPsvita, javax.swing.GroupLayout.DEFAULT_SIZE, 294, Short.MAX_VALUE)
                                .addComponent(jtfUrlPsx))
                            .addComponent(jtfUrlPsp, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jtfUrlPsp, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jSpinner_simultaneous_downloads, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jSpinner_download_speed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(jCheckBoxAutoCleanup))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jtfUrlPsvita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jtfUrlPsp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jtfUrlPsx, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jSpinner_simultaneous_downloads, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jSpinner_download_speed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jCheckBoxAutoCleanup)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbSave)
                    .addComponent(jbClear)
                    .addComponent(jbClose))
                .addGap(23, 23, 23))
        );

        setContentPane(jPanel1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jbClearActionPerformed(java.awt.event.ActionEvent evt) {
        jtfUrlPsvita.setText("");
        jtfUrlPsp.setText("");
        jtfUrlPsx.setText("");
    }

    private void jbSaveActionPerformed(java.awt.event.ActionEvent evt) {
        // Validate download speed limit
        int speedLimit = (Integer) jSpinner_download_speed.getValue();
        if (speedLimit < 0) {
            JOptionPane.showMessageDialog(this, 
                "Download speed limit cannot be negative. Use 0 for unlimited.", 
                "Invalid Value", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validate simultaneous downloads
        int simDownloads = (Integer) jSpinner_simultaneous_downloads.getValue();
        if (simDownloads < 1 || simDownloads > 10) {
            JOptionPane.showMessageDialog(this, 
                "Simultaneous downloads must be between 1 and 10", 
                "Invalid Value", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        configManager.setProperty("psvita.url", jtfUrlPsvita.getText());
        configManager.setProperty("psp.url", jtfUrlPsp.getText());
        configManager.setProperty("psx.url", jtfUrlPsx.getText());
        configManager.setProperty("simultaneousDownloads", jSpinner_simultaneous_downloads.getValue().toString());
        configManager.setProperty("downloadSpeedLimit", jSpinner_download_speed.getValue().toString());
        configManager.setProperty("autoCleanupPkg", String.valueOf(jCheckBoxAutoCleanup.isSelected()));
        configManager.saveConfig();
        if (configListener != null) {
            configListener.onConfigSaved();
        }
        JOptionPane.showMessageDialog(null, "Configuration saved successfully");
    }

    private void jbCloseActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    public void loadValues() {
        String psvitaUrl = configManager.getPsVitaUrl();
        String pspUrl = configManager.getPspUrl();
        String psxUrl = configManager.getPsxUrl();
        String simultaneousDownloads = configManager.getProperty("simultaneousDownloads");
        int downloadSpeedLimit = configManager.getDownloadSpeedLimit();
        String autoCleanup = configManager.getProperty("autoCleanupPkg");

        if (psvitaUrl != null) jtfUrlPsvita.setText(psvitaUrl);
        if (pspUrl != null) jtfUrlPsp.setText(pspUrl);
        if (psxUrl != null) jtfUrlPsx.setText(psxUrl);
        
        if (simultaneousDownloads != null && simultaneousDownloads.matches("[0-9]+") && Integer.parseInt(simultaneousDownloads) > 0) {
            jSpinner_simultaneous_downloads.setValue(Integer.valueOf(simultaneousDownloads));
        } else {
            jSpinner_simultaneous_downloads.setValue(1);
        }
        
        jSpinner_download_speed.setValue(downloadSpeedLimit);
        
        jCheckBoxAutoCleanup.setSelected(autoCleanup != null && Boolean.parseBoolean(autoCleanup));
    }

    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Config.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Config.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Config.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Config.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Config().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jCheckBoxAutoCleanup;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSpinner jSpinner_simultaneous_downloads;
    private javax.swing.JSpinner jSpinner_download_speed;
    private javax.swing.JButton jbClear;
    private javax.swing.JButton jbClose;
    private javax.swing.JButton jbSave;
    private javax.swing.JTextField jtfUrlPsp;
    private javax.swing.JTextField jtfUrlPsvita;
    private javax.swing.JTextField jtfUrlPsx;
    // End of variables declaration//GEN-END:variables
    
}
