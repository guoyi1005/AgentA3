import { avatarApi, type AvatarActionInput, type AvatarStreamInfo } from "../api/avatar";

type FlvJsLike = {
  isSupported: () => boolean;
  createPlayer: (mediaDataSource: Record<string, any>, config?: Record<string, any>) => any;
};

function wait(ms: number): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

let flvJsLoading: Promise<FlvJsLike | null> | null = null;

async function ensureFlvJs(): Promise<FlvJsLike | null> {
  const win = window as any;
  if (win.flvjs) return win.flvjs as FlvJsLike;
  if (flvJsLoading) return flvJsLoading;

  flvJsLoading = new Promise((resolve) => {
    const script = document.createElement("script");
    script.src = "https://cdn.jsdelivr.net/npm/flv.js@1.6.2/dist/flv.min.js";
    script.async = true;
    script.onload = () => {
      resolve((window as any).flvjs ? ((window as any).flvjs as FlvJsLike) : null);
    };
    script.onerror = () => resolve(null);
    document.head.appendChild(script);
  });
  return flvJsLoading;
}

export class AvatarWebApiDriver {
  private avatarSessionId: string | null = null;
  private activeProtocol: "webrtc" | "flv" | null = null;
  private videoEl: HTMLVideoElement | null = null;
  private flvPlayer: any = null;
  private resumeBound = false;
  private visibilityResumeHandler: (() => void) | null = null;
  private interactionResumeHandler: (() => void) | null = null;
  private hasUserActivatedAudio = false;

  attachVideo(el: HTMLVideoElement): void {
    this.videoEl = el;
  }

  isConnected(): boolean {
    return !!this.avatarSessionId;
  }

  getProtocol(): "webrtc" | "flv" | null {
    return this.activeProtocol;
  }

  private async playByNativeVideo(url: string): Promise<void> {
    if (!this.videoEl) throw new Error("视频容器未就绪");
    this.videoEl.srcObject = null;
    this.videoEl.src = url;
    this.applyAudioPreference();
    this.videoEl.autoplay = true;
    this.videoEl.playsInline = true;
    await this.playElementWithRecovery();
  }

  private async playByFlv(url: string): Promise<void> {
    if (!this.videoEl) throw new Error("视频容器未就绪");
    const flvjs = await ensureFlvJs();
    if (!flvjs || !flvjs.isSupported()) {
      throw new Error("flv.js 不可用或浏览器不支持 MSE");
    }
    this.flvPlayer = flvjs.createPlayer(
      {
        type: "flv",
        url,
        isLive: true,
        hasAudio: true,
        hasVideo: true,
      },
      {
        enableStashBuffer: false,
        stashInitialSize: 32,
        isLive: true,
      },
    );
    this.flvPlayer.attachMediaElement(this.videoEl);
    this.flvPlayer.load();
    this.applyAudioPreference();
    const playResult = this.flvPlayer.play();
    if (playResult && typeof playResult.then === "function") {
      try {
        await playResult;
      } catch (e) {
        if (this.isPowerSavingPauseError(e)) {
          this.bindResumeOnUserInteraction();
          return;
        }
        throw e;
      }
    }
  }

  private isPowerSavingPauseError(error: unknown): boolean {
    const msg = String((error as any)?.message || error || "").toLowerCase();
    return msg.includes("video-only background media was paused to save power");
  }

  private async playElementWithRecovery(): Promise<void> {
    if (!this.videoEl) throw new Error("视频容器未就绪");
    try {
      const p = this.videoEl.play();
      if (p && typeof p.then === "function") {
        await p;
      }
    } catch (e) {
      if (this.isPowerSavingPauseError(e)) {
        this.bindResumeOnUserInteraction();
        return;
      }
      throw e;
    }
  }

  private applyAudioPreference(): void {
    if (!this.videoEl) return;
    this.videoEl.volume = 1;
    this.videoEl.muted = !this.hasUserActivatedAudio;
  }

  private activateAudio(): void {
    this.hasUserActivatedAudio = true;
    this.applyAudioPreference();
  }

  private bindResumeOnUserInteraction(): void {
    if (this.resumeBound) return;
    this.resumeBound = true;

    this.visibilityResumeHandler = () => {
      if (document.visibilityState !== "visible") return;
      void this.tryResumePlayback();
    };
    document.addEventListener("visibilitychange", this.visibilityResumeHandler, { passive: true });

    this.interactionResumeHandler = () => {
      this.activateAudio();
      void this.tryResumePlayback();
    };
    window.addEventListener("pointerdown", this.interactionResumeHandler, { passive: true, once: true });
    window.addEventListener("keydown", this.interactionResumeHandler, { passive: true, once: true });
    window.addEventListener("touchstart", this.interactionResumeHandler, { passive: true, once: true });
  }

