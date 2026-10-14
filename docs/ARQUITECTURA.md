# ARQUITECTURA — Guía rápida para desarrolladores nuevos

Este documento resume la arquitectura de TaskFlow: capas y paquetes, el recorrido de POST `/projects/{projectId}/tasks`, dónde viven las reglas de negocio, cómo funciona la seguridad JWT y cómo están organizados los tests.

1) Capas y paquetes

- `controller` — puerta HTTP, traduce DTOs ↔ dominio, valida frontera y delega a servicios. Ej.: `src/main/java/com/taskflow/controller/TaskController.java` (`TaskController`) y `src/main/java/com/taskflow/controller/ProjectController.java` (`ProjectController`).

- `service` — orquestación de casos de uso; coordina repositorios, mappers y dominio. Ej.: `src/main/java/com/taskflow/service/TaskService.java` (`TaskService`) y `src/main/java/com/taskflow/service/ProjectService.java` (`ProjectService`).

- `mapper` — mapeo manual entre DTOs y entidades (puerta entre contrato y dominio). Ej.: `src/main/java/com/taskflow/mapper/TaskMapper.java` (`TaskMapper`).

- `model` — entidades de dominio con sus reglas y fábricas. Ej.: `src/main/java/com/taskflow/model/Task.java` (`Task`).

- `repository` — Spring Data JPA; persistencia. Ej.: `src/main/java/com/taskflow/repository/TaskRepository.java` (`TaskRepository`).

- `advice` / `exception` — manejo centralizado de errores. Ej.: `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java` (`GlobalExceptionHandler`).

- `security` / `config` — filtros JWT y configuración de seguridad. Ej.: `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java` (`JwtAuthenticationFilter`), `src/main/java/com/taskflow/security/JwtService.java` (`JwtService`), y `src/main/java/com/taskflow/config/SecurityConfig.java` (`SecurityConfig`).

2) Recorrido completo de POST `/projects/{projectId}/tasks`

- El request HTTP llega a `TaskController` (`src/main/java/com/taskflow/controller/TaskController.java`). Firma: `@PostMapping("/projects/{projectId}/tasks")`.
- Bean Validation (`@Valid` sobre `TaskRequest`) se ejecuta en la frontera; fallos → 400 por `GlobalExceptionHandler`.
- El controller verifica que el proyecto exista con `ProjectService.buscarPorId(projectId)` (si no, lanza `ProjectNotFoundException` → 404).
- Si existe, el controller delega a `TaskService.crear(request, projectId)` (`src/main/java/com/taskflow/service/TaskService.java`).
- `TaskService.crear` usa `TaskMapper.aEntidadNueva(request, projectId)` (`src/main/java/com/taskflow/mapper/TaskMapper.java`) para convertir el DTO en una entidad de dominio NUEVA. El mapper invoca la fábrica de dominio: `Task.crear(...)` (`src/main/java/com/taskflow/model/Task.java`).
- La fábrica / constructor en `Task` aplica reglas de negocio (p. ej. "dueDate no puede estar en el pasado al crear", status inicial `TODO`, título obligatorio). Si una regla falla lanza `TaskValidationException` (checked) que asciende al `GlobalExceptionHandler` y se traduce a 400.
- Si la entidad es válida, `TaskService` llama a `TaskRepository.save(nueva)` (`src/main/java/com/taskflow/repository/TaskRepository.java`) y JPA/Hibernate persiste la fila en la base de datos; el id se asigna en la persistencia.
- `TaskController` construye la respuesta `201 Created` con `Location: /tasks/{id}` y el `TaskResponse` mapeado por `TaskMapper.aResponse`.

Diagrama secuencial (resumido):
`HTTP -> TaskController (/projects/{projectId}/tasks)` -> `ProjectService.buscarPorId` -> `TaskService.crear` -> `TaskMapper.aEntidadNueva` -> `Task.crear` (reglas dominio) -> `TaskRepository.save` -> DB -> response 201.

3) Dónde viven las reglas de negocio

