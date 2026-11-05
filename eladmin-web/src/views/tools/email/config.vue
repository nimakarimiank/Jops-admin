<template>
  <el-form ref="form" :model="form" :rules="rules" style="margin-top: 6px;" size="small" label-width="100px">
    <el-form-item label="Sender Email" prop="fromUser">
      <el-input v-model="form.fromUser" style="width: 40%" />
      <span style="color: #C0C0C0;margin-left: 10px;">Sender mailbox</span>
    </el-form-item>
    <el-form-item label="Sender User" prop="user">
      <el-input v-model="form.user" style="width: 40%;" />
      <span style="color: #C0C0C0;margin-left: 10px;">Sender usernamex</span>
    </el-form-item>
    <el-form-item label="Password" prop="pass">
      <el-input v-model="form.pass" type="password" style="width: 40%;" />
      <span style="color: #C0C0C0;margin-left: 10px;">email Password</span>
    </el-form-item>
    <el-form-item label="SMTP Host" prop="host">
      <el-input v-model="form.host" style="width: 40%;" />
      <span style="color: #C0C0C0;margin-left: 10px;">SMTP address</span>
    </el-form-item>
    <el-form-item label="SMTP Port" prop="port">
      <el-input v-model="form.port" style="width: 40%;" />
      <span style="color: #C0C0C0;margin-left: 10px;">SMTP port</span>
    </el-form-item>
    <el-form-item label="">
      <el-button :loading="loading" size="medium" type="primary" @click="doSubmit">Save Configuration</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { get, update } from '@/api/tools/email'
export default {
  name: 'Config',
  data() {
    return {
      loading: false, form: { id: 1, fromUser: '', user: '', pass: '', host: '', port: '', sslEnable: '' },
      rules: {
        fromUser: [
          { required: true, message: 'Please enter sender email', trigger: 'blur' },
          { type: 'email', message: 'Please enter a valid email address', trigger: 'blur' }
        ],
        user: [
          { required: true, message: 'Please enter sender username', trigger: 'blur' }
        ],
        pass: [
          { required: true, message: 'Password cannot be empty', trigger: 'blur' }
        ],
        host: [
          { required: true, message: 'SMTP host cannot be empty', trigger: 'blur' }
        ],
        port: [
          { required: true, message: 'SMTP port cannot be empty', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.init()
  },
  methods: {
    init() {
      get().then(res => {
        this.form = res
      })
    },
    doSubmit() {
      this.$refs['form'].validate((valid) => {
        if (valid) {
          this.loading = true
          update(this.form).then(res => {
            this.$notify({
              title: 'Modified successfully',
              type: 'success',
              duration: 2500
            })
            this.loading = false
          }).catch(err => {
            this.loading = false
            console.log(err.response.data.message)
          })
        } else {
          return false
        }
      })
    }
  }
}
</script>

<style scoped>

</style>
