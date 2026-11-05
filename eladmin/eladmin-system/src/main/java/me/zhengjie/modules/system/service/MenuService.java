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
import me.zhengjie.modules.system.domain.Menu;
import me.zhengjie.modules.system.domain.dto.MenuQueryCriteria;
import me.zhengjie.modules.system.domain.dto.MenuVo;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author Zheng Jie
 * @date 2018-12-17
 */
public interface MenuService extends IService<Menu> {

    /**
     * Query all data
     * @param criteria Criteria
     * @param isQuery /
     * @throws Exception /
     * @return /
     */
    List<Menu> queryAll(MenuQueryCriteria criteria, Boolean isQuery) throws Exception;

    /**
     * Query by ID
     * @param id /
     * @return /
     */
    Menu findById(long id);

    /**
     * Create
     * @param resources /
     */
    void create(Menu resources);

    /**
     * Update
     * @param resources /
     */
    void update(Menu resources);

    /**
     * Get all child nodes, including self ID
     * @param menuList /
     * @param menuSet /
     * @return /
     */
    Set<Menu> getChildMenus(List<Menu> menuList, Set<Menu> menuSet);

    /**
     * Build menu tree
     * @param menus Raw data
     * @return /
     */
    List<Menu> buildTree(List<Menu> menus);

    /**
     * Build menu tree
     * @param menus /
     * @return /
     */
    List<MenuVo> buildMenus(List<Menu> menus);

    /**
     * Delete
     * @param menuSet /
     */
    void delete(Set<Menu> menuSet);

    /**
     * Export
     * @param menus Data to export
     * @param response /
     * @throws IOException /
     */
    void download(List<Menu> menus, HttpServletResponse response) throws IOException;

    /**
     * Lazy load menu data
     * @param pid /
     * @return /
     */
    List<Menu> getMenus(Long pid);

    /**
     * Get sibling and parent data by ID
     * @param menu /
     * @param objects /
     * @return /
     */
    List<Menu> getSuperior(Menu menu, List<Menu> objects);

    /**
     * Get menus for current user
     * @param currentUserId /
     * @return /
     */
    List<Menu> findByUser(Long currentUserId);
}
