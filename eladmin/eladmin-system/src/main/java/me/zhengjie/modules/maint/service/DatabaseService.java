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
package me.zhengjie.modules.maint.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.zhengjie.modules.maint.domain.Database;
import me.zhengjie.modules.maint.domain.dto.DatabaseQueryCriteria;
import me.zhengjie.utils.PageResult;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author ZhangHouYing
 * @date 2019-08-24
 */
public interface DatabaseService extends IService<Database> {

    /**
     * Pagination query
     *
     * @param criteria Criteria
     * @param page     Pagination parameters
     * @return /
     */
    PageResult<Database> queryAll(DatabaseQueryCriteria criteria, Page<Object> page);

    /**
     * Query all
     * @param criteria Criteria
     * @return /
     */
    List<Database> queryAll(DatabaseQueryCriteria criteria);

    /**
     * Create
     * @param resources /
     */
    void create(Database resources);

    /**
     * Edit
     * @param resources /
     */
    void update(Database resources);

    /**
     * Delete
     * @param ids /
     */
    void delete(Set<String> ids);

    /**
     * Test connection
     * @param resources /
     * @return /
     */
    boolean testConnection(Database resources);

    /**
     * Export data
     * @param queryAll /
     * @param response /
     * @throws IOException e
     */
    void download(List<Database> queryAll, HttpServletResponse response) throws IOException;
}
