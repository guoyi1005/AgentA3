<template>
	<div class="page-layout">
		<Sidebar />
		<main class="main-content">
			<div class="report-shell">
				<header class="report-header">
					<div class="report-title-wrap">
						<span class="title-icon">▣</span>
						<h1>AI 面试评估报告</h1>
					</div>
					<button class="ghost-btn" @click="goBack">返回历史</button>
				</header>

				<section class="report-card" v-if="loading">
					<div class="state-text">报告加载中...</div>
				</section>

				<section class="report-card" v-else-if="errorText">
					<div class="state-text error">{{ errorText }}</div>
					<button class="action-btn" @click="loadReport">重新加载</button>
				</section>

				<template v-else-if="report">
					<section class="score-zone">
						<div class="score-value">{{ report.score }}</div>
						<div class="score-sub">综合面试评分 · 极具潜力</div>
					</section>

					<section class="report-card">
						<h2 class="card-title">核心结论</h2>
						<p class="conclusion">{{ report.core_conclusion || '暂无核心结论' }}</p>
					</section>

					<section class="dual-grid">
						<article class="report-card report-card-success">
							<h3 class="card-title">优势亮点</h3>
							<div class="tag-list">
								<span class="tag success" v-for="(item, idx) in strengthsList" :key="`s-${idx}`">{{ item }}</span>
							</div>
						</article>

						<article class="report-card report-card-warning">
							<h3 class="card-title">评估不足</h3>
							<div class="tag-list">
								<span class="tag warn" v-for="(item, idx) in weaknessesList" :key="`w-${idx}`">{{ item }}</span>
							</div>
						</article>
					</section>

					<section class="report-card">
						<h3 class="card-title">改进建议</h3>
						<ol class="improve-list">
							<li v-for="(item, idx) in improvementsList" :key="`i-${idx}`">
								<span class="order">{{ idx + 1 }}</span>
								<p>{{ item }}</p>
							</li>
						</ol>
					</section>

					<section class="report-footer">
						<button class="action-btn" @click="goBack">前往深度复盘</button>
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

const loading = ref(false);
const errorText = ref('');
const report = ref<InterviewSummaryReport | null>(null);

const conversationId = computed(() => {
	const q = new URLSearchParams(window.location.search);
	return (q.get('conversation_id') || q.get('cid') || localStorage.getItem('conversation_id') || '').trim();
});

function splitTextToList(value: string | null | undefined): string[] {
	const raw = String(value || '').trim();
	if (!raw) return ['暂无数据'];
	const lines = raw
		.replace(/\r\n/g, '\n')
		.split(/\n|；|;|。/)
		.map((x) => x.trim())
		.filter(Boolean);
	if (lines.length === 0) return ['暂无数据'];
	return lines.slice(0, 8);
}

const strengthsList = computed(() => splitTextToList(report.value?.strengths));
const weaknessesList = computed(() => splitTextToList(report.value?.weaknesses));
const improvementsList = computed(() => splitTextToList(report.value?.improvements));

async function loadReport() {
	errorText.value = '';
	report.value = null;
	const convId = conversationId.value;
	if (!convId) {
		errorText.value = '缺少会话ID，无法加载AI报告';
		return;
	}
	loading.value = true;
	try {
		const res = await conversationApi.summarizeInterview({ conversation_id: convId });
		report.value = res;
	} catch (e: any) {
		errorText.value = e?.message || '加载报告失败';
	} finally {
		loading.value = false;
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
	overflow: hidden;
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

.report-footer {
	display: flex;
	justify-content: center;
	padding: 8px 0 6px;
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
	padding: 18px 8px;
	text-align: center;
	color: #bddfff;
}

.state-text.error {
	color: #ffb0b0;
}

@media (max-width: 980px) {
	.main-content {
		margin-left: 0;
		overflow: auto;
	}

	.dual-grid {
		grid-template-columns: 1fr;
	}

	.report-header {
		flex-direction: column;
		align-items: flex-start;
		gap: 12px;
	}

	.improve-list {
		grid-template-columns: 1fr;
	}
}
</style>
