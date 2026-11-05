<template>
  <div class="app-container">
    <el-alert :closable="false" title="Level 3 Menu 1" type="success" />
    <el-form label-width="170px" style="margin-top: 20px">
      <el-form-item label="Level 3 Menu Cache Test Area">
        <el-input v-model="input" placeholder="Please enter content" style="width: 360px;" />
      </el-form-item>
    </el-form>
    <div>
      <blockquote class="my-blockquote"> Level 3 Menu Cache Configuration Guide</blockquote>
      <pre class="my-code">
 1. Update frontend and backend code to the latest version, or modify according to commit records, click to view -> <a href="https://gitee.com/elunez/eladmin/commit/43d1a63577f9d5347924355708429a2d210e29f7" target="_blank">Commit (1)</a>, <a href="https://gitee.com/elunez/eladmin/commit/46393875148fcca5eaa327d4073f72edb3752f5c" target="_blank">Commit (2)</a>, <a href="https://gitee.com/elunez/eladmin-web/commit/c93c99d8921abbb2c52afc806635f5ca08d6bda8" target="_blank">Commit (3)</a>
 2. Set Menu Type of Level 2 Menu to Directory level, and clear the original Component Path
 3. Set Menu Cache of Level 3 Menu to Yes, and fill in the Component Name correctly
 4. For specific settings, refer to Multi-level Menu configuration in Menu Management to modify accordingly
 </pre>
      <blockquote class="my-blockquote">More Help</blockquote>
      <pre class="my-code">QQ Groups: Group 1: 891137268, Group 2: 947578238, Group 3: 659622532</pre>
    </div>
  </div>
</template>
<script>
export default {
  name: 'Test',
  data() {
    return {
      input: ''
    }
  }
}
</script>
<style scoped>
  .my-code a{
    color:#009688;
  }
</style>
