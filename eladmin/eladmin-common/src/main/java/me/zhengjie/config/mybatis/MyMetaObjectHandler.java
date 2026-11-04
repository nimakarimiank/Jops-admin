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
package me.zhengjie.config.mybatis;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import me.zhengjie.utils.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;

/**
 * @author Zheng Jie
 * @description
 * @date 2023-06-13
 **/
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        /* Creation time */
        this.strictInsertFill(metaObject, "createTime", Timestamp.class, DateTime.now().toTimestamp());
        this.strictInsertFill(metaObject, "updateTime", Timestamp.class, DateTime.now().toTimestamp());
        /* Operator */
        String username = "System";
        try {username = SecurityUtils.getCurrentUsername();}catch (Exception ignored){}
        this.strictInsertFill(metaObject, "createBy", String.class, username);
        this.strictInsertFill(metaObject, "updateBy", String.class, username);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        /* Update time */
        this.strictUpdateFill(metaObject, "updateTime", Timestamp.class, DateTime.now().toTimestamp());
        /* Operator */
        String username = "System";
        try {username = SecurityUtils.getCurrentUsername();}catch (Exception ignored){}
        this.strictUpdateFill(metaObject, "updateBy", String.class, username);
    }
}

