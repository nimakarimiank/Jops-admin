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
import me.zhengjie.modules.maint.domain.Server;
import me.zhengjie.modules.maint.domain.dto.ServerQueryCriteria;
import me.zhengjie.utils.PageResult;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
* @author zhanghouying
* @date 2019-08-24
*/
public interface ServerService extends IService<Server> {

    /**
     * Pagination query
     *
     * @param criteria Criteria
     * @param page     Pagination parameters
     * @return /
     */
    PageResult<Server> queryAll(ServerQueryCriteria criteria, Page<Object> page);

    /**
     * Query all data
     * @param criteria Criteria
     * @return /
     */
    List<Server> queryAll(ServerQueryCriteria criteria);

    /**
     * Create
     * @param resources /
     */
    void create(Server resources);

    /**
     * Edit
     * @param resources /
     */
    void update(Server resources);

    /**
     * Delete
     * @param ids /
     */
    void delete(Set<Long> ids);

    /**
     * Query by IP
     *
     * @param ip /
     * @return /
     */
    Server findByIp(String ip);

    /**
     * Test connection
     * @param resources /
     * @return /
     */
    Boolean testConnect(Server resources);

    /**
     * Export data
     * @param queryAll /
     * @param response /
     * @throws IOException /
     */
    void download(List<Server> queryAll, HttpServletResponse response) throws IOException;
}
