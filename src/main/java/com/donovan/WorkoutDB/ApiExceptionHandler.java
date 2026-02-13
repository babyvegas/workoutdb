package com.donovan.WorkoutDB;

import com.donovan.WorkoutDB.exercise.DuplicateExerciseException;
import com.donovan.WorkoutDB.exercise.ExerciseNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ExerciseNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ApiError handleNotFound(ExerciseNotFoundException ex) {
		return new ApiError("not_found", ex.getMessage(), List.of());
	}

	@ExceptionHandler(DuplicateExerciseException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ApiError handleDuplicate(DuplicateExerciseException ex) {
		return new ApiError("conflict", ex.getMessage(), List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiError handleValidation(MethodArgumentNotValidException ex) {
		List<String> details = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.toList();
		return new ApiError("validation_error", "Invalid request body", details);
	}

	public record ApiError(
			String error,
			String message,
			List<String> details,
			Instant timestamp
	) {
		public ApiError(String error, String message, List<String> details) {
			this(error, message, details, Instant.now());
		}
	}
}
