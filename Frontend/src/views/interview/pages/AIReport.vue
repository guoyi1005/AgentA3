<template>
	<div class="page-layout">
		<Sidebar />
		<main class="main-content">
			<div class="report-shell">
				<header class="report-header">
					<div class="report-title-wrap">
						<span class="title-icon">▣</span>
						<div class="title-block">
							<h1>AI 面试评估报告</h1>
							<p class="report-meta" v-if="report">
								<span>岗位：{{ report.job_role || '未填写' }}</span>
								<span>面试时间：{{ interviewTime }}</span>
								<span>面试时长：{{ durationText }}</span>
							</p>
						</div>
					</div>
					<button class="ghost-btn" @click="goBack">返回历史</button>
				</header>

				<section class="report-card" v-if="loading">
					<div class="state-text">{{ loadingText }}</div>
					<p class="state-hint" v-if="loadingHint">{{ loadingHint }}</p>
				</section>

				<section class="report-card" v-else-if="errorText">
					<div class="state-text error">{{ errorTitle }}</div>
					<p class="state-hint">{{ errorText }}</p>
					<div class="state-actions">
						<button class="action-btn" v-if="canRegenerate" @click="generateReport">重新生成评估报告</button>
						<button class="ghost-btn" v-else-if="canReload" @click="loadReport">重新加载</button>
						<button class="ghost-btn" @click="goBack">返回历史</button>
					</div>
				</section>

				<template v-else-if="report">
					<section class="score-zone">
						<div class="score-value">{{ report.score }}</div>
						<div class="score-sub">综合面试评分 · {{ report.job_role || '通用岗位' }}</div>
					</section>

					<section class="report-card" v-if="dimensionCards.length">
						<h2 class="card-title">维度评分</h2>
						<div class="dimension-grid">
							<article class="dimension-card" v-for="dim in dimensionCards" :key="dim.key">
								<div class="dimension-head">
									<span class="dimension-name">{{ dim.label }}</span>
									<span class="dimension-score">{{ dim.score }}</span>
								</div>
								<div class="dimension-bar">
									<span :style="{ width: `${dim.score}%` }"></span>
								</div>
								<p class="dimension-comment">{{ dim.comment || '暂无点评' }}</p>
							</article>
						</div>
					</section>

					<section class="report-card">
						<h2 class="card-title">总体评价</h2>
						<p class="conclusion">{{ report.core_conclusion || '暂无总体评价' }}</p>
					</section>

					<section class="dual-grid">
						<article class="report-card report-card-success">
							<h3 class="card-title">优势亮点</h3>
							<div class="tag-list">
								<span class="tag success" v-for="(item, idx) in strengthsList" :key="`s-${idx}`">{{ item }}</span>
							</div>
						</article>

						<article class="report-card report-card-warning">
							<h3 class="card-title">待提升</h3>
							<div class="tag-list">
								<span class="tag warn" v-for="(item, idx) in weaknessesList" :key="`w-${idx}`">{{ item }}</span>
							</div>
						</article>
					</section>

					<section class="report-card">
						<h3 class="card-title">AI 改进建议</h3>
						<ol class="improve-list">
							<li v-for="(item, idx) in improvementsList" :key="`i-${idx}`">
								<span class="order">{{ idx + 1 }}</span>
								<p>{{ item }}</p>
							</li>
						</ol>
					</section>

					<section class="report-card">
						<h3 class="card-title">面试问答分析</h3>
						<div class="qa-list" v-if="questionList.length">
							<article class="qa-card" v-for="(item, idx) in questionList" :key="`q-${idx}`">
								<div class="qa-head">
									<span class="qa-index">第 {{ idx + 1 }} 题</span>
									<span class="qa-score">{{ item.score }} / 100</span>
								</div>
								<p class="qa-question">{{ item.question }}</p>
								<div class="qa-block">
									<span class="qa-label">我的回答</span>
									<p class="qa-answer">{{ item.answer || '（本题没有作答记录）' }}</p>
								</div>
								<div class="qa-block">
									<span class="qa-label">AI 点评</span>
									<p class="qa-comment">{{ item.comment || item.answer_summary || '暂无点评' }}</p>
								</div>
							</article>
						</div>
						<p class="state-hint" v-else>本次面试没有可用于逐题分析的问答记录</p>
					</section>

					<section class="report-footer">
						<button class="action-btn" @click="goBack">返回历史</button>
					</section>
				</template>
			</div>
		</main>
	</div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router';
import Sidebar from '../components/Sidebar.vue';
import { conversationApi, type InterviewSummaryReport } from '../api/conversation';
import { PATHS } from '../routes/paths';

const router = useRouter()

