package org.ohdsi.whiteRabbit.gui;

import javax.swing.*;
import java.util.*;

public class SourcePanel extends JPanel {
    private List<JComponent> clearableComponents = new ArrayList<>();

    public void addReplacable(String name, JComponent component) {

        this.add(component);
        clearableComponents.add(component);
    }

    public void clear() {
        // remove the components in the reverse order of how they were added, keeps the layout of the JPanel intact
        Collections.reverse(clearableComponents);
        clearableComponents.forEach(this::remove);
        clearableComponents.clear();
    }

}
