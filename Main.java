/**
 * Эта система предназначена для преобразования входного xml-файла таким образом, чтобы в результате преобразованный текст состоял из двух строк, где первая строка - xml-заголовок, а вторая строка - весь xml-контент документа.
 * Система должна позволять создавать новый файл, либо перезаписывать текущий.
 * Система должна проверять доступное место на диске. Если места недостаточно, то программа должна прерывать работу и писать в консоль сообщение о нехватки памяти; при этом она должна выводить текущий объём памяти и необходимый объём для нового файла.
 * Система должна вернуть весь стек вызовов, если произошло необработанное исключение. 
 */
package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.util.Optional;

import org.apache.commons.cli.*;

/**
 * @author Artyom
 *
 */
public class Main {
	/*
	 * "/" - Unix "\\" - Windows
	 */
	private static final String WINDOWS_OS_PATH_DELIMITER = "\\";
	private static final String UNIX_OS_PATH_DELIMITER = "\\";
	private static final boolean isDebug = true;
	private static final boolean isDebugProcess = true;

	/**
	 *  
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();

		Option inputFileNameOption = new Option("i", "inputFile", true, "input file path");
		inputFileNameOption.setRequired(true);
		options.addOption(inputFileNameOption);

		Option outputFileNameOption = new Option("o", "outputFile", true, "output file");
		outputFileNameOption.setRequired(false);
		options.addOption(outputFileNameOption);

		/*
		 * Option modeOption = new Option("m", "mode", true, "output file");
		 * modeOption.setRequired(false); options.addOption(modeOption);
		 */

		Option rewriteIfExistsOption = new Option("r", "rewriteIfExists", false,
				"If output file exists - then rewrite it");
		rewriteIfExistsOption.setRequired(false);
		
