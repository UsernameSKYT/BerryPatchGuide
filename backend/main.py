"""
Berry Patch Guide – FastAPI backend
Fetches real patch/mod data from GameBanana, ModDB, and RHDN.
"""
import asyncio
import logging
import time
from concurrent.futures import ThreadPoolExecutor
from typing import List, Optional

from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy import create_engine, Column, Integer, String, Text
from sqlalchemy.orm import sessionmaker, declarative_base

import sys, os
sys.path.insert(0, os.path.dirname(__file__))

import scrapers.gamebanana as gb
import scrapers.moddb as moddb
import scrapers.rhdn as rhdn

# ─── Logging ────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s – %(message)s",
)
logger = logging.getLogger("main")

# ─── App ────────────────────────────────────────────────────────────────────
app = FastAPI(title="Berry Patch Guide API", version="2.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─── DB ─────────────────────────────────────────────────────────────────────
DATABASE_URL = "sqlite:///./patchguide.db"
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(bind=engine)
Base = declarative_base()


class Patch(Base):
    __tablename__ = "patches"
    id = Column(Integer, primary_key=True, index=True)
    patch_id = Column(String(100), unique=True)
    title = Column(String(255))
    description = Column(Text)
    author = Column(String(100))
    thumbnail_url = Column(String(500))
    download_url = Column(String(500))
    source = Column(String(50))
    created_at = Column(String(50))
    tags = Column(String(500))


Base.metadata.create_all(bind=engine)

# ─── Pydantic models ─────────────────────────────────────────────────────────
class PatchItem(BaseModel):
    id: str
    title: str
    description: Optional[str] = None
    author: Optional[str] = None
    thumbnail_url: Optional[str] = None
    download_url: Optional[str] = None
    source: str
    created_at: Optional[str] = None
    tags: List[str] = []
    # extra fields returned by scrapers (ignored by older clients)
    pageUrl: Optional[str] = None
    downloads: Optional[int] = None
    rating: Optional[int] = None
    updatedAt: Optional[str] = None


class SearchResponse(BaseModel):
    results: List[PatchItem]
    total: int
    page: int
    per_page: int
    sources_status: Optional[dict] = None  # {"gamebanana":"ok","moddb":"ok"|"blocked","rhdn":"ok"|"blocked"}


# ─── In-memory cache (TTL = 10 minutes) ─────────────────────────────────────
_CACHE: dict[str, tuple[float, list[dict]]] = {}
_TTL = 600  # seconds


def _cache_get(key: str) -> list[dict] | None:
    entry = _CACHE.get(key)
    if entry and (time.time() - entry[0]) < _TTL:
        logger.info("Cache HIT for key='%s'", key)
        return entry[1]
    return None


def _cache_set(key: str, data: list[dict]) -> None:
    _CACHE[key] = (time.time(), data)


# ─── Parallel scraper helpers ────────────────────────────────────────────────
_EXECUTOR = ThreadPoolExecutor(max_workers=6)


def _safe_call(fn, *args, **kwargs) -> list[dict]:
    """Call a scraper function, returning [] on any exception."""
    try:
        return fn(*args, **kwargs) or []
    except Exception as exc:
        logger.warning("Scraper %s failed: %s", fn.__qualname__, exc)
        return []


async def _parallel_search(query: str, limit: int) -> tuple[list[dict], dict]:
    cache_key = f"search:{query}:{limit}"
    cached = _cache_get(cache_key)
    if cached is not None:
        return cached

    loop = asyncio.get_event_loop()
    per = max(10, limit)
    futures = [
        loop.run_in_executor(_EXECUTOR, _safe_call, gb.search, query, per),
        loop.run_in_executor(_EXECUTOR, _safe_call, moddb.search, query, per),
        loop.run_in_executor(_EXECUTOR, _safe_call, rhdn.search, query, per),
    ]
    gb_list, moddb_list, rhdn_list = await asyncio.gather(*futures)
    source_lists = [gb_list, moddb_list, rhdn_list]

    # Interleave results from different sources so page-1 has all 3 sources
    all_results = []
    max_len = max((len(s) for s in source_lists), default=0)
    for i in range(max_len):
        for src in source_lists:
            if i < len(src):
                all_results.append(src[i])

    status = {
        "gamebanana": "ok" if gb_list else "blocked",
        "moddb": moddb.bypass_status if moddb.bypass_status != "unknown" else ("ok" if moddb_list else "blocked"),
        "rhdn": rhdn.bypass_status if rhdn.bypass_status != "unknown" else ("ok" if rhdn_list else "blocked"),
    }

    logger.info("_parallel_search('%s') total=%d status=%s", query, len(all_results), status)
    _cache_set(cache_key, (all_results, status))
    return all_results, status


async def _parallel_featured(limit: int) -> tuple[list[dict], dict]:
    cache_key = f"featured:{limit}"
    cached = _cache_get(cache_key)
    if cached is not None:
        return cached

    loop = asyncio.get_event_loop()
    per = max(10, limit)
    futures = [
        loop.run_in_executor(_EXECUTOR, _safe_call, gb.featured, per),
        loop.run_in_executor(_EXECUTOR, _safe_call, moddb.featured, per),
        loop.run_in_executor(_EXECUTOR, _safe_call, rhdn.featured, per),
    ]
    gb_list, moddb_list, rhdn_list = await asyncio.gather(*futures)
    source_lists_f = [gb_list, moddb_list, rhdn_list]

    # Interleave sources
    all_results = []
    max_len = max((len(s) for s in source_lists_f), default=0)
    for i in range(max_len):
        for src in source_lists_f:
            if i < len(src):
                all_results.append(src[i])

    status = {
        "gamebanana": "ok" if gb_list else "blocked",
        "moddb": moddb.bypass_status if moddb.bypass_status != "unknown" else ("ok" if moddb_list else "blocked"),
        "rhdn": rhdn.bypass_status if rhdn.bypass_status != "unknown" else ("ok" if rhdn_list else "blocked"),
    }

    logger.info("_parallel_featured() total=%d status=%s", len(all_results), status)
    _cache_set(cache_key, (all_results, status))
    return all_results, status


# ─── Helper: dict → PatchItem ────────────────────────────────────────────────
def _to_patch_item(d: dict) -> PatchItem:
    return PatchItem(
        id=d.get("id", ""),
        title=d.get("title", ""),
        description=d.get("description"),
        author=d.get("author"),
        thumbnail_url=d.get("thumbnailUrl"),
        download_url=d.get("downloadUrl") or d.get("pageUrl"),
        source=d.get("source", ""),
        created_at=d.get("updatedAt"),
        tags=d.get("tags", []),
        pageUrl=d.get("pageUrl"),
        downloads=d.get("downloads"),
        rating=d.get("rating"),
        updatedAt=d.get("updatedAt"),
    )


# ─── Endpoints ───────────────────────────────────────────────────────────────
@app.get("/")
def root():
    return {"message": "Berry Patch Guide API", "version": "2.0.0"}


@app.get("/search", response_model=SearchResponse)
async def search(
    q: str = Query(..., description="검색어"),
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
):
    """모든 소스에서 병렬 검색 (GameBanana + ModDB + RHDN)"""
    raw, status = await _parallel_search(q, limit * 3)
    start = (page - 1) * limit
    page_data = raw[start : start + limit]
    results = [_to_patch_item(d) for d in page_data]
    return SearchResponse(results=results, total=len(raw), page=page, per_page=limit, sources_status=status)


@app.get("/featured", response_model=SearchResponse)
async def featured(
    page: int = Query(1, ge=1),
    limit: int = Query(10, ge=1, le=50),
):
    """추천 패치 (3개 소스 병렬, 캐싱)"""
    raw, status = await _parallel_featured(limit * 3)
    start = (page - 1) * limit
    page_data = raw[start : start + limit]
    results = [_to_patch_item(d) for d in page_data]
    return SearchResponse(results=results, total=len(raw), page=page, per_page=limit, sources_status=status)


@app.get("/sources/{source}", response_model=SearchResponse)
async def search_by_source(
    source: str,
    q: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
):
    """특정 소스에서만 검색"""
    source_lower = source.lower()
    scraper_map = {
        "gamebanana": gb,
        "moddb": moddb,
        "rhdn": rhdn,
    }
    mod = scraper_map.get(source_lower)
    if mod is None:
        return SearchResponse(results=[], total=0, page=page, per_page=limit, sources_status={source_lower: "unknown"})

    loop = asyncio.get_event_loop()
    if q:
        raw = await loop.run_in_executor(_EXECUTOR, _safe_call, mod.search, q, limit)
    else:
        raw = await loop.run_in_executor(_EXECUTOR, _safe_call, mod.featured, limit)

    bypass = getattr(mod, "bypass_status", None)
    src_status = bypass if bypass and bypass != "unknown" else ("ok" if raw else "blocked")
    results = [_to_patch_item(d) for d in raw]
    return SearchResponse(results=results, total=len(results), page=page, per_page=limit, sources_status={source_lower: src_status})


@app.get("/cache/clear")
def cache_clear():
    _CACHE.clear()
    return {"message": "Cache cleared"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
