const db = require('../db');

let crumb = null;
let cookies = '';

function getProxyUrl() {
  const row = db.prepare("SELECT value FROM settings WHERE key = 'proxy_url'").get();
  return (row?.value || '').trim();
}

function proxyRewrite(url) {
  const proxy = getProxyUrl();
  if (!proxy) return url;
  // Supported formats:
  //   https://corsproxy.io/?           → prepend: https://corsproxy.io/?https://query1...
  //   https://my-proxy.com/yahoo       → replace base: https://my-proxy.com/yahoo/v8/finance/...
  //   http://localhost:3001            → replace base: http://localhost:3001/v8/finance/...
  if (proxy.endsWith('?')) {
    return proxy + encodeURIComponent(url);
  }
  // Replace the Yahoo host with the proxy base
  return url
    .replace('https://query1.finance.yahoo.com', proxy)
    .replace('https://query2.finance.yahoo.com', proxy);
}

async function yf(url, opts = {}) {
  const proxied = proxyRewrite(url);
  const resp = await fetch(proxied, opts);
  return resp;
}

async function refreshCrumb() {
  try {
    const r1 = await yf('https://fc.yahoo.com/cupcake', { redirect: 'manual' });
    cookies = r1.headers.get('set-cookie') || '';
    const r2 = await yf('https://query2.finance.yahoo.com/v1/test/getcrumb', {
      headers: { Cookie: cookies }
    });
    crumb = await r2.text();
  } catch (err) {
    console.error('Crumb refresh failed:', err.message);
  }
}

async function fetchQuote(ticker) {
  const url = `https://query1.finance.yahoo.com/v8/finance/chart/${encodeURIComponent(ticker)}?range=1d&interval=1m`;
  const resp = await yf(url);
  if (!resp.ok) throw new Error(`Yahoo quote ${ticker}: ${resp.status}`);
  const data = await resp.json();
  const result = data.chart?.result?.[0];
  if (!result) throw new Error(`No data for ${ticker}`);
  const meta = result.meta;
  const quotes = result.indicators?.quote?.[0] || {};
  const highs = (quotes.high || []).filter(v => v != null && v > 0);
  const lows = (quotes.low || []).filter(v => v != null && v > 0);
  return {
    price: meta.regularMarketPrice,
    previousClose: meta.chartPreviousClose || meta.previousClose || 0,
    shortName: meta.shortName || null,
    dayHigh: highs.length ? Math.max(...highs) : 0,
    dayLow: lows.length ? Math.min(...lows) : 0,
    quoteType: meta.instrumentType || meta.quoteType || null,
  };
}

async function fetchPriceHistory(ticker, range, interval) {
  const url = `https://query1.finance.yahoo.com/v8/finance/chart/${encodeURIComponent(ticker)}?range=${range}&interval=${interval}`;
  const resp = await yf(url);
  if (!resp.ok) throw new Error(`Yahoo history ${ticker}: ${resp.status}`);
  const data = await resp.json();
  const result = data.chart?.result?.[0];
  if (!result) return [];
  const timestamps = result.timestamp || [];
  const closes = result.indicators?.quote?.[0]?.close || [];
  return timestamps
    .map((t, i) => ({ timestamp: t, close: closes[i] }))
    .filter(p => p.close != null);
}

async function fetchPriceHistoryByPeriod(ticker, period1, period2, interval) {
  const url = `https://query1.finance.yahoo.com/v8/finance/chart/${encodeURIComponent(ticker)}?period1=${period1}&period2=${period2}&interval=${interval}`;
  const resp = await yf(url);
  if (!resp.ok) throw new Error(`Yahoo history-period ${ticker}: ${resp.status}`);
  const data = await resp.json();
  const result = data.chart?.result?.[0];
  if (!result) return [];
  const timestamps = result.timestamp || [];
  const closes = result.indicators?.quote?.[0]?.close || [];
  return timestamps
    .map((t, i) => ({ timestamp: t, close: closes[i] }))
    .filter(p => p.close != null);
}

async function fetchAnalysisInfo(ticker) {
  if (!crumb) await refreshCrumb();
  const modules = 'assetProfile,defaultKeyStatistics,financialData,summaryDetail,calendarEvents,recommendationTrend,fundProfile,topHoldings';
  const url = `https://query2.finance.yahoo.com/v10/finance/quoteSummary/${encodeURIComponent(ticker)}?modules=${modules}&crumb=${encodeURIComponent(crumb || '')}`;
  const resp = await yf(url, { headers: { Cookie: cookies } });
  if (resp.status === 401 || resp.status === 403) {
    await refreshCrumb();
    const retryUrl = `https://query2.finance.yahoo.com/v10/finance/quoteSummary/${encodeURIComponent(ticker)}?modules=${modules}&crumb=${encodeURIComponent(crumb || '')}`;
    const retry = await yf(retryUrl, { headers: { Cookie: cookies } });
    if (!retry.ok) throw new Error(`Yahoo analysis ${ticker}: ${retry.status}`);
    const data = await retry.json();
    return parseAnalysis(data);
  }
  if (!resp.ok) throw new Error(`Yahoo analysis ${ticker}: ${resp.status}`);
  const data = await resp.json();
  return parseAnalysis(data);
}

