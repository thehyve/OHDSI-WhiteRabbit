package org.ohdsi.whiteRabbit.gui;

import org.ohdsi.databases.DBConnectorInterface;
import org.ohdsi.databases.configuration.DBChoice;
import org.ohdsi.databases.configuration.DBConfiguration;
import org.ohdsi.databases.configuration.DBConfigurationException;
import org.ohdsi.whiteRabbit.PanelsManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Objects;

import static org.ohdsi.whiteRabbit.WhiteRabbitMain.DELIMITED_TEXT_FILES;
import static org.ohdsi.whiteRabbit.WhiteRabbitMain.LABEL_TEST_CONNECTION;

public class LocationsPanel extends JPanel {
    public static final String LABEL_LOCATIONS = "Locations";
    public static final String LABEL_SERVER_LOCATION = "Server location";
    public static final String LABEL_USER_NAME = "User name";
    public static final String LABEL_PASSWORD = "Password";
    public static final String LABEL_DATABASE_NAME = "Database name";
    public static final String LABEL_DELIMITER = "Delimiter";

    private final JFrame parentFrame;
    private JTextField folderField;
    private JComboBox<String> sourceType;
    private JTextField sourceDelimiterField;
    private JTextField sourceServerField;
    private JTextField sourceUserField;
    private JTextField sourcePasswordField;
    private JTextField sourceDatabaseField;

    private boolean sourceIsFiles = true;
    private boolean sourceIsSas = false;

    private final transient PanelsManager panelsManager;

    public LocationsPanel(JFrame parentFrame, PanelsManager panelsManager) {
        super();
        this.parentFrame = parentFrame;
        this.panelsManager = panelsManager;
        this.createLocationsPanel();
    }

    private void createLocationsPanel() {
        JPanel panel = this;
        panel.setName(LABEL_LOCATIONS);

        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.5;

        JPanel folderPanel = new JPanel();
        folderPanel.setLayout(new BoxLayout(folderPanel, BoxLayout.X_AXIS));
        folderPanel.setBorder(BorderFactory.createTitledBorder("Working folder"));
        folderField = new JTextField();
        folderField.setName("FolderField");
        folderField.setText((new File("").getAbsolutePath()));
        folderField.setToolTipText("The folder where all output will be written");
        folderPanel.add(folderField);
        JButton pickButton = new JButton("Pick folder");
        pickButton.setToolTipText("Pick a different working folder");
        folderPanel.add(pickButton);
        pickButton.addActionListener(e -> pickFolder());
        panelsManager.getComponentsToDisableWhenRunning().add(pickButton);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        panel.add(folderPanel, c);

        JPanel sourcePanel = createSourcePanel();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        panel.add(sourcePanel, c);

        JPanel testConnectionButtonPanel = new JPanel();
        testConnectionButtonPanel.setLayout(new BoxLayout(testConnectionButtonPanel, BoxLayout.X_AXIS));
        testConnectionButtonPanel.add(Box.createHorizontalGlue());

        JButton testConnectionButton = new JButton(LABEL_TEST_CONNECTION);
        testConnectionButton.setName(LABEL_TEST_CONNECTION);
        testConnectionButton.setBackground(new Color(151, 220, 141));
        testConnectionButton.setToolTipText("Test the connection");
        testConnectionButton.addActionListener(e -> panelsManager.runConnectionTest());
        panelsManager.getComponentsToDisableWhenRunning().add(testConnectionButton);
        testConnectionButtonPanel.add(testConnectionButton);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        panel.add(testConnectionButtonPanel, c);
    }

    private void createDatabaseFields(ItemEvent itemEvent) {
        String selectedSourceType = itemEvent.getItem().toString();

        DBChoice dbChoice = DBChoice.getDBChoice(selectedSourceType);
        if (dbChoice.supportsDBConnectorInterface()) {
            createDatabaseFields(itemEvent, dbChoice);
        } else {
            createDatabaseFields(itemEvent, selectedSourceType);
        }
    }

    private void createDatabaseFields(ItemEvent itemEvent, DBChoice dbChoice) {
        //throw new DBConfigurationException("Not implemented (yet)");

        DBConnectorInterface dbConnectorInterface = dbChoice.getDbConnectorInterface();
        dbConnectorInterface.getFields().stream().forEach(f -> {

        });
    }

