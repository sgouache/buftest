#include <fstream>
#include <google/protobuf/util/time_util.h>
#include <iostream>
#include <string>
#include <google/protobuf/text_format.h>
#include "khiops/simple/v101/calls.pb.h"

using namespace std;

int main(int argc, char* argv[]) {
  // Verify that the version of the library that we linked against is
  // compatible with the version of the headers we compiled against.
  GOOGLE_PROTOBUF_VERIFY_VERSION;

  if (argc != 2) {
    cerr << "Usage:  " << argv[0] << " PROTO_FILE" << endl;
    return -1;
  }

  khiops::simple::v101::KhiopsTask khiops_task;


  {
    // Read the existing task.
    fstream input(argv[1], ios::in | ios::binary);
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

}