/*******************************************************************************
 * Copyright (c) 2021 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.sirius.web.services.explorer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.sirius.web.core.api.IEditingContext;
import org.eclipse.sirius.web.representations.Failure;
import org.eclipse.sirius.web.representations.IStatus;
import org.eclipse.sirius.web.representations.Success;
import org.eclipse.sirius.web.services.explorer.api.IDeleteTreeItemHandler;
import org.eclipse.sirius.web.spring.collaborative.api.ChangeKind;
import org.eclipse.sirius.web.spring.collaborative.projects.EditingContextEventProcessor;
import org.eclipse.sirius.web.trees.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles representation deletion triggered via a tree item from the explorer.
 *
 * @author pcdavid
 */
@Service
public class DeleteRepresentationTreeItemEventHandler implements IDeleteTreeItemHandler {

    private final Logger logger = LoggerFactory.getLogger(DeleteRepresentationTreeItemEventHandler.class);

    @Override
    public boolean canHandle(IEditingContext editingContext, TreeItem treeItem) {
        return !ExplorerDescriptionProvider.DOCUMENT_KIND.equals(treeItem.getKind()) && !treeItem.getKind().contains("::"); //$NON-NLS-1$
    }

    @Override
    public IStatus handle(IEditingContext editingContext, TreeItem treeItem) {
        var optionalRepresentationId = this.parse(treeItem.getId());
        if (optionalRepresentationId.isPresent()) {
            UUID representationId = optionalRepresentationId.get();
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(EditingContextEventProcessor.REPRESENTATION_ID, representationId);
            return new Success(ChangeKind.REPRESENTATION_TO_DELETE, parameters);
        }
        return new Failure(""); //$NON-NLS-1$
    }

    private Optional<UUID> parse(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            return Optional.of(uuid);
        } catch (IllegalArgumentException exception) {
            this.logger.warn(exception.getMessage(), exception);
        }
        return Optional.empty();
    }
}
