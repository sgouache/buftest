package org.khiops;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;

import org.khiops.Calls.TrainPredictor;
import org.khiops.Calls.TrainPredictor.TrainPredictorAdditionalDataTablesTuple;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

public class KhiopsAPI {

    public static void train(TrainPredictor message) throws InvalidProtocolBufferException {
        String json = messageToJson(message);
        //String json = com.google.protobuf.util.JsonFormat.printer().includingDefaultValueFields().print(message);
        //String json = com.google.protobuf.util.JsonFormat.printer().alwaysPrintFieldsWithNoPresence().print(message);
        System.out.println(json);
        try {
            java.nio.file.Files.write(Paths.get("/tmp/output.json"), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            KhiopsTaskRunner.runStandardTask("test", "khiops", "templates/trainpredictor.kht", json, false);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        // Extract json from params
        TrainPredictor message = TrainPredictor.newBuilder()
        .setBytesDictionaryFilePath(ByteString.copyFrom("Whatever you want!".getBytes()))
        .setDictionaryName("xxx")
        .setDataTablePath("zzz")  
        .setBytesTargetVariable(ByteString.copyFrom(new byte[] { (byte)75, (byte)233, (byte)107, (byte)233 }))
        .setBytesAnalysisReportFilePath(ByteString.copyFrom("xxx".getBytes()))
        .addAdditionalDataTables(
            TrainPredictorAdditionalDataTablesTuple.newBuilder()
            .setDataPath("xxx")
            .setFilePath("yyy")
            .build()
        )
/*         .addAdditionalDataTables(
            TrainPredictorAdditionalDataTablesTuple.newBuilder()
            .setBytesDataPath(ByteString.copyFrom(new byte[] { (byte)233 }))
            .setBytesFilePath(ByteString.copyFrom("Some other bytes".getBytes()))
            .build()
        )
 */        .setGroupTargetValue(true)
        .setMaxTrees(5)


        .setMemoryLimitMb(1024) // FIXME: needed until we get a recent khiops build!
        .setMaxCores(4)
        .setTempDir("/tmp")

        .build(); 

        //KhiopsAPI.call(message);
        try {
            train(message);
        } catch (InvalidProtocolBufferException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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

}