const API = "/api";
const userId = localStorage.getItem("userId");
const token = localStorage.getItem("token");

if (!userId || !token) {
    window.location.href = "login.html";
}

const money = value => `₹${Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const signedMoney = value => {
    const n = Number(value || 0);
    return `${n >= 0 ? "+" : "-"}${money(Math.abs(n))}`;
};
const setText = (id, value) => {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
};

function showToast(message, type = "success") {
    const toast = document.getElementById("toast");
    if (!toast) return;
    toast.textContent = message;
    toast.className = `toast ${type} visible`;
    clearTimeout(window.toastTimer);
    window.toastTimer = setTimeout(() => toast.classList.remove("visible"), 3000);
}

async function apiFetch(url, options = {}) {
    options.headers = {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(options.headers || {}),
        Authorization: `Bearer ${localStorage.getItem("token") || ""}`
    };
    const response = await fetch(url, options);
    if (response.status === 401 || response.status === 403) {
        localStorage.clear();
        window.location.href = "login.html";
        throw new Error("Session expired");
    }
    return response;
}

setText("username", localStorage.getItem("name") || localStorage.getItem("username") || "User");

let allStocks = [];
let selectedSymbol = null;
let selectedPrice = 0;
let selectedType = null;

async function loadStocks() {
    const body = document.getElementById("stockTableBody");
    if (body) body.innerHTML = `<tr><td colspan="6" class="empty">Loading market...</td></tr>`;
    try {
        const response = await fetch(`${API}/stocks`);
        if (!response.ok) throw new Error("Market unavailable");
        allStocks = await response.json();
        displayStocks(allStocks);
        populateWhatIfStocks();
        populateBacktestStocks();
    } catch (error) {
        if (body) body.innerHTML = `<tr><td colspan="6" class="empty error-state">Unable to load market data.</td></tr>`;
    }
}

function displayStocks(stocks) {
    const body = document.getElementById("stockTableBody");
    if (!body) return;
    body.innerHTML = "";
    if (!stocks.length) {
        body.innerHTML = `<tr><td colspan="6" class="empty">No stocks match your search.</td></tr>`;
        return;
    }
    stocks.forEach(stock => {
        const change = Number(stock.changePercent);
        const row = document.createElement("tr");
        row.innerHTML = `
            <td><strong>${stock.symbol}</strong></td>
            <td><span class="company-name">${stock.company}</span></td>
            <td>${stock.sector}</td>
            <td><strong>${money(stock.price)}</strong></td>
            <td class="${change >= 0 ? "positive" : "negative"}">${change >= 0 ? "+" : ""}${change.toFixed(2)}%</td>
            <td><button class="buy-btn" onclick="buyStock('${stock.symbol}')">Buy</button><button class="sell-btn" onclick="sellStock('${stock.symbol}')">Sell</button></td>`;
        body.appendChild(row);
    });
}

function populateBacktestStocks() {
    const select = document.getElementById("backtestSymbol");
    if (!select) return;
    const previous = select.value;
    select.innerHTML = allStocks.map(s => `<option value="${s.symbol}">${s.symbol} — ${s.company}</option>`).join("");
    if (previous && allStocks.some(s => s.symbol === previous)) select.value = previous;
}

function populateWhatIfStocks() {
    const select = document.getElementById("whatIfSymbol");
    if (!select) return;
    const previous = select.value;
    select.innerHTML = allStocks.map(s => `<option value="${s.symbol}">${s.symbol} — ${s.company}</option>`).join("");
    if (previous && allStocks.some(s => s.symbol === previous)) select.value = previous;
}

async function openTradeModal(symbol, type) {
    selectedSymbol = symbol;
    selectedType = type;
    const modal = document.getElementById("tradeModal");
    document.getElementById("tradeError").textContent = "";
    document.getElementById("tradeQuantity").value = "";
    document.getElementById("tradeTotal").textContent = money(0);
    document.getElementById("tradeTitle").textContent = type === "BUY" ? "Buy Stock" : "Sell Stock";
    document.getElementById("confirmTrade").textContent = type === "BUY" ? "Confirm Buy" : "Confirm Sell";
    try {
        const response = await fetch(`${API}/stocks/${symbol}`);
        const stock = await response.json();
        if (!response.ok) throw new Error(stock.message || "Stock unavailable");
        selectedPrice = Number(stock.price);
        document.getElementById("tradeStockInfo").textContent = `${stock.symbol} • ${stock.company}`;
        document.getElementById("tradePrice").textContent = money(selectedPrice);
        modal.classList.remove("hidden");
        document.getElementById("tradeQuantity").focus();
    } catch (error) {
        showToast(error.message, "error");
    }
}
function buyStock(symbol) { openTradeModal(symbol, "BUY"); }
function sellStock(symbol) { openTradeModal(symbol, "SELL"); }

const tradeQuantity = document.getElementById("tradeQuantity");
if (tradeQuantity) tradeQuantity.addEventListener("input", () => {
    const q = Number(tradeQuantity.value);
    document.getElementById("tradeTotal").textContent = q > 0 ? money(q * selectedPrice) : money(0);
});

document.getElementById("confirmTrade")?.addEventListener("click", async () => {
    const quantity = Number(tradeQuantity.value);
    const errorEl = document.getElementById("tradeError");
    const button = document.getElementById("confirmTrade");
    errorEl.textContent = "";
    if (!Number.isInteger(quantity) || quantity <= 0) {
        errorEl.textContent = "Enter a valid whole-number quantity.";
        return;
    }
    button.disabled = true;
    button.textContent = "Processing...";
    try {
        const response = await apiFetch(`${API}/users/${userId}/${selectedType === "BUY" ? "buy" : "sell"}`, {
            method: "POST",
            body: JSON.stringify({ symbol: selectedSymbol, quantity })
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Trade failed");
        document.getElementById("tradeModal").classList.add("hidden");
        await refreshDashboard();
        showToast(`${selectedType} ${quantity} × ${data.symbol} successful.`);
    } catch (error) {
        errorEl.textContent = error.message;
    } finally {
        button.disabled = false;
        button.textContent = selectedType === "BUY" ? "Confirm Buy" : "Confirm Sell";
    }
});

document.getElementById("closeModal")?.addEventListener("click", () => document.getElementById("tradeModal").classList.add("hidden"));
document.getElementById("tradeModal")?.addEventListener("click", e => { if (e.target.id === "tradeModal") e.target.classList.add("hidden"); });
document.addEventListener("keydown", e => { if (e.key === "Escape") document.getElementById("tradeModal")?.classList.add("hidden"); });

async function loadPortfolio() {
    try {
        const response = await apiFetch(`${API}/users/${userId}/portfolio`);
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Portfolio unavailable");
        setText("cashBalance", money(data.cashBalance));
        displayHoldings(data.holdings);
    } catch (error) {
        const body = document.getElementById("holdingsTableBody");
        if (body) body.innerHTML = `<tr><td colspan="5" class="empty error-state">Unable to load portfolio.</td></tr>`;
    }
}

function displayHoldings(holdings) {
    const body = document.getElementById("holdingsTableBody");
    if (!body) return;
    body.innerHTML = "";
    let portfolioValue = 0, totalPnl = 0;
    if (!holdings.length) {
        body.innerHTML = `<tr><td colspan="5" class="empty">No holdings yet. Buy a stock to build your portfolio.</td></tr>`;
        setText("portfolioValue", money(0));
        setText("totalPnL", money(0));
        return;
    }
    holdings.forEach(h => {
        const invested = Number(h.averagePrice) * h.quantity;
        const current = Number(h.currentPrice) * h.quantity;
        const pnl = current - invested;
        portfolioValue += current;
        totalPnl += pnl;
        const row = document.createElement("tr");
        row.innerHTML = `<td><strong>${h.symbol}</strong><br><span class="company-name">${h.company}</span></td><td>${h.quantity}</td><td>${money(h.averagePrice)}</td><td>${money(h.currentPrice)}</td><td class="${pnl >= 0 ? "positive" : "negative"}">${signedMoney(pnl)}</td>`;
        body.appendChild(row);
    });
    setText("portfolioValue", money(portfolioValue));
    const pnlEl = document.getElementById("totalPnL");
    if (pnlEl) { pnlEl.textContent = signedMoney(totalPnl); pnlEl.className = totalPnl >= 0 ? "positive" : "negative"; }
}

async function loadInsights() {
    try {
        const response = await apiFetch(`${API}/users/${userId}/insights`);
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Insights unavailable");
        renderInsights(data);
    } catch (error) {
        setText("diversificationScore", "—");
        setText("scoreTitle", "Unavailable");
        setText("riskMessage", error.message);
    }
}
function renderInsights(data) {
    const score = Number(data.diversificationScore || 0);
    setText("diversificationScore", Math.round(score));
    setText("cashPercentage", `${Number(data.cashPercentage || 0).toFixed(1)}%`);
    setText("riskMessage", data.riskMessage || "");
    setText("scoreTitle", score >= 75 ? "Well diversified" : score >= 50 ? "Moderately diversified" : score > 0 ? "Concentration detected" : "No portfolio yet");
    const ring = document.getElementById("scoreRing");
    if (ring) ring.style.setProperty("--score", `${score * 3.6}deg`);
    const meter = document.getElementById("cashMeter");
    if (meter) meter.style.width = `${Math.min(100, Math.max(0, Number(data.cashPercentage || 0)))}%`;
    setHoldingInsight("bestHolding", "bestHoldingPnl", data.bestHolding);
    setHoldingInsight("worstHolding", "worstHoldingPnl", data.worstHolding);
    const allocation = document.getElementById("sectorAllocation");
    if (allocation) {
        const sectors = data.sectorAllocation || [];
        allocation.innerHTML = sectors.length ? sectors.map(s => `<div class="allocation-row"><div class="allocation-label"><span>${s.sector}</span><strong>${Number(s.percentage).toFixed(1)}%</strong></div><div class="allocation-track"><span style="width:${Math.min(100, s.percentage)}%"></span></div></div>`).join("") : `<div class="allocation-empty">No holdings yet. Sector analysis will appear here.</div>`;
    }
    setText("allocationTotal", `${(data.sectorAllocation || []).reduce((a, b) => a + Number(b.percentage), 0).toFixed(1)}%`);
}
function setHoldingInsight(nameId, pnlId, item) {
    setText(nameId, item?.symbol || "—");
    setText(pnlId, item ? signedMoney(item.pnl) : "No holdings");
    const el = document.getElementById(pnlId);
    if (el) el.className = item && Number(item.pnl) < 0 ? "negative" : "positive";
}

async function loadAnalytics() {
    try {
        const response = await apiFetch(`${API}/users/${userId}/analytics`);
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Analytics unavailable");
        setText("totalOrders", data.totalOrders);
        setText("buyOrders", data.buyOrders);
        setText("sellOrders", data.sellOrders);
        setText("mostTraded", data.mostTraded);
        setText("buyVolume", money(data.buyVolume));
        setText("sellVolume", money(data.sellVolume));
        setText("realizedPnl", signedMoney(data.realizedPnl));
        setText("openShareCount", data.openShareCount);
    } catch (error) {
        setText("mostTraded", "—");
    }
}

async function runWhatIf() {
    const symbol = document.getElementById("whatIfSymbol")?.value;
    const change = Number(document.getElementById("whatIfChange")?.value);
    const result = document.getElementById("whatIfResult");
    if (!symbol || Number.isNaN(change) || change < -100) {
        showToast("Enter a valid scenario (minimum -100%).", "error");
        return;
    }
    result.innerHTML = `<div class="scenario-loading">Calculating scenario...</div>`;
    try {
        const response = await apiFetch(`${API}/users/${userId}/what-if`, {
            method: "POST",
            body: JSON.stringify({ symbol, percentChange: change })
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Simulation failed");
        const impactClass = Number(data.portfolioImpact) >= 0 ? "positive" : "negative";
        result.innerHTML = `
            <div class="scenario-head"><div><span class="muted-label">${data.symbol} SCENARIO</span><h3>${data.percentChange >= 0 ? "+" : ""}${Number(data.percentChange).toFixed(1)}% price move</h3></div><span class="scenario-tag">${data.affectedQuantity} shares held</span></div>
            <div class="scenario-grid">
                <div><span>Current price</span><strong>${money(data.currentPrice)}</strong></div>
                <div><span>Projected price</span><strong>${money(data.projectedPrice)}</strong></div>
                <div><span>Portfolio impact</span><strong class="${impactClass}">${signedMoney(data.portfolioImpact)}</strong></div>
                <div><span>Projected P&amp;L</span><strong class="${Number(data.projectedPnl) >= 0 ? "positive" : "negative"}">${signedMoney(data.projectedPnl)}</strong></div>
            </div>
            <div class="scenario-footer">If ${data.symbol} moves ${data.percentChange >= 0 ? "+" : ""}${Number(data.percentChange).toFixed(1)}%, your portfolio value would change by <strong class="${impactClass}">${signedMoney(data.portfolioImpact)}</strong>.</div>`;
    } catch (error) {
        result.innerHTML = `<div class="scenario-empty"><strong>Simulation failed</strong><span>${error.message}</span></div>`;
    }
}

function formatStrategy(strategy) {
    return {
        BUY_HOLD: "Buy & Hold",
        SMA_CROSSOVER: "SMA Crossover (5/20)",
        MOMENTUM: "10-Day Momentum"
    }[strategy] || strategy;
}

function renderEquityChart(values) {
    if (!values || values.length < 2) return "";
    const width = 720, height = 220, padX = 12, padY = 18;
    const min = Math.min(...values), max = Math.max(...values);
    const range = max - min || 1;
    const points = values.map((v, i) => {
        const x = padX + (i / (values.length - 1)) * (width - padX * 2);
        const y = height - padY - ((v - min) / range) * (height - padY * 2);
        return [x, y];
    });
    const line = points.map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`).join(" ");
    const area = `${points[0][0]},${height - padY} ${line} ${points.at(-1)[0]},${height - padY}`;
    const last = points.at(-1);
    return `<svg class="equity-chart" viewBox="0 0 ${width} ${height}" role="img" aria-label="Equity curve">
        <line class="chart-grid" x1="0" y1="${height - padY}" x2="${width}" y2="${height - padY}" />
        <line class="chart-grid" x1="0" y1="${padY}" x2="${width}" y2="${padY}" />
        <polygon class="chart-fill" points="${area}" />
        <polyline class="chart-line" points="${line}" />
        <circle class="chart-dot" cx="${last[0]}" cy="${last[1]}" r="5" />
    </svg>`;
}

