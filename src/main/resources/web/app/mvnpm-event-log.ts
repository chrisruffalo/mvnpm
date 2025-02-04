import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';

/**
 * This component shows the Event log
 */
@customElement('mvnpm-event-log')
export class MvnpmEventLog extends LitElement {
    static styles = css`
    :host {
      display: flex;
      gap: 10px;
      width: 100%;
      max-height: 40vh;
    }
    .console {
      display: flex;
      flex-direction: column;
      width: 100%;
      height: 100%;
      padding-left: 20px;
      padding-right: 20px;
      background: black;
      font-family: 'Courier New', monospace;
      font-size: small;
      filter: brightness(0.85);
    }
    .line {
        display: flex;
        flex-direction: row;
        gap: 10px;
    }

  `;

    @state({ type: Array })
    private _initEventLog: any[] | null = null;

    private _eventLogEntry = (event: CustomEvent) => this._receiveLogEntry(event.detail);

    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();

        fetch("/api/eventlog/top")
            .then(response => response.json())
            .then(initEventLog => (this._initEventLog = this._addMultipleToLog(this._initEventLog, initEventLog)));
        
        document.addEventListener('eventLogEntryEvent', this._eventLogEntry, false);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        document.removeEventListener('eventLogEntryEvent', this._eventLogEntry, false);
    }

    private _receiveLogEntry(initEventLog: any) {
        this._initEventLog = this._addToLog(this._initEventLog, initEventLog);
    }

    render() {
        return html`
      <div class="console">
        ${this._renderInitEventLog()}
      </div>
    `;
    }

    private _renderInitEventLog() {
        if (this._initEventLog && this._initEventLog.length > 0) {
            return html`
                ${this._initEventLog.map((entry) => {
                    return html`${this._renderLine(entry)}`
                })}
              `;
        } else {
            return html`<p>Nothing in the event log</p>`;
        }
    }

    private _renderLine(entry){
        let formattedTime = entry.time.substring(0, entry.time.indexOf(".")).replace('T',' ');
        
        return html`<div class="line">
                        <span style="color: grey">${formattedTime}</span>
                        <span style="color: lightblue">${entry.groupId}</span>
                        <span style="color: lightyellow">${entry.artifactId}</span>
                        <span style="color: lightpink">${entry.version}</span>
                        <span style="color: lightgrey">[${entry.stage}]</span>
                        <span style="color: ${entry.color}">${entry.message}</span>
                    </div>`;
    }

    private _addToLog(queue: any[] | null, item: any) {
        if (queue && queue.length > 0) {
            return [item, ...queue];
        } else {
            return [item];
        }
    }

    private _addMultipleToLog(queue: any[] | null, items: any[]) {
        if (queue && queue.length > 0) {
            return [...items.reverse(), ...queue];
        } else {
            return items.reverse();
        }
    }

}