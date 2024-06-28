#include <fstream>
#include <google/protobuf/util/time_util.h>
#include <iostream>
#include <string>
#include <google/protobuf/text_format.h>
#include "khiops/v101/calls.pb.h"
#include <cstddef>
#include <google/protobuf/util/json_util.h>
using namespace std;

int main(int argc, char* argv[]) {
  // Verify that the version of the library that we linked against is
  // compatible with the version of the headers we compiled against.
  GOOGLE_PROTOBUF_VERIFY_VERSION;

  if (argc != 3 || (std::string(argv[1]) != "-r" && std::string(argv[1]) != "-w")) {
    cerr << "Usage:  " << argv[0] << " -r|-w PROTO_FILE" << endl;
    return -1;
  }

  if (std::string(argv[1]) == "-r") {

    khiops::v101::KhiopsTask khiops_task;
    {
      // Read the existing task.
      fstream input(argv[2], ios::in | ios::binary);
      //if (!khiops_task.ParseFromIstream(&input)) {
      std::stringstream buffer;
      buffer << input.rdbuf();
      if (!google::protobuf::TextFormat::ParseFromString(buffer.str(), &khiops_task)) {
      //if (!khiops_task.ParseFromString(&input)) {
        cerr << "Failed to parse task." << endl;
        return -1;
      }
      for (int i=0; i<khiops_task.api_calls().size(); i++) {
        auto task = khiops_task.api_calls().Get(i);
        if (task.has_train_predictor()) {
          cout << "Train predictor" << endl;
          cout << task.train_predictor().dictionary_file_path() << endl; 
        }
      }
    }

  } else {
/*     {
      khiops::v101::OperationA opA;
      khiops::v101::Tuple *tuple = opA.add_table();
      tuple->set_t1("Hello");
      tuple->set_t2("World!");

      std::string textData;
      google::protobuf::TextFormat::PrintToString(opA, &textData);
      cout << textData << endl;
    }
    {
      khiops::v101::OperationB opB;
      khiops::v101::TupleAlt *tuple = opB.add_table();
      tuple->set_t1_value("Hello");
      const char *buf = "World!\01\02\03";
      tuple->set_t2_bytevalue(buf, strlen(buf));

      std::string textData;
      //google::protobuf::TextFormat::PrintToString(opB, &textData);
            google::protobuf::util::MessageToJsonString(opB, &textData);

      cout << textData << endl;
    } */
    {
      khiops::v101::TrainPredictorParams params;
      khiops::v101::PairParam *pair_param = params.add_pair_param();
      pair_param->set_pair_string_param1("Hello");
      const char *buf = "World!\01\02\03";
      pair_param->set_pair_string_param2_byte(buf, strlen(buf));

      khiops::v101::StringLike *vec_param1 = params.add_vector_param();
      vec_param1->set_value("XXX");
      khiops::v101::StringLike *vec_param2 = params.add_vector_param();
      vec_param2->set_bytevalue(buf, strlen(buf));

      khiops::v101::SingletonParam *singleton_param = params.add_singleton_param();
      singleton_param->set_singleton_string_param("Bonjour");
      khiops::v101::SingletonParam *singleton_param2 = params.add_singleton_param();
      const char *buf2 = "Toto!";
      singleton_param2->set_singleton_string_param_byte(buf2, strlen(buf2));

      google::protobuf::Map<std::string, std::string> *dict = params.mutable_dict_param();
      (*dict)["Toto"] = "tutu";
      (*dict)["Truc"] = "machin";

      std::string textData;
      //google::protobuf::TextFormat::PrintToString(opB, &textData);
      google::protobuf::json::PrintOptions options;
      options.always_print_fields_with_no_presence = true;
      google::protobuf::util::MessageToJsonString(params, &textData, options);

      cout << textData << endl;

      cout << "Optional string value is: " << params.optional_string_param() << endl;
    }
  }
}