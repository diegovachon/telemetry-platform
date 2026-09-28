import { Component, ElementRef, AfterViewInit, OnDestroy, OnInit, ViewChild, PLATFORM_ID, Inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Chart } from 'chart.js/auto';
import { TelemetryService, TelemetryReading } from './telemetry.service';

type MetricView = 'both' | 'cpu' | 'ram';

@Component({
  selector: 'app-root',
  imports: [CommonModule],
  template: `
    <div class="shell">
      <header class="header">
        <h1>Development Environment Telemetry</h1>
        <span class="synced" *ngIf="latest">last synced {{ latest.recordedAt | date:'HH:mm:ss' }}</span>
      </header>

      <p class="status status--loading" *ngIf="loading">Reading telemetry&hellip;</p>
      <p class="status status--error" *ngIf="errorMessage">{{ errorMessage }}</p>

      <section class="gauges" *ngIf="latest">
        <button
          class="gauge gauge--cpu"
          [class.gauge--active]="view === 'cpu'"
          (click)="setView('cpu')"
        >
          <span class="gauge__label">CPU</span>
          <span class="gauge__value">{{ latest.cpuPercent | number:'1.1-1' }}<small>%</small></span>
        </button>
        <button
          class="gauge gauge--ram"
          [class.gauge--active]="view === 'ram'"
          (click)="setView('ram')"
        >
          <span class="gauge__label">RAM</span>
          <span class="gauge__value">{{ latest.ramPercent | number:'1.1-1' }}<small>%</small></span>
        </button>
        <button
          class="gauge gauge--both"
          [class.gauge--active]="view === 'both'"
          (click)="setView('both')"
        >
          <span class="gauge__label">Show both</span>
        </button>
      </section>

      <div class="charts">
        <div
          class="chart-frame chart-frame--cpu"
          [style.display]="view === 'ram' ? 'none' : 'flex'"
        >
          <span class="chart-frame__label">CPU %</span>
          <canvas #cpuCanvas></canvas>
        </div>
        <div
          class="chart-frame chart-frame--ram"
          [style.display]="view === 'cpu' ? 'none' : 'flex'"
        >
          <span class="chart-frame__label">RAM %</span>
          <canvas #ramCanvas></canvas>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
    @import url('https://fonts.googleapis.com/css2?family=Space+Mono:wght@400;700&family=Inter:wght@400;500;600&display=swap');

    :host {
      --bg: #0b0e14;
      --surface: #12161f;
      --border: #232838;
      --text: #e7eaf0;
      --text-muted: #7c8598;
      --cpu: #ffb020;
      --ram: #43d9c8;
      --danger: #ff5c5c;

      display: block;
      height: 100vh;
      background: var(--bg);
      color: var(--text);
      font-family: 'Inter', sans-serif;
    }

    .shell {
      display: flex;
      flex-direction: column;
      height: 100%;
      box-sizing: border-box;
      padding: 24px 32px;
      gap: 16px;
    }

    .header {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      border-bottom: 1px solid var(--border);
      padding-bottom: 14px;
    }

    .header h1 {
      margin: 0;
      font-family: 'Space Mono', monospace;
      font-size: 20px;
      font-weight: 700;
      letter-spacing: 0.04em;
      text-transform: uppercase;
    }

    .synced {
      font-family: 'Space Mono', monospace;
      font-size: 12px;
      color: var(--text-muted);
    }

    .status {
      font-family: 'Space Mono', monospace;
      font-size: 13px;
      margin: 0;
    }

    .status--loading {
      color: var(--text-muted);
    }

    .status--error {
      color: var(--danger);
    }

    .gauges {
      display: flex;
      gap: 16px;
    }

    .gauge {
      flex: 1;
      background: var(--surface);
      border: 1px solid var(--border);
      border-left: 3px solid transparent;
      border-radius: 4px;
      padding: 14px 18px;
      display: flex;
      flex-direction: column;
      gap: 4px;
      font-family: inherit;
      color: var(--text);
      cursor: pointer;
      text-align: left;
      transition: background 0.15s ease, border-color 0.15s ease;
    }

    .gauge:hover {
      background: var(--border);
    }

    .gauge--cpu {
      border-left-color: var(--cpu);
    }

    .gauge--ram {
      border-left-color: var(--ram);
    }

    .gauge--both {
      justify-content: center;
      color: var(--text-muted);
    }

    .gauge--active {
      background: var(--border);
    }

    .gauge--cpu.gauge--active {
      box-shadow: inset 0 0 0 1px var(--cpu);
    }

    .gauge--ram.gauge--active {
      box-shadow: inset 0 0 0 1px var(--ram);
    }

    .gauge--both.gauge--active {
      box-shadow: inset 0 0 0 1px var(--text-muted);
      color: var(--text);
    }

    .gauge__label {
      font-family: 'Space Mono', monospace;
      font-size: 11px;
      letter-spacing: 0.08em;
      color: var(--text-muted);
    }

    .gauge__value {
      font-family: 'Space Mono', monospace;
      font-size: 32px;
      font-weight: 700;
      line-height: 1;
    }

    .gauge__value small {
      font-size: 16px;
      font-weight: 400;
      color: var(--text-muted);
      margin-left: 2px;
    }

    .charts {
      flex: 1;
      min-height: 0;
      display: flex;
      gap: 16px;
    }

    .chart-frame {
      position: relative;
      flex: 1;
      min-width: 0;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 4px;
      padding: 20px;
      box-sizing: border-box;
      flex-direction: column;
      gap: 10px;
    }

    .chart-frame > canvas {
      flex: 1;
      min-height: 0;
      box-sizing: border-box;
    }

    .chart-frame__label {
      font-family: 'Space Mono', monospace;
      font-size: 11px;
      letter-spacing: 0.08em;
      color: var(--text-muted);
    }

    /* Corner-bracket motif -- a restrained nod to instrument-panel
       bezels, one per chart, color-matched to that chart's metric. */
    .chart-frame::before,
    .chart-frame::after {
      content: '';
      position: absolute;
      width: 16px;
      height: 16px;
    }

    .chart-frame::before {
      top: 8px;
      left: 8px;
      border-top: 2px solid currentColor;
      border-left: 2px solid currentColor;
    }

    .chart-frame::after {
      bottom: 8px;
      right: 8px;
      border-bottom: 2px solid currentColor;
      border-right: 2px solid currentColor;
    }

    .chart-frame--cpu {
      color: var(--cpu);
    }

    .chart-frame--ram {
      color: var(--ram);
    }
    `,
  ],
})
export class App implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('cpuCanvas') cpuCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('ramCanvas') ramCanvas!: ElementRef<HTMLCanvasElement>;

  loading = true;
  errorMessage: string | null = null;
  latest: TelemetryReading | null = null;
  view: MetricView = 'both';

  private pendingReadings: TelemetryReading[] | null = null;
  private viewReady = false;
  private readonly isBrowser: boolean;
  private refreshTimer: ReturnType<typeof setInterval> | null = null;
  private cpuChart: Chart | null = null;
  private ramChart: Chart | null = null;

  constructor(
    private telemetryService: TelemetryService,
    private changeDetector: ChangeDetectorRef,
    @Inject(PLATFORM_ID) platformId: object
  ) {
    // SSR runs this component on the server (Node), which has no real
    // <canvas>/2D context -- Chart.js throws trying to use one there.
    // Text (loading/error/gauge state) is fine to render server-side;
    // the actual charts must wait for the browser.
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    this.loadReadings();

    if (this.isBrowser) {
      this.refreshTimer = setInterval(() => this.loadReadings(), 10_000);
    }
  }

  ngOnDestroy(): void {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
    this.cpuChart?.destroy();
    this.ramChart?.destroy();
  }

  private loadReadings(): void {
    this.telemetryService.getReadings().subscribe({
      next: (readings) => {
        this.loading = false;
        this.errorMessage = null;
        this.latest = readings.length ? readings[readings.length - 1] : null;
        this.pendingReadings = readings;
        this.tryRenderCharts();
        this.changeDetector.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage =
          err.status === 401
            ? "Can't authenticate -- check that API_KEY in config.ts matches TELEMETRY_API_KEY."
            : `Can't reach the API (${err.status}). Check it's running on port 8080.`;
        this.changeDetector.detectChanges();
      },
    });
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.tryRenderCharts();
  }

  setView(view: MetricView): void {
    this.view = view;
    // Both canvases stay mounted at all times (hidden via CSS, not
    // *ngIf) specifically so their Chart.js instances survive toggling
    // -- Chart.js's ResizeObserver picks up the layout change and
    // resizes the now-visible chart to fill the freed-up space.
  }

  private tryRenderCharts(): void {
    if (!this.isBrowser || !this.viewReady || !this.pendingReadings) {
      return;
    }
    const readings = this.pendingReadings;
    const labels = readings.map((r) => new Date(r.recordedAt).toLocaleTimeString());

    this.cpuChart = this.renderChart(
      this.cpuCanvas.nativeElement,
      labels,
      'CPU %',
      readings.map((r) => r.cpuPercent),
      '#ffb020',
      this.cpuChart
    );
    this.ramChart = this.renderChart(
      this.ramCanvas.nativeElement,
      labels,
      'RAM %',
      readings.map((r) => r.ramPercent),
      '#43d9c8',
      this.ramChart
    );

    this.pendingReadings = null;
  }

  private renderChart(
    canvas: HTMLCanvasElement,
    labels: string[],
    label: string,
    data: number[],
    color: string,
    existingChart: Chart | null
  ): Chart {
    if (existingChart) {
      existingChart.data.labels = labels;
      existingChart.data.datasets[0].data = data;
      existingChart.update('none');
      return existingChart;
    }

    const gridColor = '#232838';
    const textColor = '#7c8598';

    return new Chart(canvas, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label,
            data,
            borderColor: color,
            backgroundColor: `${color}22`,
            pointRadius: 0,
            borderWidth: 2,
            tension: 0.25,
            fill: true,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { display: false },
        },
        scales: {
          x: {
            grid: { color: gridColor },
            ticks: { color: textColor, font: { family: 'Space Mono', size: 10 }, maxRotation: 0, autoSkip: true },
          },
          y: {
            min: 0,
            max: 100,
            grid: { color: gridColor },
            ticks: { color: textColor, font: { family: 'Space Mono', size: 10 } },
          },
        },
      },
    });
  }
}