function parseAnalysis(data) {
  const r = data.quoteSummary?.result?.[0] || {};
  const ap = r.assetProfile || {};
  const fd = r.financialData || {};
  const ks = r.defaultKeyStatistics || {};
  const sd = r.summaryDetail || {};
  return {
    shortName: ap.shortName || '',
    sector: ap.sector || '',
    industry: ap.industry || '',
    longBusinessSummary: ap.longBusinessSummary || '',
    marketCap: fd.marketCap?.raw || sd.marketCap?.raw || 0,
    trailingPE: sd.trailingPE?.raw || 0,
    forwardPE: sd.forwardPE?.raw || ks.forwardPE?.raw || 0,
    eps: ks.trailingEps?.raw || fd.earningsPerShare?.raw || 0,
    dividendYield: sd.dividendYield?.raw || 0,
    fiftyTwoWeekHigh: sd.fiftyTwoWeekHigh?.raw || 0,
    fiftyTwoWeekLow: sd.fiftyTwoWeekLow?.raw || 0,
    fiftyDayAverage: sd.fiftyDayAverage?.raw || 0,
    twoHundredDayAverage: sd.twoHundredDayAverage?.raw || 0,
    targetMeanPrice: fd.targetMeanPrice?.raw || 0,
    revenuePerShare: fd.revenuePerShare?.raw || 0,
    profitMargins: fd.profitMargins?.raw || 0,
    returnOnEquity: fd.returnOnEquity?.raw || 0,
    calendarEvents: r.calendarEvents || {},
    recommendationTrend: r.recommendationTrend || {},
    fundProfile: r.fundProfile || {},
    topHoldings: r.topHoldings || {},
  };
}

async function fetchNews(ticker, count = 5) {
  const url = `https://query2.finance.yahoo.com/v1/finance/search?q=${encodeURIComponent(ticker)}&newsCount=${count}`;
  const resp = await yf(url);
  if (!resp.ok) return [];
  const data = await resp.json();
  return (data.news || []).map(n => ({
    title: n.title,
    link: n.link,
    publisher: n.publisher,
    publishedAt: n.providerPublishTime,
  }));
}

async function fetchScanData(ticker) {
  const history = await fetchPriceHistory(ticker, '1mo', '1d');
  const last20 = history.slice(-20);
  if (last20.length === 0) return { twentyDaySma: 0, avgVolume20Day: 0, dayHigh: 0, dayLow: 0, previousClose: 0 };
  const sma = last20.reduce((s, p) => s + p.close, 0) / last20.length;
  const highs = last20.map(p => p.close);
  const lows = last20.map(p => p.close);
  return {
    twentyDaySma: sma,
    dayHigh: Math.max(...highs),
    dayLow: Math.min(...lows),
    previousClose: last20[last20.length - 1]?.close || 0,
  };
}

async function fetchLogo(ticker) {
  const sources = [
    `https://companiesmarketcap.com/img/company-logos/64/${ticker.toLowerCase()}.webp`,
    `https://assets.parqet.com/logos/symbol/${ticker}`,
    `https://storage.googleapis.com/iexcloud-hl37opg/api/logos/${ticker}.png`,
  ];
  for (const url of sources) {
    try {
      const resp = await fetch(url);
      if (resp.ok) {
        const buf = Buffer.from(await resp.arrayBuffer());
        if (buf.length > 100) return buf;
      }
    } catch {}
  }
  return null;
}

async function fetchFullReport(ticker) {
  const [quote, analysis, news] = await Promise.allSettled([
    fetchQuote(ticker),
    fetchAnalysisInfo(ticker),
    fetchNews(ticker, 5),
  ]);
  return {
    quote: quote.status === 'fulfilled' ? quote.value : null,
    analysis: analysis.status === 'fulfilled' ? analysis.value : null,
    news: news.status === 'fulfilled' ? news.value : [],
  };
}

module.exports = {
  fetchQuote, fetchPriceHistory, fetchPriceHistoryByPeriod,
  fetchAnalysisInfo, fetchNews, fetchScanData, fetchLogo, fetchFullReport, refreshCrumb,
};