    private void createDatabaseFields(ItemEvent itemEvent, String selectedSourceType) {
        sourceIsFiles = selectedSourceType.equals(DELIMITED_TEXT_FILES);
        sourceIsSas = selectedSourceType.equals("SAS7bdat");
        boolean sourceIsDatabase = !(sourceIsFiles || sourceIsSas);

        sourceServerField.setEnabled(sourceIsDatabase);
        sourceUserField.setEnabled(sourceIsDatabase);
        sourcePasswordField.setEnabled(sourceIsDatabase);
        sourceDatabaseField.setEnabled(sourceIsDatabase && !selectedSourceType.equals(DBChoice.Azure.name()));
        sourceDelimiterField.setEnabled(sourceIsFiles);
        panelsManager.getAddAllButton().setEnabled(sourceIsDatabase);

        if (sourceIsDatabase && selectedSourceType.equals(DBChoice.Oracle.name())) {
            sourceServerField.setToolTipText("For Oracle servers this field contains the SID, servicename, and optionally the port: '<host>/<sid>', '<host>:<port>/<sid>', '<host>/<service name>', or '<host>:<port>/<service name>'");
            sourceUserField.setToolTipText("For Oracle servers this field contains the name of the user used to log in");
            sourcePasswordField.setToolTipText("For Oracle servers this field contains the password corresponding to the user");
            sourceDatabaseField.setToolTipText("For Oracle servers this field contains the schema (i.e. 'user' in Oracle terms) containing the source tables");
        } else if (sourceIsDatabase && selectedSourceType.equals("PostgreSQL")) {
            sourceServerField.setToolTipText("For PostgreSQL servers this field contains the host name and database name (<host>/<database>)");
            sourceUserField.setToolTipText("The user used to log in to the server");
            sourcePasswordField.setToolTipText("The password used to log in to the server");
            sourceDatabaseField.setToolTipText("For PostgreSQL servers this field contains the schema containing the source tables");
        } else if (sourceIsDatabase && selectedSourceType.equals("BigQuery")) {
            sourceServerField.setToolTipText("GBQ SA & UA:  ProjectID");
            sourceUserField.setToolTipText("GBQ SA only: OAuthServiceAccountEMAIL");
            sourcePasswordField.setToolTipText("GBQ SA only: OAuthPvtKeyPath");
            sourceDatabaseField.setToolTipText("GBQ SA & UA: Data Set within ProjectID");
        } else if (sourceIsDatabase) {
            if (selectedSourceType.equals("Azure")) {
                sourceServerField.setToolTipText("For Azure, this field contains the host name and database name (<host>;database=<database>)");
            } else {
                sourceServerField.setToolTipText("This field contains the name or IP address of the database server");
            }
            if (selectedSourceType.equals("SQL Server")) {
                sourceUserField.setToolTipText("The user used to log in to the server. Optionally, the domain can be specified as <domain>/<user> (e.g. 'MyDomain/Joe')");
            } else {
                sourceUserField.setToolTipText("The user used to log in to the server");
            }
            sourcePasswordField.setToolTipText("The password used to log in to the server");
            if (selectedSourceType.equals("Azure")) {
                sourceDatabaseField.setToolTipText("For Azure, leave this empty");
            } else {
                sourceDatabaseField.setToolTipText("The name of the database containing the source tables");
            }
        }
    }
    private JPanel createSourcePanel() {
        JPanel sourcePanel = new JPanel();
        sourcePanel.setLayout(new GridLayout(0, 2));
        sourcePanel.setBorder(BorderFactory.createTitledBorder("Source data location"));
        sourcePanel.add(new JLabel("Data type"));
        sourceType = new JComboBox<>(DBChoice.choices());
        sourceType.setName("SourceType");
        sourceType.setToolTipText("Select the type of source data available");
        sourceType.addItemListener(this::createDatabaseFields);
        sourcePanel.add(sourceType);

        sourcePanel.add(new JLabel(LABEL_SERVER_LOCATION));
        sourceServerField = new JTextField("127.0.0.1");
        sourceServerField.setName(LABEL_SERVER_LOCATION);
        sourceServerField.setEnabled(false);
        sourcePanel.add(sourceServerField);
        sourcePanel.add(new JLabel(LABEL_USER_NAME));
        sourceUserField = new JTextField("");
        sourceUserField.setName(LABEL_USER_NAME);
        sourceUserField.setEnabled(false);
        sourcePanel.add(sourceUserField);
        sourcePanel.add(new JLabel(LABEL_PASSWORD));
        sourcePasswordField = new JPasswordField("");
        sourcePasswordField.setName(LABEL_PASSWORD);
        sourcePasswordField.setEnabled(false);
        sourcePanel.add(sourcePasswordField);
        sourcePanel.add(new JLabel(LABEL_DATABASE_NAME));
        sourceDatabaseField = new JTextField("");
        sourceDatabaseField.setName(LABEL_DATABASE_NAME);
        sourceDatabaseField.setEnabled(false);
        sourcePanel.add(sourceDatabaseField);

        sourcePanel.add(new JLabel(LABEL_DELIMITER));
        JTextField delimiterField = new JTextField(",");
        delimiterField.setName(LABEL_DELIMITER);
        sourceDelimiterField = delimiterField;
        sourceDelimiterField.setToolTipText("The delimiter that separates values. Enter 'tab' for tab.");
        sourcePanel.add(sourceDelimiterField);

        return sourcePanel;
    }

    public JTextField getFolderField() {
        return folderField;
    }

    public String getSelectedSourceType() {
            return Objects.requireNonNull(sourceType.getSelectedItem()).toString();
    }

    public JTextField getSourceDelimiterField() {
        return sourceDelimiterField;
    }

    public boolean sourceIsFiles() {
        return sourceIsFiles;
    }

    public boolean sourceIsSas() {
        return sourceIsSas;
    }

    public String getSourceServerField() {
        return sourceServerField.getText();
    }

    public String getSourceUserField() {
        return sourceUserField.getText();
    }

    public String getSourcePasswordField() {
        return sourcePasswordField.getText();
    }
    public String getSourceDatabaseField() {
        return sourceDatabaseField.getText();
    }

    public boolean isSourceDatabaseFieldEnabled() {
        return sourceDatabaseField.isEnabled();
    }

    private void pickFolder() {
        JFileChooser fileChooser = new JFileChooser(new File(folderField.getText()));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fileChooser.showDialog(parentFrame, "Select folder");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            if (!selectedDirectory.exists()) {
                // When no directory is selected when approving, FileChooser incorrectly appends the current directory to the path.
                // Take the opened directory instead.
                selectedDirectory = fileChooser.getCurrentDirectory();
            }
            folderField.setText(selectedDirectory.getAbsolutePath());
        }
    }
}
