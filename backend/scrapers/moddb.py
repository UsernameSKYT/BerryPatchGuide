"""
ModDB scraper
1차: cloudscraper (Cloudflare 우회)
2차: playwright headless browser (cloudscraper도 막힐 경우)
우회 실패 시 빈 리스트 반환. Modrinth 폴백 없음.
"""
import logging
import re
import urllib.parse
from datetime import datetime

logger = logging.getLogger(__name__)

TIMEOUT = 15
MODDB_BASE = "https://www.moddb.com"
HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "en-US,en;q=0.9",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Referer": "https://www.google.com/",
}

# 마지막 요청 상태 (main.py에서 sources_status 구성에 활용)
bypass_status: str = "unknown"  # "ok" | "blocked" | "error"


# ─── HTML 파싱 ────────────────────────────────────────────────────────────────

def _parse_item_card(card) -> dict | None:
    try:
        from bs4 import BeautifulSoup  # lazy import to keep module loadable
        heading = card.select_one("h4 a, .summary a[href*='/mods/']")
        if not heading:
            return None
        title = heading.get_text(strip=True)
        href = heading.get("href", "")
        if not href.startswith("http"):
            href = MODDB_BASE + href
        item_id = href.rstrip("/").split("/")[-1]

        thumb_tag = card.select_one("img")
        thumb = None
        if thumb_tag:
            src = thumb_tag.get("src") or thumb_tag.get("data-src") or ""
            if src and not src.endswith("spacer.gif"):
                thumb = src if src.startswith("http") else MODDB_BASE + src

        desc_tag = card.select_one("p.subheading, p, .summary")
        description = desc_tag.get_text(strip=True)[:400] if desc_tag else ""

        date_tag = card.select_one("time")
        date_str = date_tag.get("datetime", "") if date_tag else ""
        if date_str:
            try:
                date_str = datetime.fromisoformat(date_str[:10]).strftime("%Y-%m-%d")
            except Exception:
                pass

        tag_tags = card.select(".tag, .category")
        tags = ["moddb"] + [t.get_text(strip=True).lower() for t in tag_tags]

        return {
            "id": f"moddb_{item_id}",
            "title": title,
            "description": description,
            "source": "moddb",
            "thumbnailUrl": thumb,
            "downloadUrl": href,
            "pageUrl": href,
            "author": "",
            "downloads": 0,
            "rating": 0,
            "updatedAt": date_str,
            "tags": list(set(tags)),
        }
    except Exception as exc:
        logger.debug("ModDB parse error: %s", exc)
        return None


def _parse_html(html: str, limit: int) -> list[dict]:
    from bs4 import BeautifulSoup
    soup = BeautifulSoup(html, "lxml")
    cards = (
        soup.select("#modgrid .inner")
        or soup.select(".results .inner")
        or soup.select(".media .inner")
        or soup.select("div.inner")
    )
    results = []
    for card in cards:
        item = _parse_item_card(card)
        if item and item["title"]:
            results.append(item)
            if len(results) >= limit:
                break
    return results


# ─── 1차: cloudscraper ────────────────────────────────────────────────────────

def _fetch_cloudscraper(url: str) -> tuple[str | None, int]:
    """HTML 텍스트와 HTTP 상태코드를 반환. 실패 시 (None, status_code)."""
    try:
        import cloudscraper
        scraper = cloudscraper.create_scraper(
            browser={"browser": "chrome", "platform": "windows", "mobile": False}
        )
        resp = scraper.get(url, headers=HEADERS, timeout=TIMEOUT)
        logger.debug("ModDB cloudscraper HTTP %s, len=%d", resp.status_code, len(resp.text))
        if resp.status_code == 200 and "Just a moment" not in resp.text:
            return resp.text, resp.status_code
        return None, resp.status_code
    except Exception as exc:
        logger.warning("ModDB cloudscraper exception: %s", exc)
        return None, -1


# ─── 2차: playwright ──────────────────────────────────────────────────────────

def _fetch_playwright(url: str) -> str | None:
    """playwright headless chromium으로 HTML 가져오기."""
    try:
        from playwright.sync_api import sync_playwright
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()
            page.goto(url, wait_until="load", timeout=15000)
            content = page.content()
            browser.close()
        if "Just a moment" not in content and len(content) > 500:
            logger.debug("ModDB playwright OK, len=%d", len(content))
            return content
        logger.debug("ModDB playwright still challenge page, len=%d", len(content))
        return None
    except ImportError:
        logger.debug("playwright not installed")
        return None
    except Exception as exc:
        logger.warning("ModDB playwright exception: %s", exc)
        return None


# ─── 통합 fetch ───────────────────────────────────────────────────────────────

def _fetch_html(url: str) -> str | None:
    global bypass_status
    html, status_code = _fetch_cloudscraper(url)
    if html:
        bypass_status = "ok"
        return html
    logger.info("ModDB cloudscraper failed (HTTP %s); trying playwright", status_code)
    html = _fetch_playwright(url)
    if html:
        bypass_status = "ok"
        return html
    bypass_status = "blocked"
    logger.warning("ModDB both bypass methods failed for %s", url)
    return None


def _scrape_moddb(url: str, limit: int) -> list[dict]:
    html = _fetch_html(url)
    if not html:
        return []
    return _parse_html(html, limit)


# ─── Public interface ────────────────────────────────────────────────────────

def search(query: str, limit: int = 20) -> list[dict]:
    url = f"{MODDB_BASE}/mods?filter=t&kw={urllib.parse.quote(query)}"
    results = _scrape_moddb(url, limit)
    logger.info("ModDB search('%s') → %d results (status=%s)", query, len(results), bypass_status)
    return results[:limit]


def featured(limit: int = 10) -> list[dict]:
    url = f"{MODDB_BASE}/mods?sort=downloads-desc"
    results = _scrape_moddb(url, limit)
    logger.info("ModDB featured → %d results (status=%s)", len(results), bypass_status)
    return results[:limit]
