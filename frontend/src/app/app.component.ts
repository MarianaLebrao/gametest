import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

type Screen = 'home' | 'create' | 'lobby' | 'gameroom' | 'round' | 'voting' | 'result' | 'scoreboard' | 'final';
type GameMode = 'preview' | 'blind';
interface Player { id: string; name: string; }
interface Track {
  id: string; videoId: string; title: string; channelTitle: string; thumbnailUrl: string;
  ownerPlayerId: string; ownerName: string;
}
interface Room {
  code: string; hostPlayerId: string; roundCount: number; roundTimer: number; gameMode: GameMode;
  players: Player[]; tracks: Track[];
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly apiBase = '/api';
  private roomPolling?: ReturnType<typeof setInterval>;

  protected readonly currentScreen = signal<Screen>('home');
  protected readonly playerName = signal('');
  protected readonly roomCode = signal('');
  protected readonly playerId = signal('');
  protected readonly room = signal<Room | null>(null);
  protected readonly videoLink = signal('');
  protected readonly errorMessage = signal('');
  protected readonly isLoading = signal(false);
  protected readonly copied = signal(false);
  protected readonly selectedVote = signal<string | null>(null);
  protected readonly roundCount = signal(8);
  protected readonly roundTimer = signal(30);
  protected readonly gameMode = signal<GameMode>('preview');

  ngOnInit(): void {
    const invitedCode = new URLSearchParams(window.location.search).get('room');
    if (invitedCode) this.roomCode.set(invitedCode.toUpperCase().slice(0, 6));
  }

  ngOnDestroy(): void { this.stopPolling(); }
  protected setScreen(screen: Screen): void { this.currentScreen.set(screen); }
  protected onPlayerNameInput(value: string): void { this.playerName.set(value.slice(0, 30)); }
  protected onRoomCodeInput(value: string): void {
    this.roomCode.set(value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6));
  }
  protected onVideoLinkInput(value: string): void { this.videoLink.set(value.trim()); }

  protected async createRoom(): Promise<void> {
    if (!this.playerName().trim()) return;
    await this.runRequest(async () => {
      const room = await firstValueFrom(this.http.post<Room>(`${this.apiBase}/rooms`, {
        hostName: this.playerName().trim(),
        roundCount: this.roundCount(),
        roundTimer: this.roundTimer(),
        gameMode: this.gameMode()
      }));
      this.enterRoom(room, room.hostPlayerId);
    }, 'Could not create the room.');
  }

  protected async joinRoom(): Promise<void> {
    if (!this.playerName().trim() || this.roomCode().length !== 6) return;
    await this.runRequest(async () => {
      const player = await firstValueFrom(this.http.post<Player>(
        `${this.apiBase}/rooms/${this.roomCode()}/players`,
        { playerName: this.playerName().trim() }
      ));
      const room = await firstValueFrom(this.http.get<Room>(`${this.apiBase}/rooms/${this.roomCode()}`));
      this.enterRoom(room, player.id);
    }, 'Could not join the room. Check the code.');
  }

  protected async addTrack(): Promise<void> {
    if (!this.videoLink() || !this.room() || !this.playerId()) return;
    await this.runRequest(async () => {
      await firstValueFrom(this.http.post<Track>(`${this.apiBase}/rooms/${this.room()!.code}/tracks`, {
        playerId: this.playerId(),
        videoUrl: this.videoLink()
      }));
      this.videoLink.set('');
      await this.refreshRoom();
    }, 'Could not add this video. Check that the YouTube link is public.');
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
  protected trackCountFor(playerId: string): number {
    return this.room()?.tracks.filter(track => track.ownerPlayerId === playerId).length ?? 0;
  }
  protected everyoneReady(): boolean {
    const room = this.room();
    return !!room?.players.length && room.players.every(player => this.trackCountFor(player.id) > 0);
  }
  protected currentTrack(): Track | null { return this.room()?.tracks[0] ?? null; }
  protected setRoundCount(value: number): void { this.roundCount.set(value); }
  protected setRoundTimer(value: number): void { this.roundTimer.set(value); }
  protected setGameMode(value: GameMode): void { this.gameMode.set(value); }

  private enterRoom(room: Room, playerId: string): void {
    this.room.set(room);
    this.roomCode.set(room.code);
    this.playerId.set(playerId);
    this.currentScreen.set('lobby');
    window.history.replaceState({}, document.title, `${window.location.pathname}?room=${room.code}`);
    this.startPolling();
  }
  private startPolling(): void {
    this.stopPolling();
    this.roomPolling = setInterval(() => this.refreshRoom(), 3000);
  }
  private stopPolling(): void { if (this.roomPolling) clearInterval(this.roomPolling); }
  private async refreshRoom(): Promise<void> {
    if (!this.roomCode()) return;
    try {
      this.room.set(await firstValueFrom(this.http.get<Room>(`${this.apiBase}/rooms/${this.roomCode()}`)));
    } catch {
      // Keep current state during brief network or cold-start failures.
    }
  }
  private async runRequest(action: () => Promise<void>, fallbackMessage: string): Promise<void> {
    this.errorMessage.set('');
    this.isLoading.set(true);
    try {
      await action();
    } catch (error: any) {
      this.errorMessage.set(error?.error?.message || fallbackMessage);
    } finally {
      this.isLoading.set(false);
    }
  }
}
