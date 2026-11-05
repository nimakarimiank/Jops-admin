<template>
  <div class="app-container">
    <!-- Toolbar -->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- Search -->
        <el-input v-model="query.jobName" clearable size="small" placeholder="Enter job name to search" style="width: 200px;" class="filter-item" @keyup.enter.native="toQuery" />
        <date-range-picker v-model="query.createTime" class="date-item" />
        <rrOperation />
      </div>
      <crudOperation :permission="permission">
        <!-- Job log -->
        <el-button
          slot="right"
          class="filter-item"
          size="mini"
          type="info"
          icon="el-icon-tickets"
          @click="doLog"
        >Log</el-button>
      </crudOperation>
      <Log ref="log" />
    </div>
    <!-- Form -->
    <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" append-to-body width="730px">
      <el-form ref="form" :inline="true" :model="form" :rules="rules" size="small" label-width="100px">
        <el-form-item label="Job Name" prop="jobName">
          <el-input v-model="form.jobName" style="width: 220px;" />
        </el-form-item>
        <el-form-item label="Job Description" prop="description">
          <el-input v-model="form.description" style="width: 220px;" />
        </el-form-item>
        <el-form-item label="Bean Name" prop="beanName">
          <el-input v-model="form.beanName" style="width: 220px;" />
        </el-form-item>
        <el-form-item label="Method Name" prop="methodName">
          <el-input v-model="form.methodName" style="width: 220px;" />
        </el-form-item>
        <el-form-item label="Cron Expression" prop="cronExpression">
          <el-input v-model="form.cronExpression" style="width: 220px;" />
        </el-form-item>
        <el-form-item label="Subtask ID">
          <el-input v-model="form.subTask" placeholder="Separate multiple with commas, execute in order" style="width: 220px;" />
        </el-form-item>
        <el-form-item label="Person in Charge" prop="personInCharge">
          <el-input v-model="form.personInCharge" style="width: 220px;" />
        </el-form-item>
        <el-form-item label="Alert Email" prop="email">
          <el-input v-model="form.email" placeholder="Separate multiple emails with commas" style="width: 220px;" />
        </el-form-item>
        <el-form-item label="Pause on Failure">
          <el-radio-group v-model="form.pauseAfterFailure" style="width: 220px">
            <el-radio :label="true">Yes</el-radio>
            <el-radio :label="false">No</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Job Status">
          <el-radio-group v-model="form.isPause" style="width: 220px">
            <el-radio :label="false">Enabled</el-radio>
            <el-radio :label="true">Paused</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Parameters">
          <el-input v-model="form.params" style="width: 556px;" rows="4" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="text" @click="crud.cancelCU">Cancel</el-button>
        <el-button :loading="crud.status.cu === 2" type="primary" @click="crud.submitCU">Confirm</el-button>
      </div>
    </el-dialog>
    <!-- Table rendering -->
    <el-table ref="table" v-loading="crud.loading" :data="crud.data" style="width: 100%;" @selection-change="crud.selectionChangeHandler">
      <el-table-column :selectable="checkboxT" type="selection" width="55" />
      <el-table-column :show-overflow-tooltip="true" prop="id" label="Job ID" />
      <el-table-column :show-overflow-tooltip="true" prop="jobName" label="Job Name" />
      <el-table-column :show-overflow-tooltip="true" prop="beanName" label="Bean Name" />
      <el-table-column :show-overflow-tooltip="true" prop="methodName" label="Method Name" />
      <el-table-column :show-overflow-tooltip="true" prop="params" label="Parameters" />
      <el-table-column :show-overflow-tooltip="true" prop="cronExpression" label="Cron Expression" />
      <el-table-column :show-overflow-tooltip="true" prop="isPause" width="90px" label="Status">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isPause ? 'warning' : 'success'">{{ scope.row.isPause ? 'Paused' : 'Running' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :show-overflow-tooltip="true" prop="description" width="150px" label="Description" />
      <el-table-column :show-overflow-tooltip="true" prop="createTime" width="136px" label="Created Date" />
      <el-table-column v-if="checkPer(['admin','timing:edit','timing:del'])" label="Actions" width="170px" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button v-permission="['admin','timing:edit']" size="mini" style="margin-right: 3px;" type="text" @click="crud.toEdit(scope.row)">Edit</el-button>
          <el-button v-permission="['admin','timing:edit']" style="margin-left: -2px" type="text" size="mini" @click="execute(scope.row.id)">Execute</el-button>
          <el-button v-permission="['admin','timing:edit']" style="margin-left: 3px" type="text" size="mini" @click="updateStatus(scope.row.id,scope.row.isPause ? 'Resume' : 'Pause')">
            {{ scope.row.isPause ? 'Resume' : 'Pause' }}
          </el-button>
          <el-popover
            :ref="scope.row.id"
            v-permission="['admin','timing:del']"
            placement="top"
            width="200"
          >
            <p>Are you sure you want to stop and delete this task?</p>
            <div style="text-align: right; margin: 0">
              <el-button size="mini" type="text" @click="$refs[scope.row.id].doClose()">Cancel</el-button>
              <el-button :loading="delLoading" type="primary" size="mini" @click="delMethod(scope.row.id)">Confirm</el-button>
            </div>
            <el-button slot="reference" type="text" size="mini">Delete</el-button>
          </el-popover>
        </template>
      </el-table-column>
    </el-table>
    <!-- Pagination component -->
    <pagination />
  </div>
