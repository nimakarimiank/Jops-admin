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
import me.zhengjie.modules.security.service.dto.AuthorityDto;
import me.zhengjie.modules.system.domain.Role;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.domain.dto.RoleQueryCriteria;
import me.zhengjie.utils.PageResult;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author Zheng Jie
 * @date 2018-12-03
 */
public interface RoleService extends IService<Role> {

    /**
     * Query all data
     * @return /
     */
    List<Role> queryAll();

    /**
     * Query by ID
     * @param id /
     * @return /
     */
    Role findById(long id);

    /**
     * Create
     * @param resources /
     */
    void create(Role resources);

    /**
     * Update
     * @param resources /
     */
    void update(Role resources);

    /**
     * Delete
     * @param ids /
     */
    void delete(Set<Long> ids);

    /**
     * Query by user ID
     * @param userId User ID
     * @return /
     */
    List<Role> findByUsersId(Long userId);

    /**
     * Query role level by roles
     * @param roles /
     * @return /
     */
    Integer findByRoles(Set<Role> roles);

    /**
     * Update bound menus
     * @param resources /
     */
    void updateMenu(Role resources);

    /**
     * Pagination query with criteria
     *
     * @param criteria Criteria
     * @param page     Pagination parameters
     * @return /
     */
    PageResult<Role> queryAll(RoleQueryCriteria criteria, Page<Object> page);

    /**
     * Query all
     * @param criteria Criteria
     * @return /
     */
    List<Role> queryAll(RoleQueryCriteria criteria);

    /**
     * Export data
     * @param roles Data to export
     * @param response /
     * @throws IOException /
     */
    void download(List<Role> roles, HttpServletResponse response) throws IOException;

    /**
     * Get user permission information
     * @param user User information
     * @return Permission information
     */
    List<AuthorityDto> buildPermissions(User user);

    /**
     * Verify whether associated with users
     * @param ids /
     */
    void verification(Set<Long> ids);

    /**
     * Query by menu ID
     * @param menuId /
     * @return /
     */
    List<Role> findByMenuId(Long menuId);
}
