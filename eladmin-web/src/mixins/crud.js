import {
  initData,
  download
} from '@/api/data'
import {
  parseTime,
  downloadFile
} from '@/utils'

export default {
  data() {
    return {
      // Table data
      data: [],
      // Page number
      page: 1,
      // Page size
      size: 10,
      // Total items count
      total: 0,
      // URL for requesting data
      url: '',
      // Query parameters
      params: {},
      // Query object
      query: {},
      // Wait time
      time: 50,
      // Whether add type form
      isAdd: false,
      // Export Loading
      downloadLoading: false,
      // Table Loading property
      loading: true,
      // Delete Loading property
      delLoading: false,
      delAllLoading: false,
      // Dialog property
      dialog: false,
      // Form data
      form: {},
      // Reset form
      resetForm: {},
      // Title
      title: ''
    }
  },
  methods: {
    parseTime,
    downloadFile,
    async init() {
      if (!await this.beforeInit()) {
        return
      }
      return new Promise((resolve, reject) => {
        this.loading = true
        // Request data
        initData(this.url, this.getQueryParams()).then(data => {
          this.total = data.totalElements
          this.data = data.content
          // Display table after time ms
          setTimeout(() => {
            this.loading = false
          }, this.time)
          resolve(data)
        }).catch(err => {
          this.loading = false
          reject(err)
        })
      })
    },
    beforeInit() {
      return true
    },
    getQueryParams: function() {
      // Clear empty parameters
      Object.keys(this.query).length !== 0 && Object.keys(this.query).forEach(item => {
        if (this.query[item] === null || this.query[item] === '') this.query[item] = undefined
      })
      Object.keys(this.params).length !== 0 && Object.keys(this.params).forEach(item => {
        if (this.params[item] === null || this.params[item] === '') this.params[item] = undefined
      })
      return {
        page: this.page,
        size: this.size,
        ...this.query,
        ...this.params
      }
    },
    // Change page number
    pageChange(e) {
      this.page = e
      this.init()
    },
    // Change page size
    sizeChange(e) {
      this.page = 1
      this.size = e
      this.init()
    },
    // Prevent empty data request caused by page index error when deleting last row or batch deleting on page 2
    dleChangePage(size) {
      if (size === undefined) {
        size = 1
      }
      if (this.data.length === size && this.page !== 0) {
        this.page = this.page - 1
      }
    },
    // Query method
    toQuery() {
      this.page = 0
      this.init()
    },
    /**
     * Common notification wrapper
     */
    submitSuccessNotify() {
      this.$notify({
        title: 'Submitted successfully',
        type: 'success',
        duration: 2500
      })
    },
    addSuccessNotify() {
      this.$notify({
        title: 'Added successfully',
        type: 'success',
        duration: 2500
      })
    },
    editSuccessNotify() {
      this.$notify({
        title: 'Updated successfully',
        type: 'success',
        duration: 2500
      })
    },
    delSuccessNotify() {
      this.$notify({
        title: 'Deleted successfully',
        type: 'success',
        duration: 2500
      })
    },
    notify(title, type) {
      this.$notify({
        title: title,
        type: type,
        duration: 2500
      })
    },
    /**
     * Hook before delete to execute custom operations
     */
    beforeDelMethod() {
      return true
    },
    /**
     * Common delete
     */
    delMethod(id) {
      if (!this.beforeDelMethod()) {
        return
      }
      this.delLoading = true
      this.crudMethod.del(id).then(() => {
        this.delLoading = false
        this.$refs[id].doClose()
        this.dleChangePage()
        this.delSuccessNotify()
        this.afterDelMethod()
        this.init()
      }).catch(() => {
        this.delLoading = false
        this.$refs[id].doClose()
      })
    },
    afterDelMethod() {},
    /**
     * Batch delete confirmation
     */
    beforeDelAllMethod() {
      this.$confirm('Are you sure you want to delete the selected data?', 'Notice', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.delAllMethod()
      })
    },
    /**
     * Batch delete
     */
    delAllMethod() {
      this.delAllLoading = true
      const data = this.$refs.table.selection
      const ids = []
      for (let i = 0; i < data.length; i++) {
        ids.push(data[i].id)
      }
      this.crudMethod.delAll(ids).then(() => {
        this.delAllLoading = false
        this.dleChangePage(ids.length)
        this.init()
        this.$notify({
          title: 'Deleted successfully',
          type: 'success',
          duration: 2500
        })
      }).catch(() => {
        this.delAllLoading = false
      })
    },
    /**
     * Hook before showing add dialog
     */
    beforeShowAddForm() {},
    /**
     * Show add dialog
     */
    showAddFormDialog() {
      this.isAdd = true
      this.resetForm = JSON.parse(JSON.stringify(this.form))
      this.beforeShowAddForm()
      this.dialog = true
    },
    /**
     * Hook before showing edit dialog
     */
    beforeShowEditForm(data) {},
    /**
     * Show edit dialog
     */
    showEditFormDialog(data = '') {
      this.isAdd = false
      if (data) {
        this.resetForm = JSON.parse(JSON.stringify(this.form))
        this.form = JSON.parse(JSON.stringify(data))
      }
      this.beforeShowEditForm(data)
      this.dialog = true
    },
    /**
     * Add method
     */
    addMethod() {
      this.crudMethod.add(this.form).then(() => {
        this.addSuccessNotify()
        this.loading = false
        this.afterAddMethod()
        this.cancel()
        this.init()
      }).catch(() => {
        this.loading = false
        this.afterAddErrorMethod()
      })
    },
    /**
     * Hook after add
     */
    afterAddMethod() {},
    /**
     * Hook after add error
     */
    afterAddErrorMethod() {},
    /**
     * Common edit method
     */
    editMethod() {
      this.crudMethod.edit(this.form).then(() => {
        this.editSuccessNotify()
        this.loading = false
        this.afterEditMethod()
        this.cancel()
        this.init()
      }).catch(() => {
        this.loading = false
      })
    },
    /**
     * Hook after edit
     */
    afterEditMethod() {},
    /**
     * Hook before submit
     */
    beforeSubmitMethod() {
      return true
    },
    /**
     * Submit
     */
    submitMethod() {
      if (!this.beforeSubmitMethod()) {
        return
      }
      if (this.$refs['form']) {
        this.$refs['form'].validate((valid) => {
          if (valid) {
            this.loading = true
            if (this.isAdd) {
              this.addMethod()
            } else this.editMethod()
          }
        })
      }
    },
    /**
     * Hide dialog
     */
    cancel() {
      this.dialog = false
      if (this.$refs['form']) {
        this.$refs['form'].clearValidate()
        this.form = this.resetForm
      }
    },
    /**
     * Get dialog title
     */
    getFormTitle() {
      return this.isAdd ? `Add ${this.title}` : `Edit ${this.title}`
    },
    /**
     * Common export
     */
    downloadMethod() {
      this.beforeInit()
      this.downloadLoading = true
      download(this.url + '/download', this.params).then(result => {
        this.downloadFile(result, this.title + ' Data', 'xlsx')
        this.downloadLoading = false
      }).catch(() => {
        this.downloadLoading = false
      })
    }
  }
}
