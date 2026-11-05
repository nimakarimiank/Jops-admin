<template>
  <div class="app-container">
    <!-- Toolbar -->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- Search -->
        <el-input v-model="query.blurry" clearable placeholder="Fuzzy search" style="width: 200px" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker v-model="query.createTime" class="date-item" />
        <rrOperation />
      </div>
      <crudOperation :permission="permission">
        <el-button
          slot="right"
          v-permission="['admin','database:add']"
          :disabled="!selectIndex"
          class="filter-item"
          size="mini"
          type="warning"
          icon="el-icon-upload"
          @click="execute"
        >Execute Script
        </el-button>
      </crudOperation>
    </div>
    <!-- Form Component -->
    <eForm ref="execute" :database-info="currentRow" />
    <el-dialog append-to-body :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="530px">
      <el-form ref="form" :model="form" :rules="rules" size="small" label-width="100px">
        <el-form-item label="Connection Name" prop="name">
          <el-input v-model="form.name" style="width: 370px" />
        </el-form-item>
        <el-form-item label="JDBC URL" prop="jdbcUrl">
          <el-input v-model="form.jdbcUrl" style="width: 300px" />
          <el-button :loading="loading" type="success" @click="testConnectDatabase">Test</el-button>
        </el-form-item>
        <el-form-item label="User" prop="userName">
          <el-input v-model="form.userName" style="width: 370px" />
        </el-form-item>
        <el-form-item label="Password" prop="pwd">
          <el-input v-model="form.pwd" type="password" style="width: 370px" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="text" @click="crud.cancelCU">Cancel</el-button>
        <el-button :loading="crud.status.cu === 2" type="primary" @click="crud.submitCU">Confirm</el-button>
      </div>
    </el-dialog>
    <!-- Table Rendering -->
    <el-table ref="table" v-loading="crud.loading" :data="crud.data" highlight-current-row stripe style="width: 100%" @selection-change="handleCurrentChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" width="130px" label="Database Name" />
      <el-table-column prop="jdbcUrl" label="Connection URL" />
      <el-table-column prop="userName" width="200px" label="Username" />
      <el-table-column prop="createTime" width="200px" label="Created Date" />
      <el-table-column v-if="checkPer(['admin','database:edit','database:del'])" label="Operation" width="150px" align="center">
        <template slot-scope="scope">
          <udOperation
            :data="scope.row"
            :permission="permission"
          />
        </template>
      </el-table-column>
    </el-table>
    <!-- Pagination Component -->
    <pagination />
  </div>
</template>

<script>
import crudDatabase from '@/api/maint/database'
import { testDbConnect } from '@/api/maint/connect'
import eForm from './execute'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'

const defaultForm = { id: null, name: null, jdbcUrl: 'jdbc:mysql://', userName: null, pwd: null }
export default {
  name: 'DataBase',
  components: { eForm, pagination, crudOperation, rrOperation, udOperation, DateRangePicker },
  cruds() {
    return CRUD({ title: 'Database', url: 'api/database', crudMethod: { ...crudDatabase }})
  },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  data() {
    return {
      currentRow: {},
      selectIndex: '',
      databaseInfo: '',
      loading: false,
      permission: {
        add: ['admin', 'database:add'],
        edit: ['admin', 'database:edit'],
        del: ['admin', 'database:del']
      },
      rules: {
        name: [
          { required: true, message: 'Please enter database name', trigger: 'blur' }
        ],
        jdbcUrl: [
          { required: true, message: 'Please enter database connection URL', trigger: 'blur' }
        ],
        userName: [
          { required: true, message: 'Please enter username', trigger: 'blur' }
        ],
        pwd: [
          { required: true, message: 'Please enter database password', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    testConnectDatabase() {
      this.$refs['form'].validate((valid) => {
        if (valid) {
          this.loading = true
          testDbConnect(this.form).then((res) => {
            this.loading = false
            this.crud.notify(res ? 'Connection successful' : 'Connection failed', res ? 'success' : 'error')
          }).catch(() => {
            this.loading = false
          })
        }
      })
    },
    execute() {
      this.$refs.execute.dialog = true
    },
    handleCurrentChange(selection) {
      this.crud.selections = selection
      if (selection.length === 1) {
        const row = selection[0]
        this.selectIndex = row.id
        this.currentRow = row
      } else {
        this.currentRow = {}
        this.selectIndex = ''
      }
    }
  }
}
</script>

<style scoped>
</style>
