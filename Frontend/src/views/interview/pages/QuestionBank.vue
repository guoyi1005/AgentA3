<template>
  <div class="page-layout">
    <Sidebar />
    <main class="main-content">
      <div class="top-sticky-shell">
        <div class="page-container top-sticky-inner">
          <div class="top-sticky-block">
          <!-- 顶部搜索栏 -->
          <div class="top-search-bar">
            <div class="search-box">
              <img class="search-icon" src="@/assets/interview/Search.png" alt="搜索" />
              <input 
                v-model="searchQuery" 
                type="text" 
                placeholder="搜索题目、知识点、企业真题..."
                @input="handleSearch"
              />
            </div>
          </div>
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    
          <!-- 分类标签 -->
          <div class="category-tabs">
            <span 
              v-for="tab in categoryTabs" 
              :key="tab.value"
              class="tab-item"
              :class="{ active: currentTab === tab.value }"
              @click="handleCategoryChange(tab.value)"
            >
              {{ tab.label }}
            </span>
          </div>
        </div>
        </div>
      </div>

      <div class="page-container page-content-body">

        <!-- 统计卡片区域 -->
        <div class="stats-section">
          <div class="stat-card">
            <div class="stat-label">已做题目</div>
            <div class="stat-value">
              <span class="number">{{ progress.totalSolved }}</span>
              <span class="total">/ {{ progress.totalQuestions }}</span>
            </div>
            <div class="stat-icon"><img src="@/assets/interview/Complete.png" alt="已做" /></div>
          </div>
          <div class="stat-card">
            <div class="stat-label">正确率</div>
            <div class="stat-value">
              <span class="number green">{{ progress.correctRate }}%</span>
            </div>
            <div class="stat-icon"><img src="@/assets/interview/Chart.png" alt="正确率" /></div>
          </div>
          <div class="stat-card">
            <div class="stat-label">连续打卡</div>
            <div class="stat-value">
              <span class="number orange">{{ progress.streakDays }}</span>
              <span class="unit">Days</span>
            </div>
            <div class="stat-icon"><img src="@/assets/interview/Fire.png" alt="连续打卡" /></div>
          </div>
        </div>

        <!-- 主体内容区域 -->
        <div class="main-body">
          <div class="section-card question-list-card">
            <div class="section-header">
              <h3>题目列表</h3>
              <span class="view-all">共 {{ filteredQuestions.length }} 题</span>
            </div>

            <div class="exam-list" v-if="paginatedQuestions.length > 0">
              <div
                v-for="q in paginatedQuestions"
                :key="q.id"
                class="exam-item"
                @click="openQuestionDetail(q)"
              >
                <div class="exam-icon" :style="{ background: q.isSolved ? '#1f4f89' : '#3a3f4b' }">题</div>
                <div class="exam-info">
                  <div class="exam-title">{{ q.title }}</div>
                  <div class="exam-meta">
                    <span class="tag">{{ q.position }}</span>
                    <span class="tag">{{ q.type }}</span>
                    <span class="difficulty" :class="`diff-${q.difficulty}`">{{ getDifficultyLabel(q.difficulty) }}</span>
                    <span class="count">{{ q.knowledgePoint }}</span>
                  </div>
                </div>
                <div class="exam-arrow">›</div>
              </div>
            </div>

            <div class="empty-result" v-else>
              {{ listLoading ? '题目加载中...' : '当前筛选条件下暂无题目' }}
            </div>

            <div class="pagination" v-if="totalPages > 1">
              <button class="page-btn" :disabled="currentPage <= 1" @click="currentPage -= 1">上一页</button>
              <span class="page-text">{{ currentPage }} / {{ totalPages }}</span>
              <button class="page-btn" :disabled="currentPage >= totalPages" @click="currentPage += 1">下一页</button>
            </div>
          </div>

          <div class="right-content">
            <div class="section-card mock-exam-card">
              <h3>模拟笔试</h3>
              <p class="mock-desc">完全模拟真实大厂面试环境，限时练习，即时评分，深度解析。</p>
              <div class="mock-features">
                <div class="feature-item">
                  <img class="feature-icon" src="@/assets/interview/Robot.png" alt="AI" />
                  <span>AI 自动化能力评估报告</span>
                </div>
                <div class="feature-item">
                  <img class="feature-icon" src="@/assets/interview/16.png" alt="摄像头" />
                  <span>支持防作弊摄像头模拟</span>
                </div>
              </div>
              <button class="mock-start-btn" @click="startMockExam">立即开始面试</button>
            </div>

            <div class="section-card">
              <h3>我的收藏</h3>
              <div class="favorite-list">
                <div
                  v-for="item in favoriteList"
                  :key="item.id"
                  class="favorite-item"
                >
                  <span class="star">★</span>
                  <div class="favorite-content">
                    <div class="favorite-title">{{ item.title }}</div>
                    <div class="favorite-tag">{{ item.tag }}</div>
                  </div>
                </div>
                <div v-if="favoriteList.length === 0" class="favorite-empty">暂无收藏记录</div>
              </div>
              <a href="#" class="manage-link" @click.prevent="goMyNote">管理收藏夹</a>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 题目详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-content question-detail-modal">
        <div class="modal-header">
          <h2>题目详情</h2>
          <button class="close-btn" @click="closeDetailModal">×</button>
        </div>
        <div class="modal-body" v-if="selectedQuestion">
          <div class="detail-tags">
            <span class="detail-tag position">{{ selectedQuestion.position }}</span>
            <span class="detail-tag type">{{ selectedQuestion.type }}</span>
            <span class="detail-tag difficulty" :class="'diff-' + selectedQuestion.difficulty">
              {{ getDifficultyLabel(selectedQuestion.difficulty) }}
            </span>
            <span class="detail-tag">{{ selectedQuestion.knowledgePoint }}</span>
          </div>
          <div class="detail-title">{{ selectedQuestion.title }}</div>
          <div class="detail-content">{{ selectedQuestion.content }}</div>
          
          <!-- 答题区 -->
          <div class="answer-section">
            <h4>我的答案</h4>
            <textarea 
              v-model="userAnswer" 
              class="answer-input"
              placeholder="请输入你的答案..."
              rows="6"
            ></textarea>
            <div class="answer-actions">
              <button class="btn-secondary" @click="showHint">查看提示</button>
              <button class="btn-primary" :disabled="gradingLoading" @click="submitAnswer">{{ gradingLoading ? '判题中...' : '提交答案' }}</button>
            </div>
          </div>

          <!-- 提示区 -->
          <div v-if="showHintSection" class="hint-section">
            <h4>答题提示</h4>
            <p>{{ selectedQuestion.hint || '暂无提示' }}</p>
          </div>

          <!-- 答案解析区（提交后显示） -->
          <div v-if="showAnalysis" class="analysis-section">
            <h4>答案解析</h4>
            <div class="analysis-content">
              <div class="analysis-item" v-if="selectedQuestion.aiScore !== undefined">
                <strong>AI评分：</strong>
                <p>{{ selectedQuestion.aiScore }} 分</p>
              </div>
              <div class="analysis-item">
                <strong>标准答案：</strong>
                <p>{{ selectedQuestion.standardAnswer }}</p>
              </div>
              <div class="analysis-item">
                <strong>解题思路：</strong>
                <p>{{ selectedQuestion.solution }}</p>
              </div>
              <div class="analysis-item">
                <strong>易错点：</strong>
                <p>{{ selectedQuestion.pitfalls }}</p>
              </div>
              <div class="analysis-item">
                <strong>面试官考察重点：</strong>
                <p>{{ selectedQuestion.keyPoints }}</p>
              </div>
            </div>
            <div class="analysis-actions">
              <button class="btn-primary" @click="nextQuestion">下一题</button>
            </div>
          </div>

          <!-- 笔记功能 -->
          <div class="notes-section">
            <h4>
              我的笔记 
              <button class="btn-icon" @click="showNoteInput = !showNoteInput">
                {{ showNoteInput ? '收起' : '添加笔记' }}
              </button>
            </h4>
            <div v-if="showNoteInput" class="note-input-area">
              <textarea 
                v-model="newNote" 
                placeholder="记录你的学习心得..."
                rows="3"
              ></textarea>
              <button class="btn-primary" :disabled="noteSaving" @click="saveNote">{{ noteSaving ? '保存中...' : '保存笔记' }}</button>
            </div>
            <div class="notes-list">
              <div v-for="(note, index) in selectedQuestion.notes" :key="index" class="note-item">
                <p>{{ note.content }}</p>
                <span class="note-time">更新于 {{ note.time }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 批量刷题弹窗 -->
    <div v-if="showBatchModal" class="modal-overlay" @click.self="closeBatchModal">
      <div class="modal-content batch-modal">
        <div class="modal-header">
          <h2>{{ currentBatchModeInfo?.name }}</h2>
          <button class="close-btn" @click="closeBatchModal">×</button>
        </div>
        <div class="modal-body">
          <p class="batch-desc">{{ currentBatchModeInfo?.description }}</p>
          
          <div class="batch-config" v-if="currentBatchMode === 'custom'">
            <label>选择题目数量：</label>
            <input 
              v-model.number="customQuestionCount" 
              type="number" 
              min="1" 
              max="50"
              class="count-input"
            />
          </div>

          <div class="batch-preview">
            <h4>即将练习的题目（{{ batchQuestions.length }}题）</h4>
            <div class="batch-questions-list">
              <div 
                v-for="(q, index) in batchQuestions" 
                :key="q.id"
                class="batch-question-item"
              >
                <span class="batch-index">{{ index + 1 }}</span>
                <span class="batch-title">{{ q.title }}</span>
                <span class="batch-difficulty" :class="'diff-' + q.difficulty">
                  {{ getDifficultyLabel(q.difficulty) }}
                </span>
              </div>
            </div>
          </div>

          <div class="batch-actions">
            <button class="btn-secondary" @click="closeBatchModal">取消</button>
            <button class="btn-primary" @click="startBatchPractice">开始练习</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router';
import Sidebar from '../components/Sidebar.vue';
import { knowledgeApi, type KnowledgeItem } from '../api/knowledge';
import { collectionApi } from '../api/collection';
import { PATHS } from '../routes/paths';

const router = useRouter()

// ==================== 类型定义 ====================
interface Question {
  id: number;
  title: string;
  content: string;
  position: string;
  type: string;
  difficulty: number;
  knowledgePoint: string;
  interviewRound: string;
  isSolved: boolean;
  isWrong: boolean;
  isFavorite: boolean;
  standardAnswer?: string;
  solution?: string;
  pitfalls?: string;
  keyPoints?: string;
  hint?: string;
  aiScore?: number;
  notes?: { content: string; time: string }[];
}

interface FilterOptions {
  positions: { label: string; value: string }[];
  questionTypes: { label: string; value: string }[];
  difficulties: { label: string; value: number }[];
  knowledgePoints: { label: string; value: string }[];
  interviewRounds: { label: string; value: string }[];
}

interface BatchMode {
  id: string;
  name: string;
  description: string;
  icon: string;
  recommendedCount?: number;
}

// ==================== 响应式数据 ====================
const searchQuery = ref('');
const viewMode = ref<'list' | 'card'>('list');
const sortBy = ref('default');
const currentPage = ref(1);
const pageSize = ref(10);
const currentTab = ref('');
const listLoading = ref(false);

const selectedFilters = ref<{
  position: string;
  questionType: string;
  difficulty: number | null;
  knowledgePoint: string;
  interviewRound: string;
}>({
  position: '',
  questionType: '',
  difficulty: null,
  knowledgePoint: '',
  interviewRound: ''
});

const currentBatchMode = ref('');
const customQuestionCount = ref(10);

// 弹窗控制
const showDetailModal = ref(false);
const showBatchModal = ref(false);
const selectedQuestion = ref<Question | null>(null);
const userAnswer = ref('');
const showHintSection = ref(false);
const showAnalysis = ref(false);
const showNoteInput = ref(false);
const newNote = ref('');
const noteSaving = ref(false);
const gradingLoading = ref(false);

// 进度数据
const progress = ref({
  totalSolved: 1284,
  totalQuestions: 5000,
  todaySolved: 12,
  correctRate: 85.4,
  streakDays: 12
});

// 分类标签
const categoryTabs = ref<{ label: string; value: string }[]>([
  { label: '全部岗位', value: '' }
]);

// 专项练习数据
const practiceItems = ref([
  {
    id: 1,
    name: 'MySQL 深度调优',
    description: '索引、事务、高可用',
    icon: '📊',
    iconBg: '#1e3a5f',
    progress: 68,
    progressColor: '#3a7bc8',
    solved: 142,
    total: 220
  },
  {
    id: 2,
    name: 'Redis 应用实战',
    description: '缓存策略、持久化、集群',
    icon: '⚡',
    iconBg: '#5c3a1e',
    progress: 30,
    progressColor: '#faad14',
    solved: 45,
    total: 150
  },
  {
    id: 3,
    name: 'Spring Boot 进阶',
    description: 'IOC/AOP 原理，自动装配',
    icon: '🍃',
    iconBg: '#3d5c3a',
    progress: 88,
    progressColor: '#52c41a',
    solved: 264,
    total: 300
  },
  {
    id: 4,
    name: '云原生架构 (K8s)',
    description: 'Pod、Service、Ingress',
    icon: '☸️',
    iconBg: '#3a5c5c',
    progress: 0,
    progressColor: '#3a7bc8',
    solved: 0,
    total: 180
  }
]);

// 高频真题集
const examList = ref([
  {
    id: 1,
    title: '字节跳动 2024 秋招后端笔试真题',
    icon: '字',
    iconBg: '#3a7bc8',
    tag: '25题',
    difficulty: 'hard',
    difficultyText: '困难',
    count: '1.2w'
  },
  {
    id: 2,
    title: '阿里巴巴 P7 级 Java 核心面试题集',
    icon: '阿',
    iconBg: '#fa8c16',
    tag: '40题',
    difficulty: 'medium',
    difficultyText: '中等',
    count: '8.5k'
  },
  {
    id: 3,
    title: '腾讯 2024 架构师岗高并发系统设计',
    icon: '腾',
    iconBg: '#52c41a',
    tag: '12题',
    difficulty: 'hard',
    difficultyText: '专家',
    count: '5.3k'
  }
]);

// 我的收藏（最近3条）
const favoriteList = ref<Array<{ id: number; title: string; tag: string }>>([]);

const formatFavoriteTag = (createdAt: string) => {
  const dt = new Date(createdAt);
  if (Number.isNaN(dt.getTime())) return '收藏于 -';
  const diffMs = Date.now() - dt.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  if (diffDays <= 0) return '收藏于 今天';
  if (diffDays < 7) return `收藏于 ${diffDays} 天前`;
  const weeks = Math.floor(diffDays / 7);
  if (weeks < 5) return `收藏于 ${weeks} 周前`;
  return `收藏于 ${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`;
};

const loadFavoriteList = async () => {
  try {
    const res = await collectionApi.list();
    favoriteList.value = (res.items || []).slice(0, 3).map((it) => ({
      id: Number(it.id || 0),
      title: String(it.question || ''),
      tag: formatFavoriteTag(String(it.created_at || '')),
    }));
  } catch (e) {
    console.error('[QuestionBank] load favorite list failed', e);
    favoriteList.value = [];
  }
};

// ==================== 筛选选项 ====================
const filterOptions: FilterOptions = {
  positions: [
    { label: '全部岗位', value: '' },
    { label: 'Java开发', value: 'java' },
    { label: '前端开发', value: 'frontend' },
    { label: '产品经理', value: 'pm' },
    { label: '运营', value: 'operation' },
    { label: '算法', value: 'algorithm' },
    { label: '测试开发', value: 'qa' },
    { label: '数据分析师', value: 'data' }
  ],
  questionTypes: [
    { label: '全部题型', value: '' },
    { label: '选择题', value: 'choice' },
    { label: '简答题', value: 'short' },
    { label: '编程题', value: 'coding' },
    { label: '场景分析题', value: 'scenario' },
    { label: '行为面试题', value: 'behavioral' },
    { label: '案例题', value: 'case' }
  ],
  difficulties: [
    { label: '全部难度', value: 0 },
    { label: '入门', value: 1 },
    { label: '基础', value: 2 },
    { label: '中等', value: 3 },
    { label: '进阶', value: 4 },
    { label: '大厂真题', value: 5 }
  ],
  knowledgePoints: [
    { label: '全部知识点', value: '' },
    { label: 'JVM', value: 'jvm' },
    { label: 'Spring', value: 'spring' },
    { label: '集合框架', value: 'collection' },
    { label: '并发编程', value: 'concurrency' },
    { label: '数据库', value: 'database' },
    { label: 'Redis', value: 'redis' },
    { label: '消息队列', value: 'mq' },
    { label: '微服务', value: 'microservice' },
    { label: '设计模式', value: 'design-pattern' }
  ],
  interviewRounds: [
    { label: '全部环节', value: '' },
    { label: '一面基础', value: 'round1' },
    { label: '二面综合', value: 'round2' },
    { label: '三面业务', value: 'round3' },
    { label: 'HR面', value: 'hr' }
  ]
};

// ==================== 批量刷题模式 ====================
const batchModes: BatchMode[] = [
  {
    id: 'daily',
    name: '每日一练',
    description: '系统根据你的学习进度智能推荐今日练习题目',
    icon: '📅',
    recommendedCount: 5
  },
  {
    id: 'special',
    name: '专项刷题',
    description: '针对薄弱知识点进行专项突破训练',
    icon: '🎯',
    recommendedCount: 10
  },
  {
    id: 'custom',
    name: '自由刷题',
    description: '自定义题目数量，自由选择练习内容',
    icon: '🔧'
  }
];

const currentBatchModeInfo = computed(() => 
  batchModes.find(m => m.id === currentBatchMode.value)
);

// ==================== 模拟题目数据 ====================
const questions = ref<Question[]>([
  {
    id: 1,
    title: '请简述HashMap的工作原理及线程安全问题',
    content: 'HashMap是Java中最常用的集合类之一，请详细说明其底层数据结构、put/get操作的流程，以及为什么它不是线程安全的？',
    position: 'Java开发',
    type: '简答题',
    difficulty: 3,
    knowledgePoint: '集合框架',
    interviewRound: '一面基础',
    isSolved: true,
    isWrong: false,
    isFavorite: true,
    standardAnswer: 'HashMap基于数组+链表/红黑树实现。put时计算key的hash值，定位到数组下标...',
    solution: '1. 说明底层结构 2. 解释hash计算 3. 说明扩容机制 4. 解释线程不安全原因',
    pitfalls: '容易忽略红黑树转换条件、扩容时的rehash过程',
    keyPoints: '考察对集合框架底层原理的理解',
    hint: '从数据结构、hash算法、冲突解决、扩容机制几个角度思考',
    notes: []
  },
  {
    id: 2,
    title: 'JVM内存模型及垃圾回收机制',
    content: '请描述JVM的内存区域划分，以及常见的垃圾回收算法和收集器。',
    position: 'Java开发',
    type: '简答题',
    difficulty: 4,
    knowledgePoint: 'JVM',
    interviewRound: '二面综合',
    isSolved: false,
    isWrong: true,
    isFavorite: false,
    standardAnswer: 'JVM内存分为堆、栈、方法区、程序计数器等区域...',
    solution: '1. 内存区域划分 2. 各区域作用 3. GC算法 4. 垃圾收集器对比',
    pitfalls: '混淆JVM内存模型和JMM（Java内存模型）',
    keyPoints: '考察JVM调优基础和内存管理能力',
    hint: '先画内存结构图，再分别说明每个区域的作用',
    notes: []
  },
  {
    id: 3,
    title: 'Spring Boot自动配置原理',
    content: 'Spring Boot是如何实现自动配置的？请从@SpringBootApplication注解开始分析。',
    position: 'Java开发',
    type: '简答题',
    difficulty: 4,
    knowledgePoint: 'Spring',
    interviewRound: '二面综合',
    isSolved: false,
    isWrong: false,
    isFavorite: true,
    standardAnswer: '@SpringBootApplication包含@Configuration、@EnableAutoConfiguration、@ComponentScan...',
    solution: '1. 分析注解组成 2. 讲解@EnableAutoConfiguration 3. 说明spring.factories 4. 条件注解',
    pitfalls: '只说自动配置，不解释具体实现机制',
    keyPoints: '考察对Spring Boot核心原理的理解',
    hint: '从@SpringBootApplication的元注解入手',
    notes: []
  },
  {
    id: 4,
    title: 'Redis缓存穿透、击穿、雪崩及解决方案',
    content: '请解释Redis使用中的三个常见问题及其应对措施。',
    position: 'Java开发',
    type: '简答题',
    difficulty: 3,
    knowledgePoint: 'Redis',
    interviewRound: '一面基础',
    isSolved: true,
    isWrong: false,
    isFavorite: false,
    standardAnswer: '缓存穿透：查询不存在数据，解决方案布隆过滤器...',
    solution: '分别解释三个概念，给出对应解决方案',
    pitfalls: '混淆三个概念的区别',
    keyPoints: '考察Redis实战经验和问题解决能力',
    hint: '先明确三个问题的定义和区别',
    notes: []
  },
  {
    id: 5,
    title: '线程池的核心参数及执行流程',
    content: 'ThreadPoolExecutor有哪些核心参数？提交任务后的执行流程是怎样的？',
    position: 'Java开发',
    type: '简答题',
    difficulty: 3,
    knowledgePoint: '并发编程',
    interviewRound: '一面基础',
    isSolved: false,
    isWrong: false,
    isFavorite: false,
    standardAnswer: '核心参数：corePoolSize、maximumPoolSize、keepAliveTime、workQueue...',
    solution: '1. 七个参数说明 2. 任务提交流程 3. 拒绝策略',
    pitfalls: '说不清楚任务进入队列的时机',
    keyPoints: '考察并发编程基础',
    hint: '结合execute方法的源码流程图',
    notes: []
  },
  {
    id: 6,
    title: 'MySQL索引优化实战',
    content: '如何分析和优化慢查询？explain命令的关键字段有哪些？',
    position: 'Java开发',
    type: '简答题',
    difficulty: 4,
    knowledgePoint: '数据库',
    interviewRound: '二面综合',
    isSolved: false,
    isWrong: true,
    isFavorite: true,
    standardAnswer: '使用explain分析执行计划，关注type、key、rows、Extra等字段...',
    solution: '1. 慢查询定位 2. explain分析 3. 索引优化策略',
    pitfalls: '只看是否用到索引，不关注索引选择性',
    keyPoints: '考察SQL优化能力',
    hint: '从慢查询日志到explain分析完整流程',
    notes: []
  },
  {
    id: 7,
    title: '消息队列如何保证消息不丢失',
    content: '在使用Kafka/RabbitMQ等消息队列时，如何保证消息的可靠传输？',
    position: 'Java开发',
    type: '简答题',
    difficulty: 4,
    knowledgePoint: '消息队列',
    interviewRound: '二面综合',
    isSolved: true,
    isWrong: false,
    isFavorite: false,
    standardAnswer: '生产者确认、消息持久化、消费者确认三个层面保证...',
    solution: '分别从生产者、MQ、消费者三个角度分析',
    pitfalls: '只考虑单一环节，不全面考虑整个链路',
    keyPoints: '考察分布式系统消息可靠性设计',
    hint: '画出消息流转的完整链路',
    notes: []
  },
  {
    id: 8,
    title: '单例模式的双重检查锁定',
    content: '手写线程安全的单例模式，并解释为什么需要双重检查？',
    position: 'Java开发',
    type: '编程题',
    difficulty: 3,
    knowledgePoint: '设计模式',
    interviewRound: '一面基础',
    isSolved: true,
    isWrong: false,
    isFavorite: true,
    standardAnswer: 'public class Singleton { private volatile static Singleton instance; ... }',
    solution: '1. 私有构造器 2. volatile关键字 3. 双重检查逻辑',
    pitfalls: '忘记volatile关键字，导致指令重排序问题',
    keyPoints: '考察设计模式与并发基础',
    hint: '考虑指令重排序对单例的影响',
    notes: []
  }
]);

// ==================== 计算属性 ====================
const hasActiveFilters = computed(() => {
  return Object.values(selectedFilters.value).some(v => v !== '');
});

const activeFilterTags = computed(() => {
  const tags: { key: 'position' | 'questionType' | 'difficulty' | 'knowledgePoint' | 'interviewRound'; label: string }[] = [];
  if (selectedFilters.value.position) {
    const item = filterOptions.positions.find(p => p.value === selectedFilters.value.position);
    if (item) tags.push({ key: 'position', label: item.label });
  }
  if (selectedFilters.value.questionType) {
    const item = filterOptions.questionTypes.find(t => t.value === selectedFilters.value.questionType);
    if (item) tags.push({ key: 'questionType', label: item.label });
  }
  if (selectedFilters.value.difficulty !== null && selectedFilters.value.difficulty !== 0) {
    const item = filterOptions.difficulties.find(d => d.value === selectedFilters.value.difficulty);
    if (item) tags.push({ key: 'difficulty', label: item.label });
  }
  if (selectedFilters.value.knowledgePoint) {
    const item = filterOptions.knowledgePoints.find(k => k.value === selectedFilters.value.knowledgePoint);
    if (item) tags.push({ key: 'knowledgePoint', label: item.label });
  }
  if (selectedFilters.value.interviewRound) {
    const item = filterOptions.interviewRounds.find(r => r.value === selectedFilters.value.interviewRound);
    if (item) tags.push({ key: 'interviewRound', label: item.label });
  }
  return tags;
});

const filteredQuestions = computed(() => {
  let result = questions.value;

  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(q => 
      q.title.toLowerCase().includes(query) || 
      q.content.toLowerCase().includes(query) ||
      q.knowledgePoint.toLowerCase().includes(query)
    );
  }

  // 筛选过滤
  if (selectedFilters.value.position) {
    result = result.filter(q => q.position.toLowerCase().includes(selectedFilters.value.position.toLowerCase()));
  }
  if (selectedFilters.value.questionType) {
    result = result.filter(q => q.type.toLowerCase().includes(selectedFilters.value.questionType.toLowerCase()));
  }
  if (selectedFilters.value.difficulty !== null) {
    result = result.filter(q => q.difficulty === selectedFilters.value.difficulty);
  }
  if (selectedFilters.value.knowledgePoint) {
    result = result.filter(q => q.knowledgePoint.toLowerCase().includes(selectedFilters.value.knowledgePoint.toLowerCase()));
  }
  if (selectedFilters.value.interviewRound) {
    result = result.filter(q => q.interviewRound.toLowerCase().includes(selectedFilters.value.interviewRound.toLowerCase()));
  }

  // 排序
  if (sortBy.value === 'difficulty-asc') {
    result = [...result].sort((a, b) => a.difficulty - b.difficulty);
  } else if (sortBy.value === 'difficulty-desc') {
    result = [...result].sort((a, b) => b.difficulty - a.difficulty);
  } else if (sortBy.value === 'newest') {
    result = [...result].sort((a, b) => b.id - a.id);
  }

  return result;
});

