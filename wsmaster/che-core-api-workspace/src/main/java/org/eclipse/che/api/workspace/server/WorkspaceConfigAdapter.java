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

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.eclipse.che.api.core.ConflictException;

import javax.inject.Singleton;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;

/**
 * Adapts an old workspace configuration object format to a new format.
 *
 * <pre>
 * Old workspace config format:
 * {
 *      "name" : "default",
 *      "defaultEnv" : "dev-env",
 *      "description" : "This is workspace description",
 *      "environments": [
 *          {
 *              "name": "dev-env",
 *              "machineConfigs": [
 *                  {
 *                      "name": "dev", <- goes to recipe content
 *                      "limits": {
 *                          "ram": 2048 <- in bytes
 *                      },
 *                      "source": { <- goes to recipe content
 *                          "location": "https://somewhere/Dockerfile",
 *                          "type": "dockerfile"
 *                      },
 *                      "type": "docker", <- will be defined by environment recipe type
 *                      "dev": true, <- if agents contain 'ws-agent'
 *                      "envVariables" : { <- goes to recipe content
 *                          "env1" : "value1",
 *                          "env2" : "value2
 *                      },
 *                      "servers" : [ <- goes to machine definition
 *                          {
 *                              {
 *                                  "ref" : "some_reference",
 *                                  "port" : "9090/udp",
 *                                  "protocol" : "some_protocol",
 *                                  "path" : "/some/path"
 *                              }
 *                          }
 *                      ]
 *                  }
 *                  {
 *                      "name" : "db",
 *                      "limits" : {
 *                          "ram": 2048 <- in bytes
 *                      },
 *                      "source" : {
 *                          "type" : "image",
 *                          "location" : "codenvy/ubuntu_jdk8"
 *                      },
 *                      "type" : "docker",
 *                      "dev" : false,
 *                      "servers" : [
 *                          {
 *                              "ref" : "db_server",
 *                              "port" : "3311/tcp",
 *                              "protocol" : "db-protocol",
 *                              "path" : "db-path"
 *                          }
 *                      ]
 *                  }
 *              ]
 *          }
 *      ],
 * }
 *
 * New workspace config format:
 * {
 *      "name" : "default",
 *      "defaultEnv" : "dev-env",
 *      "description" : "This is workspace description",
 *      "environments" : {
 *          "dev-env" : {
 *              "recipe" : {
 *                  "type" : "compose",
 *                  "contentType" : "application/x-yaml",
 *                  "content" : "
 *                      services :
 *                          dev-machine:
 *                              build:
 *                                  context: https://somewhere/Dockerfile
 *                              mem_limit: 2147483648
 *                              environment:
 *                                  - env1=value1
 *                                  - env2=value2
 *                          db:
 *                              image : codenvy/ubuntu_jdk8
 *                              mem_limit: 2147483648
 *                  "
 *              },
 *              "machines" : {
 *                  "dev-machine" : {
 *                      "agents" : [ "exec-agent", "ws-agent" ],
 *                      "servers" : {
 *                          "some_reference" : {
 *                              "port" : "9090/udp",
 *                              "protocol" : "some_protocol",
 *                              "properties" : {
 *                                  "prop1" : "value1"
 *                              }
 *                          }
 *                      }
 *                  },
 *                  "db" : {
 *                      "servers" : {
 *                          "db_server" : {
 *                              "port" : "3311/tcp",
 *                              "protocol" : "db-protocol",
 *                              "path" : "db-path"
 *                          }
 *                      }
 *                  }
 *              }
 *          }
 *      }
 * }
 * </pre>
 *
 * @author Yevhenii Voevodin
 */
@Singleton
public class WorkspaceConfigAdapter {

