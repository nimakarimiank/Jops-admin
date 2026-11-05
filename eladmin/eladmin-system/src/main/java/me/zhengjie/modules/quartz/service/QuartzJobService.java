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
package me.zhengjie.modules.quartz.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.zhengjie.modules.quartz.domain.QuartzJob;
import me.zhengjie.modules.quartz.domain.QuartzLog;
import me.zhengjie.modules.quartz.domain.dto.QuartzJobQueryCriteria;
import me.zhengjie.utils.PageResult;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author Zheng Jie
 * @date 2019-01-07
 */
public interface QuartzJobService extends IService<QuartzJob> {

    /**
     * Pagination query
     *
     * @param criteria Criteria
     * @param page     Pagination parameters
     * @return /
     */
    PageResult<QuartzJob> queryAll(QuartzJobQueryCriteria criteria, Page<Object> page);

    /**
     * Query all
     * @param criteria Criteria
     * @return /
     */
    List<QuartzJob> queryAll(QuartzJobQueryCriteria criteria);

    /**
     * Pagination query logs
     *
     * @param criteria Criteria
     * @param page     Pagination parameters
     * @return /
     */
    PageResult<QuartzLog> queryAllLog(QuartzJobQueryCriteria criteria, Page<Object> page);

    /**
     * Query all
     * @param criteria Criteria
     * @return /
     */
    List<QuartzLog> queryAllLog(QuartzJobQueryCriteria criteria);

    /**
     * Create
     * @param resources /
     */
    void create(QuartzJob resources);

    /**
     * Edit
     * @param resources /
     */
    void update(QuartzJob resources);

    /**
     * Delete task
     * @param ids /
     */
    void delete(Set<Long> ids);

    /**
     * Update scheduled task status
     * @param quartzJob /
     */
    void updateIsPause(QuartzJob quartzJob);

    /**
     * Execute scheduled task immediately
     * @param quartzJob /
     */
    void execution(QuartzJob quartzJob);

    /**
     * Export scheduled tasks
     * @param queryAll Data to export
     * @param response /
     * @throws IOException /
     */
    void download(List<QuartzJob> queryAll, HttpServletResponse response) throws IOException;

    /**
     * Export scheduled task logs
     * @param queryAllLog Data to export
     * @param response /
     * @throws IOException /
     */
    void downloadLog(List<QuartzLog> queryAllLog, HttpServletResponse response) throws IOException;

    /**
     * Execute subtasks
     * @param tasks /
     * @throws InterruptedException /
     */
    void executionSubJob(String[] tasks) throws InterruptedException;
}
