package com.taskflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * TaskAssigneeUpdateRequest — cuerpo de PATCH /tasks/{id}/assignee.
 *
 * Contiene únicamente el nuevo assigneeId que se quiere asignar a la tarea.
 */
public record TaskAssigneeUpdateRequest(

        @NotNull(message = "El id del responsable es obligatorio.")
        @Positive(message = "El id del responsable debe ser un número positivo.")
        Long assigneeId
) {
}
