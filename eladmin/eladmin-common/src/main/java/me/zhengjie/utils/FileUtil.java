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

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BadRequestException;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * File utility class, extends Hutool toolkit
 *
 * @author Zheng Jie
 * @date 2018-12-27
 */
@Slf4j
public class FileUtil extends cn.hutool.core.io.FileUtil {

    /**
     * System temporary directory
     * <br>
     * Windows contains path separator, but Linux does not,
     * Under Windows \\==\ premise,
     * For safety, consistently assemble path separator,
     * <pre>
     *       java.io.tmpdir
     *       windows : C:\Users/xxx\AppData\Local\Temp\
     *       linux: /temp
     * </pre>
     */
    public static final String SYS_TEM_DIR = System.getProperty("java.io.tmpdir") + File.separator;
    /**
     * Define calculation constant for GB
     */
    private static final int GB = 1024 * 1024 * 1024;
    /**
     * Define calculation constant for MB
     */
    private static final int MB = 1024 * 1024;
    /**
     * Define calculation constant for KB
     */
    private static final int KB = 1024;

    /**
     * Format decimal
     */
    private static final DecimalFormat DF = new DecimalFormat("0.00");

    public static final String IMAGE = "Image";
    public static final String TXT = "Document";
    public static final String MUSIC = "Music";
    public static final String VIDEO = "Video";
    public static final String OTHER = "Other";


