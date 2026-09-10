import { get } from '../utls/request'

export interface WrongBookItem {
  id: number
  knowledge_id: number
  title: string
  content: string
  source: string
  job_position: string
  question_type: string
  difficulty: number
  knowledge_point: string
  assessment_focus: string
  user_answer: string
  correct_answer: string
  wrong_count: number
  last_wrong_time: string
}

export interface WrongBookListResponse {
  items: WrongBookItem[]
}

export const wrongBookApi = {
  list: (): Promise<WrongBookListResponse> => {
    return get<WrongBookListResponse>('/wrong-book/list')
  },
}
