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
	val versions: List<String>
)
