package org.eclipse.checkstyle.configurator.api;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Plugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface ICheckstyleProjectConfigurator {

    public URL locateRuleSet(final String location);

    public void updateProjectConfiguration(final IProject project, final URL ruleSet, final Properties properties,
            final List<String> compileSource, final String configurationName, final IProgressMonitor monitor)
            throws CoreException;

    public boolean pluginChanged(final Plugin current, final Plugin old);

    public boolean urlChanged(final URL current, final URL old);
}