  private async tryResumePlayback(): Promise<void> {
    if (!this.videoEl) return;
    try {
      this.applyAudioPreference();
      await this.videoEl.play();
      this.unbindResumeHooks();
    } catch {
      // ignore; wait for next interaction or visibility change
    }
  }

  private unbindResumeHooks(): void {
    if (!this.resumeBound) return;
    this.resumeBound = false;
    if (this.visibilityResumeHandler) {
      document.removeEventListener("visibilitychange", this.visibilityResumeHandler as EventListener);
      this.visibilityResumeHandler = null;
    }
    if (this.interactionResumeHandler) {
      window.removeEventListener("pointerdown", this.interactionResumeHandler as EventListener);
      window.removeEventListener("keydown", this.interactionResumeHandler as EventListener);
      window.removeEventListener("touchstart", this.interactionResumeHandler as EventListener);
      this.interactionResumeHandler = null;
    }
  }

  async playStream(stream: AvatarStreamInfo): Promise<void> {
    await this.cleanupPlayer();
    if (stream.protocol === "flv") {
      await this.playByFlv(stream.stream_url);
      this.activeProtocol = "flv";
      return;
    }
    await this.playByNativeVideo(stream.stream_url);
    this.activeProtocol = "webrtc";
  }

  async startAndPlay(conversationId?: string | null, preferredProtocol?: "webrtc" | "flv"): Promise<void> {
    const started = await avatarApi.startSession({
      conversation_id: conversationId || undefined,
      protocol: preferredProtocol,
    });
    this.avatarSessionId = started.avatar_session_id;
    this.activeProtocol = started.stream.protocol;
    await this.playStream(started.stream);
  }

  async startWithFallback(
    conversationId?: string | null,
    preferredProtocol: "webrtc" | "flv" = "webrtc",
    allowFallback = true,
  ): Promise<void> {
    try {
      await this.startAndPlay(conversationId, preferredProtocol);
    } catch (firstError) {
      if (!allowFallback) {
        throw firstError;
      }
      const fallbackProtocol: "webrtc" | "flv" = preferredProtocol === "webrtc" ? "flv" : "webrtc";
      await this.stop().catch(() => {
        // ignore
      });
      await wait(120);
      await this.startAndPlay(conversationId, fallbackProtocol);
      console.warn(`[avatar] ${preferredProtocol.toUpperCase()} 启动失败，已切换 ${fallbackProtocol.toUpperCase()}:`, firstError);
    }
  }

  async speak(ttsFilename: string, frameMs?: number, actions?: AvatarActionInput[]): Promise<void> {
    if (!this.avatarSessionId) {
      throw new Error("数字人会话未初始化");
    }
    this.activateAudio();
    await this.tryResumePlayback().catch(() => {
      // ignore
    });
    await avatarApi.speakSession({
      avatar_session_id: this.avatarSessionId,
      tts_filename: ttsFilename,
      frame_ms: frameMs,
      actions,
    });
  }

  async interrupt(): Promise<void> {
    if (!this.avatarSessionId) return;
    await avatarApi.interruptSession({ avatar_session_id: this.avatarSessionId });
  }

  private async cleanupPlayer(): Promise<void> {
    try {
      this.unbindResumeHooks();
      if (this.flvPlayer) {
        try {
          this.flvPlayer.pause?.();
        } catch {
          // ignore
        }
        try {
          this.flvPlayer.unload?.();
        } catch {
          // ignore
        }
        try {
          this.flvPlayer.detachMediaElement?.();
        } catch {
          // ignore
        }
        try {
          this.flvPlayer.destroy?.();
        } catch {
          // ignore
        }
      }
      this.flvPlayer = null;
      if (this.videoEl) {
        try {
          this.videoEl.pause();
        } catch {
          // ignore
        }
        this.videoEl.srcObject = null;
        this.videoEl.removeAttribute("src");
        this.videoEl.load();
      }
    } catch {
      // ignore
    }
  }

  async stop(): Promise<void> {
    const sid = this.avatarSessionId;
    this.avatarSessionId = null;
    this.activeProtocol = null;
    if (sid) {
      await avatarApi.stopSession({ avatar_session_id: sid }).catch(() => {
        // ignore
      });
    }
    await this.cleanupPlayer();
  }

  async destroy(): Promise<void> {
    await this.stop();
  }
}
