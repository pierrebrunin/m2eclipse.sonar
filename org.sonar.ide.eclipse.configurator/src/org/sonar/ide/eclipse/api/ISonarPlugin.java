package org.sonar.ide.eclipse.api;

import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

public interface ISonarPlugin {

    public String configure(final ProjectConfigurationRequest projectConfigurationRequest, final URL remoteRulesSet,
            final IProgressMonitor progressMonitor);

    public void mavenProjectChanged(final MavenProjectChangedEvent event, final URL remoteRulesSet,
            final IProgressMonitor monitor);

}
