import { api } from "./index";

export interface AvatarRuntimeConfig {
  app_id: string;
  scene_id: string;
  server_url: string;
  stream: {
    protocol: "webrtc" | "flv";
    fallback_protocol: "flv" | null;
    allow_fallback: boolean;
  };
  avatar: {
    avatar_id: string;
  };
  audio: {
    sample_rate: 16000;
    bit_depth: 16;
    channels: 1;
    default_frame_ms: number;
  };
  features: {
    audio_driver: boolean;
    text_driver: boolean;
    text_interact: boolean;
  };
  opening?: {
    text?: string;
    auto_speak?: boolean;
  };
}

export interface AvatarStreamInfo {
  protocol: "webrtc" | "flv";
  stream_url: string;
  stream_extend: Record<string, any>;
}

export interface AvatarSessionStartResponse {
  avatar_session_id: string;
  conversation_id?: string | null;
  stream: AvatarStreamInfo;
}

export interface AvatarSessionSpeakResponse {
  accepted: boolean;
  avatar_session_id: string;
  tts_filename: string;
  frame_count: number;
  duration_ms: number;
  frame_ms: number;
  action_count?: number;
}

export interface AvatarActionInput {
  type?: string;
  value: string;
  tb?: number;
}

export const avatarApi = {
  getRuntimeConfig: () => api.get<AvatarRuntimeConfig>("/avatar/runtime-config"),
  startSession: (payload?: { conversation_id?: string; protocol?: "webrtc" | "flv" }) =>
    api.post<AvatarSessionStartResponse>("/avatar/session/start", payload || {}),
  speakSession: (payload: { avatar_session_id: string; tts_filename: string; frame_ms?: number; actions?: AvatarActionInput[] }) =>
    api.post<AvatarSessionSpeakResponse>("/avatar/session/speak", payload),
  interruptSession: (payload: { avatar_session_id: string }) =>
    api.post<{ accepted: boolean; avatar_session_id: string }>("/avatar/session/interrupt", payload),
  stopSession: (payload: { avatar_session_id: string }) =>
    api.post<{ accepted: boolean; avatar_session_id: string }>("/avatar/session/stop", payload),
};
