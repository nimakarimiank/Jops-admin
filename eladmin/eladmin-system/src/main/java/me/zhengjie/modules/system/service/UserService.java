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
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.domain.dto.UserQueryCriteria;
import me.zhengjie.utils.PageResult;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Zheng Jie
 * @date 2018-11-23
 */
public interface UserService extends IService<User> {

    /**
     * Query by ID
     *
     * @param id ID
     * @return /
     */
    User findById(long id);

    /**
     * Create user
     * @param resources /
     */
    void create(User resources);

    /**
     * Update user
     * @param resources /
     * @throws Exception /
     */
    void update(User resources) throws Exception;

    /**
     * Delete user
     * @param ids /
     */
    void delete(Set<Long> ids);

    /**
     * Query by username
     *
     * @param userName /
     * @return /
     */
    User findByName(String userName);

    /**
     * Query by username
     * @param userName /
     * @return /
     */
    User getLoginData(String userName);

    /**
     * Update password
     * @param username Username
     * @param encryptPassword Password
     */
    void updatePass(String username, String encryptPassword);

    /**
     * Update avatar
     * @param file File
     * @return /
     */
    Map<String, String> updateAvatar(MultipartFile file);

    /**
     * Update email
     * @param username Username
     * @param email Email
     */
    void updateEmail(String username, String email);

    /**
     * Query all
     *
     * @param criteria Criteria
     * @param page     Pagination parameters
     * @return /
     */
    PageResult<User> queryAll(UserQueryCriteria criteria, Page<Object> page);

    /**
     * Query all unpaged
     *
     * @param criteria Criteria
     * @return /
     */
    List<User> queryAll(UserQueryCriteria criteria);

    /**
     * Export data
     * @param queryAll Data to export
     * @param response /
     * @throws IOException /
     */
    void download(List<User> queryAll, HttpServletResponse response) throws IOException;

    /**
     * User self-service profile update
     * @param resources /
     */
    void updateCenter(User resources);

    /**
     * Reset password
     * @param ids User IDs
     * @param pwd Password
     */
    void resetPwd(Set<Long> ids, String pwd);
}
