<template>
	<div class="page-layout">
		<Sidebar />
		<main class="main-content">
			<div class="config-shell">
				<header class="topbar">
					<div class="brand-wrap">
						<div class="brand-main">Warp Sync</div>
						<div class="brand-sub">折跃同步</div>
					</div>
					<div class="top-title">AI面试大屏配置</div>
					<div class="top-user"></div>
				</header>

				<section class="content">
					<aside class="left-panel">
						<h2>参数配置</h2>
						<p class="left-sub">设置您的AI模拟面试偏好</p>

						<div class="field-group">
							<label>目标职位</label>
							<input v-model="jobRole" placeholder="请在此粘贴职位描述或具体要求..." @input="jobRoleError = ''" />
							<p v-if="jobRoleError" class="field-error" role="alert">{{ jobRoleError }}</p>
						</div>

						<div class="field-group">
							<label>面试模式选择</label>
							<div class="mode-grid">
								<button
									type="button"
									class="mode-btn"
									:class="{ active: config.interview_mode === 'full' }"
									@click="config.interview_mode = 'full'"
								>
									全流程面试
								</button>
								<button
									type="button"
									class="mode-btn"
									:class="{ active: config.interview_mode === 'focused' }"
									@click="config.interview_mode = 'focused'"
								>
									专项强化
								</button>
							</div>
						</div>

						<div class="field-group" v-if="config.interview_mode === 'focused'">
							<label>专项标签</label>
							<input v-model="config.focus_tags" placeholder="例如：系统设计, Redis, MySQL" />
						</div>

						<div class="field-group">
							<label>面试官风格</label>
							<select v-model="config.interviewer_persona">
								<option value="neutral">中立理性</option>
								<option value="friendly">亲和鼓励</option>
								<option value="challenging">严格犀利</option>
								<option value="pragmatic">务实追问</option>
							</select>
						</div>

						<div class="field-group">
							<label>面试难度（1-5）</label>
							<input v-model.number="config.difficulty" type="number" min="1" max="5" />
						</div>

						<div class="field-group">
							<label>职位详情</label>
							<textarea
								v-model="config.requirements"
								rows="6"
								placeholder="请在此粘贴职位描述或具体要求..."
							></textarea>
						</div>

						<div class="field-group">
							<label>备注（可选）</label>
							<input v-model="config.remark" placeholder="可选" />
						</div>
					</aside>

					<section class="hero-panel">
						<h3 class="picker-title">请选择您的面试官</h3>

						<div
							class="carousel-stage"
							@mouseenter="pauseAutoRotate"
							@mouseleave="resumeAutoRotate"
						>
							<button
								class="carousel-nav"
								type="button"
								aria-label="上一位面试官"
								@click="selectPreviousInterviewer"
							>
								<img class="carousel-nav-icon" :src="arrowNavImg" alt="" aria-hidden="true" />
							</button>

							<div class="hero-image-wrap">
								<img
									:key="selectedInterviewer.id"
									:src="selectedInterviewer.image"
									:alt="selectedInterviewer.name"
									class="hero-image"
								/>
							</div>

							<button
								class="carousel-nav is-next"
								type="button"
								aria-label="下一位面试官"
								@click="selectNextInterviewer"
							>
								<img class="carousel-nav-icon" :src="arrowNavImg" alt="" aria-hidden="true" />
							</button>
						</div>

						<div class="thumb-row" aria-label="全部面试官">
							<button
								v-for="item in interviewers"
								:key="item.id"
								type="button"
								class="thumb-btn"
								:class="{ active: selectedInterviewerId === item.id }"
								@click="selectInterviewer(item.id)"
								:aria-label="`选择${item.name}`"
							>
								<img :src="item.image" :alt="item.name" />
							</button>
						</div>

						<button class="start-btn" type="button" :disabled="starting" @click="handleStart">
							<span v-if="starting">开始中...</span>
							<span v-else>开始面试 ▶</span>
						</button>
					</section>
				</section>
			</div>
		</main>
	</div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router';
