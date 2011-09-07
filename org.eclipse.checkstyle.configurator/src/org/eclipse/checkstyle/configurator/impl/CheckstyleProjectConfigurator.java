package org.eclipse.checkstyle.configurator.impl;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import net.sf.eclipsecs.core.CheckstylePlugin;
import net.sf.eclipsecs.core.builder.CheckstyleBuilder;
import net.sf.eclipsecs.core.builder.CheckstyleMarker;
import net.sf.eclipsecs.core.config.CheckConfigurationWorkingCopy;
import net.sf.eclipsecs.core.config.ICheckConfiguration;
import net.sf.eclipsecs.core.config.ICheckConfigurationWorkingSet;
import net.sf.eclipsecs.core.config.ResolvableProperty;
import net.sf.eclipsecs.core.config.configtypes.ConfigurationTypes;
import net.sf.eclipsecs.core.config.configtypes.IConfigurationType;
import net.sf.eclipsecs.core.projectconfig.FileMatchPattern;
import net.sf.eclipsecs.core.projectconfig.FileSet;
import net.sf.eclipsecs.core.projectconfig.IProjectConfiguration;
import net.sf.eclipsecs.core.projectconfig.ProjectConfigurationFactory;
import net.sf.eclipsecs.core.projectconfig.ProjectConfigurationWorkingCopy;
import net.sf.eclipsecs.core.util.CheckstylePluginException;

import org.apache.maven.model.Plugin;
import org.eclipse.checkstyle.configurator.api.ICheckstyleProjectConfigurator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.core.MavenLogger;

/**
 * Specialization of AbstractProjectConfigurator for mavenPlugin related tasks.
 * 
 * @author <a href="mailto:nicolas@apache.org">Nicolas De loof</a>
 */
public class CheckstyleProjectConfigurator implements ICheckstyleProjectConfigurator {

    /** Name of source fileset. **/
    public static final String JAVA_SOURCE = "java-source";


    /**
     * Load a ruleset by trying various load strategies.
     */
    public URL locateRuleSet(final String location) {
        // Try filesystem
        final File file = new File(location);
        if (file.exists()) {
            try {
                // uh uh uh :)
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                // ??
            }
        }

        // try a url
        try {
            final URL url = new URL(location);
            url.openStream();
            return url;

        } catch (MalformedURLException e) {
            // Not a valid URL
        } catch (Exception e) {
            // Valid URL but does not exist
        }
        return null;
    }

    /**
     * Retrieve a pre-existing LocalCheckConfiguration for maven to eclipse-cs
     * integration, or create a new one
     */
    public ICheckConfiguration createOrUpdateLocalCheckConfiguration(final IProject project,
            final ProjectConfigurationWorkingCopy projectConfig, final URL ruleSet, final String configurationName)
            throws CheckstylePluginException {

        /** IConfigurationType for remote configuration. */
        final IConfigurationType remoteConfigurationType = ConfigurationTypes.getByInternalName("remote");
        
        MavenLogger.log("Configure checkstyle from ruleSet " + ruleSet);
        final ICheckConfigurationWorkingSet workingSet = projectConfig.getLocalCheckConfigWorkingSet();

        CheckConfigurationWorkingCopy workingCopy = null;
        // Try to retrieve an existing checkstyle configuration to be updated
        final CheckConfigurationWorkingCopy[] workingCopies = workingSet.getWorkingCopies();
        if (workingCopies != null) {
            for (final CheckConfigurationWorkingCopy copy : workingCopies) {
                if (configurationName.equals(copy.getName())) {
                    if (remoteConfigurationType.equals(copy.getType())) {

                        MavenLogger.log("A local Checkstyle configuration allready exists with name "
                                + configurationName + ". It will be updated to maven plugin configuration");
                        workingCopy = copy;
                        break;
                    } else {
                        MavenLogger.log("A local Checkstyle configuration allready exists with name "
                                + configurationName + " with incompatible type");
                        return null;
                    }
                }
            }
        }

        if (ruleSet == null) {
            // remove existing config if ruleset not set
            if (workingCopy != null) {
                workingSet.removeCheckConfiguration(workingCopy);
                remoteConfigurationType.notifyCheckConfigRemoved(workingCopy);
                workingSet.store();
            }
            return null;
        }
        if (workingCopy == null) {
            // Create a fresh check config
            workingCopy = workingSet.newWorkingCopy(remoteConfigurationType);
            workingCopy.setName(configurationName);
            workingSet.addCheckConfiguration(workingCopy);
            workingSet.store();
        }

        workingCopy.setDescription("Maven checkstyle configuration");
        workingCopy.setLocation(ruleSet.toExternalForm());

        return workingCopy;
    }

