import kotlinx.serialization.Serializable

@Serializable
class GitHubGistContent(val files: Map<String, GitHubGistFileContent>) {
	companion object {
		fun of(fileName: String, content: String): GitHubGistContent =
			GitHubGistContent(mapOf(fileName to GitHubGistFileContent(content)))
	}
}

@Serializable
class GitHubGistFileContent(val content: String)
