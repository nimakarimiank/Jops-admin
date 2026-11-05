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
package me.zhengjie.modules.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import me.zhengjie.base.BaseEntity;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 * @author Zheng Jie
 * @date 2018-11-22
 */
@Getter
@Setter
@TableName("sys_user")
public class User extends BaseEntity implements Serializable {

    @NotNull(groups = Update.class)
    @TableId(value="user_id", type = IdType.AUTO)
    @ApiModelProperty(value = "ID", hidden = true)
    private Long id;

    @TableField(exist = false)
    @ApiModelProperty(value = "User roles")
    private Set<Role> roles;

    @TableField(exist = false)
    @ApiModelProperty(value = "User jobs")
    private Set<Job> jobs;

    @TableField(value = "dept_id")
    @ApiModelProperty(hidden = true)
    private Long deptId;

    @ApiModelProperty(value = "User department")
    @TableField(exist = false)
    private Dept dept;

    @NotBlank
    @ApiModelProperty(value = "Username")
    private String username;

    @NotBlank
    @ApiModelProperty(value = "User nickname")
    private String nickName;

    @Email
    @NotBlank
    @ApiModelProperty(value = "Email")
    private String email;

    @NotBlank
    @ApiModelProperty(value = "Phone number")
    private String phone;

    @ApiModelProperty(value = "User gender")
    private String gender;

    @ApiModelProperty(value = "Real avatar name",hidden = true)
    private String avatarName;

    @ApiModelProperty(value = "Avatar storage path", hidden = true)
    private String avatarPath;

    @ApiModelProperty(value = "Password")
    private String password;

    @NotNull
    @ApiModelProperty(value = "Is enabled")
    private Boolean enabled;

    @JSONField(deserialize = false)
    @ApiModelProperty(value = "Is admin account", hidden = true)
    private Boolean isAdmin = false;

    @ApiModelProperty(value = "Last password reset time", hidden = true)
    private Date pwdResetTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}