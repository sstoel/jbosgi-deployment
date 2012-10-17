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
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.osgi.framework.Bundle;

/**
 * Logging Id ranges: 10500-10599
 *
 * https://docs.jboss.org/author/display/JBOSGI/JBossOSGi+Logging
 *
 * @author Thomas.Diesler@jboss.com
 */
@MessageBundle(projectCode = "JBOSGI")
public interface DeploymentMessages {

    DeploymentMessages MESSAGES = Messages.getBundle(DeploymentMessages.class);

    @Message(id = 10500, value = "%s is null")
    IllegalArgumentException illegalArgumentNull(String name);

    @Message(id = 10501, value = "Start level must be greater than one: %d")
    IllegalArgumentException illegalArgumentStartLevel(int startLevel);

    @Message(id = 10502, value = "Interceptor with no inputs should have been added already")
    IllegalStateException illegalStateInterceptorWithNoInputsAdded();

    @Message(id = 10503, value = "Cannot get invocation context for: %s")
    IllegalStateException illegalStateCannotObtainInvocationContext(Bundle bundle);

    @Message(id = 10504, value = "Cannot obtain root URL")
    IllegalStateException illegalStateCannotObtainRootURL(@Cause Throwable cause);

    @Message(id = 10505, value = "Cannot obtain root file")
    IllegalStateException illegalStateCannotObtainRootFile(@Cause Throwable cause);
}
