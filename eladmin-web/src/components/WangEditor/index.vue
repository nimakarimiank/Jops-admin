<template>
  <div ref="editor" style="border: 1px solid #ccc;">
    <Toolbar
      style="border-bottom: 1px solid #ccc"
      :editor="editor"
      :default-config="toolbarConfig"
      :mode="editMode"
    />
    <Editor
      v-model="editValue"
      :style="{'height': editorHeight +'px', 'overflow-y': 'hidden'}"
      :default-config="editorConfig"
      :mode="editMode"
      @onCreated="onCreated"
    />
  </div>
</template>

<script>
import { upload } from '@/utils/upload'
import { Toolbar, Editor } from '@wangeditor/editor-for-vue'
import { mapGetters } from 'vuex'

export default {
  name: 'WangEditor',
  components: { Toolbar, Editor },
  props: {
    value: {
      type: String,
      required: false,
      default: ''
    },
    editorHeight: {
      type: Number,
      required: false,
      default: 420
    }
  },
  data() {
    const _this = this
    return {
      toolbarConfig: {},
      editorConfig: { placeholder: 'Please enter content...', MENU_CONF: {
        'uploadImage': {
          // File type limit when selecting files, default is ['image/*']. If no limit, set to []
          allowedFileTypes: ['image/*'],
          // Custom upload
          async customUpload(file, insertFn) { // JS syntax
            upload(_this.imagesUploadApi, file).then(res => {
              const data = res.data
              const url = _this.baseApi + '/file/' + data.type + '/' + data.realName
              // Finally insert image
              insertFn(url, '', '')
            })
          }
        }
      }},
      editMode: 'simple',
      editor: null,
      editValue: null
    }
  },
  computed: {
    ...mapGetters([
      'imagesUploadApi',
      'baseApi'
    ])
  },
  watch: {
    editValue(newVal, oldVal) {
      this.$emit('input', newVal)
    }
  },
  mounted() {

  },
  methods: {
    onCreated(editor) {
      this.editor = Object.seal(editor)
      this.editValue = this.value
    }
  }
}
</script>
<style src="@wangeditor/editor/dist/css/style.css"></style>
<style scoped>
.text {
  text-align:left;
}
::v-deep .w-e-text-container {
  height: 420px !important;
}
</style>
