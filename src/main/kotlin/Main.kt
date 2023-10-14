import com.opencsv.bean.StatefulBeanToCsvBuilder
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import java.time.Instant

const val username = "bright_spark"
const val modrinthProjectsUrl = "https://api.modrinth.com/v2/user/$username/projects"
const val userAgent = "thebrightspark/ModrinthAnalyticsGatherer/1.0.0"
val csvPath: Path = Path.of("modrinthProjectAnalytics.csv")

@OptIn(ExperimentalSerializationApi::class)
val client = HttpClient(CIO) {
	install(ContentNegotiation) {
		json(Json {
			ignoreUnknownKeys = true
			namingStrategy = JsonNamingStrategy.SnakeCase
		})
	}
	install(Logging) {
		logger = Logger.SIMPLE
		level = LogLevel.INFO
	}
}

suspend fun main() {
	val timestamp = Instant.now()
	println("Started at $timestamp")
	val projects = get<List<ModrinthProject>>(modrinthProjectsUrl)
	println("Got Modrinth projects:\n${projects.joinToString("\n")}")

	withContext(Dispatchers.IO) {
		Files.newBufferedWriter(csvPath, CREATE, WRITE, APPEND).use { writer ->
			StatefulBeanToCsvBuilder<ModrinthProject>(writer)
				.withOrderedResults(true)
				.withMappingStrategy(ModrinthProjectMappingStrategy(timestamp))
				.withApplyQuotesToAll(false)
				.build()
				.run { projects.forEach { write(it) } }
		}
	}

	println("Finished")
}

suspend inline fun <reified T> get(url: String): T = client.get(url) {
	userAgent(userAgent)
}.run {
	if (!status.isSuccess())
		error("Non-successful response from ${this.request.method} ${this.request.url}")
	return@run body<T>()
}
