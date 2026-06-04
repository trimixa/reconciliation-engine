document.addEventListener('DOMContentLoaded', () => {
    let currentPage = 0;
    const pageSize = 10;
    
    const elements = {
        totalAnomalies: document.getElementById('total-anomalies'),
        openAnomalies: document.getElementById('open-anomalies'),
        resolvedAnomalies: document.getElementById('resolved-anomalies'),
        resolutionRate: document.getElementById('resolution-rate'),
        tableBody: document.getElementById('anomalies-body'),
        prevBtn: document.getElementById('prev-page'),
        nextBtn: document.getElementById('next-page'),
        pageInfo: document.getElementById('page-info'),
        refreshBtn: document.getElementById('refresh-btn'),
        resolveAllBtn: document.getElementById('resolve-all-btn')
    };

    // Initial Fetch
    fetchStats();
    fetchAnomalies();

    // Setup SSE
    const eventSource = new EventSource('/api/anomalies/stream');
    eventSource.addEventListener('stats', (e) => {
        const stats = JSON.parse(e.data);
        updateStatsUI(stats);
    });

    eventSource.onerror = (error) => {
        console.error("SSE Error:", error);
    };

    // Event Listeners
    elements.prevBtn.addEventListener('click', () => {
        if (currentPage > 0) {
            currentPage--;
            fetchAnomalies();
        }
    });

    elements.nextBtn.addEventListener('click', () => {
        currentPage++;
        fetchAnomalies();
    });

    elements.refreshBtn.addEventListener('click', () => {
        fetchAnomalies();
        fetchStats();
    });

    elements.resolveAllBtn.addEventListener('click', async () => {
        try {
            const idempotencyKey = crypto.randomUUID();
            const res = await fetch('/api/anomalies/resolve-all', {
                method: 'POST',
                headers: {
                    'Idempotency-Key': idempotencyKey
                }
            });
            
            if (res.ok) {
                fetchAnomalies();
                fetchStats();
            } else {
                alert('Failed to resolve all anomalies');
            }
        } catch (error) {
            console.error("Error resolving all anomalies", error);
            alert('Failed to connect to server');
        }
    });

    async function fetchStats() {
        try {
            const res = await fetch('/api/anomalies/stats');
            const stats = await res.json();
            updateStatsUI(stats);
        } catch (error) {
            console.error("Failed to fetch stats", error);
        }
    }

    function updateStatsUI(stats) {
        animateValue(elements.totalAnomalies, parseInt(elements.totalAnomalies.innerText), stats.totalAnomalies, 500);
        animateValue(elements.openAnomalies, parseInt(elements.openAnomalies.innerText), stats.openAnomalies, 500);
        animateValue(elements.resolvedAnomalies, parseInt(elements.resolvedAnomalies.innerText), stats.resolvedAnomalies, 500);
        
        const rate = stats.resolutionRate.toFixed(1);
        elements.resolutionRate.innerText = `${rate}%`;

        if (stats.openAnomalies === 0) {
            elements.resolveAllBtn.disabled = true;
            elements.resolveAllBtn.style.opacity = '0.5';
            elements.resolveAllBtn.style.cursor = 'not-allowed';
        } else {
            elements.resolveAllBtn.disabled = false;
            elements.resolveAllBtn.style.opacity = '1';
            elements.resolveAllBtn.style.cursor = 'pointer';
        }
    }

    async function fetchAnomalies() {
        try {
            const res = await fetch(`/api/anomalies?page=${currentPage}&size=${pageSize}`);
            const data = await res.json();
            
            renderTable(data.content);
            
            elements.pageInfo.innerText = `Page ${data.number + 1} of ${data.totalPages || 1}`;
            elements.prevBtn.disabled = data.first;
            elements.nextBtn.disabled = data.last;
            
        } catch (error) {
            console.error("Failed to fetch anomalies", error);
        }
    }

    function renderTable(anomalies) {
        elements.tableBody.innerHTML = '';
        
        if (anomalies.length === 0) {
            elements.tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:#94a3b8">No anomalies found</td></tr>`;
            return;
        }

        anomalies.forEach((a, index) => {
            const tr = document.createElement('tr');
            tr.style.animation = `fadeInUp 0.3s ease-out ${index * 0.05}s backwards`;
            
            const date = new Date(a.timestamp).toLocaleString();
            const isResolved = a.status === 'RESOLVED';
            
            tr.innerHTML = `
                <td style="font-family: monospace; color: var(--accent-primary);">${a.transactionId}</td>
                <td>${a.reason}</td>
                <td style="color: var(--text-secondary); font-size: 0.875rem;">${date}</td>
                <td>
                    <span class="status-badge ${isResolved ? 'resolved' : 'open'}">
                        ${isResolved ? 'Resolved' : 'Open'}
                    </span>
                </td>
                <td>
                    ${!isResolved ? `<button class="btn resolve-btn" onclick="resolveAnomaly('${a.transactionId}')">Resolve</button>` : '-'}
                </td>
            `;
            elements.tableBody.appendChild(tr);
        });
    }

    window.resolveAnomaly = async function(id) {
        try {
            const idempotencyKey = crypto.randomUUID();
            const res = await fetch(`/api/anomalies/${id}/resolve`, {
                method: 'POST',
                headers: {
                    'Idempotency-Key': idempotencyKey
                }
            });
            
            if (res.ok) {
                fetchAnomalies();
            } else {
                alert('Failed to resolve anomaly');
            }
        } catch (error) {
            console.error("Error resolving anomaly", error);
            alert('Failed to connect to server');
        }
    };

    function animateValue(obj, start, end, duration) {
        if (start === end || isNaN(start) || isNaN(end)) {
            obj.innerHTML = end;
            return;
        }
        let startTimestamp = null;
        const step = (timestamp) => {
            if (!startTimestamp) startTimestamp = timestamp;
            const progress = Math.min((timestamp - startTimestamp) / duration, 1);
            obj.innerHTML = Math.floor(progress * (end - start) + start);
            if (progress < 1) {
                window.requestAnimationFrame(step);
            }
        };
        window.requestAnimationFrame(step);
    }
});
