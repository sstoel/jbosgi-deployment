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
package org.jboss.osgi.deployment;

import static org.jboss.osgi.deployment.DeploymentMessages.MESSAGES;

import java.util.Dictionary;
import java.util.Hashtable;

import org.jboss.osgi.deployment.deployer.DeployerService;
import org.jboss.osgi.deployment.deployer.SystemDeployerService;
import org.osgi.framework.BundleContext;

/**
 * An activator for the deployment services.
 * 
 * This can be used like a BundleActivator altough the deployment services 
 * are not installed as a bundle.
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Oct-2009
 */
public class DeploymentServicesActivator {
    public void start(BundleContext context) {
        if (context == null)
            throw MESSAGES.illegalArgumentNull("context");

        // Register the SystemDeployerService
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("provider", "system");
        SystemDeployerService deployerService = new SystemDeployerService(context);
        context.registerService(DeployerService.class.getName(), deployerService, props);
    }

    public void stop(BundleContext context) {
    }
}