package gui.components;
import org.javatuples.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

/**
 * THIS FILE WORKS, DO NOT CHANGE IT
 */
public class TabbedGUIContainer extends JPanel {
    public interface ScreenRequirement {
        public boolean canOpen();
    }
    public interface TabPanel {
        public void setNotebookContainer(TabbedGUIContainer cont);
        public void onSelected();
    }

    private static final int TAB_BUTTON_MARGIN = 5;

    private final JPanel tabContainer = new JPanel();
    private final JPanel tabButtonList = new JPanel(new GridBagLayout());
    private GridBagConstraints tabButtonConstraints;

    private final JPanel contentContainer = new JPanel();
    private GridBagConstraints contentConstraints;

    private final Map<String, Triplet<JPanel, ScreenRequirement, JButton>> panels = new HashMap<>();

    private String currentTab = null;

    private void initPanel(float splitRatio) {
        // ensures that each screen fills the space

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();

        this.setLayout(gbl);
        this.contentContainer.setLayout(gbl);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.01;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        gbl.setConstraints(tabContainer, gbc);
        this.add(tabContainer, gbc);

        gbc.weightx = splitRatio;
        gbc.gridx = 1;

        gbl.setConstraints(contentContainer, gbc);
        this.add(contentContainer, gbc);

        this.contentConstraints = new GridBagConstraints();
        this.contentConstraints.fill = GridBagConstraints.BOTH;
        this.contentConstraints.weighty = 1;
        this.contentConstraints.weightx = 1;

        this.tabContainer.setMinimumSize(new Dimension(170, 0));

        initTabButtonContainer();
        resetTabButtonDisplay();
    }

    /**
     * Sets up the layout settings for the tab sidebar and the child buttons
     */
    private void initTabButtonContainer() {
        this.tabContainer.setLayout(new BorderLayout());

        this.tabButtonConstraints = new GridBagConstraints();
        this.tabButtonConstraints.insets = new Insets(TAB_BUTTON_MARGIN,TAB_BUTTON_MARGIN,TAB_BUTTON_MARGIN,TAB_BUTTON_MARGIN);
        this.tabButtonConstraints.gridwidth = GridBagConstraints.REMAINDER;
        this.tabButtonConstraints.fill = GridBagConstraints.HORIZONTAL;
        this.tabButtonConstraints.weightx = 1;
        this.tabButtonConstraints.weighty = 1;
    }

    private void resetTabButtonDisplay() {
        // this function makes the buttons display in a nice list from the top within the sidebar
        this.tabContainer.removeAll();
        this.tabButtonList.removeAll();

        this.tabButtonConstraints.weightx = 1;
        this.tabButtonConstraints.weighty = 1;
        tabButtonList.add(new JPanel(), this.tabButtonConstraints);
        this.tabContainer.add(new JScrollPane(tabButtonList));
    }

    private void enableAllButtons() {
        for (Triplet<JPanel, ScreenRequirement, JButton> panel : panels.values()) {
            panel.getValue2().setEnabled(true);
        }
    }

    public TabbedGUIContainer(float splitRatio) {
        this.initPanel(splitRatio);
    }

    /**
     * Inserts a tab into the screen, with no requirements
     * @param name The UNIQUELY IDENTIFYING name for this panel, also the text for the button
     * @param root The root JPanel of this specific page, containing it completely
     */
    public void insertTab(String name, JPanel root) {
        this.insertTab(name, root, new ScreenRequirement() {
            @Override
            public boolean canOpen() {
                return true;
            }
        });
    }

    /**
     * Adds a new panel option to this GUI
     * @param name The UNIQUELY IDENTIFYING name for this panel, also the text for the button
     * @param root The root JPanel of this specific page, containing it completely
     * @param constraints A predicate function that runs a check if a user can open this screen (for controller logic)
     */
    public void insertTab(String name, JPanel root, ScreenRequirement constraints) {
        // do not add a panel with the same name as another
        if (panels.containsKey(name)) {
            throw new IllegalArgumentException("[GUITabs] You cannot have two screens with the same name.");
        }

        JButton tb = new JButton(name);
        tb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchTab(name);
                enableAllButtons();
                tb.setEnabled(false);
            }
        });
        this.tabButtonConstraints.weighty = 0;
        this.tabButtonList.add(tb, this.tabButtonConstraints, 0);

        panels.put(name, Triplet.with(root, constraints, tb));

        if (Arrays.asList(root.getClass().getInterfaces()).contains(TabPanel.class)) {
            ((TabPanel)root).setNotebookContainer(this);
        }

        if (!panels.containsKey(currentTab)) {
            switchTab(name);
        }

        revalidate();
        repaint();
    }

    /**
     * Inserts a button into the tab menu that does not link to an actual screen
     * @param button Your beautiful button
     */
    public void insertNonTabButton(JButton button) {
        this.tabButtonConstraints.weighty = 0;
        this.tabButtonList.add(button, this.tabButtonConstraints, 0);
    }

    /**
     * Adds a division in the button tab list for grouping pages
     */
    public void insertDivider() {
        this.tabButtonList.add(new JSeparator(SwingConstants.HORIZONTAL), this.tabButtonConstraints, 0);
    }

    /**
     * Changes the currently active screen tab
     * @param name Must be a panel that has already been registered
     */
    public void switchTab(String name) {
        try {
            JPanel ui = panels.get(name).getValue0();
            if (ui == null)
                throw new NullPointerException();

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    contentContainer.removeAll();
                    contentContainer.add(ui, contentConstraints);
                    enableAllButtons();
                    panels.get(name).getValue2().setEnabled(false);

                    if (Arrays.asList(ui.getClass().getInterfaces()).contains(TabPanel.class)) {
                        ((TabPanel)ui).onSelected();
                    }

                    revalidate();
                    repaint();
                }
            });

            this.currentTab = name;

        } catch (NullPointerException e) {
            throw new NullPointerException("[GUITabs] No panel with that name or type in this tabset!");
        }
    }

    /**
     * Resets the internal state of the tab manager
     */
    public void removeAllTabs() {
        this.panels.clear();
        this.currentTab = null;

        this.contentContainer.removeAll();
        resetTabButtonDisplay();

        this.revalidate();
        this.repaint();
    }

    /**
     * Which tab is the user currently looking at
     * @return The string name as used in insertTab()
     */
    public String getCurrentTab() {
        return currentTab;
    }

    /**
     * For any extra controller logic you want to add to the screen buttons
     * @return Returns a mapping of a screen name to its button object
     */
    public Map<String, JButton> getTabButtons() {
        List<String> panelNames = panels.keySet().stream().toList();
        List<JButton> panelButtons = panels.values().stream().map(Triplet::getValue2).toList();
        Map<String, JButton> res = new HashMap<>();
        IntStream.range(0, panelNames.size()).boxed().forEach(i -> res.put(panelNames.get(i), panelButtons.get(i)));
        return res;
    }
}
