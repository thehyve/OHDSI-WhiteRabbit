package org.ohdsi.whiterabbit;


import org.ohdsi.databases.DBConnectorInterface;
import org.ohdsi.databases.configuration.DBConfiguration;

import javax.swing.*;
import java.util.List;

/**
 * Defines the interface between the application's main class and its (Swing) components (panels).
 */
public interface PanelsManager {
    void runConnectionTest();
    void runConnectionTest(DBConfiguration dbConfiguration);

    JButton getAddAllButton();

    List<JComponent> getComponentsToDisableWhenRunning();
}
