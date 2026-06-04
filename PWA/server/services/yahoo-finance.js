let crumb = null;
let cookies = '';

async function refreshCrumb() {
  try {
    const r1 = await fetch('https://fc.yahoo.com/cupcake', { redirect: 'manual' });
    cookies = r1.headers.get('set-cookie') || '';
    const r2 = await fetch('https://query2.finance.yahoo.com/v1/test/getcrumb', {
      headers: { Cookie: cookies }
    });
    crumb = await r2.text();
  } catch (err) {
    console.error('Crumb refresh failed:', err.message);
  }
}

async function fetchQuote(ticker) {
  const url = `https://query1.finance.yahoo.com/v8/finance/chart/${encodeURIComponent(ticker)}?range=1d&interval=1m`;
  const resp = await fetch(url);
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
    dividendRate: meta.trailingAnnualDividendRate || 0,
  };
}

async function fetchPriceHistory(ticker, range, interval) {
  const url = `https://query1.finance.yahoo.com/v8/finance/chart/${encodeURIComponent(ticker)}?range=${range}&interval=${interval}`;
  const resp = await fetch(url);
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
  const resp = await fetch(url);
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
  const modules = 'assetProfile,defaultKeyStatistics,financialData,summaryDetail,calendarEvents,recommendationTrend,fundProfile,topHoldings';
  const enc = encodeURIComponent(ticker);

  // Try v10 with crumb auth
  try {
    if (!crumb) await refreshCrumb();
    if (crumb) {
      const url = `https://query2.finance.yahoo.com/v10/finance/quoteSummary/${enc}?modules=${modules}&crumb=${encodeURIComponent(crumb)}`;
      const resp = await fetch(url, { headers: { Cookie: cookies } });
      if (resp.status === 401 || resp.status === 403) {
        await refreshCrumb();
        if (crumb) {
          const retry = await fetch(`https://query2.finance.yahoo.com/v10/finance/quoteSummary/${enc}?modules=${modules}&crumb=${encodeURIComponent(crumb)}`, { headers: { Cookie: cookies } });
          if (retry.ok) return parseAnalysis(await retry.json());
        }
      } else if (resp.ok) {
        return parseAnalysis(await resp.json());
      }
    }
  } catch (e) {
    console.error(`v10 analysis failed for ${ticker}:`, e.message);
  }

  // Fallback: try v10 without crumb (works for some data)
  try {
    const url = `https://query2.finance.yahoo.com/v10/finance/quoteSummary/${enc}?modules=${modules}`;
    const resp = await fetch(url);
    if (resp.ok) return parseAnalysis(await resp.json());
  } catch (e) {
    console.error(`v10 no-crumb failed for ${ticker}:`, e.message);
  }

  // Fallback: build partial analysis from v8 chart data
  try {
    const quote = await fetchQuote(ticker);
    return {
      shortName: quote.shortName || '', sector: '', industry: '', longBusinessSummary: '',
      marketCap: 0, trailingPE: 0, forwardPE: 0, eps: 0, dividendYield: 0,
      trailingAnnualDividendRate: quote.dividendRate || 0,
      fiftyTwoWeekHigh: 0, fiftyTwoWeekLow: 0, fiftyDayAverage: 0, twoHundredDayAverage: 0,
      targetMeanPrice: 0, revenuePerShare: 0, profitMargins: 0, returnOnEquity: 0,
      calendarEvents: {}, recommendationTrend: {}, fundProfile: {}, topHoldings: {},
      _partial: true,
    };
  } catch (e) {
    throw new Error(`All analysis sources failed for ${ticker}`);
  }
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
    trailingAnnualDividendRate: sd.trailingAnnualDividendRate?.raw || 0,
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
  const resp = await fetch(url);
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
  const url = `https://query1.finance.yahoo.com/v8/finance/chart/${encodeURIComponent(ticker)}?range=1mo&interval=1d`;
  const resp = await fetch(url);
  if (!resp.ok) throw new Error(`Yahoo scan ${ticker}: ${resp.status}`);
  const data = await resp.json();
  const result = data.chart?.result?.[0];
  if (!result) return { twentyDaySma: 0, avgVolume20Day: 0, closingVolume: 0, dayHigh: 0, dayLow: 0, previousClose: 0 };
  const timestamps = result.timestamp || [];
  const quote = result.indicators?.quote?.[0] || {};
  const closes = quote.close || [];
  const volumes = quote.volume || [];
  const points = timestamps.map((t, i) => ({ close: closes[i], volume: volumes[i] })).filter(p => p.close != null);
  const last20 = points.slice(-20);
  if (last20.length === 0) return { twentyDaySma: 0, avgVolume20Day: 0, closingVolume: 0, dayHigh: 0, dayLow: 0, previousClose: 0 };
  const sma = last20.reduce((s, p) => s + p.close, 0) / last20.length;
  const avgVol = last20.reduce((s, p) => s + (p.volume || 0), 0) / last20.length;
  const lastPoint = last20[last20.length - 1];
  return {
    twentyDaySma: sma,
    avgVolume20Day: Math.round(avgVol),
    closingVolume: lastPoint.volume || 0,
    dayHigh: Math.max(...last20.map(p => p.close)),
    dayLow: Math.min(...last20.map(p => p.close)),
    previousClose: lastPoint.close || 0,
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