const totalPages = computed(() => Math.ceil(filteredQuestions.value.length / pageSize.value));

const paginatedQuestions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredQuestions.value.slice(start, start + pageSize.value);
});

const batchQuestions = computed(() => {
  if (currentBatchMode.value === 'daily') {
    return filteredQuestions.value.slice(0, 5);
  } else if (currentBatchMode.value === 'special') {
    return filteredQuestions.value.filter(q => q.isWrong).slice(0, 10);
  } else {
    return filteredQuestions.value.slice(0, customQuestionCount.value);
  }
});

// ==================== 方法 ====================
const getDifficultyLabel = (difficulty: number) => {
  const labels: Record<number, string> = {
    1: '入门',
    2: '基础',
    3: '中等',
    4: '进阶',
    5: '大厂真题'
  };
  return labels[difficulty] || '未知';
};

const selectFilter = (key: keyof typeof selectedFilters.value, value: string | number) => {
  if (key === 'difficulty') {
    selectedFilters.value[key] = value === '' ? null : (value as number);
  } else {
    selectedFilters.value[key] = value as string;
  }
  currentPage.value = 1;
};

const removeFilter = (key: keyof typeof selectedFilters.value) => {
  if (key === 'difficulty') {
    selectedFilters.value[key] = null;
  } else {
    selectedFilters.value[key] = '';
  }
  currentPage.value = 1;
};

