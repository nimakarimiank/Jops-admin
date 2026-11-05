<template>
  <el-tabs v-model="activeName" style="padding-left: 8px;" @tab-click="tabClick">
    <el-tab-pane label="Local Storage" name="first">
      <Local ref="local" />
    </el-tab-pane>
    <el-tab-pane label="Object Storage" name="second">
      <template slot="label">
        Object Storage
        <el-tooltip content="Object storage compatible with Amazon S3 protocol, test is not provided, please run test yourself" placement="top">
          <i class="el-icon-question" style="margin-left: 8px; cursor: pointer;" />
        </el-tooltip>
      </template>
      <S3 ref="s3" />
    </el-tab-pane>
  </el-tabs>
</template>

<script>
import S3 from './s3/index'
import Local from './local/index'
export default {
  name: 'Storage',
  components: { S3, Local },
  data() {
    return {
      activeName: 'first'
    }
  },
  methods: {
    tabClick(name) {
      if (this.activeName === 'first') {
        this.$refs.local.crud.toQuery()
      } else {
        this.$refs.s3.crud.toQuery()
      }
    }
  }
}
</script>

<style scoped>
/* Custom question mark icon style */
.el-icon-question {
  font-size: 16px;
  color: #409EFF;
}
</style>
