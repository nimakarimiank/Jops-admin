<template>
  <el-dialog :visible.sync="dialog" append-to-body title="Execution Log" width="88%">
    <!-- Search -->
    <div class="head-container">
      <el-input v-model="query.jobName" clearable size="small" placeholder="Enter job name to search" style="width: 200px;" class="filter-item" @keyup.enter.native="toQuery" />
      <date-range-picker v-model="query.createTime" class="date-item" />
      <el-select v-model="query.isSuccess" placeholder="Log status" clearable size="small" class="filter-item" style="width: 110px" @change="toQuery">
        <el-option v-for="item in enabledTypeOptions" :key="item.key" :label="item.display_name" :value="item.key" />
      </el-select>
      <el-button class="filter-item" size="mini" type="success" icon="el-icon-search" @click="toQuery">Search</el-button>
      <!-- Export -->
      <div style="display: inline-block;">
        <el-button
          :loading="downloadLoading"
          size="mini"
          class="filter-item"
          type="warning"
          icon="el-icon-download"
          @click="downloadMethod"
        >Export</el-button>
      </div>
    </div>
    <!-- Table rendering -->
    <el-table v-loading="loading" :data="data" style="width: 100%;margin-top: -10px;">
      <el-table-column :show-overflow-tooltip="true" prop="jobName" label="Job Name" />
      <el-table-column :show-overflow-tooltip="true" prop="beanName" label="Bean Name" />
      <el-table-column :show-overflow-tooltip="true" prop="methodName" label="Method Name" />
      <el-table-column :show-overflow-tooltip="true" prop="params" width="120px" label="Parameters" />
      <el-table-column :show-overflow-tooltip="true" prop="cronExpression" label="Cron Expression" />
      <el-table-column prop="createTime" label="Exception Details" width="110px">
        <template slot-scope="scope">
          <el-button v-show="scope.row.exceptionDetail" size="mini" type="text" @click="info(scope.row.exceptionDetail)">View Details</el-button>
        </template>
      </el-table-column>
      <el-table-column :show-overflow-tooltip="true" align="center" prop="time" width="100px" label="Duration (ms)" />
      <el-table-column align="center" prop="isSuccess" width="80px" label="Status">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isSuccess ? 'success' : 'danger'">{{ scope.row.isSuccess ? 'Success' : 'Failure' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :show-overflow-tooltip="true" prop="createTime" label="Created Date" />
    </el-table>
    <el-dialog :visible.sync="errorDialog" append-to-body title="Exception Details" width="85%">
      <pre>{{ errorInfo }}</pre>
    </el-dialog>
    <!-- Pagination component -->
    <el-pagination
      :total="total"
      :current-page="page"
      :page-size="6"
      style="margin-top:8px;"
      layout="total, prev, pager, next"
      @size-change="sizeChange"
      @current-change="pageChange"
    />
  </el-dialog>
</template>

<script>
import crud from '@/mixins/crud'
import DateRangePicker from '@/components/DateRangePicker'
export default {
  components: { DateRangePicker },
  mixins: [crud],
  data() {
    return {
      title: 'Job Log',
      errorInfo: '', errorDialog: false,
      enabledTypeOptions: [
        { key: 'true', display_name: 'Success' },
        { key: 'false', display_name: 'Failure' }
      ]
    }
  },
  methods: {
    doInit() {
      this.$nextTick(() => {
        this.init()
      })
    },
    // Set API URL before fetching data
    beforeInit() {
      this.url = 'api/jobs/logs'
      this.size = 6
      return true
    },
    // Exception details
    info(errorInfo) {
      this.errorInfo = errorInfo
      this.errorDialog = true
    }
  }
}
</script>

<style scoped>
  .java.hljs{
    color: #444;
    background: #ffffff !important;
  }
 ::v-deep .el-dialog__body{
    padding: 0 20px 10px 20px !important;
  }
</style>
