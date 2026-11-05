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
package me.zhengjie.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.zhengjie.modules.system.domain.Job;
import me.zhengjie.modules.system.domain.dto.JobQueryCriteria;
import me.zhengjie.utils.PageResult;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
* @author Zheng Jie
* @date 2019-03-29
*/
public interface JobService extends IService<Job> {

    /**
     * Query by ID
     * @param id /
     * @return /
     */
    Job findById(Long id);

    /**
     * Create
     * @param resources /
     */
    void create(Job resources);

    /**
     * Update
     * @param resources /
     */
    void update(Job resources);

    /**
     * Delete
     * @param ids /
     */
    void delete(Set<Long> ids);

    /**
     * Pagination query
     *
     * @param criteria Criteria
     * @param page     Pagination parameters
     * @return /
     */
    PageResult<Job> queryAll(JobQueryCriteria criteria, Page<Object> page);

    /**
     * Query all data
     * @param criteria /
     * @return /
     */
    List<Job> queryAll(JobQueryCriteria criteria);

    /**
     * Export data
     * @param jobs Data to export
     * @param response /
     * @throws IOException /
     */
    void download(List<Job> jobs, HttpServletResponse response) throws IOException;

    /**
     * Verify whether associated with users
     * @param ids /
     */
    void verification(Set<Long> ids);
}