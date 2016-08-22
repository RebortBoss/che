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
 *                      "name": "default",
 *                      "limits": {
 *                          "ram": 1000
 *                      },
 *                      "source": {
 *                          "location": "stub",
 *                          "type": "dockerfile"
 *                      },
 *                      "type": "docker", <- no
 *                      "dev": true, <- if agents contain 'ws-agent'
 *                      "envVariables" : {
 *                          "env1" : "value1"
 *                      },
 *                      "servers" : [ <- the format is different
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
 *                  "contentType" : "application/x-yaml",
 *
 *              },
 *              "machines" : [
 *                  {
 *                      "agents" : [ "exec-agent", "ws-agent" ],
 *                      "servers" : {
 *                          "some_reference" : {
 *                          "port" : "9090/udp",
 *                          "protocol" : "some_protocol",
 *                          "properties" : {
 *                              "prop1" : "value1"
 *                          }
 *                      }
 *                  }
 *              }
 *          }
 *      }
 * }
 *
 * </pre>
 *
 * @author Yevhenii Voevodin
 */
@Singleton
public class WorkspaceConfigAdapter {

    // TODO use input stream for detecting whether the object can be adapted
    public JsonObject adapt(JsonObject sourceObj) throws ConflictException {
        if (!sourceObj.has("environments") || !sourceObj.get("environments").isJsonArray()) {
            return sourceObj;
        }
        final JsonArray environments = sourceObj.getAsJsonArray("environments");
        for (JsonElement environmentEl : environments) {
            final JsonObject environmentObj = environmentEl.getAsJsonObject();

        }
    }
}
