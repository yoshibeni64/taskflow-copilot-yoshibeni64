package com.taskflow.unit;

import com.taskflow.exception.TaskStateException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReasignarTareaServiceTest {

    @Mock
    private TaskRepository repository;

    @InjectMocks
    private com.taskflow.service.TaskService service;

    @Test
    void reasignar_todoSinAssignee_guardaConNuevoAssignee() throws Exception {
        Task tarea = new Task(1L, "Tarea", "d", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(1));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reasignar(tarea, 42L);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(repository).save(captor.capture());
        assertEquals(42L, captor.getValue().getAssigneeId());
    }

    @Test
    void reasignar_tareaDone_lanzaTaskStateExceptionYNuncaGuarda() throws Exception {
        Task tarea = new Task(2L, "Hecha", "d", TaskStatus.DONE, Priority.MED, 1L, 7L, LocalDate.now().plusDays(1));

        assertThrows(TaskStateException.class, () -> service.reasignar(tarea, 99L));
        verify(repository, never()).save(any(Task.class));
    }
}
