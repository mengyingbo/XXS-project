import { request } from './http'
import type { Movie } from '@/types/api'

/** 电影片库公开接口（免登录，需求文档 07 F-H5-15） */
export const movieApi = {
  /** 获取已上架电影列表 */
  list(): Promise<Movie[]> {
    return request({ url: '/movie', method: 'GET' })
  }
}
