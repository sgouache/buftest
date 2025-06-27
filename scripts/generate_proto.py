import json
import sys
import os
from pathlib import Path

def generate_proto_from_json_path(input_json_path):
    try:
        with open(input_json_path, 'r', encoding='utf-8') as f:
            json_data = json.load(f)
    except Exception as e:
        print(f"Error reading JSON file: {e}")
        sys.exit(1)

    name = json_data.get("name")
    if not name:
        print("Warning: 'name' field is missing in JSON.")
        name = "GeneratedMessage"

    arguments = json_data.get("arguments", [])
    if not isinstance(arguments, list):
        print("Warning: 'arguments' should be a list. Using empty list.")
        arguments = []

    lines = []

    lines.append('/*')
    for d in json_data.get("description_sections")[0].get("lines"):
        lines.append(' {}'.format(d))
    lines.append(' */')

    msg_name = to_pascal_case(name)
    lines.append('message {} {{\n'.format(msg_name))
    
    field_number = 1

    for arg in arguments:
        if not isinstance(arg, dict):
            print(f"Warning: Argument {arg} is not a dict. Skipping.")
            continue

        arg_name = arg.get("name")
        if not arg_name:
            print("Warning: An argument is missing 'name'. Skipping.")
            continue

        arg_type = arg.get("arg_type")
        default = arg.get("default")
        description = arg.get("description", "")
        if not description:
            print(f"Warning: Argument {arg_name} is missing 'description'. Skipping.")
            continue
        description = description.replace("\n", " ").replace("\"", "\\\"")

        if arg_type == "str":
            # Generate oneof for string and bytes
            lines.append('  oneof {}_option {{'.format(arg_name))
            lines.append('    // {}' .format(description))
            default_str = default if default is not None else ""
            lines.append('    string {} = {} [default = "{}"];'.format(arg_name, field_number, default_str))
            field_number += 1
            lines.append('    // {}' .format(description))
            lines.append('    bytes byte_{} = {};'.format(arg_name, field_number))
            lines.append('  }')
        elif arg_type == "bool":
            default_value = ' [default = {}]'.format(default.lower()) if default != 'None' else ''
            lines.append('  // {}' .format(description))
            lines.append('  optional bool {} = {}{};'.format(arg_name, field_number, default_value))
        elif arg_type == "int":
            default_value = ' [default = {}]'.format(default.lower()) if default != 'None' else 0
            lines.append('  // {}' .format(description))
            lines.append('  optional int32 {} = {}{};'.format(arg_name, field_number, default_value))
        elif arg_type == "float":
            default_value = default if default is not None else 0.0
            lines.append('  // {}' .format(description))
            lines.append('  optional float {} = {} [default = {}];'.format(arg_name, field_number, default_value))
        elif arg_type == "list":
            # Check for tuple_fields
            tuple_fields = arg.get("tuple_fields", [])
            if tuple_fields:
                nested_message_name = "{}_{}_tuple".format(name, arg_name)
                nested_message_name = to_pascal_case(nested_message_name)
                lines.append('  message {} {{'.format(nested_message_name))
                for field in tuple_fields:
                    if not isinstance(field, dict):
                        print(f"Warning: Tuple field {field} is not a dict. Skipping.")
                        continue
                    fname = field.get("name")
                    ftype = field.get("arg_type")
                    default_f = field.get("default")
                    f_desc = field.get("description", "").replace("\n", " ").replace("\"", "\\\"")
                    if not fname:
                        print("Warning: A tuple field is missing 'name'. Skipping.")
                        continue
                    if ftype == "str":
                        lines.append('    oneof {}_option {{'.format(fname))
                        lines.append('      // {}' .format(f_desc))
                        lines.append('      string {} = {} [default = "{}"];'.format(fname, field_number, default_f))
                        field_number += 1
                        lines.append('      // {}' .format(f_desc))
                        lines.append('      bytes byte_{} = {};'.format(fname, field_number))
                        lines.append('    }')
                    elif ftype == "int":
                        def_val = default_f if default_f is not None else 0
                        lines.append('    // {}' .format(f_desc))
                        lines.append('    optional int32 {} = {} [default = {}];'.format(fname, field_number, def_val))
                    elif ftype == "bool":
                        def_val = default_f if default_f is not None else "false"
                        lines.append('    // {}' .format(f_desc))
                        lines.append('    optional bool {} = {} [default = {}];'.format(fname, field_number, str(def_val).lower()))
                    else:
                        print(f"Warning: Unsupported tuple field type '{ftype}'. Skipping.")
                    field_number += 1
                lines.append('  }\n')
                lines.append('  // {}' .format(description))
                lines.append('  repeated {} {} = {};'.format(nested_message_name, arg_name, field_number))
            else:
                # Default to list of strings
                lines.append('  // {}' .format(description))
                lines.append('  repeated string {} = {};'.format(arg_name, field_number))
        else:
            # Default to string
            lines.append('  // {}' .format(description))
            default_str = default if default is not None else ""
            lines.append('  optional string {} = {} [default = "{}"];'.format(arg_name, field_number, default_str))
        lines.append('')
        field_number += 1

    lines.append('}\n')
    return msg_name, name, lines

