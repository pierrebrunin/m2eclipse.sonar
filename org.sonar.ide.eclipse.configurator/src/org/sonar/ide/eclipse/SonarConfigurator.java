package org.sonar.ide.eclipse;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.api.ISonarPlugin;

/**
 * 
 * @author Pierre BRUNIN
 * 
 */
public class SonarConfigurator extends AbstractProjectConfigurator {

    /* keyt of maven-sonar-plugin */
    public final static String SONAR_PLUGIN_KEY = "org.codehaus.mojo:sonar-maven-plugin";

    /**
     * Foreach sonar extension, resolve permalink url and call project
     * configuration. This last step is implement in sonar extension.
     */
    @Override
    public void configure(ProjectConfigurationRequest projectConfigurationRequest, IProgressMonitor progressMonitor) throws CoreException {
        final MavenProject mavenProject = projectConfigurationRequest.getMavenProject();
        if (projectConfigurationRequest != null) {
            final Plugin sonar = getSonarPlugin(mavenProject);
            if (sonar != null) {
                // sonar.profile
                final String sonarProfile = mavenProject.getProperties().getProperty("sonar.profile");
                // sonar.host.url
                final String sonarHostUrl = mavenProject.getProperties().getProperty("sonar.host.url");
                if (sonarProfile == null) {
                    Activator.getDefault().getLog().log(new Status(Status.INFO, Activator.PLUGIN_ID, "Property sonar.profile not found"));
                }
                if (sonarHostUrl == null) {
                    Activator.getDefault().getLog().log(new Status(Status.INFO, Activator.PLUGIN_ID, "Property sonar.host.url not found"));
                }
                if (sonarProfile != null && sonarHostUrl != null) {
                    final List<IConfigurationElement> configurationElements = Activator.getDefault().getSonarExtension();
                    for (IConfigurationElement configurationElement : configurationElements) {
                        // plugin name (checkstyle, pmd, etc)
                        final String pluginName = configurationElement.getAttribute("plugin_name");
                        final URL ruleSet = resolveURLConfig(pluginName, sonarProfile, sonarHostUrl);
                        if (ruleSet != null) {
                            final ISonarPlugin sonarPlugin = (ISonarPlugin) configurationElement.createExecutableExtension("class");
                            final String nature = sonarPlugin.configure(projectConfigurationRequest, ruleSet, progressMonitor);
                            if (nature != null) {
                                addNature(projectConfigurationRequest.getMavenProjectFacade().getProject(), nature, progressMonitor);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Return sonar permalinks.
     * 
     * @param mavenProject
     *            mavenProjet give sonar.profile, sonar.host.url properties
     * @param configurationElement
     *            give plugin name (checkstyle, pmd, etc)
     * @return return permalink or null if is not accessible
     */
    private URL resolveURLConfig(final String pluginName, final String sonarProfile, final String sonarHostUrl) {
        if (pluginName == null) {
            Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, "Pluging name missing"));
            throw new IllegalArgumentException("Pluging name missing");
        }
        final String antislash = sonarHostUrl.endsWith("/") ? "" : "/";
        String sonarPermalink;
        try {
            sonarPermalink = sonarHostUrl + antislash + "profiles/export?format=" + pluginName + "&language=java&name="
                    + URLEncoder.encode(URLEncoder.encode(sonarProfile, "UTF-8"), "UTF-8");

            URL url = new URL(sonarPermalink);
            if (checkURl(url)) {
                return url;
            }
            return null;
        } catch (final InvalidRegistryObjectException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
        super.mavenProjectChanged(event, monitor);
    }

    /**
     * Return instance of sonar plugin
     * 
     * @param mavenProject
     *            Instance of Maven project
     * @return null if sonar plugin not found
     */
    public static Plugin getSonarPlugin(final MavenProject mavenProject) {
        for (final Plugin plugin : mavenProject.getPluginManagement().getPlugins()) {
            if (plugin.getKey().equals(SONAR_PLUGIN_KEY)) {
                return plugin;
            }
        }
        return mavenProject.getPlugin(SONAR_PLUGIN_KEY);
    }

    private boolean checkURl(final URL url) {
        try {
            url.openStream();
            return true;
        } catch (final MalformedURLException e) {
            Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
            return false;
        } catch (final Exception e) {
            Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
            return false;
        }
    }
}