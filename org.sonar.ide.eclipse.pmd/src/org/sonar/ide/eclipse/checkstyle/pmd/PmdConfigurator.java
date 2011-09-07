package org.sonar.ide.eclipse.checkstyle.pmd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import net.sourceforge.pmd.eclipse.plugin.PMDPlugin;
import net.sourceforge.pmd.eclipse.runtime.builder.PMDNature;
import net.sourceforge.pmd.eclipse.runtime.properties.IProjectProperties;
import net.sourceforge.pmd.eclipse.runtime.properties.PropertiesException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.api.ISonarPlugin;

public class PmdConfigurator implements ISonarPlugin {

    @Override
    public String configure(final ProjectConfigurationRequest projectConfigurationRequest, final URL url, final IProgressMonitor progressMonitor) {
        try {
            final InputStream inputStream = url.openStream();
            storeRuleSet(projectConfigurationRequest.getProject(), inputStream);
            MavenLogger.log("Configure pmd from ruleSet " + url);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return PMDNature.PMD_NATURE;
    }

    @Override
    public void mavenProjectChanged(final MavenProjectChangedEvent mavenProjectChangedEvent, final URL url, final IProgressMonitor progressMonitor) {

    }

    /**
     * Update rules with inputstreams.
     * 
     * @param inputStream
     *            rules
     * @return instance of rules
     */
    protected void storeRuleSet(final IProject project, final InputStream inputStream) {
        final byte[] buffer = new byte[1024];
        try {
            final URI uri = URI.create(project.getLocationURI() + "/.ruleset");
            final FileWriter fileWriter = new FileWriter(new File(uri));
            try {
                int i = 0;
                while ((i = inputStream.read(buffer)) != -1) {
                    fileWriter.write(new String(buffer, 0, i));
                }
            } finally {
                fileWriter.flush();
                fileWriter.close();
            }
            final IProjectProperties projectProperties = PMDPlugin.getDefault().getPropertiesManager().loadProjectProperties(project);
            projectProperties.setPmdEnabled(true);
            projectProperties.setRuleSetFile(uri.getPath());
            projectProperties.setNeedRebuild(true);
            projectProperties.setRuleSetStoredInProject(true);
            PMDPlugin.getDefault().getPropertiesManager().storeProjectProperties(projectProperties);
        } catch (final PropertiesException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}