<template>
  <div class="in-containloger">
    <div class="login-bg"></div>
    <img src="@/assets/interview/logo.png" alt="折跃同步" class="brand-logo" />

    <div class="auth-content">
      <section class="advantage-panel">
        <h2>用真实训练<br />准备关键面试</h2>
        <p class="panel-summary">围绕目标岗位构建训练、追问与复盘闭环，让每一次练习都更接近真实面试现场。</p>
        <ul>
          <li>覆盖高频岗位题库，更快进入面试状态</li>
          <li>AI 实时追问与点评，快速定位短板</li>
          <li>自动生成复盘报告，持续追踪成长曲线</li>
        </ul>
      </section>

      <section class="visual-panel">
        <div class="visual-frame">
          <img src="@/assets/interview/login.png" alt="登录页视觉图" class="login-visual" />
        </div>
      </section>
    </div>

    <div class="entry-actions">
      <button type="button" class="entry-btn entry-btn-primary" @click="openModal('login')">
        立即登录
      </button>
      <button type="button" class="entry-btn entry-btn-secondary" @click="openModal('register')">
        立即注册账号
      </button>
    </div>

    <Teleport to="body">
      <div v-if="modalVisible" class="auth-modal-overlay" @click.self="closeModal">
        <div class="auth-modal">
          <button type="button" class="auth-modal-close" @click="closeModal">×</button>

          <div class="auth-modal-header">
            <p class="auth-modal-eyebrow">{{ modalMode === 'login' ? '欢迎回来' : '注册账号' }}</p>
            <h3>{{ modalMode === 'login' ? '登录你的账号' : '创建你的账号' }}</h3>
            <p>{{ modalMode === 'login' ? '输入手机号或邮箱后继续使用平台。' : '填写基础信息，立即开启智能面试训练。' }}</p>
          </div>

          <form v-if="modalMode === 'login'" class="auth-form" @submit.prevent="handleLogin">
            <label class="auth-field">
              <span>账号</span>
              <input
                v-model.trim="loginForm.username"
                type="text"
                class="auth-input"
                placeholder="手机号或邮箱"
                autocomplete="username"
              />
            </label>

            <label class="auth-field">
              <span>密码</span>
              <input
                v-model="loginForm.password"
                type="password"
                class="auth-input"
                placeholder="请输入密码"
                autocomplete="current-password"
              />
            </label>

            <button type="submit" class="auth-submit-btn" :disabled="submitting">
              {{ submitting ? '登录中...' : '登录' }}
            </button>
          </form>

          <form v-else class="auth-form" @submit.prevent="handleRegister">
            <label class="auth-field">
              <span>昵称</span>
              <input
                v-model.trim="registerForm.nickname"
                type="text"
                class="auth-input"
                placeholder="请输入昵称"
                autocomplete="nickname"
              />
            </label>

            <label class="auth-field">
              <span>手机号</span>
              <input
                v-model.trim="registerForm.phone"
                type="text"
                class="auth-input"
                placeholder="请输入手机号"
                autocomplete="tel"
              />
            </label>

            <label class="auth-field">
              <span>邮箱</span>
              <input
                v-model.trim="registerForm.email"
                type="email"
                class="auth-input"
                placeholder="请输入邮箱"
                autocomplete="email"
              />
            </label>

            <label class="auth-field">
              <span>密码</span>
              <input
                v-model="registerForm.password"
                type="password"
                class="auth-input"
                placeholder="请输入密码"
                autocomplete="new-password"
              />
            </label>

            <label class="auth-field">
              <span>确认密码</span>
              <input
                v-model="registerForm.confirmPassword"
                type="password"
                class="auth-input"
                placeholder="请再次输入密码"
                autocomplete="new-password"
              />
            </label>

            <button type="submit" class="auth-submit-btn" :disabled="submitting">
              {{ submitting ? '注册中...' : '注册账号' }}
            </button>
          </form>

          <div class="auth-modal-footer">
            <span>{{ modalMode === 'login' ? '还没有账号？' : '已经有账号？' }}</span>
            <button type="button" class="auth-switch-btn" @click="switchMode">
              {{ modalMode === 'login' ? '立即注册' : '去登录' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue';
import { authApi } from '../api/auth';
import type { RegisterParams } from '../api/auth';
import { PATHS } from '../routes/paths';

type ModalMode = 'login' | 'register';

const modalVisible = ref(false);
const modalMode = ref<ModalMode>('login');
const submitting = ref(false);

const loginForm = reactive({
  username: '',
  password: '',
});

const registerForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
});

const resetForms = () => {
  loginForm.username = '';
  loginForm.password = '';
  registerForm.nickname = '';
  registerForm.phone = '';
  registerForm.email = '';
  registerForm.password = '';
  registerForm.confirmPassword = '';
};

const openModal = (mode: ModalMode) => {
  modalMode.value = mode;
  modalVisible.value = true;
};

const closeModal = () => {
  modalVisible.value = false;
  submitting.value = false;
};

const switchMode = () => {
  modalMode.value = modalMode.value === 'login' ? 'register' : 'login';
};

