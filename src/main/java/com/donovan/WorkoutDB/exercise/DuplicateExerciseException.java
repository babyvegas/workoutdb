package com.donovan.WorkoutDB.exercise;

public class DuplicateExerciseException extends RuntimeException {

	public DuplicateExerciseException(String name) {
		super("Exercise already exists with name: " + name);
	}
}