const DIMENSION_LABELS: Record<string, string> = {
	professionalKnowledge: '专业知识',
	technicalDepth: '技术深度',
	expression: '表达能力',
	logicalThinking: '逻辑思维',
	jobMatching: '岗位匹配度',
};
const DIMENSION_ORDER = ['professionalKnowledge', 'technicalDepth', 'expression', 'logicalThinking', 'jobMatching'];

/** 这些错误可以通过“重新生成评估报告”重试，其余情况只提供重新加载或返回。 */
const REGENERABLE_CODES = ['AI_REPORT_GENERATION_FAILED', 'AI_NOT_CONFIGURED', 'AI_REPORT_FORMAT_ERROR'];

const loading = ref(false);
const loadingText = ref('报告加载中...');
const loadingHint = ref('');
const errorCode = ref('');
const errorText = ref('');
const report = ref<InterviewSummaryReport | null>(null);

const conversationId = computed(() => {
	const q = new URLSearchParams(window.location.search);
	return (q.get('conversation_id') || q.get('cid') || localStorage.getItem('conversation_id') || '').trim();
});

function splitTextToList(value: string | null | undefined, max = 5): string[] {
	const raw = String(value || '').trim();
	if (!raw) return [];
	const lines = raw
		.replace(/\r\n/g, '\n')
		.split(/\n|；|;/)
		.map((x) => x.trim())
		.filter(Boolean);
	return lines.slice(0, max);
}

const strengthsList = computed(() => splitTextToList(report.value?.strengths));
const weaknessesList = computed(() => splitTextToList(report.value?.weaknesses));
const improvementsList = computed(() => splitTextToList(report.value?.improvements));

const dimensionCards = computed(() => {
	const dimensions = report.value?.dimensions || {};
	return DIMENSION_ORDER
		.filter((key) => Boolean(dimensions[key]))
		.map((key) => ({
			key,
			label: DIMENSION_LABELS[key] || key,
			score: Number(dimensions[key]?.score ?? 0),
			comment: dimensions[key]?.comment || '',
		}));
});

const questionList = computed(() => report.value?.question_analysis || []);

function formatDateTime(value?: string | null): string {
	if (!value) return '--';
	const parsed = new Date(value);
	if (Number.isNaN(parsed.getTime())) return value;
	const pad = (n: number) => String(n).padStart(2, '0');
	return `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())} ${pad(parsed.getHours())}:${pad(parsed.getMinutes())}`;
}

const interviewTime = computed(() => formatDateTime(report.value?.started_at || report.value?.created_at));

const durationText = computed(() => {
	const seconds = report.value?.duration_seconds;
	if (seconds === null || seconds === undefined) return '--';
	const total = Math.max(0, Math.round(Number(seconds)));
	const minutes = Math.floor(total / 60);
	const rest = total % 60;
	return minutes > 0 ? `${minutes} 分 ${rest} 秒` : `${rest} 秒`;
});

const errorTitle = computed(() => {
	if (errorCode.value === 'CONVERSATION_NOT_FOUND') return '未找到本次面试记录';
	if (REGENERABLE_CODES.includes(errorCode.value)) return '评估报告生成失败';
	return '报告加载失败';
});

const canRegenerate = computed(() => REGENERABLE_CODES.includes(errorCode.value));
const canReload = computed(() => !canRegenerate.value && errorCode.value === 'REQUEST_FAILED');

function setError(code: string, message: string) {
	errorCode.value = code;
	errorText.value = message;
}

function applyResponse(res: any) {
	if (res?.success && res.report) {
		report.value = res.report as InterviewSummaryReport;
		return;
	}
	const code = String(res?.code || 'UNKNOWN');
	if (code === 'REPORT_NOT_GENERATED') {
		// 面试记录存在但报告还没生成：自动触发一次生成，并显示生成中状态。
		void generateReport();
		return;
	}
	if (code === 'CONVERSATION_NOT_FOUND' || code === 'MISSING_CONVERSATION_ID') {
		setError('CONVERSATION_NOT_FOUND', '未找到本次面试记录');
		return;
	}
	setError(code, String(res?.message || 'AI 评估报告生成失败，请稍后重试'));
}

async function loadReport() {
	errorCode.value = '';
	errorText.value = '';
	report.value = null;
	const convId = conversationId.value;
	if (!convId) {
		setError('CONVERSATION_NOT_FOUND', '未找到本次面试记录');
		return;
	}
	loading.value = true;
	loadingText.value = '报告加载中...';
	loadingHint.value = '';
	try {
		applyResponse(await conversationApi.getEvaluationReport(convId));
	} catch (e: any) {
		setError('REQUEST_FAILED', e?.message || '报告加载失败，请稍后重试');
	} finally {
		loading.value = false;
	}
}

