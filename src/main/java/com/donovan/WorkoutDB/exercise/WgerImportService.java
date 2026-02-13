package com.donovan.WorkoutDB.exercise;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class WgerImportService {

	private static final String WGER_BASE_URL = "https://wger.de";
	private static final String WGER_EXERCISE_URL = WGER_BASE_URL + "/api/v2/exerciseinfo/";
	private static final String WGER_MUSCLE_URL = WGER_BASE_URL + "/api/v2/muscle/";
	private static final int PAGE_SIZE = 200;
	private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	private final RestClient restClient;
	private final ExerciseService exerciseService;

	public WgerImportService(ExerciseService exerciseService) {
		this.restClient = RestClient.create();
		this.exerciseService = exerciseService;
	}

	public ImportSummary importExercises(int languageId, int maxRecords) {
		Map<Integer, String> muscleLookup = loadMuscleLookup();
		Set<String> seenNames = new HashSet<>();

		int requested = Math.max(1, maxRecords);
		int fetched = 0;
		int imported = 0;
		int skipped = 0;

		String nextUrl = UriComponentsBuilder.fromUriString(WGER_EXERCISE_URL)
				.queryParam("language", languageId)
				.queryParam("limit", Math.min(PAGE_SIZE, requested))
				.build()
				.toUriString();

		while (nextUrl != null && fetched < requested) {
			JsonNode payload = fetchJson(nextUrl);
			JsonNode results = payload.path("results");
			if (!results.isArray()) {
				break;
			}

			for (JsonNode exerciseNode : results) {
				if (fetched >= requested) {
					break;
				}
				fetched++;

				Optional<ExerciseRequest> mappedExercise = mapExercise(exerciseNode, muscleLookup, languageId);
				if (mappedExercise.isEmpty()) {
					skipped++;
					continue;
				}

				ExerciseRequest candidate = mappedExercise.get();
				String dedupeKey = candidate.name().toLowerCase(Locale.ROOT);
				if (!seenNames.add(dedupeKey)) {
					skipped++;
					continue;
				}

				boolean created = exerciseService.createIfNotExists(candidate);
				if (created) {
					imported++;
				} else {
					skipped++;
				}
			}

			nextUrl = normalizeNextUrl(textOrNull(payload.path("next")));
		}

		return new ImportSummary("wger", languageId, requested, fetched, imported, skipped, Instant.now());
	}

	private Map<Integer, String> loadMuscleLookup() {
		Map<Integer, String> muscleLookup = new HashMap<>();
		String nextUrl = UriComponentsBuilder.fromUriString(WGER_MUSCLE_URL)
				.queryParam("limit", PAGE_SIZE)
				.build()
				.toUriString();

		while (nextUrl != null) {
			JsonNode payload = fetchJson(nextUrl);
			JsonNode results = payload.path("results");
			if (!results.isArray()) {
				break;
			}

			for (JsonNode muscleNode : results) {
				int muscleId = muscleNode.path("id").asInt(-1);
				if (muscleId <= 0) {
					continue;
				}
				String muscleName = firstNonBlank(
						textOrNull(muscleNode.path("name_en")),
						textOrNull(muscleNode.path("name"))
				);
				if (muscleName != null) {
					muscleLookup.put(muscleId, cleanText(muscleName));
				}
			}

			nextUrl = normalizeNextUrl(textOrNull(payload.path("next")));
		}

		return muscleLookup;
	}

	private Optional<ExerciseRequest> mapExercise(
			JsonNode exerciseNode,
			Map<Integer, String> muscleLookup,
			int languageId
	) {
		String name = firstNonBlank(
				textOrNull(exerciseNode.path("name")),
				findTranslationText(exerciseNode.path("translations"), languageId, "name"),
				findTranslationText(exerciseNode.path("translations"), 0, "name")
		);
		name = cleanText(name);

		if (name == null) {
			return Optional.empty();
		}

		String rawInstructions = firstNonBlank(
				textOrNull(exerciseNode.path("description")),
				findTranslationText(exerciseNode.path("translations"), languageId, "description"),
				findTranslationText(exerciseNode.path("translations"), 0, "description")
		);
		String instructions = cleanInstructions(rawInstructions);
		if (instructions == null) {
			instructions = "No instructions provided.";
		}

		String primaryMuscle = extractMuscleName(exerciseNode.path("muscles"), muscleLookup);
		if (primaryMuscle == null) {
			primaryMuscle = "Unknown";
		}

		String secondaryMuscle = extractMuscleName(exerciseNode.path("muscles_secondary"), muscleLookup);
		String equipment = extractEquipment(exerciseNode.path("equipment"));

		return Optional.of(new ExerciseRequest(name, instructions, primaryMuscle, secondaryMuscle, equipment));
	}

	private String extractMuscleName(JsonNode musclesNode, Map<Integer, String> muscleLookup) {
		if (!musclesNode.isArray() || musclesNode.isEmpty()) {
			return null;
		}

		JsonNode muscleNode = musclesNode.get(0);
		if (muscleNode == null || muscleNode.isNull()) {
			return null;
		}

		if (muscleNode.isTextual()) {
			return cleanText(muscleNode.asText());
		}

		if (muscleNode.canConvertToInt()) {
			return cleanText(muscleLookup.get(muscleNode.asInt(-1)));
		}

		if (muscleNode.isObject()) {
			String name = firstNonBlank(
					textOrNull(muscleNode.path("name_en")),
					textOrNull(muscleNode.path("name"))
			);
			if (name != null) {
				return cleanText(name);
			}

			int muscleId = muscleNode.path("id").asInt(-1);
			if (muscleId > 0) {
				return cleanText(muscleLookup.get(muscleId));
			}
		}

		return null;
	}

	private String findTranslationText(JsonNode translationsNode, int languageId, String field) {
		if (!translationsNode.isArray()) {
			return null;
		}

		String firstAvailable = null;
		for (JsonNode translationNode : translationsNode) {
			String value = textOrNull(translationNode.path(field));
			if (value == null) {
				continue;
			}

			if (firstAvailable == null) {
				firstAvailable = value;
			}

			if (languageId > 0 && translationNode.path("language").asInt(-1) == languageId) {
				return value;
			}
		}

		return firstAvailable;
	}

	private String extractEquipment(JsonNode equipmentNode) {
		if (!equipmentNode.isArray() || equipmentNode.isEmpty()) {
			return null;
		}

		List<String> equipments = new ArrayList<>();
		for (JsonNode item : equipmentNode) {
			String name;
			if (item.isObject()) {
				name = firstNonBlank(
						textOrNull(item.path("name")),
						textOrNull(item.path("name_en"))
				);
			} else {
				name = textOrNull(item);
			}

			name = cleanText(name);
			if (name != null && !equipments.contains(name)) {
				equipments.add(name);
			}
		}

		if (equipments.isEmpty()) {
			return null;
		}

		return String.join(", ", equipments);
	}

	private JsonNode fetchJson(String url) {
		try {
			JsonNode json = restClient.get()
					.uri(url)
					.retrieve()
					.body(JsonNode.class);
			if (json == null) {
				throw new ExternalApiException("wger returned an empty response", null);
			}
			return json;
		} catch (RestClientException ex) {
			throw new ExternalApiException("Could not fetch data from wger", ex);
		}
	}

	private String normalizeNextUrl(String nextUrl) {
		if (nextUrl == null || nextUrl.isBlank()) {
			return null;
		}
		if (nextUrl.startsWith("http://") || nextUrl.startsWith("https://")) {
			return nextUrl;
		}
		if (nextUrl.startsWith("/")) {
			return WGER_BASE_URL + nextUrl;
		}
		return WGER_BASE_URL + "/" + nextUrl;
	}

	private String cleanInstructions(String instructions) {
		if (instructions == null || instructions.isBlank()) {
			return null;
		}

		String normalized = instructions
				.replaceAll("(?i)<br\\s*/?>", " ")
				.replaceAll("(?i)</p>", " ");
		String noTags = HTML_TAG_PATTERN.matcher(normalized).replaceAll(" ");
		String decoded = decodeBasicHtmlEntities(noTags);
		String collapsed = WHITESPACE_PATTERN.matcher(decoded).replaceAll(" ").trim();
		return collapsed.isBlank() ? null : collapsed;
	}

	private String cleanText(String value) {
		if (value == null) {
			return null;
		}
		String normalized = WHITESPACE_PATTERN.matcher(decodeBasicHtmlEntities(value)).replaceAll(" ").trim();
		return normalized.isBlank() ? null : normalized;
	}

	private String decodeBasicHtmlEntities(String value) {
		return value
				.replace("&nbsp;", " ")
				.replace("&amp;", "&")
				.replace("&quot;", "\"")
				.replace("&#39;", "'")
				.replace("&lt;", "<")
				.replace("&gt;", ">");
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private String textOrNull(JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			return null;
		}
		if (!node.isValueNode()) {
			return null;
		}
		String value = node.isTextual() ? node.textValue() : node.toString();
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}

	public record ImportSummary(
			String source,
			int language,
			int requested,
			int fetched,
			int imported,
			int skipped,
			Instant importedAt
	) {
	}
}
