import sys, os, json, time, shutil
sys.path.insert(0, os.path.dirname(__file__))
from build_anim import *
EXJ = json.load(open('/home/claude/grow/app/src/main/assets/exercises.json'))
main_ids = [e['id'] for e in EXJ if e['p'] != 'J']
jaw_ids = [e['id'] for e in EXJ if e['p'] == 'J']
NO_MID = {4, 28, 29, 30, 34}          # their intermediate pose is almost identical to the end pose
ALT = {1, 5, 6}
STEP = {1, 3, 5, 6, 7, 8, 9, 11, 12, 13, 22, 23}   # fast cyclic moves: clean key poses, no in-between ghosting
FLIP_START = {19, 20}                                  # their start pose faces the other way
CFG = {8: dict(head=True, align='bottom'), 15: dict(head=True, align='top', manual=[1.0, 0.9, 0.83])}                         # front-view alternating exercises: second half uses the mirrored frames
J5 = 'ChatGPT_Image_5_oct_2026__11_20_48.png'

def job(kind, n, id_):
    if kind == 'A':
        keys = [cell(*start_of(n))]
        if n not in NO_MID: keys.append(cell(*mid_of(n)))
        keys.append(cell(*end_of(n)))
    else:
        s = single(J5) if n == 5 else cell('JS', n)
        keys = [s, cell('JE', n)]
    return keys

def run(only=None, mode='flow'):
    os.makedirs(OUT, exist_ok=True)
    meta = json.load(open(OUT + '/meta.json')) if os.path.exists(OUT + '/meta.json') else {}
    report = {}
    todo = [('A', i + 1, id_) for i, id_ in enumerate(main_ids)] + [('B', i + 1, id_) for i, id_ in enumerate(jaw_ids)]
    for kind, n, id_ in todo:
        if only and id_ not in only: continue
        t = time.time()
        keys = job(kind, n, id_)
        step = kind == 'A' and n in STEP
        nin = 0 if step else (N_IN if len(keys) == 3 else 7)
        import build_anim as ba
        ba.N_IN = nin
        flips = [kind == 'A' and n in FLIP_START] + [False] * (len(keys) - 1)
        seq, F, size, info = frames_for(keys, mode, CFG.get(n) if kind == 'A' else None, flips)
        d = f'{OUT}/{id_}'
        shutil.rmtree(d, ignore_errors=True); os.makedirs(d)
        total = 0
        for k, f in enumerate(seq):
            total += save_webp(f, f'{d}/{k:02d}.webp', 80)
        # thumbnail from the first frame
        th = Image.fromarray(np.clip(seq[0], 0, 255).astype(np.uint8), 'RGBA')
        th = th.resize((int(th.width * 150 / th.height), 150), Image.LANCZOS)
        total += (th.save(f'{d}/t.webp', 'WEBP', quality=70, method=6) or os.path.getsize(f'{d}/t.webp'))
        meta[id_] = dict(n=len(seq), w=size[0], h=size[1], alt=int(kind == 'A' and n in ALT), step=int(step), bust=int(kind == 'B'),
                         ground=int(not (kind == 'B' or id_ in ('hang', 'kneeraise'))))
        report[id_] = dict(reg=[(a, b, bool(c)) for a, b, c in info], bytes=total, size=size, secs=round(time.time() - t, 1))
        print(id_, report[id_], flush=True)
        json.dump(meta, open(OUT + '/meta.json', 'w'), separators=(',', ':'))
    json.dump(report, open('/tmp/anim_report.json', 'w'))

if __name__ == '__main__':
    only = sys.argv[1].split(',') if len(sys.argv) > 1 and sys.argv[1] != 'all' else None
    run(only)
