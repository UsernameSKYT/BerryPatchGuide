"""
RHDN (ROMHacking.net) scraper
1차: cloudscraper (Cloudflare 우회)
2차: playwright headless browser (cloudscraper도 막힐 경우)
우회 실패 시 빈 리스트 반환. Modrinth 폴백 없음.
"""
import logging
import re
import urllib.parse

logger = logging.getLogger(__name__)

TIMEOUT = 15
BASE = "https://www.romhacking.net"
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

# 마지막 요청 상태
bypass_status: str = "unknown"  # "ok" | "blocked" | "error"


# ─── HTML 파싱 ────────────────────────────────────────────────────────────────

def _parse_row(row, section: str) -> dict | None:
    try:
        cells = row.find_all("td")
        if len(cells) < 2:
            return None

        link_tag = None
        for cell in cells[:3]:
            link_tag = cell.find("a")
            if link_tag:
                break
        if not link_tag:
            return None

        title = link_tag.get_text(strip=True)
        href = link_tag.get("href", "")
        if not href:
            return None
        if not href.startswith("http"):
            href = BASE + href

        id_match = re.search(r"/(\d+)/?$", href)
        item_id = id_match.group(1) if id_match else href.replace("/", "_")

        author = ""
        for cell in cells:
            author_link = cell.find("a")
            if author_link:
                candidate = author_link.get_text(strip=True)
                if candidate and candidate != title:
                    author = candidate
                    break

        description = f"ROM hack / translation – {title}"
        date_str = ""
        last_cell_txt = cells[-1].get_text(strip=True)
        date_match = re.search(r"(\d{4}-\d{2}-\d{2}|\d{2}/\d{2}/\d{4})", last_cell_txt)
        if date_match:
            date_str = date_match.group(1)

        return {
            "id": f"rhdn_{item_id}",
            "title": title,
            "description": description,
            "source": "rhdn",
            "thumbnailUrl": None,
            "downloadUrl": href,
            "pageUrl": href,
            "author": author,
            "downloads": 0,
            "rating": 0,
            "updatedAt": date_str,
            "tags": ["rhdn", section],
        }
    except Exception as exc:
        logger.debug("RHDN parse row error: %s", exc)
        return None


def _parse_html(html: str, section: str, limit: int) -> list[dict]:
    from bs4 import BeautifulSoup
    soup = BeautifulSoup(html, "lxml")
    table = (
        soup.find("table", class_="entrylist")
        or soup.find("table", {"class": re.compile(r"entry")})
        or soup.find("table")
    )
    if not table:
        return []
    results = []
    rows = table.find_all("tr")
    for row in rows[1:]:
        item = _parse_row(row, section)
        if item and item["title"]:
            results.append(item)
            if len(results) >= limit:
                break
    return results


# ─── 1차: cloudscraper ────────────────────────────────────────────────────────

def _fetch_cloudscraper(url: str) -> tuple[str | None, int]:
    try:
        import cloudscraper
        scraper = cloudscraper.create_scraper(
            browser={"browser": "chrome", "platform": "windows", "mobile": False}
        )
        resp = scraper.get(url, headers=HEADERS, timeout=TIMEOUT)
        logger.debug("RHDN cloudscraper HTTP %s, len=%d", resp.status_code, len(resp.text))
        if resp.status_code == 200 and "Just a moment" not in resp.text:
            return resp.text, resp.status_code
        return None, resp.status_code
    except Exception as exc:
        logger.warning("RHDN cloudscraper exception: %s", exc)
        return None, -1


# ─── 2차: playwright ──────────────────────────────────────────────────────────

def _fetch_playwright(url: str) -> str | None:
    try:
        from playwright.sync_api import sync_playwright
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()
            page.goto(url, wait_until="load", timeout=15000)
            content = page.content()
            browser.close()
        if "Just a moment" not in content and len(content) > 500:
            logger.debug("RHDN playwright OK, len=%d", len(content))
            return content
        logger.debug("RHDN playwright still challenge page, len=%d", len(content))
        return None
    except ImportError:
        logger.debug("playwright not installed")
        return None
    except Exception as exc:
        logger.warning("RHDN playwright exception: %s", exc)
        return None


# ─── 통합 fetch ───────────────────────────────────────────────────────────────

def _fetch_html(url: str) -> str | None:
    global bypass_status
    html, status_code = _fetch_cloudscraper(url)
    if html:
        bypass_status = "ok"
        return html
    logger.info("RHDN cloudscraper failed (HTTP %s); trying playwright", status_code)
    html = _fetch_playwright(url)
    if html:
        bypass_status = "ok"
        return html
    bypass_status = "blocked"
    logger.warning("RHDN both bypass methods failed for %s", url)
    return None


def _fetch_listing(url: str, section: str, limit: int) -> list[dict]:
    html = _fetch_html(url)
    if not html:
        return []
    return _parse_html(html, section, limit)


# ─── Public interface ────────────────────────────────────────────────────────

def search(query: str, limit: int = 20) -> list[dict]:
    q = urllib.parse.quote(query)
    results: list[dict] = []

    url_t = f"{BASE}/?page=translations&search={q}&perpage=20&submit=Filter"
    results.extend(_fetch_listing(url_t, "translation", limit))

    if len(results) < limit:
        url_h = f"{BASE}/?page=hacks&search={q}&perpage=20&submit=Filter"
        results.extend(_fetch_listing(url_h, "hack", limit - len(results)))

    logger.info("RHDN search('%s') → %d results (status=%s)", query, len(results), bypass_status)
    return results[:limit]


def featured(limit: int = 10) -> list[dict]:
    results: list[dict] = []

    url_t = f"{BASE}/?page=translations&perpage={limit}&startpage=1&order=date"
    results.extend(_fetch_listing(url_t, "translation", limit))

    if len(results) < limit:
        url_h = f"{BASE}/?page=hacks&perpage={limit}&startpage=1&order=date"
        results.extend(_fetch_listing(url_h, "hack", limit - len(results)))

    logger.info("RHDN featured → %d results (status=%s)", len(results), bypass_status)
    return results[:limit]
