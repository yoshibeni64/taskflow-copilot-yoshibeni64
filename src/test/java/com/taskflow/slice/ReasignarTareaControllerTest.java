package com.taskflow.slice;

import com.taskflow.controller.TaskController;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReasignarTareaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void patchAssignee_valido_retorna200YAssignee() throws Exception {
        Task tarea;
        try {
            tarea = new Task(1L, "Tarea", "d", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(5));
        } catch (TaskValidationException e) {
            throw new IllegalStateException(e);
        }
        when(taskService.buscarPorId(1L)).thenReturn(Optional.of(tarea));
        when(taskService.reasignar(any(Task.class), eq(5L))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setAssigneeId(5L);
            return t;
        });

        mockMvc.perform(patch("/tasks/1/assignee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"assigneeId\": 5 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(5));
    }

    @Test
    void patchAssignee_serviceLanza422_retorna422() throws Exception {
        Task tarea;
        try {
            tarea = new Task(6L, "Tarea", "d", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(5));
        } catch (TaskValidationException e) {
            throw new IllegalStateException(e);
        }
        when(taskService.buscarPorId(6L)).thenReturn(Optional.of(tarea));
        when(taskService.reasignar(any(Task.class), eq(1L))).thenThrow(new TaskStateException("No se puede reasignar una tarea terminada."));

        mockMvc.perform(patch("/tasks/6/assignee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"assigneeId\": 1 }"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void patchAssignee_inexistente_retorna404() throws Exception {
        when(taskService.buscarPorId(999L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/tasks/999/assignee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"assigneeId\": 10 }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void patchAssignee_cuerpoInvalido_retorna400YNoLlamaService() throws Exception {
        mockMvc.perform(patch("/tasks/1/assignee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).reasignar(any(Task.class), any(Long.class));
    }
}
