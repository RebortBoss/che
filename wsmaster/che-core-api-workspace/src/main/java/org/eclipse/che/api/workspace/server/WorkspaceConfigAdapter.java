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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.eclipse.che.api.core.ConflictException;

import javax.inject.Singleton;

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

    // TODO use input stream for detecting whether the object can be adapted
    public JsonObject adapt(JsonObject sourceObj) throws ConflictException {
        final JsonArray environments = sourceObj.getAsJsonArray("environments");
        for (JsonElement environmentEl : environments) {
            final JsonObject environmentObj = environmentEl.getAsJsonObject();
        }
    }
}
