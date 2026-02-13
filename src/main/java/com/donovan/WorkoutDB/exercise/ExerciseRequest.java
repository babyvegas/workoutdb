package com.donovan.WorkoutDB.exercise;

import jakarta.validation.constraints.NotBlank;

public record ExerciseRequest(
		@NotBlank(message = "name is required")
		String name,
		@NotBlank(message = "instructions are required")
		String instructions,
		@NotBlank(message = "primaryMuscle is required")
		String primaryMuscle,
		String secondaryMuscle
) {
}