    /**
     * Configure the Checkstyle FileSet to match the maven project
     * compileSourceRoots.
     */
    public void removeAndAddNewFileSet(final IProject project, final List<String> compileSourceRoots,
            final ProjectConfigurationWorkingCopy copy, final ICheckConfiguration checkConfig)
            throws CheckstylePluginException {
        // remove existing filesets
        copy.getFileSets().clear();

        if (checkConfig != null) {

            final URI projectURI = project.getLocationURI();
            final FileSet fileSet = generateCompileFileSet(compileSourceRoots, projectURI, JAVA_SOURCE, checkConfig);

            // add to copy filesets
            copy.getFileSets().add(fileSet);
        }
    }

    /**
     * Generate Fileset with source list.
     * 
     * @param compileSource
     *            source list
     * @param basedir
     *            basedir
     * @param fileSet
     * @throws CheckstylePluginException
     */
    protected FileSet generateCompileFileSet(final List<String> compileSource, final URI basedir,
            final String fileSetName, final ICheckConfiguration checkConfig) throws CheckstylePluginException {
        final FileSet fileSet = new FileSet(fileSetName, checkConfig);
        fileSet.setEnabled(true);

        final List<FileMatchPattern> patterns = new ArrayList<FileMatchPattern>();

        for (final String compileSourceRoot : compileSource) {

            final File compileSourceRootFile = new File(compileSourceRoot);
            final URI compileSourceRootURI = compileSourceRootFile.toURI();

            final String relativePath = basedir.relativize(compileSourceRootURI).getPath();
            patterns.add(new FileMatchPattern(relativePath));
        }

        fileSet.setFileMatchPatterns(patterns);
        return fileSet;
    }

    /**
     * Add custom properties.
     * 
     * @param mavenPlugin
     * @param checkConfig
     */
    public void addCustomProperties(final Properties properties, final ICheckConfiguration checkConfig) {
        // Custom properties

        properties.setProperty("checkstyle.cache.file", "${project_loc}/checkstyle-cachefile");

        List<ResolvableProperty> props = checkConfig.getResolvableProperties();
        props.clear();
        for (final Entry entry : properties.entrySet()) {
            props.add(new ResolvableProperty((String) entry.getKey(), (String) entry.getValue()));
        }
    }

    /**
     * Configure the eclipse Checkstyle plugin based on maven plugin
     * configuration and resources.
     */
    public void updateProjectConfiguration(final IProject project, final URL ruleSet, final Properties properties,
            final List<String> compileSource, final String configurationName, final IProgressMonitor monitor)
            throws CoreException {
        try {
            final IProjectConfiguration projectConfig = ProjectConfigurationFactory.getConfiguration(project);
            final ProjectConfigurationWorkingCopy workingCopy = new ProjectConfigurationWorkingCopy(projectConfig);
            workingCopy.setUseSimpleConfig(false);

            monitor.worked(1);
            final ICheckConfiguration checkConfig = createOrUpdateLocalCheckConfiguration(project, workingCopy,
                    ruleSet, configurationName);

            removeAndAddNewFileSet(project, compileSource, workingCopy, checkConfig);

            if (checkConfig != null) {
                addCustomProperties(properties, checkConfig);
            }

            monitor.worked(1);
            if (workingCopy.isDirty()) {
                workingCopy.store();
            }
            CheckstyleBuilder.buildProject(project);

        } catch (CheckstylePluginException cpe) {
            MavenLogger.log("Failed to configure Checkstyle plugin", cpe);
        }
    }

    /**
     * Check if maven-checkstyle is added/removed.
     * 
     * @param current
     *            plugin in current MavenProject
     * @param old
     *            plugin in previous MavenProject
     * @return true if maven-checkstyle is added/removed
     */
    public boolean pluginChanged(final Plugin current, final Plugin old) {
        // plugin added
        if (old == null && current != null) {
            return true;
        }
        // plugin removed
        if (current == null && old != null) {
            return true;
        }
        // no change
        return false;
    }

    /**
     * Check if update of checkstyle configuratrion is needed.
     * 
     * @param current
     *            url of current checkstyle configuration.
     * @param old
     *            url of previous checkstyle configuration.
     * @return true if url change.
     */
    public boolean urlChanged(final URL current, final URL old) {
        // new config added
        if (current != null && old == null) {
            return true;
        }
        // config removed
        if (current == null && old != null) {
            return true;
        }
        // existing config updated
        if (current != null && old != null) {
            return !current.equals(old);
        }
        return false;
    }
}
