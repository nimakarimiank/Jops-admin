<template>
  <el-form ref="form" :model="form" :rules="rules" style="margin-top: 6px;" size="small" label-width="100px">
    <el-form-item label="appID" prop="appId">
      <el-input v-model="form.appId" style="width: 40%" />
      <span style="color: #C0C0C0;margin-left: 10px;">Application APPID, payee account is the Alipay account corresponding to APPID</span>
    </el-form-item>
    <el-form-item label="Merchant ID" prop="sysServiceProviderId">
      <el-input v-model="form.sysServiceProviderId" style="width: 40%;" />
      <span style="color: #C0C0C0;margin-left: 10px;">Merchant ID</span>
    </el-form-item>
    <el-form-item label="Private Key" prop="privateKey">
      <el-input v-model="form.privateKey" type="password" style="width: 40%;" />
      <span style="color: #C0C0C0;margin-left: 10px;">Merchant private key, your PKCS8 format RSA2 private key</span>
    </el-form-item>
    <el-form-item label="Alipay Public Key" prop="publicKey">
      <el-input v-model="form.publicKey" type="password" style="width: 40%;" />
      <span style="color: #C0C0C0;margin-left: 10px;">Alipay public key</span>
    </el-form-item>
    <el-form-item label="Return URL" prop="returnUrl">
      <el-input v-model="form.returnUrl" style="width: 40%;" />
      <span style="color: #C0C0C0;margin-left: 10px;">Return address after order completion</span>
    </el-form-item>
    <el-form-item label="Notify URL" prop="notifyUrl">
      <el-input v-model="form.notifyUrl" style="width: 40%;" />
      <span style="color: #C0C0C0;margin-left: 10px;">Payment result asynchronous notification address</span>
    </el-form-item>
    <el-form-item label="">
      <el-button :loading="loading" size="medium" type="primary" @click="doSubmit">Save Configuration</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { get, update } from '@/api/tools/alipay'
export default {
  name: 'Config',
  data() {
    return {
      loading: false,
      form: { appId: '', sysServiceProviderId: '', privateKey: '', publicKey: '', returnUrl: '', notifyUrl: '' },
      rules: {
        appId: [
          { required: true, message: 'Please enter appID', trigger: 'blur' }
        ],
        sysServiceProviderId: [
          { required: true, message: 'Please enter merchant ID', trigger: 'blur' }
        ],
        privateKey: [
          { required: true, message: 'Merchant private key cannot be empty', trigger: 'blur' }
        ],
        publicKey: [
          { required: true, message: 'Alipay public key cannot be empty', trigger: 'blur' }
        ],
        returnUrl: [
          { required: true, message: 'Return URL cannot be empty', trigger: 'blur' }
        ],
        notifyUrl: [
          { required: true, message: 'Notify URL cannot be empty', trigger: 'blur' }
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
