# WorkoutDB Backend MVP

Backend REST para ejercicios de gimnasio con base de datos local SQLite.

## Stack

- Java 17
- Spring Boot 4
- SQLite (archivo local `workout.db`)

## Cómo ejecutar

```bash
./mvnw spring-boot:run
```

Al arrancar, se crea automáticamente la base local y se cargan ejercicios iniciales.

## Base URL

`http://localhost:8080/api/exercises`

## Modelo de ejercicio

```json
{
  "id": 1,
  "name": "Bench Press",
  "instructions": "Acuestate en un banco...",
  "primaryMuscle": "Pectoral",
  "secondaryMuscle": "Triceps"
}
```

## Endpoints MVP

- `GET /api/exercises` -> lista todos los ejercicios.
- `GET /api/exercises/{id}` -> devuelve un ejercicio por ID.
- `POST /api/exercises` -> crea un ejercicio.
- `PUT /api/exercises/{id}` -> actualiza un ejercicio.
- `DELETE /api/exercises/{id}` -> elimina un ejercicio.

## Ejemplo de creación

```bash
curl -X POST http://localhost:8080/api/exercises \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Curl Martillo",
    "instructions": "De pie, flexiona el codo con agarre neutro y controla la bajada.",
    "primaryMuscle": "Biceps",
    "secondaryMuscle": "Antebrazo"
  }'
```
