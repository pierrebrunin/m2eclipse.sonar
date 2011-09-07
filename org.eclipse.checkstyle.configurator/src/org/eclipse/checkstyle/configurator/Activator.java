package org.eclipse.checkstyle.configurator;

import org.eclipse.checkstyle.configurator.api.ICheckstyleProjectConfigurator;
import org.eclipse.checkstyle.configurator.impl.CheckstyleProjectConfigurator;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.checkstyle.configuration"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	private ICheckstyleProjectConfigurator checkstyleProjectConfigurator = null;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
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

	public ICheckstyleProjectConfigurator getCheckstyleProjectConfigurator() {
	    if(checkstyleProjectConfigurator == null) {
	        checkstyleProjectConfigurator = new CheckstyleProjectConfigurator();
	    }
	    return checkstyleProjectConfigurator;
	}
}
