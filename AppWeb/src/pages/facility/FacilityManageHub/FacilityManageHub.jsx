import { useMemo } from 'react'
import { Button } from 'antd'
import {
  AppstoreOutlined,
  BankOutlined,
  HomeOutlined,
  PushpinOutlined,
  ShopOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { useNavigate, useSearchParams } from 'react-router-dom'
import FacilityPlaceManage from '../FacilityPlaceManage/FacilityPlaceManage'
import './FacilityManageHub.css'

const FACILITY_TYPES = [
  {
    key: 'canteen',
    sceneType: 'CANTEEN',
    label: '食堂',
    icon: ShopOutlined,
    color: '#f97316',
  },
  {
    key: 'teaching',
    sceneType: 'TEACHING',
    label: '教学楼',
    icon: BankOutlined,
    color: '#3b82f6',
  },
  {
    key: 'dormitory',
    sceneType: 'DORMITORY',
    label: '宿舍楼',
    icon: HomeOutlined,
    color: '#8b5cf6',
  },
  {
    key: 'sports',
    sceneType: 'SPORTS',
    label: '运动场',
    icon: ThunderboltOutlined,
    color: '#10b981',
  },
  {
    key: 'other',
    sceneType: 'OTHER',
    label: '其他',
    icon: AppstoreOutlined,
    color: '#64748b',
  },
]

export default function FacilityManageHub() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const activeType = useMemo(() => {
    const requested = searchParams.get('type')
    return FACILITY_TYPES.find((item) => item.key === requested) || FACILITY_TYPES[0]
  }, [searchParams])

  const changeType = (item) => {
    setSearchParams({ type: item.key }, { replace: true })
  }

  return (
    <div className="facility-hub-page">
      <header className="facility-hub-header">
        <div>
          <span className="facility-hub-kicker">校园设施</span>
          <h1>设施管理</h1>
          <p>维护移动端校园地图使用的设施资料、空间层级与展示内容。</p>
        </div>
        <Button
          icon={<PushpinOutlined />}
          onClick={() => navigate('/facility/marker')}
        >
          地图点位
        </Button>
      </header>

      <nav className="facility-hub-tabs" aria-label="设施类型">
        {FACILITY_TYPES.map((item) => {
          const Icon = item.icon
          const active = item.key === activeType.key
          return (
            <button
              key={item.key}
              type="button"
              className={`facility-hub-tab${active ? ' active' : ''}`}
              style={{ '--facility-tab-color': item.color }}
              aria-current={active ? 'page' : undefined}
              onClick={() => changeType(item)}
            >
              <span className="facility-hub-tab__icon"><Icon /></span>
              <span className="facility-hub-tab__copy">
                <strong>{item.label}</strong>
              </span>
            </button>
          )
        })}
      </nav>

      <FacilityPlaceManage
        key={activeType.sceneType}
        sceneType={activeType.sceneType}
        embedded
      />
    </div>
  )
}
