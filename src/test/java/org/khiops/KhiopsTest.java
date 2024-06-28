package org.khiops;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.khiops.v101.Calls.DatapathParam;
import org.khiops.v101.Calls.OperationA;
import org.khiops.v101.Calls.OperationB;
import org.khiops.v101.Calls.TrainPredictor;
import org.khiops.v101.Calls.Tuple;
import org.khiops.v101.Calls.TupleAlt;

@Tag("main")
class KhiopsTest {

	@Test
	void whenOpACreatedWithData_thenDataShouldMatch() {

        OperationA opA = OperationA.newBuilder()
            .addTable(Tuple.newBuilder()
                .setT1("Hello")
                .setT2("World!")
                .build())
            .build();

        assertEquals("Hello", opA.getTable(0).getT1());
        assertEquals("World!", opA.getTable(0).getT2());
	}

	@Test
	void whenOpBCreatedWithData_thenDataShouldMatch() {

        OperationB opB = OperationB.newBuilder()
            .addTable(TupleAlt.newBuilder()
                .setT1Value("Hello")
                .setT2Value("World!")
                //.setT2Bytevalue(ByteString.copyFrom("Whoever you want!".getBytes()))
                .build())
            .build();

        assertEquals("Hello", opB.getTable(0).getT1Value());
        assertEquals("World!", opB.getTable(0).getT2Value());
	}

	@Test
	void whenTrainParamsCreatedWithData_thenDataShouldMatch() {

        TrainPredictor train = TrainPredictor.newBuilder()
            .setDictionaryFilePath("xxx")
            .setDictionaryName("xxx")
            .setDataTablePath("xxx")
            .setTargetVariable("")
            .setResultsDir("xxx")
            .addAdditionalDataTable(
                DatapathParam.newBuilder()
                    .setDataPath("xxx")
                    .build())
            .build();

        assertEquals("xxx", train.getAdditionalDataTable(0).getDataPath());
	}

    @Test
    void convertToJson() throws com.google.protobuf.InvalidProtocolBufferException {
        // Extract json from params
        TrainPredictor message = TrainPredictor.newBuilder()
        .setDictionaryFilePath("xxx")
        .setDictionaryName("xxx")
        .setDataTablePath("xxx")
        .setTargetVariable("")
        .setResultsDir("xxx")
        .addAdditionalDataTable(
            DatapathParam.newBuilder()
                .setDataPath("xxx")
                .build())
        .build();

        // Iterate over message fields
        com.google.protobuf.Descriptors.Descriptor descriptor = message.getDescriptorForType();
        StringBuffer str = new StringBuffer();
        for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields()) {
            String fieldName = field.getName();
            Object fieldValue = message.getField(field);
            if ((field.hasPresence() && message.hasField(field)) || field.hasDefaultValue()) {
                str.append("Attribut : " + fieldName + ", Valeur : " + fieldValue + "\n");
            }
        }

        // Transformer le message en JSON
        String json = com.google.protobuf.util.JsonFormat.printer().print(message);
        System.out.println(json);

        assertEquals(str, json);
    }

}