</template>

<script>
import crudJob from '@/api/system/timing'
import Log from './log'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'

const defaultForm = { id: null, jobName: null, subTask: null, beanName: null, methodName: null, params: null, cronExpression: null, pauseAfterFailure: true, isPause: false, personInCharge: null, email: null, description: null }
export default {
  name: 'Timing',
  components: { Log, pagination, crudOperation, rrOperation, DateRangePicker },
  cruds() {
    return CRUD({ title: 'Scheduled Task', url: 'api/jobs', crudMethod: { ...crudJob }})
  },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  data() {
    return {
      delLoading: false,
      permission: {
        add: ['admin', 'timing:add'],
        edit: ['admin', 'timing:edit'],
        del: ['admin', 'timing:del']
      },
      rules: {
        jobName: [
          { required: true, message: 'Please enter a job name', trigger: 'blur' }
        ],
        description: [
          { required: true, message: 'Please enter a job description', trigger: 'blur' }
        ],
        beanName: [
          { required: true, message: 'Please enter a bean name', trigger: 'blur' }
        ],
        methodName: [
          { required: true, message: 'Please enter a method name', trigger: 'blur' }
        ],
        cronExpression: [
          { required: true, message: 'Please enter a cron expression', trigger: 'blur' }
        ],
        personInCharge: [
          { required: true, message: 'Please enter person in charge', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    // Execute
    execute(id) {
      crudJob.execution(id).then(res => {
        this.crud.notify('Executed successfully', CRUD.NOTIFICATION_TYPE.SUCCESS)
      }).catch(err => {
        console.log(err.response.data.message)
      })
    },
    // Change status
    updateStatus(id, status) {
      if (status === 'Resume') {
        this.updateParams(id)
      }
      crudJob.updateIsPause(id).then(res => {
        this.crud.toQuery()
        this.crud.notify(status + ' succeeded', CRUD.NOTIFICATION_TYPE.SUCCESS)
      }).catch(err => {
        console.log(err.response.data.message)
      })
    },
    updateParams(id) {
      console.log(id)
    },
    delMethod(id) {
      this.delLoading = true
      crudJob.del([id]).then(() => {
        this.delLoading = false
        this.$refs[id].doClose()
        this.crud.dleChangePage(1)
        this.crud.delSuccessNotify()
        this.crud.toQuery()
      }).catch(() => {
        this.delLoading = false
        this.$refs[id].doClose()
      })
    },
    // Show logs
    doLog() {
      this.$refs.log.dialog = true
      this.$refs.log.doInit()
    },
    checkboxT(row, rowIndex) {
      return row.id !== 1
    }
  }
}
</script>
