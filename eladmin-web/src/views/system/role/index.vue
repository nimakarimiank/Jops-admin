<template>
  <div class="app-container">
    <!-- Toolbar -->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- Search -->
        <el-input v-model="query.blurry" size="small" clearable placeholder="Enter name or description to search" style="width: 200px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker v-model="query.createTime" class="date-item" />
        <rrOperation />
      </div>
      <crudOperation :permission="permission" />
    </div>
    <!-- Form rendering -->
    <el-dialog append-to-body :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="520px">
      <el-form ref="form" :inline="true" :model="form" :rules="rules" size="small" label-width="80px">
        <el-form-item label="Role Name" prop="name">
          <el-input v-model="form.name" style="width: 380px;" />
        </el-form-item>
        <el-form-item label="Role Level" prop="level">
          <el-input-number v-model.number="form.level" :min="1" controls-position="right" style="width: 145px;" />
        </el-form-item>
        <el-form-item label="Data Scope" prop="dataScope">
          <el-select v-model="form.dataScope" style="width: 140px" placeholder="Select data scope" @change="changeScope">
            <el-option
              v-for="item in dateScopes"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.dataScope === 'Custom'" label="Data Scope" prop="depts">
          <treeselect
            v-model="deptDatas"
            :load-options="loadDepts"
            :options="depts"
            :flat="true"
            :auto-select-descendants="true"
            :default-expand-level="1"
            multiple
            style="width: 380px"
            placeholder="Please select"
          />
        </el-form-item>
        <el-form-item label="Description" prop="description">
          <el-input v-model="form.description" style="width: 380px;" rows="5" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="text" @click="crud.cancelCU">Cancel</el-button>
        <el-button :loading="crud.status.cu === 2" type="primary" @click="crud.submitCU">Confirm</el-button>
      </div>
    </el-dialog>
    <el-row :gutter="15">
      <!-- Role management -->
      <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="17" style="margin-bottom: 10px">
        <el-card class="box-card" shadow="never">
          <div slot="header" class="clearfix">
            <span class="role-span">Role List</span>
          </div>
          <el-table ref="table" v-loading="crud.loading" highlight-current-row style="width: 100%;" :data="crud.data" @selection-change="crud.selectionChangeHandler" @current-change="handleCurrentChange">
            <el-table-column :selectable="checkboxT" type="selection" width="55" />
            <el-table-column prop="name" label="Name" />
            <el-table-column prop="dataScope" label="Data Scope" />
            <el-table-column prop="level" label="Role Level" />
            <el-table-column :show-overflow-tooltip="true" prop="description" label="Description">
              <template slot-scope="scope">
                {{ scope.row.description == null ? '-' : scope.row.description }}
              </template>
            </el-table-column>
            <el-table-column :show-overflow-tooltip="true" width="135px" prop="createTime" label="Created Date" />
            <el-table-column v-if="checkPer(['admin','roles:edit','roles:del'])" label="Actions" width="130px" align="center" fixed="right">
              <template slot-scope="scope">
                <udOperation
                  v-if="scope.row.level >= level"
                  :data="scope.row"
                  :permission="permission"
                />
              </template>
            </el-table-column>
          </el-table>
          <!-- Pagination component -->
          <pagination />
        </el-card>
      </el-col>
      <!-- Menu authorization -->
      <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="7">
        <el-card class="box-card" shadow="never">
          <div slot="header" class="clearfix">
            <el-tooltip class="item" effect="dark" content="Select a role to assign menus" placement="top">
              <span class="role-span">Menu Assignment</span>
            </el-tooltip>
            <el-button
              v-permission="['admin','roles:edit']"
              :disabled="!showButton"
              :loading="menuLoading"
              icon="el-icon-check"
              size="mini"
              style="float: right; padding: 6px 9px"
              type="primary"
              @click="saveMenu"
            >Save</el-button>
          </div>
          <el-tree
            ref="menu"
            lazy
            :data="menus"
            :default-checked-keys="menuIds"
            :load="getMenuDatas"
            :props="defaultProps"
            check-strictly
            accordion
            show-checkbox
            node-key="id"
            @check="menuChange"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import crudRoles from '@/api/system/role'
import { getDepts, getDeptSuperior } from '@/api/system/dept'
import { getMenusTree, getChild } from '@/api/system/menu'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import { LOAD_CHILDREN_OPTIONS } from '@riophae/vue-treeselect'
import DateRangePicker from '@/components/DateRangePicker'

