import request from '../utils/request'

/** 通用接口：文件上传 / 聚合搜索 */
export const uploadImageApi = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const searchApi = (keyword) => request.get('/search', { params: { keyword } })
