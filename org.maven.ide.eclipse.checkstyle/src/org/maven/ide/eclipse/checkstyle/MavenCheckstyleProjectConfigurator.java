//============================================================================
//
// Copyright (C) 2008  Nicolas De loof
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//============================================================================

package org.maven.ide.eclipse.checkstyle;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import net.sf.eclipsecs.core.nature.CheckstyleNature;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.checkstyle.configurator.Activator;
import org.eclipse.checkstyle.configurator.api.ICheckstyleProjectConfigurator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

/**
 * Configure eclipse-cs on maven project import, if the maven project has
 * maven-checkstyle-project configured.
 * <p>
 * The configurator create (or update) a remote configuration that uses the
 * configLocation set in maven as an URL. If the configLocation points to a
 * dependency resource, it will be resolved using the special <code>jar:!</code>
 * URL syntax.
 */
public class MavenCheckstyleProjectConfigurator extends AbstractProjectConfigurator {

    /** Checkstyle ruleset name to match maven's one. */
    private static final String CONFIGURATION_NAME = "maven-chekstyle-plugin";

    /** checkstyle maven plugin groupId. */
    private static final String CHECKSTYLE_PLUGIN_GROUPID = "org.apache.maven.plugins";

    /** checkstyle maven plugin artifactId. */
    private static final String CHECKSTYLE_PLUGIN_ARTIFACTID = "maven-checkstyle-plugin";

    private ICheckstyleProjectConfigurator checkstyleProjectConfigurator = Activator.getDefault()
            .getCheckstyleProjectConfigurator();

    /**
     * {@inheritDoc}
     * 
     * @see org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator#configure(org.apache.maven.embedder.MavenEmbedder,
     *      org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest,
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void configure(final ProjectConfigurationRequest request, final IProgressMonitor monitor)
            throws CoreException {
        configure(request.getMavenProject(), request.getProject(), monitor);
    }

    private void configure(final MavenProject mavenProject, final IProject project, final IProgressMonitor monitor)
            throws CoreException {
        monitor.beginTask("Checkstyle configuration update", 2);
        try {
            final Plugin plugin = getCheckstylePlugin(mavenProject);
            if (plugin != null) {
                final URL ruleset = getCheckstyleConfiguration(mavenProject, plugin, monitor);
                final Properties properties = extractCustomProperties(plugin);
                final List<String> compileSource = mavenProject.getCompileSourceRoots();
                checkstyleProjectConfigurator.updateProjectConfiguration(project, ruleset, properties, compileSource,
                        CONFIGURATION_NAME, monitor);
                addNature(project, CheckstyleNature.NATURE_ID, monitor);
            } else {
                // TODO remove CheckStyle Nature
            }
        } finally {
            monitor.done();
        }
    }

    /* Return true if contents are modified, or new */
    private URL getCheckstyleConfiguration(final MavenProject mavenProject, final Plugin mavenPlugin,
            final IProgressMonitor monitor) throws CoreException {

        final String configLocation = extractMavenConfiguration(mavenPlugin, "configLocation");
        if (configLocation != null) {
            return checkstyleProjectConfigurator.locateRuleSet(configLocation);
        }

        return getSunCheckStyleConfiguration(mavenPlugin, monitor);
    }

    /**
     * Return sun checkstyle configuration.
     * 
     * @param mavenPlugin
     *            Instance of maven-checkstyle-plugin
     * @param monitor
     * @return null if conf not found or load
     * @throws CoreException
     */
    private URL getSunCheckStyleConfiguration(final Plugin mavenPlugin, final IProgressMonitor monitor)
            throws CoreException {
        final Artifact artifact = maven.resolve(mavenPlugin.getGroupId(), mavenPlugin.getArtifactId(),
                mavenPlugin.getVersion(), "jar", null, maven.getPluginArtifactRepositories(), monitor);
        try {
            URL url = new URL("jar:" + artifact.getFile().toURI().toURL().toString() + "!/config/sun_checks.xml");
            try {
                url.openConnection();
                return url;
            } catch (IOException e) {
                MavenLogger.log("Failed to load sun checks configuration", e);
            }
        } catch (MalformedURLException e) {
            MavenLogger.log("Failed to load sun checks configuration", e);
        }
        return null;
    }

    /**
     * Extract the configured properties from the checkstyle configuration
     * 
     * @see http 
     *      ://maven.apache.org/plugins/maven-checkstyle-plugin/examples/custom
     *      -property-expansion.html
     */
    private Properties extractCustomProperties(Plugin plugin) {
        Properties properties = new Properties();
        String propertiesLocation = extractMavenConfiguration(plugin, "propertiesLocation");
        if (propertiesLocation != null) {
            final URL url = checkstyleProjectConfigurator.locateRuleSet(propertiesLocation);
            if (url == null) {
                console.logError("Failed to resolve propertiesLocation " + propertiesLocation);
            } else {
                try {
                    properties.load(url.openStream());
                } catch (IOException e) {
                    console.logError("Failed to load properties from " + propertiesLocation);
                }
            }
        }

        String propertyExpansion = extractMavenConfiguration(plugin, "propertyExpansion");
        if (propertyExpansion != null) {
            try {
                properties.load(new StringReader(propertyExpansion));
            } catch (IOException e) {
                console.logError("Failed to parser checkstyle propertyExpansion " + propertyExpansion);
            }
        }
        return properties;
    }

    /**
     * Find (if exist) the maven-checkstyle-plugin configuration in the
     * mavenProject
     */
    private Plugin getCheckstylePlugin(final MavenProject mavenProject) {
        return mavenProject.getPlugin(CHECKSTYLE_PLUGIN_GROUPID + ":" + CHECKSTYLE_PLUGIN_ARTIFACTID);
    }

    @Override
    public void mavenProjectChanged(final MavenProjectChangedEvent event, final IProgressMonitor monitor)
            throws CoreException {
        super.mavenProjectChanged(event, monitor);
        if (event.getMavenProject().getMavenProject() != null) {

            final Plugin currentPlugin = getCheckstylePlugin(event.getMavenProject().getMavenProject());
            URL currentURL = null;
            if (currentPlugin != null) {
                currentURL = getCheckstyleConfiguration(event.getMavenProject().getMavenProject(), currentPlugin,
                        monitor);
            }

            Plugin oldPlugin = null;
            URL oldURL = null;
            if (event.getOldMavenProject() != null && event.getOldMavenProject().getMavenProject() != null) {
                oldPlugin = getCheckstylePlugin(event.getOldMavenProject().getMavenProject());
                if (oldPlugin != null) {
                    oldURL = getCheckstyleConfiguration(event.getOldMavenProject().getMavenProject(), oldPlugin,
                            monitor);
                }
            }
            if (checkstyleProjectConfigurator.pluginChanged(currentPlugin, oldPlugin) || checkstyleProjectConfigurator.urlChanged(currentURL, oldURL)) {
                configure(event.getMavenProject().getMavenProject(), event.getMavenProject().getProject(), monitor);
            }
        }
    }

    /**
     * Retrieve a configuration paramter from Maven plugin configuration
     * 
     * @param String
     *            the considered maven plugin
     * @param parameter
     *            the plugin configuration parameter name
     * @return the configured value, or null if not found
     */
    protected String extractMavenConfiguration(Plugin plugin, String parameter) {
        Object configuration = plugin.getConfiguration();
        if (configuration instanceof Xpp3Dom) {
            Xpp3Dom configDom = (Xpp3Dom) configuration;
            Xpp3Dom parameterValue = configDom.getChild(parameter);
            if (parameterValue != null) {
                return parameterValue.getValue();
            }
        }
        return null;
    }
}