const clearAllFilters = () => {
  selectedFilters.value = {
    position: '',
    questionType: '',
    difficulty: null,
    knowledgePoint: '',
    interviewRound: ''
  };
  currentPage.value = 1;
};

const handleSearch = () => {
  currentPage.value = 1;
};

const mapKnowledgeToQuestion = (item: KnowledgeItem): Question => {
  const firstKeyword = String(item.keywords || '')
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean)[0] || '通用';

  return {
    id: item.id,
    title: item.question,
    content: item.question,
    position: item.job_position,
    type: item.question_type,
    difficulty: Number(item.difficulty || 3),
    knowledgePoint: firstKeyword,
    interviewRound: '一面基础',
    isSolved: false,
    isWrong: false,
    isFavorite: false,
    standardAnswer: item.excellent_answer || '',
    solution: item.answer_points || '',
    pitfalls: item.remark || '',
    keyPoints: item.question_intent || '',
    hint: item.answer_points || '',
    notes: []
  };
};

const loadQuestionList = async () => {
  listLoading.value = true;
  try {
    const params: { page: number; page_size: number; status: number; job_position?: string } = {
      page: 1,
      page_size: 500,
      status: 1
    };
    if (selectedFilters.value.position) {
      params.job_position = selectedFilters.value.position;
    }
    const res = await knowledgeApi.list(params);
    questions.value = (res.items || []).map(mapKnowledgeToQuestion);
  } catch (e) {
    console.error('[QuestionBank] load questions failed', e);
    questions.value = [];
  } finally {
    listLoading.value = false;
  }
};

