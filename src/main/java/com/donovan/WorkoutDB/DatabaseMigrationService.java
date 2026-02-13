package com.donovan.WorkoutDB;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseMigrationService {

	private final JdbcTemplate jdbcTemplate;

	public DatabaseMigrationService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@PostConstruct
	public void ensureEquipmentColumn() {
		List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(exercises)");
		boolean hasEquipmentColumn = columns.stream()
				.map(column -> String.valueOf(column.get("name")))
				.anyMatch("equipment"::equalsIgnoreCase);

		if (!hasEquipmentColumn) {
			jdbcTemplate.execute("ALTER TABLE exercises ADD COLUMN equipment TEXT");
		}
	}
}
