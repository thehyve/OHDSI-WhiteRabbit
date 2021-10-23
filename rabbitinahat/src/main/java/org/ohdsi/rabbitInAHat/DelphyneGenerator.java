package org.ohdsi.rabbitInAHat;

import org.apache.commons.lang.StringUtils;
import org.ohdsi.rabbitInAHat.dataModel.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created 04-01-17.
 * Generates separate delphyne template python files for each table to table mapping in the given directory.
 */
public class DelphyneGenerator {
    ETL etl;
    File outputDirectory;
    Writer out;

    public DelphyneGenerator(ETL etl, String directory) {
        this.etl = etl;
        this.outputDirectory = new File(directory);
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdir()) {
                throw new RuntimeException("Python output directory " + directory + " could not be created.");
            }
        }
    }

    public void generate() {
        // Generate a sql file for each source to target table mapping
        for (ItemToItemMap tableToTableMap : etl.getTableToTableMapping().getSourceToTargetMaps()) {
            writeSqlFile(tableToTableMap);
        }
    }

    private void writeSqlFile(ItemToItemMap tableToTableMap){
        Table sourceTable = (Table) tableToTableMap.getSourceItem();
        Table targetTable = (Table) tableToTableMap.getTargetItem();
        Mapping<Field> fieldToFieldMapping = etl.getFieldToFieldMapping(sourceTable, targetTable);
        List<ItemToItemMap> mappings = fieldToFieldMapping.getSourceToTargetMapsOrderedByCdmItems();

        // Create new sql file in the selected directory
        File outFile = new File( outputDirectory,
                sourceTable.getName().replace(".csv", "") + "_to_" + targetTable.getName() + ".py");

        try (FileOutputStream fout = new FileOutputStream(outFile);
             OutputStreamWriter fwr = new OutputStreamWriter(fout, StandardCharsets.UTF_8);
             Writer out = new BufferedWriter(fwr)) {
            this.out = out;

            // Boilerplate
            this.write(0,"from __future__ import annotations\n\n" +
                    "from typing import List, TYPE_CHECKING\n\n" +
                    "if TYPE_CHECKING:\n" +
                    "    from src.main.python.wrapper import Wrapper\n\n\n");

            String targetTableClassName = this.toClassName(targetTable.getName());
            this.write(0,"def " +
                    sourceTable.getName().replace(".csv", "") + "_to_" + targetTable.getName() +
                    "(wrapper: Wrapper) -> List[Wrapper.cdm." + targetTableClassName + "]:", true);

            // Table specific comments
            this.write(1, createBlockComment(sourceTable.getComment()), true);
            this.write(1, createBlockComment(targetTable.getComment()), true);
            this.write(1, createBlockComment(tableToTableMap.getComment()), true);
            this.write(1, createBlockComment(tableToTableMap.getLogic()), true);

            // Source file
            this.write(1, "source = wrapper.source_data.get_source_file('" +
                    sourceTable.getName().replace(".csv", ".tsv") + "')", true);
            this.write(1, "df = source.get_csv_as_df(apply_dtypes=True)", true);

            // Comment
            this.write(1, "for _, row in df.iterrows():", true);
            this.write(2, "yield wrapper.cdm." + targetTableClassName + "(", true);

            int n_mappings = mappings.size();
            Set<String> targetFieldsSeen = new HashSet<>();
            for (int i=0;i<n_mappings;i++) {
                ItemToItemMap mapping = mappings.get(i);
                String targetName;
                if (mapping == null) {
                    Field target = targetTable.getFieldByName(fieldToFieldMapping.getTargetItems().get(targetFieldsSeen.size()).getName());
//                    this.write(3, createComment("[TARGET COMMENT]", target.getComment()), true);
                    targetName = target.getName();
                    this.write(3, targetName);
                    out.write("=");
                    out.write("None");
                } else {
                    Field source = sourceTable.getFieldByName(mapping.getSourceItem().getName());
                    this.write(3, createComment("[VALUE   COMMENT]", source.getComment()), true);
                    this.write(3, createComment("[MAPPING   LOGIC]", mapping.getLogic()), true);
                    this.write(3, createComment("[MAPPING COMMENT]", mapping.getComment()), true);

                    targetName = mapping.getTargetItem().getName();
//                    Field target = targetTable.getFieldByName(targetName);
//                    this.write(3, createComment("[TARGET COMMENT]", target.getComment()), true);

                    this.write(3, targetName);
                    out.write("=");
                    out.write("row['" + source.getName() + "']");
                }

                // Do not print comma if last is reached
                if (i != n_mappings-1) {
                    out.write(",");
                }

                Field target = targetTable.getFieldByName(targetName);
                out.write(createInLineComment(target.getComment()));

                if (mapping == null) {
                    out.write(createInLineComment("[!WARNING!] NO SOURCE COLUMN MAPPED TO THIS TARGET"));
                } else if (targetFieldsSeen.contains(targetName)) {
                    out.write(createInLineComment("[!#WARNING!#] THIS TARGET FIELD WAS ALREADY USED"));
                }

                targetFieldsSeen.add(targetName);

                this.writeNewLine();

                if (i == n_mappings-1) {
                    this.write(2,")");
                    this.writeNewLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String createInLineComment(String input) {
        if (input.trim().equals(""))
            return "";

        return "  # " + input.replaceAll("\\s"," ");
    }

    private static String createComment(String prefix, String input) {
        if (input.trim().equals(""))
            return "";

        return String.format("# %s %s", prefix, input.replaceAll("\\s"," "));
    }

    private static String createBlockComment(String input) {
        if (input.trim().equals(""))
            return "";

        // Let new sentences span multiple lines
        input = input.replaceAll("\\.\\s","\n");

        return String.format("\"\"\"%n%s%n\"\"\"", input.trim());
    }

    private void write(int indentationLevel, String str) throws IOException {
        this.write(indentationLevel, str, false);
    }

    private void write(int indentationLevel, String str, Boolean eol) throws IOException {
        if (str.isEmpty()) {
            return;
        }
        String indentation = StringUtils.repeat(" ", indentationLevel*4);
        str = str.replace("\n", "\n" + indentation);
        this.out.write(indentation + str);
        if (eol) {
            this.writeNewLine();
        }
    }

    private void writeNewLine() throws IOException {
        this.out.write("\n");
    }

    private String toClassName(String str) {
        str = str.replaceFirst("[a-z]", String.valueOf(Character.toUpperCase(str.charAt(0))));
        while(str.contains("_")) {
            str = str.replaceFirst("_[a-z]", String.valueOf(Character.toUpperCase(str.charAt(str.indexOf("_") + 1))));
        }
        return str;
    }
}
