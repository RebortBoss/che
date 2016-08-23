/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.agent.server;

import com.google.inject.AbstractModule;

import org.eclipse.che.api.agent.server.impl.AgentRegistryImpl;
import org.eclipse.che.api.agent.server.impl.AgentRegistryUrlProviderImpl;

/**
 * @author Anatolii Bazko
 */
public class AgentModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AgentRegistryUrlProvider.class).to(AgentRegistryUrlProviderImpl.class);
        bind(AgentRegistry.class).to(AgentRegistryImpl.class);
        bind(AgentService.class);
    }
}
