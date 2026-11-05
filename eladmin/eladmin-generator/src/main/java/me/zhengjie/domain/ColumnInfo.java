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
package me.zhengjie.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;

/**
 * Column data information
 * @author Zheng Jie
 * @date 2019-01-02
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("code_column")
public class ColumnInfo implements Serializable {

    @ApiModelProperty(value = "ID", hidden = true)
    @TableId(value = "column_id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Table name")
    private String tableName;

    @ApiModelProperty(value = "Database column name")
    private String columnName;

    @ApiModelProperty(value = "Database column type")
    private String columnType;

    @ApiModelProperty(value = "Database column key type")
    private String keyType;

    @ApiModelProperty(value = "Extra column parameters")
    private String extra;

    @ApiModelProperty(value = "Database column description")
    private String remark;

    @ApiModelProperty(value = "Whether required")
    private Boolean notNull;

    @ApiModelProperty(value = "Whether to display in list")
    private Boolean listShow = true;

    @ApiModelProperty(value = "Whether to display in form")
    private Boolean formShow = true;

    @ApiModelProperty(value = "Form type")
    private String formType;

    @ApiModelProperty(value = "Query 1: Fuzzy 2: Exact")
    private String queryType;

    @ApiModelProperty(value = "Dictionary name")
    private String dictName;
}
