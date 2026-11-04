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
package me.zhengjie.service;

import me.zhengjie.domain.S3Storage;
import me.zhengjie.domain.dto.S3StorageQueryCriteria;
import java.util.Map;
import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.zhengjie.utils.PageResult;
import org.springframework.web.multipart.MultipartFile;

/**
* @description Service interface
* @author Zheng Jie
* @date 2025-06-19
**/
public interface S3StorageService extends IService<S3Storage> {

    /**
    * Paginated query
    * @param criteria Criteria
    * @param page Pagination parameters
    * @return PageResult
    */
    PageResult<S3Storage> queryAll(S3StorageQueryCriteria criteria, Page<Object> page);

    /**
    * Query all data unpaged
    * @param criteria Criteria parameters
    * @return List<S3StorageDto>
    */
    List<S3Storage> queryAll(S3StorageQueryCriteria criteria);

    /**
    * Batch delete
    * @param ids /
    */
    void deleteAll(List<Long> ids);

    /**
    * Export data
    * @param all Data to export
    * @param response /
    * @throws IOException /
    */
    void download(List<S3Storage> all, HttpServletResponse response) throws IOException;

    /**
     * Private download (for reference only; multiple methods exist)
     * @param id File ID
     */
    Map<String, String> privateDownload(Long id);

    /**
     * Upload file
     * @param file File to upload
     * @return S3Storage object containing file storage information
     */
    S3Storage upload(MultipartFile file);
}