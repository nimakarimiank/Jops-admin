/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.zhengjie.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.*;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.domain.GenConfig;
import me.zhengjie.domain.ColumnInfo;
import org.springframework.util.ObjectUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.util.*;
import static me.zhengjie.utils.FileUtil.SYS_TEM_DIR;

/**
 * Code generation
 *
 * @author Zheng Jie
 * @date 2019-01-02
 */
@Slf4j
@SuppressWarnings({"unchecked", "all"})
public class GenUtil {

    private static final String TIMESTAMP = "Timestamp";

    private static final String BIGDECIMAL = "BigDecimal";

    public static final String PK = "PRI";

    public static final String EXTRA = "auto_increment";

    /**
     * Get backend code template names
     *
     * @return List
     */
    private static List<String> getAdminTemplateNames() {
        List<String> templateNames = new ArrayList<>();
        templateNames.add("Entity");
        templateNames.add("Controller");
        templateNames.add("QueryCriteria");
        templateNames.add("Service");
        templateNames.add("ServiceImpl");
        templateNames.add("Mapper");
        templateNames.add("Mapper-xml");
        return templateNames;
    }

    /**
     * Get frontend code template names
     *
     * @return List
     */
    private static List<String> getFrontTemplateNames() {
        List<String> templateNames = new ArrayList<>();
        templateNames.add("index");
        templateNames.add("api");
        return templateNames;
    }

