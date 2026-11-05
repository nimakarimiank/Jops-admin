<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- Side department data -->
      <el-col :xs="9" :sm="6" :md="5" :lg="4" :xl="4">
        <div class="head-container">
          <el-input
            v-model="deptName"
            clearable
            size="small"
            placeholder="Enter department name to search"
            prefix-icon="el-icon-search"
            class="filter-item"
            @input="getDeptDatas"
          />
        </div>
        <el-tree
          :data="deptDatas"
          :load="getDeptDatas"
          :props="defaultProps"
          :expand-on-click-node="false"
          lazy
          @node-click="handleNodeClick"
        />
      </el-col>
      <!-- User data -->
      <el-col :xs="15" :sm="18" :md="19" :lg="20" :xl="20">
        <!-- Toolbar -->
        <div class="head-container">
          <div v-if="crud.props.searchToggle">
            <!-- Search -->
            <el-input
              v-model="query.blurry"
              clearable
              size="small"
              placeholder="Enter name or email to search"
              style="width: 200px;"
              class="filter-item"
              @keyup.enter.native="crud.toQuery"
            />
            <date-range-picker v-model="query.createTime" class="date-item" />
            <el-select
              v-model="query.enabled"
              clearable
              size="small"
              placeholder="Status"
              class="filter-item"
              style="width: 90px"
              @change="crud.toQuery"
            >
              <el-option
                v-for="item in enabledTypeOptions"
                :key="item.key"
                :label="item.display_name"
                :value="item.key"
              />
            </el-select>
            <rrOperation />
          </div>
          <crudOperation show="" :permission="permission">
            <el-button
              slot="right"
              v-permission="['admin','user:add']"
              :disabled="crud.selections.length === 0"
              class="filter-item"
              size="mini"
              type="primary"
              icon="el-icon-refresh-left"
              @click="resetPwd(crud.selections)"
            >Reset Password
            </el-button>
          </crudOperation>
        </div>
        <!-- Form rendering -->
        <el-dialog append-to-body :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="555px">
          <el-form ref="form" :inline="true" :model="form" :rules="rules" size="small" label-width="66px">
            <el-form-item label="Username" prop="username">
              <el-input v-model="form.username" @keydown.native="keydown($event)" />
            </el-form-item>
            <el-form-item label="Phone" prop="phone">
              <el-input v-model.number="form.phone" />
            </el-form-item>
            <el-form-item label="Nickname" prop="nickName">
              <el-input v-model="form.nickName" @keydown.native="keydown($event)" />
            </el-form-item>
            <el-form-item label="Email" prop="email">
              <el-input v-model="form.email" />
            </el-form-item>
            <el-form-item label="Department" prop="dept.id">
              <treeselect
                v-model="form.dept.id"
                :options="depts"
                :load-options="loadDepts"
                style="width: 173px"
                placeholder="Select department"
              />
            </el-form-item>
            <el-form-item label="Job" prop="jobDatas" class="is-required">
              <el-select
                v-model="jobDatas"
                style="width: 172px"
                multiple
                placeholder="Please select"
                @remove-tag="deleteTag"
                @change="changeJob"
              >
                <el-option
                  v-for="item in jobs"
                  :key="item.name"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="Gender">
              <el-radio-group v-model="form.gender" style="width: 178px">
                <el-radio label="Male">Male</el-radio>
                <el-radio label="Female">Female</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="Status">
              <el-radio-group v-model="form.enabled" :disabled="form.id === user.id">
                <el-radio
                  v-for="item in dict.user_status"
                  :key="item.id"
                  :label="item.value"
                >{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item style="margin-bottom: 0;" label="Role" prop="roleDatas" class="is-required">
              <el-select
                v-model="roleDatas"
                :disabled="form.id === user.id"
                style="width: 426px"
                multiple
                placeholder="Please select"
                @remove-tag="deleteTag"
                @change="changeRole"
              >
                <el-option
                  v-for="item in roles"
                  :key="item.name"
                  :disabled="level !== 1 && item.level <= level"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
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
          <el-table-column :show-overflow-tooltip="true" prop="username" label="Username" />
          <el-table-column :show-overflow-tooltip="true" prop="nickName" label="Nickname" />
          <el-table-column prop="gender" label="Gender" />
          <el-table-column :show-overflow-tooltip="true" prop="phone" width="100" label="Phone" />
          <el-table-column :show-overflow-tooltip="true" width="135" prop="email" label="Email" />
          <el-table-column :show-overflow-tooltip="true" prop="dept" label="Department">
            <template slot-scope="scope">
              <div>{{ scope.row.dept.name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="Status" align="center" prop="enabled">
            <template slot-scope="scope">
              <el-switch
                v-model="scope.row.enabled"
                :disabled="user.id === scope.row.id"
                active-color="#409EFF"
                inactive-color="#F56C6C"
                @change="changeEnabled(scope.row, scope.row.enabled)"
              />
            </template>
          </el-table-column>
          <el-table-column :show-overflow-tooltip="true" prop="createTime" width="135" label="Created Date" />
          <el-table-column
            v-if="checkPer(['admin','user:edit','user:del'])"
            label="Actions"
            width="115"
            align="center"
            fixed="right"
          >
            <template slot-scope="scope">
              <udOperation
                :data="scope.row"
                :permission="permission"
                :disabled-dle="scope.row.id === user.id"
              />
            </template>
          </el-table-column>
        </el-table>
        <!-- Pagination component -->
        <pagination />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import crudUser from '@/api/system/user'
import { isvalidPhone } from '@/utils/validate'
import { getDepts, getDeptSuperior } from '@/api/system/dept'
import { getAll, getLevel } from '@/api/system/role'
import { getAllJob } from '@/api/system/job'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'
import Treeselect from '@riophae/vue-treeselect'
import { mapGetters } from 'vuex'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import { LOAD_CHILDREN_OPTIONS } from '@riophae/vue-treeselect'
let userRoles = []
let userJobs = []
const defaultForm = { id: null, username: null, nickName: null, gender: 'Male', email: null, enabled: 'false', roles: [], jobs: [], dept: { id: null }, phone: null }
export default {
  name: 'User',
  components: { Treeselect, crudOperation, rrOperation, udOperation, pagination, DateRangePicker },
  cruds() {
    return CRUD({ title: 'User', url: 'api/users', crudMethod: { ...crudUser }})
  },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  // Data dictionary
  dicts: ['user_status'],
  data() {
    // Custom validation
    const validPhone = (rule, value, callback) => {
      if (!value) {
        callback(new Error('Please enter a phone number'))
      } else if (!isvalidPhone(value)) {
        callback(new Error('Please enter a valid 11-digit phone number'))
      } else {
        callback()
      }
    }
    return {
      height: document.documentElement.clientHeight - 180 + 'px;',
      deptName: '', depts: [], deptDatas: [], jobs: [], level: 3, roles: [],
      jobDatas: [], roleDatas: [], // Used for multiple selection
      defaultProps: { children: 'children', label: 'name', isLeaf: 'leaf' },
      permission: {
        add: ['admin', 'user:add'],
        edit: ['admin', 'user:edit'],
        del: ['admin', 'user:del']
      },
      enabledTypeOptions: [
        { key: 'true', display_name: 'Active' },
        { key: 'false', display_name: 'Locked' }
      ],
      rules: {
        username: [
          { required: true, message: 'Please enter a username', trigger: 'blur' },
          { min: 2, max: 20, message: 'Length must be between 2 and 20 characters', trigger: 'blur' }
        ],
        nickName: [
          { required: true, message: 'Please enter a nickname', trigger: 'blur' },
          { min: 2, max: 20, message: 'Length must be between 2 and 20 characters', trigger: 'blur' }
        ],
        email: [
          { required: true, message: 'Please enter an email address', trigger: 'blur' },
          { type: 'email', message: 'Please enter a valid email address', trigger: 'blur' }
        ],
        phone: [
          { required: true, trigger: 'blur', validator: validPhone }
        ],
        'dept.id': [
          { required: true, message: 'Department cannot be empty', trigger: 'blur' }
        ],
        jobDatas: [
          {
            validator: (rule, value, callback) => {
              value = this.jobDatas
              if (!value || value.length === 0) {
                callback(new Error('Please select at least one job'))
              } else {
                callback()
              }
            },
            trigger: 'change'
          }
        ],
        roleDatas: [
          {
            validator: (rule, value, callback) => {
              value = this.roleDatas
              if (!value || value.length === 0) {
                callback(new Error('Please select at least one role'))
              } else {
                callback()
              }
            },
            trigger: 'change'
          }
        ]
      }
    }
  },
  computed: {
    ...mapGetters([
      'user'
    ])
  },
  created() {
    this.crud.msg.add = 'Added successfully, default password: 123456'
  },
  mounted: function() {
    const that = this
    window.onresize = function temp() {
      that.height = document.documentElement.clientHeight - 180 + 'px;'
    }
  },
  methods: {
    // Disallow space input
    keydown(e) {
      if (e.keyCode === 32) {
        e.returnValue = false
      }
    },
    changeRole(value) {
      userRoles = []
      value.forEach(function(data, index) {
        const role = { id: data }
        userRoles.push(role)
      })
    },
    changeJob(value) {
      userJobs = []
      value.forEach(function(data, index) {
        const job = { id: data }
        userJobs.push(job)
      })
    },
    deleteTag(value) {
      userRoles.forEach(function(data, index) {
        if (data.id === value) {
          userRoles.splice(index, value)
        }
      })
    },
    // Operations performed before adding and editing
    [CRUD.HOOK.afterToCU](crud, form) {
      this.getRoles()
      if (form.id == null) {
        this.getDepts()
      } else {
        this.getSupDepts(form.dept.id)
      }
      this.getRoleLevel()
      this.getJobs()
      form.enabled = form.enabled.toString()
    },
    // Clear multi-select values before adding
    [CRUD.HOOK.beforeToAdd]() {
      this.jobDatas = []
      this.roleDatas = []
    },
    // Initialize roles and jobs when editing
    [CRUD.HOOK.beforeToEdit](crud, form) {
      this.getJobs(this.form.dept.id)
      this.jobDatas = []
      this.roleDatas = []
      userRoles = []
      userJobs = []
      const _this = this
      form.roles.forEach(function(role, index) {
        _this.roleDatas.push(role.id)
        const rol = { id: role.id }
        userRoles.push(rol)
      })
      form.jobs.forEach(function(job, index) {
        _this.jobDatas.push(job.id)
        const data = { id: job.id }
        userJobs.push(data)
      })
    },
    // Operations performed before submit
    [CRUD.HOOK.afterValidateCU](crud) {
      crud.form.roles = userRoles
      crud.form.jobs = userJobs
      console.log(this.jobDatas)
      return true
    },
    // Get left department data
    getDeptDatas(node, resolve) {
      const params = {}
      if (typeof node !== 'object') {
        if (node) {
          params['name'] = node
        }
      } else if (node.level !== 0) {
        params['pid'] = node.data.id
      }
      setTimeout(() => {
        getDepts(params).then(res => {
          if (resolve) {
            resolve(res.content)
          } else {
            this.deptDatas = res.content
          }
        })
      }, 100)
    },
    getDepts() {
      getDepts({ enabled: true }).then(res => {
        this.depts = res.content.map(function(obj) {
          if (obj.hasChildren) {
            obj.children = null
          }
          return obj
        })
      })
    },
    getSupDepts(deptId) {
      getDeptSuperior(deptId).then(res => {
        const date = res.content
        this.buildDepts(date)
        this.depts = date
      })
    },
    buildDepts(depts) {
      depts.forEach(data => {
        if (data.children) {
          this.buildDepts(data.children)
        }
        if (data.hasChildren && !data.children) {
          data.children = null
        }
      })
    },
    // Get department data in modal
    loadDepts({ action, parentNode, callback }) {
      if (action === LOAD_CHILDREN_OPTIONS) {
        getDepts({ enabled: true, pid: parentNode.id }).then(res => {
          parentNode.children = res.content.map(function(obj) {
            if (obj.hasChildren) {
              obj.children = null
            }
            return obj
          })
          setTimeout(() => {
            callback()
          }, 200)
        })
      }
    },
    // Switch department
    handleNodeClick(data) {
      if (data.pid === 0) {
        this.query.deptId = null
      } else {
        this.query.deptId = data.id
      }
      this.crud.toQuery()
    },
    // Change status
    changeEnabled(data, val) {
      this.$confirm('This action will "' + this.dict.label.user_status[val] + '" ' + data.username + ', continue?', 'Hint', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        crudUser.edit(data).then(res => {
          this.crud.notify(this.dict.label.user_status[val] + ' succeeded', CRUD.NOTIFICATION_TYPE.SUCCESS)
        }).catch(() => {
          data.enabled = !data.enabled
        })
      }).catch(() => {
        data.enabled = !data.enabled
      })
    },
    // Get role data in modal
    getRoles() {
      getAll().then(res => {
        this.roles = res
      }).catch(() => { })
    },
    // Get job data in modal
    getJobs() {
      getAllJob().then(res => {
        this.jobs = res.content
      }).catch(() => { })
    },
    // Get role level
    getRoleLevel() {
      getLevel().then(res => {
        this.level = res.level
      }).catch(() => { })
    },
    checkboxT(row, rowIndex) {
      return row.id !== this.user.id
    },
    resetPwd(datas) {
      this.$confirm(`You selected ${datas.length} users, confirm reset password for these users?`, 'Hint', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        const ids = []
        datas.forEach(val => {
          ids.push(val.id)
        })
        console.log(ids)
        crudUser.resetPwd(ids).then(() => {
          this.crud.notify('Reset successfully, new user password: 123456', CRUD.NOTIFICATION_TYPE.SUCCESS)
        }).catch(() => {})
      }).catch(() => {
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
  ::v-deep .vue-treeselect__control,::v-deep .vue-treeselect__placeholder,::v-deep .vue-treeselect__single-value {
    height: 30px;
    line-height: 30px;
  }
</style>