    /**
     * Convert MultipartFile to File
     */
    public static File toFile(MultipartFile multipartFile) {
        // Get file name
        String fileName = multipartFile.getOriginalFilename();
        // Get file suffix
        String prefix = "." + getExtensionName(fileName);
        File file = null;
        try {
            // Use UUID as file name to avoid duplicate temporary files
            file = new File(SYS_TEM_DIR + IdUtil.simpleUUID() + prefix);
            // MultipartFile to File
            multipartFile.transferTo(file);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return file;
    }

    /**
     * Get file extension without dot
     */
    public static String getExtensionName(String filename) {
        if ((filename != null) && (!filename.isEmpty())) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }

    /**
     * Java file operations: get file name without extension
     */
    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (!filename.isEmpty())) {
            int dot = filename.lastIndexOf('.');
            if (dot > -1) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    /**
     * Convert file size format
     */
    public static String getSize(long size) {
        String resultSize;
        if (size / GB >= 1) {
            // If current Byte value is greater than or equal to 1GB
            resultSize = DF.format(size / (float) GB) + "GB";
        } else if (size / MB >= 1) {
            // If current Byte value is greater than or equal to 1MB
            resultSize = DF.format(size / (float) MB) + "MB";
        } else if (size / KB >= 1) {
            // If current Byte value is greater than or equal to 1KB
            resultSize = DF.format(size / (float) KB) + "KB";
        } else {
            resultSize = size + "B";
        }
        return resultSize;
    }

    /**
     * Convert InputStream to File
     */
    static File inputStreamToFile(InputStream ins, String name){
        File file = new File(SYS_TEM_DIR + name);
        if (file.exists()) {
            return file;
        }
        OutputStream os = null;
        try {
            os = Files.newOutputStream(file.toPath());
            int bytesRead;
            int len = 8192;
            byte[] buffer = new byte[len];
            while ((bytesRead = ins.read(buffer, 0, len)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            CloseUtil.close(os);
            CloseUtil.close(ins);
        }
        return file;
    }

    /**
     * Resolve file name into upload file path
     */
    public static File upload(MultipartFile file, String filePath) {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmssS");
        // Filter illegal file name
        String name = getFileNameNoEx(verifyFilename(file.getOriginalFilename()));
        String suffix = getExtensionName(file.getOriginalFilename());
        String nowStr = "-" + format.format(date);
        try {
            String fileName = name + nowStr + "." + suffix;
            String path = filePath + fileName;
            // getCanonicalFile resolves various paths correctly
            File dest = new File(path).getCanonicalFile();
            // Check if directory exists
            if (!dest.getParentFile().exists()) {
                if (!dest.getParentFile().mkdirs()) {
                    System.out.println("was not successful.");
                }
            }
            // Write file
            file.transferTo(dest);
            return dest;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Export Excel
     */
    public static void downloadExcel(List<Map<String, Object>> list, HttpServletResponse response) throws IOException {
        String tempPath = SYS_TEM_DIR + IdUtil.fastSimpleUUID() + ".xlsx";
        File file = new File(tempPath);
        BigExcelWriter writer = ExcelUtil.getBigWriter(file);
        // Sanitize data to prevent CSV injection
        List<Map<String, Object>> sanitizedList = list.parallelStream().map(map -> {
            Map<String, Object> sanitizedMap = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if (value instanceof String) {
                    String strValue = (String) value;
                    // Check and handle values starting with special characters
                    if (strValue.startsWith("=") || strValue.startsWith("+") || strValue.startsWith("-") || strValue.startsWith("@")) {
                        strValue = "'" + strValue; // Add single quote prefix
                    }
                    sanitizedMap.put(key, strValue);
                } else {
                    sanitizedMap.put(key, value);
                }
            });
            return sanitizedMap;
        }).collect(Collectors.toList());
        // Write content at once using default style, force header output
        writer.write(sanitizedList, true);
        SXSSFSheet sheet = (SXSSFSheet)writer.getSheet();
        // Above requires casting to SXSSFSheet, otherwise trackAllColumnsForAutoSizing is unavailable
        sheet.trackAllColumnsForAutoSizing();
        // Auto-size column width
        writer.autoSizeColumnAll();
        // response is HttpServletResponse object
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        // file.xlsx is the download dialog filename
        response.setHeader("Content-Disposition", "attachment;filename=file.xlsx");
        ServletOutputStream out = response.getOutputStream();
        // Delete temporary file on exit
        file.deleteOnExit();
        writer.flush(out, true);
        // Remember to close output Servlet stream
        IoUtil.close(out);
    }

    public static String getFileType(String type) {
        String documents = "txt doc pdf ppt pps xlsx xls docx";
        String music = "mp3 wav wma mpa ram ra aac aif m4a";
        String video = "avi mpg mpe mpeg asf wmv mov qt rm mp4 flv m4v webm ogv ogg";
        String image = "bmp dib pcp dif wmf gif jpg tif eps psd cdr iff tga pcd mpt png jpeg";
        if (image.contains(type)) {
            return IMAGE;
        } else if (documents.contains(type)) {
            return TXT;
        } else if (music.contains(type)) {
            return MUSIC;
        } else if (video.contains(type)) {
            return VIDEO;
        } else {
            return OTHER;
        }
    }

    public static void checkSize(long maxSize, long size) {
        // 1M
        int len = 1024 * 1024;
        if (size > (maxSize * len)) {
            throw new BadRequestException("File size exceeds allowed limit: " + maxSize + "MB");
        }
    }

    /**
     * Check if two files are identical
     */
    public static boolean check(File file1, File file2) {
        String img1Md5 = getMd5(file1);
        String img2Md5 = getMd5(file2);
        if(img1Md5 != null){
            return img1Md5.equals(img2Md5);
        }
        return false;
    }

    /**
     * Check if two files are identical
     */
    public static boolean check(String file1Md5, String file2Md5) {
        return file1Md5.equals(file2Md5);
    }

    private static byte[] getByte(File file) {
        // Get file length
        byte[] b = new byte[(int) file.length()];
        InputStream in = null;
        try {
            in = Files.newInputStream(file.toPath());
            try {
                System.out.println(in.read(b));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        } finally {
            CloseUtil.close(in);
        }
        return b;
    }

    private static String getMd5(byte[] bytes) {
        // Hex characters
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(bytes);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            // Shift and output string
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Download file
     *
     * @param request  /
     * @param response /
     * @param file     /
     */
    public static void downloadFile(HttpServletRequest request, HttpServletResponse response, File file, boolean deleteOnExit) {
        response.setCharacterEncoding(request.getCharacterEncoding());
        response.setContentType("application/octet-stream");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
            IOUtils.copy(fis, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                    if (deleteOnExit) {
                        file.deleteOnExit();
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Validate and filter illegal file names
     * @param fileName File name
     * @return File name
     */
    public static String verifyFilename(String fileName) {
        // Filter out special characters
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|~\\s]", "");

        // Trim leading and trailing spaces and dots
        fileName = fileName.trim().replaceAll("^[. ]+|[. ]+$", "");

        // Disallow file names exceeding 255 (Mac/Linux) or 260 (Windows) characters
        int maxFileNameLength = 255;
        if (System.getProperty("os.name").startsWith("Windows")) {
            maxFileNameLength = 260;
        }
        if (fileName.length() > maxFileNameLength) {
            fileName = fileName.substring(0, maxFileNameLength);
        }

        // Filter out control characters
        fileName = fileName.replaceAll("[\\p{Cntrl}]", "");

        // Filter out ".." path traversal
        fileName = fileName.replaceAll("\\.{2,}", "");

        // Remove leading ".." from file name
        fileName = fileName.replaceAll("^\\.+/", "");

        // Retain only the last "." in file name, remove other dots
        fileName = fileName.replaceAll("^(.*)(\\.[^.]*)$", "$1").replaceAll("\\.", "") +
                fileName.replaceAll("^(.*)(\\.[^.]*)$", "$2");

        return fileName;
    }

    public static String getMd5(File file) {
        return getMd5(getByte(file));
    }
}
