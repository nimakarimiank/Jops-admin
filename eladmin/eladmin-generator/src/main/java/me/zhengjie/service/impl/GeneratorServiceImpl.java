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
package me.zhengjie.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ZipUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.domain.GenConfig;
import me.zhengjie.domain.ColumnInfo;
import me.zhengjie.domain.dto.TableInfo;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.mapper.ColumnInfoMapper;
import me.zhengjie.service.GeneratorService;
import me.zhengjie.utils.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Zheng Jie
 * @date 2019-01-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratorServiceImpl extends ServiceImpl<ColumnInfoMapper, ColumnInfo> implements GeneratorService {

    private final ColumnInfoMapper columnInfoMapper;
    private final String CONFIG_MESSAGE = "Please configure the generator first";

    @Override
    public PageResult<TableInfo> getTables(String name, Page<Object> page) {
        return PageUtil.toPage(columnInfoMapper.getTables(name, page));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ColumnInfo> getColumns(String tableName) {
        List<ColumnInfo> columnInfos = columnInfoMapper.findByTableNameOrderByIdAsc(tableName);
        if (CollectionUtil.isNotEmpty(columnInfos)) {
            return columnInfos;
        } else {
            columnInfos = query(tableName);
            saveBatch(columnInfos);
            return columnInfos;
        }
    }

    @Override
    public List<ColumnInfo> query(String tableName) {
        List<ColumnInfo> columnInfos = columnInfoMapper.getColumns(tableName);
        for (ColumnInfo columnInfo : columnInfos) {
            columnInfo.setTableName(tableName);
            if(GenUtil.PK.equalsIgnoreCase(columnInfo.getKeyType())
                    && GenUtil.EXTRA.equalsIgnoreCase(columnInfo.getExtra())){
                columnInfo.setNotNull(false);
            }
        }
        return columnInfos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sync(List<ColumnInfo> columnInfos, List<ColumnInfo> columnInfoList) {
        // Case 1: database field modified or added
        for (ColumnInfo columnInfo : columnInfoList) {
            // Find by column name
            List<ColumnInfo> columns = columnInfos.stream().filter(c -> c.getColumnName().equals(columnInfo.getColumnName())).collect(Collectors.toList());
            // If found, update modified fields
            if (CollectionUtil.isNotEmpty(columns)) {
                ColumnInfo column = columns.get(0);
                column.setColumnType(columnInfo.getColumnType());
                column.setExtra(columnInfo.getExtra());
                column.setKeyType(columnInfo.getKeyType());
                if (StringUtils.isBlank(column.getRemark())) {
                    column.setRemark(columnInfo.getRemark());
                }
                saveOrUpdate(column);
            } else {
                // If not found, save new column info
                save(columnInfo);
            }
        }
        // Case 2: database column was deleted
        for (ColumnInfo columnInfo : columnInfos) {
            // Find by column name
            List<ColumnInfo> columns = columnInfoList.stream().filter(c -> c.getColumnName().equals(columnInfo.getColumnName())).collect(Collectors.toList());
            // If not found, the column was deleted and should be removed
            if (CollectionUtil.isEmpty(columns)) {
                removeById(columnInfo);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(List<ColumnInfo> columnInfos) {
        saveOrUpdateBatch(columnInfos);
    }

    @Override
    public void generator(GenConfig genConfig, List<ColumnInfo> columns) {
        if (genConfig.getId() == null) {
            throw new BadRequestException(CONFIG_MESSAGE);
        }
        try {
            GenUtil.generatorCode(columns, genConfig);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new BadRequestException("Generation failed, please manually handle generated files");
        }
    }

    @Override
    public ResponseEntity<Object> preview(GenConfig genConfig, List<ColumnInfo> columns) {
        if (genConfig.getId() == null) {
            throw new BadRequestException(CONFIG_MESSAGE);
        }
        List<Map<String, Object>> genList = GenUtil.preview(columns, genConfig);
        return new ResponseEntity<>(genList, HttpStatus.OK);
    }

    @Override
    public void download(GenConfig genConfig, List<ColumnInfo> columns, HttpServletRequest request, HttpServletResponse response) {
        if (genConfig.getId() == null) {
            throw new BadRequestException(CONFIG_MESSAGE);
        }
        try {
            File file = new File(GenUtil.download(columns, genConfig));
            String zipPath = file.getPath() + ".zip";
            ZipUtil.zip(file.getPath(), zipPath);
            FileUtil.downloadFile(request, response, new File(zipPath), true);
        } catch (IOException e) {
            throw new BadRequestException("Packaging failed");
        }
    }
}
