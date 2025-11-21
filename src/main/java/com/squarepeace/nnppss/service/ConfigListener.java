package com.squarepeace.nnppss.service;

/**
 * Listener interface for configuration changes.
 * Implement this interface to be notified when configuration is saved.
 */
public interface ConfigListener {
    /**
     * Called when configuration has been saved successfully
     */
    void onConfigSaved();
}
