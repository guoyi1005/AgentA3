import { get, post, del } from '../utls/request'

export interface CollectionItem {
  id: number
  knowledge_id: number
  question: string
  job_position: string
  question_type: string
  remark: string
  created_at: string
  updated_at: string
}

export interface CollectionListResponse {
  items: CollectionItem[]
}

export const collectionApi = {
  list: (params?: { q?: string; job_position?: string; knowledge_id?: number }): Promise<CollectionListResponse> => {
    return get<CollectionListResponse>('/collection/list', { params })
  },

  create: (params: { knowledge_id: number; remark?: string | null }): Promise<CollectionItem> => {
    return post<CollectionItem>('/collection/create', params)
  },

  updateRemark: (params: { id: number; remark?: string | null }): Promise<CollectionItem> => {
    return post<CollectionItem>('/collection/update-remark', params)
  },

  delete: (id: number): Promise<{ ok: boolean }> => {
    return del<{ ok: boolean }>('/collection/delete', { params: { id } })
  },
}
