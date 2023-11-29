package org.ohdsi.whiterabbit;


import org.ohdsi.databases.DbSettings;

import javax.swing.*;
import java.util.List;

/**
 * Defines the interface between the application's main class and its (Swing) components (panels).
 */
public interface PanelsManager {
    void runConnectionTest();

    JButton getAddAllButton();

    List<JComponent> getComponentsToDisableWhenRunning();
}
