package com.taskflow.dto;

import java.util.Map;

/**
 * ProjectSummaryResponse — DTO de salida para GET /projects/{id}/summary.
 * Contiene el resumen por estado y el número de tareas vencidas.
 */
public record ProjectSummaryResponse(Long projectId,
                                    String projectName,
                                    int totalTasks,
                                    Map<String, Integer> byStatus,
                                    int overdue) {
}
