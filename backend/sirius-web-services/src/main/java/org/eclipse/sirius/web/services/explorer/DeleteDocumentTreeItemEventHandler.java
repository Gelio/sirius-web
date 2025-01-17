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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.sirius.web.core.api.IEditingContext;
import org.eclipse.sirius.web.emf.services.EditingContext;
import org.eclipse.sirius.web.persistence.entities.DocumentEntity;
import org.eclipse.sirius.web.persistence.repositories.IDocumentRepository;
import org.eclipse.sirius.web.representations.Failure;
import org.eclipse.sirius.web.representations.IStatus;
import org.eclipse.sirius.web.representations.Success;
import org.eclipse.sirius.web.services.explorer.api.IDeleteTreeItemHandler;
import org.eclipse.sirius.web.spring.collaborative.api.ChangeKind;
import org.eclipse.sirius.web.trees.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles document deletion triggered via a tree item from the explorer.
 *
 * @author pcdavid
 */
@Service
public class DeleteDocumentTreeItemEventHandler implements IDeleteTreeItemHandler {

    private final IDocumentRepository documentRepository;

    private final Logger logger = LoggerFactory.getLogger(DeleteDocumentTreeItemEventHandler.class);

    public DeleteDocumentTreeItemEventHandler(IDocumentRepository documentRepository) {
        this.documentRepository = Objects.requireNonNull(documentRepository);
    }

    @Override
    public boolean canHandle(IEditingContext editingContext, TreeItem treeItem) {
        return treeItem.getKind().equals(ExplorerDescriptionProvider.DOCUMENT_KIND);
    }

    @Override
    public IStatus handle(IEditingContext editingContext, TreeItem treeItem) {
        // @formatter:off
        var optionalEditingDomain = Optional.of(editingContext)
                .filter(EditingContext.class::isInstance)
                .map(EditingContext.class::cast)
                .map(EditingContext::getDomain);
        // @formatter:on

        var optionalDocumentEntity = this.parse(treeItem.getId()).flatMap(this.documentRepository::findById);
        if (optionalEditingDomain.isPresent() && optionalDocumentEntity.isPresent()) {
            AdapterFactoryEditingDomain editingDomain = optionalEditingDomain.get();
            DocumentEntity documentEntity = optionalDocumentEntity.get();

            ResourceSet resourceSet = editingDomain.getResourceSet();
            URI uri = URI.createURI(documentEntity.getId().toString());

            // @formatter:off
                List<Resource> resourcesToDelete = resourceSet.getResources().stream()
                        .filter(resource -> resource.getURI().equals(uri))
                        .collect(Collectors.toUnmodifiableList());
                resourcesToDelete.stream().forEach(resourceSet.getResources()::remove);
                // @formatter:on

            this.documentRepository.delete(documentEntity);

            return new Success(ChangeKind.SEMANTIC_CHANGE, Map.of());
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
