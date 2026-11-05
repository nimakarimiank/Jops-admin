// Adapt to Nginx reverse proxy
const baseUrl = process.env.VUE_APP_BASE_API === '/' ? '' : process.env.VUE_APP_BASE_API
const api = {
  state: {
    // Deployment package upload
    deployUploadApi: baseUrl + '/api/deploy/upload',
    // SQL script upload
    databaseUploadApi: baseUrl + '/api/database/upload',
    // Image upload
    imagesUploadApi: baseUrl + '/api/localStorage/pictures',
    // Update avatar
    updateAvatarApi: baseUrl + '/api/users/updateAvatar',
    // Upload file to cloud storage
    s3UploadApi: baseUrl + '/api/s3Storage',
    // Sql monitor
    sqlApi: baseUrl + '/druid/index.html',
    // swagger
    swaggerApi: baseUrl + '/doc.html',
    // File upload
    fileUploadApi: baseUrl + '/api/localStorage',
    // baseUrl
    baseApi: baseUrl
  }
}

export default api
