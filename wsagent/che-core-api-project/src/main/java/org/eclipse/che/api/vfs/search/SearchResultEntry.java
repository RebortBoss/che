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
package org.eclipse.che.api.vfs.search;

/**
 * Single item in {@code SearchResult}.
 */
public class SearchResultEntry {
    private final String filePath;

    public SearchResultEntry(String filePath) {
        this.filePath = filePath;
    }

    /** Path of file that matches the search criteria. */
    public String getFilePath() {
        return filePath;
    }
}
