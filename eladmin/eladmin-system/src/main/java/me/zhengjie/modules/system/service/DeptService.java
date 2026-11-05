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

import com.baomidou.mybatisplus.extension.service.IService;
import me.zhengjie.modules.system.domain.Dept;
import me.zhengjie.modules.system.domain.dto.DeptQueryCriteria;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
* @author Zheng Jie
* @date 2019-03-25
*/
public interface DeptService extends IService<Dept> {

    /**
     * Query all data
     * @param criteria Criteria
     * @param isQuery /
     * @throws Exception /
     * @return /
     */
    List<Dept> queryAll(DeptQueryCriteria criteria, Boolean isQuery) throws Exception;

    /**
     * Query by ID
     * @param id /
     * @return /
     */
    Dept findById(Long id);

    /**
     * Create
     * @param resources /
     */
    void create(Dept resources);

    /**
     * Update
     * @param resources /
     */
    void update(Dept resources);

    /**
     * Delete
     * @param depts /
     *
     */
    void delete(Set<Dept> depts);

    /**
     * Query by PID
     * @param pid /
     * @return /
     */
    List<Dept> findByPid(long pid);

    /**
     * Query by role ID
     * @param id /
     * @return /
     */
    Set<Dept> findByRoleId(Long id);

    /**
     * Export data
     * @param depts Data to export
     * @param response /
     * @throws IOException /
     */
    void download(List<Dept> depts, HttpServletResponse response) throws IOException;

    /**
     * Get departments to delete
     * @param deptList /
     * @param depts /
     * @return /
     */
    Set<Dept> getDeleteDepts(List<Dept> deptList, Set<Dept> depts);

    /**
     * Get sibling and parent data by ID
     * @param dept /
     * @param depts /
     * @return /
     */
    List<Dept> getSuperior(Dept dept, List<Dept> depts);

    /**
     * Build tree data
     * @param depts /
     * @return /
     */
    Object buildTree(List<Dept> depts);

    /**
     * Get
     * @param deptList /
     * @return /
     */
    List<Long> getDeptChildren(List<Dept> deptList);

    /**
     * Verify whether associated with roles or users
     * @param depts /
     */
    void verification(Set<Dept> depts);
}