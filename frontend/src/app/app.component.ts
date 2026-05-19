import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';

type Screen =
  | 'home'
  | 'create'
  | 'import'
  | 'lobby'
  | 'identity'
  | 'gameroom'
  | 'round'
  | 'voting'
  | 'result'
  | 'scoreboard'
  | 'final';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  private readonly http = inject(HttpClient);
  private readonly apiBase = 'https://gametest-0trr.onrender.com';

  protected readonly currentScreen = signal<Screen>('home');
  protected readonly roomCode = signal('');
  protected readonly playlistLink = signal('');
  protected readonly spotifyAccessToken = signal('');
  protected readonly importError = signal('');
  protected readonly isImporting = signal(false);

  protected readonly playlistName = signal('Summer Vibes 2024');
  protected readonly importedTracks = signal<Array<{ trackName: string; ownerDisplayName: string }>>([]);
  protected readonly importedMembers = signal<Array<{ spotifyUserId: string; displayName: string }>>([]);

  protected readonly selectedIdentity = signal<string | null>(null);
  protected readonly selectedVote = signal<string | null>(null);
  protected readonly copied = signal(false);
  protected readonly roundCount = signal(8);
  protected readonly roundTimer = signal(30);
  protected readonly gameMode = signal<'preview' | 'blind'>('preview');

  ngOnInit(): void {
    const params = new URLSearchParams(window.location.search);
    const accessToken = params.get('accessToken');
    if (accessToken) {
      this.spotifyAccessToken.set(accessToken);
      this.importError.set('');
      this.currentScreen.set('create');
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }

  protected setScreen(screen: Screen): void {
    this.currentScreen.set(screen);
  }

  protected onRoomCodeInput(value: string): void {
    this.roomCode.set(value.toUpperCase().slice(0, 6));
  }

  protected onPlaylistInput(value: string): void {
    this.playlistLink.set(value);
  }

  protected onSpotifyTokenInput(value: string): void {
    this.spotifyAccessToken.set(value.trim());
  }

  protected connectSpotify(): void {
    this.importError.set('');
    this.http
      .get<{ url: string }>(`${this.apiBase}/api/spotify/login-url`)
      .subscribe({
        next: (res) => {
          if (!res?.url) {
            this.importError.set('Could not generate Spotify login URL.');
            return;
          }
          window.open(res.url, '_blank', 'noopener,noreferrer');
        },
        error: () => this.importError.set('Failed to contact backend for Spotify login.')
      });
  }

  protected importPlaylist(): void {
    if (!this.playlistLink() || !this.spotifyAccessToken()) {
      return;
    }

    this.importError.set('');
    this.isImporting.set(true);

    this.http
      .post<{
        playlistId: string;
        playlistName: string;
        tracks: Array<{ trackName: string; ownerDisplayName: string }>;
        members: Array<{ spotifyUserId: string; displayName: string }>;
      }>(`${this.apiBase}/api/playlists/import`, {
        playlistUrl: this.playlistLink(),
        spotifyAccessToken: this.spotifyAccessToken()
      })
      .subscribe({
        next: (res) => {
          this.playlistName.set(res.playlistName || 'Imported Playlist');
          this.importedTracks.set(res.tracks || []);
          this.importedMembers.set(res.members || []);
          this.isImporting.set(false);
          this.setScreen('import');
        },
        error: (err) => {
          const message = err?.error?.message || 'Failed to import playlist. Check token and playlist URL.';
          this.importError.set(message);
          this.isImporting.set(false);
        }
      });
  }

  protected copyCode(): void {
    this.copied.set(true);
    setTimeout(() => this.copied.set(false), 1800);
  }

  protected setRoundCount(value: number): void {
    this.roundCount.set(value);
  }

  protected setRoundTimer(value: number): void {
    this.roundTimer.set(value);
  }

  protected setGameMode(value: 'preview' | 'blind'): void {
    this.gameMode.set(value);
  }
}
