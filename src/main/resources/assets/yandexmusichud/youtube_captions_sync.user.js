// ==UserScript==
// @name         MusicSubtitles - YouTube Live Captions Sync
// @namespace    https://github.com/Nia_Teppelin_0/MusicSubtitles
// @version      2.4.0
// @description  Мгновенно транслирует субтитры любого видео YouTube прямо в Minecraft (3D-пузыри над миром или в HUD)!
// @author       Nia_Teppelin_0
// @match        *://www.youtube.com/*
// @grant        none
// @run-at       document-idle
// ==/UserScript==

(function () {
    'use strict';

    const MOD_PORT = 38473;
    const PUSH_URL = `http://127.0.0.1:${MOD_PORT}/captions/push`;
    const TRANSCRIPT_URL = `http://127.0.0.1:${MOD_PORT}/captions/transcript`;

    let lastPushedText = '';
    let lastPushTime = 0;
    let lastScanTime = 0;
    let transcriptSentForVideo = '';
    let syncInFlight = false;
    let noticeTimer = null;
    let observedRoot = null;
    let captionObserver = null;

    function getVideoId() {
        try {
            const urlParams = new URLSearchParams(window.location.search);
            return urlParams.get('v') || '';
        } catch {
            return '';
        }
    }

    function getVideoTitle() {
        const titleElem = document.querySelector('h1.ytd-watch-metadata yt-formatted-string') || document.querySelector('h1.title');
        return titleElem ? titleElem.textContent.trim() : document.title.replace(/\s*-\s*YouTube$/, '').trim();
    }

    async function trySyncFullTranscript() {
        const vid = getVideoId();
        if (!vid || vid === transcriptSentForVideo || syncInFlight) return;

        const player = document.getElementById('movie_player') || document.querySelector('.html5-video-player');

        syncInFlight = true;
        try {
            let tracklist = player && typeof player.getOption === 'function' ? player.getOption('captions', 'tracklist') : null;
            if ((!tracklist || tracklist.length === 0) && player && typeof player.loadModule === 'function') {
                try {
                    player.loadModule('captions');
                } catch {}
                tracklist = typeof player.getOption === 'function' ? player.getOption('captions', 'tracklist') : null;
            }
            if (!tracklist || tracklist.length === 0) {
                const playerResp = player && typeof player.getPlayerResponse === 'function' ? player.getPlayerResponse() : null;
                if (playerResp && (!playerResp.videoDetails || playerResp.videoDetails.videoId === vid)) {
                    tracklist = playerResp.captions && playerResp.captions.playerCaptionsTracklistRenderer ? playerResp.captions.playerCaptionsTracklistRenderer.captionTracks : null;
                }
            }
            if (!tracklist || tracklist.length === 0) {
                const initResp = window.ytInitialPlayerResponse;
                if (initResp && initResp.videoDetails && initResp.videoDetails.videoId === vid) {
                    tracklist = initResp.captions && initResp.captions.playerCaptionsTracklistRenderer ? initResp.captions.playerCaptionsTracklistRenderer.captionTracks : null;
                }
            }
            if (!tracklist || tracklist.length === 0) return;

            const normalizedTracks = tracklist.map(t => ({
                baseUrl: t.baseUrl,
                languageCode: (t.languageCode || t.language_code || '').toLowerCase(),
                is_generated: Boolean(t.is_generated || t.kind === 'asr')
            }));

            let bestTrack = normalizedTracks.find(t => t.languageCode === 'ru' && !t.is_generated);
            if (!bestTrack) bestTrack = normalizedTracks.find(t => t.languageCode === 'ru');
            if (!bestTrack) bestTrack = normalizedTracks.find(t => t.languageCode === 'en' && !t.is_generated);
            if (!bestTrack) bestTrack = normalizedTracks.find(t => t.languageCode === 'en');
            if (!bestTrack) bestTrack = normalizedTracks.find(t => !t.is_generated) || normalizedTracks[0];

            if (!bestTrack || !bestTrack.baseUrl) return;

            let json3Url;
            try {
                const u = new URL(bestTrack.baseUrl, window.location.href);
                u.searchParams.set('fmt', 'json3');
                json3Url = u.toString();
            } catch {
                json3Url = bestTrack.baseUrl + (bestTrack.baseUrl.includes('?') ? '&' : '?') + 'fmt=json3';
            }

            const resp = await fetch(json3Url);
            if (!resp.ok) return;

            const data = await resp.json();
            if (!data.events || data.events.length === 0) return;

            const lines = [];
            for (const ev of data.events) {
                if (!ev.segs || ev.segs.length === 0) continue;
                const startMs = ev.tStartMs || 0;
                const durMs = ev.dDurationMs || 3500;
                const lineWords = [];
                const textParts = [];
                let cursorLen = 0;

                for (let sIdx = 0; sIdx < ev.segs.length; sIdx++) {
                    const seg = ev.segs[sIdx];
                    const cleanSeg = (seg.utf8 || '').replace(/\n+/g, ' ').trim();
                    if (!cleanSeg) continue;
                    const segStart = startMs + (seg.tOffsetMs || 0);
                    let segEnd = startMs + durMs;
                    for (let nIdx = sIdx + 1; nIdx < ev.segs.length; nIdx++) {
                        if (typeof ev.segs[nIdx].tOffsetMs === 'number' && (ev.segs[nIdx].utf8 || '').trim()) {
                            segEnd = Math.max(segStart + 80, startMs + ev.segs[nIdx].tOffsetMs);
                            break;
                        }
                    }
                    const tokens = cleanSeg.split(/\s+/).filter(Boolean);
                    const totalChars = tokens.reduce((acc, tk) => acc + tk.length, 0) || 1;
                    let tCur = segStart;
                    const segSpan = Math.max(80, segEnd - segStart);
                    for (let tIdx = 0; tIdx < tokens.length; tIdx++) {
                        const tok = tokens[tIdx];
                        const tokDur = tIdx === tokens.length - 1 ? Math.max(40, segStart + segSpan - tCur) : Math.max(40, Math.round(segSpan * tok.length / totalChars));
                        if (textParts.length > 0) {
                            cursorLen += 1;
                        }
                        const charStart = cursorLen;
                        textParts.push(tok);
                        cursorLen += tok.length;
                        lineWords.push({
                            startMs: tCur,
                            endMs: Math.min(startMs + durMs, tCur + tokDur),
                            text: tok,
                            charStart: charStart,
                            charEnd: cursorLen
                        });
                        tCur += tokDur;
                    }
                }

                const text = textParts.join(' ').trim();
                if (text && text !== '\n') {
                    lines.push(lineWords.length > 0
                        ? { startMs: startMs, endMs: startMs + durMs, text: text, words: lineWords }
                        : { startMs: startMs, endMs: startMs + durMs, text: text });
                }
            }

            if (lines.length > 0) {
                const title = getVideoTitle();
                const postResp = await fetch(TRANSCRIPT_URL, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ videoTitle: title, lines: lines })
                });
                if (!postResp.ok) return;
                transcriptSentForVideo = vid;
                showSyncNotice(`⚡ Субтитры синхронизированы с Minecraft (${lines.length} строк)`);
            }
        } catch {
            return;
        } finally {
            syncInFlight = false;
        }
    }

    function checkLiveCaptions() {
        const now = Date.now();
        if (now - lastScanTime < 120) return;
        lastScanTime = now;

        const vid = getVideoId();
        if (vid && vid === transcriptSentForVideo) return;

        const root = observedRoot || document;
        const segments = root.querySelectorAll('.ytp-caption-segment');
        if (!segments || segments.length === 0) return;

        const fullText = Array.from(segments).map(s => s.textContent).join(' ').trim();
        if (fullText && fullText !== lastPushedText && (now - lastPushTime > 400)) {
            lastPushedText = fullText;
            lastPushTime = now;

            fetch(PUSH_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    text: fullText,
                    durationMs: 3800
                })
            }).catch(() => {});
        }
    }

    function bindCaptionObserverTarget() {
        if (!captionObserver) return;
        const playerRoot = document.querySelector('.ytp-caption-window-container')
            || document.getElementById('movie_player')
            || document.querySelector('.html5-video-player')
            || document.body;
        if (!playerRoot || (playerRoot === observedRoot && observedRoot.isConnected)) return;
        captionObserver.disconnect();
        observedRoot = playerRoot;
        captionObserver.observe(playerRoot, { childList: true, subtree: true, characterData: true });
    }

    function setupCaptionObserver() {
        captionObserver = new MutationObserver(() => {
            checkLiveCaptions();
        });
        bindCaptionObserverTarget();
    }

    function showSyncNotice(msg) {
        let badge = document.getElementById('ms-yt-badge');
        if (!badge) {
            badge = document.createElement('div');
            badge.id = 'ms-yt-badge';
            badge.style.position = 'fixed';
            badge.style.bottom = '24px';
            badge.style.right = '24px';
            badge.style.backgroundColor = 'rgba(10, 10, 20, 0.88)';
            badge.style.border = '1px solid #FFCC00';
            badge.style.borderRadius = '8px';
            badge.style.padding = '8px 14px';
            badge.style.color = '#FFFFFF';
            badge.style.fontFamily = 'Roboto, Arial, sans-serif';
            badge.style.fontSize = '12px';
            badge.style.zIndex = '999999';
            badge.style.boxShadow = '0 4px 16px rgba(0,0,0,0.6)';
            badge.style.transition = 'opacity 0.5s ease';
            document.body.appendChild(badge);
        }
        badge.replaceChildren();
        const tag = document.createElement('span');
        tag.style.color = '#FFCC00';
        tag.style.fontWeight = 'bold';
        tag.textContent = '✦ MusicSubtitles';
        badge.appendChild(tag);
        badge.appendChild(document.createTextNode(` | ${msg}`));
        badge.style.opacity = '1';
        if (noticeTimer) {
            clearTimeout(noticeTimer);
        }
        noticeTimer = setTimeout(() => {
            if (badge) badge.style.opacity = '0';
        }, 5000);
    }

    setupCaptionObserver();
    window.addEventListener('yt-navigate-finish', () => {
        lastPushedText = '';
        lastPushTime = 0;
        bindCaptionObserverTarget();
        setTimeout(trySyncFullTranscript, 1200);
    });
    setInterval(() => {
        bindCaptionObserverTarget();
        trySyncFullTranscript();
    }, 3500);

})();
