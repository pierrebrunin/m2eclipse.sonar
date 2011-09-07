package org.sonar.ide.eclipse.checkstyle.impl;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import net.sf.eclipsecs.core.nature.CheckstyleNature;

import org.apache.maven.project.MavenProject;
import org.eclipse.checkstyle.configurator.Activator;
import org.eclipse.checkstyle.configurator.api.ICheckstyleProjectConfigurator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.api.ISonarPlugin;

/**
 * This class add checkstyle rulesSet in project configuration.
 * 
 * @author Pierre BRUNIN
 * 
 */
public class SonarCheckstyleProjectConfigurator implements ISonarPlugin {

    public String configure(ProjectConfigurationRequest projectConfigurationRequest, URL remoteRulesSet,
            IProgressMonitor progressMonitor) {
        final MavenProject mavenProject = projectConfigurationRequest.getMavenProject();
        final IProject project = projectConfigurationRequest.getMavenProjectFacade().getProject();
        final ICheckstyleProjectConfigurator checkstyleProjectConfigurator = Activator.getDefault()
                .getCheckstyleProjectConfigurator();
        final Properties properties = new Properties();
        final List<String> compileSource = mavenProject.getCompileSourceRoots();
        try {
            checkstyleProjectConfigurator.updateProjectConfiguration(project, remoteRulesSet, properties,
                    compileSource, "sonar-rules", progressMonitor);
        } catch (final CoreException e) {
            throw new RuntimeException(e);
        }
        return CheckstyleNature.NATURE_ID;
    }

    public void mavenProjectChanged(MavenProjectChangedEvent event, URL remoteRulesSet, IProgressMonitor monitor) {
        // TODO Auto-generated method stub
    }

}
