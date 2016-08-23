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

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.api.machine.server.recipe.RecipeService;
import org.eclipse.che.api.machine.shared.Constants;
import org.eclipse.che.api.machine.shared.dto.recipe.NewRecipe;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final HttpJsonRequestFactory httpReqFactory;

    @Inject
    public WorkspaceConfigAdapter(HttpJsonRequestFactory httpReqFactory) {
        this.httpReqFactory = httpReqFactory;
    }

    public JsonObject adapt(JsonObject confSourceObj) throws BadRequestException, ServerException {
        final JsonArray oldEnvironmentsArr = confSourceObj.getAsJsonArray("environments");
        final JsonObject newEnvironmentsObj = new JsonObject();
        for (JsonElement oldEnvEl : oldEnvironmentsArr) {
            final JsonObject oldEnvObj = oldEnvEl.getAsJsonObject();
            final JsonObject newEnvObj = new JsonObject();

            // Check the name first
            if (!oldEnvObj.has("name")) {
                throw new BadRequestException("Bad format, environment name is missing");
            }
            final String envName = oldEnvObj.get("name").getAsString();

            // Check machineConfigs field is present
            if (!oldEnvObj.has("machineConfigs") || !oldEnvObj.get("machineConfigs").isJsonArray()) {
                throw new BadRequestException(format("Bad format, environment '%s' must declare at least one machine config", envName));
            }

            // Convert old machine configs to new ones
            final Map<String, Service> services = new LinkedHashMap<>();
            final JsonObject newMachinesObj = new JsonObject();
            for (JsonElement machineConfigEl : oldEnvObj.get("machineConfigs").getAsJsonArray()) {
                if (!machineConfigEl.isJsonObject()) {
                    throw new BadRequestException(format("Bad format of the machine in environment '%s', " +
                                                         "machine config must be json object", envName));
                }
                final JsonObject oldMachineConfObj = machineConfigEl.getAsJsonObject();

                // Check the name field is present
                if (!oldMachineConfObj.has("name")) {
                    throw new BadRequestException(format("Bad format of the machine in environment '%s', machine name is missing",
                                                         envName));
                }
                final String machineName = oldMachineConfObj.get("name").getAsString();
                newMachinesObj.add(machineName, asMachine(oldMachineConfObj, machineName, envName));
                services.put(machineName, asService(oldMachineConfObj, envName, machineName));
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

    private static JsonObject asMachine(JsonObject oldMachineConfObj, String machineName, String envName) throws BadRequestException {
        final JsonObject newMachineObj = new JsonObject();

        // If machine is dev machine then new machine must contain ws-agent in agents list
        if (oldMachineConfObj.has("dev")) {
            final JsonElement dev = oldMachineConfObj.get("dev");
            if (dev.isJsonPrimitive() && dev.getAsBoolean()) {
                final JsonArray agents = new JsonArray();
                agents.add(new JsonPrimitive("ws-agent"));
                newMachineObj.add("agents", agents);
            }
        }

        // Convert convert servers
        if (oldMachineConfObj.has("servers")) {
            if (!oldMachineConfObj.get("servers").isJsonArray()) {
                throw new BadRequestException(format("Bad format of the servers in machine '%s:%s', json array required",
                                                     envName,
                                                     machineName));
            }
            final JsonObject newServersObj = new JsonObject();
            for (JsonElement serversEl : oldMachineConfObj.get("servers").getAsJsonArray()) {
                if (!serversEl.isJsonObject()) {
                    throw new BadRequestException(format("Bad format of server in machine '%s:%s', json object required",
                                                         envName,
                                                         machineName));
                }
                final JsonObject oldServerObj = serversEl.getAsJsonObject();
                if (!oldServerObj.has("ref")) {
                    throw new BadRequestException(format("Bad format of server in machine '%s:%s', server must contain ref",
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
        return newMachineObj;
    }

    /** Converts machine configuration to service. */
    private Service asService(JsonObject machineObj, String envName, String machineName) throws BadRequestException, ServerException {
        final Service service = new Service();

        // Convert machine source
        if (!machineObj.has("source") || !machineObj.get("source").isJsonObject()) {
            throw new BadRequestException(format("Bad format, source for machine '%s:%s' is missing",
                                                 envName,
                                                 machineName));
        }
        final JsonObject sourceObj = machineObj.getAsJsonObject("source");

        // Convert source type
        if (!sourceObj.has("type") || !sourceObj.get("type").isJsonPrimitive()) {
            throw new BadRequestException(format("Bad format, machine '%s:%s', type is missing",
                                                 envName,
                                                 machineName));
        }

        final String type = sourceObj.get("type").getAsString();
        // type = image                 - becomes service -> image
        // type = dockerfile + location - becomes service -> build -> context = location
        // type = dockerfile + content  - becomes service -> build -> context = generated_recipe_location
        if ("dockerfile".equals(type)) {
            final String contextLink;g
            if (sourceObj.has("content")) {
                final RecipeDescriptor rd;
                try {
                    rd = httpReqFactory.fromUrl(UriBuilder.fromResource(RecipeService.class)
                                                          .path(RecipeService.class, "createRecipe")
                                                          .build()
                                                          .toString())
                                       .setBody(DtoFactory.newDto(NewRecipe.class)
                                                          .withName("generated")
                                                          .withType("docker")
                                                          .withScript(sourceObj.get("content").getAsString()))
                                       .request()
                                       .asDto(RecipeDescriptor.class);
                } catch (Exception x) {
                    throw new ServerException(x.getLocalizedMessage(), x);
                }
                contextLink = rd.getLink(Constants.LINK_REL_GET_RECIPE_SCRIPT).getHref();
            } else {
                contextLink = sourceObj.get("location").getAsString();
            }
            service.setBuildContext(contextLink);
        } else if ("image".equals(type)) {
            service.setImage(sourceObj.get("location").getAsString());
        } else {
            throw new BadRequestException(format("Bad format, type '%s' is not supported", type));
        }

        // limits.ram(mb) - service -> mem_limit(b)
        if (machineObj.has("limits")) {
            final JsonObject limits = machineObj.getAsJsonObject("limits");
            if (limits.has("ram")) {
                service.setMemoryLimit(1024L * 1024L * limits.get("ram").getAsInt());
            }
        }

        // env1=val - environment: -env1=env2
        if (machineObj.has("envVariables")) {
            final List<String> envList = machineObj.getAsJsonObject("envVariables")
                                                   .entrySet()
                                                   .stream()
                                                   .map(e -> e.getKey() + '=' + e.getValue().getAsString())
                                                   .collect(Collectors.toList());
            service.setEnvironment(envList);
        }
        return service;
    }

    private static class Service extends LinkedHashMap<String, Object> {
        public void setMemoryLimit(long memoryLimit) {
            put("mem_limit", memoryLimit);
        }

        public void setBuildContext(String location) {
            put("build", singletonMap("context", location));
        }

        public void setImage(String image) {
            put("image", image);
        }

        public void setEnvironment(List<String> environment) {
            put("environment", environment);
        }
    }
}