async function runBacktest() {
    const symbol = document.getElementById("backtestSymbol")?.value;
    const strategy = document.getElementById("backtestStrategy")?.value;
    const initialCapital = Number(document.getElementById("backtestCapital")?.value);
    const lookbackDays = Number(document.getElementById("backtestDays")?.value);
    const result = document.getElementById("quantResult");
    if (!symbol || !strategy || initialCapital < 1000 || !lookbackDays) {
        showToast("Enter valid backtest parameters.", "error");
        return;
    }
    result.innerHTML = `<div class="scenario-loading">Running strategy simulation...</div>`;
    try {
        const response = await apiFetch(`${API}/users/${userId}/quant/backtest`, {
            method: "POST",
            body: JSON.stringify({ symbol, strategy, initialCapital, lookbackDays })
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Backtest failed");
        const returnClass = Number(data.totalReturn) >= 0 ? "positive" : "negative";
        const benchmarkClass = Number(data.benchmarkReturn) >= 0 ? "positive" : "negative";
        result.innerHTML = `
            <div class="quant-dashboard">
                <div class="quant-chart-card">
                    <div class="quant-card-head">
                        <div><span class="muted-label">${data.symbol} • ${data.lookbackDays} DAYS</span><h3>${formatStrategy(data.strategy)}</h3></div>
                        <div class="quant-return"><strong class="${returnClass}">${Number(data.totalReturn) >= 0 ? "+" : ""}${Number(data.totalReturn).toFixed(2)}%</strong><span>strategy return</span></div>
                    </div>
                    ${renderEquityChart(data.equityCurve)}
                    <div class="quant-methodology"><strong>Research note:</strong> The simulator uses a deterministic synthetic price path seeded by the instrument, so the same experiment is reproducible. This is a learning/backtesting environment, not live financial data.</div>
                </div>
                <div class="quant-metrics-card">
                    <div class="quant-card-head"><div><span class="muted-label">RISK &amp; PERFORMANCE</span><h3>Strategy diagnostics</h3></div></div>
                    <div class="quant-metrics">
                        <div class="quant-metric"><span>Final Value</span><strong>${money(data.finalValue)}</strong></div>
                        <div class="quant-metric"><span>Benchmark</span><strong class="${benchmarkClass}">${Number(data.benchmarkReturn) >= 0 ? "+" : ""}${Number(data.benchmarkReturn).toFixed(2)}%</strong></div>
                        <div class="quant-metric"><span>Max Drawdown</span><strong class="negative">-${Number(data.maxDrawdown).toFixed(2)}%</strong></div>
                        <div class="quant-metric"><span>Volatility</span><strong>${Number(data.annualizedVolatility).toFixed(2)}%</strong></div>
                        <div class="quant-metric"><span>Sharpe Ratio</span><strong>${Number(data.sharpeRatio).toFixed(2)}</strong></div>
                        <div class="quant-metric"><span>Trades</span><strong>${data.trades}</strong></div>
                    </div>
                    <div class="quant-methodology"><strong>Interpretation:</strong> Higher Sharpe generally means better risk-adjusted return. Max drawdown shows the largest peak-to-trough decline. Compare these metrics across strategies instead of judging by return alone.</div>
                </div>
            </div>`;
    } catch (error) {
        result.innerHTML = `<div class="scenario-empty"><strong>Backtest failed</strong><span>${error.message}</span></div>`;
    }
}

document.getElementById("runBacktest")?.addEventListener("click", runBacktest);

document.getElementById("runWhatIf")?.addEventListener("click", runWhatIf);

document.getElementById("stockSearch")?.addEventListener("input", e => {
    const q = e.target.value.trim().toLowerCase();
    displayStocks(allStocks.filter(s => s.symbol.toLowerCase().includes(q) || s.company.toLowerCase().includes(q)));
});

document.getElementById("logoutBtn")?.addEventListener("click", () => {
    localStorage.clear();
    window.location.href = "login.html";
});

async function loadOrders() {
    try {
        const response = await apiFetch(`${API}/users/${userId}/orders`);
        const orders = await response.json();
        if (!response.ok) throw new Error(orders.message || "Orders unavailable");
        const body = document.getElementById("ordersTableBody");
        if (!body) return;
        body.innerHTML = orders.length ? "" : `<tr><td colspan="6" class="empty">No orders yet.</td></tr>`;
        orders.forEach(order => {
            const row = document.createElement("tr");
            const typeClass = order.type === "BUY" ? "positive" : "negative";
            const date = new Date(order.createdAt).toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
            row.innerHTML = `<td><strong>${order.symbol}</strong><br><span class="company-name">${order.company}</span></td><td class="${typeClass}"><strong>${order.type}</strong></td><td>${order.quantity}</td><td>${money(order.price)}</td><td>${money(order.totalAmount)}</td><td>${date}</td>`;
            body.appendChild(row);
        });
    } catch (error) {
        const body = document.getElementById("ordersTableBody");
        if (body) body.innerHTML = `<tr><td colspan="6" class="empty error-state">Unable to load orders.</td></tr>`;
    }
}

async function refreshDashboard() {
    await Promise.all([loadStocks(), loadPortfolio(), loadOrders(), loadInsights(), loadAnalytics()]);
}

refreshDashboard();
setInterval(loadStocks, 30000);
setInterval(() => Promise.all([loadPortfolio(), loadInsights(), loadAnalytics()]), 30000);
