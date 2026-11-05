<template>
  <el-dialog append-to-body :close-on-click-modal="false" :visible.sync="dialog" title="Execute Script" width="400px">
    <el-form ref="form" :rules="rules" size="small">
      <el-upload
        :action="databaseUploadApi"
        :data="databaseInfo"
        :headers="headers"
        :on-success="handleSuccess"
        :on-error="handleError"
        class="upload-demo"
        drag
      >
        <i class="el-icon-upload" />
        <div class="el-upload__text">
          Drop files here, or
          <em>click to upload</em>
        </div>
        <div slot="tip" class="el-upload__tip">After upload, the system will automatically execute the SQL script</div>
      </el-upload>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="cancel">Close</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { mapGetters } from 'vuex'
import { getToken } from '@/utils/auth'
export default {
  props: {
    databaseInfo: {
      type: Object,
      default() {
        return {}
      }
    }
  },
  data() {
    return {
      loading: false,
      dialog: false,
      headers: {
        Authorization: getToken()
      },
      rules: {}
    }
  },
  computed: {
    ...mapGetters(['databaseUploadApi'])
  },
  mounted() {
  },
  methods: {
    cancel() {
      this.dialog = false
    },
    handleSuccess(response, file, fileList) {
      if (response === 'success') {
        this.$notify({
          title: 'Executed successfully',
          type: 'success',
          duration: 2500
        })
      } else {
        this.$notify({
          title: response,
          type: 'error',
          duration: 0
        })
      }
    },
    handleError(e, file, fileList) {
      const msg = JSON.parse(e.message)
      this.$notify({
        title: msg.message,
        type: 'error',
        duration: 0
      })
    }
  }
}
</script>

<style scoped>
</style>
