import kotlinx.serialization.Serializable

@Serializable
data class ModrinthProject(
	val id: String,
	val slug: String,
	val title: String,
	val color: Int?,
	val downloads: Int,
	val followers: Int,
	val updated: String,
	val versions: List<String>,
	val gameVersions: List<String>,
	val loaders: List<String>
) {
	companion object {
		// Done this so that I can ensure the order of properties mapped to columns in the CSV
		val PROPERTIES = arrayOf(
			ModrinthProject::id,
			ModrinthProject::slug,
			ModrinthProject::title,
			ModrinthProject::color,
			ModrinthProject::downloads,
			ModrinthProject::followers,
			ModrinthProject::updated,
			ModrinthProject::versions,
			ModrinthProject::gameVersions,
			ModrinthProject::loaders
		)
	}
}
