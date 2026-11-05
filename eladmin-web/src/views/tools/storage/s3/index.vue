<template>
  <div class="app-container" style="padding: 8px;">
    <!-- Form Component -->
    <eForm ref="form" />
    <!-- Toolbar -->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- Search -->
        <el-input v-model="query.key" clearable size="small" placeholder="Search by file name" style="width: 200px;" class="filter-item" @keyup.enter.native="toQuery" />
        <date-range-picker v-model="query.createTime" class="date-item" />
        <rrOperation />
      </div>
      <crudOperation :permission="permission">
        <template slot="left">
          <!-- Upload -->
          <el-button class="filter-item" size="mini" type="primary" icon="el-icon-upload" @click="dialog = true">Upload</el-button>
        </template>
      </crudOperation>
      <!-- File Upload -->
      <el-dialog :visible.sync="dialog" :close-on-click-modal="false" append-to-body width="500px" @close="doSubmit">
        <el-upload
          :before-remove="handleBeforeRemove"
          :on-success="handleSuccess"
          :on-error="handleError"
          :file-list="fileList"
          :headers="headers"
          :action="s3UploadApi"
          class="upload-demo"
          multiple
        >
          <el-button size="small" type="primary">Click to Upload</el-button>
          <div slot="tip" style="display: block;" class="el-upload__tip">Please do not upload illegal files, file size should not exceed 15MB</div>
        </el-upload>
        <div slot="footer" class="dialog-footer">
          <el-button type="primary" @click="doSubmit">Confirm</el-button>
        </div>
      </el-dialog>
      <!-- Table Rendering -->
      <el-table ref="table" v-loading="crud.loading" :data="crud.data" style="width: 100%;" @selection-change="crud.selectionChangeHandler">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="fileName" :show-overflow-tooltip="true" label="File Name">
          <template slot-scope="scope">
            <a href="JavaScript:" class="el-link el-link--primary" target="_blank" type="primary" @click="download(scope.row.id)">{{ scope.row.fileName }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="fileRealName" width="350" label="Real File Name" />
        <el-table-column :show-overflow-tooltip="true" prop="fileType" label="File Type" @selection-change="crud.selectionChangeHandler" />
        <el-table-column prop="fileSize" label="File Size" />
        <el-table-column prop="updateTime" label="Created Date" />
      </el-table>
      <!-- Pagination Component -->
      <pagination />
    </div>
  </div>
</template>

<script>
import s3Storage from '@/api/tools/s3Storage'
import { mapGetters } from 'vuex'
import { getToken } from '@/utils/auth'
import CRUD, { presenter, header, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'

export default {
  components: { pagination, crudOperation, rrOperation, DateRangePicker },
  cruds() {
    return CRUD({ title: 'Object Storage', url: 'api/s3Storage', crudMethod: { ...s3Storage }})
  },
  mixins: [presenter(), header(), crud()],
  data() {
    return {
      permission: {
        del: ['admin', 'storage:del']
      },
      title: 'File', dialog: false,
      icon: 'el-icon-refresh',
      url: '', headers: { 'Authorization': getToken() },
      dialogImageUrl: '', dialogVisible: false, fileList: [], files: [], newWin: null
    }
  },
  computed: {
    ...mapGetters([
      's3UploadApi'
    ])
  },
  watch: {
    url(newVal, oldVal) {
      if (newVal && this.newWin) {
        this.newWin.sessionStorage.clear()
        this.newWin.location.href = newVal
        // Reset url and newWin after redirection
        this.url = ''
        this.newWin = null
      }
    }
  },
  created() {
    this.crud.optShow.add = false
    this.crud.optShow.edit = false
  },
  methods: {
    handleSuccess(response, file, fileList) {
      const uid = file.uid
      const id = response.id
      this.files.push({ uid, id })
    },
    handleBeforeRemove(file, fileList) {
      for (let i = 0; i < this.files.length; i++) {
        if (this.files[i].uid === file.uid) {
          s3Storage.del([this.files[i].id]).then(res => {})
          return true
        }
      }
    },
    handlePictureCardPreview(file) {
      this.dialogImageUrl = file.url
      this.dialogVisible = true
    },
    // Refresh list data
    doSubmit() {
      this.fileList = []
      this.dialogVisible = false
      this.dialogImageUrl = ''
      this.dialog = false
      this.crud.toQuery()
    },
    // Listen for upload failure
    handleError(e, file, fileList) {
      const msg = JSON.parse(e.message)
      this.crud.notify(msg.message, CRUD.NOTIFICATION_TYPE.ERROR)
    },
    // Download file
    download(id) {
      this.downloadLoading = true
      // Open an empty new window first, then send request
      this.newWin = window.open()
      s3Storage.download(id).then(res => {
        this.downloadLoading = false
        this.url = res.url
      }).catch(err => {
        this.downloadLoading = false
        console.log(err.response.data.message)
      })
    },
    // Synchronize data
    synchronize() {
      this.icon = 'el-icon-loading'
      s3Storage.sync().then(res => {
        this.icon = 'el-icon-refresh'
        this.$message({
          showClose: true,
          message: 'Data synchronized successfully',
          type: 'success',
          duration: 1500
        })
        this.crud.toQuery()
      }).catch(err => {
        this.icon = 'el-icon-refresh'
        console.log(err.response.data.message)
      })
    }
  }
}
</script>

<style scoped>

</style>