const handleCategoryChange = (val: string) => {
  currentTab.value = val;
  selectedFilters.value.position = val;
  selectedFilters.value.questionType = '';
  currentPage.value = 1;
  void loadQuestionList();
};

const selectBatchMode = (modeId: string) => {
  currentBatchMode.value = modeId;
  showBatchModal.value = true;
};

const toggleFavorite = (question: Question) => {
  question.isFavorite = !question.isFavorite;
};

const loadCollectionNoteForQuestion = async (question: Question) => {
  try {
    const res = await collectionApi.list({ knowledge_id: question.id });
    const hit = (res.items || [])[0];
    if (!hit) {
      question.isFavorite = false;
      question.notes = [];
      return;
    }
    question.isFavorite = true;
    const remark = String(hit.remark || '').trim();
    if (!remark) {
      question.notes = [];
      return;
    }
    question.notes = [{
      content: remark,
      time: String(hit.updated_at || hit.created_at || '').replace('T', ' ').slice(0, 19),
    }];
  } catch (e) {
    console.error('[QuestionBank] load collection note failed', e);
  }
};

const openQuestionDetail = async (question: Question) => {
  selectedQuestion.value = question;
  userAnswer.value = '';
  showHintSection.value = false;
  showAnalysis.value = false;
  showNoteInput.value = false;
  newNote.value = '';
  await loadCollectionNoteForQuestion(question);
  showDetailModal.value = true;
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedQuestion.value = null;
};

