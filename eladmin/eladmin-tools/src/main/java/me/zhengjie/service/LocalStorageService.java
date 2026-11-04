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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.zhengjie.domain.LocalStorage;
import me.zhengjie.domain.dto.LocalStorageQueryCriteria;
import me.zhengjie.utils.PageResult;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
* @author Zheng Jie
* @date 2019-09-05
*/
public interface LocalStorageService extends IService<LocalStorage> {

    /**
     * Paginated query
     *
     * @param criteria Criteria
     * @param page     Pagination parameters
     * @return /
     */
    PageResult<LocalStorage> queryAll(LocalStorageQueryCriteria criteria, Page<Object> page);

    /**
     * Query all data
     * @param criteria Criteria
     * @return /
     */
    List<LocalStorage> queryAll(LocalStorageQueryCriteria criteria);

    /**
     * Upload
     * @param name File name
     * @param file File
     * @return /
     */
    LocalStorage create(String name, MultipartFile file);

    /**
     * Edit
     * @param resources File information
     */
    void update(LocalStorage resources);

    /**
     * Batch delete
     * @param ids /
     */
    void deleteAll(Long[] ids);

    /**
     * Export data
     * @param localStorages Data to export
     * @param response /
     * @throws IOException /
     */
    void download(List<LocalStorage> localStorages, HttpServletResponse response) throws IOException;
}