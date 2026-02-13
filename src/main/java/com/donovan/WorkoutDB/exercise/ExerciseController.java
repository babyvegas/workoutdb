package com.donovan.WorkoutDB.exercise;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

	private final ExerciseService exerciseService;

	public ExerciseController(ExerciseService exerciseService) {
		this.exerciseService = exerciseService;
	}

	@GetMapping
	public List<Exercise> getAll(
			@RequestParam(required = false) String muscle,
			@RequestParam(required = false) String primaryMuscle,
			@RequestParam(required = false) String secondaryMuscle,
			@RequestParam(required = false) String equipment
	) {
		if (hasAnyFilter(muscle, primaryMuscle, secondaryMuscle, equipment)) {
			return exerciseService.getByFilters(muscle, primaryMuscle, secondaryMuscle, equipment);
		}
		return exerciseService.getAll();
	}

	@GetMapping("/{id}")
	public Exercise getById(@PathVariable Long id) {
		return exerciseService.getById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Exercise create(@Valid @RequestBody ExerciseRequest request) {
		return exerciseService.create(request);
	}

	@PutMapping("/{id}")
	public Exercise update(@PathVariable Long id, @Valid @RequestBody ExerciseRequest request) {
		return exerciseService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		exerciseService.delete(id);
	}

	private boolean hasAnyFilter(String muscle, String primaryMuscle, String secondaryMuscle, String equipment) {
		return hasText(muscle) || hasText(primaryMuscle) || hasText(secondaryMuscle) || hasText(equipment);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
