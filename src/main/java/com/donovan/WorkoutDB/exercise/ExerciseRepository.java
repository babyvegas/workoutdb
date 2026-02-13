package com.donovan.WorkoutDB.exercise;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ExerciseRepository {

	private static final String BASE_SELECT = """
			SELECT id, name, instructions, primary_muscle, secondary_muscle, equipment
			FROM exercises
			""";

	private final JdbcTemplate jdbcTemplate;
	private final RowMapper<Exercise> exerciseRowMapper = (rs, rowNum) -> new Exercise(
			rs.getLong("id"),
			rs.getString("name"),
			rs.getString("instructions"),
			rs.getString("primary_muscle"),
			rs.getString("secondary_muscle"),
			rs.getString("equipment")
	);

	public ExerciseRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Exercise> findAll() {
		return jdbcTemplate.query(BASE_SELECT + " ORDER BY name ASC", exerciseRowMapper);
	}

	public List<Exercise> findByFilters(
			String muscle,
			String primaryMuscle,
			String secondaryMuscle,
			String equipment
	) {
		StringBuilder sql = new StringBuilder(BASE_SELECT + " WHERE 1 = 1");
		List<Object> params = new ArrayList<>();

		if (hasText(muscle)) {
			sql.append(" AND (LOWER(primary_muscle) LIKE ? OR LOWER(COALESCE(secondary_muscle, '')) LIKE ?)");
			String pattern = "%" + muscle.trim().toLowerCase() + "%";
			params.add(pattern);
			params.add(pattern);
		}

		if (hasText(primaryMuscle)) {
			sql.append(" AND LOWER(primary_muscle) LIKE ?");
			params.add("%" + primaryMuscle.trim().toLowerCase() + "%");
		}

		if (hasText(secondaryMuscle)) {
			sql.append(" AND LOWER(COALESCE(secondary_muscle, '')) LIKE ?");
			params.add("%" + secondaryMuscle.trim().toLowerCase() + "%");
		}

		if (hasText(equipment)) {
			sql.append(" AND LOWER(COALESCE(equipment, '')) LIKE ?");
			params.add("%" + equipment.trim().toLowerCase() + "%");
		}

		sql.append(" ORDER BY name ASC");

		return jdbcTemplate.query(sql.toString(), exerciseRowMapper, params.toArray());
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
							INSERT INTO exercises (name, instructions, primary_muscle, secondary_muscle, equipment)
							VALUES (?, ?, ?, ?, ?)
							""",
					Statement.RETURN_GENERATED_KEYS
			);
			statement.setString(1, request.name());
			statement.setString(2, request.instructions());
			statement.setString(3, request.primaryMuscle());
			statement.setString(4, request.secondaryMuscle());
			statement.setString(5, request.equipment());
			return statement;
		}, keyHolder);

		Number generatedId = keyHolder.getKey();
		if (generatedId == null) {
			throw new IllegalStateException("Could not generate id for exercise");
		}

		return findById(generatedId.longValue())
				.orElseThrow(() -> new IllegalStateException("Could not load created exercise"));
	}

	public boolean createIfNotExists(ExerciseRequest request) {
		return jdbcTemplate.update(
				"""
						INSERT OR IGNORE INTO exercises (name, instructions, primary_muscle, secondary_muscle, equipment)
						VALUES (?, ?, ?, ?, ?)
						""",
				request.name(),
				request.instructions(),
				request.primaryMuscle(),
				request.secondaryMuscle(),
				request.equipment()
		) > 0;
	}

	public Optional<Exercise> update(Long id, ExerciseRequest request) {
		int updatedRows = jdbcTemplate.update(
				"""
						UPDATE exercises
						SET name = ?, instructions = ?, primary_muscle = ?, secondary_muscle = ?, equipment = ?
						WHERE id = ?
						""",
				request.name(),
				request.instructions(),
				request.primaryMuscle(),
				request.secondaryMuscle(),
				request.equipment(),
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

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