const closeBatchModal = () => {
  showBatchModal.value = false;
};

const startPractice = (question: Question) => {
  void openQuestionDetail(question);
};

const startBatchPractice = () => {
  if (batchQuestions.value.length > 0) {
    const firstQuestion = batchQuestions.value[0];
    if (firstQuestion) {
      void openQuestionDetail(firstQuestion);
    }
  }
  closeBatchModal();
};

const showHint = () => {
  showHintSection.value = true;
};

const submitAnswer = async () => {
  if (!userAnswer.value.trim()) {
    alert('请先输入答案');
    return;
  }
  if (!selectedQuestion.value) {
    return;
  }
  gradingLoading.value = true;
  try {
    const q = selectedQuestion.value;
    const res = await knowledgeApi.grade({
      id: q.id,
      user_answer: userAnswer.value.trim(),
    });
    q.aiScore = Number(res.score || 0);
    q.standardAnswer = res.reference_answer || q.standardAnswer || '';
    q.solution = res.improvement_suggestions || q.solution || '';
    q.pitfalls = res.weaknesses || q.pitfalls || '';
    q.keyPoints = res.strengths || q.keyPoints || '';
    q.hint = res.overall_comment || q.hint || '';
    q.isSolved = true;
    q.isWrong = Number(res.is_wrong_book || 0) === 1;
    showAnalysis.value = true;
  } catch (e) {
    console.error('[QuestionBank] grade answer failed', e);
    const err = e as any;
    const status = Number(err?.response?.status || 0);
    const code = String(err?.response?.data?.error || '');
    // 401 时请求拦截器会跳转登录，这里不再弹“判题失败”误导用户
    if (status === 401 || code === 'invalid_or_expired_session') {
      return;
    }
    alert(err?.message || 'AI判题失败，请稍后重试');
  } finally {
    gradingLoading.value = false;
  }
};

