import com.opencsv.CSVReader
import com.opencsv.bean.MappingStrategy
import java.time.Instant
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.jvmErasure

class ModrinthProjectMappingStrategy(private val timestamp: Instant) : MappingStrategy<ModrinthProject> {
	companion object {
		private val REGEX_FIELD_WORDS = Regex("[A-Z]?[a-z]+")
	}

	override fun captureHeader(reader: CSVReader) = Unit

	override fun populateNewBean(line: Array<out String>): ModrinthProject {
		throw NotImplementedError("Not implemented as shouldn't be needed")
	}

	override fun transmuteBean(bean: ModrinthProject): Array<String> = Array(ModrinthProject.PROPERTIES.size + 1) { i ->
		when (i) {
			0 -> timestamp.toString()
			else -> propertyToString(ModrinthProject.PROPERTIES[i - 1], bean)
		}
	}

	private fun propertyToString(property: KProperty1<ModrinthProject, *>, bean: ModrinthProject): String {
		val value = property.get(bean)
		return when (property.returnType.jvmErasure) {
			List::class -> (value as List<*>).joinToString(separator = ", ")
			else -> value.toString()
		}
	}

	override fun setType(type: Class<out ModrinthProject>) = Unit

	override fun generateHeader(bean: ModrinthProject): Array<String> {
		val names = ModrinthProject.PROPERTIES
			.map { prop -> REGEX_FIELD_WORDS.findAll(prop.name).joinToString(" ") { it.value.capitalise() } }
			.toMutableList()
		names.add(0, "Timestamp")
		return names.toTypedArray()
	}

	private fun String.capitalise(): String =
		replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
