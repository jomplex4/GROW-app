"""Build GROWTH exercise frames from the RepDB flat illustrations (https://repdb.co, free tier, attribution shown in the app):
recolour to the app palette (red top, black shorts, grey stage), morph between the start and peak poses, write WebP frames + meta."""
import sys, os, json, shutil, time
sys.path.insert(0, os.path.dirname(__file__))
import numpy as np, cv2
from PIL import Image
from matte import matte
from build_anim import bleed
from mls_morph import blend, premult, unpremult, keypoints, fix_swaps
import build3 as b3
from repdb_map import MAP

SRC = os.path.join(os.path.dirname(__file__), 'repdb')
OUT = '/home/claude/grow/app/src/main/assets/anim'
SZ = 448
STAGE = np.array([209, 210, 215], np.float32)        # flat stage grey (#D1D2D7)
INFO = json.load(open(os.path.join(os.path.dirname(__file__), 'repdb_info.json')))

def recolor(rgb):
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV_FULL).astype(np.float32)
    H = hsv[..., 0] * 360.0 / 255.0; S = hsv[..., 1] / 255.0; V = hsv[..., 2] / 255.0
    blue = (H >= 185) & (H <= 235)
    tank = blue & (H <= 203) & (S > 0.58) & (V > 0.50)
    neutral = blue & ~tank
    Vn = np.interp(V, [0, .19, .40, .65, .80, .99, 1.0], [0, .09, .19, .50, .68, .83, .85])
    H2 = H.copy(); S2 = S.copy(); V2 = V.copy()
    H2[tank] = 357.0; S2[tank] = np.minimum(1.0, S[tank] * 1.12); V2[tank] = V[tank] * 0.98
    S2[neutral] = S[neutral] * 0.08; V2[neutral] = Vn[neutral]
    out = np.stack([H2 * 255.0 / 360.0, S2 * 255.0, V2 * 255.0], axis=-1)
    return cv2.cvtColor(np.clip(out, 0, 255).astype(np.uint8), cv2.COLOR_HSV2RGB_FULL)

def load_rgba(path):
    pil = Image.open(path).convert('RGB')
    rgb = np.asarray(pil)
    rec = recolor(rgb)
    m = np.asarray(matte(pil, number_box=(0.0, 0.0)))[..., 3]          # alpha from the original (distance to the background)
    rgba = Image.fromarray(np.dstack([rec, m]), 'RGBA')
    col, al = bleed(rgba)
    out = np.dstack([col, al]).astype(np.float32)
    return cv2.resize(out, (SZ, SZ), interpolation=cv2.INTER_AREA)

def to_rgb(f):
    a = (f[..., 3:4] / 255.0)
    return np.clip(f[..., :3] * a + STAGE * (1 - a), 0, 255).astype(np.uint8)

def save(frames, d):
    shutil.rmtree(d, ignore_errors=True); os.makedirs(d)
    tot = 0
    for k, f in enumerate(frames):
        p = f'{d}/{k:02d}.webp'
        Image.fromarray(to_rgb(f), 'RGB').save(p, 'WEBP', quality=84, method=6)
        tot += os.path.getsize(p)
    return tot

def keys_for(oid):
    v = INFO[oid]; f = v['files']; rid = v['repid']
    if oid == 'burpee':
        sq = load_rgba(f"{SRC}/jump-squat-start.webp"); pk = load_rgba(f"{SRC}/burpees-main.webp"); jp = load_rgba(f"{SRC}/jump-squat-peak.webp")
        return [sq, pk, sq, jp], True
    if 'start' in f: return [load_rgba(f"{SRC}/{rid}-start.webp"), load_rgba(f"{SRC}/{rid}-peak.webp")], False
    return [load_rgba(f"{SRC}/{rid}-main.webp")], False

IDLE = {'rope': ('bounce', 0.52), 'highknee': ('alt', 0.50), 'walk': ('alt', 0.85),
        'jog': ('run', 0.62), 'run': ('run', 0.50), 'sprint': ('run', 0.40), 'sidebend': ('alt', 4.0)}
LOOP2 = {'jj': (1.40, .45, .05, .45, 0), 'march': (3.0, .40, .15, .40, 0), 'legswing': (2.4, .50, .0, .50, 0), 'buttkick': (3.2, .40, .20, .35, 0),
         'squatjump': (2.4, .40, .15, .35, 0), 'lungejump': (2.6, .45, .05, .45, 0), 'kneeraise': (3.2, .40, .20, .30, 0),
         'mclimb': (1.5, .45, .05, .45, 0), 'wallangel': (3.2, .40, .15, .35, 0), 'deadbug': (3.6, .35, .20, .35, 2),
         'birddog': (4.0, .35, .30, .25, 2), 'superman': (4.5, .30, .35, .25, 0), 'bridge': (3.6, .35, .25, .30, 0)}

b3.FORCE_FLOW.add(('legswing', 0))

def build(oid, meta, n_in=5):
    if oid == 'burpee': n_in = 0           # four clear poses with a quick change, no ghosting
    t0 = time.time()
    keys, cycle = keys_for(oid)
    dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    entry = dict(w=SZ, h=SZ, alt=0, step=0, cyc=0, bust=0, ground=0, opaque=1, br=0, idle='breath', ri=.5, ho=0., fa=.5, loop=4.0, mode='pp')
    if len(keys) == 1:
        seq = keys
        kind, loop = IDLE.get(oid, ('breath', 4.0))
        entry.update(idle=kind, loop=loop, mode='idle')
        if oid in ('hang', 'plank', 'wallstand', 'cobra', 'child', 'hamfold', 'knees2chest'): entry['idle'] = 'breath'
    else:
        try:
            kps = keypoints(keys); kps = fix_swaps(kps) if kps else None
        except Exception:
            kps = None
        seq = []; meth = []
        pairs = list(zip(keys, keys[1:])) + ([(keys[-1], keys[0])] if cycle else [])
        for i, (A, B) in enumerate(pairs):
            ka = kps[i] if kps else None; kb = (kps[(i + 1) % len(keys)] if kps else None)
            seq.append(A)
            ins, how = b3.inb(A, B, ka, kb, n_in, dis, oid, i); seq += ins; meth.append(how)
        if not cycle: seq.append(keys[-1])
        loop, ri, ho, fa, alt = LOOP2.get(oid, (4.0, .4, .2, .4, 0))
        if oid == 'burpee': loop, ri, ho, fa, alt = 3.8, .0, .0, .0, 0
        entry.update(loop=loop, ri=ri, ho=ho, fa=fa, alt=alt, mode='cy' if cycle else 'pp')
        print(oid, meth, end=' ')
    tot = save(seq, f'{OUT}/{oid}')
    entry['n'] = len(seq)
    meta[oid] = entry
    print(f'{oid}: {len(seq)} frames {tot // 1024} KB {round(time.time() - t0, 1)}s', flush=True)

if __name__ == '__main__':
    only = sys.argv[1].split(',') if len(sys.argv) > 1 and sys.argv[1] != 'all' else list(MAP)
    meta = json.load(open(OUT + '/meta.json'))
    for oid in only:
        build(oid, meta)
        json.dump(meta, open(OUT + '/meta.json', 'w'), separators=(',', ':'))
