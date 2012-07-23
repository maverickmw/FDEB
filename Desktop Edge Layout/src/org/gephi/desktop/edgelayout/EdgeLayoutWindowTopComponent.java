/*
 * Api package isn't the best place to host gui, probably.
 */
package org.gephi.desktop.edgelayout;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import javax.swing.DefaultComboBoxModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.gephi.desktop.preview.api.PreviewUIController;
import org.gephi.edgelayout.api.*;
import org.gephi.edgelayout.spi.EdgeLayout;
import org.gephi.edgelayout.spi.EdgeLayoutBuilder;
import org.gephi.edgelayout.spi.EdgeLayoutData;
import org.gephi.edgelayout.spi.EdgeLayoutProperty;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.preview.api.ManagedRenderer;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.plugin.renderers.ArrowRenderer;
import org.gephi.preview.plugin.renderers.EdgeRenderer;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.project.api.WorkspaceListener;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//org.gephi.desktop.edgelayout//EdgeLayoutWindow//EN",
autostore = false)
@TopComponent.Description(preferredID = "EdgeLayoutWindowTopComponent",
//iconBase="SET/PATH/TO/ICON/HERE", 
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "rankingmode", openAtStartup = true, roles = {"preview"})
@ActionID(category = "Window", id = "org.gephi.desktop.edgelayout.EdgeLayoutWindowTopComponent")
@ActionReference(path = "Menu/Window" /*
 * , position = 333
 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_EdgeLayoutWindowAction",
preferredID = "EdgeLayoutWindowTopComponent")
@Messages({
    "CTL_EdgeLayoutWindowAction=EdgeLayoutWindow",
    "CTL_EdgeLayoutWindowTopComponent=EdgeLayoutWindow Window",
    "HINT_EdgeLayoutWindowTopComponent=This is a EdgeLayoutWindow window"
})
public final class EdgeLayoutWindowTopComponent extends TopComponent implements PropertyChangeListener {

    private EdgeLayoutController controller;
    private EdgeLayoutModel model;
    private PropertyChangeListener listener;

    public EdgeLayoutWindowTopComponent() {
        listener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                regenerateSettings();
                regenerateRunButton();
                if (layoutComboBox.getSelectedItem() instanceof EdgeLayoutWrapper
                        && !controller.getModel().getSelectedBuilder().getName().equals(((EdgeLayoutWrapper) layoutComboBox.getSelectedItem()).builder.getName())) {
                    layoutComboBoxItemStateChanged(null);
                }
            }
        };

        initComponents();
        setName(Bundle.CTL_EdgeLayoutWindowTopComponent());
        setToolTipText(Bundle.HINT_EdgeLayoutWindowTopComponent());
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);
        controller = Lookup.getDefault().lookup(EdgeLayoutController.class);
        Lookup.getDefault().lookup(ProjectController.class).addWorkspaceListener(new WorkspaceListener() {

            public void initialize(Workspace workspace) {
                regenerateSettings();
                regenerateRunButton();
            }

            public void select(Workspace workspace) {
                model = workspace.getLookup().lookup(EdgeLayoutModel.class);
                model.addPropertyChangeListener(listener);
                if (model.getSelectedBuilder() != null) {
                    System.err.println("select and getselectedBuilder " + model.getSelectedBuilder().getName());
                } else {
                    System.err.println("select and null builder");
                }
                if (model.getSelectedBuilder() != null) {
                    for (int i = 0; i < layoutComboBox.getItemCount(); i++) {
                        if (layoutComboBox.getItemAt(i) instanceof EdgeLayoutWrapper
                                && ((EdgeLayoutWrapper) layoutComboBox.getItemAt(i)).getBuilder().equals(model.getSelectedBuilder())) {
                            layoutComboBox.setSelectedIndex(i);
                            System.err.println("select " + i);
                            break;
                        } else if (i + 1 == layoutComboBox.getItemCount()) {
                            System.err.println("fail :(");
                        }
                    }
                } else {
                    layoutComboBox.setSelectedIndex(0);
                }
                if (layoutComboBox.getSelectedItem() instanceof EdgeLayoutWrapper
                        && controller.getModel().getSelectedBuilder() != ((EdgeLayoutWrapper) layoutComboBox.getSelectedItem()).builder) {
                    layoutComboBoxItemStateChanged(null);
                }
                regenerateSettings();
                regenerateRunButton();
            }

            public void unselect(Workspace workspace) {
                model = workspace.getLookup().lookup(EdgeLayoutModel.class);
                model.removePropertyChangeListener(listener);
            }

            public void close(Workspace workspace) {
                layoutComboBox.setSelectedIndex(0);
            }

            public void disable() {
                model = null;
            }
        });
        Lookup.getDefault().lookup(EdgeLayoutController.class).addRefreshListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                Lookup.getDefault().lookup(PreviewUIController.class).refreshPreview();
            }
        });
        regenerateRunButton();
        regenerateSettings();
        regenerateCombobox();
    }

    private void regenerateSettings() {
        if (controller.getModel() != null && controller.getModel().getSelectedLayout() != null) {
            LayoutNode layoutNode;
            layoutNode = new LayoutNode(controller.getModel().getSelectedLayout());
            layoutNode.getPropertySets();
            ((PropertySheet) settingsPanel).setNodes(new Node[]{layoutNode});
        } else {
            ((PropertySheet) settingsPanel).setNodes(new Node[0]);
        }
        settingsPanel.setVisible(true);
    }

    private void regenerateCombobox() {
        ArrayList<EdgeLayoutBuilder> list = new ArrayList(Lookup.getDefault().lookupAll(EdgeLayoutBuilder.class));

        ArrayList sortedlist = new ArrayList(list.size());
        for (int i = 0; i < list.size(); i++) {
            sortedlist.add(new EdgeLayoutWrapper(list.get(i)));
        }
        Collections.sort(sortedlist);
        sortedlist.add(0, "--Choose a layout");
        DefaultComboBoxModel model = new DefaultComboBoxModel(sortedlist.toArray());
        layoutComboBox.setModel(model);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        runButton = new javax.swing.JButton();
        layoutComboBox = new javax.swing.JComboBox();
        settingsPanel = new PropertySheet();
        deleteButton = new javax.swing.JButton();
        generateGradientsButton = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(runButton, org.openide.util.NbBundle.getMessage(EdgeLayoutWindowTopComponent.class, "EdgeLayoutWindowTopComponent.runButton.text")); // NOI18N
        runButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runButtonActionPerformed(evt);
            }
        });

        layoutComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        layoutComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                layoutComboBoxItemStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(deleteButton, org.openide.util.NbBundle.getMessage(EdgeLayoutWindowTopComponent.class, "EdgeLayoutWindowTopComponent.deleteButton.text")); // NOI18N
        deleteButton.setToolTipText(org.openide.util.NbBundle.getMessage(EdgeLayoutWindowTopComponent.class, "EdgeLayoutWindowTopComponent.deleteButton.toolTipText")); // NOI18N
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(generateGradientsButton, org.openide.util.NbBundle.getMessage(EdgeLayoutWindowTopComponent.class, "EdgeLayoutWindowTopComponent.generateGradientsButton.text")); // NOI18N
        generateGradientsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateGradientsButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(deleteButton, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(layoutComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(generateGradientsButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(runButton, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(settingsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(layoutComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(runButton)
                    .addComponent(generateGradientsButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(settingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(deleteButton)
                .addGap(6, 6, 6))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void switchToGradientSubdividedRendererAndRefresh(boolean refresh) {
        ManagedRenderer[] renderers = Lookup.getDefault().lookup(PreviewController.class).getModel().getManagedRenderers();
        for (int i = 0; i < renderers.length; i++) {
            if (renderers[i].getRenderer() instanceof EdgeRenderer || renderers[i].getRenderer() instanceof ArrowRenderer
                    || renderers[i].getRenderer() instanceof SubdividedEdgeRenderer) {
                renderers[i].setEnabled(false);
            }
        }

        Lookup.getDefault().lookup(PreviewController.class).refreshPreview();
        boolean found = false;
        for (int i = 0; i < renderers.length; i++) {
            if (renderers[i].getRenderer() instanceof SubdividedEdgeGradientRenderer) {
                renderers[i].setEnabled(true);
                if (refresh) {
                    ((SubdividedEdgeGradientRenderer) renderers[i].getRenderer()).forcePreProcess(Lookup.getDefault().lookup(PreviewController.class).getModel());
                }
                found = true;
            }
        }
        renderers = Arrays.copyOf(renderers, renderers.length + 1);
        renderers[renderers.length - 1] = new ManagedRenderer(new SubdividedEdgeRenderer(), true);
        Lookup.getDefault().lookup(PreviewController.class).getModel().setManagedRenderers(renderers);

    }

    private void switchToSubdividedRenderer() {
        ManagedRenderer[] renderers = Lookup.getDefault().lookup(PreviewController.class).getModel().getManagedRenderers();
        for (int i = 0; i < renderers.length; i++) {
            if (renderers[i].getRenderer() instanceof EdgeRenderer || renderers[i].getRenderer() instanceof ArrowRenderer) {
                renderers[i].setEnabled(false);
            }
        }

        boolean found = false;
        for (int i = 0; i < renderers.length; i++) {
            if (renderers[i].getRenderer() instanceof SubdividedEdgeRenderer) {
                renderers[i].setEnabled(true);
                found = true;
            }
        }
        renderers = Arrays.copyOf(renderers, renderers.length + 1);
        renderers[renderers.length - 1] = new ManagedRenderer(new SubdividedEdgeRenderer(), true);
        Lookup.getDefault().lookup(PreviewController.class).getModel().setManagedRenderers(renderers);
    }

    private void switchToEdgeRenderer() {
        ManagedRenderer[] renderers = Lookup.getDefault().lookup(PreviewController.class).getModel().getManagedRenderers();
        for (int i = 0; i < renderers.length; i++) {
            if (renderers[i].getRenderer() instanceof EdgeRenderer || renderers[i].getRenderer() instanceof ArrowRenderer) {
                renderers[i].setEnabled(true);
            } else if (renderers[i].getRenderer() instanceof SubdividedEdgeRenderer) {
                renderers[i].setEnabled(false);
            }
        }
    }

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        EdgeLayout layout = (EdgeLayout) controller.getModel().getSelectedLayout();
        layout.removeLayoutData();
        switchToEdgeRenderer();
        /*
         * Note: had to make Desktop Preview public, to make
         * PreviewUIController.class accessible
         */
        //Lookup.getDefault().lookup(PreviewUIController.class).refreshPreview();
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void run() {
        if (controller.canExecute()) {
            switchToSubdividedRenderer();
            controller.executeLayout();
        }
    }

    private void stop() {
        if (controller.canStop()) {
            controller.stopLayout();
        }
    }

    private boolean layoutIsRunning() {
        return (controller.getModel() != null && controller.getModel().isRunning());
    }

    private void regenerateRunButton() {
        if (layoutIsRunning() != !layoutComboBox.isEnabled()) //i.e., it just changed and we should update preview
        {
            Lookup.getDefault().lookup(PreviewUIController.class).refreshPreview();
        }
        if (layoutIsRunning()) {
            runButton.setText("Stop");
            layoutComboBox.setEnabled(false);
            //Lookup.getDefault().lookup(PreviewController.class).render(Lookup.getDefault().lookup(PreviewController.class).getRenderTarget(RenderTarget.PROCESSING_TARGET)); TODO: find how to update preview
        } else {
            runButton.setText("Run");
            layoutComboBox.setEnabled(true);
        }
    }

    private void runButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runButtonActionPerformed
        regenerateRunButton();
        if (layoutIsRunning()) {
            stop();
        } else {
            run();
        }
        regenerateRunButton();
        regenerateSettings();
    }//GEN-LAST:event_runButtonActionPerformed

    private void layoutComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_layoutComboBoxItemStateChanged
        if (layoutComboBox.getSelectedItem() instanceof EdgeLayoutWrapper) {
            EdgeLayoutWrapper wrapper = (EdgeLayoutWrapper) layoutComboBox.getSelectedItem();
            controller.setLayout(wrapper.getBuilder().buildLayout());
            runButton.setEnabled(true);
        } else {
            runButton.setEnabled(false);
        }
        regenerateSettings();
    }//GEN-LAST:event_layoutComboBoxItemStateChanged
    private int counter = 0;
    private void generateGradientsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateGradientsButtonActionPerformed
        switchToGradientSubdividedRendererAndRefresh(true);
    }//GEN-LAST:event_generateGradientsButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton deleteButton;
    private javax.swing.JButton generateGradientsButton;
    private javax.swing.JComboBox layoutComboBox;
    private javax.swing.JButton runButton;
    private javax.swing.JPanel settingsPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        regenerateRunButton();
        regenerateSettings();
    }

    private class EdgeLayoutWrapper implements Comparable<EdgeLayoutWrapper> {

        EdgeLayoutBuilder builder;

        public EdgeLayoutWrapper(EdgeLayoutBuilder builder) {
            this.builder = builder;
        }

        public EdgeLayoutBuilder getBuilder() {
            return builder;
        }

        @Override
        public String toString() {
            return builder.getName().toString();
        }

        @Override
        public int compareTo(EdgeLayoutWrapper o) {
            return builder.getName().compareTo(o.getBuilder().getName());
        }
    }

    /**
     *
     * @author Mathieu Bastian
     */
    private class LayoutNode extends AbstractNode {

        private EdgeLayout layout;
        private PropertySet[] propertySets;

        public LayoutNode(EdgeLayout layout) {
            super(Children.LEAF);
            this.layout = layout;
            setName(layout.getBuilder().getName());
        }

        @Override
        public PropertySet[] getPropertySets() {
            if (propertySets == null) {
                try {
                    Map<String, Sheet.Set> sheetMap = new HashMap<String, Sheet.Set>();
                    System.err.println("layout has " + layout.getProperties().length + " properties");
                    for (EdgeLayoutProperty layoutProperty : layout.getProperties()) {
                        Sheet.Set set = sheetMap.get(layoutProperty.getCategory());
                        if (set == null) {
                            set = Sheet.createPropertiesSet();
                            set.setDisplayName(layoutProperty.getCategory());
                            sheetMap.put(layoutProperty.getCategory(), set);
                        }
                        set.put(layoutProperty.getProperty());
                    }
                    propertySets = sheetMap.values().toArray(new PropertySet[0]);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                    return null;
                }
            }
            return propertySets;
        }

        public EdgeLayout getLayout() {
            return layout;
        }
    }
}