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
package me.zhengjie.modules.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.modules.system.domain.Menu;
import me.zhengjie.modules.system.domain.Role;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.domain.dto.MenuMetaVo;
import me.zhengjie.modules.system.domain.dto.MenuVo;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.exception.EntityExistException;
import me.zhengjie.modules.system.mapper.MenuMapper;
import me.zhengjie.modules.system.mapper.RoleMenuMapper;
import me.zhengjie.modules.system.mapper.UserMapper;
import me.zhengjie.modules.system.service.MenuService;
import me.zhengjie.modules.system.service.RoleService;
import me.zhengjie.modules.system.domain.dto.MenuQueryCriteria;
import me.zhengjie.utils.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Zheng Jie
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserMapper userMapper;
    private final RoleService roleService;
    private final RedisUtils redisUtils;

    private static final String HTTP_PRE = "http://";
    private static final String HTTPS_PRE = "https://";
    private static final String YES_STR = "Yes";
    private static final String NO_STR = "No";
    private static final String BAD_REQUEST = "External link must start with http:// or https://";
    
    @Override
    public List<Menu> queryAll(MenuQueryCriteria criteria, Boolean isQuery) throws Exception {
        if(Boolean.TRUE.equals(isQuery)){
            criteria.setPidIsNull(true);
            List<Field> fields = StringUtils.getAllFields(criteria.getClass(), new ArrayList<>());
            for (Field field : fields) {
                // Set accessible permission on object to allow access to private properties
                field.setAccessible(true);
                Object val = field.get(criteria);
                if("pidIsNull".equals(field.getName())){
                    continue;
                }
                // If there are query criteria, do not specify pidIsNull
                if (ObjectUtil.isNotNull(val)) {
                    criteria.setPidIsNull(null);
                    break;
                }
            }
        }
        return menuMapper.findAll(criteria);
    }

    @Override
    public Menu findById(long id) {
        String key = CacheKey.MENU_ID + id;
        Menu menu = redisUtils.get(key, Menu.class);
        if(menu == null){
            menu = getById(id);
            redisUtils.set(key, menu, 1, TimeUnit.DAYS);
        }
        return menu;
    }

    /**
     * Clear cache when user role changes
     * @param currentUserId /
     * @return /
     */
    @Override
    public List<Menu> findByUser(Long currentUserId) {
        String key = CacheKey.MENU_USER + currentUserId;
        List<Menu> menus = redisUtils.getList(key, Menu.class);
        if (CollUtil.isEmpty(menus)){
            List<Role> roles = roleService.findByUsersId(currentUserId);
            Set<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
            menus = new ArrayList<>(menuMapper.findByRoleIdsAndTypeNot(roleIds, 2));
            redisUtils.set(key, menus, 1, TimeUnit.DAYS);
        }
        return menus;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Menu resources) {
        if(menuMapper.findByTitle(resources.getTitle()) != null){
            throw new EntityExistException(Menu.class,"title",resources.getTitle());
        }
        if(StringUtils.isNotBlank(resources.getComponentName())){
            if(menuMapper.findByComponentName(resources.getComponentName()) != null){
                throw new EntityExistException(Menu.class,"componentName",resources.getComponentName());
            }
        }
        if (Long.valueOf(0L).equals(resources.getPid())) {
            resources.setPid(null);
        }
        if(resources.getIFrame()){
            if (!(resources.getPath().toLowerCase().startsWith(HTTP_PRE)||resources.getPath().toLowerCase().startsWith(HTTPS_PRE))) {
                throw new BadRequestException(BAD_REQUEST);
            }
        }
        save(resources);
        // Calculate sub node count
        resources.setSubCount(0);
        // Update parent menu sub node count
        updateSubCnt(resources.getPid());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Menu resources) {
        if(resources.getId().equals(resources.getPid())) {
            throw new BadRequestException("The parent menu cannot be itself");
        }
        Menu menu = getById(resources.getId());
        if(resources.getIFrame()){
            if (!(resources.getPath().toLowerCase().startsWith(HTTP_PRE)||resources.getPath().toLowerCase().startsWith(HTTPS_PRE))) {
                throw new BadRequestException(BAD_REQUEST);
            }
        }
        Menu menu1 = menuMapper.findByTitle(resources.getTitle());

        if(menu1 != null && !menu1.getId().equals(menu.getId())){
            throw new EntityExistException(Menu.class,"title",resources.getTitle());
        }

        if(resources.getPid().equals(0L)){
            resources.setPid(null);
        }

        // Recorded parent node ID
        Long oldPid = menu.getPid();
        Long newPid = resources.getPid();

        if(StringUtils.isNotBlank(resources.getComponentName())){
            menu1 = menuMapper.findByComponentName(resources.getComponentName());
            if(menu1 != null && !menu1.getId().equals(menu.getId())){
                throw new EntityExistException(Menu.class,"componentName",resources.getComponentName());
            }
        }
        menu.setTitle(resources.getTitle());
        menu.setComponent(resources.getComponent());
        menu.setPath(resources.getPath());
        menu.setIcon(resources.getIcon());
        menu.setIFrame(resources.getIFrame());
        menu.setPid(resources.getPid());
        menu.setMenuSort(resources.getMenuSort());
        menu.setCache(resources.getCache());
        menu.setHidden(resources.getHidden());
        menu.setComponentName(resources.getComponentName());
        menu.setPermission(resources.getPermission());
        menu.setType(resources.getType());
        saveOrUpdate(menu);
        // Calculate parent menu node count
        updateSubCnt(oldPid);
        updateSubCnt(newPid);
        // Clear cache
        delCaches(resources.getId());
    }

    @Override
    public Set<Menu> getChildMenus(List<Menu> menuList, Set<Menu> menuSet) {
        for (Menu menu : menuList) {
            menuSet.add(menu);
            List<Menu> menus = menuMapper.findByPidOrderByMenuSort(menu.getId());
            if(CollUtil.isNotEmpty(menus)){
                getChildMenus(menus, menuSet);
            }
        }
        return menuSet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Menu> menuSet) {
        for (Menu menu : menuSet) {
            // Clear cache
            delCaches(menu.getId());
            roleMenuMapper.deleteByMenuId(menu.getId());
            menuMapper.deleteById(menu.getId());
            updateSubCnt(menu.getPid());
        }
    }

    @Override
    public List<Menu> getMenus(Long pid) {
        List<Menu> menus;
        if(pid != null && !pid.equals(0L)){
            menus = menuMapper.findByPidOrderByMenuSort(pid);
        } else {
            menus = menuMapper.findByPidIsNullOrderByMenuSort();
        }
        return menus;
    }

    @Override
    public List<Menu> getSuperior(Menu menu, List<Menu> menus) {
        if(menu.getPid() == null){
            menus.addAll(menuMapper.findByPidIsNullOrderByMenuSort());
            return menus;
        }
        menus.addAll(menuMapper.findByPidOrderByMenuSort(menu.getPid()));
        return getSuperior(findById(menu.getPid()), menus);
    }

    @Override
    public List<Menu> buildTree(List<Menu> menus) {
        List<Menu> trees = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        for (Menu menu : menus) {
            if (menu.getPid() == null) {
                trees.add(menu);
            }
            for (Menu it : menus) {
                if (menu.getId().equals(it.getPid())) {
                    if (menu.getChildren() == null) {
                        menu.setChildren(new ArrayList<>());
                    }
                    menu.getChildren().add(it);
                    ids.add(it.getId());
                }
            }
        }
        if(CollUtil.isNotEmpty(trees)){
            trees = menus.stream().filter(s -> !ids.contains(s.getId())).collect(Collectors.toList());
        }
        return trees;
    }

    @Override
    public List<MenuVo> buildMenus(List<Menu> menus) {
        List<MenuVo> list = new LinkedList<>();
        menus.forEach(menu -> {
                    if (menu!=null){
                        List<Menu> menuList = menu.getChildren();
                        MenuVo menuVo = new MenuVo();
                        menuVo.setName(ObjectUtil.isNotEmpty(menu.getComponentName())  ? menu.getComponentName() : menu.getTitle());
                        // First-level directory requires leading slash to avoid warnings
                        menuVo.setPath(menu.getPid() == null ? "/" + menu.getPath() :menu.getPath());
                        menuVo.setHidden(menu.getHidden());
                        // If not an external link
                        if(!menu.getIFrame()){
                            if(menu.getPid() == null){
                                menuVo.setComponent(StringUtils.isEmpty(menu.getComponent())?"Layout":menu.getComponent());
                                // If not top-level menu and type is directory, it represents a multi-level menu
                            }else if(menu.getType() == 0){
                                menuVo.setComponent(StringUtils.isEmpty(menu.getComponent())?"ParentView":menu.getComponent());
                            }else if(StringUtils.isNoneBlank(menu.getComponent())){
                                menuVo.setComponent(menu.getComponent());
                            }
                        }
                        menuVo.setMeta(new MenuMetaVo(menu.getTitle(),menu.getIcon(),!menu.getCache()));
                        if(CollectionUtil.isNotEmpty(menuList)){
                            menuVo.setAlwaysShow(true);
                            menuVo.setRedirect("noredirect");
                            menuVo.setChildren(buildMenus(menuList));
                            // Handle case of top-level menu without children
                        } else if(menu.getPid() == null){
                            MenuVo menuVo1 = getMenuVo(menu, menuVo);
                            menuVo.setName(null);
                            menuVo.setMeta(null);
                            menuVo.setComponent("Layout");
                            List<MenuVo> list1 = new ArrayList<>();
                            list1.add(menuVo1);
                            menuVo.setChildren(list1);
                        }
                        list.add(menuVo);
                    }
                }
        );
        return list;
    }

    @Override
    public void download(List<Menu> menus, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Menu menu : menus) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("Menu Title", menu.getTitle());
            map.put("Menu Type", menu.getType() == null ? "Directory" : menu.getType() == 1 ? "Menu" : "Button");
            map.put("Permission", menu.getPermission());
            map.put("External Link", menu.getIFrame() ? YES_STR : NO_STR);
            map.put("Visible", menu.getHidden() ? NO_STR : YES_STR);
            map.put("Cache", menu.getCache() ? YES_STR : NO_STR);
            map.put("Create Date", menu.getCreateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    private void updateSubCnt(Long menuId){
        if(menuId != null){
            int count = menuMapper.countByPid(menuId);
            menuMapper.updateSubCntById(count, menuId);
        }
    }

    /**
     * Clear cache
     * @param id Menu ID
     */
    public void delCaches(Long id){
        List<User> users = userMapper.findByMenuId(id);
        redisUtils.del(CacheKey.MENU_ID + id);
        redisUtils.delByKeys(CacheKey.MENU_USER, users.stream().map(User::getId).collect(Collectors.toSet()));
        // Clear Role cache
        List<Role> roles = roleService.findByMenuId(id);
        redisUtils.delByKeys(CacheKey.ROLE_ID, roles.stream().map(Role::getId).collect(Collectors.toSet()));
    }

    /**
     * Get MenuVo
     * @param menu /
     * @param menuVo /
     * @return /
     */
    private static MenuVo getMenuVo(Menu menu, MenuVo menuVo) {
        MenuVo menuVo1 = new MenuVo();
        menuVo1.setMeta(menuVo.getMeta());
        // Non-external link
        if(!menu.getIFrame()){
            menuVo1.setPath("index");
            menuVo1.setName(menuVo.getName());
            menuVo1.setComponent(menuVo.getComponent());
        } else {
            menuVo1.setPath(menu.getPath());
        }
        return menuVo1;
    }
}
