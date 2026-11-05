<template>
  <div class="app-container">
    <el-row :gutter="15">
      <el-col style="margin-bottom: 10px">
        <el-card class="box-card" shadow="never">
          <div slot="header" class="clearfix">
            <span class="role-span">Field Configuration: {{ tableName }}</span>
            <el-button
              :loading="genLoading"
              icon="el-icon-s-promotion"
              size="mini"
              style="float: right; padding: 6px 9px;"
              type="success"
              @click="toGen"
            >Save & Generate</el-button>
            <el-button
              :loading="columnLoading"
              icon="el-icon-check"
              size="mini"
              style="float: right; padding: 6px 9px;margin-right: 9px"
              type="primary"
              @click="saveColumnConfig"
            >Save</el-button>
            <el-tooltip class="item" effect="dark" content="Use this feature when table columns in database change" placement="top-start">
              <el-button
                :loading="syncLoading"
                icon="el-icon-refresh"
                size="mini"
                style="float: right; padding: 6px 9px;"
                type="info"
                @click="sync"
              >Sync</el-button>
            </el-tooltip>
          </div>
          <el-form size="small" label-width="90px">
            <el-table v-loading="loading" :data="data" :max-height="tableHeight" size="small" style="width: 100%;margin-bottom: 15px">
              <el-table-column prop="columnName" width="110px" label="Field Name" />
              <el-table-column prop="columnType" width="110px" label="Field Type" />
              <el-table-column prop="remark" label="Field Description">
                <template slot-scope="scope">
                  <el-input v-model="data[scope.$index].remark" size="mini" class="edit-input" />
                </template>
              </el-table-column>
              <el-table-column align="center" label="Required" width="80px">
                <template slot-scope="scope">
                  <el-checkbox v-model="data[scope.$index].notNull" />
                </template>
              </el-table-column>
              <el-table-column align="center" label="List" width="80px">
                <template slot-scope="scope">
                  <el-checkbox v-model="data[scope.$index].listShow" />
                </template>
              </el-table-column>
              <el-table-column align="center" label="Form" width="80px">
                <template slot-scope="scope">
                  <el-checkbox v-model="data[scope.$index].formShow" />
                </template>
              </el-table-column>
              <el-table-column label="Form Type">
                <template slot-scope="scope">
                  <el-select v-model="data[scope.$index].formType" filterable class="edit-input" clearable size="mini" placeholder="Please select">
                    <el-option
                      label="Text Input"
                      value="Input"
                    />
                    <el-option
                      label="Textarea"
                      value="Textarea"
                    />
                    <el-option
                      label="Radio"
                      value="Radio"
                    />
                    <el-option
                      label="Select"
                      value="Select"
                    />
                    <el-option
                      label="Date"
                      value="Date"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="Query Type">
                <template slot-scope="scope">
                  <el-select v-model="data[scope.$index].queryType" filterable class="edit-input" clearable size="mini" placeholder="Please select">
                    <el-option
                      label="="
                      value="="
                    />
                    <el-option
                      label="!="
                      value="!="
                    />
                    <el-option
                      label=">="
                      value=">="
                    />
                    <el-option
                      label="<="
                      value="<="
                    />
                    <el-option
                      label="Like"
                      value="Like"
                    />
                    <el-option
                      label="NotNull"
                      value="NotNull"
                    />
                    <el-option
                      label="BetWeen"
                      value="BetWeen"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="Associated Dict">
                <template slot-scope="scope">
                  <el-select v-model="data[scope.$index].dictName" filterable class="edit-input" clearable size="mini" placeholder="Please select">
                    <el-option v-for="item in dicts" :key="item.id" :label="item.remark === '' ? item.name : item.remark" :value="item.name" />
                  </el-select>
                </template>
              </el-table-column>
            </el-table>
          </el-form>
        </el-card>
      </el-col>
      <el-col>
        <el-card class="box-card" shadow="never">
          <div slot="header" class="clearfix">
            <span class="role-span">Generation Configuration</span>
            <el-button
              :loading="configLoading"
              icon="el-icon-check"
              size="mini"
              style="float: right; padding: 6px 9px"
              type="primary"
              @click="doSubmit"
            >Save</el-button>
          </div>
          <el-form ref="form" :model="form" :rules="rules" size="small" label-width="78px">
            <el-form-item label="Author Name" prop="author">
              <el-input v-model="form.author" style="width: 40%" />
              <span style="color: #C0C0C0;margin-left: 10px;">Author name above class</span>
            </el-form-item>
            <el-form-item label="Module Name" prop="moduleName">
              <el-input v-model="form.moduleName" style="width: 40%" />
              <span style="color: #C0C0C0;margin-left: 10px;">Module name, please select an existing module in project</span>
            </el-form-item>
            <el-form-item label="Package Name" prop="pack">
              <el-input v-model="form.pack" style="width: 40%" />
              <span style="color: #C0C0C0;margin-left: 10px;">Package name, e.g.: me.zhengjie</span>
            </el-form-item>
            <el-form-item label="API Name" prop="apiAlias">
              <el-input v-model="form.apiAlias" style="width: 40%" />
              <span style="color: #C0C0C0;margin-left: 10px;">API name, used in controller and API documentation</span>
            </el-form-item>
            <el-form-item label="Frontend Path" prop="path">
              <el-input v-model="form.path" style="width: 40%" />
              <span style="color: #C0C0C0;margin-left: 10px;">Enter directory under views folder, will be created if not exists</span>
            </el-form-item>
            <!--            <el-form-item label="API Directory">-->
            <!--              <el-input v-model="form.apiPath" style="width: 40%" />-->
            <!--              <span style="color: #C0C0C0;margin-left: 10px;">API path [src/api], auto-generated if empty</span>-->
            <!--            </el-form-item>-->
            <el-form-item label="Remove Prefix" prop="prefix">
              <el-input v-model="form.prefix" placeholder="Do not remove table prefix by default" style="width: 40%" />
              <span style="color: #C0C0C0;margin-left: 10px;">Table prefix is not removed by default, customizable</span>
            </el-form-item>
            <el-form-item label="Overwrite" prop="cover">
              <el-radio-group v-model="form.cover" size="mini" style="width: 40%">
                <el-radio-button label="true">Yes</el-radio-button>
                <el-radio-button label="false">No</el-radio-button>
              </el-radio-group>
              <span style="color: #C0C0C0;margin-left: 10px;">Beware of accidental overwrite, choose carefully</span>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import crud from '@/mixins/crud'
