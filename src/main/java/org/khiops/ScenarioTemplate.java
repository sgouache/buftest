package org.khiops;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScenarioTemplate {

    ArrayList<ScenarioSection> sections = new ArrayList<ScenarioSection>();

    private ScenarioTemplate() {
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        for (ScenarioSection section: sections) {
            if (section instanceof ScenarioConditional) {
                buffer.append("IF "+((ScenarioConditional)section).conditional+"\n");
                buffer.append("   "+section.lines+"\n");
            } else if (section instanceof ScenarioLoop) {
                buffer.append("FOR "+((ScenarioLoop)section).array+"\n");
                buffer.append("   "+section.lines+"\n");
            } else if (section instanceof ScenarioSection) {
                buffer.append(" "+section.lines+"\n");
            }
        }

        return buffer.toString();
    }

    public static ScenarioTemplate parseFromInputStream(InputStream is) throws IOException {

        ScenarioTemplate template = new ScenarioTemplate();

        InputStreamReader streamReader =
            new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);


        Pattern pattern = Pattern.compile("\\s*(([a-zA-Z.]+)\\s+)?__(\\w+)__(\\s+)?(//.*)?");
        Pattern ctrlPattern = Pattern.compile("(LOOP|IF|END)\\s+(__(\\w+)__|LOOP|IF)");

        ScenarioSection section = null;

        String line;
        while ((line = reader.readLine()) != null) {
            //System.out.println("Line = "+line);
            Matcher ctrlMatcher = ctrlPattern.matcher(line);
            Matcher matcher = pattern.matcher(line);

            if (ctrlMatcher.matches()) {
                System.out.println("== " + ctrlMatcher.group(1) + " (" + ctrlMatcher.group(2) + ")");
                if (ctrlMatcher.group(1).equals("LOOP")) {
                    System.out.println("Begin loop");
                    section = new ScenarioLoop(ctrlMatcher.group(3));
                    template.sections.add(section);
                }
                if (ctrlMatcher.group(1).equals("IF")) {
                    System.out.println("Begin cond");
                    section = new ScenarioConditional(ctrlMatcher.group(3));
                    template.sections.add(section);
                }
                if (ctrlMatcher.group(1).equals("END")) {
                    System.out.println("End cond or loop");
                    section = null;
                }
            } else {

                if (section == null) {
                    section = new ScenarioSection();
                    template.sections.add(section);
                }

                if (matcher.matches() && matcher.groupCount() >= 3) {
                    System.out.println("== Param   " + matcher.group(1) + " Param: " + matcher.group(3));
                    section.lines.add(new ScenarioLine(matcher.group(1), matcher.group(3)));

                } else {
                    System.out.println("== Plain   " + line);
                    section.lines.add(new ScenarioLine(line, null));

                }
            }
        }

        return template;
    }

    public void render(String in, ByteBuffer buffer) {
        JSONObject json = new JSONObject(in);
        for (ScenarioSection section: sections) {
            if (section instanceof ScenarioConditional) {
                //System.out.println("Render conditional section \n");
                if (json.getBoolean(((ScenarioConditional)section).conditional)) {
                    renderLines(section.lines, json, buffer);
                    //System.out.println("conditional section: "+buffer.position());
                    //System.out.println(new String(buffer.array()));
                }
            } else if (section instanceof ScenarioLoop) {
                //System.out.println("Render array section \n");
                JSONArray array = json.getJSONArray(((ScenarioLoop)section).array);
                for (int i=0; i<array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    renderLines(section.lines, obj, buffer);
                    //System.out.println("array section: "+buffer.position());
                    //System.out.println(new String(buffer.array()));
                }
            } else if (section instanceof ScenarioSection) {
                //System.out.println("Render normal section \n");
                renderLines(section.lines, json, buffer);
                //System.out.println("normal section: "+buffer.position());
                //System.out.println(new String(buffer.array()));
            }
        }
    }

    private String byteKey(String str) {
        return "byte"+str.substring(0, 1).toUpperCase() + str.substring(1);
    } 

    private void renderLines(ArrayList<ScenarioLine> lines, JSONObject json, ByteBuffer buffer) {
        for (ScenarioLine line: lines) {
            if (line.param != null) {
                Object value = null;
                String paramName = line.param;
                boolean isByteParam = false;
                
                if (json.has(paramName)) {
                    value = json.get(paramName);
                } else {
                    String byteParamName = byteKey(paramName);
                    if (json.has(byteParamName)) {
                        value = json.get(byteParamName);
                        isByteParam = true;
                    } else {
                        System.out.println("Parameter "+paramName+" was not found in JSON!\n");
                        System.exit(1);
                    }
                }

                buffer.put(line.prefix.getBytes());

                if (isByteParam) {
                    byte[] decoded = Base64.getDecoder().decode(value.toString());
                    //value = decoded;
                    //value = new String(decoded, StandardCharsets.UTF_8);
                    buffer.put(decoded);
                } else {
                    buffer.put(value.toString().getBytes());
                }

                buffer.put("\n".getBytes());
            } else {
                buffer.put((line.prefix+"\n").getBytes());
            }
        }

        //System.out.println("renderlines: "+buffer.position());
        //System.out.println(new String(buffer.array()));
    }
}
