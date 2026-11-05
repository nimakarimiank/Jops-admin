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
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import me.zhengjie.base.BaseEntity;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Zheng Jie
 * @date 2018-12-17
 */
@Getter
@Setter
@TableName("sys_menu")
public class Menu extends BaseEntity implements Serializable {

    @NotNull(groups = {Update.class})
    @TableId(value="menu_id", type = IdType.AUTO)
    @ApiModelProperty(value = "ID", hidden = true)
    private Long id;

    @TableField(exist = false)
    @JSONField(serialize = false)
    @ApiModelProperty(value = "Menu roles")
    private Set<Role> roles;

    @TableField(exist = false)
    private List<Menu> children;

    @ApiModelProperty(value = "Menu title")
    private String title;

    @TableField(value = "name")
    @ApiModelProperty(value = "Menu component name")
    private String componentName;

    @ApiModelProperty(value = "Sort")
    private Integer menuSort = 999;

    @ApiModelProperty(value = "Component path")
    private String component;

    @ApiModelProperty(value = "Route path")
    private String path;

    @ApiModelProperty(value = "Menu type: directory, menu, button")
    private Integer type;

    @ApiModelProperty(value = "Permission")
    private String permission;

    @ApiModelProperty(value = "Menu icon")
    private String icon;

    @ApiModelProperty(value = "Cache")
    private Boolean cache;

    @ApiModelProperty(value = "Is hidden")
    private Boolean hidden;

    @ApiModelProperty(value = "Parent menu")
    private Long pid;

    @ApiModelProperty(value = "Sub node count", hidden = true)
    private Integer subCount = 0;

    @ApiModelProperty(value = "External link menu")
    @JsonProperty("iFrame")
    private Boolean iFrame;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Menu menu = (Menu) o;
        return Objects.equals(id, menu.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
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
        return title;
    }
}
