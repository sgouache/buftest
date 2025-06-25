package org.khiops;

import org.khiops.Calls.DetectDataTableFormat;
import org.khiops.Calls.TrainPredictor;
import org.khiops.Calls.TrainPredictor.TrainPredictorAdditionalDataTablesTuple;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class KhiopsAPItest {

    public static void main(String[] args) {
        // Extract json from params
        TrainPredictor message = TrainPredictor.newBuilder()
        .setByteDictionaryFilePath(ByteString.copyFrom("Whatever you want!".getBytes()))
        .setDictionaryName("xxx")
        .setDataTablePath("zzz")  
        .setByteTargetVariable(ByteString.copyFrom(new byte[] { (byte)75, (byte)233, (byte)107, (byte)233 }))
        .setByteAnalysisReportFilePath(ByteString.copyFrom("xxx".getBytes()))
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

        try {
            KhiopsAPI.trainPredictor(message);
        } catch (InvalidProtocolBufferException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        DetectDataTableFormat msg = DetectDataTableFormat.newBuilder()
        .setDataTablePath("/home/nmms4680/khiops_data/samples/Adult/Adult.txt")

        .setMemoryLimitMb(1024) // FIXME: needed until we get a recent khiops build!
        .setMaxCores(4)
        .setTempDir("/tmp")
        .build();


        try {
            KhiopsAPI.detectDataTableFormat(msg);
        } catch (InvalidProtocolBufferException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

}