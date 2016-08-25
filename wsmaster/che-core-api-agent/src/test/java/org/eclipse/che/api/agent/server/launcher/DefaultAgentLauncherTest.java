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
package org.eclipse.che.api.agent.server.launcher;

import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Anatolii Bazko
 */
@Listeners(value = {MockitoTestNGListener.class})
public class DefaultAgentLauncherTest {

    @Mock
    private Instance                        machine;
    @Mock
    private Agent                           agent;
    @Mock
    private InstanceProcess                 instanceProcess;

    private AgentLauncher agentLauncher;

    @BeforeMethod
    public void setUp() throws Exception {
        agentLauncher = new DefaultAgentLauncher(120000, 10);

        when(agent.getScript()).thenReturn("script1");
    }

    @Test
    public void shouldLaunchAgent() throws Exception {
        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);

        agentLauncher.launch(machine, agent);

//        verify(machineProcessManager).exec(anyString(), anyString(), commandCaptor.capture(), any());

        Command command = commandCaptor.getValue();
        assertEquals(command.getCommandLine(), "script1");
    }

    @Test(expectedExceptions = MachineException.class, expectedExceptionsMessageRegExp = "Start failed")
    public void executeScriptsShouldFailIfProcessFailed() throws Exception {
//        doThrow(new MachineException("Start failed")).when(machineProcessManager).exec(any(), any(), any(), any());

        agentLauncher.launch(machine, agent);
    }

}
