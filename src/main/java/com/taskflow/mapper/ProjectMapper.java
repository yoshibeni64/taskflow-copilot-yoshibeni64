package com.taskflow.mapper;

import com.taskflow.dto.ProjectResponse;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.model.Project;
import com.taskflow.model.TaskStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ProjectMapper — puente DTO <-> dominio del lado Project. Estático, a mano, sin MapStruct.
 */
public final class ProjectMapper {

    private ProjectMapper() {
        // no instanciable
    }

    /** Entidad -> DTO de salida. ownerId sale directo del campo (sin puente por objeto). */
    public static ProjectResponse aResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getOwnerId(), p.getCreatedAt());
    }

    /**
     * Entidad Project + conteos -> ProjectSummaryResponse.
     * Normaliza el mapa para asegurar las tres claves TODO/IN_PROGRESS/DONE.
     */
    public static ProjectSummaryResponse aSummary(Project p, Map<String, Integer> byStatus, int totalTasks, int overdue) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            normalized.put(s.name(), byStatus.getOrDefault(s.name(), 0));
        }
        return new ProjectSummaryResponse(p.getId(), p.getName(), totalTasks, normalized, overdue);
    }
}