const nextQuestion = () => {
  const currentIndex = filteredQuestions.value.findIndex(q => q.id === selectedQuestion.value?.id);
  if (currentIndex < filteredQuestions.value.length - 1) {
    const nextQ = filteredQuestions.value[currentIndex + 1];
    if (nextQ) {
      void openQuestionDetail(nextQ);
    }
  } else {
    alert('已经是最后一题了');
  }
};

const saveNote = async () => {
  if (!selectedQuestion.value) {
    return;
  }
  noteSaving.value = true;
  try {
    const question = selectedQuestion.value;
    const remark = newNote.value.trim();
    const saved = await collectionApi.create({
      knowledge_id: question.id,
      remark: remark || null,
    });

    question.isFavorite = true;
    if (remark) {
      if (!question.notes) {
        question.notes = [];
      }
      question.notes.unshift({
        content: remark,
        time: String(saved.updated_at || saved.created_at || '').replace('T', ' ').slice(0, 19),
      });
      newNote.value = '';
    }
    showNoteInput.value = false;
    alert(remark ? '已收藏到个人题库，备注已保存' : '已收藏到个人题库');
  } catch (e) {
    console.error('[QuestionBank] save note failed', e);
    alert('保存失败，请稍后重试');
  } finally {
    noteSaving.value = false;
  }
};

// 新增方法
const openPracticeDetail = (item: any) => {
  console.log('打开专项练习:', item);
};

const openExamDetail = (exam: any) => {
  console.log('打开真题集:', exam);
};

const startMockExam = () => {
  router.push(PATHS.AI_MOCK_INTERVIEW);
};

const goMyNote = () => {
  router.push(PATHS.MY_NOTE);
};

onMounted(() => {
  // 初始化加载岗位分类标签（来自后端题库）
  knowledgeApi.positions()
    .then((res) => {
      const tabs = (res.items || [])
        .map((it) => {
          const label = String(it.label || '').trim();
          const value = String(it.value || '').trim();
          if (!label || !value) return null;
          return { label, value };
        })
        .filter((x): x is { label: string; value: string } => Boolean(x));
      categoryTabs.value = [{ label: '全部岗位', value: '' }, ...tabs];
    })
    .catch(() => {
      categoryTabs.value = [{ label: '全部岗位', value: '' }];
    });

  void loadQuestionList();
  void loadFavoriteList();
});
</script>

<style scoped>
/* ==================== 页面布局 ==================== */
.page-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #121820;
}

.main-content {
  flex: 1;
  margin-left: clamp(200px, 25vw, 320px);
  overflow-y: auto;
  height: 100vh;
  background: #121820;
}

.page-container {
  padding: 0 clamp(16px, 3vw, 24px);
  max-width: 1400px;
  margin: 0 auto;
}

.top-sticky-shell {
  position: sticky;
  top: 0;
  z-index: 40;
  background: #121820;
  border-bottom: 1px solid #22324a;
}

.top-sticky-inner {
  padding-top: clamp(20px, 4vh, 32px);
}

.page-content-body {
  padding-top: 14px;
}

