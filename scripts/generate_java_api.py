import json
import sys
import os
from pathlib import Path

def to_pascal_case(name):
    return name.replace("_", " ").title().replace(" ", "")


def convert_json_folder(input_json_folder, output_gen_folder):
    output_java_folder = os.path.join(output_gen_folder, "java", "org", "khiops")
    output_templates_folder = os.path.join(output_gen_folder, "resources", "templates")
    os.makedirs(output_java_folder, exist_ok=True)
    os.makedirs(output_templates_folder, exist_ok=True)
    lines = []
    lines.append('''package org.khiops;

import java.io.IOException;
import com.google.protobuf.InvalidProtocolBufferException;

public class KhiopsAPI {
''')
    
    for entry in os.scandir(input_json_folder):
        print('processing {}'.format(entry.path))
        try:
            with open(entry.path, 'r', encoding='utf-8') as f:
                json_data = json.load(f)
        except Exception as e:
            print(f"Error reading JSON file: {e}")
            sys.exit(1)

        name = json_data.get("name")
        if not name:
            print("Warning: 'name' field is missing in JSON.")
            sys.exit(1)
        tool_name = json_data.get("tool")
        if not tool_name:
            print("Warning: 'tool' field is missing in JSON.")
            sys.exit(1)
        scenario = json_data.get("scenario")
        if not scenario:
            print("Warning: 'scenario' field is missing in JSON.")
            # Temporary: allow missing scenario template
            scenario = ''
            # sys.exit(1)

        message_name = to_pascal_case(name)
        method_name = message_name[0].lower() + message_name[1:]
        print(name, message_name, tool_name)

        output_template_path = os.path.join(output_templates_folder, '{}.kht'.format(name))
        try:
            with open(output_template_path, 'w', encoding='utf-8') as f:
                f.write(scenario)
            print(f"Scenario template file generated at {output_template_path}")
        except Exception as e:
            print(f"Error writing scenario template file: {e}")
            sys.exit(1)

 
        lines.append('    /**')
        for d in json_data.get("description_sections")[0].get("lines"):
            lines.append('     * {}'.format(d))

        lines.append('     * @param message  The configuration message for the task')
        lines.append('     */')

        lines.append('''
    public static void {}(Calls.{} message) throws InvalidProtocolBufferException {{
                     
        String json = KhiopsTaskRunner.messageToJson(message);
        System.out.println(json);

        try {{
            KhiopsTaskRunner.runStandardTask("{}", "{}", "templates/{}.kht", json, false);
        }} catch (IOException e) {{
            // TODO Auto-generated catch block
            e.printStackTrace();
        }} catch (InterruptedException e) {{
            // TODO Auto-generated catch block
            e.printStackTrace();
        }}
    }}
'''.format(method_name, message_name, name, tool_name, name))
    
    lines.append('}')

    java_code = "\n".join(lines)
    output_java_path = os.path.join(output_java_folder, 'KhiopsAPI.java')
    try:
        with open(output_java_path, 'w', encoding='utf-8') as f:
            f.write(java_code)
        print(f"Java file generated at {output_java_path}")
    except Exception as e:
        print(f"Error writing java file: {e}")
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python generate_java_api.py input_folder output_gen_folder")
        sys.exit(1)

    input_json_path = sys.argv[1]
    output_gen_path = sys.argv[2]

    # Check if the path refers to a directory.
    if os.path.isdir(input_json_path) and os.path.isdir(output_gen_path):
        # Handle complete directory conversion
        convert_json_folder(input_json_path, output_gen_path)

    else:
        print(f"Invalid arguments, refer to usage")
        sys.exit(1)