import Sidebar from '../components/Sidebar.vue';
import { PATHS } from '../routes/paths';
import arrowNavImg from '@/assets/interview/arrow-nav.svg';
import interviewerAnranImg from '@/assets/interview/interviewer-anran.png';
import interviewerMingxuanImg from '@/assets/interview/interviewer-mingxuan.png';
import interviewerMingxuanAltImg from '@/assets/interview/interviewer-mingxuan-alt.png';
import interviewerKexinImg from '@/assets/interview/interviewer-kexin.png';
import interviewerLanyiImg from '@/assets/interview/interviewer-lanyi.png';

const router = useRouter()
const CONFIG_STORAGE_KEY = 'interview_config_draft';
const INTERVIEWER_STORAGE_KEY = 'selected_interviewer';
const AUTO_ROTATE_MS = 5000;

type InterviewerId = 'anran' | 'mingxuan' | 'kexin' | 'lanyi' | 'mingxuan_alt';
type InterviewerPersona = 'neutral' | 'friendly' | 'challenging' | 'pragmatic';

type InterviewerOption = {
	id: InterviewerId;
	name: string;
	image: string;
	defaultPersona: InterviewerPersona;
};

const interviewers: InterviewerOption[] = [
	{
		id: 'anran',
		name: '安然',
		image: interviewerAnranImg,
		defaultPersona: 'friendly',
	},
	{
		id: 'mingxuan',
		name: '明轩',
		image: interviewerMingxuanImg,
		defaultPersona: 'challenging',
	},
	{
		id: 'mingxuan_alt',
		name: '明轩（版本2）',
		image: interviewerMingxuanAltImg,
		defaultPersona: 'challenging',
	},
	{
		id: 'kexin',
		name: '可欣',
		image: interviewerKexinImg,
		defaultPersona: 'friendly',
	},
	{
		id: 'lanyi',
		name: '蓝衣',
		image: interviewerLanyiImg,
		defaultPersona: 'neutral',
	},
];

const jobRole = ref(localStorage.getItem('job_role') || '');
const jobRoleError = ref('');
const starting = ref(false);
const selectedInterviewerId = ref<InterviewerId>('anran');

const config = ref({
	interview_mode: 'full' as 'full' | 'focused',
	interviewer_persona: 'neutral' as InterviewerPersona,
	focus_tags: '',
	requirements: '',
	difficulty: 3,
	remark: '',
	status: 1,
});

function isInterviewerId(value: unknown): value is InterviewerId {
	return typeof value === 'string' && interviewers.some((item) => item.id === value);
}

try {
	const raw = localStorage.getItem(CONFIG_STORAGE_KEY);
	if (raw) {
		const parsed = JSON.parse(raw);
		if (isInterviewerId(parsed?.interviewer_profile)) {
			selectedInterviewerId.value = parsed.interviewer_profile;
		}
		config.value = {
			...config.value,
			...parsed,
			difficulty: Math.min(5, Math.max(1, Number(parsed?.difficulty || 3))),
		};
	}
} catch {
	// ignore stale value
}

const interviewerFromStorage = localStorage.getItem(INTERVIEWER_STORAGE_KEY);
if (isInterviewerId(interviewerFromStorage)) {
	selectedInterviewerId.value = interviewerFromStorage;
}

const selectedInterviewer = computed<InterviewerOption>(
	() => interviewers.find((item) => item.id === selectedInterviewerId.value) ?? interviewers[0]
);

const selectInterviewer = (id: InterviewerId) => {
	selectedInterviewerId.value = id;
	const selected = interviewers.find((item) => item.id === id);
	if (selected) {
		config.value.interviewer_persona = selected.defaultPersona;
	}
};

const selectPreviousInterviewer = () => {
	const currentIndex = interviewers.findIndex((item) => item.id === selectedInterviewerId.value);
	const nextIndex = currentIndex <= 0 ? interviewers.length - 1 : currentIndex - 1;
	const target = interviewers[nextIndex] ?? interviewers[0];
	selectInterviewer(target.id);
	resumeAutoRotate();
};

const selectNextInterviewer = () => {
	const currentIndex = interviewers.findIndex((item) => item.id === selectedInterviewerId.value);
	const nextIndex = currentIndex >= interviewers.length - 1 ? 0 : currentIndex + 1;
	const target = interviewers[nextIndex] ?? interviewers[0];
	selectInterviewer(target.id);
	resumeAutoRotate();
};

let autoRotateTimerId: number | null = null;

