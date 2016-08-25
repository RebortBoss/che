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
import com.google.common.io.CharStreams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * @author Yevhenii Voevodin
 */
@Beta
public abstract class StringMessageBodyAdapter implements MessageBodyAdapter {

    @Override
    public InputStream adapt(InputStream entityStream) throws IOException {
        try (Reader r = new InputStreamReader(entityStream)) {
            final String result = adapt(CharStreams.toString(r));
            return new ByteArrayInputStream(result.getBytes(Charset.defaultCharset()));
        }
    }

    public abstract String adapt(String body) throws IOException;
}