const redirectAfterLogin = (payload: {
  session_token: string;
  id: number;
  nickname: string;
  is_manager: number;
  target_position?: string | null;
}) => {
  localStorage.setItem('session_token', payload.session_token);
  localStorage.setItem('user_id', String(payload.id || ''));
  localStorage.setItem('nickname', String(payload.nickname || ''));
  localStorage.setItem('is_manager', String(payload.is_manager ?? 0));

  if (payload.target_position != null && String(payload.target_position).trim()) {
    localStorage.setItem('job_role', String(payload.target_position).trim());
  }

  closeModal();
  window.history.pushState({}, '', PATHS.INDEX);
  window.dispatchEvent(new PopStateEvent('popstate'));
};

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    alert('请输入手机号/邮箱和密码');
    return;
  }

  const username = loginForm.username.trim();
  const isEmail = username.includes('@');
  const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(username);
  const phoneOk = /^1\d{10}$/.test(username) || /^\+?\d{6,20}$/.test(username);

  if ((isEmail && !emailOk) || (!isEmail && !phoneOk)) {
    alert('登录账号仅支持手机号或邮箱');
    return;
  }

  submitting.value = true;

  try {
    const res = await authApi.login(
      isEmail ? { email: username, password: loginForm.password } : { phone: username, password: loginForm.password }
    );
    redirectAfterLogin(res);
  } catch (error: any) {
    alert(error.message || '登录失败，请检查账号和密码');
  } finally {
    submitting.value = false;
  }
};

const handleRegister = async () => {
  if (!registerForm.nickname || !registerForm.phone || !registerForm.email || !registerForm.password) {
    alert('请填写完整的注册信息');
    return;
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    alert('两次输入的密码不一致');
    return;
  }

  submitting.value = true;

  try {
    const params: Partial<RegisterParams> = {
      nickname: registerForm.nickname,
      phone: registerForm.phone,
      email: registerForm.email,
      password: registerForm.password,
    };

    await authApi.register(params);
    alert('注册成功，请登录');
    modalMode.value = 'login';
    loginForm.username = registerForm.phone || registerForm.email;
    loginForm.password = '';
    registerForm.password = '';
    registerForm.confirmPassword = '';
  } catch (error: any) {
    alert(error.message || '注册失败，请检查输入信息');
  } finally {
    submitting.value = false;
  }
};

watch(modalVisible, (visible) => {
  if (!visible) {
    resetForms();
  }
});

onMounted(() => {
  // 当前分支未实现请求取消，这里保留挂载点以便后续扩展
});
</script>

<style scoped>
:global(html),
:global(body),
:global(#app) {
  height: 100%;
  overflow: hidden;
}

.in-containloger {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100vh;
  overflow: hidden;
  padding: clamp(72px, 8vh, 92px) clamp(28px, 4vw, 56px) 24px;
}

.login-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background:
    radial-gradient(circle at 20% 18%, rgba(58, 123, 200, 0.22), transparent 28%),
    radial-gradient(circle at 82% 22%, rgba(45, 107, 179, 0.16), transparent 24%),
    radial-gradient(circle at 70% 78%, rgba(58, 123, 200, 0.12), transparent 26%),
    linear-gradient(135deg, #07111f 0%, #0a0c12 46%, #0d1728 100%);
  z-index: -1;
}

.brand-logo {
  position: fixed;
  top: clamp(22px, 3.5vh, 36px);
  left: clamp(24px, 3vw, 44px);
  z-index: 2;
  width: clamp(170px, 17vw, 250px);
  height: auto;
  user-select: none;
}

.auth-content {
  width: min(1280px, 100%);
  display: grid;
  grid-template-columns: minmax(320px, 390px) minmax(620px, 1fr);
  align-items: center;
  gap: clamp(56px, 6vw, 110px);
  transform: translateY(-34px);
}

.advantage-panel {
  color: #eef5ff;
  max-width: 410px;
}

.advantage-panel h2 {
  margin: 0 0 22px;
  color: rgba(255, 255, 255, 0.99);
  font-size: clamp(46px, 4.6vw, 68px);
  line-height: 1.08;
  font-weight: 700;
  letter-spacing: -0.055em;
}

.panel-summary {
  margin: 0 0 26px;
  color: rgba(232, 241, 252, 0.68);
  font-size: clamp(14px, 1.05vw, 16px);
  line-height: 1.9;
  max-width: 350px;
}

.advantage-panel ul {
  margin: 0;
  padding-left: 0;
  display: grid;
  gap: 12px;
  list-style: none;
}

.advantage-panel li {
  position: relative;
  padding: 0 0 0 18px;
  color: rgba(240, 246, 255, 0.8);
  font-size: clamp(13px, 1vw, 15px);
  line-height: 1.82;
  letter-spacing: 0.01em;
}

.advantage-panel li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.82em;
  width: 5px;
  height: 5px;
  border-radius: 999px;
  background: rgba(108, 164, 255, 0.9);
  box-shadow: 0 0 10px rgba(97, 170, 255, 0.25);
}

