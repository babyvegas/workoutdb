package com.donovan.WorkoutDB.exercise;

public record Exercise(
		Long id,
		String name,
		String instructions,
		String primaryMuscle,
		String secondaryMuscle
) {
}
