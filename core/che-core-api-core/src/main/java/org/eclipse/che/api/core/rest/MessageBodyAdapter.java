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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Adapts an entity stream in an implementation specific way.
 *
 * @author Yevhenii Voevodin
 */
@Beta
public interface MessageBodyAdapter {

    /**
     * Checks whether adaptation for given type should be triggered or not.
     * If true is returned it means only that the stream MAY be adapted
     * if {@link #adapt(InputStream)} is called.
     *
     * @param type
     *         the {@code type}
     * @param genericType
     *         the {@code genericType}
     * @return true if the entity may be adapted by given adapter
     */
    boolean canAdapt(Class<?> type, Type genericType);

    /**
     * Adapts entity stream to a new one, if necessary.
     * It is expected that caller checked that {@link #canAdapt(Class, Type)}
     * returned true before invoking this method.
     *
     * @param entityStream
     *         an entity stream processed by {@link CheJsonProvider#readFrom}
     * @return a new stream with an adapted data or the same {@code entityStream}
     * if there is nothing to adapt
     */
    InputStream adapt(InputStream entityStream) throws IOException;
}
