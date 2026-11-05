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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.modules.security.service.UserCacheManager;
import me.zhengjie.modules.security.service.dto.AuthorityDto;
import me.zhengjie.modules.system.domain.Menu;
import me.zhengjie.modules.system.domain.Role;
import me.zhengjie.exception.EntityExistException;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.mapper.RoleDeptMapper;
import me.zhengjie.modules.system.mapper.RoleMapper;
import me.zhengjie.modules.system.mapper.RoleMenuMapper;
import me.zhengjie.modules.system.mapper.UserMapper;
import me.zhengjie.modules.system.service.RoleService;
import me.zhengjie.modules.system.domain.dto.RoleQueryCriteria;
import me.zhengjie.utils.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Zheng Jie
 * @date 2018-12-03
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMapper roleMapper;
    private final RoleDeptMapper roleDeptMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final RedisUtils redisUtils;
    private final UserMapper userMapper;
    private final UserCacheManager userCacheManager;

    @Override
    public List<Role> queryAll() {
        return roleMapper.queryAll();
    }

    @Override
    public List<Role> queryAll(RoleQueryCriteria criteria) {
        return roleMapper.findAll(criteria);
    }

    @Override
    public PageResult<Role> queryAll(RoleQueryCriteria criteria, Page<Object> page) {
        criteria.setOffset(page.offset());
        List<Role> roles = roleMapper.findAll(criteria);
        Long total = roleMapper.countAll(criteria);
        return PageUtil.toPage(roles, total);
    }

    @Override
    public Role findById(long id) {
        String key = CacheKey.ROLE_ID + id;
        Role role = redisUtils.get(key, Role.class);
        if (role == null) {
            role = roleMapper.findById(id);
            redisUtils.set(key, role, 1, TimeUnit.DAYS);
        }
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Role resources) {
        if (roleMapper.findByName(resources.getName()) != null) {
            throw new EntityExistException(Role.class, "username", resources.getName());
        }
        save(resources);
        // Determine if department data exists, and create association if present
        if (CollectionUtil.isNotEmpty(resources.getDepts())) {
            roleDeptMapper.insertData(resources.getId(), resources.getDepts());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Role resources) {
        Role role = getById(resources.getId());
        Role role1 = roleMapper.findByName(resources.getName());
        if (role1 != null && !role1.getId().equals(role.getId())) {
            throw new EntityExistException(Role.class, "username", resources.getName());
        }
        role.setName(resources.getName());
        role.setDescription(resources.getDescription());
        role.setDataScope(resources.getDataScope());
        role.setDepts(resources.getDepts());
        role.setLevel(resources.getLevel());
        // Update
        saveOrUpdate(role);
        // Delete associated department data
        roleDeptMapper.deleteByRoleId(resources.getId());
        // Determine if department data exists, and update association if present
        if (CollectionUtil.isNotEmpty(resources.getDepts())) {
            roleDeptMapper.insertData(resources.getId(), resources.getDepts());
        }
        // Update relevant cache
        delCaches(role.getId(), null);
    }

    @Override
    public void updateMenu(Role role) {
        List<User> users = userMapper.findByRoleId(role.getId());
        // Update menus
        roleMenuMapper.deleteByRoleId(role.getId());
        // Check whether empty
        if(CollUtil.isNotEmpty(role.getMenus())){
            roleMenuMapper.insertData(role.getId(), role.getMenus());
        }
        // Update cache
        delCaches(role.getId(), users);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Long> ids) {
        for (Long id : ids) {
            // Update relevant cache
            delCaches(id, null);
        }
        removeBatchByIds(ids);
        // Delete role-department association and role-menu association data
        roleDeptMapper.deleteByRoleIds(ids);
        roleMenuMapper.deleteByRoleIds(ids);
    }

    @Override
    public List<Role> findByUsersId(Long userId) {
        String key = CacheKey.ROLE_USER + userId;
        List<Role> roles = redisUtils.getList(key, Role.class);
        if (CollUtil.isEmpty(roles)) {
            roles = roleMapper.findByUserId(userId);
            redisUtils.set(key, roles, 1, TimeUnit.DAYS);
        }
        return roles;
    }

    @Override
    public Integer findByRoles(Set<Role> roles) {
        if (CollUtil.isEmpty(roles)) {
            return Integer.MAX_VALUE;
        }
        Set<Role> roleSet = new HashSet<>();
        for (Role role : roles) {
            roleSet.add(findById(role.getId()));
        }
        return Collections.min(roleSet.stream().map(Role::getLevel).collect(Collectors.toList()));
    }

    @Override
    public List<AuthorityDto> buildPermissions(User user) {
        String key = CacheKey.ROLE_AUTH + user.getId();
        List<AuthorityDto> authorityDtos = redisUtils.getList(key, AuthorityDto.class);
        if (CollUtil.isEmpty(authorityDtos)) {
            Set<String> permissions = new HashSet<>();
            // If administrator, return directly
            if (user.getIsAdmin()) {
                permissions.add("admin");
                return permissions.stream().map(AuthorityDto::new)
                        .collect(Collectors.toList());
            }
            List<Role> roles = roleMapper.findByUserId(user.getId());
            permissions = roles.stream().flatMap(role -> role.getMenus().stream())
                    .map(Menu::getPermission)
                    .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
            authorityDtos = permissions.stream().map(AuthorityDto::new)
                    .collect(Collectors.toList());
            redisUtils.set(key, authorityDtos, 1, TimeUnit.HOURS);
        }
        return authorityDtos;
    }

    @Override
    public void download(List<Role> roles, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Role role : roles) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Role Name", role.getName());
            map.put("Role Level", role.getLevel());
            map.put("Description", role.getDescription());
            map.put("Create Date", role.getCreateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public void verification(Set<Long> ids) {
        if (userMapper.countByRoles(ids) > 0) {
            throw new BadRequestException("The selected role has associated users, please dissociate before proceeding!");
        }
    }

    @Override
    public List<Role> findByMenuId(Long menuId) {
        return roleMapper.findByMenuId(menuId);
    }

    /**
     * Clear cache
     * @param id /
     */
    public void delCaches(Long id, List<User> users) {
        users = CollectionUtil.isEmpty(users) ? userMapper.findByRoleId(id) : users;
        if (CollectionUtil.isNotEmpty(users)) {
            users.forEach(item -> userCacheManager.cleanUserCache(item.getUsername()));
            Set<Long> userIds = users.stream().map(User::getId).collect(Collectors.toSet());
            redisUtils.delByKeys(CacheKey.DATA_USER, userIds);
            redisUtils.delByKeys(CacheKey.MENU_USER, userIds);
            redisUtils.delByKeys(CacheKey.ROLE_AUTH, userIds);
            redisUtils.delByKeys(CacheKey.ROLE_USER, userIds);
        }
        redisUtils.del(CacheKey.ROLE_ID + id);
    }
}