const defaultForm = { id: null, name: null, depts: [], description: null, dataScope: 'All', level: 3 }
export default {
  name: 'Role',
  components: { Treeselect, pagination, crudOperation, rrOperation, udOperation, DateRangePicker },
  cruds() {
    return CRUD({ title: 'Role', url: 'api/roles', crudMethod: { ...crudRoles }})
  },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  data() {
    return {
      defaultProps: { children: 'children', label: 'label', isLeaf: 'leaf' },
      dateScopes: ['All', 'This level', 'Custom'], level: 3,
      currentId: 0, menuLoading: false, showButton: false,
      menus: [], menuIds: [], depts: [], deptDatas: [], // Used for multiple selection
      permission: {
        add: ['admin', 'roles:add'],
        edit: ['admin', 'roles:edit'],
        del: ['admin', 'roles:del']
      },
      rules: {
        name: [
          { required: true, message: 'Please enter a name', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    crudRoles.getLevel().then(data => {
      this.level = data.level
    })
  },
  methods: {
    getMenuDatas(node, resolve) {
      setTimeout(() => {
        getMenusTree(node.data.id ? node.data.id : 0).then(res => {
          resolve(res)
        })
      }, 100)
    },
    [CRUD.HOOK.afterRefresh]() {
      this.$refs.menu.setCheckedKeys([])
    },
    // Initialize department info before adding
    [CRUD.HOOK.beforeToAdd](crud, form) {
      this.deptDatas = []
      form.menus = null
    },
    // Initialize custom data permission department info before editing
    [CRUD.HOOK.beforeToEdit](crud, form) {
      this.deptDatas = []
      if (form.dataScope === 'Custom') {
        this.getSupDepts(form.depts)
      }
      const _this = this
      form.depts.forEach(function(dept) {
        _this.deptDatas.push(dept.id)
      })
      // Clear role menus to prevent log database data from being too long
      form.menus = null
    },
    // Operations performed before submit
    [CRUD.HOOK.afterValidateCU](crud) {
      if (crud.form.dataScope === 'Custom' && this.deptDatas.length === 0) {
        this.$message({
          message: 'Custom data permissions cannot be empty',
          type: 'warning'
        })
        return false
      } else if (crud.form.dataScope === 'Custom') {
        const depts = []
        this.deptDatas.forEach(function(data) {
          const dept = { id: data }
          depts.push(dept)
        })
        crud.form.depts = depts
      } else {
        crud.form.depts = []
      }
      return true
    },
    // Trigger single select
    handleCurrentChange(val) {
      if (val) {
        const _this = this
        // Clear menu selection
        this.$refs.menu.setCheckedKeys([])
        // Save current role ID
        this.currentId = val.id
        // Initialize default selected keys
        this.menuIds = []
        val.menus.forEach(function(data) {
          _this.menuIds.push(data.id)
        })
        this.showButton = true
      }
    },
    menuChange(menu) {
      // Get all child nodes of this node, ID includes self
      getChild(menu.id).then(childIds => {
        // Check if exists in menuIds, remove if exists, otherwise add
        if (this.menuIds.indexOf(menu.id) !== -1) {
          for (let i = 0; i < childIds.length; i++) {
            const index = this.menuIds.indexOf(childIds[i])
            if (index !== -1) {
              this.menuIds.splice(index, 1)
            }
          }
        } else {
          for (let i = 0; i < childIds.length; i++) {
            const index = this.menuIds.indexOf(childIds[i])
            if (index === -1) {
              this.menuIds.push(childIds[i])
            }
          }
        }
        this.$refs.menu.setCheckedKeys(this.menuIds)
      })
    },
    // Save menus
    saveMenu() {
      this.menuLoading = true
      const role = { id: this.currentId, menus: [] }
      // Get selected key values
      this.menuIds.forEach(function(id) {
        const menu = { id: id }
        role.menus.push(menu)
      })
      crudRoles.editMenu(role).then(() => {
        this.crud.notify('Saved successfully', CRUD.NOTIFICATION_TYPE.SUCCESS)
        this.menuLoading = false
        this.update()
      }).catch(err => {
        this.menuLoading = false
        console.log(err.response.data.message)
      })
    },
    // Change data
    update() {
      // Refresh table data without full reload
      crudRoles.get(this.currentId).then(res => {
        for (let i = 0; i < this.crud.data.length; i++) {
          if (res.id === this.crud.data[i].id) {
            this.crud.data[i] = res
            break
          }
        }
      })
    },
    // Get department data
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
    getSupDepts(depts) {
      const ids = []
      depts.forEach(dept => {
        ids.push(dept.id)
      })
      getDeptSuperior(ids).then(res => {
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
    // Fetch department data if data scope is custom
    changeScope() {
      if (this.form.dataScope === 'Custom') {
        this.getDepts()
      }
    },
    checkboxT(row) {
      return row.level >= this.level
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss">
  .role-span {
    font-weight: bold;color: #303133;
    font-size: 15px;
  }
</style>

<style rel="stylesheet/scss" lang="scss" scoped>
 ::v-deep .el-input-number .el-input__inner {
    text-align: left;
  }
 ::v-deep .vue-treeselect__multi-value{
    margin-bottom: 0;
  }
 ::v-deep .vue-treeselect__multi-value-item{
    border: 0;
    padding: 0;
  }
</style>
