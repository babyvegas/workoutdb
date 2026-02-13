package com.donovan.WorkoutDB.exercise;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ExerciseRepository {

	private static final String BASE_SELECT = """
			SELECT id, name, instructions, primary_muscle, secondary_muscle
			FROM exercises
			""";

	private final JdbcTemplate jdbcTemplate;
	private final RowMapper<Exercise> exerciseRowMapper = (rs, rowNum) -> new Exercise(
			rs.getLong("id"),
			rs.getString("name"),
			rs.getString("instructions"),
			rs.getString("primary_muscle"),
			rs.getString("secondary_muscle")
	);

	public ExerciseRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Exercise> findAll() {
		return jdbcTemplate.query(BASE_SELECT + " ORDER BY name ASC", exerciseRowMapper);
	}

	public Optional<Exercise> findById(Long id) {
		return jdbcTemplate.query(
				BASE_SELECT + " WHERE id = ?",
				exerciseRowMapper,
				id
		).stream().findFirst();
	}

	public Exercise create(ExerciseRequest request) {
		KeyHolder keyHolder = new GeneratedKeyHolder();

		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(
					"""
							INSERT INTO exercises (name, instructions, primary_muscle, secondary_muscle)
							VALUES (?, ?, ?, ?)
							""",
					Statement.RETURN_GENERATED_KEYS
			);
			statement.setString(1, request.name());
			statement.setString(2, request.instructions());
			statement.setString(3, request.primaryMuscle());
			statement.setString(4, request.secondaryMuscle());
			return statement;
		}, keyHolder);

		Number generatedId = keyHolder.getKey();
		if (generatedId == null) {
			throw new IllegalStateException("Could not generate id for exercise");
		}

		return findById(generatedId.longValue())
				.orElseThrow(() -> new IllegalStateException("Could not load created exercise"));
	}

	public Optional<Exercise> update(Long id, ExerciseRequest request) {
		int updatedRows = jdbcTemplate.update(
				"""
						UPDATE exercises
						SET name = ?, instructions = ?, primary_muscle = ?, secondary_muscle = ?
						WHERE id = ?
						""",
				request.name(),
				request.instructions(),
				request.primaryMuscle(),
				request.secondaryMuscle(),
				id
		);

		if (updatedRows == 0) {
			return Optional.empty();
		}

		return findById(id);
	}

	public boolean deleteById(Long id) {
		return jdbcTemplate.update("DELETE FROM exercises WHERE id = ?", id) > 0;
	}
}
