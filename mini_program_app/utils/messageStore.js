import { getEnabledAnnouncements } from '@/api/notice'
import { getAppMessageUnreadCount } from '@/api/message'
import { getToken } from '@/utils/storage'
import { startMessageSocket, stopMessageSocket } from '@/utils/messageSocket'

const state = {
  unreadNoticeCount: 0,
  unreadExamCount: 0,
  totalUnreadCount: 0,
  lastSyncAt: 0,
  syncing: false,
  started: false
}

let realtimeRefreshTimer = null
let realtimeRefreshPending = false
let lastRealtimeRefreshAt = 0
const REALTIME_MIN_INTERVAL = 2000
const listeners = new Set()
let lastSignature = ''

function numberValue(value) {
  const next = Number(value)
  return Number.isFinite(next) ? next : 0
}

function getRecords(res) {
  if (Array.isArray(res?.data?.records)) return res.data.records
  if (Array.isArray(res?.data)) return res.data
  return []
}

function getAnnouncementUnreadCount(res) {
  const list = getRecords(res)
  const lastSeenId = Number(uni.getStorageSync('marketLastSeenAnnounceId') || 0)
  return list.filter((item) => Number(item.id || 0) > lastSeenId).length
}

function buildSignature(nextState) {
  return [
    nextState.unreadNoticeCount,
    nextState.unreadExamCount
  ].join('::')
}

function notify(reason = 'sync') {
  const snapshot = getMessageState()
  listeners.forEach((listener) => {
    try {
      listener(snapshot, reason)
    } catch (error) {
      console.warn('messageStore listener failed', error)
    }
  })
  if (typeof uni !== 'undefined' && uni.$emit) {
    uni.$emit('message-store:change', { state: snapshot, reason })
  }
}

export function getMessageState() {
  return {
    unreadNoticeCount: state.unreadNoticeCount,
    unreadExamCount: state.unreadExamCount,
    totalUnreadCount: state.totalUnreadCount,
    lastSyncAt: state.lastSyncAt,
    syncing: state.syncing,
    started: state.started
  }
}

export function subscribeMessageStore(listener) {
  if (typeof listener !== 'function') return () => {}
  listeners.add(listener)
  listener(getMessageState(), 'subscribe')
  return () => {
    listeners.delete(listener)
  }
}

export async function refreshMessageState(reason = 'manual') {
  if (!getToken()) return getMessageState()
  if (state.syncing) return getMessageState()
  state.syncing = true
  try {
    const [announceRes, appMessageUnreadRes] = await Promise.all([
      getEnabledAnnouncements().catch(() => ({ data: [] })),
      getAppMessageUnreadCount({ showError: false }).catch(() => ({ data: { exam: 0 } }))
    ])

    state.unreadNoticeCount = getAnnouncementUnreadCount(announceRes)
    state.unreadExamCount = numberValue(appMessageUnreadRes?.data?.exam)
    state.totalUnreadCount = state.unreadNoticeCount + state.unreadExamCount
    state.lastSyncAt = Date.now()

    const signature = buildSignature(state)
    if (signature !== lastSignature || reason !== 'realtime') {
      lastSignature = signature
      notify(reason)
    }
  } catch (error) {
    console.warn('messageStore refresh failed', error)
  } finally {
    state.syncing = false
    if (realtimeRefreshPending) scheduleRealtimeRefresh()
  }
  return getMessageState()
}

function scheduleRealtimeRefresh() {
  if (realtimeRefreshTimer) return
  const wait = Math.max(120, REALTIME_MIN_INTERVAL - (Date.now() - lastRealtimeRefreshAt))
  realtimeRefreshTimer = setTimeout(() => {
    realtimeRefreshTimer = null
    if (state.syncing) return
    realtimeRefreshPending = false
    lastRealtimeRefreshAt = Date.now()
    refreshMessageState('realtime')
  }, wait)
}

function handleRealtimeEvent(event) {
  if (event?.type !== 'MESSAGE_STATE_CHANGED') return
  if (Array.isArray(event.scopes) && event.scopes.includes('exam')) {
    uni.showToast({ title: '题库后台任务已结束，请到消息中心查看', icon: 'none', duration: 3000 })
  }
  realtimeRefreshPending = true
  scheduleRealtimeRefresh()
}

export function startMessageSync() {
  state.started = true
  startMessageSocket(handleRealtimeEvent)
}

export function stopMessageSync() {
  if (realtimeRefreshTimer) {
    clearTimeout(realtimeRefreshTimer)
    realtimeRefreshTimer = null
  }
  realtimeRefreshPending = false
  stopMessageSocket()
  state.started = false
}

export default {
  state,
  getState: getMessageState,
  subscribe: subscribeMessageStore,
  refresh: refreshMessageState,
  start: startMessageSync,
  stop: stopMessageSync
}