const clearAutoRotate = () => {
	if (autoRotateTimerId !== null) {
		window.clearInterval(autoRotateTimerId);
		autoRotateTimerId = null;
	}
};

const resumeAutoRotate = () => {
	clearAutoRotate();
	autoRotateTimerId = window.setInterval(() => {
		selectNextInterviewer();
	}, AUTO_ROTATE_MS);
};

const pauseAutoRotate = () => {
	clearAutoRotate();
};

resumeAutoRotate();

onBeforeUnmount(() => {
	clearAutoRotate();
});

const handleStart = async () => {
	if (!jobRole.value.trim()) {
		jobRoleError.value = '请先填写目标职位';
		return;
	}
	jobRoleError.value = '';
	starting.value = true;
	try {
		config.value.difficulty = Math.min(5, Math.max(1, Number(config.value.difficulty || 3)));
		localStorage.setItem('job_role', jobRole.value.trim());
		localStorage.setItem(INTERVIEWER_STORAGE_KEY, selectedInterviewerId.value);
		localStorage.setItem(
			CONFIG_STORAGE_KEY,
			JSON.stringify({
				...config.value,
				interviewer_profile: selectedInterviewerId.value,
				interviewer_name: selectedInterviewer.value.name,
			})
		);

		router.push(PATHS.AI_CHAT);
	} finally {
		starting.value = false;
	}
};
</script>