.top-sticky-block {
  padding-bottom: 12px;
}

/* ==================== 顶部搜索栏 ==================== */
.top-search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0;
}

.top-search-bar .search-box {
  flex: 1;
  max-width: 500px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: #1b2431;
  border-radius: 12px;
  border: 1px solid #2a3441;
}

.top-search-bar .search-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.top-search-bar input {
  flex: 1;
  background: transparent;
  border: none;
  color: #fff;
  font-size: 14px;
  outline: none;
}

.top-search-bar input::placeholder {
  color: #5a6a7d;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* ==================== 分类标签 ==================== */
.category-tabs {
  display: flex;
  gap: 32px;
  margin-top: 12px;
  margin-bottom: 0;
  border-bottom: 0;
  padding-bottom: 0;
}

.tab-item {
  color: #8b9aae;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.3s;
  position: relative;
}

.tab-item:hover {
  color: #fff;
}

.tab-item.active {
  color: #3a7bc8;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 0;
  right: 0;
  height: 2px;
  background: #3a7bc8;
}

/* ==================== 统计卡片 ==================== */
.stats-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: #1b2431;
  border-radius: 12px;
  padding: 20px;
  position: relative;
  border: 1px solid #2a3441;
}

.stat-card .stat-label {
  font-size: 13px;
  color: #8b9aae;
  margin-bottom: 8px;
}

.stat-card .stat-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-card .number {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
}

.stat-card .number.green {
  color: #52c41a;
}

.stat-card .number.orange {
  color: #fa8c16;
}

.stat-card .total,
.stat-card .unit {
  font-size: 14px;
  color: #5a6a7d;
}

.stat-card .stat-icon {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card .stat-icon img {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

/* ==================== 主体内容区域 ==================== */
.main-body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
}

.right-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: sticky;
  top: 110px;
  align-self: start;
}

/* ==================== 通用卡片样式 ==================== */
.section-card {
  background: #1b2431;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #2a3441;
}

.question-list-card {
  min-height: 360px;
}

.empty-result {
  text-align: center;
  color: #8ea0b8;
  padding: 42px 12px;
  border: 1px dashed #30445d;
  border-radius: 10px;
  background: #1b2431;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}

.page-btn {
  border: 1px solid #32465f;
  background: #1a2a3d;
  color: #c9d8eb;
  border-radius: 8px;
  padding: 6px 12px;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.page-text {
  color: #9cb0c9;
  font-size: 13px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.view-all {
  font-size: 13px;
  color: #3a7bc8;
  text-decoration: none;
}

.filter-tabs {
  display: flex;
  gap: 16px;
}

.filter-tabs .tab {
  font-size: 13px;
  color: #5a6a7d;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 4px;
  transition: all 0.3s;
}

.filter-tabs .tab.active {
  background: #2a3441;
  color: #fff;
}

/* ==================== 专项练习 ==================== */
.practice-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.practice-card {
  background: #1b2431;
  border-radius: 10px;
  padding: 16px;
  display: flex;
  gap: 12px;
  cursor: pointer;
  border: 1px solid #2a3441;
}

.practice-card:hover {
  border-color: #3a7bc8;
}

.practice-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.practice-info {
  flex: 1;
  min-width: 0;
}

.practice-name {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.practice-desc {
  font-size: 12px;
  color: #5a6a7d;
  margin-bottom: 12px;
}

.practice-progress {
  margin-top: 8px;
}

.progress-bar {
  height: 4px;
  background: #2a3441;
  border-radius: 2px;
  margin-bottom: 8px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #5a6a7d;
}

.progress-text .count {
  color: #8b9aae;
}

/* ==================== 高频真题集 ==================== */
.exam-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exam-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #1b2431;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid #2a3441;
}

.exam-item:hover {
  border-color: #3a7bc8;
}

.exam-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}

.exam-info {
  flex: 1;
  min-width: 0;
}

.exam-title {
  font-size: 14px;
  color: #fff;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.exam-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.exam-meta .tag {
  color: #5a6a7d;
}

.exam-meta .difficulty {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
}

.exam-meta .difficulty.medium {
  background: rgba(250, 140, 22, 0.15);
  color: #fa8c16;
}

.exam-meta .difficulty.hard {
  background: rgba(255, 77, 79, 0.15);
  color: #ff4d4f;
}

.exam-meta .count {
  color: #5a6a7d;
}

.exam-arrow {
  color: #5a6a7d;
  font-size: 20px;
}

/* ==================== 模拟笔试 ==================== */
.mock-exam-card {
  text-align: center;
}

.mock-exam-card h3 {
  text-align: left;
  margin-bottom: 12px;
  color: #ffffff;
}

.section-card h3 {
  color: #ffffff;
}

.mock-desc {
  font-size: 13px;
  color: #8b9aae;
  text-align: left;
  line-height: 1.6;
  margin-bottom: 16px;
}

.mock-features {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
  text-align: left;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #8b9aae;
}

.feature-icon {
  width: 25px;
  height: 25px;
  object-fit: contain;
  font-size: 16px;
}

.mock-start-btn {
  width: 100%;
  padding: 12px 24px;
  background: #fff;
  color: #121820;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.mock-start-btn:hover {
  background: #e8e8e8;
}

/* ==================== 我的收藏 ==================== */
.favorite-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.favorite-empty {
  font-size: 13px;
  color: #8ea0b8;
  padding: 10px 2px;
}

.favorite-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
  padding: 12px;
  border-radius: 8px;
  background: #1b2431;
  border: 1px solid #2a3441;
}

.favorite-item:hover {
  background: #1b2431;
}

.favorite-item .star {
  color: #faad14;
  font-size: 14px;
  margin-top: 2px;
}

.favorite-content {
  flex: 1;
}

.favorite-title {
  font-size: 13px;
  color: #fff;
  margin-bottom: 4px;
  line-height: 1.4;
}

.favorite-tag {
  font-size: 11px;
  color: #5a6a7d;
}

.manage-link {
  font-size: 13px;
  color: #ffffff;
  text-decoration: none;
  display: block;
  text-align: center;
}

/* ==================== 弹窗 ==================== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: #1b2431;
  border-radius: 16px;
  width: 100%;
  max-width: 800px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid #2a3441;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #2a3441;
}

.modal-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #8b9aae;
  cursor: pointer;
  line-height: 1;
  transition: color 0.3s;
}

.close-btn:hover {
  color: #fff;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  max-height: calc(90vh - 80px);
}

/* 题目详情 */
.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.detail-tag {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
}

.detail-tag.position {
  background: rgba(58, 123, 200, 0.15);
  color: #3a7bc8;
}

.detail-tag.type {
  background: rgba(82, 196, 26, 0.15);
  color: #52c41a;
}

.detail-tag.difficulty {
  background: rgba(250, 140, 22, 0.15);
}

.detail-tag.difficulty.diff-1,
.detail-tag.difficulty.diff-2 {
  color: #52c41a;
}

.detail-tag.difficulty.diff-3 {
  color: #faad14;
}

.detail-tag.difficulty.diff-4,
.detail-tag.difficulty.diff-5 {
  color: #ff4d4f;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 16px;
  line-height: 1.5;
}

.detail-content {
  font-size: 15px;
  color: #c9d8eb;
  line-height: 1.8;
  margin-bottom: 24px;
  padding: 16px;
  background: #1b2431;
  border-radius: 8px;
  border: 1px solid #2a3441;
}

/* 答题区 */
.answer-section {
  margin-bottom: 24px;
}

.answer-section h4,
.hint-section h4,
.analysis-section h4,
.notes-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 12px;
}