.visual-panel {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.visual-frame {
  width: min(100%, 820px);
  padding: 7px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.045);
  border: 1px solid rgba(119, 176, 255, 0.22);
  backdrop-filter: blur(1px);
  -webkit-backdrop-filter: blur(1px);
  box-shadow:
    0 0 0 1px rgba(138, 194, 255, 0.12) inset,
    0 0 26px rgba(63, 140, 255, 0.16),
    0 0 56px rgba(63, 140, 255, 0.12),
    0 16px 30px rgba(8, 22, 46, 0.12);
}

.login-visual {
  width: 100%;
  height: auto;
  display: block;
  object-fit: contain;
  border-radius: 18px;
  box-shadow: 0 12px 24px rgba(8, 18, 40, 0.1);
}

.entry-actions {
  margin-top: 18px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.entry-btn {
  min-width: 180px;
  height: 50px;
  padding: 0 28px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #4da3ff 0%, #2f6dff 100%);
  color: #ffffff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.04em;
  cursor: pointer;
  box-shadow: 0 14px 26px rgba(47, 109, 255, 0.28);
  transition: transform 0.22s ease, box-shadow 0.22s ease, filter 0.22s ease;
}

.entry-btn:hover {
  transform: translateY(-2px) scale(1.05);
  filter: brightness(1.08);
  box-shadow: 0 0 0 6px rgba(109, 170, 255, 0.14), 0 20px 38px rgba(47, 109, 255, 0.38);
}

.entry-btn-secondary {
  background: linear-gradient(135deg, #66b0ff 0%, #3d7cff 100%);
}

.auth-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(8, 16, 34, 0.42);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.auth-modal {
  position: relative;
  width: min(100%, 460px);
  padding: 30px 28px 26px;
  border-radius: 28px;
  background:
    linear-gradient(180deg, rgba(18, 29, 48, 0.96), rgba(11, 18, 31, 0.94));
  border: 1px solid rgba(93, 148, 255, 0.18);
  box-shadow:
    0 24px 80px rgba(2, 10, 24, 0.48),
    inset 0 1px 0 rgba(146, 190, 255, 0.08);
}

.auth-modal-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: #9ebfff;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.auth-modal-header {
  margin-bottom: 22px;
}

.auth-modal-eyebrow {
  margin: 0 0 10px;
  color: #66a8ff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.auth-modal-header h3 {
  margin: 0 0 10px;
  color: #f5f9ff;
  font-size: 28px;
  line-height: 1.2;
}

.auth-modal-header p {
  margin: 0;
  color: rgba(210, 223, 245, 0.72);
  font-size: 14px;
  line-height: 1.7;
}

.auth-form {
  display: grid;
  gap: 16px;
}

.auth-field {
  display: grid;
  gap: 8px;
}

.auth-field span {
  color: #d7e6ff;
  font-size: 13px;
  font-weight: 600;
}

.auth-input {
  width: 100%;
  height: 48px;
  padding: 0 14px;
  border: 1px solid rgba(108, 149, 214, 0.16);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.05);
  color: #f5f9ff;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  box-sizing: border-box;
}

.auth-input::placeholder {
  color: rgba(189, 205, 232, 0.48);
}

.auth-input:focus {
  border-color: rgba(92, 159, 255, 0.68);
  box-shadow: 0 0 0 4px rgba(75, 143, 255, 0.12);
}

.auth-submit-btn {
  margin-top: 4px;
  height: 50px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #4da3ff 0%, #2f6dff 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.auth-submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.auth-modal-footer {
  margin-top: 18px;
  display: flex;
  justify-content: center;
  gap: 8px;
  color: rgba(202, 216, 241, 0.66);
  font-size: 14px;
}

.auth-switch-btn {
  padding: 0;
  border: none;
  background: none;
  color: #2f6dff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 1100px) {
  .in-containloger {
    height: auto;
    min-height: 100vh;
    padding-top: clamp(64px, 10vh, 88px);
    overflow: auto;
  }

  .auth-content {
    grid-template-columns: 1fr;
    gap: 28px;
    transform: none;
  }

  .advantage-panel {
    text-align: center;
    max-width: none;
  }

  .panel-summary {
    max-width: none;
  }

  .advantage-panel ul {
    width: min(760px, 100%);
    margin: 0 auto;
    text-align: left;
  }

  .visual-panel {
    justify-content: center;
  }
}

@media (max-width: 640px) {
  .brand-logo {
    width: clamp(140px, 42vw, 190px);
    top: 16px;
    left: 14px;
  }

  .in-containloger {
    padding-bottom: 24px;
  }

  .advantage-panel h2 {
    font-size: clamp(34px, 11vw, 44px);
  }

  .visual-frame {
    padding: 6px;
    border-radius: 20px;
  }

  .entry-actions {
    width: 100%;
    flex-direction: column;
  }

  .entry-btn {
    width: min(100%, 260px);
    height: 46px;
    font-size: 15px;
  }

  .auth-modal {
    padding: 26px 18px 20px;
    border-radius: 22px;
  }
}
</style>