    public static List<Map<String, Object>> preview(List<ColumnInfo> columns, GenConfig genConfig) {
        Map<String, Object> genMap = getGenMap(columns, genConfig);
        List<Map<String, Object>> genList = new ArrayList<>();
        // Get backend templates
        List<String> templates = getAdminTemplateNames();
        TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("template", TemplateConfig.ResourceMode.CLASSPATH));
        for (String templateName : templates) {
            Map<String, Object> map = new HashMap<>(1);
            Template template = engine.getTemplate("admin/" + templateName + ".ftl");
            map.put("content", template.render(genMap));
            map.put("name", templateName.replace("-xml", ".xml"));
            genList.add(map);
        }
        // Get frontend templates
        templates = getFrontTemplateNames();
        for (String templateName : templates) {
            Map<String, Object> map = new HashMap<>(1);
            Template template = engine.getTemplate("front/" + templateName + ".ftl");
            map.put(templateName, template.render(genMap));
            map.put("content", template.render(genMap));
            map.put("name", templateName);
            genList.add(map);
        }
        return genList;
    }

    public static String download(List<ColumnInfo> columns, GenConfig genConfig) throws IOException {
        // Concatenated path: /tmpeladmin-gen-temp/, which requires root permission on Linux; changed to: /tmp/eladmin-gen-temp/
        // String tempPath =SYS_TEM_DIR + "eladmin-gen-temp" + File.separator + genConfig.getTableName() + File.separator;
        String tempPath = SYS_TEM_DIR + "eladmin-gen-temp" + File.separator + genConfig.getTableName() + File.separator;
        Map<String, Object> genMap = getGenMap(columns, genConfig);
        TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("template", TemplateConfig.ResourceMode.CLASSPATH));
        // Generate backend code
        List<String> templates = getAdminTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("admin/" + templateName + ".ftl");
            String filePath = getAdminFilePath(templateName, genConfig, genMap.get("className").toString(), tempPath + "eladmin" + File.separator);
            assert filePath != null;
            File file = new File(filePath);
            // If non-overwrite generation
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // Generate code
            genFile(file, template, genMap);
        }
        // Generate frontend code
        templates = getFrontTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("front/" + templateName + ".ftl");
            String path = tempPath + "eladmin-web" + File.separator;
            String apiPath = path + "src" + File.separator + "api" + File.separator;
            String srcPath = path + "src" + File.separator + "views" + File.separator + genMap.get("changeClassName").toString() + File.separator;
            String filePath = getFrontFilePath(templateName, apiPath, srcPath, genMap.get("changeClassName").toString());
            assert filePath != null;
            File file = new File(filePath);
            // If non-overwrite generation
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // Generate code
            genFile(file, template, genMap);
        }
        return tempPath;
    }

    public static void generatorCode(List<ColumnInfo> columnInfos, GenConfig genConfig) throws IOException {
        Map<String, Object> genMap = getGenMap(columnInfos, genConfig);
        TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("template", TemplateConfig.ResourceMode.CLASSPATH));
        // Generate backend code
        List<String> templates = getAdminTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("admin/" + templateName + ".ftl");
            String rootPath = System.getProperty("user.dir");
            String filePath = getAdminFilePath(templateName, genConfig, genMap.get("className").toString(), rootPath);

            assert filePath != null;
            File file = new File(filePath);

            // If non-overwrite generation
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // Generate code
            genFile(file, template, genMap);
        }

        // Generate frontend code
        templates = getFrontTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("front/" + templateName + ".ftl");
            String filePath = getFrontFilePath(templateName, genConfig.getApiPath(), genConfig.getPath(), genMap.get("changeClassName").toString());

            assert filePath != null;
            File file = new File(filePath);

            // If non-overwrite generation
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // Generate code
            genFile(file, template, genMap);
        }
    }

    // Get template data
    private static Map<String, Object> getGenMap(List<ColumnInfo> columnInfos, GenConfig genConfig) {
        // Store template field data
        Map<String, Object> genMap = new HashMap<>(16);
        // API alias
        genMap.put("apiAlias", genConfig.getApiAlias());
        // Package name
        genMap.put("package", genConfig.getPack());
        // Module name
        genMap.put("moduleName", genConfig.getModuleName());
        // Author
        genMap.put("author", genConfig.getAuthor());
        // Creation date
        genMap.put("date", LocalDate.now().toString());
        // Table name
        genMap.put("tableName", genConfig.getTableName());
        // PascalCase class name
        String className = StringUtils.toCapitalizeCamelCase(genConfig.getTableName());
        // camelCase class name
        String changeClassName = StringUtils.toCamelCase(genConfig.getTableName());
        // Check whether to remove table prefix
        if (StringUtils.isNotEmpty(genConfig.getPrefix())) {
            className = StringUtils.toCapitalizeCamelCase(StrUtil.removePrefix(genConfig.getTableName(), genConfig.getPrefix()));
            changeClassName = StringUtils.toCamelCase(StrUtil.removePrefix(genConfig.getTableName(), genConfig.getPrefix()));
            changeClassName = StringUtils.uncapitalize(changeClassName);
        }
        // Save class name
        genMap.put("className", className);
        // Save camelCase class name
        genMap.put("changeClassName", changeClassName);
        // Whether Timestamp field exists
        genMap.put("hasTimestamp", false);
        // Whether Timestamp field exists in query class
        genMap.put("queryHasTimestamp", false);
        // Whether BigDecimal field exists
        genMap.put("hasBigDecimal", false);
        // Whether BigDecimal field exists in query class
        genMap.put("queryHasBigDecimal", false);
        // Whether query needs to be created
        genMap.put("hasQuery", false);
        // Auto-increment primary key
        genMap.put("auto", false);
        // Whether dictionary exists
        genMap.put("hasDict", false);
        // Whether date annotation exists
        genMap.put("hasDateAnnotation", false);
        // Store primary key field name
        genMap.put("pkIdName", "none");
        // Store symbol
        genMap.put("symbol", "#");
        // Save column information
        List<Map<String, Object>> columns = new ArrayList<>();
        // Save query column information
        List<Map<String, Object>> queryColumns = new ArrayList<>();
        // Store dictionary information
        List<String> dicts = new ArrayList<>();
        // Store between information
        List<Map<String, Object>> betweens = new ArrayList<>();
        // Store non-null column information
        List<Map<String, Object>> isNotNullColumns = new ArrayList<>();

        for (ColumnInfo column : columnInfos) {
            Map<String, Object> listMap = new HashMap<>(16);
            // Column description
            listMap.put("remark", column.getRemark());
            // Column type
            listMap.put("columnKey", column.getKeyType());
            // Primary key type
            String colType = ColUtil.cloToJava(column.getColumnType());
            // camelCase column name
            String changeColumnName = StringUtils.toCamelCase(column.getColumnName());
            // PascalCase column name
            String capitalColumnName = StringUtils.toCapitalizeCamelCase(column.getColumnName());
            if (PK.equals(column.getKeyType())) {
                // Store primary key type
                genMap.put("pkColumnType", colType);
                // Store camelCase field name
                genMap.put("pkChangeColName", changeColumnName);
                // Store PascalCase field name
                genMap.put("pkCapitalColName", capitalColumnName);
                // Store primary key field name
                genMap.put("pkIdName", column.getColumnName());
            }
            // Check if Timestamp type column exists
            if (TIMESTAMP.equals(colType)) {
                genMap.put("hasTimestamp", true);
            }
            // Check if BigDecimal type column exists
            if (BIGDECIMAL.equals(colType)) {
                genMap.put("hasBigDecimal", true);
            }
            // Whether primary key is auto-increment
            if (EXTRA.equals(column.getExtra())) {
                genMap.put("auto", true);
            }
            // Primary key has dictionary
            if (StringUtils.isNotBlank(column.getDictName())) {
                genMap.put("hasDict", true);
                if(!dicts.contains(column.getDictName()))
                    dicts.add(column.getDictName());
            }

            // Store column type
            listMap.put("columnType", colType);
            // Store raw column name
            listMap.put("columnName", column.getColumnName());
            // Not null
            listMap.put("istNotNull", column.getNotNull());
            // Display column in list
            listMap.put("columnShow", column.getListShow());
            // Display in form
            listMap.put("formShow", column.getFormShow());
            // Form component type
            listMap.put("formType", StringUtils.isNotBlank(column.getFormType()) ? column.getFormType() : "Input");
            // camelCase column name
            listMap.put("changeColumnName", changeColumnName);
            // PascalCase column name
            listMap.put("capitalColumnName", capitalColumnName);
            // Dictionary name
            listMap.put("dictName", column.getDictName());
            // Add non-null column info
            if (column.getNotNull()) {
                isNotNullColumns.add(listMap);
            }
            // Check if query exists; if so, set query column into columnQuery
            if (!StringUtils.isBlank(column.getQueryType())) {
                // Query type
                listMap.put("queryType", column.getQueryType());
                // Whether query exists
                genMap.put("hasQuery", true);
                if (TIMESTAMP.equals(colType)) {
                    // Store Timestamp type in query
                    genMap.put("queryHasTimestamp", true);
                }
                if (BIGDECIMAL.equals(colType)) {
                    // Store BigDecimal type in query
                    genMap.put("queryHasBigDecimal", true);
                }
                if ("between".equalsIgnoreCase(column.getQueryType())) {
                    betweens.add(listMap);
                } else {
                    // Add to query list
                    queryColumns.add(listMap);
                }
            }
            // Add to column list
            columns.add(listMap);
        }
        // Save column list
        genMap.put("columns", columns);
        // Save query list
        genMap.put("queryColumns", queryColumns);
        // Save dictionary list
        genMap.put("dicts", dicts);
        // Save between list
        genMap.put("betweens", betweens);
        // Save non-null column list
        genMap.put("isNotNullColumns", isNotNullColumns);
        return genMap;
    }

    /**
     * Define backend file path and name
     */
    private static String getAdminFilePath(String templateName, GenConfig genConfig, String className, String rootPath) {
        String projectPath = rootPath + File.separator + genConfig.getModuleName();
        String packagePath = projectPath + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;
        String mpXmlPath = projectPath + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator;
        if (!ObjectUtils.isEmpty(genConfig.getPack())) {
            packagePath += genConfig.getPack().replace(".", File.separator) + File.separator;
        }

        if ("Entity".equals(templateName)) {
            return packagePath + "domain" + File.separator + className + ".java";
        }

        if ("Controller".equals(templateName)) {
            return packagePath + "rest" + File.separator + className + "Controller.java";
        }

        if ("Service".equals(templateName)) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if ("ServiceImpl".equals(templateName)) {
            return packagePath + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }

        if ("QueryCriteria".equals(templateName)) {
            return packagePath + "domain" + File.separator + "dto" + File.separator + className + "QueryCriteria.java";
        }

        if ("Mapper".equals(templateName)) {
            return packagePath + "mapper" + File.separator + className + "Mapper.java";
        }

        if ("Mapper-xml".equals(templateName)) {
            return mpXmlPath + "mapper" + File.separator + className + "Mapper.xml";
        }

        return null;
    }

    /**
     * Define frontend file path and name
     */
    private static String getFrontFilePath(String templateName, String apiPath, String path, String apiName) {

        if ("api".equals(templateName)) {
            return apiPath + File.separator + apiName + ".js";
        }

        if ("index".equals(templateName)) {
            return path + File.separator + "index.vue";
        }

        return null;
    }

    private static void genFile(File file, Template template, Map<String, Object> map) throws IOException {
        // Generate target file
        Writer writer = null;
        try {
            FileUtil.touch(file);
            writer = new FileWriter(file);
            template.render(map, writer);
        } catch (TemplateException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            assert writer != null;
            writer.close();
        }
    }
}
