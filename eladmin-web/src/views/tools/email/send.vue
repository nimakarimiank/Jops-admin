<template>
  <div>
    <el-form ref="form" :model="form" :rules="rules" style="margin-top: 6px;" size="small" label-width="100px">
      <el-form-item label="Subject" prop="subject">
        <el-input v-model="form.subject" style="width: 646px" placeholder="Please enter email subject, subject cannot be empty" />
      </el-form-item>
      <el-form-item label="Recipient" prop="tos">
        <el-input v-model="form.tos" style="width: 646px" placeholder="Please enter recipient address, separate multiple addresses with comma (,)" />
      </el-form-item>
      <div ref="editor" class="editor" />
      <el-button :loading="loading" style="margin-left:1.6%;margin-bottom: 30px" size="medium" type="primary" @click="doSubmit">Send Email</el-button>
    </el-form>
  </div>
</template>

<script>
import { send } from '@/api/tools/email'
import { upload } from '@/utils/upload'
import { mapGetters } from 'vuex'
import E from 'wangeditor'
export default {
  name: 'Index',
  data() {
    return {
      loading: false, form: { subject: '', tos: '', content: '' },
      rules: {
        subject: [
          { required: true, message: 'Subject cannot be empty', trigger: 'blur' }
        ],
        tos: [
          { required: true, message: 'Recipient cannot be empty', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters([
      'imagesUploadApi',
      'baseApi'
    ])
  },
  mounted() {
    const _this = this
    var editor = new E(this.$refs.editor)
    // Custom menu configuration
    editor.config.zIndex = 10
    // File upload
    editor.config.customUploadImg = function(files, insert) {
      // files is the list of selected files from the input
      // insert is the method to insert image url into editor after retrieval
      files.forEach(image => {
        upload(_this.imagesUploadApi, image).then(res => {
          const data = res.data
          const url = _this.baseApi + '/file/' + data.type + '/' + data.realName
          insert(url)
        })
      })
    }
    editor.config.onchange = (html) => {
      this.form.content = html
    }
    editor.create()
  },
  methods: {
    doSubmit() {
      this.$refs['form'].validate((valid) => {
        if (valid) {
          this.loading = true
          send(this.form).then(res => {
            this.$notify({
              title: 'Sent successfully',
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
  .editor{
    text-align:left;
    margin: 20px;
    width: 730px;
  }
 ::v-deep .w-e-text-container {
    height: 360px !important;
  }
</style>
