package org.khiops;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;

import org.khiops.v101.Calls.DatapathParam;
import org.khiops.v101.Calls.TrainPredictor;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

public class KhiopsAPI {

    public static void main(String[] args) throws IOException {
        // Extract json from params
        TrainPredictor message = TrainPredictor.newBuilder()
        .setByteDictionaryFilePath(ByteString.copyFrom("Whatever you want!".getBytes()))
        .setDictionaryName("xxx")
        .setDataTablePath("zzz")  
    .setByteTargetVariable(ByteString.copyFrom(new byte[] { (byte)75, (byte)233, (byte)107, (byte)233 }))
        .setByteResultsDir(ByteString.copyFrom("xxx".getBytes()))
        .addAdditionalDataTable(
            DatapathParam.newBuilder()
            .setDataPath("xxx")
            .setFilePath("yyy")
            .build()
        )
        .addAdditionalDataTable(
            DatapathParam.newBuilder()
            .setByteDataPath(ByteString.copyFrom(new byte[] { (byte)233 }))
            .setByteFilePath(ByteString.copyFrom("Some other bytes".getBytes()))
            .build()
        )
        .build();

        String json = messageToJson(message);
        //String json = com.google.protobuf.util.JsonFormat.printer().includingDefaultValueFields().print(message);
        //String json = com.google.protobuf.util.JsonFormat.printer().alwaysPrintFieldsWithNoPresence().print(message);
        System.out.println(json);

        KhiopsAPI api = new KhiopsAPI();
        InputStream is = api.getFileFromResourceAsStream("templates/trainpredictor.kht");

        //printInputStream(is);

        ScenarioTemplate template = ScenarioTemplate.parseFromInputStream(is);
        System.out.println(template);
        //String renderedTemplate = ScenarioTemplate.render(is, json);
        ByteBuffer rendered = ByteBuffer.allocate(10*1024);
        template.render(json, rendered);
        //System.out.println(Charset.defaultCharset().decode(rendered).toString());
        System.out.println("Final output: "+rendered.position());
        System.out.println(new String(rendered.array()));
        
        byte[] output = new byte[rendered.position()];
        int len = rendered.position();
        rendered.rewind();
        rendered.get(output, 0, len);
        java.nio.file.Files.write(Paths.get("/tmp/output.kh"), output);
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
            System.out.println("Attribut: " + fieldName + ", valeur: " + fieldValue + "\n");
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

    // get a file from the resources folder
    // works everywhere, IDEA, unit test and JAR file.
    private InputStream getFileFromResourceAsStream(String fileName) {

        // The class loader that loaded the class
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

    // print input stream
    private static void printInputStream(InputStream is) {

        try (InputStreamReader streamReader =
                    new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}