import { update, get } from '@/api/generator/genConfig'
import { save, sync, generator } from '@/api/generator/generator'
import { getDicts } from '@/api/system/dict'
export default {
  name: 'GeneratorConfig',
  components: {},
  mixins: [crud],
  data() {
    return {
      activeName: 'first', tableName: '', tableHeight: 550, columnLoading: false, configLoading: false, dicts: [], syncLoading: false, genLoading: false,
      form: { id: null, tableName: '', author: '', pack: '', path: '', moduleName: '', cover: 'false', apiPath: '', prefix: '', apiAlias: null },
      rules: {
        author: [
          { required: true, message: 'Author cannot be empty', trigger: 'blur' }
        ],
        pack: [
          { required: true, message: 'Package path cannot be empty', trigger: 'blur' }
        ],
        moduleName: [
          { required: true, message: 'Module name cannot be empty', trigger: 'blur' }
        ],
        path: [
          { required: true, message: 'Frontend path cannot be empty', trigger: 'blur' }
        ],
        apiAlias: [
          { required: true, message: 'API name cannot be empty', trigger: 'blur' }
        ],
        cover: [
          { required: true, message: 'Cannot be empty', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.tableHeight = document.documentElement.clientHeight - 385
    this.tableName = this.$route.params.tableName
    this.$nextTick(() => {
      this.init()
      get(this.tableName).then(data => {
        this.form = data
        this.form.cover = this.form.cover.toString()
      })
      getDicts().then(data => {
        this.dicts = data
      })
    })
  },
  methods: {
    beforeInit() {
      this.url = 'api/generator/columns'
      const tableName = this.tableName
      this.params = { tableName }
      return true
    },
    saveColumnConfig() {
      this.columnLoading = true
      save(this.data).then(() => {
        this.notify('Saved successfully', 'success')
        this.columnLoading = false
      }).catch(err => {
        this.columnLoading = false
        console.log(err.response.data.message)
      })
    },
    doSubmit() {
      this.$refs['form'].validate((valid) => {
        if (valid) {
          this.configLoading = true
          update(this.form).then(res => {
            this.notify('Saved successfully', 'success')
            this.form = res
            this.form.cover = this.form.cover.toString()
            this.configLoading = false
          }).catch(err => {
            this.configLoading = false
            console.log(err.response.data.message)
          })
        }
      })
    },
    sync() {
      this.syncLoading = true
      sync([this.tableName]).then(() => {
        this.init()
        this.notify('Synced successfully', 'success')
        this.syncLoading = false
      }).then(() => {
        this.syncLoading = false
      })
    },
    toGen() {
      this.genLoading = true
      save(this.data).then(res => {
        this.notify('Saved successfully', 'success')
        // Generate code
        generator(this.tableName, 0).then(data => {
          this.genLoading = false
          this.notify('Generated successfully', 'success')
        }).catch(err => {
          this.genLoading = false
          console.log(err.response.data.message)
        })
      }).catch(err => {
        this.genLoading = false
        console.log(err.response.data.message)
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss">
  .edit-input {
    .el-input__inner {
      border: 1px solid #e5e6e7;
    }
  }
</style>

<style scoped>
 ::v-deep .input-with-select .el-input-group__prepend {
    background-color: #fff;
  }
</style>
