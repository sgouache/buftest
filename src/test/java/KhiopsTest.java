package org.khiops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.khiops.v101.Calls.*;
import com.google.protobuf.ByteString;

class ProtobufCodeGenerationUnitTest {

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
                //.setT2Value("World!")
                .setT2Bytevalue(ByteString.copyFrom("Whoever you want!".getBytes()))
                .build())
            .build();

        assertEquals("Hello", opB.getTable(0).getT1Value());
        assertEquals("World!", opB.getTable(0).getT2Value());
	}

	@Test
	void whenTrainParamsCreatedWithData_thenDataShouldMatch() {

        TrainPredictorParams train = TrainPredictorParams.newBuilder()
        .setDictionaryFilePath("xxx")
        .setDictionaryName("yyy")
            .addTable(TupleAlt.newBuilder()
                .setT1Value("Hello")
                //.setT2Value("World!")
                .setT2Bytevalue(ByteString.copyFrom("Whoever you want!".getBytes()))
                .build())
            .build();

        // Extract json from params
        train.get

        assertEquals("Hello", train.getTable(0).getT1Value());
        assertEquals("World!", train.getTable(0).getT2Value());
	}

}