async function generateReport() {
	const convId = conversationId.value;
	if (!convId) {
		setError('CONVERSATION_NOT_FOUND', '未找到本次面试记录');
		return;
	}
	errorCode.value = '';
	errorText.value = '';
	report.value = null;
	loading.value = true;
	loadingText.value = 'AI 正在生成面试评估报告……';
	loadingHint.value = '正在依据本次面试的真实问答记录生成评估结果';
	try {
		const res = await conversationApi.summarizeInterview({ conversation_id: convId });
		if (res?.success && res.report) {
			report.value = res.report as InterviewSummaryReport;
		} else {
			applyResponse(res);
		}
	} catch (e: any) {
		setError('REQUEST_FAILED', e?.message || '评估报告生成失败，请稍后重试');
	} finally {
		loading.value = false;
		loadingHint.value = '';
	}
}

function goBack() {
	router.push(PATHS.AI_MOCK_INTERVIEW);
}

onMounted(() => {
	loadReport();
});
</script>

<style scoped>
.page-layout {
	display: flex;
	height: 100vh;
	overflow: hidden;
	background:
		radial-gradient(65% 50% at 55% 0%, rgba(5, 152, 186, 0.18), transparent 60%),
		radial-gradient(50% 50% at 15% 10%, rgba(23, 80, 164, 0.12), transparent 70%),
		#050c1a;
}

.main-content {
	flex: 1;
	margin-left: clamp(200px, 25vw, 320px);
	height: 100vh;
	padding: clamp(12px, 2.4vh, 22px) clamp(16px, 3.5vw, 34px);
	overflow: hidden;
}

.report-shell {
	max-width: 1120px;
	margin: 0 auto;
	color: #d8ecff;
	height: 100%;
	overflow-y: auto;
	padding-right: 6px;
}

.report-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px;
	padding-bottom: 10px;
	border-bottom: 1px solid rgba(94, 138, 197, 0.22);
}

.report-title-wrap {
	display: flex;
	align-items: center;
	gap: 10px;
}

.title-icon {
	width: 28px;
	height: 28px;
	border-radius: 8px;
	display: grid;
	place-items: center;
	color: #06e4f0;
	background: rgba(7, 198, 216, 0.16);
	box-shadow: 0 0 18px rgba(7, 198, 216, 0.4);
}

.report-header h1 {
	margin: 0;
	font-size: clamp(20px, 2.4vw, 30px);
	letter-spacing: 0.6px;
	color: #e8f7ff;
}

.report-meta {
	margin: 6px 0 0;
	display: flex;
	flex-wrap: wrap;
	gap: 14px;
	font-size: 13px;
	color: rgba(163, 205, 240, 0.85);
}

.ghost-btn,
.action-btn {
	border: 1px solid rgba(78, 128, 193, 0.45);
	background: rgba(10, 26, 56, 0.7);
	color: #a7ddff;
	border-radius: 14px;
	padding: 10px 16px;
	cursor: pointer;
}

.ghost-btn:hover,
.action-btn:hover {
	border-color: rgba(15, 233, 245, 0.6);
	color: #e0fcff;
}

.score-zone {
	text-align: center;
	padding: 12px 10px 10px;
}

.score-value {
	font-size: clamp(52px, 7vw, 96px);
	line-height: 0.95;
	color: #0fe8f4;
	text-shadow: 0 0 24px rgba(15, 232, 244, 0.55);
	font-weight: 700;
}

.score-sub {
	margin-top: 10px;
	color: rgba(185, 222, 255, 0.84);
	letter-spacing: 0.4px;
}

.report-card {
	border: 1px solid rgba(66, 114, 184, 0.36);
	background: linear-gradient(170deg, rgba(6, 20, 45, 0.9), rgba(4, 14, 34, 0.76));
	border-radius: 18px;
	padding: 14px;
	margin-bottom: 12px;
	box-shadow: inset 0 1px 0 rgba(136, 179, 255, 0.08);
}

.card-title {
	margin: 0 0 12px;
	font-size: 18px;
	color: #e4f5ff;
}

.conclusion {
	margin: 0;
	color: rgba(214, 234, 255, 0.9);
	line-height: 1.7;
}

.dimension-grid {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
	gap: 12px;
}

.dimension-card {
	border: 1px solid rgba(81, 132, 198, 0.24);
	border-radius: 14px;
	padding: 12px;
	background: rgba(5, 18, 44, 0.56);
}

.dimension-head {
	display: flex;
	align-items: baseline;
	justify-content: space-between;
	gap: 8px;
}

