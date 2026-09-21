import '@ant-design/v5-patch-for-react-19'
import { createRoot } from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import dayjs from 'dayjs'
import './index.css'
import './styles/admin-soft-brutalism.css'
import App from './App.jsx'
import AppErrorBoundary from './components/AppErrorBoundary/AppErrorBoundary.jsx'

// 确保 window.dayjs 存在，Ant Design 内部可能依赖它
window.dayjs = dayjs

// Soft Brutalism 主题：奶油底 + 深色描边 + 大圆角，与前台用户端视觉保持一致
const appTheme = {
  token: {
    colorPrimary: '#171717',
    colorInfo: '#171717',
    colorLink: '#171717',
    colorLinkHover: '#4a453c',
    colorBgLayout: '#F5F0E7',
    colorBgContainer: '#FBF8F2',
    colorBgElevated: '#FBF8F2',
    colorText: '#171717',
    colorTextSecondary: '#6F6A60',
    colorTextTertiary: '#9A9384',
    colorBorder: '#222222',
    colorBorderSecondary: '#E6E0D3',
    colorSplit: '#E6E0D3',
    borderRadius: 14,
    borderRadiusLG: 18,
    borderRadiusSM: 10,
    boxShadow: 'none',
    boxShadowSecondary: 'none',
    boxShadowTertiary: 'none',
    fontFamily: "'Plus Jakarta Sans', 'Avenir Next', 'PingFang SC', 'Microsoft YaHei', sans-serif",
    controlHeight: 40,
  },
  components: {
    Button: {
      fontWeight: 600,
      primaryShadow: 'none',
      defaultShadow: 'none',
      dangerShadow: 'none',
      defaultBg: '#FBF8F2',
      defaultBorderColor: '#222222',
    },
    Card: {
      headerBg: 'transparent',
      colorBorderSecondary: '#222222',
    },
    Table: {
      headerBg: '#F1EBDF',
      headerColor: '#4A453C',
      headerSplitColor: '#E6E0D3',
      rowHoverBg: '#F7F2E8',
      borderColor: '#E6E0D3',
    },
    Modal: {
      contentBg: '#FBF8F2',
      headerBg: '#FBF8F2',
    },
    Tag: {
      defaultBg: '#FBF8F2',
      defaultColor: '#171717',
    },
    Tabs: {
      itemSelectedColor: '#171717',
      inkBarColor: '#171717',
    },
    Pagination: {
      itemActiveBg: '#171717',
    },
  },
}

createRoot(document.getElementById('root')).render(
  <ConfigProvider locale={zhCN} theme={appTheme}>
    <AppErrorBoundary>
      <App />
    </AppErrorBoundary>
  </ConfigProvider>,
)
