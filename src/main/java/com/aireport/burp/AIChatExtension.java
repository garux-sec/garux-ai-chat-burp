package com.aireport.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.aireport.burp.ui.ChatPanel;
import com.aireport.burp.ui.ContextMenuProvider;
import com.aireport.burp.ui.SettingsPanel;
import com.aireport.burp.ui.TabBadgeComponent;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class AIChatExtension implements BurpExtension {

    static final String NAME = "Garux AI Chat";

    private MontoyaApi    api;
    private SettingsPanel settingsPanel;
    private ChatPanel     chatPanel;
    private JPanel        mainTabPanel;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName(NAME);

        settingsPanel = new SettingsPanel(api);
        chatPanel     = new ChatPanel(api, settingsPanel);
        mainTabPanel  = buildMainTab();

        api.userInterface().registerSuiteTab(NAME, mainTabPanel);

        api.userInterface().registerContextMenuItemsProvider(
                new ContextMenuProvider(api, chatPanel)
        );

        // Inject red dot badge after Burp renders the tab
        SwingUtilities.invokeLater(this::injectTabBadge);

        api.logging().logToOutput("[" + NAME + "] Extension loaded successfully.");
    }

    private JPanel buildMainTab() {
        JPanel root = new JPanel(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("💬 Chat",     chatPanel);
        tabs.addTab("⚙ Settings", settingsPanel);

        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private void injectTabBadge() {
        Timer timer = new Timer(200, null);
        final int[] attempts = {0};

        timer.addActionListener(e -> {
            attempts[0]++;
            JTabbedPane burpTabs = findParentTabbedPane(mainTabPanel);

            if (burpTabs != null) {
                for (int i = 0; i < burpTabs.getTabCount(); i++) {
                    if (mainTabPanel.equals(burpTabs.getComponentAt(i))) {
                        TabBadgeComponent badge = new TabBadgeComponent(NAME);
                        burpTabs.setTabComponentAt(i, badge);
                        chatPanel.setTabBadge(badge);

                        // Clear badge when user clicks our tab
                        burpTabs.addChangeListener(ce -> {
                            if (burpTabs.getSelectedComponent() == mainTabPanel) {
                                badge.showBadge(false);
                            }
                        });

                        timer.stop();
                        api.logging().logToOutput("[" + NAME + "] Tab badge injected.");
                        return;
                    }
                }
            }

            if (attempts[0] >= 10) {
                timer.stop();
            }
        });

        timer.setRepeats(true);
        timer.start();
    }

    private static JTabbedPane findParentTabbedPane(Component comp) {
        Container parent = comp.getParent();
        while (parent != null) {
            if (parent instanceof JTabbedPane tp) return tp;
            parent = parent.getParent();
        }
        return null;
    }
}
