package org.sonar.ide.eclipse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.sonar.ide.eclipse"; //$NON-NLS-1$

    public static final String SONAR_M2ECLIPSE_CONFIGURATOR = "org.sonar.ide.eclipse.configurator";

    // The shared instance
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
     * )
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    private List<IConfigurationElement> sonarPlugins = null;

    public List<IConfigurationElement> getSonarExtension() {
        if (sonarPlugins == null) {
            sonarPlugins = new ArrayList<IConfigurationElement>();
            final IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
                    Activator.SONAR_M2ECLIPSE_CONFIGURATOR);
            for (final IConfigurationElement e : config) {
                sonarPlugins.add(e);
            }
        }
        return sonarPlugins;
    }
}