		options.addOption(rewriteIfExistsOption);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);

			String inputFileName = cmd.getOptionValue("inputFile");
			String outputFileName = cmd.getOptionValue("outputFile");
			/* String modeValue = cmd.getOptionValue("mode"); */
			boolean isRewriteIfExists = cmd.hasOption("rewriteIfExists"); 
			//boolean isRewriteIfExists = Optional.ofNullable(cmd.hasOption("rewriteIfExists")).orElse(false);
			
			
			if (isDebug) {
				/** debug */
				System.out.println(inputFileName);
				System.out.println(outputFileName);
				/* System.out.println(modeValue); */
				System.out.println(isRewriteIfExists);
				/** debug */
			}

			if (inputFileName.length() == 0) {
				System.out.println("Input file name is empty.");
				System.exit(1);
			}

			File inputFile = new File(inputFileName);

			if (inputFile.isDirectory()) {
				System.out.println("inputFileName' is a directory.");
				System.exit(2);
			}

			if (outputFileName != null) {
				if (outputFileName.length() == 0) {
					System.out.println("Output file name is empty.");
					System.exit(3);
				}

				File outputFile = new File(outputFileName);

				if (outputFile.isDirectory()) {
					System.out.println("'outputFileName' is a directory.");
					System.exit(4);
				}
			}

			/*
			 * if (modeValue.length() > 1) { throw new
			 * Exception("'mode' should be represented as single character"); } char mode =
			 * modeValue.charAt(0);
			 * 
			 * if (isDebug) {
			 *//** debug */
			/*
			 * System.out.println(mode);
			 *//** debug *//*
							 * }
							 */
			
			/*
			 * Проверим, что начальный и конечный файлы не совпадают. Необходимо также
			 * проверить возможность ссылки - когда путь является ссылкой, а не явным путём.
			 */
			if (outputFileName != null && new File(inputFileName).toPath().toRealPath().equals(new File(outputFileName).toPath().toRealPath()))
			{
				System.out.println("Input file name specified and output file name are the same. Need omit ouptutFileName to rewrite original file.");
				System.exit(6);
			}
			
			/*
			 * Проверим, что целевой указанный файл не существует, либо указан флаг перезаписи "rewriteIfExists"
			 */
			if (outputFileName != null && new File(outputFileName).exists() && !isRewriteIfExists)
			{
				System.out.println(
						"Output file exists already and rewriteIfExists option is not present. Specify not existing file name or -rewriteIfExists flag.");
				System.exit(7);
			}

			/*
			 * Если выходной файл не задан, то целимся в исходный файл.
			 * Установим флаг перезаписи, раз явно нужно перезаписать исходный файл
			 */
			if (outputFileName == null)
			{
				outputFileName = inputFileName;
				isRewriteIfExists = true;
			}

			/*
			 * Проверим, что на диске достаточно места
			 */
			long inputFileSize = inputFile.length();

			if (isDebug) {
				/** debug */
				System.out.println("inputFileSize = " + inputFileSize + " B");
				/** debug */
			}

			/*
			 * Определим, какой слэш используется в текущей системе для того, чтобы получить
			 * путь к корню диска, на который нужно писать файл.
			 */
			String OSpathDelimeter = null;
			if (System.getProperty("os.name").matches("Windows"))
			{
				OSpathDelimeter = WINDOWS_OS_PATH_DELIMITER;
			} else {
				OSpathDelimeter = UNIX_OS_PATH_DELIMITER;
			}

			/*
			 * Возьмём количество байт, свободных в корне.
			 */
			long outputFilePathFreeSpace = (new File(outputFileName.substring(0, outputFileName.indexOf(OSpathDelimeter)))).getFreeSpace();

			if (isDebug)
			{
				/** debug */
				System.out.println("getFreeSpace = " + outputFilePathFreeSpace);
				System.out.println("getFreeSpace/1024/1024 = " + outputFilePathFreeSpace / 1024 / 1024);
				System.out.println();
				/** debug */
			}

			/*
			 * Если размер входного файла больше, чем места для выходного файла:
			 */
			if (inputFileSize > outputFilePathFreeSpace) {
				System.out.println("Need additional freespace to write new file. Need " + inputFileSize / 1024 / 1024
						+ " MB more.");
				System.exit(5);
			}

			/*
			 * Запускаем обработку файла
			 */
			if (isDebugProcess) {
				processFile(inputFileName, outputFileName, /*mode, */isRewriteIfExists);
			}

		}
		catch (ParseException e)
		{
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", options);

			System.exit(8);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(9);
		}
	}

	/**
	 * @param inputFileName  - полный путь к исходному файлу
	 * @param outputFileName - полный путь к новому файлу
	 * @param isRewriteIfExists - если true - перезаписать файл, если он существует; иначе - сгенерировать исключение
	 */
	/*
	 * @param mode - режим, в котором должна работать программа. Конкретный режим
	 * определяет поведение программы в отношении создания файла и перезаписи
	 * иходного файла. Параметр является английской буквой и может быть в любом
	 * регистре. <ol> <li>r - (replace) перезаписать входной файл. При этом
	 * rewriteIfExists считается равным true <li>c - (create) создать новый файл с
	 * указанным именем, не изменять входной файл </ol>
	 */
	@SuppressWarnings("unused")
	public static void processFile(String inputFileName, String outputFileName,
			/* char mode, */boolean isRewriteIfExists) {

		System.out.println("Start of work: " + String.valueOf(LocalTime.now()));

		if (false) {
			Charset ch = Charset.defaultCharset();
			System.out.println(ch.name());
			System.out.println(ch.displayName());
			System.out.println(ch.toString());
			try {
				File tempFile = File.createTempFile(new File(inputFileName).getName(), ".tmp");
				System.out.println(tempFile.getPath());
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(10);
			}
		} else {

			try {

				/*
				 * Получим уникальное имя временного файла В него будем писать текст, после чего
				 * переименуем файл в требуемое название
				 */
				File tempFile = File.createTempFile(new File(inputFileName).getName(), ".tmp");

				/* TimeUnit.SECONDS.sleep(3); */
				/*
				 * Открываем буферное чтение для исходного файла
				 */
				BufferedReader bufferedReader = new BufferedReader(new FileReader(
						inputFileName/* "J:\\MyTemp\\XML_test\\XML-bill_3813158_OOO_MELON_TEST.xml" *//*
																										 * "J:\\MyTemp\\XML_test\\XML-bill 70021563 OOO LSR Nedvizhimost 03.01.2018.xml"
																										 */));

				/*
				 * Создаём целевой файл
				 */
				PrintWriter writer = new PrintWriter(/* outputFile */tempFile/*
																				 * "J:\\MyTemp\\XML_test\\XML-bill 70021563 OOO LSR Nedvizhimost 03.01.2018 new.xml"
																				 *//* , "UTF-8" */);

				/*
				 * Читаем строку из исходного файла и записываем её в конечный файл, пока не
				 * дойдём до конца файла:
				 */

				/*
				 * Первую строку запишем с переносом каретки, т.к. это должен быть
				 * XML-заголовок.
				 */
				/*
				 * Сначала встречает такое. И это нужно объединить в одну отдельную строку.
				 * <?xml version="1.0" encoding="wi ndows-1251" ?>
				 */
				String currentLine = bufferedReader.readLine();
				writer.print(currentLine); // <?xml version="1.0" encoding="wi
//			System.out.println(currentLine);

				currentLine = bufferedReader.readLine();
				writer.println(currentLine); // ndows-1251" ?>
//			System.out.println(currentLine);

				/*
				 * Читаем следующую строку - уже с первой строкой, к которой нужно приклеять
				 * остальные
				 */
				currentLine = bufferedReader.readLine();

				while (currentLine != null) {
					/*
					 * Записываем полученную строку в целевой файл
					 */

					writer.print(currentLine);

					/*
					 * Читаем следующую строку
					 */
					currentLine = bufferedReader.readLine();

				}
				
				writer.println();

				/*
				 * Закрываем целевой файл
				 */
				writer.close();

				/*
				 * Закрываем буферный читальщик
				 */
				bufferedReader.close();

				/*
				 * Переносим временный файл в запрошенное место
				 */
				// tempFile
				Path temp = null;
				String tempFilePath = tempFile.getPath();
				String outputFilePath = new File(outputFileName).getPath();
				if (isRewriteIfExists) {
					temp = Files.move(Paths.get(tempFilePath), // separatorChar
							Paths.get(outputFilePath), StandardCopyOption.REPLACE_EXISTING);
				} else {
					temp = Files.move(Paths.get(tempFilePath), Paths.get(outputFilePath));
				}

				if (temp != null) {
					System.out
							.println("File " + tempFilePath + " renamed and moved successfully into " + outputFilePath);
				} else {
					System.out.println("Failed to move the file " + tempFilePath + " into " + outputFilePath);
					System.exit(11);
				}

			}
			/*
			 * catch (InterruptedException e) { System.out.println("Can't sleep");
			 * e.printStackTrace(); System.exit(12); }
			 */
			catch (IOException e) {
				System.out.println("Error while reading or writing file");
				e.printStackTrace();
				System.exit(13);
			}
		}

		System.out.println("End of work: " + String.valueOf(LocalTime.now()));

	}

}