def to_pascal_case(name):
    return name.replace("_", " ").title().replace(" ", "")

def convert_json_file(input_json_path, output_proto_path):
    lines = []
    lines.append('syntax = "proto2";\n')
    lines.append('package khiops.v11;')
    lines.append('option java_package = "org.khiops";')
    lines.append('option go_package = "khiops.org/proto/khiops";\n')
    msg_name, field_name, proto = generate_proto_from_json_path(input_json_path)
    lines += proto

    proto_code =  "\n".join(lines)

    try:
        with open(output_proto_path, 'w', encoding='utf-8') as f:
            f.write(proto_code)
        print(f"Proto file generated at {output_proto_path}")
    except Exception as e:
        print(f"Error writing proto file: {e}")
        sys.exit(1)

def convert_json_folder(input_json_folder, output_proto_folder):
    lines = []
    lines.append('syntax = "proto2";\n')
    lines.append('package khiops.v11;')
    lines.append('option java_package = "org.khiops";')
    lines.append('option go_package = "khiops.org/proto/khiops";\n')

    messages = []
    for entry in sorted(map(lambda x: x.path,
                    os.scandir(input_json_folder))):
        print('processing {}'.format(entry))
        msg_name, field_name,  proto = generate_proto_from_json_path(entry)
        lines += proto
        # FIXME: should be returned by gen_proto function
        messages.append({'name': msg_name, 'id': field_name})

    lines.append('// A call to the Khiops API')
    lines.append('message KhiopsApiCall {')
    lines.append('    oneof api_calls {')

    field_number = 1
    for msg in messages:
        lines.append('        {} {} = {};'.format(msg['name'], msg['id'], field_number))
        field_number += 1
    lines.append('    }')
    lines.append('}\n')

    lines.append('// A task to be executed by Khiops')
    lines.append('// It may be composed of one or more KhiopsApiCalls')
    lines.append('message KhiopsTask {')
    lines.append('    repeated KhiopsApiCall api_calls = 1;')
    lines.append('}')

    proto_code = "\n".join(lines)
    output_proto_path = os.path.join(output_proto_folder, 'calls.proto')
    try:
        with open(output_proto_path, 'w', encoding='utf-8') as f:
            f.write(proto_code)
        print(f"Proto file generated at {output_proto_path}")
    except Exception as e:
        print(f"Error writing proto file: {e}")
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python generate_proto.py input_json_file output_proto_file")
        print("       python generate_proto.py input_folder output_proto_folder")
        sys.exit(1)

    input_json_path = sys.argv[1]
    output_proto_path = sys.argv[2]

    # Check if the path refers to a directory.
    if os.path.isdir(input_json_path) and os.path.isdir(output_proto_path):
        # Handle complete directory conversion
        convert_json_folder(input_json_path, output_proto_path)

    # Check if the path refers to a regular file.
    elif os.path.isfile(input_json_path):
        # Convert single file
        convert_json_file(input_json_path, output_proto_path)

    else:
        print(f"Invalid arguments, refer to usage")
        sys.exit(1)