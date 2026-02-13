package com.donovan.WorkoutDB.exercise;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/exercises/import")
public class ExerciseImportController {

	private final WgerImportService wgerImportService;

	public ExerciseImportController(WgerImportService wgerImportService) {
		this.wgerImportService = wgerImportService;
	}

	@PostMapping("/wger")
	public WgerImportService.ImportSummary importFromWger(
			@RequestParam(defaultValue = "2")
			@Min(value = 1, message = "language must be >= 1")
			int language,
			@RequestParam(defaultValue = "200")
			@Min(value = 1, message = "limit must be >= 1")
			@Max(value = 2000, message = "limit must be <= 2000")
			int limit
	) {
		return wgerImportService.importExercises(language, limit);
	}
}