.dimension-name {
	color: #d9ecff;
	font-size: 14px;
}

.dimension-score {
	font-size: 22px;
	font-weight: 700;
	color: #0fe8f4;
	text-shadow: 0 0 16px rgba(15, 232, 244, 0.4);
}

.dimension-bar {
	margin: 8px 0 6px;
	height: 6px;
	border-radius: 999px;
	background: rgba(37, 74, 128, 0.5);
	overflow: hidden;
}

.dimension-bar span {
	display: block;
	height: 100%;
	border-radius: 999px;
	background: linear-gradient(90deg, #06d7e6, #3d89ff);
}

.dimension-comment {
	margin: 0;
	font-size: 13px;
	line-height: 1.6;
	color: rgba(190, 219, 245, 0.85);
}

.dual-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 14px;
}

.report-card-success {
	border-color: rgba(25, 214, 172, 0.42);
}

.report-card-warning {
	border-color: rgba(235, 176, 21, 0.4);
}

.tag-list {
	display: flex;
	flex-wrap: wrap;
	gap: 10px;
}

.tag {
	display: inline-flex;
	align-items: center;
	border-radius: 999px;
	padding: 7px 12px;
	font-size: 13px;
	letter-spacing: 0.2px;
}

.tag.success {
	color: #33f6c5;
	border: 1px solid rgba(36, 216, 160, 0.35);
	background: rgba(20, 103, 86, 0.25);
}

.tag.warn {
	color: #ffd16a;
	border: 1px solid rgba(224, 165, 29, 0.42);
	background: rgba(128, 92, 17, 0.22);
}

.improve-list {
	margin: 0;
	padding: 0;
	list-style: none;
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 10px;
}

.improve-list li {
	display: grid;
	grid-template-columns: 34px 1fr;
	gap: 10px;
	align-items: flex-start;
	padding: 10px;
	border-radius: 12px;
	border: 1px solid rgba(81, 132, 198, 0.24);
	background: rgba(5, 18, 44, 0.56);
}

.order {
	width: 26px;
	height: 26px;
	border-radius: 999px;
	display: grid;
	place-items: center;
	color: #7bc4ff;
	font-weight: 600;
	background: rgba(44, 108, 206, 0.35);
}

.improve-list p {
	margin: 2px 0 0;
	line-height: 1.65;
	color: rgba(214, 234, 255, 0.9);
}

.qa-list {
	display: grid;
	gap: 12px;
}

.qa-card {
	border: 1px solid rgba(81, 132, 198, 0.24);
	border-radius: 14px;
	padding: 12px;
	background: rgba(5, 18, 44, 0.56);
}

.qa-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 10px;
	margin-bottom: 8px;
}

.qa-index {
	font-size: 13px;
	color: #7bc4ff;
	letter-spacing: 0.4px;
}

.qa-score {
	font-size: 14px;
	font-weight: 600;
	color: #0fe8f4;
}

.qa-question {
	margin: 0 0 10px;
	line-height: 1.65;
	color: #e4f5ff;
}

.qa-block {
	margin-top: 8px;
}

.qa-label {
	display: inline-block;
	margin-bottom: 4px;
	font-size: 12px;
	letter-spacing: 0.4px;
	color: rgba(148, 196, 235, 0.9);
}

.qa-answer,
.qa-comment {
	margin: 0;
	line-height: 1.7;
	color: rgba(214, 234, 255, 0.9);
	white-space: pre-wrap;
	word-break: break-word;
}

.qa-comment {
	color: rgba(198, 226, 250, 0.92);
}

.report-footer {
	display: flex;
	justify-content: center;
	padding: 8px 0 18px;
}

.action-btn {
	min-width: 180px;
	border: 0;
	color: #052337;
	font-weight: 600;
	background: linear-gradient(92deg, #06d7e6, #3d89ff);
	box-shadow: 0 10px 26px rgba(27, 154, 255, 0.33);
}

.state-text {
	padding: 18px 8px 6px;
	text-align: center;
	color: #bddfff;
}

.state-text.error {
	color: #ffb0b0;
}

.state-hint {
	margin: 0 0 12px;
	text-align: center;
	font-size: 13px;
	color: rgba(178, 212, 240, 0.85);
}

.state-actions {
	display: flex;
	flex-wrap: wrap;
	gap: 10px;
	justify-content: center;
	padding-bottom: 6px;
}

@media (max-width: 980px) {
	.main-content {
		margin-left: 0;
		overflow: auto;
	}

	.dual-grid,
	.improve-list {
		grid-template-columns: 1fr;
	}

	.report-header {
		flex-direction: column;
		align-items: flex-start;
		gap: 12px;
	}
}
</style>
