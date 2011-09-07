package org.eclipse.checkstyle.configuration.test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.sf.eclipsecs.core.config.ICheckConfiguration;
import net.sf.eclipsecs.core.config.ResolvableProperty;
import net.sf.eclipsecs.core.projectconfig.IProjectConfiguration;
import net.sf.eclipsecs.core.projectconfig.ProjectConfigurationFactory;
import net.sf.eclipsecs.core.projectconfig.ProjectConfigurationWorkingCopy;
import net.sf.eclipsecs.core.util.CheckstylePluginException;

import org.eclipse.checkstyle.configurator.Activator;
import org.eclipse.checkstyle.configurator.impl.CheckstyleProjectConfigurator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;
import org.junit.Test;

public class CheckstyleProjectConfiguratorTest {

    protected final static String PROJECT_NAME = "checkstyle.configurator.test";

    protected IProject createProject(final String name) {

        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IWorkspaceRoot root = workspace.getRoot();
        final IProject project = root.getProject(name);
        try {
            project.create(null);
            project.open(null);
        } catch (final CoreException e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    @Test
    public void removeAndAddNewFileSetTest() {
        final CheckstyleProjectConfigurator checkstyleProjectConfigurator = new CheckstyleProjectConfigurator();
        final IProject project = createProject(PROJECT_NAME);
        try {
            final List<String> compileSourceRoots = new ArrayList<String>();
            compileSourceRoots.add(".");
            IProjectConfiguration projectConfig;
            projectConfig = ProjectConfigurationFactory.getConfiguration(project);
            final ProjectConfigurationWorkingCopy workingCopy = new ProjectConfigurationWorkingCopy(projectConfig);
            final URL ruleSet = Activator.getDefault().getBundle().getResource("resource/rules");
            final ICheckConfiguration checkConfig = checkstyleProjectConfigurator
                    .createOrUpdateLocalCheckConfiguration(project, workingCopy, ruleSet, "config test");
            checkstyleProjectConfigurator.removeAndAddNewFileSet(project, compileSourceRoots, workingCopy, checkConfig);
            Assert.assertEquals(workingCopy.getFileSets().size(), 1);
            Assert.assertEquals(workingCopy.getFileSets().get(0).getName(), CheckstyleProjectConfigurator.JAVA_SOURCE);
        } catch (final CheckstylePluginException e) {
            throw new RuntimeException(e);
        } finally {
            deleteProject(project);
        }
    }

    @Test
    public void addCustomPropertiesTest() {
        final CheckstyleProjectConfigurator checkstyleProjectConfigurator = new CheckstyleProjectConfigurator();
        final IProject project = createProject(PROJECT_NAME);
        try {
            final List<String> compileSourceRoots = new ArrayList<String>();
            compileSourceRoots.add(".");
            IProjectConfiguration projectConfig;
            projectConfig = ProjectConfigurationFactory.getConfiguration(project);
            final ProjectConfigurationWorkingCopy workingCopy = new ProjectConfigurationWorkingCopy(projectConfig);
            final URL ruleSet = Activator.getDefault().getBundle().getResource("resource/rules");
            final ICheckConfiguration checkConfig = checkstyleProjectConfigurator
                    .createOrUpdateLocalCheckConfiguration(project, workingCopy, ruleSet, "config test");
            final Properties properties = new Properties();
            properties.setProperty("test1", "test1");
            checkConfig.getResolvableProperties().add(new ResolvableProperty("test2", "test3"));
            checkstyleProjectConfigurator.addCustomProperties(properties, checkConfig);
            Assert.assertEquals(checkConfig.getResolvableProperties().size(), 2);
            Assert.assertEquals(checkConfig.getResolvableProperties().get(1).getPropertyName(), "test1");
            Assert.assertEquals(checkConfig.getResolvableProperties().get(0).getPropertyName(), "checkstyle.cache.file");
        } catch (final CheckstylePluginException e) {
            throw new RuntimeException(e);
        } finally {
            deleteProject(project);
        }
    }

    @Test
    public void locateRuleSetTest() {
        final CheckstyleProjectConfigurator checkstyleProjectConfigurator = new CheckstyleProjectConfigurator();
        final URL ruleSet = Activator.getDefault().getBundle().getResource("resource/rules");
        final String srulesset = Activator.getDefault().getBundle().getResource("resource/rules").toString();
        final URL url = checkstyleProjectConfigurator.locateRuleSet(srulesset);
        Assert.assertEquals(ruleSet, url);
    }

    @Test
    public void createOrUpdateLocalCheckConfigurationTest() {
        final CheckstyleProjectConfigurator checkstyleProjectConfigurator = new CheckstyleProjectConfigurator();
        final IProject project = createProject(PROJECT_NAME);
        try {
            IProjectConfiguration projectConfig;
            projectConfig = ProjectConfigurationFactory.getConfiguration(project);
            final ProjectConfigurationWorkingCopy workingCopy = new ProjectConfigurationWorkingCopy(projectConfig);
            final URL ruleSet = Activator.getDefault().getBundle().getResource("resource/rules");
            final ICheckConfiguration checkConfig = checkstyleProjectConfigurator
                    .createOrUpdateLocalCheckConfiguration(project, workingCopy, ruleSet, "config test");
            Assert.assertEquals(checkConfig.getName(), "config test");
            Assert.assertEquals(checkConfig.getLocation(), ruleSet.toExternalForm());
        } catch (final CheckstylePluginException e) {
            throw new RuntimeException(e);
        } finally {
            deleteProject(project);
        }
    }

    protected void deleteProject(final IProject project) {
        try {
            project.close(new NullProgressMonitor());
            project.delete(true, true, new NullProgressMonitor());
        } catch (final CoreException e) {
            throw new RuntimeException(e);
        }
    }
}
