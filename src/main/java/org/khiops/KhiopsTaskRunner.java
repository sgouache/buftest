package org.khiops;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;


public class KhiopsTaskRunner {

    private static final String PROTO_TOOL_DIR = "/usr/bin";
    private static final String PROTO_TEMP_DIR = "/tmp";

    private static final List<String> SPECIAL_KEYS = Arrays.asList(
            "log_file_path", "output_scenario_path", "task_file_path", "trace",
            "stdout_file_path", "stderr_file_path", "scenario_prologue",
            "force_ansi_scenario", "batch_mode"
    );

    // Utility: snake_case to camelCase
    private static String snakeToCamel(String snake) {
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else {
                if (upperNext) {
                    result.append(Character.toUpperCase(c));
                    upperNext = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    // Utility: convert camelCase to byte-prefixed key (simulate Python's b"key")
    private static String toByteName(String camelKey) {
        // In Python, b"key" is bytes literal; here we simulate by prefixing "b" or similar
        // Since keys are strings in Java Map, just prefix with "b" to simulate
        return "b" + camelKey;
    }

    // Trace method placeholder
    private static void trace(String msg) {
        System.out.println(msg);
    }

    // Example JSON parser method (replace with your preferred JSON library)
    private static Map<String, Object> parseJsonToMap(String json) throws IOException {
        // Minimal JSON parsing using Jackson (if available)
        // Otherwise, implement or use another library
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.readValue(json, Map.class);
    }

    // get a file from the resources folder
    // works everywhere, IDEA, unit test and JAR file.
    private static InputStream getFileFromResourceAsStream(String fileName) {
        ClassLoader classLoader = KhiopsTaskRunner.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
    
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

    private static Object[] extractDetectFormatResults(String logFilePath) throws IOException, KhiopsRuntimeError {
        // Parse the log file to obtain the header_line and field_separator parameters
        // Notes:
        // - If there is an error the run method will raise an exception; so at this stage we
        //   have a warning in the worst case.
        // - The contents of this Khiops execution are always ASCII
        BufferedReader logFile = new BufferedReader(new FileReader(logFilePath));
        
        List<String> logFileLines = new ArrayList<>();
        String line;
        while ((line = logFile.readLine()) != null) {
            logFileLines.add(line);
        }
        logFile.close();

        // Obtain the first line that contains the format spec
        Boolean headerLine = null;
        String fieldSeparator = null;
        String formatLinePattern = "File format detected: ";
        for (String currentLine : logFileLines) {
            if (currentLine.startsWith(formatLinePattern)) {
                String rawFormatSpec = currentLine.trim().replace(formatLinePattern, "");
                String[] formatParts = rawFormatSpec.split(" and field separator ");
                String headerLineStr = formatParts[0];
                String fieldSeparatorStr = formatParts[1];
                
                headerLine = headerLineStr.equals("header line");
                if (fieldSeparatorStr.equals("tabulation")) {
                    fieldSeparator = "\t";
                } else {
                    fieldSeparator = String.valueOf(fieldSeparatorStr.charAt(1));
                }
                break;
            }
        }

        // Fail if there was no file format in the log
        if (headerLine == null || fieldSeparator == null) {
            throw new KhiopsRuntimeError(
                "Khiops did not write the log line with the data table file format."
            );
        }
        System.out.println("Header line: " + headerLine);
        System.out.println("Separator: "+ fieldSeparator + "EOL");

        return new Object[] { headerLine, fieldSeparator };
    }

    public static Object[] runStandardTask(String name, String tool, String scenarioResourcePath, String localsJson, boolean nopOutputScenario) throws IOException, InterruptedException {
        // Parse JSON string to Map
        Map<String, Object> localsDict = parseJsonToMap(localsJson);

        // Ensure temp directory exists
        Files.createDirectories(Paths.get(PROTO_TEMP_DIR));

        Map<String, Object> specialValues = new HashMap<>();

        // Extract special values from localsDict
        for (String key : SPECIAL_KEYS) {
            String camelKey = snakeToCamel(key);
            String byteCamelKey = toByteName(camelKey);

            Object value = null;
            if (localsDict.containsKey(camelKey)) {
                value = localsDict.remove(camelKey);
            } else if (localsDict.containsKey(byteCamelKey)) {
                value = localsDict.remove(byteCamelKey);
                // Decode base64 string to bytes
                if (value instanceof String) {
                    value = Base64.getDecoder().decode((String) value);
                }
            }

            if (value != null) {
                specialValues.put(key, value);
            }
        }

        // Load scenario template
        String scenario = readStream(getFileFromResourceAsStream(scenarioResourcePath));

        trace(String.format("Run name (%s, %d) %s", tool, scenario.length(), localsDict));
        trace(" Special params: " + specialValues);

        // Initialize variables with defaults
        String logFilePath = getStringOrDefault(specialValues.get("log_file_path"), "");
        String outputScenarioPath = getStringOrDefault(specialValues.get("output_scenario_path"), "");
        String taskFilePath = getStringOrDefault(specialValues.get("task_file_path"), "");
        String stdoutFilePath = getStringOrDefault(specialValues.get("stdout_file_path"), "");
        String stderrFilePath = getStringOrDefault(specialValues.get("stderr_file_path"), "");
        Object scenarioPrologueObj = specialValues.get("scenario_prologue");
        boolean forceAnsiScenario = getBooleanOrDefault(specialValues.get("force_ansi_scenario"), false);
        boolean batchMode = getBooleanOrDefault(specialValues.get("batch_mode"), true);

        // Determine tool executable path
        Path toolExePath;
        if ("khiops".equals(tool)) {
            toolExePath = Paths.get(PROTO_TOOL_DIR, "MODL_openmpi");
        } else {
            toolExePath = Paths.get(PROTO_TOOL_DIR, "MODL_coclustering_openmpi");
        }
        if (!Files.isRegularFile(toolExePath)) {
            throw new FileNotFoundException("Missing " + tool + " executable at " + toolExePath);
        }

        // Set default file paths if not specified
        if (logFilePath.isEmpty()) {
            logFilePath = Paths.get(PROTO_TEMP_DIR, "log.txt").toString();
        }
        if (stdoutFilePath.isEmpty()) {
            stdoutFilePath = Paths.get(PROTO_TEMP_DIR, "stdout.txt").toString();
        }
        if (stderrFilePath.isEmpty()) {
            stderrFilePath = Paths.get(PROTO_TEMP_DIR, "stderr.txt").toString();
        }

        // Write scenario file, potentially with a prologue
        Path scenarioPath = Paths.get(PROTO_TEMP_DIR, "scenario.prm");
        if (scenarioPrologueObj != null) {
            // scenarioPrologue can be byte[] or String
            try (OutputStream os = Files.newOutputStream(scenarioPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                if (scenarioPrologueObj instanceof byte[]) {
                    os.write((byte[]) scenarioPrologueObj);
                } else if (scenarioPrologueObj instanceof String) {
                    os.write(((String) scenarioPrologueObj).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("scenario_prologue must be byte[] or String");
                }
                os.write(scenario.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            Files.write(scenarioPath, scenario.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        // Write JSON parameter file
        Path jsonParamPath = Paths.get(PROTO_TEMP_DIR, "param.json");
        String jsonContent = toJson(localsDict);
        Files.write(jsonParamPath, jsonContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Prepare command-line arguments
        List<String> khiopsParams = new ArrayList<>();
        khiopsParams.add(toolExePath.toString());
        khiopsParams.add("-i");
        khiopsParams.add(scenarioPath.toString());
        khiopsParams.add("-j");
        khiopsParams.add(jsonParamPath.toString());

        if (!logFilePath.isEmpty()) {
            khiopsParams.add("-e");
            khiopsParams.add(logFilePath);
        }
        if (!outputScenarioPath.isEmpty()) {
            khiopsParams.add(nopOutputScenario ? "-O" : "-o");
            khiopsParams.add(outputScenarioPath);
        }
        if (!taskFilePath.isEmpty()) {
            khiopsParams.add("-p");
            khiopsParams.add(taskFilePath);
        }
        if (batchMode) {
            khiopsParams.add("-b");
        }

        trace(" Khiops params: " + khiopsParams);

        // Run subprocess
        ProcessBuilder pb = new ProcessBuilder(khiopsParams);
        pb.redirectInput(ProcessBuilder.Redirect.PIPE);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);

        Process process = pb.start();

        boolean finished = process.waitFor(100_000, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Process timeout expired");
        }

        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());

        // Save outputs
        if (!stdoutFilePath.isEmpty()) {
            Files.write(Paths.get(stdoutFilePath), stdout.getBytes(StandardCharsets.UTF_8));
        }
        if (!stderrFilePath.isEmpty()) {
            Files.write(Paths.get(stderrFilePath), stderr.getBytes(StandardCharsets.UTF_8));
        }

        Object[] result = {};
        if (name.equalsIgnoreCase("detect_data_table_format"))
            result = extractDetectFormatResults(logFilePath);

        // TODO cleanup trace & logfile

        return result;
    }

    // Helper: read InputStream fully as String
    private static String readStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString();
        }
    }

    // Helper: get String or default from Object
    private static String getStringOrDefault(Object obj, String defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof byte[]) {
            return new String((byte[]) obj, StandardCharsets.UTF_8);
        }
        return obj.toString();
    }

    // Helper: get boolean or default from Object
    private static boolean getBooleanOrDefault(Object obj, boolean defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof String) return Boolean.parseBoolean((String) obj);
        return defaultVal;
    }

    // Simple JSON serializer for Map<String,Object> (only handles primitives and strings)
    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            sb.append("  \"").append(escapeJson(e.getKey())).append("\": ");
            sb.append(toJsonValue(e.getValue()));
            if (it.hasNext()) sb.append(",");
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String toJsonValue(Object val) {
        if (val == null) return "null";
        if (val instanceof String) return "\"" + escapeJson((String) val) + "\"";
        if (val instanceof Number || val instanceof Boolean) return val.toString();
        if (val instanceof byte[]) return "\"" + Base64.getEncoder().encodeToString((byte[]) val) + "\"";
        if (val instanceof Map) return toJson((Map<String, Object>) val);
        if (val instanceof Collection) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            Iterator<?> it = ((Collection<?>) val).iterator();
            while (it.hasNext()) {
                sb.append(toJsonValue(it.next()));
                if (it.hasNext()) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(val.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    public static String messageToJson(GeneratedMessage message) throws InvalidProtocolBufferException {
        HashSet<FieldDescriptor> fields = new HashSet<>();
    
        Builder builder = message.toBuilder();
    
        // Iterate over message fields
        Descriptor descriptor = builder.getDescriptorForType();
        StringBuffer str = new StringBuffer();
        for (FieldDescriptor field : descriptor.getFields()) {
            String fieldName = field.getName();
            Object fieldValue = builder.getField(field);
    
            // TODO si aucun des oneof n'est défini, forcer l'attribut string à la valeur ""
            //System.out.println("Attribut: " + fieldName + ", valeur: " + fieldValue + "\n");
            OneofDescriptor oneof = field.getContainingOneof();
            if (oneof != null && oneof.getFields().size() == 2 && field.getJavaType() == FieldDescriptor.JavaType.STRING && fieldValue.equals("")) {
                // Drop current field from oneof
                List<FieldDescriptor> filtered = oneof.getFields().stream()
                .filter(f -> !field.equals(f))
                .collect(java.util.stream.Collectors.toList());
    
                if (filtered.get(0).getName().equals("byte_"+fieldName)) {
                    ByteString s = (ByteString)message.getField(filtered.get(0));
    
                    System.out.println(s);
                    if (s.size() == 0) {
                        System.out.println("Forcing value of "+fieldName+" to \"\"");
                        builder.setField(field, "");
                        fields.add(field);
                    }
                }
            }
    
    
            if ((field.hasPresence() && message.hasField(field)) || field.hasDefaultValue() || field.isRepeated()) {
                //str.append("Attribut : " + fieldName + ", Valeur : " + fieldValue + "\n");
                //message2.getF().putIfAbsent(field, fieldValue);
                fields.add(field);
            }
        }
        //System.out.println(str);
        Message updatedMessage = builder.build();
    
        // Transformer le message en JSON
        //String json = com.google.protobuf.util.JsonFormat.printer().includingDefaultValueFields(fields).print(message);
        String json = com.google.protobuf.util.JsonFormat.printer().includingDefaultValueFields(fields).print(updatedMessage);
        return json;
    }
}
