/*
 * #%L
 * JBossOSGi Deployment
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.osgi.deployment.deployer;

import static org.jboss.osgi.deployment.internal.DeploymentLogger.LOGGER;
import static org.jboss.osgi.deployment.internal.DeploymentMessages.MESSAGES;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

/**
 * A {@link DeployerService} that installs/uninstalls the bundles directly on the OSGi framework.
 *
 * @author thomas.diesler@jboss.com
 * @since 27-May-2009
 */
public class SystemDeployerService implements DeployerService {
    private final BundleContext context;
    private final StartLevel startLevel;

    public SystemDeployerService(BundleContext context) {
        if (context == null)
            throw MESSAGES.illegalArgumentNull("context");

        this.context = context;

        ServiceReference sref = context.getServiceReference(StartLevel.class.getName());
        this.startLevel = sref != null ? (StartLevel) context.getService(sref) : null;
    }

    @Override
    public Bundle deploy(Deployment dep) throws BundleException {
        Bundle bundle = deployInternal(dep);
        startInternal(dep);
        return bundle;
    }

    @Override
    public Bundle undeploy(Deployment dep) throws BundleException {
        return undeployInternal(dep);
    }

    @Override
    public void deploy(Deployment[] depArr) throws BundleException {
        // Install bundle deployments
        for (Deployment dep : depArr)
            deployInternal(dep);

        // Start the installed bundles
        for (Deployment dep : depArr)
            startInternal(dep);
    }

    @Override
    public void undeploy(Deployment[] depArr) throws BundleException {
        for (Deployment dep : depArr)
            undeployInternal(dep);
    }

    private Bundle deployInternal(Deployment dep) throws BundleException {
        LOGGER.debugf("Deploy: %s", dep);
        Bundle bundle = installBundle(dep);
        dep.addAttachment(Bundle.class, bundle);

        Integer level = dep.getStartLevel();
        if (startLevel != null && level != null && level > 0)
            startLevel.setBundleStartLevel(bundle, level);

        return bundle;
    }

    private void startInternal(Deployment dep) throws BundleException {
        Bundle bundle = dep.getAttachment(Bundle.class);
        if (bundle != null && dep.isAutoStart()) {
            LOGGER.debugf("Start: %s", bundle);

            // Added support for Bundle.START_ACTIVATION_POLICY on start
            // http://issues.apache.org/jira/browse/FELIX-1317
            // bundle.start(Bundle.START_ACTIVATION_POLICY);

            bundle.start();
        }
    }

    private Bundle undeployInternal(Deployment dep) {
        LOGGER.debugf("Undeploy: %s", dep);
        Bundle bundle = dep.getAttachment(Bundle.class);
        if (bundle == null) {
            LOGGER.warnCannotObtainBundleForDeployment(dep);
            return null;
        }

        try {
            if (bundle.getState() != Bundle.UNINSTALLED) {
                LOGGER.debugf("Uninstall: %s", bundle);
                uninstallBundle(dep, bundle);
            }
        } catch (Throwable ex) {
            LOGGER.warnCannotUninstallBundleForDeployment(ex, dep);
        }
        return bundle;
    }

    protected Bundle installBundle(Deployment dep) throws BundleException {
        return context.installBundle(dep.getLocation());
    }

    protected void uninstallBundle(Deployment dep, Bundle bundle) throws BundleException {
        bundle.uninstall();
    }
}