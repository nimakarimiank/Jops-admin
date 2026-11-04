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
package me.zhengjie.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.config.AmzS3Config;
import me.zhengjie.domain.S3Storage;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.service.S3StorageService;
import me.zhengjie.domain.dto.S3StorageQueryCriteria;
import me.zhengjie.mapper.S3StorageMapper;
import me.zhengjie.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;
import java.util.*;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import me.zhengjie.utils.PageResult;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

/**
* @description Service implementation
* @author Zheng Jie
* @date 2025-06-19
**/
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl extends ServiceImpl<S3StorageMapper, S3Storage> implements S3StorageService {

    private final S3Client s3Client;
    private final AmzS3Config amzS3Config;
    private final S3StorageMapper s3StorageMapper;

    @Override
    public PageResult<S3Storage> queryAll(S3StorageQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(s3StorageMapper.findAll(criteria, page));
    }

    @Override
    public List<S3Storage> queryAll(S3StorageQueryCriteria criteria){
        return s3StorageMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        // Check if bucket exists
        String bucketName = amzS3Config.getDefaultBucket();
        if (!bucketExists(bucketName)) {
            throw new BadRequestException("Bucket does not exist. Please check configuration or permissions.");
        }
        // Iterate through ID list, delete corresponding files and database records
        for (Long id : ids) {
            String filePath = s3StorageMapper.selectFilePathById(id);
            if (filePath == null) {
                System.err.println("File record not found for ID " + id + ", cannot delete.");
                continue;
            }
            try {
                // Create DeleteObjectRequest, specifying bucket and file key
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(filePath)
                        .build();
                // Call deleteObject method
                s3Client.deleteObject(deleteObjectRequest);
                // Delete database data
                s3StorageMapper.deleteById(id);
            } catch (S3Exception e) {
                // Handle AWS specific exceptions
                log.error("Error deleting file from S3: {}", e.awsErrorDetails().errorMessage(), e);
            }
        }
    }

    @Override
    public S3Storage upload(MultipartFile file) {
        String bucketName = amzS3Config.getDefaultBucket();
        // Check if bucket exists
        if (!bucketExists(bucketName)) {
            log.warn("Bucket {} does not exist, attempting to create...", bucketName);
            if (createBucket(bucketName)){
                log.info("Bucket {} created successfully.", bucketName);
            } else {
                throw new BadRequestException("Failed to create bucket. Please check configuration or permissions.");
            }
        }
        // Get file name
        String originalName = file.getOriginalFilename();
        if (StringUtils.isBlank(originalName)) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        // Generate storage path and file name
        String folder = DateUtil.format(new Date(), amzS3Config.getTimeformat());
        String fileName = IdUtil.simpleUUID() + "." + FileUtil.getExtensionName(originalName);
        String filePath = folder + "/" + fileName;
        // Build upload request
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(amzS3Config.getDefaultBucket())
                .key(filePath)
                .build();
        // Create S3Storage instance
        S3Storage s3Storage = new S3Storage();
        try {
            // Upload file to S3
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            // Set S3Storage properties
            s3Storage.setFileMimeType(FileUtil.getMimeType(originalName));
            s3Storage.setFileName(originalName);
            s3Storage.setFileRealName(fileName);
            s3Storage.setFileSize(FileUtil.getSize(file.getSize()));
            s3Storage.setFileType(FileUtil.getExtensionName(originalName));
            s3Storage.setFilePath(filePath);
            // Save to database
            s3StorageMapper.insert(s3Storage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Set URL
        return s3Storage;
    }

    @Override
    public void download(List<S3Storage> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (S3Storage s3Storage : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("File Name", s3Storage.getFileName());
            map.put("Storage File Name", s3Storage.getFileRealName());
            map.put("File Size", s3Storage.getFileSize());
            map.put("File MIME Type", s3Storage.getFileMimeType());
            map.put("File Type", s3Storage.getFileType());
            map.put("File Path", s3Storage.getFilePath());
            map.put("Created By", s3Storage.getCreateBy());
            map.put("Updated By", s3Storage.getUpdateBy());
            map.put("Creation Date", s3Storage.getCreateTime());
            map.put("Update Time", s3Storage.getUpdateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    public Map<String, String> privateDownload(Long id) {
        S3Storage storage = s3StorageMapper.selectById(id);
        if (storage == null) {
            throw new BadRequestException("File does not exist or has been deleted");
        }
        // Create GetObjectRequest, specifying bucket and file key
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(amzS3Config.getDefaultBucket())
                .key(storage.getFilePath())
                .build();
        String base64Data;
        // Use try-with-resources to ensure stream is automatically closed
        // s3Client.getObject() returns a ResponseInputStream, which is an input stream containing S3 object data
        try (ResponseInputStream<GetObjectResponse> s3InputStream = s3Client.getObject(getObjectRequest)) {
            // Use IOUtils.toByteArray to directly convert input stream to byte array
            byte[] fileBytes = IOUtils.toByteArray(s3InputStream);
            // Use Java built-in Base64 encoder to convert byte array to Base64 string
            base64Data = Base64.getEncoder().encodeToString(fileBytes);
        } catch (S3Exception e) {
            // Handle AWS specific exceptions
            throw new BadRequestException("Error downloading file from S3: " + e.awsErrorDetails().errorMessage());
        } catch (IOException e) {
            // Handle general IO exceptions (IOUtils.toByteArray may throw)
            throw new BadRequestException("Error reading S3 input stream: " + e.getMessage());
        }
        // Construct return data
        Map<String, String> responseData = new HashMap<>();
        // File name
        responseData.put("fileName", storage.getFileName());
        // File type
        responseData.put("fileMimeType", storage.getFileMimeType());
        // File content
        responseData.put("base64Data", base64Data);
        return responseData;
    }

    /**
     * Check if cloud storage bucket exists
     * @param bucketName Bucket name
     */
    @SuppressWarnings({"all"})
    private boolean bucketExists(String bucketName) {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.headBucket(headBucketRequest);
            return true;
        } catch (S3Exception e) {
            // If status code is 404 (Not Found), bucket does not exist
            if (e.statusCode() == 404) {
                log.error("Bucket '{}' does not exist.", bucketName);
                return false;
            }
            // Other exceptions (such as 403 Forbidden) indicate issues, but cannot determine it does not exist
            throw new BadRequestException("Error checking bucket: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Create cloud storage bucket
     * @param bucketName Bucket name
     */
    private boolean createBucket(String bucketName) {
        try {
            // Use S3Waiter to wait until bucket creation completes
            S3Waiter s3Waiter = s3Client.waiter();
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .acl(BucketCannedACL.PRIVATE)
                    .build();
            s3Client.createBucket(bucketRequest);
            // Wait until bucket creation completes
            HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            // Use WaiterResponse to wait until bucket exists
            WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
            waiterResponse.matched().response().ifPresent(response ->
                    log.info("Bucket '{}' created successfully, status: {}", bucketName, response.sdkHttpResponse().statusCode())
            );
        } catch (BucketAlreadyOwnedByYouException e) {
            log.warn("Bucket '{}' is already owned by you, no need to recreate.", bucketName);
        } catch (S3Exception e) {
            throw new BadRequestException("Error creating bucket: " + e.awsErrorDetails().errorMessage());
        }
        return true;
    }
}