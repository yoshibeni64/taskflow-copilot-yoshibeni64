package com.taskflow.unit;

import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import com.taskflow.exception.TaskValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void resumen_proyectoConTareas_yVencida() throws TaskValidationException {
        Project p = new Project(2L, "App Móvil", "d", 2L, null);

        List<Task> tasks = List.of(
                new Task(10L, "T-1", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null),
                new Task(11L, "T-2", "d", TaskStatus.IN_PROGRESS, Priority.HIGH, 2L, 1L, LocalDate.now().minusDays(2)),
                new Task(12L, "T-3", "d", TaskStatus.IN_PROGRESS, Priority.LOW, 2L, 1L, null),
                new Task(13L, "T-4", "d", TaskStatus.DONE, Priority.MED, 2L, 1L, LocalDate.now().minusDays(10))
        );

        when(taskRepository.findByProjectId(2L)).thenReturn(tasks);

        ProjectSummaryResponse resp = projectService.resumen(p);

        assertEquals(4, resp.totalTasks());
        assertEquals(1, resp.byStatus().get("TODO"));
        assertEquals(2, resp.byStatus().get("IN_PROGRESS"));
        assertEquals(1, resp.byStatus().get("DONE"));
        assertEquals(1, resp.overdue());
    }

    @Test
    void resumen_proyectoSinTareas_devuelveCeros() {
        Project p = new Project(3L, "Empty", "d", 1L, null);
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        ProjectSummaryResponse resp = projectService.resumen(p);

        assertEquals(0, resp.totalTasks());
        assertEquals(0, resp.byStatus().get("TODO"));
        assertEquals(0, resp.byStatus().get("IN_PROGRESS"));
        assertEquals(0, resp.byStatus().get("DONE"));
        assertEquals(0, resp.overdue());
    }
}
