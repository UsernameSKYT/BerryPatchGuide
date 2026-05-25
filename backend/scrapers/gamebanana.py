"""
GameBanana scraper – uses the official GameBanana API v11.
Search:   GET https://gamebanana.com/apiv11/Util/Search/Results
Index:    GET https://gamebanana.com/apiv11/Mod/Index
"""
import logging
import re
import requests
from datetime import datetime, timezone

logger = logging.getLogger(__name__)

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    )
}
TIMEOUT = 10
API_BASE = "https://gamebanana.com/apiv11"
_SEARCH_MODELS = ["Mod", "Sound", "Texture", "Wip"]


def _thumb_from_media(preview_media: dict) -> str | None:
    images = preview_media.get("_aImages", [])
    if not images:
        return None
    img = images[0]
    base = img.get("_sBaseUrl", "")
    for key in ("_sFile220", "_sFile530", "_sFile100", "_sFile"):
        f = img.get(key, "")
        if f:
            return f"{base}/{f}" if base else f
    return None


def _build_item(raw: dict) -> dict:
    item_id = str(raw.get("_idRow", ""))
    model = raw.get("_sModelName", "Mod")
    name = raw.get("_sName", "")
    page_url = raw.get("_sProfileUrl") or f"https://gamebanana.com/{model.lower()}s/{item_id}"
    download_url = raw.get("_sDownloadUrl") or page_url
    thumb = _thumb_from_media(raw.get("_aPreviewMedia") or {})

    submitter = raw.get("_aSubmitter") or {}
    author = submitter.get("_sName", "") if isinstance(submitter, dict) else ""

    downloads = raw.get("_nDownloadCount", 0) or 0
    rating = raw.get("_nLikeCount", 0) or 0

    ts = raw.get("_tsDateModified") or raw.get("_tsDateAdded")
    date_str = ""
    if ts:
        try:
            date_str = datetime.fromtimestamp(ts, tz=timezone.utc).strftime("%Y-%m-%d")
        except Exception:
            pass

    raw_tags = raw.get("_aTags") or []
    tags = ["gamebanana", model.lower()]
    for t in raw_tags:
        cleaned = t.split(":")[0].strip().lower().replace(" ", "-")
        if cleaned:
            tags.append(cleaned)

    desc_raw = raw.get("_sText") or raw.get("_sDescription") or ""
    description = re.sub(r"<[^>]+>", "", desc_raw)[:500]

    return {
        "id": f"gb_{item_id}",
        "title": name,
        "description": description,
        "source": "gamebanana",
        "thumbnailUrl": thumb,
        "downloadUrl": download_url,
        "pageUrl": page_url,
        "author": author,
        "downloads": downloads,
        "rating": rating,
        "updatedAt": date_str,
        "tags": list(dict.fromkeys(tags)),
    }


def search(query: str, limit: int = 20) -> list[dict]:
    """Search GameBanana via Util/Search/Results endpoint."""
    results: list[dict] = []
    for model in _SEARCH_MODELS:
        if len(results) >= limit:
            break
        try:
            resp = requests.get(
                f"{API_BASE}/Util/Search/Results",
                params={
                    "_sSearchString": query,
                    "_nPerpage": min(15, limit),
                    "_nPage": 1,
                    "_sModelName": model,
                },
                headers=HEADERS,
                timeout=TIMEOUT,
            )
            if resp.status_code != 200:
                logger.debug("GameBanana search [%s] HTTP %s", model, resp.status_code)
                continue
            data = resp.json()
            records = data.get("_aRecords", [])
            for raw in records:
                results.append(_build_item(raw))
                if len(results) >= limit:
                    break
        except Exception as exc:
            logger.warning("GameBanana search [%s] error: %s", model, exc)
            continue

    logger.info("GameBanana search('%s') → %d results", query, len(results))
    if not results:
        logger.debug("GameBanana: 0 results for '%s'", query)
    return results[:limit]


def featured(limit: int = 10) -> list[dict]:
    """Return latest GameBanana mods from Mod/Index."""
    results: list[dict] = []
    try:
        resp = requests.get(
            f"{API_BASE}/Mod/Index",
            params={
                "_nPerpage": limit,
                "_nPage": 1,
                "_sOrderBy": "_tsDateModified",
                "_sOrder": "DESC",
            },
            headers=HEADERS,
            timeout=TIMEOUT,
        )
        if resp.status_code == 200:
            data = resp.json()
            for raw in data.get("_aRecords", []):
                results.append(_build_item(raw))
                if len(results) >= limit:
                    break
        else:
            logger.debug("GameBanana featured HTTP %s", resp.status_code)
    except Exception as exc:
        logger.warning("GameBanana featured error: %s", exc)

    logger.info("GameBanana featured → %d results", len(results))
    if not results:
        logger.debug("GameBanana: 0 featured results")
    return results[:limit]
