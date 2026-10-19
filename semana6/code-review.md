# Code Review — PF-6

## PR #5: feat: assignee (proyecto final)

### Comentario 1 — Documentación OpenAPI

- **Archivo:** `src/main/java/com/taskflow/controller/TaskController.java`
- **Ubicación:** endpoint `PATCH /tasks/{id}/assignee` (línea aproximada 161).
- **Revisor:** GitHub Copilot.
- **Severidad:** Low.
- **Hallazgo:** falta la anotación `@Operation` para documentar la reasignación del responsable y las restricciones aplicables a tareas en estado `DONE`.
- **Evaluación:** comentario válido. La funcionalidad y sus pruebas ya existían; faltaba documentar explícitamente el endpoint en OpenAPI.
- **Corrección aplicada:** se añadió `@Operation` con un resumen y una descripción en español, incluyendo las respuestas 400, 404 y 422.
- **Verificación:** `git diff --check` sin errores y `mvn -q test` finalizado sin errores.
- **Estado:** corregido manualmente; pendiente de confirmar la actualización del PR.
