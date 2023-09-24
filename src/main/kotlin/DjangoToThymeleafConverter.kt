import java.nio.file.Files
import java.nio.file.Paths

object DjangoToThymeleafConverter {

    private val conversions = listOf(
        Conversion("\\{\\{\\s*(.*?)\\s*\\}\\}", "\\\${$1}"),
        Conversion("\\{%\\s*for\\s+(.*?)\\s+in\\s+(.*?)\\s*%\\}", "<th:block th:each=\"$1 : \\\${$2}\">"),
        Conversion("\\{%\\s*endfor\\s*%\\}", "</th:block>"),
        Conversion("\\{%\\s*if\\s+(.*?)\\s*%\\}", "<th:block th:if=\"\\\$\\{$1\\}\">"),
        Conversion("\\{%\\s*else\\s*%\\}", "<th:block th:unless=\"\\\$\\{#bools.isTrue(#vars.previousExpression)}\">"),
        Conversion("\\{%\\s*endif\\s*%\\}", "</th:block>"),
        Conversion("\\{\\{\\s*(.*?)\\|toMoney\\s*\\}\\}", "\\\${#numbers.formatDecimal($1, 1, 'COMMA', 2, 'POINT')}")
    )

    fun convert(djangoTemplate: String): String {
        var thymeleafTemplate = djangoTemplate

        // Add Thymeleaf namespace if not exists
        if (!thymeleafTemplate.contains("xmlns:th=\"http://www.thymeleaf.org\"")) {
            thymeleafTemplate = thymeleafTemplate.replace("<html", "<html xmlns:th=\"http://www.thymeleaf.org\"")
        }

        conversions.forEach { conversion ->
            thymeleafTemplate = thymeleafTemplate.replace(Regex(conversion.from), conversion.to)
        }
        return thymeleafTemplate
    }

    fun convertFile(inputPath: String, outputPath: String) {
        val djangoTemplate = String(Files.readAllBytes(Paths.get(inputPath)))
        val thymeleafTemplate = convert(djangoTemplate)
        Files.write(Paths.get(outputPath), thymeleafTemplate.toByteArray())
    }

    data class Conversion(val from: String, val to: String)
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Please provide the input file path as a program argument.")
        return
    }

    val inputPath = args[0]
    val outputPath = if (args.size > 1) args[1] else "src/main/resources/converted_thymeleaf_template.html"

    // Create resources directory if it doesn't exist
    val resourcesDir = Paths.get("src/main/resources")
    if (Files.notExists(resourcesDir)) {
        Files.createDirectories(resourcesDir)
    }

    DjangoToThymeleafConverter.convertFile(inputPath, outputPath)
    println("Conversion completed. Output written to $outputPath")
}
