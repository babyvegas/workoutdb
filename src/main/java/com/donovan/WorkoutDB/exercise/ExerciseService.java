package com.donovan.WorkoutDB.exercise;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExerciseService {

	private final ExerciseRepository exerciseRepository;

	public ExerciseService(ExerciseRepository exerciseRepository) {
		this.exerciseRepository = exerciseRepository;
	}

	public List<Exercise> getAll() {
		return exerciseRepository.findAll();
	}

	public Exercise getById(Long id) {
		return exerciseRepository.findById(id)
				.orElseThrow(() -> new ExerciseNotFoundException(id));
	}

	public Exercise create(ExerciseRequest request) {
		ExerciseRequest normalized = normalize(request);
		try {
			return exerciseRepository.create(normalized);
		} catch (DataIntegrityViolationException ex) {
			throw new DuplicateExerciseException(normalized.name());
		}
	}

	public Exercise update(Long id, ExerciseRequest request) {
		ExerciseRequest normalized = normalize(request);
		try {
			return exerciseRepository.update(id, normalized)
					.orElseThrow(() -> new ExerciseNotFoundException(id));
		} catch (DataIntegrityViolationException ex) {
			throw new DuplicateExerciseException(normalized.name());
		}
	}

	public void delete(Long id) {
		boolean deleted = exerciseRepository.deleteById(id);
		if (!deleted) {
			throw new ExerciseNotFoundException(id);
		}
	}

	private ExerciseRequest normalize(ExerciseRequest request) {
		String secondaryMuscle = request.secondaryMuscle();
		if (secondaryMuscle != null) {
			secondaryMuscle = secondaryMuscle.trim();
			if (secondaryMuscle.isBlank()) {
				secondaryMuscle = null;
			}
		}

		return new ExerciseRequest(
				request.name().trim(),
				request.instructions().trim(),
				request.primaryMuscle().trim(),
				secondaryMuscle
		);
	}
}
