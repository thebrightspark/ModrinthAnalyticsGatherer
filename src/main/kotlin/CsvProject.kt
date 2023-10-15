import java.util.*
import kotlin.reflect.jvm.jvmErasure

data class CsvProject(
	val timestamp: String,
	val id: String,
	val slug: String,
	val title: String,
	val color: Int?,
	val downloads: Int,
	val followers: Int,
	val updated: String,
	val lastVersion: String
) {
	companion object {
		private val REGEX_LIST_SEPARATOR = Regex("\\s*,\\s*")
		private val REGEX_FIELD_WORDS = Regex("[A-Z]?[a-z]+")

		// Done this so that I can ensure the order of properties mapped to columns in the CSV
		val PROPERTIES = arrayOf(
			CsvProject::timestamp,
			CsvProject::id,
			CsvProject::slug,
			CsvProject::title,
			CsvProject::color,
			CsvProject::downloads,
			CsvProject::followers,
			CsvProject::updated,
			CsvProject::lastVersion
		)

		private fun String.capitalise(): String =
			replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

		private fun String.toStringList(): List<String> = split(REGEX_LIST_SEPARATOR)

		fun generateCsvHeader(): Array<String> = PROPERTIES
			.map { prop -> REGEX_FIELD_WORDS.findAll(prop.name).joinToString(" ") { it.value.capitalise() } }
			.toTypedArray()

		fun fromCsvLine(line: Array<out String>): CsvProject = CsvProject(
			line[0],
			line[1],
			line[2],
			line[3],
			line[4].toIntOrNull(),
			line[5].toInt(),
			line[6].toInt(),
			line[7],
			line[8]
		)

		fun fromModrinthProject(modrinthProject: ModrinthProject, timestamp: String): CsvProject = CsvProject(
			timestamp,
			modrinthProject.id,
			modrinthProject.slug,
			modrinthProject.title,
			modrinthProject.color,
			modrinthProject.downloads,
			modrinthProject.followers,
			modrinthProject.updated,
			modrinthProject.versions.last()
		)
	}

	fun toCsvLine(): Array<String> = Array(PROPERTIES.size) { i ->
		val prop = PROPERTIES[i]
		val value = prop.get(this)
		return@Array when (prop.returnType.jvmErasure) {
			List::class -> (value as List<*>).joinToString(separator = ", ")
			else -> value.toString()
		}
	}
}
