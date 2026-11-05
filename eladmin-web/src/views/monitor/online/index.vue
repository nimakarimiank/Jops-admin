<template>
  <div class="app-container">
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <el-input v-model="query.username" clearable size="small" placeholder="Enter username to search" style="width: 200px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <rrOperation />
      </div>
      <crudOperation>
        <el-button
          slot="left"
          class="filter-item"
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :loading="delLoading"
          :disabled="crud.selections.length === 0"
          @click="doDelete(crud.selections)"
        >
          Force Logout
        </el-button>
      </crudOperation>
    </div>
    <!-- Table rendering -->
    <el-table ref="table" v-loading="crud.loading" :data="crud.data" style="width: 100%;" @selection-change="crud.selectionChangeHandler">
      <el-table-column type="selection" width="55" />

      <el-table-column :show-overflow-tooltip="true" prop="uid" label="Session ID" />
      <el-table-column prop="userName" label="Username" />
      <el-table-column prop="nickName" label="User Nickname" />
      <el-table-column prop="dept" label="Department" />
      <el-table-column prop="ip" label="Login IP" />
      <el-table-column :show-overflow-tooltip="true" prop="address" label="Login Location" />
      <el-table-column prop="browser" label="Browser" />
      <el-table-column prop="loginTime" label="Login Time" />
      <el-table-column label="Actions" width="70px" fixed="right">
        <template slot-scope="scope">
          <el-popover
            :ref="scope.$index"
            v-permission="['admin']"
            placement="top"
            width="180"
          >
            <p>Are you sure you want to force logout this user?</p>
            <div style="text-align: right; margin: 0">
              <el-button size="mini" type="text" @click="$refs[scope.$index].doClose()">Cancel</el-button>
              <el-button :loading="delLoading" type="primary" size="mini" @click="delMethod(scope.row.key, scope.$index)">Confirm</el-button>
            </div>
            <el-button slot="reference" size="mini" type="text">Force Logout</el-button>
          </el-popover>
        </template>
      </el-table-column>
    </el-table>
    <!-- Pagination component -->
    <pagination />
  </div>
</template>

<script>
import { del } from '@/api/monitor/online'
import CRUD, { presenter, header, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import pagination from '@crud/Pagination'

export default {
  name: 'OnlineUser',
  components: { pagination, crudOperation, rrOperation },
  cruds() {
    return CRUD({ url: 'auth/online', title: 'Online Users' })
  },
  mixins: [presenter(), header(), crud()],
  data() {
    return {
      delLoading: false,
      permission: {}
    }
  },
  created() {
    this.crud.msg.del = 'Force logout succeeded!'
    this.crud.optShow = {
      add: false,
      edit: false,
      del: false,
      download: true
    }
  },
  methods: {
    doDelete(datas) {
      this.$confirm(`Confirm force logout of selected ${datas.length} users?`, 'Hint', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.delMethod(datas)
      }).catch(() => {})
    },
    // Kick out user
    delMethod(key, index) {
      const ids = []
      if (key instanceof Array) {
        key.forEach(val => {
          ids.push(val.key)
        })
      } else ids.push(key)
      this.delLoading = true
      del(ids).then(() => {
        this.delLoading = false
        if (this.$refs[index]) {
          this.$refs[index].doClose()
        }
        this.crud.dleChangePage(1)
        this.crud.delSuccessNotify()
        this.crud.toQuery()
      }).catch(() => {
        this.delLoading = false
        if (this.$refs[index]) {
          this.$refs[index].doClose()
        }
      })
    }
  }
}
</script>
