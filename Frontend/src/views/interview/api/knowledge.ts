import { get, post, put, del } from '../utls/request'

// ——— 数据类型定义 ———

export interface KnowledgeItem {
  id: number
  job_position: string
  question_type: string
  question: string
  difficulty?: number
  status: number
  in_chroma?: number  // 是否已入向量库 0否 1是
  excellent_answer?: string
  answer_points?: string
  score_standard?: string
  question_intent?: string
  keywords?: string
  suitable_level?: string
  remark?: string
  total_attempts?: number
  wrong_attempts?: number
  latest_score?: number | null
  best_score?: number | null
  avg_score?: number | null
  is_wrong_book?: number
  last_attempt_at?: string | null
  created_at: string
  updated_at: string
}

export interface KnowledgePracticeSummary {
  solved_questions: number
  total_questions: number
  total_attempts: number
  correct_rate: number | null
}

export interface KnowledgeListResponse {
  total: number
  page: number
  page_size: number
  items: KnowledgeItem[]
  summary?: KnowledgePracticeSummary
}

export interface KnowledgeListParams {
  page?: number
  page_size?: number
  job_position?: string
  question_type?: string
  q?: string
  status?: number
}

export interface KnowledgeCreateParams {
  job_position: string
  question_type: string
  question: string
  difficulty?: number
  status?: number
  excellent_answer?: string
  keywords?: string
  question_intent?: string
  answer_points?: string
  score_standard?: string
  suitable_level?: string
  remark?: string
}

export interface KnowledgeUpdateParams {
  id: number
  job_position?: string
  question_type?: string
  question?: string
  status?: number
}

export interface ChromaMatchParams {
  q: string
  k?: number
  job_position?: string
  question_type?: string
  status?: number
}

export interface ChromaMatchItem {
  doc_id: string
  knowledge_id?: number | null
  distance?: number | null
  document?: string | null
  metadata?: Record<string, unknown>
  knowledge?: KnowledgeItem | null
}

export interface ChromaMatchResponse {
  items: ChromaMatchItem[]
}

export interface KnowledgeTypeItem {
  label: string
  value: string
}

export interface KnowledgeTypeListResponse {
  items: KnowledgeTypeItem[]
}

export interface KnowledgePositionItem {
  label: string
  value: string
}

export interface KnowledgePositionListResponse {
  items: KnowledgePositionItem[]
}

export interface AiRecognizeParams {
  source_text: string
  job_position?: string
  question_type?: string
}

export interface AiRecognizeResult {
  job_position: string
  question_type: string
  question: string
  difficulty: number
  excellent_answer?: string
  keywords?: string
  question_intent?: string
  answer_points?: string
  score_standard?: string
  suitable_level?: string
  remark?: string
  job_position_exists?: boolean
}

export interface KnowledgeGradeParams {
  id: number
  user_answer: string
}

export interface KnowledgeGradeResponse {
  id: number
  question: string
  attempt_id?: number
  is_wrong_book?: number
  wrong_streak?: number
  score: number
  reference_answer: string
  strengths: string
  weaknesses: string
  improvement_suggestions: string
  overall_comment: string
}

// ——— 题库 API ———

export const knowledgeApi = {
  /**
   * 创建题目
   * POST /api/knowledge/create
   */
  create: (data: KnowledgeCreateParams): Promise<KnowledgeItem> => {
    return post<KnowledgeItem>('/knowledge/create', data)
  },

  /**
   * 获取题目详情
   * GET /api/knowledge/detail?id=xxx
   */
  detail: (id: number): Promise<KnowledgeItem> => {
    return get<KnowledgeItem>('/knowledge/detail', { params: { id } })
  },

  /**
   * 更新题目（id 必填，放在请求体中）
   * PUT /api/knowledge/update
   */
  update: (data: KnowledgeUpdateParams): Promise<KnowledgeItem> => {
    return put<KnowledgeItem>('/knowledge/update', data)
  },

  /**
   * 删除题目
   * DELETE /api/knowledge/delete
   */
  delete: (id: number): Promise<{ ok: boolean }> => {
    return del<{ ok: boolean }>('/knowledge/delete', { params: { id } })
  },

  /**
   * 分页查询题目列表
   * GET /api/knowledge/list
   */
  list: (params?: KnowledgeListParams): Promise<KnowledgeListResponse> => {
    return get<KnowledgeListResponse>('/knowledge/list', { params })
  },

  /**
   * 查询题库中已存在的题目类型
   * GET /api/knowledge/types
   */
  types: (params?: { job_position?: string; status?: number }): Promise<KnowledgeTypeListResponse> => {
    return get<KnowledgeTypeListResponse>('/knowledge/types', { params })
  },

  /**
   * 查询题库中已存在的岗位名称
   * GET /api/knowledge/positions
   */
  positions: (params?: { question_type?: string; status?: number }): Promise<KnowledgePositionListResponse> => {
    return get<KnowledgePositionListResponse>('/knowledge/positions', { params })
  },

  /**
   * AI 判题
   * POST /api/knowledge/grade
   */
  /**
   * AI 自动识别题目字段
   * POST /api/knowledge/ai-recognize
   */
  aiRecognize: (data: AiRecognizeParams): Promise<AiRecognizeResult> => {
    return post<AiRecognizeResult>('/knowledge/ai-recognize', data, { timeout: 60000 })
  },

  grade: (data: KnowledgeGradeParams): Promise<KnowledgeGradeResponse> => {
    return post<KnowledgeGradeResponse>('/knowledge/grade', data, { timeout: 120000 })
  },
}

// ——— Chroma 向量相关 API ———

export const chromaApi = {
  /**
   * 将题目写入 Chroma 向量库
   * POST /api/chroma/create
   * @param id 题目 id
   */
  create: (id: number): Promise<{ ok: boolean; in_chroma?: number }> => {
    return post<{ ok: boolean; in_chroma?: number }>('/chroma/create', { id })
  },

  /**
   * 向量语义匹配题目
   * GET /api/chroma/match?q=&k=&job_position=&question_type=&status=
   */
  match: (params: ChromaMatchParams): Promise<ChromaMatchResponse> => {
    return get<ChromaMatchResponse>('/chroma/match', { params })
  },
}
