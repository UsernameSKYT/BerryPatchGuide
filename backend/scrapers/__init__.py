"""
Berry Patch Guide – scraper package
Each submodule exposes:
  search(query: str, limit: int = 20) -> list[dict]
  featured(limit: int = 10) -> list[dict]
"""
from .gamebanana import search as gb_search, featured as gb_featured
from .moddb import search as moddb_search, featured as moddb_featured
from .rhdn import search as rhdn_search, featured as rhdn_featured

__all__ = [
    "gb_search", "gb_featured",
    "moddb_search", "moddb_featured",
    "rhdn_search", "rhdn_featured",
]