- Reglas de invariantes y transiciones de estado están en las entidades del dominio (`src/main/java/com/taskflow/model/Task.java`). Ejemplos: creación via `Task.crear(...)` (valida dueDate), `Task.setStatus(...)` (valida transiciones: no pasar a `DONE` sin `assignee`).
- Las reglas se REUTILIZAN por los servicios y mappers: los servicios orquestan pero delegan la lógica concreta al dominio; no duplicar reglas en controles o servicios.
- Traducción de excepciones: las reglas pueden lanzar `TaskValidationException` (checked) o el servicio puede traducirlo a `TaskStateException` (unchecked) para distinguir 400 vs 422. El `GlobalExceptionHandler` unifica códigos HTTP.

4) Cómo funciona la seguridad con JWT

- `SecurityConfig` (`src/main/java/com/taskflow/config/SecurityConfig.java`) define la `SecurityFilterChain`: rutas públicas (`/auth/**`, `/info`, Swagger, UI estática, H2 console) y `anyRequest().authenticated()` para el resto.
- `JwtAuthenticationFilter` (`src/main/java/com/taskflow/security/JwtAuthenticationFilter.java`) se ejecuta ANTES del `UsernamePasswordAuthenticationFilter`. Behaviors clave:
  - Si no hay header `Authorization: Bearer ...`, la petición continúa anónima.
  - Si hay token, el filtro llama a `JwtService` para extraer y validar el username/firmas. Si el token es inválido o expiró, el filtro responde 401 JSON inmediatamente (el `ControllerAdvice` no ve estas excepciones porque el filtro corre antes del DispatcherServlet).
  - Si es válido, carga `UserDetails` (desde DB), crea una `Authentication` y la pone en el `SecurityContext` para que `@PreAuthorize` y los matchers URL lo vean.
- Autorización data-driven: reglas finas usan `@PreAuthorize` que puede invocar beans como `ProjectSecurity` (`src/main/java/com/taskflow/security/ProjectSecurity.java`) para comprobar que el `authentication.name` sea el owner del recurso.
- Estado: la seguridad es stateless (tokens), `SessionCreationPolicy.STATELESS` en `SecurityConfig`.

5) Organización de tests

- Unit tests: bajo `src/test/java/com/taskflow/unit/` prueban clases en aislamiento (JUnit 5 + Mockito). Ej.: `src/test/java/com/taskflow/unit/TaskServiceTest.java` prueba la lógica de `TaskService` sin arrancar Spring.

- Slice tests: bajo `src/test/java/com/taskflow/slice/` usan `@WebMvcTest` o `@DataJpaTest` para probar controladores o repositorios con contexto limitado. Ej.: `src/test/java/com/taskflow/slice/ProjectControllerTest.java`.

- Integration tests: bajo `src/test/java/com/taskflow/integration/` o `*IT.java` usan `@SpringBootTest` y (cuando están habilitados) Testcontainers. Por defecto estos IT no se ejecutan con `mvn test` salvo que se active `-Ddocker.tests=true`.

Comandos útiles (Windows / PowerShell, desde la raíz):
- Ejecutar tests normales: `mvn -q test`
- Ejecutar una clase de test: `mvn -q test "-Dtest=TaskServiceTest"`
- Ejecutar la app con H2 (datos de ejemplo): `mvn spring-boot:run "-Dspring-boot.run.profiles=h2"`

Notas finales

- No duplicar reglas: si la regla pertenece al dominio, colócala en `Task` y úsala desde servicios y mappers.
- El `GlobalExceptionHandler` estandariza respuestas: 400 (validación), 404 (no existe), 422 (estado prohibido), 409 (conflicto), 401/403 (auth).
- Para autorización fina (por ejemplo "solo owner borra"), usa `@PreAuthorize` combinado con el bean `ProjectSecurity`.

Si quieres, agrego un diagrama ASCII o ejemplos de requests/responses para `/projects/{projectId}/tasks`.
Las fechas límite se validan en la fábrica/entidad `Task` (`src/main/java/com/taskflow/model/Task.java`).
