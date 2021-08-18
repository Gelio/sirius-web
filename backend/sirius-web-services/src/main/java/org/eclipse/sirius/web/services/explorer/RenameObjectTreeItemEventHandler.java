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

import java.util.Objects;
import java.util.UUID;

import org.eclipse.sirius.web.core.api.ErrorPayload;
import org.eclipse.sirius.web.core.api.IEditService;
import org.eclipse.sirius.web.core.api.IEditingContext;
import org.eclipse.sirius.web.core.api.IInput;
import org.eclipse.sirius.web.core.api.IObjectService;
import org.eclipse.sirius.web.core.api.IPayload;
import org.eclipse.sirius.web.spring.collaborative.api.ChangeDescription;
import org.eclipse.sirius.web.spring.collaborative.api.ChangeKind;
import org.eclipse.sirius.web.spring.collaborative.api.IEditingContextEventHandler;
import org.eclipse.sirius.web.spring.collaborative.api.Monitoring;
import org.eclipse.sirius.web.spring.collaborative.dto.RenameObjectInput;
import org.eclipse.sirius.web.spring.collaborative.messages.ICollaborativeMessageService;
import org.eclipse.sirius.web.spring.collaborative.trees.dto.RenameTreeItemInput;
import org.eclipse.sirius.web.spring.collaborative.trees.dto.RenameTreeItemSuccessPayload;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Sinks.Many;
import reactor.core.publisher.Sinks.One;

/**
 * Handles domain objet renaming triggered via a tree item from the explorer.
 *
 * @author pcdavid
 */
@Service
public class RenameObjectTreeItemEventHandler implements IEditingContextEventHandler {
    private final ICollaborativeMessageService messageService;

    private final IObjectService objectService;

    private final IEditService editService;

    private final Counter counter;

    public RenameObjectTreeItemEventHandler(ICollaborativeMessageService messageService, IObjectService objectService, IEditService editService, MeterRegistry meterRegistry) {
        this.messageService = Objects.requireNonNull(messageService);
        this.objectService = Objects.requireNonNull(objectService);
        this.editService = Objects.requireNonNull(editService);

        // @formatter:off
        this.counter = Counter.builder(Monitoring.EVENT_HANDLER)
                .tag(Monitoring.NAME, this.getClass().getSimpleName())
                .register(meterRegistry);
        // @formatter:on
    }

    @Override
    public boolean canHandle(IEditingContext editingContext, IInput input) {
        return input instanceof RenameTreeItemInput && this.isDomainObjectKind(((RenameTreeItemInput) input).getKind());
    }

    private boolean isDomainObjectKind(String kind) {
        // TODO There should really a better way to do this...
        return kind != null && kind.contains("::"); //$NON-NLS-1$
    }

    @Override
    public void handle(One<IPayload> payloadSink, Many<ChangeDescription> changeDescriptionSink, IEditingContext editingContext, IInput input) {
        this.counter.increment();

        ChangeDescription changeDescription = new ChangeDescription(ChangeKind.NOTHING, editingContext.getId(), input);
        String message = this.messageService.invalidInput(input.getClass().getSimpleName(), RenameObjectInput.class.getSimpleName());
        IPayload payload = new ErrorPayload(input.getId(), message);

        if (input instanceof RenameTreeItemInput) {
            RenameTreeItemInput renameObjectInput = (RenameTreeItemInput) input;
            UUID objectId = renameObjectInput.getTreeItemId();
            String newName = renameObjectInput.getNewName();
            var optionalObject = this.objectService.getObject(editingContext, objectId.toString());
            if (optionalObject.isPresent()) {
                Object object = optionalObject.get();
                var optionalLabelField = this.objectService.getLabelField(object);
                if (optionalLabelField.isPresent()) {
                    String labelField = optionalLabelField.get();
                    this.editService.editLabel(object, labelField, newName);
                    changeDescription = new ChangeDescription(ChangeKind.SEMANTIC_CHANGE, editingContext.getId(), input);
                    payload = new RenameTreeItemSuccessPayload(input.getId());
                }
            }
        }
        payloadSink.tryEmitValue(payload);
        changeDescriptionSink.tryEmitNext(changeDescription);
    }

}