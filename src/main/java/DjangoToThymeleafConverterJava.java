import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DjangoToThymeleafConverterJava {

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Please provide the input file path as a program argument.");
			return;
		}

		String inputPath = args[0];
		String outputPath = args.length > 1 ? args[1] : "src/main/resources/converted_thymeleaf_template.html";

		// Create resources directory if it doesn't exist
		Path resourcesDir = Paths.get("src/main/resources");
		if (!Files.exists(resourcesDir)) {
			try {
				Files.createDirectories(resourcesDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			convertFile(inputPath, outputPath);
			System.out.println("Conversion completed. Output written to " + outputPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void convertFile(String inputPath, String outputPath) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(inputPath));
		String djangoTemplate = new String(bytes);
		String thymeleafTemplate = convert(djangoTemplate);
		Files.write(Paths.get(outputPath), thymeleafTemplate.getBytes());
	}

	public static String convert(String djangoTemplate) {
		String thymeleafTemplate = djangoTemplate;

		// Add Thymeleaf namespace if not exists
		if (!thymeleafTemplate.contains("xmlns:th=\"http://www.thymeleaf.org\"")) {
			thymeleafTemplate = thymeleafTemplate.replace("<html", "<html xmlns:th=\"http://www.thymeleaf.org\"");
		}

		List<Conversion> conversions = Arrays.asList(
			new Conversion("\\{\\{\\s*(.*?)\\s*\\}\\}", "\\${$1}"),
			new Conversion("\\{%\\s*for\\s+(.*?)\\s+in\\s+(.*?)\\s*%\\}", "<th:block th:each=\"$1 : \\${$2}\">"),
			new Conversion("\\{%\\s*endfor\\s*%\\}", "</th:block>"),
			new Conversion("\\{%\\s*if\\s+(.*?)\\s*%\\}", "<th:block th:if=\"\\${$1}\">"),
			new Conversion("\\{%\\s*else\\s*%\\}", "<th:block th:unless=\"\\${#bools.isTrue(#vars.previousExpression)}\">"),
			new Conversion("\\{%\\s*endif\\s*%\\}", "</th:block>"),
			new Conversion("\\{\\{\\s*(.*?)\\|toMoney\\s*\\}\\}", "\\${#numbers.formatDecimal($1, 1, 'COMMA', 2, 'POINT')}")
		);

		for (Conversion conversion : conversions) {
			Pattern pattern = Pattern.compile(conversion.from);
			Matcher matcher = pattern.matcher(thymeleafTemplate);
			thymeleafTemplate = matcher.replaceAll(conversion.to);
		}

		return thymeleafTemplate;
	}

	static class Conversion {
		String from;
		String to;

		public Conversion(String from, String to) {
			this.from = from;
			this.to = to;
		}
	}
}
