<template>
  <el-tabs v-model="activeName" style="padding-left: 5px;">
    <el-tab-pane label="Configuration" name="first">
      <Config />
    </el-tab-pane>
    <el-tab-pane label="Payment Test" name="second">
      <ToPay />
    </el-tab-pane>
    <el-tab-pane label="Instructions" name="third">
      <div>
        <blockquote class="my-blockquote">Notice</blockquote>
        <pre class="my-code">
Parameters used for testing are sandbox environment, for testing only. Application URL: <a style="color: #00a0e9" href="https://openhome.alipay.com/platform/appDaily.htm?tab=info" target="_blank">Alipay Open Platform</a>
For payment testing, please use:
Account: uuxesw9745@sandbox.com
Password and payment password: 111111</pre>
        <blockquote class="my-blockquote"> Payment Settings</blockquote>
        <pre class="my-code">
// Payment provides two interfaces,
// PC and Mobile, identified by code on frontend
if (/(Android)/i.test(navigator.userAgent)){     // Determine whether it is an Android phone
    url = "/aliPay/toPayAsWeb"
}else if(/(iPhone|iPad|iPod|iOS)/i.test(navigator.userAgent)){  // Determine whether it is an Apple phone
    url = "/aliPay/toPayAsWeb"
} else {
    url = "/aliPay/toPayAsPC"
}</pre>
      </div>
    </el-tab-pane>
  </el-tabs>
</template>

<script>
import Config from './config'
import ToPay from './toPay'
export default {
  name: 'AliPay',
  components: { Config, ToPay },
  data() {
    return {
      activeName: 'second'
    }
  }
}
</script>

<style scoped>
</style>
