<template>
  <div class="app-container">
    <!-- Toolbar -->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- Search -->
        <el-input v-model="query.name" clearable placeholder="Search by name" style="width: 200px" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker v-model="query.createTime" class="date-item" />
        <rrOperation />
      </div>
      <crudOperation :permission="permission">
        <el-button
          slot="left"
          v-permission="['admin','app:add']"
          :disabled="!currentRow"
          class="filter-item"
          size="mini"
          type="primary"
          icon="el-icon-plus"
          @click="copy"
        >Copy</el-button>
      </crudOperation>
    </div>
    <!-- Form Component -->
    <el-dialog append-to-body :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="800px">
      <el-form ref="form" :model="form" :rules="rules" size="small" label-width="100px">
        <el-form-item label="App Name" prop="name">
          <el-input v-model="form.name" style="width: 670px" placeholder="Deployed file or directory name, used for backup" />
        </el-form-item>
        <el-form-item label="App Port" prop="port">
          <el-input-number v-model.number="form.port" placeholder="e.g.: 8080" />
        </el-form-item>
        <el-form-item label="Upload Path" prop="uploadPath">
          <el-input v-model="form.uploadPath" style="width: 670px" placeholder="e.g.: /opt/upload" />
        </el-form-item>
        <el-form-item label="Deploy Path" prop="deployPath">
          <el-input v-model="form.deployPath" style="width: 670px" placeholder="e.g.: /opt/app" />
        </el-form-item>
        <el-form-item label="Backup Path" prop="backupPath">
          <el-input v-model="form.backupPath" style="width: 670px" placeholder="e.g.: /opt/backup" />
        </el-form-item>
        <el-form-item label="Deploy Script" prop="deployScript">
          <el-input v-model="form.deployScript" :rows="3" type="textarea" autosize style="width: 670px" placeholder="" />
        </el-form-item>
        <el-form-item label="Start Script" prop="startScript">
          <el-input v-model="form.startScript" :rows="3" type="textarea" autosize style="width: 670px" placeholder="" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="text" @click="crud.cancelCU">Cancel</el-button>
        <el-button :loading="crud.status.cu === 2" type="primary" @click="crud.submitCU">Confirm</el-button>
      </div>
    </el-dialog>
    <!-- Table Rendering -->
    <el-table ref="table" v-loading="crud.loading" :data="crud.data" highlight-current-row style="width: 100%" @selection-change="crud.selectionChangeHandler" @current-change="handleCurrentChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="App Name" />
      <el-table-column prop="port" label="Port" />
      <el-table-column prop="uploadPath" label="Upload Path" />
      <el-table-column prop="deployPath" label="Deploy Path" />
      <el-table-column prop="backupPath" label="Backup Path" />
      <el-table-column prop="createTime" label="Created Date" />
      <el-table-column v-if="checkPer(['admin','app:edit','app:del'])" label="Operation" width="150px" align="center">
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
import crudApp from '@/api/maint/app'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'

const defaultForm = { id: null, name: null, port: 8080, uploadPath: '/opt/upload', deployPath: '/opt/app', backupPath: '/opt/backup', startScript: null, deployScript: null }
export default {
  name: 'App',
  components: { pagination, crudOperation, rrOperation, udOperation, DateRangePicker },
  cruds() {
    return CRUD({ title: 'App', url: 'api/app', crudMethod: { ...crudApp }})
  },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  data() {
    return {
      currentRow: null,
      permission: {
        add: ['admin', 'app:add'],
        edit: ['admin', 'app:edit'],
        del: ['admin', 'app:del']
      },
      rules: {
        name: [
          { required: true, message: 'Please enter app name', trigger: 'blur' }
        ],
        port: [
          { required: true, message: 'Please enter app port', trigger: 'blur', type: 'number' }
        ],
        uploadPath: [
          { required: true, message: 'Please enter upload path', trigger: 'blur' }
        ],
        deployPath: [
          { required: true, message: 'Please enter deploy path', trigger: 'blur' }
        ],
        backupPath: [
          { required: true, message: 'Please enter backup path', trigger: 'blur' }
        ],
        startScript: [
          { required: true, message: 'Please enter start script', trigger: 'blur' }
        ],
        deployScript: [
          { required: true, message: 'Please enter deploy script', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    copy() {
      for (const key in this.currentRow) {
        this.form[key] = this.currentRow[key]
      }
      this.form.id = null
      this.form.createTime = null
      this.crud.toAdd()
    },
    handleCurrentChange(row) {
      this.currentRow = JSON.parse(JSON.stringify(row))
    }
  }
}
</script>

<style scoped>
</style>