<style scoped>
.page-layout {
	display: flex;
	min-height: 100vh;
	background: radial-gradient(circle at 70% 20%, #1e2a58 0%, #081226 52%, #050b18 100%);
}

.main-content {
	flex: 1;
	margin-left: clamp(200px, 25vw, 320px);
	min-height: 100vh;
	color: #e7efff;
}

.config-shell {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
}

.topbar {
	height: 72px;
	display: grid;
	grid-template-columns: 220px 1fr 220px;
	align-items: center;
	padding: 0 24px;
	border-bottom: 1px solid rgba(106, 142, 197, 0.28);
	background: rgba(8, 16, 34, 0.75);
	backdrop-filter: blur(8px);
}

.brand-wrap {
	display: flex;
	flex-direction: column;
}

.brand-main {
	font-size: 24px;
	line-height: 1;
	font-weight: 700;
	letter-spacing: 0.5px;
}

.brand-sub {
	margin-top: 6px;
	font-size: 12px;
	color: #86a1cb;
}

.top-title {
	text-align: center;
	font-size: 20px;
	font-weight: 700;
}

.content {
	flex: 1;
	display: grid;
	grid-template-columns: minmax(320px, 460px) 1fr;
}

.left-panel {
	border-right: 1px solid rgba(106, 142, 197, 0.25);
	padding: 26px 24px 24px;
	background: rgba(10, 18, 35, 0.65);
	overflow: auto;
}

.left-panel h2 {
	margin: 0;
	font-size: 34px;
	font-weight: 800;
}

.left-sub {
	margin: 8px 0 24px;
	color: #9cb3d8;
	font-size: 14px;
}

.field-group {
	margin-bottom: 16px;
}

.field-group label {
	display: block;
	margin-bottom: 8px;
	font-size: 13px;
	color: #aac1e6;
}

input,
select,
textarea {
	width: 100%;
	box-sizing: border-box;
	border: 1px solid #23426c;
	background: #081222;
	color: #eaf2ff;
	border-radius: 14px;
	padding: 12px 14px;
	font-size: 14px;
	outline: none;
}

input:focus,
select:focus,
textarea:focus {
	border-color: #4a89ff;
	box-shadow: 0 0 0 2px rgba(74, 137, 255, 0.25);
}

textarea {
	min-height: 132px;
	resize: vertical;
}

.mode-grid {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 10px;
}

.mode-btn {
	height: 62px;
	border-radius: 14px;
	border: 1px solid #2c4f80;
	background: #17263f;
	color: #d5e4ff;
	cursor: pointer;
	font-weight: 700;
}

.mode-btn.active {
	border-color: #3e8bff;
	background: #102e58;
	box-shadow: inset 0 0 0 1px rgba(77, 157, 255, 0.5);
}

.hero-panel {
	padding: 36px 34px 30px;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.picker-title {
	margin: 0 0 20px;
	font-size: clamp(32px, 3.5vw, 52px);
	font-weight: 800;
	line-height: 1.1;
}

.carousel-stage {
	position: relative;
	width: min(960px, 100%);
	display: flex;
	align-items: center;
	justify-content: center;
}

.carousel-nav {
	position: absolute;
	top: 50%;
	transform: translateY(-50%);
	height: 50px;
	width: 50px;
	border-radius: 10px;
	border: 1px solid rgba(132, 177, 255, 0.42);
	background: rgba(8, 26, 56, 0.8);
	padding: 0;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	cursor: pointer;
	z-index: 3;
	transition: border-color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.carousel-nav:first-of-type {
	left: 8px;
}

.carousel-nav:last-of-type {
	right: 8px;
}

.carousel-nav:hover {
	border-color: rgba(178, 210, 255, 0.85);
	background: rgba(16, 46, 92, 0.9);
	transform: translateY(-50%) scale(1.03);
}

.carousel-nav-icon {
	width: 22px;
	height: 22px;
	object-fit: contain;
	filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
}

.carousel-nav.is-next .carousel-nav-icon {
	transform: scaleX(-1);
}

.hero-image-wrap {
	height: min(54vh, 620px);
	width: 100%;
	display: flex;
	align-items: flex-end;
	justify-content: center;
}

.hero-image {
	max-width: min(56vw, 560px);
	max-height: 100%;
	object-fit: contain;
	object-position: center bottom;
	animation: fadeIn 0.24s ease;
}

.thumb-row {
	width: min(960px, 100%);
	margin-top: 16px;
	display: flex;
	justify-content: center;
	gap: 10px;
	overflow-x: auto;
	padding-bottom: 2px;
}

.thumb-btn {
	flex: 0 0 102px;
	height: 74px;
	border: 1px solid rgba(96, 136, 201, 0.45);
	border-radius: 8px;
	background: rgba(10, 26, 56, 0.78);
	padding: 6px;
	cursor: pointer;
	opacity: 0.72;
	transition: opacity 0.2s ease, border-color 0.2s ease;
}

.thumb-btn img {
	width: 100%;
	height: 100%;
	object-fit: contain;
	object-position: center bottom;
}

.thumb-btn.active {
	opacity: 1;
	border-color: #67a8ff;
}

.start-btn {
	margin-top: 24px;
	min-width: 210px;
	height: 58px;
	border-radius: 14px;
	border: none;
	font-size: 24px;
	font-weight: 800;
	cursor: pointer;
	color: #101a2b;
	background: linear-gradient(180deg, #f8fbff 0%, #dee9ff 100%);
	box-shadow: 0 10px 26px rgba(0, 0, 0, 0.36);
}

.start-btn:disabled {
	opacity: 0.66;
	cursor: not-allowed;
}

@keyframes fadeIn {
	from {
		opacity: 0.42;
		transform: translateY(4px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

@media (max-width: 1200px) {
	.content {
		grid-template-columns: 1fr;
	}

	.left-panel {
		border-right: none;
		border-bottom: 1px solid rgba(106, 142, 197, 0.25);
	}
}

@media (max-width: 768px) {
	.main-content {
		margin-left: 0;
	}

	.topbar {
		grid-template-columns: 1fr;
		height: auto;
		gap: 8px;
		padding: 14px 16px;
	}

	.top-title {
		text-align: left;
		font-size: 18px;
	}

	.left-panel {
		padding: 18px 16px;
	}

	.left-panel h2 {
		font-size: 28px;
	}

	.hero-panel {
		padding: 24px 12px 28px;
	}

	.picker-title {
		font-size: 34px;
		text-align: center;
	}

	.carousel-stage {
		width: 100%;
	}

	.carousel-nav {
		height: 40px;
		width: 40px;
	}

	.carousel-nav-icon {
		width: 18px;
		height: 18px;
	}

	.carousel-nav:first-of-type {
		left: 2px;
	}

	.carousel-nav:last-of-type {
		right: 2px;
	}

	.hero-image-wrap {
		height: min(44vh, 420px);
	}

	.hero-image {
		max-width: 92%;
	}

	.thumb-btn {
		flex-basis: 88px;
		height: 66px;
	}

	.start-btn {
		width: 100%;
		max-width: 320px;
		font-size: 21px;
	}
}
</style>
