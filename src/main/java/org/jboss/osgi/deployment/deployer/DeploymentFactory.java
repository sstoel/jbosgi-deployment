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

//$Id$

import org.jboss.osgi.deployment.internal.DeploymentImpl;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.osgi.vfs.VirtualFile;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

/**
 * A deployment factory.
 * 
 * @author thomas.diesler@jboss.com
 * @since 16-Oct-2009
 */
public class DeploymentFactory
{
   // Hide default ctor
   private DeploymentFactory()
   {
   }
   
   public static Deployment createDeployment(VirtualFile rootFile, String location, String symbolicName, Version version)
   {
      return new DeploymentImpl(rootFile, location, symbolicName, version);
   }
   
   public static Deployment createDeployment(BundleInfo info) throws BundleException
   {
      if (info == null)
         throw new IllegalArgumentException("Null info");
      return new DeploymentImpl(info.getRoot(), info.getLocation(), info.getSymbolicName(), info.getVersion());
   }
}