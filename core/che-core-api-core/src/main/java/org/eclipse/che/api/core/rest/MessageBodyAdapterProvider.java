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
package org.eclipse.che.api.core.rest;

import com.google.common.annotations.Beta;
import com.google.inject.Singleton;

import org.eclipse.che.commons.lang.Pair;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Assumes that there are no two different adapter instances which
 * can adapt the same type and returns the first which matches.
 *
 * @author Yevhenii Voevodin
 */
@Beta
@Singleton
public class MessageBodyAdapterProvider {

    private final Set<MessageBodyAdapter>                                 adapters;
    private final ConcurrentMap<Pair<Class<?>, Type>, MessageBodyAdapter> cache;

    @Inject
    public MessageBodyAdapterProvider(Set<MessageBodyAdapter> adapters) {
        this.adapters = adapters;
        this.cache = new ConcurrentHashMap<>();
    }

    public MessageBodyAdapter getAdapter(Class<?> type, Type genericType) {
        final Pair<Class<?>, Type> key = Pair.of(type, genericType);
        final MessageBodyAdapter adapter = cache.get(key);
        if (adapter != null) {
            return adapter;
        }
        for (MessageBodyAdapter candidate : adapters) {
            if (candidate.canAdapt(type, genericType)) {
                cache.putIfAbsent(key, candidate);
                return candidate;
            }
        }
        return null;
    }
}
