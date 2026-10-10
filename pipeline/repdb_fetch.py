"""Downloads the RepDB flat illustrations used by the app into pipeline/repdb/ (not shipped in the repository: RepDB license term 3).
Then run: python3 repdb_build.py all"""
import json, os, urllib.request
HERE = os.path.dirname(__file__)
from repdb_map import MAP
BASE = "https://raw.githubusercontent.com/RepDB/exercise-dataset/main/"
def get(p): return urllib.request.urlopen(urllib.request.Request(BASE + p, headers={'User-Agent': 'Mozilla/5.0'}), timeout=60).read()
data = json.loads(get('exercises.json'))['exercises']; by = {e['name_en']: e for e in data}
os.makedirs(f'{HERE}/repdb', exist_ok=True); info = {}
need = dict(MAP); need['_burpee_squat'] = ('Jump Squat', '')
for oid, (nm, flag) in MAP.items():
    e = by[nm]; info[oid] = dict(rep=nm, repid=e['id'], files=e['images']['flat'], unilateral=e.get('is_unilateral'), met=e.get('met'))
for nm in set(v[0] for v in MAP.values()) | {'Jump Squat', 'Burpees'}:
    for k, p in by[nm]['images']['flat'].items():
        out = f"{HERE}/repdb/{by[nm]['id']}-{k}.webp"
        if not os.path.exists(out): open(out, 'wb').write(get(p))
json.dump(info, open(f'{HERE}/repdb_info.json', 'w'), indent=1)
print('done', len(os.listdir(f'{HERE}/repdb')), 'images')
