import com.opencsv.bean.CsvToBeanBuilder
import com.opencsv.bean.StatefulBeanToCsvBuilder
import com.opencsv.enums.CSVReaderNullFieldIndicator
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
import org.apache.commons.lang3.time.StopWatch
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import kotlin.io.path.reader
import kotlin.io.path.writer

val propertiesPath: Path = Path.of("app.properties")
lateinit var properties: Properties
const val csvNameKey = "csvName"
const val modrinthUsernameKey = "modrinthUsername"
const val modrinthUserAgentKey = "modrinthUserAgent"
const val githubTokenKey = "githubToken"
const val gistIdKey = "gistId"

val githubContentType = ContentType("application", "vnd.github+json")

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

fun getProp(prop: String): String = properties.getProperty(prop)

suspend fun main() {
	val timestamp = Instant.now().toString()
	println("Started at $timestamp")
	val stopwatch = StopWatch.createStarted()

	println("\nLoading properties")
	loadProperties()
	var splitTime: String = stopwatch.splitAndGetString()
	println("Loaded properties ($splitTime)")

	println("\nGetting Modrinth projects")
	val projects = getModrinthProjects()
	splitTime = stopwatch.splitAndGetString()
	println("Got ${projects.size} Modrinth projects ($splitTime):\n${projects.joinToString("\n")}")

	println("\nGetting Gist")
	val gistCsv = getGist(getProp(githubTokenKey))
	splitTime = stopwatch.splitAndGetString()
	println("Got Gist ($splitTime)")

	println("\nParsing projects from Gist")
	val csvProjects = parseCsv(gistCsv).toMutableList()
	splitTime = stopwatch.splitAndGetString()
	println("Read ${csvProjects.size} projects ($splitTime)")
	csvProjects.addAll(projects.map { CsvProject.fromModrinthProject(it, timestamp) })

	println("\nConverting ${csvProjects.size} projects to CSV")
	val csvString = projectsToCsv(csvProjects)
	splitTime = stopwatch.splitAndGetString()
	println("Converted projects ($splitTime)")

	println("\nUpdating Gist")
	updateGist(getProp(githubTokenKey), GitHubGistContent.of(getProp(csvNameKey), csvString))
	splitTime = stopwatch.splitAndGetString()
	println("Updated Gist ($splitTime)")

	stopwatch.stop()
	println("\nFinished (${stopwatch.formatTime()})")
}

fun StopWatch.splitAndGetString(): String {
	split()
	return formatSplitTime()
}

fun loadProperties() {
	if (Files.exists(propertiesPath)) {
		properties = Properties().apply { propertiesPath.reader().use { load(it) } }
	} else {
		Properties().run {
			setProperty(csvNameKey, "modrinthProjectAnalytics.csv")
			setProperty(modrinthUsernameKey, "")
			setProperty(modrinthUserAgentKey, "")
			setProperty(githubTokenKey, "")
			setProperty(gistIdKey, "")
			propertiesPath.writer().use { store(it, null) }
		}
		error("No properties file found - created $propertiesPath with empty values")
	}
}

suspend fun parseCsv(csv: String): List<CsvProject> = withContext(Dispatchers.IO) {
	csv.reader().use {
		CsvToBeanBuilder<CsvProject>(it)
			.withMappingStrategy(ProjectMappingStrategy)
			.withOrderedResults(true)
			.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
			.withIgnoreEmptyLine(true)
			.withSkipLines(1)
			.build()
			.parse()
	}
}

suspend fun projectsToCsv(projects: List<CsvProject>): String = withContext(Dispatchers.IO) {
	StringWriter().use { writer ->
		StatefulBeanToCsvBuilder<CsvProject>(writer)
			.withMappingStrategy(ProjectMappingStrategy)
			.withOrderedResults(true)
			.withApplyQuotesToAll(false)
			.build()
			.run { projects.forEach { write(it) } }
		writer.toString()
	}
}

suspend fun getModrinthProjects(): List<ModrinthProject> = withContext(Dispatchers.IO) {
	client.get("https://api.modrinth.com/v2/user/${getProp(modrinthUsernameKey)}/projects") {
		userAgent(getProp(modrinthUserAgentKey))
	}.run {
		handleResponseError()
		return@run body()
	}
}

suspend fun getGist(githubToken: String): String = withContext(Dispatchers.IO) {
	client.get("https://api.github.com/gists/${getProp(gistIdKey)}") {
		bearerAuth(githubToken)
		accept(githubContentType)
	}.run {
		handleResponseError()
		return@run body<GitHubGistContent>().files.getValue(getProp(csvNameKey)).content
	}
}

suspend fun updateGist(githubToken: String, fileContents: GitHubGistContent): Unit = withContext(Dispatchers.IO) {
	client.patch("https://api.github.com/gists/${getProp(gistIdKey)}") {
		bearerAuth(githubToken)
		accept(githubContentType)
		contentType(ContentType.Application.Json)
		setBody(fileContents)
	}.run {
		handleResponseError()
	}
}

fun HttpResponse.handleResponseError() {
	if (!status.isSuccess())
		error("Non-successful response from ${request.method} ${request.url}: $status")
}
