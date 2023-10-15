import com.opencsv.CSVReader
import com.opencsv.bean.MappingStrategy

object ProjectMappingStrategy : MappingStrategy<CsvProject> {
	override fun captureHeader(reader: CSVReader) = Unit

	override fun populateNewBean(line: Array<out String>): CsvProject = CsvProject.fromCsvLine(line)

	override fun transmuteBean(bean: CsvProject): Array<String> = bean.toCsvLine()

	override fun setType(type: Class<out CsvProject>) = Unit

	override fun generateHeader(bean: CsvProject): Array<String> = CsvProject.generateCsvHeader()
}
