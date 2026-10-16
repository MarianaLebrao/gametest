import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';

type Screen = 'home' | 'create' | 'lobby' | 'round' | 'result' | 'final';
type GameMode = 'preview' | 'blind';
type RoomStatus = 'LOBBY' | 'PLAYING' | 'RESULTS' | 'FINISHED';
interface Player { id: string; name: string; }
interface Track {
  id: string; videoId: string; title: string | null; channelTitle: string | null; thumbnailUrl: string | null;
  ownerPlayerId: string | null; ownerName: string | null; previewStartSeconds: number;
}
interface Room {
  code: string; hostPlayerId: string; roundCount: number; roundTimer: number; gameMode: GameMode;
  status: RoomStatus; currentRound: number; totalRounds: number; players: Player[]; tracks: Track[];
  currentTrack: Track | null; votedPlayerIds: string[]; scores: Record<string, number>; roundEndsAt: string | null;
}

interface SavedSession { roomCode: string; playerId: string; playerName: string; }

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly apiBase = 'https://gametest-0trr.onrender.com/api';
  private roomPolling?: ReturnType<typeof setInterval>;
  private roundClock?: ReturnType<typeof setInterval>;
  private lastRoundKey = '';
  private readonly sessionKey = 'who-added-that-session';

  protected readonly currentScreen = signal<Screen>('home');
  protected readonly playerName = signal('');
  protected readonly roomCode = signal('');
  protected readonly playerId = signal('');
  protected readonly room = signal<Room | null>(null);
  protected readonly videoLink = signal('');
  protected readonly previewStartSeconds = signal(30);
  protected readonly errorMessage = signal('');
  protected readonly isLoading = signal(false);
  protected readonly copied = signal(false);
  protected readonly selectedVote = signal<string | null>(null);
  protected readonly activePreviewUrl = signal<SafeResourceUrl | null>(null);
  protected readonly secondsLeft = signal(30);
  protected readonly roundCount = signal(8);
  protected readonly roundTimer = signal(30);
  protected readonly gameMode = signal<GameMode>('preview');
  protected readonly trackTimeDrafts = signal<Record<string, number>>({});

  ngOnInit(): void {
    const invitedCode = new URLSearchParams(window.location.search).get('room');
    const code = invitedCode?.toUpperCase().slice(0, 6);
    if (code) this.roomCode.set(code);
    const session = this.readSession();
    if (session && (!code || session.roomCode === code)) void this.restoreSession(session);
  }

  ngOnDestroy(): void { this.stopPolling(); this.stopRoundClock(); }
  protected setScreen(screen: Screen): void { this.currentScreen.set(screen); }
  protected async leaveRoom(): Promise<void> {
    const room = this.room();
    const playerId = this.playerId();
    if (room && playerId) {
      try {
        await firstValueFrom(this.http.delete(
          `${this.apiBase}/rooms/${room.code}/players/${playerId}?requesterPlayerId=${encodeURIComponent(playerId)}`));
      } catch {
        // Leaving locally must remain possible during a connection failure.
      }
    }
    this.clearSession();
  }

  protected async removePlayer(targetPlayerId: string): Promise<void> {
    const room = this.room();
    if (!room || !this.isHost() || targetPlayerId === this.playerId()) return;
    const player = room.players.find(candidate => candidate.id === targetPlayerId);
    if (!window.confirm(`Remover ${player?.name ?? 'este jogador'} da sala?`)) return;
    await this.runRequest(async () => {
      await firstValueFrom(this.http.delete(
        `${this.apiBase}/rooms/${room.code}/players/${targetPlayerId}?requesterPlayerId=${encodeURIComponent(this.playerId())}`));
      await this.refreshRoom();
    }, 'Não foi possível remover esse jogador.');
  }

  private clearSession(): void {
    this.stopPolling();
    this.stopRoundClock();
    localStorage.removeItem(this.sessionKey);
    this.room.set(null);
    this.playerId.set('');
    this.roomCode.set('');
    this.lastRoundKey = '';
    this.currentScreen.set('home');
    window.history.replaceState({}, document.title, window.location.pathname);
  }
  protected onPlayerNameInput(value: string): void { this.playerName.set(value.slice(0, 30)); }
  protected onRoomCodeInput(value: string): void {
    this.roomCode.set(value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6));
  }
  protected onVideoLinkInput(value: string): void { this.videoLink.set(value.trim()); }
  protected previewMinutes(): number { return Math.floor(this.previewStartSeconds() / 60); }
  protected previewSecondsPart(): number { return this.previewStartSeconds() % 60; }
  protected onPreviewMinutesInput(value: string): void {
    const minutes = Math.max(0, Math.min(59, Math.trunc(Number(value) || 0)));
    this.previewStartSeconds.set(minutes * 60 + this.previewSecondsPart());
  }
  protected onPreviewSecondsInput(value: string): void {
    const seconds = Math.max(0, Math.min(59, Math.trunc(Number(value) || 0)));
    this.previewStartSeconds.set(this.previewMinutes() * 60 + seconds);
  }

  protected async createRoom(): Promise<void> {
    if (!this.playerName().trim()) return;
    await this.runRequest(async () => {
      const room = await firstValueFrom(this.http.post<Room>(`${this.apiBase}/rooms`, {
        hostName: this.playerName().trim(), roundCount: this.roundCount(), roundTimer: this.roundTimer(),
        gameMode: this.gameMode()
      }));
      this.enterRoom(room, room.hostPlayerId);
    }, 'Could not create the room.');
  }

  protected async joinRoom(): Promise<void> {
    if (!this.playerName().trim() || this.roomCode().length !== 6) return;
    await this.runRequest(async () => {
      const player = await firstValueFrom(this.http.post<Player>(
        `${this.apiBase}/rooms/${this.roomCode()}/players`, { playerName: this.playerName().trim() }));
      const room = await firstValueFrom(this.http.get<Room>(`${this.apiBase}/rooms/${this.roomCode()}`));
      this.enterRoom(room, player.id);
    }, 'Could not join the room. Check the code or player limit.');
  }

  protected async addTrack(): Promise<void> {
    if (!this.videoLink() || !this.room() || !this.playerId()) return;
    await this.runRequest(async () => {
      await firstValueFrom(this.http.post<Track>(`${this.apiBase}/rooms/${this.room()!.code}/tracks`, {
        playerId: this.playerId(), videoUrl: this.videoLink(), previewStartSeconds: this.previewStartSeconds()
      }));
      this.videoLink.set('');
      this.previewStartSeconds.set(30);
      await this.refreshRoom();
    }, 'Could not add this video. Check that the YouTube link is public.');
  }

  protected ownTracks(): Track[] {
    return this.room()?.tracks.filter(track => track.ownerPlayerId === this.playerId()) ?? [];
  }
  protected trackDraftMinutes(track: Track): number {
    return Math.floor(this.trackDraftSeconds(track) / 60);
  }
  protected trackDraftSecondsPart(track: Track): number {
    return this.trackDraftSeconds(track) % 60;
  }
  protected onTrackMinutesInput(track: Track, value: string): void {
    const minutes = Math.max(0, Math.min(59, Math.trunc(Number(value) || 0)));
    this.setTrackDraft(track.id, minutes * 60 + this.trackDraftSecondsPart(track));
  }
  protected onTrackSecondsInput(track: Track, value: string): void {
    const seconds = Math.max(0, Math.min(59, Math.trunc(Number(value) || 0)));
    this.setTrackDraft(track.id, this.trackDraftMinutes(track) * 60 + seconds);
  }
  protected trackTimeChanged(track: Track): boolean {
    return this.trackDraftSeconds(track) !== track.previewStartSeconds;
  }
  protected async saveTrackTime(track: Track): Promise<void> {
    const room = this.room();
    if (!room || !this.trackTimeChanged(track)) return;
    await this.runRequest(async () => {
      await firstValueFrom(this.http.put<Track>(`${this.apiBase}/rooms/${room.code}/tracks/${track.id}`, {
        playerId: this.playerId(), previewStartSeconds: this.trackDraftSeconds(track)
      }));
      await this.refreshRoom();
    }, 'Não foi possível atualizar o tempo da música.');
  }
  protected async deleteTrack(track: Track): Promise<void> {
    const room = this.room();
    if (!room || !window.confirm(`Excluir “${track.title ?? 'esta música'}”?`)) return;
    await this.runRequest(async () => {
      await firstValueFrom(this.http.delete(
        `${this.apiBase}/rooms/${room.code}/tracks/${track.id}?playerId=${encodeURIComponent(this.playerId())}`));
      this.trackTimeDrafts.update(drafts => {
        const updated = { ...drafts };
        delete updated[track.id];
        return updated;
      });
      await this.refreshRoom();
    }, 'Não foi possível excluir essa música.');
  }

  protected async startGame(): Promise<void> {
    await this.roomAction('start', { playerId: this.playerId() }, 'Could not start the game.');
  }

  protected async submitVote(): Promise<void> {
    if (!this.selectedVote()) return;
    await this.roomAction('votes', {
      playerId: this.playerId(), targetPlayerId: this.selectedVote()
    }, 'Could not submit your vote.');
  }

  protected async advanceRound(): Promise<void> {
    await this.roomAction('advance', { playerId: this.playerId() }, 'Could not advance the round.');
  }

  protected async copyInvite(): Promise<void> {
    await navigator.clipboard.writeText(`Join my WhoAddedThat? room: ${this.inviteUrl()}`);
    this.copied.set(true);
    setTimeout(() => this.copied.set(false), 1800);
  }

  protected inviteUrl(): string {
    return `${window.location.origin}${window.location.pathname}?room=${this.room()?.code ?? this.roomCode()}`;
  }
  protected isHost(): boolean { return this.room()?.hostPlayerId === this.playerId(); }
  protected hasVoted(): boolean { return this.room()?.votedPlayerIds.includes(this.playerId()) ?? false; }
  protected trackCountFor(playerId: string): number {
    return this.room()?.tracks.filter(track => track.ownerPlayerId === playerId).length ?? 0;
  }
  protected canAddTrack(): boolean { return this.trackCountFor(this.playerId()) < 10; }
  protected everyoneReady(): boolean {
    const room = this.room();
    return !!room && room.players.length >= 2 && room.tracks.length >= room.roundCount
      && room.players.every(player => this.trackCountFor(player.id) > 0);
  }
  protected sortedPlayers(): Player[] {
    const room = this.room();
    return [...(room?.players ?? [])].sort((a, b) => (room?.scores[b.id] ?? 0) - (room?.scores[a.id] ?? 0));
  }
  protected scoreFor(playerId: string): number { return this.room()?.scores[playerId] ?? 0; }
  protected setRoundCount(value: number): void { this.roundCount.set(value); }
  protected setRoundTimer(value: number): void { this.roundTimer.set(value); }
  protected setGameMode(value: GameMode): void { this.gameMode.set(value); }
  protected playPreview(): void {
    const track = this.room()?.currentTrack;
    if (!track || this.activePreviewUrl()) return;
    const start = track.previewStartSeconds;
    const end = start + Math.min(this.room()?.roundTimer ?? 30, 60);
    this.activePreviewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://www.youtube-nocookie.com/embed/${track.videoId}?autoplay=1&start=${start}&end=${end}&controls=0&rel=0&playsinline=1`
    ));
  }

  private async roomAction(path: string, body: object, fallback: string): Promise<void> {
    if (!this.room()) return;
    await this.runRequest(async () => {
      const room = await firstValueFrom(this.http.post<Room>(
        `${this.apiBase}/rooms/${this.room()!.code}/${path}`, body));
      this.applyRoom(room);
    }, fallback);
  }

  private enterRoom(room: Room, playerId: string): void {
    this.roomCode.set(room.code);
    this.playerId.set(playerId);
    const player = room.players.find(candidate => candidate.id === playerId);
    if (player) {
      this.playerName.set(player.name);
      localStorage.setItem(this.sessionKey, JSON.stringify({
        roomCode: room.code, playerId, playerName: player.name
      } satisfies SavedSession));
    }
    this.applyRoom(room);
    window.history.replaceState({}, document.title, `${window.location.pathname}?room=${room.code}`);
    this.startPolling();
  }

  private applyRoom(room: Room): void {
    this.room.set(room);
    this.trackTimeDrafts.update(drafts => {
      const updated = { ...drafts };
      room.tracks.filter(track => track.ownerPlayerId === this.playerId()).forEach(track => {
        if (updated[track.id] === undefined) updated[track.id] = track.previewStartSeconds;
      });
      return updated;
    });
    const roundKey = `${room.status}-${room.currentRound}`;
    if (roundKey !== this.lastRoundKey) {
      this.lastRoundKey = roundKey;
      this.selectedVote.set(null);
      this.activePreviewUrl.set(null);
      if (room.status === 'PLAYING') this.startRoundClock(room.roundEndsAt);
      else this.stopRoundClock();
    }
    const screens: Record<RoomStatus, Screen> = {
      LOBBY: 'lobby', PLAYING: 'round', RESULTS: 'result', FINISHED: 'final'
    };
    this.currentScreen.set(screens[room.status]);
  }

  private startRoundClock(roundEndsAt: string | null): void {
    this.stopRoundClock();
    const update = () => {
      const milliseconds = roundEndsAt ? new Date(roundEndsAt).getTime() - Date.now() : 0;
      this.secondsLeft.set(Math.max(0, Math.ceil(milliseconds / 1000)));
      if (milliseconds <= 0) void this.refreshRoom();
    };
    update();
    this.roundClock = setInterval(update, 500);
  }
  private stopRoundClock(): void { if (this.roundClock) clearInterval(this.roundClock); }
  private startPolling(): void {
    this.stopPolling();
    this.roomPolling = setInterval(() => this.refreshRoom(), 2000);
  }
  private stopPolling(): void { if (this.roomPolling) clearInterval(this.roomPolling); }
  private async refreshRoom(): Promise<void> {
    if (!this.roomCode()) return;
    try {
      const room = await firstValueFrom(this.http.get<Room>(`${this.apiBase}/rooms/${this.roomCode()}`));
      if (this.playerId() && !room.players.some(player => player.id === this.playerId())) {
        this.clearSession();
        this.errorMessage.set('Você foi removido da sala pelo anfitrião.');
        return;
      }
      this.applyRoom(room);
    } catch {
      // Keep current state during brief network or cold-start failures.
    }
  }
  private async runRequest(action: () => Promise<void>, fallbackMessage: string): Promise<void> {
    this.errorMessage.set('');
    this.isLoading.set(true);
    try { await action(); }
    catch (error: any) { this.errorMessage.set(this.friendlyError(error?.error?.message, fallbackMessage)); }
    finally { this.isLoading.set(false); }
  }

  private trackDraftSeconds(track: Track): number {
    return this.trackTimeDrafts()[track.id] ?? track.previewStartSeconds;
  }

  private friendlyError(message: string | undefined, fallback: string): string {
    if (message === 'Room not found.') {
      return 'Sala não encontrada. O convite pode ter expirado ou o anfitrião pode ter encerrado a sala.';
    }
    return message || fallback;
  }

  private setTrackDraft(trackId: string, seconds: number): void {
    this.trackTimeDrafts.update(drafts => ({ ...drafts, [trackId]: seconds }));
  }

  private readSession(): SavedSession | null {
    try {
      const value = localStorage.getItem(this.sessionKey);
      return value ? JSON.parse(value) as SavedSession : null;
    } catch {
      localStorage.removeItem(this.sessionKey);
      return null;
    }
  }

  private async restoreSession(session: SavedSession): Promise<void> {
    this.isLoading.set(true);
    try {
      const room = await firstValueFrom(this.http.get<Room>(`${this.apiBase}/rooms/${session.roomCode}`));
      if (!room.players.some(player => player.id === session.playerId)) throw new Error('Jogador não encontrado');
      this.enterRoom(room, session.playerId);
    } catch {
      localStorage.removeItem(this.sessionKey);
      this.playerId.set('');
    } finally {
      this.isLoading.set(false);
    }
  }
}
