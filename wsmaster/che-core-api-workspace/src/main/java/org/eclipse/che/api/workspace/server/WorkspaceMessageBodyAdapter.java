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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.rest.StringMessageBodyAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Adapts an old workspace format to a new one.
 *
 * @author Yevhenii Voevodin
 */
@Singleton
public class WorkspaceMessageBodyAdapter extends StringMessageBodyAdapter {

    private static final Pattern OLD_WORKSPACE_PATTERN = Pattern.compile(".*\"environments\"\\s*:\\s*\\[");

    @Inject
    private WorkspaceConfigAdapter configAdapter;

    @Override
    public boolean canAdapt(Class<?> type, Type genericType) {
        return Workspace.class.isAssignableFrom(type) || isListOfType(type, genericType, Workspace.class);
    }

    @Override
    public String adapt(String body) throws IOException {
        if (OLD_WORKSPACE_PATTERN.matcher(body).matches()) {
            final JsonParser parser = new JsonParser();
            try {
                final JsonObject workspaceObj = parser.parse(body).getAsJsonObject();
                if (workspaceObj.has("config") && workspaceObj.get("config").isJsonObject()) {
                    return configAdapter.adapt(workspaceObj.getAsJsonObject("config")).toString();
                }
            } catch (BadRequestException | ServerException | RuntimeException x) {
                throw new IOException(x.getMessage(), x);
            }
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private static boolean isListOfType(Class<?> type, Type genericType, Class<?> targetType) {
        if (type.isAssignableFrom(List.class) && genericType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType)genericType;
            final Type[] args = parameterizedType.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class) {
                final Class genericClass = (Class)args[0];
                return genericClass.isAssignableFrom(targetType);
            }
        }
        return false;
    }
}