.answer-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #2a3441;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  min-height: 120px;
  box-sizing: border-box;
  background: #1b2431;
  color: #fff;
}

.answer-input::placeholder {
  color: #5a6a7d;
}

.answer-input:focus {
  outline: none;
  border-color: #3a7bc8;
}

.answer-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.btn-primary {
  padding: 10px 24px;
  background: #3a7bc8;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-primary:hover {
  background: #2e6ab5;
}

.btn-secondary {
  padding: 10px 24px;
  background: transparent;
  color: #8b9aae;
  border: 1px solid #2a3441;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-secondary:hover {
  border-color: #3a7bc8;
  color: #3a7bc8;
}

.btn-icon {
  background: none;
  border: none;
  color: #3a7bc8;
  font-size: 13px;
  cursor: pointer;
  margin-left: 8px;
}

/* 提示区 */
.hint-section {
  margin-bottom: 24px;
  padding: 16px;
  background: rgba(250, 140, 22, 0.1);
  border: 1px solid rgba(250, 140, 22, 0.3);
  border-radius: 8px;
}

.hint-section p {
  margin: 0;
  color: #c9d8eb;
  line-height: 1.6;
}

/* 解析区 */
.analysis-section {
  margin-bottom: 24px;
  padding: 20px;
  background: rgba(82, 196, 26, 0.1);
  border: 1px solid rgba(82, 196, 26, 0.3);
  border-radius: 8px;
}

.analysis-content {
  margin-bottom: 16px;
}

.analysis-item {
  margin-bottom: 16px;
}

.analysis-item:last-child {
  margin-bottom: 0;
}

.analysis-item strong {
  display: block;
  color: #fff;
  margin-bottom: 6px;
}

.analysis-item p {
  margin: 0;
  color: #c9d8eb;
  line-height: 1.6;
}

.analysis-actions {
  display: flex;
  gap: 12px;
}

/* 笔记区 */
.notes-section {
  padding-top: 20px;
  border-top: 1px solid #2a3441;
}

.note-input-area {
  margin-bottom: 16px;
}

.note-input-area textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #2a3441;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  margin-bottom: 12px;
  box-sizing: border-box;
  background: #1b2431;
  color: #fff;
}

.note-input-area textarea::placeholder {
  color: #5a6a7d;
}

.note-input-area textarea:focus {
  outline: none;
  border-color: #3a7bc8;
}

.notes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-item {
  padding: 12px 16px;
  background: #1b2431;
  border-radius: 8px;
  border: 1px solid #2a3441;
}

.note-item p {
  margin: 0 0 6px 0;
  color: #c9d8eb;
  line-height: 1.5;
}

.note-time {
  font-size: 12px;
  color: #5a6a7d;
}

/* 批量刷题弹窗 */
.batch-desc {
  color: #8b9aae;
  margin-bottom: 20px;
}

.batch-config {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: #1b2431;
  border-radius: 8px;
  border: 1px solid #2a3441;
}

.count-input {
  width: 80px;
  padding: 8px 12px;
  border: 1px solid #2a3441;
  border-radius: 6px;
  font-size: 14px;
  text-align: center;
  background: #1b2431;
  color: #fff;
}

.batch-preview h4 {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 12px;
}

.batch-questions-list {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #2a3441;
  border-radius: 8px;
  margin-bottom: 20px;
  background: #1b2431;
}

.batch-question-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #2a3441;
}

.batch-question-item:last-child {
  border-bottom: none;
}

.batch-index {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #3a7bc8;
  color: white;
  border-radius: 50%;
  font-size: 12px;
  flex-shrink: 0;
}

.batch-title {
  flex: 1;
  font-size: 14px;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.batch-difficulty {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
}

.batch-difficulty.diff-1,
.batch-difficulty.diff-2 {
  background: rgba(82, 196, 26, 0.15);
  color: #52c41a;
}

.batch-difficulty.diff-3 {
  background: rgba(250, 140, 22, 0.15);
  color: #faad14;
}

.batch-difficulty.diff-4,
.batch-difficulty.diff-5 {
  background: rgba(255, 77, 79, 0.15);
  color: #ff4d4f;
}

.batch-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 1200px) {
  .main-body {
    grid-template-columns: 1fr;
  }

  .right-content {
    order: -1;
    position: static;
  }
}

@media (max-width: 768px) {
  .stats-section {
    grid-template-columns: 1fr;
  }

  .practice-grid {
    grid-template-columns: 1fr;
  }

  .category-tabs {
    overflow-x: auto;
    gap: 20px;
  }

  .top-search-bar {
    flex-direction: column;
    gap: 16px;
  }

  .top-search-bar .search-box {
    max-width: 100%;
    width: 100%;
  }
}
</style>
