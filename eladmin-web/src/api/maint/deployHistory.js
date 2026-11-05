import request from '@/utils/request'

export function del(ids) {
  return request({
    url: 'api/deployHistory',
    method: 'delete',
    data: ids
  })
}

/**
 * Version rollback
 * @param data Selected row
 */
export function reducte(data) {
  return request({
    url: 'api/deploy/serverReduction',
    method: 'post',
    data
  })
}
