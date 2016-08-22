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
package org.eclipse.che.api.workspace.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.testng.annotations.Test;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Yevhenii Voevodin
 */
public class WorkspaceConfigAdapterTest {

    private static final String OLD_CONTENT = "{\n" +
                                              "  \"name\": \"default\",\n" +
                                              "  \"defaultEnv\": \"dev-env\",\n" +
                                              "  \"description\": \"This is workspace description\",\n" +
                                              "  \"environments\": [\n" +
                                              "    {\n" +
                                              "      \"name\": \"dev-env\",\n" +
                                              "      \"machineConfigs\": [\n" +
                                              "        {\n" +
                                              "          \"name\": \"dev\",\n" +
                                              "          \"type\": \"docker\",\n" +
                                              "          \"dev\": true,\n" +
                                              "          \"limits\": {\n" +
                                              "            \"ram\": 2048\n" +
                                              "          },\n" +
                                              "          \"source\": {\n" +
                                              "            \"location\": \"https://somewhere/Dockerfile\",\n" +
                                              "            \"type\": \"dockerfile\"\n" +
                                              "          },\n" +
                                              "          \"envVariables\": {\n" +
                                              "            \"env1\": \"value1\",\n" +
                                              "            \"env2\": \"value2\"\n" +
                                              "          },\n" +
                                              "          \"servers\": [\n" +
                                              "            {\n" +
                                              "              \"ref\": \"ref\",\n" +
                                              "              \"port\": \"9090/udp\",\n" +
                                              "              \"protocol\": \"protocol\",\n" +
                                              "              \"path\": \"/any/path\"\n" +
                                              "            }\n" +
                                              "          ]\n" +
                                              "        },\n" +
                                              "        {\n" +
                                              "          \"name\": \"db\",\n" +
                                              "          \"type\": \"docker\",\n" +
                                              "          \"dev\": false,\n" +
                                              "          \"limits\": {\n" +
                                              "            \"ram\": 2048\n" +
                                              "          },\n" +
                                              "          \"source\": {\n" +
                                              "            \"type\": \"image\",\n" +
                                              "            \"location\": \"codenvy/ubuntu_jdk8\"\n" +
                                              "          },\n" +
                                              "          \"servers\": [\n" +
                                              "            {\n" +
                                              "              \"ref\": \"ref\",\n" +
                                              "              \"port\": \"3311/tcp\",\n" +
                                              "              \"protocol\": \"protocol\",\n" +
                                              "              \"path\": \"/any/path\"\n" +
                                              "            }\n" +
                                              "          ]\n" +
                                              "        }\n" +
                                              "      ]\n" +
                                              "    }\n" +
                                              "  ]\n" +
                                              "}\n";

    @Test
    public void testWorkspaceConfigAdaptation() throws Exception {
        final JsonElement root = new JsonParser().parse(OLD_CONTENT);
        final JsonObject newConfig = new WorkspaceConfigAdapter().adapt(root.getAsJsonObject());

        // The type of environments must be changed from array to map
        assertTrue(newConfig.has("environments"), "contains environments object");
        assertTrue(newConfig.get("environments").isJsonObject(), "environments is json object");

        // Environment must be moved out of the environment object
        final JsonObject environmentsObj = newConfig.get("environments").getAsJsonObject();
        assertTrue(environmentsObj.has("dev-env"), "'dev-env' is present in environments list");
        assertTrue(environmentsObj.get("dev-env").isJsonObject(), "'dev-env' is json object");

        final JsonObject environmentObj = environmentsObj.get("dev-env").getAsJsonObject();
        // 'machineConfigs' -> 'machines'
        assertTrue(environmentObj.has("machines"), "'machines' are present in environment object");
        assertTrue(environmentObj.get("machines").isJsonObject(), "'machines' is json object");
        final JsonObject machinesObj = environmentObj.get("machines").getAsJsonObject();
        assertEquals(machinesObj.entrySet().size(), 2, "machines size");

        // check 'dev' machine
        assertTrue(machinesObj.has("dev"), "'machines' contains machine with name 'dev-machine'");
        assertTrue(machinesObj.get("dev").isJsonObject(), "dev machine is json object");
        final JsonObject devMachineObj = machinesObj.get("dev").getAsJsonObject();
        assertTrue(devMachineObj.has("servers"), "dev machine contains servers field");
        assertTrue(devMachineObj.get("servers").isJsonObject(), "dev machine servers is json object");
        final JsonObject devMachineServersObj = devMachineObj.get("servers").getAsJsonObject();
        assertTrue(devMachineServersObj.has("ref"), "contains servers with reference 'ref'");
        assertTrue(devMachineServersObj.get("ref").isJsonObject(), "server is json object");
        final JsonObject devMachineServerObj = devMachineServersObj.get("ref").getAsJsonObject();
        assertEquals(devMachineServerObj.get("port").getAsString(), "9090/udp");
        assertEquals(devMachineServerObj.get("protocol").getAsString(), "protocol");
        assertTrue(devMachineObj.has("agents"), "dev machine has agents");

        // check 'db' machine
        assertTrue(machinesObj.has("db"), "'machines' contains machine with name 'db'");
        assertTrue(machinesObj.get("db").isJsonObject(), "db machine is json object");
        final JsonObject dbMachineObj = machinesObj.get("db").getAsJsonObject();
        assertTrue(dbMachineObj.has("servers"), "db machine contains servers field");
        assertTrue(dbMachineObj.get("servers").isJsonObject(), "db machine servers is json object");
        final JsonObject dbMachineServersObj = dbMachineObj.get("servers").getAsJsonObject();
        assertTrue(dbMachineServersObj.has("ref"), "contains servers with reference 'ref'");
        assertTrue(dbMachineServersObj.get("ref").isJsonObject(), "server is json object");
        final JsonObject dbMachineServer = dbMachineServersObj.get("ref").getAsJsonObject();
        assertEquals(dbMachineServer.get("port").getAsString(), "3311/tcp");
        assertEquals(dbMachineServer.get("protocol").getAsString(), "protocol");

        // check environment recipe
        assertTrue(environmentObj.has("recipe"), "environment contains recipe");
        assertTrue(environmentObj.get("recipe").isJsonObject(), "environment recipe is json object");
        final JsonObject recipeObj = environmentObj.get("recipe").getAsJsonObject();
        assertEquals(recipeObj.get("type").getAsString(), "compose");
        assertEquals(recipeObj.get("contentType").getAsString(), "application/x-yaml");
        assertEquals(recipeObj.get("content").getAsString(), "services:\n" +
                                                             "  dev:\n" +
                                                             "    build:\n" +
                                                             "      context: https://somewhere/Dockerfile\n" +
                                                             "    mem_limit: 2147483648\n" +
                                                             "    environment:\n" +
                                                             "      - env1=value1\n" +
                                                             "      - env2=value2\n" +
                                                             "  db:\n" +
                                                             "    mem_limit: 2147483648\n" +
                                                             "    image: codenvy/ubuntu_jdk8\n");
    }

    private static class Service extends HashMap<String, Object> {
        public Service setMemoryLimit(int memoryLimit) {
            put("mem_limit", memoryLimit);
            return this;
        }
    }

    private static class Build {

    }
}
