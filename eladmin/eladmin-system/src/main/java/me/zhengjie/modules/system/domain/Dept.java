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

import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import me.zhengjie.base.BaseEntity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
* @author Zheng Jie
* @date 2019-03-25
*/
@Getter
@Setter
@TableName("sys_dept")
public class Dept extends BaseEntity implements Serializable {

    @NotNull(groups = Update.class)
    @TableId(value="dept_id", type = IdType.AUTO)
    @ApiModelProperty(value = "ID", hidden = true)
    private Long id;

    @TableField(exist = false)
    @JSONField(serialize = false)
    @ApiModelProperty(value = "Roles")
    private Set<Role> roles;

    @TableField(exist = false)
    @ApiModelProperty(value = "Sub departments")
    private List<Dept> children;

    @ApiModelProperty(value = "Sort")
    private Integer deptSort;

    @NotBlank
    @ApiModelProperty(value = "Department name")
    private String name;

    @NotNull
    @ApiModelProperty(value = "Is enabled")
    private Boolean enabled;

    @ApiModelProperty(value = "Parent department")
    private Long pid;

    @ApiModelProperty(value = "Sub node count", hidden = true)
    private Integer subCount = 0;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Dept dept = (Dept) o;
        return Objects.equals(id, dept.id) &&
                Objects.equals(name, dept.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @ApiModelProperty(value = "Has children")
    public Boolean getHasChildren() {
        return subCount > 0;
    }


    @ApiModelProperty(value = "Is leaf")
    public Boolean getLeaf() {
        return subCount <= 0;
    }

    @ApiModelProperty(value = "Label name")
    public String getLabel() {
        return name;
    }
}