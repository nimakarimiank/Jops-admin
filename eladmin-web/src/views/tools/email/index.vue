<template>
  <el-tabs v-model="activeName" style="padding-left: 8px;">
    <el-tab-pane label="Email Config" name="first">
      <Config />
    </el-tab-pane>
    <el-tab-pane label="Send Email" name="second">
      <Send />
    </el-tab-pane>
    <el-tab-pane label="Instructions" name="third">
      <div>
        <blockquote class="my-blockquote"> Mail Server Configuration</blockquote>
        <pre class="my-code">
 # SMTP address of the mail server, optional, default is smtp
 # SMTP port of the mail server, optional, default is 465 or 25
 # Sender (must be correct, otherwise sending will fail)
 # Username, default is the prefix of the sender email
 # Password (Note: some mailboxes require a separate password for SMTP service, such as QQ and 163, etc.)
 # Whether to enable SSL, enabled by default</pre>
        <blockquote class="my-blockquote">More Help</blockquote>
        <pre class="my-code">For more help, please refer to the documentation: <a style="color:#009688" href="http://hutool.mydoc.io/#text_319499" target="_black">Hutool Toolkit</a></pre>
      </div>
    </el-tab-pane>
  </el-tabs>
</template>

<script>
import Config from './config'
import Send from './send'
export default {
  name: 'Email',
  components: { Config, Send },
  data() {
    return {
      activeName: 'second'
    }
  }
}
</script>

<style scoped>
</style>