    public JsonObject adapt(JsonObject confSourceObj) throws ConflictException {
        final JsonArray oldEnvironmentsArr = confSourceObj.getAsJsonArray("environments");
        final JsonObject newEnvironmentsObj = new JsonObject();
        for (JsonElement oldEnvEl : oldEnvironmentsArr) {
            final JsonObject oldEnvObj = oldEnvEl.getAsJsonObject();
            final JsonObject newEnvObj = new JsonObject();

            // Check the name first
            if (!oldEnvObj.has("name")) {
                throw new ConflictException("The format of the environment conflicts with a new format, name is missing");
            }
            final String envName = oldEnvObj.get("name").getAsString();

            // Check machineConfigs field is present
            if (!oldEnvObj.has("machineConfigs") || !oldEnvObj.get("machineConfigs").isJsonArray()) {
                throw new ConflictException("The format of the environment is unappropriated");
            }

            // Convert old machine configs to new ones
            final Map<String, Service> services = new HashMap<>();
            final JsonObject newMachinesObj = new JsonObject();
            for (JsonElement machineConfigEl : oldEnvObj.get("machineConfigs").getAsJsonArray()) {
                if (!machineConfigEl.isJsonObject()) {
                    throw new ConflictException(format("The format of the machine in environment '%s' conflicts with a new format",
                                                       envName));
                }
                final JsonObject oldMachineConfObj = machineConfigEl.getAsJsonObject();
                final JsonObject newMachineObj = new JsonObject();

                // Check the name field is present
                if (!oldMachineConfObj.has("name")) {
                    throw new ConflictException(format("The format of the machine in environment '%s' " +
                                                       "conflicts with a new format, machine name is missing",
                                                       envName));
                }
                final String machineName = oldMachineConfObj.get("name").getAsString();

                // If machine is dev machine then new machine must contain ws-agent in agents list
                if (oldMachineConfObj.has("dev")) {
                    final JsonElement dev = oldMachineConfObj.get("dev");
                    if (dev.isJsonPrimitive() && dev.getAsBoolean()) {
                        final JsonArray agents = new JsonArray();
                        agents.add(new JsonPrimitive("ws-agent"));
                        newMachineObj.add("agents", agents);
                    }
                }

                // Convert services
                if (oldMachineConfObj.has("servers")) {
                    if (!oldMachineConfObj.get("servers").isJsonArray()) {
                        throw new ConflictException(format("The format of the servers in machine '%s:%s' conflicts with a new format",
                                                           envName,
                                                           machineName));
                    }
                    final JsonObject newServersObj = new JsonObject();
                    for (JsonElement serversEl : oldMachineConfObj.get("servers").getAsJsonArray()) {
                        if (!serversEl.isJsonObject()) {
                            throw new ConflictException(format("The format of servers in machine '%s:%s' conflicts with a new format",
                                                               envName,
                                                               machineName));
                        }
                        final JsonObject oldServerObj = serversEl.getAsJsonObject();
                        if (!oldServerObj.has("ref")) {
                            throw new ConflictException(format("The format of server in machine '%s:%s' conflicts with a new format",
                                                               envName,
                                                               machineName));
                        }
                        final String ref = oldServerObj.get("ref").getAsString();
                        final JsonObject newServerObj = new JsonObject();
                        if (oldServerObj.has("port")) {
                            newServerObj.add("port", oldServerObj.get("port"));
                        }
                        if (oldServerObj.has("protocol")) {
                            newServerObj.add("protocol", oldServerObj.get("protocol"));
                        }
                        newServersObj.add(ref, newServerObj);
                    }
                    newMachineObj.add("servers", newServersObj);
                }
                newMachinesObj.add(machineName, newMachineObj);
            }
            newEnvObj.add("machines", newMachinesObj);

            // Adapt recipe
            final JsonObject recipeObj = new JsonObject();
            recipeObj.addProperty("type", "compose");
            recipeObj.addProperty("contentType", "application/x-yaml");
            recipeObj.addProperty("content", new Yaml().dumpAsMap(singletonMap("services", services)));

            newEnvObj.add("recipe", recipeObj);
            newEnvironmentsObj.add(envName, newEnvObj);
        }
        confSourceObj.add("environments", newEnvironmentsObj);
        return confSourceObj;
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
